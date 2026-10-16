package com.example.ui

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.GempakuApplication
import com.example.data.local.ChecklistItemEntity
import com.example.data.model.*
import com.example.data.repository.EarthquakeRepository
import com.example.service.EarthquakeMonitoringService
import com.example.service.EarthquakeNotificationHelper
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class ConnectionStatus(val label: String) {
    ONLINE_LIVE("LIVE • Terhubung BMKG & USGS"),
    OFFLINE_CACHED("OFFLINE • Menampilkan Data Tersimpan"),
    SYNCING("MEMPERBARUI DATA...")
}

enum class FeedFilter(val label: String) {
    ALL("Semua"),
    M5_PLUS("M ≥ 5.0"),
    FELT("Dirasakan"),
    INDONESIA("Indonesia"),
    GLOBAL("Dunia")
}

data class EarthquakeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val earthquakes: List<UnifiedEarthquake> = emptyList(),
    val autoGempa: UnifiedEarthquake? = null,
    val selectedEarthquake: UnifiedEarthquake? = null,
    val favoriteIds: Set<String> = emptySet(),
    val userLocation: UserLocation = UserLocation(-6.2088, 106.8456, "Jakarta (DKI Jakarta)", false),
    val notificationPref: EarthquakeNotificationPreference = EarthquakeNotificationPreference(),
    val lastUpdatedEpoch: Long = 0L,
    val connectionStatus: ConnectionStatus = ConnectionStatus.SYNCING,
    val selectedFilter: FeedFilter = FeedFilter.ALL,
    val searchQuery: String = "",
    val minMagFilter: Float = 0.0f,
    val maxDepthFilter: Float = 1000f,
    val checklistItems: List<ChecklistItemEntity> = emptyList(),
    val errorMessage: String? = null
)

class EarthquakeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as GempakuApplication
    private val repository: EarthquakeRepository = app.repository
    private val prefManager = app.preferenceManager

    private val _uiState = MutableStateFlow(EarthquakeUiState())
    val uiState: StateFlow<EarthquakeUiState> = _uiState.asStateFlow()

    init {
        // Collect earthquakes from DB flow
        viewModelScope.launch {
            repository.earthquakesFlow.collect { list ->
                val auto = list.find { it.isAutoGempa } ?: list.firstOrNull()
                _uiState.update { current ->
                    current.copy(
                        earthquakes = list,
                        autoGempa = auto,
                        selectedEarthquake = current.selectedEarthquake?.let { selected ->
                            list.find { it.id == selected.id } ?: selected
                        } ?: auto,
                        isLoading = false
                    )
                }
            }
        }

        // Collect favorite IDs
        viewModelScope.launch {
            repository.favoriteIdsFlow.collect { favs ->
                _uiState.update { it.copy(favoriteIds = favs.toSet()) }
            }
        }

        // Collect checklist items
        viewModelScope.launch {
            repository.getChecklistItemsFlow().collect { items ->
                _uiState.update { it.copy(checklistItems = items) }
            }
        }

        // Load preferences & initial user location
        _uiState.update {
            it.copy(
                userLocation = prefManager.getUserLocation(),
                notificationPref = prefManager.getNotificationPreferences(),
                lastUpdatedEpoch = prefManager.getLastSyncTimestamp()
            )
        }

        // Trigger initial data refresh
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, connectionStatus = ConnectionStatus.SYNCING, errorMessage = null) }
            val result = repository.refreshAllData()
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        connectionStatus = ConnectionStatus.ONLINE_LIVE,
                        lastUpdatedEpoch = System.currentTimeMillis()
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        connectionStatus = if (it.earthquakes.isNotEmpty()) ConnectionStatus.OFFLINE_CACHED else ConnectionStatus.ONLINE_LIVE,
                        errorMessage = result.exceptionOrNull()?.message ?: "Gagal memperbarui data gempa."
                    )
                }
            }
        }
    }

    fun selectEarthquake(earthquake: UnifiedEarthquake) {
        _uiState.update { it.copy(selectedEarthquake = earthquake) }
    }

    fun selectEarthquakeById(id: String) {
        viewModelScope.launch {
            val found = _uiState.value.earthquakes.find { it.id == id } ?: repository.getEarthquakeById(id)
            if (found != null) {
                _uiState.update { it.copy(selectedEarthquake = found) }
            }
        }
    }

    fun toggleFavorite(earthquakeId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(earthquakeId)
        }
    }

    fun setFilter(filter: FeedFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setMinMagFilter(minMag: Float) {
        _uiState.update { it.copy(minMagFilter = minMag) }
    }

    fun setUserLocation(location: UserLocation) {
        prefManager.saveUserLocation(location)
        _uiState.update { it.copy(userLocation = location) }
        // Re-evaluate earthquakes with new distances
        viewModelScope.launch {
            val list = _uiState.value.earthquakes
            val updated = list.map { eq ->
                val dist = com.example.util.GeoUtils.calculateDistanceKm(
                    location.latitude, location.longitude, eq.latitude, eq.longitude
                )
                val bearing = com.example.util.GeoUtils.calculateBearing(
                    location.latitude, location.longitude, eq.latitude, eq.longitude
                )
                eq.copy(
                    distanceKmFromUser = dist,
                    compassDirectionFromUser = com.example.util.GeoUtils.getCompassDirectionId(bearing)
                )
            }
            _uiState.update { it.copy(earthquakes = updated) }
        }
    }

    fun updateGpsLocation(lat: Double, lon: Double) {
        setUserLocation(UserLocation(lat, lon, "Lokasi GPS Terkini", isGps = true))
    }

    fun updateNotificationPref(pref: EarthquakeNotificationPreference) {
        prefManager.saveNotificationPreferences(pref)
        _uiState.update { it.copy(notificationPref = pref) }
        if (pref.enabled) {
            EarthquakeMonitoringService.startService(app)
        } else {
            EarthquakeMonitoringService.stopService(app)
        }
    }

    fun sendTestNotification() {
        EarthquakeNotificationHelper.showTestNotification(app)
    }

    fun toggleChecklistItem(id: String, isChecked: Boolean) {
        viewModelScope.launch {
            repository.updateChecklistItem(id, isChecked)
        }
    }

    fun getFilteredEarthquakes(): List<UnifiedEarthquake> {
        val state = _uiState.value
        return state.earthquakes.filter { eq ->
            // Category filter
            val matchesCategory = when (state.selectedFilter) {
                FeedFilter.ALL -> true
                FeedFilter.M5_PLUS -> eq.magnitude >= 5.0
                FeedFilter.FELT -> !eq.feltInfo.isNullOrBlank()
                FeedFilter.INDONESIA -> eq.source == EarthquakeSource.BMKG
                FeedFilter.GLOBAL -> eq.source == EarthquakeSource.USGS
            }

            // Min Mag filter
            val matchesMag = eq.magnitude >= state.minMagFilter

            // Search query
            val matchesSearch = if (state.searchQuery.isBlank()) true else {
                eq.place.contains(state.searchQuery, ignoreCase = true) ||
                eq.id.contains(state.searchQuery, ignoreCase = true) ||
                (eq.feltInfo?.contains(state.searchQuery, ignoreCase = true) == true)
            }

            matchesCategory && matchesMag && matchesSearch
        }
    }

    fun exportEarthquakesAsCsv(): String {
        val list = getFilteredEarthquakes()
        val sb = StringBuilder()
        sb.append("ID,Sumber,Magnitudo,Tipe,Kedalaman_KM,Lokasi,Waktu_WIB,Waktu_UTC,Latitude,Longitude,Tsunami_Status,Dirasakan\n")
        list.forEach { eq ->
            sb.append("\"${eq.id}\",")
            sb.append("\"${eq.source.name}\",")
            sb.append("${eq.magnitude},")
            sb.append("\"${eq.magnitudeType}\",")
            sb.append("${eq.depthKm},")
            sb.append("\"${eq.place.replace("\"", "'")}\",")
            sb.append("\"${eq.formattedTimeWib}\",")
            sb.append("\"${eq.formattedTimeUtc}\",")
            sb.append("${eq.latitude},")
            sb.append("${eq.longitude},")
            sb.append("\"${eq.tsunamiStatus.name}\",")
            sb.append("\"${(eq.feltInfo ?: "").replace("\"", "'")}\"\n")
        }
        return sb.toString()
    }

    fun exportEarthquakesAsJson(): String {
        val list = getFilteredEarthquakes()
        val jsonArray = JSONArray()
        list.forEach { eq ->
            val obj = JSONObject()
            obj.put("id", eq.id)
            obj.put("source", eq.source.name)
            obj.put("magnitude", eq.magnitude)
            obj.put("magnitudeType", eq.magnitudeType)
            obj.put("depthKm", eq.depthKm)
            obj.put("place", eq.place)
            obj.put("timeEpochMillis", eq.timeEpochMillis)
            obj.put("timeWib", eq.formattedTimeWib)
            obj.put("timeUtc", eq.formattedTimeUtc)
            obj.put("latitude", eq.latitude)
            obj.put("longitude", eq.longitude)
            obj.put("tsunamiStatus", eq.tsunamiStatus.name)
            obj.put("tsunamiDescription", eq.tsunamiDescription)
            obj.put("felt", eq.feltInfo)
            obj.put("shakemapUrl", eq.shakemapUrl)
            jsonArray.put(obj)
        }
        return jsonArray.toString(2)
    }
}
