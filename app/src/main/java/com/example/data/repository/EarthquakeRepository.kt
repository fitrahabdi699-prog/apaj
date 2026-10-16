package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.ApiClient
import com.example.data.api.BmkgApiService
import com.example.data.api.UsgsApiService
import com.example.data.local.*
import com.example.data.model.*
import com.example.service.EarthquakeNotificationHelper
import com.example.util.DateTimeUtils
import com.example.util.GeoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.abs

class EarthquakeRepository(
    private val context: Context,
    private val bmkgApi: BmkgApiService = ApiClient.bmkgApi,
    private val usgsApi: UsgsApiService = ApiClient.usgsApi,
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    private val prefManager: PreferenceManager = PreferenceManager(context)
) {
    private val earthquakeDao = database.earthquakeDao()
    private val favoriteDao = database.favoriteDao()
    private val notificationLogDao = database.notificationLogDao()
    private val checklistDao = database.checklistDao()

    val earthquakesFlow: Flow<List<UnifiedEarthquake>> =
        earthquakeDao.getAllEarthquakesFlow().map { list ->
            val userLoc = prefManager.getUserLocation()
            list.map { it.toDomain(userLoc.latitude, userLoc.longitude) }
        }

    val favoriteIdsFlow: Flow<List<String>> = favoriteDao.getAllFavoriteIdsFlow()

    suspend fun getLatestAutoGempa(): UnifiedEarthquake? = withContext(Dispatchers.IO) {
        val userLoc = prefManager.getUserLocation()
        earthquakeDao.getLatestAutoGempa()?.toDomain(userLoc.latitude, userLoc.longitude)
    }

    suspend fun getEarthquakeById(id: String): UnifiedEarthquake? = withContext(Dispatchers.IO) {
        val userLoc = prefManager.getUserLocation()
        earthquakeDao.getEarthquakeById(id)?.toDomain(userLoc.latitude, userLoc.longitude)
    }

    suspend fun refreshAllData(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val combinedList = mutableListOf<UnifiedEarthquake>()

            // 1. Fetch BMKG AutoGempa (latest)
            var autoGempa: UnifiedEarthquake? = null
            try {
                val autoResponse = bmkgApi.getAutogempa()
                val item = autoResponse.infoGempa?.gempa
                if (item != null) {
                    val parsed = parseBmkgItem(item, isAuto = true)
                    if (parsed != null) {
                        autoGempa = parsed
                        combinedList.add(parsed)
                    }
                }
            } catch (e: Exception) {
                Log.w("EarthquakeRepo", "BMKG AutoGempa fetch error: ${e.message}")
            }

            // 2. Fetch BMKG Gempa Terkini (M 5.0+)
            try {
                val terkiniResponse = bmkgApi.getGempaTerkini()
                terkiniResponse.infoGempa?.gempaList?.forEach { item ->
                    parseBmkgItem(item, isAuto = false)?.let { combinedList.add(it) }
                }
            } catch (e: Exception) {
                Log.w("EarthquakeRepo", "BMKG Gempa Terkini fetch error: ${e.message}")
            }

            // 3. Fetch BMKG Gempa Dirasakan
            try {
                val dirasakanResponse = bmkgApi.getGempaDirasakan()
                dirasakanResponse.infoGempa?.gempaList?.forEach { item ->
                    parseBmkgItem(item, isAuto = false)?.let { combinedList.add(it) }
                }
            } catch (e: Exception) {
                Log.w("EarthquakeRepo", "BMKG Gempa Dirasakan fetch error: ${e.message}")
            }

            // 4. Fetch USGS (Global)
            try {
                val usgsHourResponse = usgsApi.get45Hour()
                usgsHourResponse.features?.forEach { feature ->
                    parseUsgsFeature(feature)?.let { combinedList.add(it) }
                }
            } catch (e: Exception) {
                Log.w("EarthquakeRepo", "USGS 4.5 Hour fetch error: ${e.message}")
            }

            // Fetch USGS 2.5 Day for rich global dataset
            try {
                val usgsDayResponse = usgsApi.get25Day()
                usgsDayResponse.features?.forEach { feature ->
                    parseUsgsFeature(feature)?.let { combinedList.add(it) }
                }
            } catch (e: Exception) {
                Log.w("EarthquakeRepo", "USGS 2.5 Day fetch error: ${e.message}")
            }

            if (combinedList.isEmpty()) {
                val cached = earthquakeDao.getRecentEarthquakes()
                return@withContext if (cached.isNotEmpty()) {
                    Result.success(cached.size)
                } else {
                    Result.failure(Exception("Gagal mengambil data dari BMKG dan USGS. Periksa koneksi internet."))
                }
            }

            // 5. Deduplicate across sources
            val deduplicated = deduplicateEvents(combinedList)

            // 6. Save to Room DB
            val entities = deduplicated.map { EarthquakeEntity.fromDomain(it) }
            earthquakeDao.insertEarthquakes(entities)
            prefManager.saveLastSyncTimestamp(System.currentTimeMillis())

            // 7. Evaluate notifications for new earthquakes
            evaluateAndTriggerNotifications(deduplicated)

            Result.success(deduplicated.size)
        } catch (e: Exception) {
            Log.e("EarthquakeRepo", "Refresh error", e)
            val cached = earthquakeDao.getRecentEarthquakes()
            if (cached.isNotEmpty()) {
                Result.success(cached.size)
            } else {
                Result.failure(e)
            }
        }
    }

    private fun parseBmkgItem(item: BmkgGempaItem, isAuto: Boolean): UnifiedEarthquake? {
        val mag = item.magnitude?.toDoubleOrNull() ?: return null
        val timeMillis = DateTimeUtils.parseBmkgDateTime(item.dateTime, item.tanggal, item.jam)
        val wibStr = DateTimeUtils.formatWib(timeMillis)
        val utcStr = DateTimeUtils.formatUtc(timeMillis)

        // Parse coordinates
        var lat = 0.0
        var lon = 0.0
        val coords = item.coordinates?.split(",")
        if (coords != null && coords.size >= 2) {
            lat = coords[0].trim().toDoubleOrNull() ?: 0.0
            lon = coords[1].trim().toDoubleOrNull() ?: 0.0
        } else {
            // parse from Lintang and Bujur
            item.lintang?.let { l ->
                val v = l.replace(" LS", "").replace(" LU", "").trim().toDoubleOrNull() ?: 0.0
                lat = if (l.contains("LS")) -v else v
            }
            item.bujur?.let { b ->
                val v = b.replace(" BT", "").replace(" BB", "").trim().toDoubleOrNull() ?: 0.0
                lon = if (b.contains("BB")) -v else v
            }
        }

        val depthKm = item.kedalaman?.replace(" km", "")?.trim()?.toDoubleOrNull() ?: 10.0
        val place = item.wilayah ?: "Indonesia"

        // Tsunami status
        val potensiText = item.potensi ?: ""
        val tsunamiStatus = when {
            potensiText.contains("tidak berpotensi tsunami", ignoreCase = true) -> TsunamiStatus.NO_POTENTIAL
            potensiText.contains("awas", ignoreCase = true) -> TsunamiStatus.POTENTIAL_WARNING
            potensiText.contains("siaga", ignoreCase = true) -> TsunamiStatus.POTENTIAL_ADVISORY
            potensiText.contains("waspada", ignoreCase = true) -> TsunamiStatus.POTENTIAL_WATCH
            potensiText.contains("berpotensi tsunami", ignoreCase = true) -> TsunamiStatus.POTENTIAL_WARNING
            potensiText.isNotBlank() -> TsunamiStatus.NO_POTENTIAL
            else -> TsunamiStatus.UNKNOWN_OR_UNSPECIFIED
        }

        val shakemapUrl = if (!item.shakemap.isNullOrBlank()) {
            "https://data.bmkg.go.id/DataMKG/TEWS/${item.shakemap}"
        } else null

        // Generate stable unique ID based on time and coordinates
        val cleanTime = item.dateTime?.replace(Regex("[^0-9]"), "") ?: "${timeMillis}"
        val id = "bmkg_${cleanTime}_${(lat * 100).toInt()}_${(lon * 100).toInt()}"

        return UnifiedEarthquake(
            id = id,
            source = EarthquakeSource.BMKG,
            magnitude = mag,
            magnitudeType = "SR",
            depthKm = depthKm,
            place = place,
            timeEpochMillis = timeMillis,
            formattedTimeWib = wibStr,
            formattedTimeUtc = utcStr,
            latitude = lat,
            longitude = lon,
            tsunamiStatus = tsunamiStatus,
            tsunamiDescription = if (potensiText.isNotBlank()) potensiText else tsunamiStatus.label,
            feltInfo = item.dirasakan,
            shakemapUrl = shakemapUrl,
            officialUrl = "https://www.bmkg.go.id",
            eventStatus = "confirmed",
            isAutoGempa = isAuto,
            rawCoordinatesString = "${item.lintang ?: lat}, ${item.bujur ?: lon}"
        )
    }

    private fun parseUsgsFeature(feature: UsgsFeature): UnifiedEarthquake? {
        val prop = feature.properties ?: return null
        val geom = feature.geometry ?: return null
        val coords = geom.coordinates ?: return null
        if (coords.size < 2) return null

        val lon = coords[0]
        val lat = coords[1]
        val depthKm = if (coords.size >= 3) coords[2] else 10.0
        val mag = prop.mag ?: return null
        val timeMillis = prop.time ?: System.currentTimeMillis()
        val id = "usgs_${feature.id ?: timeMillis}"

        val tsunamiInt = prop.tsunami ?: 0
        val tsunamiStatus = if (tsunamiInt == 1) TsunamiStatus.POTENTIAL_WARNING else TsunamiStatus.NO_POTENTIAL

        return UnifiedEarthquake(
            id = id,
            source = EarthquakeSource.USGS,
            magnitude = mag,
            magnitudeType = prop.magType?.uppercase() ?: "M",
            depthKm = depthKm,
            place = prop.place ?: "Global",
            timeEpochMillis = timeMillis,
            formattedTimeWib = DateTimeUtils.formatWib(timeMillis),
            formattedTimeUtc = DateTimeUtils.formatUtc(timeMillis),
            latitude = lat,
            longitude = lon,
            tsunamiStatus = tsunamiStatus,
            tsunamiDescription = if (tsunamiInt == 1) "Peringatan Tsunami USGS" else "Tidak Ada Peringatan Tsunami",
            feltInfo = if (prop.felt != null && prop.felt > 0) "${prop.felt} laporan dirasakan" else null,
            shakemapUrl = null,
            officialUrl = prop.url,
            eventStatus = prop.status ?: "automatic",
            isAutoGempa = false,
            rawCoordinatesString = "$lat, $lon"
        )
    }

    /**
     * Cross-source deduplication:
     * If two events from different sources occur within 5 minutes and 100 km of each other,
     * merge them into a single record prioritizing BMKG for Indonesian coordinates.
     */
    fun deduplicateEvents(events: List<UnifiedEarthquake>): List<UnifiedEarthquake> {
        val sorted = events.sortedByDescending { it.timeEpochMillis }
        val result = mutableListOf<UnifiedEarthquake>()

        for (candidate in sorted) {
            val duplicate = result.find { existing ->
                val timeDiff = abs(existing.timeEpochMillis - candidate.timeEpochMillis)
                val dist = GeoUtils.calculateDistanceKm(
                    existing.latitude, existing.longitude,
                    candidate.latitude, candidate.longitude
                )
                timeDiff <= 5 * 60 * 1000L && dist <= 100.0
            }

            if (duplicate == null) {
                result.add(candidate)
            } else {
                // If the candidate is from BMKG and existing is USGS, swap to prefer BMKG's data for Indonesia
                if (candidate.source == EarthquakeSource.BMKG && duplicate.source == EarthquakeSource.USGS) {
                    val index = result.indexOf(duplicate)
                    if (index != -1) {
                        result[index] = candidate.copy(
                            officialUrl = candidate.officialUrl ?: duplicate.officialUrl
                        )
                    }
                }
            }
        }
        return result
    }

    private suspend fun evaluateAndTriggerNotifications(earthquakes: List<UnifiedEarthquake>) {
        val pref = prefManager.getNotificationPreferences()
        if (!pref.enabled) return

        // Check quiet hours
        if (pref.quietHoursEnabled) {
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val inQuietHours = if (pref.quietHoursStartHour > pref.quietHoursEndHour) {
                currentHour >= pref.quietHoursStartHour || currentHour < pref.quietHoursEndHour
            } else {
                currentHour in pref.quietHoursStartHour until pref.quietHoursEndHour
            }
            if (inQuietHours) return
        }

        val userLoc = prefManager.getUserLocation()

        for (eq in earthquakes) {
            // Check if already notified
            if (notificationLogDao.hasBeenNotified(eq.id)) continue

            // Don't send notification for events older than 6 hours
            if (System.currentTimeMillis() - eq.timeEpochMillis > 6 * 60 * 60 * 1000L) {
                notificationLogDao.logNotification(
                    NotificationLogEntity(eq.id, System.currentTimeMillis(), eq.magnitude, eq.place)
                )
                continue
            }

            // Criteria check
            val dist = GeoUtils.calculateDistanceKm(userLoc.latitude, userLoc.longitude, eq.latitude, eq.longitude)
            val distanceMatch = pref.notifyAllRegions || dist <= pref.radiusKm
            val magMatch = eq.magnitude >= pref.minMagnitude
            val tsunamiMatch = !pref.onlyTsunamiAlerts || eq.tsunamiStatus.isWarning
            val feltMatch = !pref.onlyFeltEarthquakes || !eq.feltInfo.isNullOrBlank()

            if (distanceMatch && magMatch && tsunamiMatch && feltMatch) {
                val withDist = eq.copy(
                    distanceKmFromUser = dist,
                    compassDirectionFromUser = GeoUtils.getCompassDirectionId(
                        GeoUtils.calculateBearing(userLoc.latitude, userLoc.longitude, eq.latitude, eq.longitude)
                    )
                )
                EarthquakeNotificationHelper.showEarthquakeNotification(
                    context,
                    withDist,
                    soundEnabled = pref.soundEnabled,
                    vibrateEnabled = pref.vibrateEnabled
                )
            }

            // Mark as processed in notification log
            notificationLogDao.logNotification(
                NotificationLogEntity(eq.id, System.currentTimeMillis(), eq.magnitude, eq.place)
            )
        }
    }

    // Favorites
    suspend fun toggleFavorite(earthquakeId: String) = withContext(Dispatchers.IO) {
        if (favoriteDao.isFavorite(earthquakeId)) {
            favoriteDao.removeFavorite(earthquakeId)
        } else {
            favoriteDao.addFavorite(FavoriteEntity(earthquakeId))
        }
    }

    suspend fun isFavorite(earthquakeId: String): Boolean = withContext(Dispatchers.IO) {
        favoriteDao.isFavorite(earthquakeId)
    }

    // Checklist
    fun getChecklistItemsFlow(): Flow<List<ChecklistItemEntity>> = checklistDao.getAllChecklistItemsFlow()

    suspend fun updateChecklistItem(id: String, isChecked: Boolean) = withContext(Dispatchers.IO) {
        checklistDao.updateCheckedStatus(id, isChecked)
    }

    suspend fun initializeDefaultChecklistIfEmpty() = withContext(Dispatchers.IO) {
        if (checklistDao.getAllChecklistItems().isEmpty()) {
            val defaults = listOf(
                ChecklistItemEntity("doc_1", "Dokumen Penting & Fotokopi", "KTP, KK, Akta, Polis Asuransi, Surat Tanah dalam kantong kedap air", "Dokumen", false),
                ChecklistItemEntity("med_1", "Kotak P3K Lengkap", "Plester, kassa steril, cairan antiseptik, obat pribadi, parasetamol", "Kesehatan", false),
                ChecklistItemEntity("food_1", "Makanan Siap Saji Tahan Lama", "Biskuit gandum, kornet, abon, makanan kaleng untuk min. 3 hari", "Logistik", false),
                ChecklistItemEntity("water_1", "Air Bersih Minum", "Minimal 3 liter per orang untuk kebutuhan 72 jam pertama", "Logistik", false),
                ChecklistItemEntity("light_1", "Senter LED & Baterai Cadangan", "Penerangan darurat saat listrik padam total pasca gempa", "Alat Bantu", false),
                ChecklistItemEntity("radio_1", "Radio Portabel (Baterai / Engkol)", "Untuk memantau siaran informasi resmi darurat BMKG & BNPB", "Alat Bantu", false),
                ChecklistItemEntity("whistle_1", "Peluit Darurat", "Untuk memberi sinyal suara jika terjebak di bawah reruntuhan", "Alat Bantu", false),
                ChecklistItemEntity("mask_1", "Masker Debu & Helm/Topi Pelindung", "Melindungi pernapasan dan kepala dari debu reruntuhan", "Kesehatan", false),
                ChecklistItemEntity("power_1", "Powerbank Terisi Penuh & Kabel", "Menjaga daya baterai ponsel untuk komunikasi darurat", "Alat Bantu", false),
                ChecklistItemEntity("clothes_1", "Pakaian Ganti & Selimut Termal", "Pakaian hangat cadangan, jas hujan tipis, dan pakaian dalam", "Sandang", false),
                ChecklistItemEntity("cash_1", "Uang Tunai Pecahan Kecil", "ATM dan sistem digital berpotensi padam pasca bencana", "Dokumen", false),
                ChecklistItemEntity("hygiene_1", "Perlengkapan Sanitasi Pribadi", "Tisu basah, sabun cair kecil, pembalut, sikat gigi", "Kesehatan", false)
            )
            checklistDao.insertAll(defaults)
        }
    }
}
