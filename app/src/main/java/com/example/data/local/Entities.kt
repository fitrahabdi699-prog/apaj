package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DepthCategory
import com.example.data.model.EarthquakeSource
import com.example.data.model.TsunamiStatus
import com.example.data.model.UnifiedEarthquake

@Entity(tableName = "earthquakes")
data class EarthquakeEntity(
    @PrimaryKey val id: String,
    val source: String,
    val magnitude: Double,
    val magnitudeType: String,
    val depthKm: Double,
    val place: String,
    val timeEpochMillis: Long,
    val formattedTimeWib: String,
    val formattedTimeUtc: String,
    val latitude: Double,
    val longitude: Double,
    val tsunamiStatus: String,
    val tsunamiDescription: String,
    val feltInfo: String?,
    val shakemapUrl: String?,
    val officialUrl: String?,
    val eventStatus: String,
    val isAutoGempa: Boolean,
    val rawCoordinatesString: String
) {
    fun toDomain(userLat: Double? = null, userLon: Double? = null): UnifiedEarthquake {
        val dist = if (userLat != null && userLon != null) {
            com.example.util.GeoUtils.calculateDistanceKm(userLat, userLon, latitude, longitude)
        } else null

        val dir = if (userLat != null && userLon != null) {
            val bearing = com.example.util.GeoUtils.calculateBearing(userLat, userLon, latitude, longitude)
            com.example.util.GeoUtils.getCompassDirectionId(bearing)
        } else null

        val parsedSource = try {
            EarthquakeSource.valueOf(source)
        } catch (_: Exception) {
            EarthquakeSource.BMKG
        }

        val parsedTsunami = try {
            TsunamiStatus.valueOf(tsunamiStatus)
        } catch (_: Exception) {
            TsunamiStatus.UNKNOWN_OR_UNSPECIFIED
        }

        return UnifiedEarthquake(
            id = id,
            source = parsedSource,
            magnitude = magnitude,
            magnitudeType = magnitudeType,
            depthKm = depthKm,
            place = place,
            timeEpochMillis = timeEpochMillis,
            formattedTimeWib = formattedTimeWib,
            formattedTimeUtc = formattedTimeUtc,
            latitude = latitude,
            longitude = longitude,
            tsunamiStatus = parsedTsunami,
            tsunamiDescription = tsunamiDescription,
            feltInfo = feltInfo,
            shakemapUrl = shakemapUrl,
            officialUrl = officialUrl,
            eventStatus = eventStatus,
            isAutoGempa = isAutoGempa,
            rawCoordinatesString = rawCoordinatesString,
            distanceKmFromUser = dist,
            compassDirectionFromUser = dir
        )
    }

    companion object {
        fun fromDomain(d: UnifiedEarthquake): EarthquakeEntity {
            return EarthquakeEntity(
                id = d.id,
                source = d.source.name,
                magnitude = d.magnitude,
                magnitudeType = d.magnitudeType,
                depthKm = d.depthKm,
                place = d.place,
                timeEpochMillis = d.timeEpochMillis,
                formattedTimeWib = d.formattedTimeWib,
                formattedTimeUtc = d.formattedTimeUtc,
                latitude = d.latitude,
                longitude = d.longitude,
                tsunamiStatus = d.tsunamiStatus.name,
                tsunamiDescription = d.tsunamiDescription,
                feltInfo = d.feltInfo,
                shakemapUrl = d.shakemapUrl,
                officialUrl = d.officialUrl,
                eventStatus = d.eventStatus,
                isAutoGempa = d.isAutoGempa,
                rawCoordinatesString = d.rawCoordinatesString
            )
        }
    }
}

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val earthquakeId: String,
    val savedAtEpochMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "notification_logs")
data class NotificationLogEntity(
    @PrimaryKey val earthquakeId: String,
    val sentAtEpochMillis: Long = System.currentTimeMillis(),
    val magnitude: Double,
    val place: String
)

@Entity(tableName = "checklist_items")
data class ChecklistItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,
    val isChecked: Boolean = false
)
