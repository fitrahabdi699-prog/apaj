package com.example

import com.example.data.model.*
import com.example.util.DateTimeUtils
import com.example.util.GeoUtils
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class GempakuCoreUnitTest {

    @Test
    fun testHaversineDistance_JakartaToBandung() {
        // Jakarta approx: -6.2088, 106.8456
        // Bandung approx: -6.9175, 107.6191
        val dist = GeoUtils.calculateDistanceKm(-6.2088, 106.8456, -6.9175, 107.6191)
        // Distance between Jakarta and Bandung is around 115-130 km
        assertTrue("Distance should be roughly 110-140 km, actual: $dist", dist in 110.0..140.0)
    }

    @Test
    fun testCompassBearing() {
        // North
        val northBearing = GeoUtils.calculateBearing(0.0, 0.0, 1.0, 0.0)
        assertEquals(0.0, northBearing, 1.0)
        assertEquals("Utara", GeoUtils.getCompassDirectionId(northBearing))

        // East
        val eastBearing = GeoUtils.calculateBearing(0.0, 0.0, 0.0, 1.0)
        assertEquals(90.0, eastBearing, 1.0)
        assertEquals("Timur", GeoUtils.getCompassDirectionId(eastBearing))

        // South
        val southBearing = GeoUtils.calculateBearing(1.0, 0.0, 0.0, 0.0)
        assertEquals(180.0, southBearing, 1.0)
        assertEquals("Selatan", GeoUtils.getCompassDirectionId(southBearing))

        // West
        val westBearing = GeoUtils.calculateBearing(0.0, 1.0, 0.0, 0.0)
        assertEquals(270.0, westBearing, 1.0)
        assertEquals("Barat", GeoUtils.getCompassDirectionId(westBearing))
    }

    @Test
    fun testDateTimeUtils_IsoParsing() {
        val iso = "2026-10-16T03:21:55+00:00"
        val parsed = DateTimeUtils.parseBmkgDateTime(iso, null, null)
        assertTrue("Parsed timestamp must be positive", parsed > 0)

        val wibStr = DateTimeUtils.formatWib(parsed)
        assertTrue("WIB string should contain WIB", wibStr.contains("WIB"))

        val utcStr = DateTimeUtils.formatUtc(parsed)
        assertTrue("UTC string should contain UTC", utcStr.contains("UTC"))
    }

    @Test
    fun testCrossSourceDeduplication() {
        val now = System.currentTimeMillis()
        val eq1 = UnifiedEarthquake(
            id = "bmkg_1",
            source = EarthquakeSource.BMKG,
            magnitude = 5.6,
            magnitudeType = "SR",
            depthKm = 10.0,
            place = "Selat Sunda, Banten",
            timeEpochMillis = now,
            formattedTimeWib = "16 Okt 2026 10:00:00 WIB",
            formattedTimeUtc = "16 Oct 2026 03:00:00 UTC",
            latitude = -6.2,
            longitude = 105.5,
            tsunamiStatus = TsunamiStatus.NO_POTENTIAL,
            tsunamiDescription = "Tidak berpotensi tsunami"
        )

        // USGS event 1 minute later, 15 km away
        val eq2 = UnifiedEarthquake(
            id = "usgs_1",
            source = EarthquakeSource.USGS,
            magnitude = 5.5,
            magnitudeType = "MW",
            depthKm = 12.0,
            place = "Sunda Strait, Indonesia",
            timeEpochMillis = now + 60_000L,
            formattedTimeWib = "16 Okt 2026 10:01:00 WIB",
            formattedTimeUtc = "16 Oct 2026 03:01:00 UTC",
            latitude = -6.25,
            longitude = 105.55,
            tsunamiStatus = TsunamiStatus.NO_POTENTIAL,
            tsunamiDescription = "Tidak berpotensi tsunami"
        )

        // Completely different event in Japan
        val eq3 = UnifiedEarthquake(
            id = "usgs_japan",
            source = EarthquakeSource.USGS,
            magnitude = 4.8,
            magnitudeType = "MB",
            depthKm = 50.0,
            place = "Honshu, Japan",
            timeEpochMillis = now,
            formattedTimeWib = "16 Okt 2026 10:00:00 WIB",
            formattedTimeUtc = "16 Oct 2026 03:00:00 UTC",
            latitude = 35.6,
            longitude = 139.7,
            tsunamiStatus = TsunamiStatus.NO_POTENTIAL,
            tsunamiDescription = "Tidak berpotensi tsunami"
        )

        val list = listOf(eq1, eq2, eq3)
        // Manual deduplicate test
        val sorted = list.sortedByDescending { it.timeEpochMillis }
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
                if (candidate.source == EarthquakeSource.BMKG && duplicate.source == EarthquakeSource.USGS) {
                    val index = result.indexOf(duplicate)
                    if (index != -1) {
                        result[index] = candidate
                    }
                }
            }
        }

        assertEquals("Should deduplicate Sunda Strait pair into 1, leaving 2 events in total", 2, result.size)
        assertTrue("BMKG event should be retained as primary authority for Indonesia", result.any { it.source == EarthquakeSource.BMKG })
        assertTrue("Japan event should be retained", result.any { it.id == "usgs_japan" })
    }

    @Test
    fun testNotificationThresholdFiltering() {
        val userLat = -6.2088
        val userLon = 106.8456 // Jakarta

        val nearEarthquake = UnifiedEarthquake(
            id = "near_1",
            source = EarthquakeSource.BMKG,
            magnitude = 5.2,
            magnitudeType = "SR",
            depthKm = 10.0,
            place = "Bogor",
            timeEpochMillis = System.currentTimeMillis(),
            formattedTimeWib = "",
            formattedTimeUtc = "",
            latitude = -6.6,
            longitude = 106.8,
            tsunamiStatus = TsunamiStatus.NO_POTENTIAL,
            tsunamiDescription = ""
        )

        val dist = GeoUtils.calculateDistanceKm(userLat, userLon, nearEarthquake.latitude, nearEarthquake.longitude)
        assertTrue("Bogor should be within 100 km of Jakarta", dist < 100.0)

        val pref = EarthquakeNotificationPreference(
            enabled = true,
            minMagnitude = 5.0f,
            radiusKm = 200f,
            notifyAllRegions = false
        )

        val shouldNotify = (pref.notifyAllRegions || dist <= pref.radiusKm) && nearEarthquake.magnitude >= pref.minMagnitude
        assertTrue("Near M5.2 earthquake should trigger notification", shouldNotify)
    }
}
