package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class EarthquakeSource(val displayName: String) {
    BMKG("BMKG (Indonesia)"),
    USGS("USGS (Global)")
}

enum class TsunamiStatus(val label: String, val isWarning: Boolean) {
    NO_POTENTIAL("Tidak Berpotensi Tsunami", false),
    POTENTIAL_WARNING("AWAS Tsunami (Evakuasi Segera)", true),
    POTENTIAL_ADVISORY("SIAGA Tsunami (Jauhi Pantai)", true),
    POTENTIAL_WATCH("WASPADA Tsunami (Pantau Arahan)", true),
    UNKNOWN_OR_UNSPECIFIED("Informasi Tidak Tersedia", false)
}

enum class DepthCategory(val label: String) {
    DANGKAL("Dangkal (<60 km)"),
    MENENGAH("Menengah (60-300 km)"),
    DALAM("Dalam (>300 km)")
}

// ---------------------------------------------------------------------------
// BMKG API Models
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class BmkgAutogempaResponse(
    @Json(name = "Infogempa") val infoGempa: BmkgAutogempaInfo?
)

@JsonClass(generateAdapter = true)
data class BmkgAutogempaInfo(
    @Json(name = "gempa") val gempa: BmkgGempaItem?
)

@JsonClass(generateAdapter = true)
data class BmkgListResponse(
    @Json(name = "Infogempa") val infoGempa: BmkgListInfo?
)

@JsonClass(generateAdapter = true)
data class BmkgListInfo(
    @Json(name = "gempa") val gempaList: List<BmkgGempaItem>?
)

@JsonClass(generateAdapter = true)
data class BmkgGempaItem(
    @Json(name = "Tanggal") val tanggal: String? = null,
    @Json(name = "Jam") val jam: String? = null,
    @Json(name = "DateTime") val dateTime: String? = null,
    @Json(name = "Coordinates") val coordinates: String? = null,
    @Json(name = "Lintang") val lintang: String? = null,
    @Json(name = "Bujur") val bujur: String? = null,
    @Json(name = "Magnitude") val magnitude: String? = null,
    @Json(name = "Kedalaman") val kedalaman: String? = null,
    @Json(name = "Wilayah") val wilayah: String? = null,
    @Json(name = "Potensi") val potensi: String? = null,
    @Json(name = "Dirasakan") val dirasakan: String? = null,
    @Json(name = "Shakemap") val shakemap: String? = null
)

// ---------------------------------------------------------------------------
// USGS API Models
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class UsgsGeoJsonResponse(
    @Json(name = "type") val type: String? = null,
    @Json(name = "metadata") val metadata: UsgsMetadata? = null,
    @Json(name = "features") val features: List<UsgsFeature>? = null
)

@JsonClass(generateAdapter = true)
data class UsgsMetadata(
    @Json(name = "generated") val generated: Long? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "status") val status: Int? = null,
    @Json(name = "count") val count: Int? = null
)

@JsonClass(generateAdapter = true)
data class UsgsFeature(
    @Json(name = "type") val type: String? = null,
    @Json(name = "properties") val properties: UsgsProperties? = null,
    @Json(name = "geometry") val geometry: UsgsGeometry? = null,
    @Json(name = "id") val id: String? = null
)

@JsonClass(generateAdapter = true)
data class UsgsProperties(
    @Json(name = "mag") val mag: Double? = null,
    @Json(name = "place") val place: String? = null,
    @Json(name = "time") val time: Long? = null,
    @Json(name = "updated") val updated: Long? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "detail") val detail: String? = null,
    @Json(name = "felt") val felt: Int? = null,
    @Json(name = "mmi") val mmi: Double? = null,
    @Json(name = "alert") val alert: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "tsunami") val tsunami: Int? = null,
    @Json(name = "magType") val magType: String? = null,
    @Json(name = "title") val title: String? = null
)

@JsonClass(generateAdapter = true)
data class UsgsGeometry(
    @Json(name = "type") val type: String? = null,
    // [longitude, latitude, depth]
    @Json(name = "coordinates") val coordinates: List<Double>? = null
)

// ---------------------------------------------------------------------------
// Unified Domain Model
// ---------------------------------------------------------------------------

data class UnifiedEarthquake(
    val id: String,
    val source: EarthquakeSource,
    val magnitude: Double,
    val magnitudeType: String,
    val depthKm: Double,
    val place: String,
    val timeEpochMillis: Long,
    val formattedTimeWib: String,
    val formattedTimeUtc: String,
    val latitude: Double,
    val longitude: Double,
    val tsunamiStatus: TsunamiStatus,
    val tsunamiDescription: String,
    val feltInfo: String? = null,
    val shakemapUrl: String? = null,
    val officialUrl: String? = null,
    val eventStatus: String = "confirmed",
    val isAutoGempa: Boolean = false,
    val rawCoordinatesString: String = "",
    val distanceKmFromUser: Double? = null,
    val compassDirectionFromUser: String? = null
) {
    val depthCategory: DepthCategory
        get() = when {
            depthKm < 60.0 -> DepthCategory.DANGKAL
            depthKm <= 300.0 -> DepthCategory.MENENGAH
            else -> DepthCategory.DALAM
        }
}

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val isGps: Boolean = true
)

data class EarthquakeNotificationPreference(
    val enabled: Boolean = true,
    val minMagnitude: Float = 5.0f,
    val radiusKm: Float = 500f,
    val notifyAllRegions: Boolean = true,
    val onlyTsunamiAlerts: Boolean = false,
    val onlyFeltEarthquakes: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 22,
    val quietHoursEndHour: Int = 6
)
