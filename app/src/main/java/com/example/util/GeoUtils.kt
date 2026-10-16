package com.example.util

import java.util.Locale
import kotlin.math.*

object GeoUtils {

    /**
     * Calculates the great-circle distance between two points in kilometers
     * using the Haversine formula.
     */
    fun calculateDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Calculates the initial bearing from point 1 to point 2 in degrees (0-360)
     */
    fun calculateBearing(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val bearing = Math.toDegrees(atan2(y, x))
        return (bearing + 360.0) % 360.0
    }

    /**
     * Converts a bearing in degrees to an Indonesian compass direction
     */
    fun getCompassDirectionId(bearingDegrees: Double): String {
        val directions = arrayOf(
            "Utara", "Timur Laut", "Timur", "Tenggara",
            "Selatan", "Barat Daya", "Barat", "Barat Laut"
        )
        val index = ((bearingDegrees + 22.5) / 45.0).toInt() % 8
        return directions[index]
    }

    fun formatDistance(km: Double): String {
        return if (km < 10) {
            String.format(Locale.getDefault(), "%.1f km", km)
        } else {
            String.format(Locale.getDefault(), "%,.0f km", km)
        }
    }
}
