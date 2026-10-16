package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.EarthquakeNotificationPreference
import com.example.data.model.UserLocation
import com.example.util.IndonesianCities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("gempaku_user_prefs", Context.MODE_PRIVATE)

    private val _notificationPrefsFlow = MutableStateFlow(getNotificationPreferences())
    val notificationPrefsFlow: StateFlow<EarthquakeNotificationPreference> = _notificationPrefsFlow.asStateFlow()

    private val _userLocationFlow = MutableStateFlow(getUserLocation())
    val userLocationFlow: StateFlow<UserLocation> = _userLocationFlow.asStateFlow()

    fun getNotificationPreferences(): EarthquakeNotificationPreference {
        return EarthquakeNotificationPreference(
            enabled = prefs.getBoolean("notif_enabled", true),
            minMagnitude = prefs.getFloat("notif_min_mag", 5.0f),
            radiusKm = prefs.getFloat("notif_radius_km", 500f),
            notifyAllRegions = prefs.getBoolean("notif_all_regions", true),
            onlyTsunamiAlerts = prefs.getBoolean("notif_only_tsunami", false),
            onlyFeltEarthquakes = prefs.getBoolean("notif_only_felt", false),
            soundEnabled = prefs.getBoolean("notif_sound", true),
            vibrateEnabled = prefs.getBoolean("notif_vibrate", true),
            quietHoursEnabled = prefs.getBoolean("notif_quiet_hours", false),
            quietHoursStartHour = prefs.getInt("notif_quiet_start", 22),
            quietHoursEndHour = prefs.getInt("notif_quiet_end", 6)
        )
    }

    fun saveNotificationPreferences(pref: EarthquakeNotificationPreference) {
        prefs.edit()
            .putBoolean("notif_enabled", pref.enabled)
            .putFloat("notif_min_mag", pref.minMagnitude)
            .putFloat("notif_radius_km", pref.radiusKm)
            .putBoolean("notif_all_regions", pref.notifyAllRegions)
            .putBoolean("notif_only_tsunami", pref.onlyTsunamiAlerts)
            .putBoolean("notif_only_felt", pref.onlyFeltEarthquakes)
            .putBoolean("notif_sound", pref.soundEnabled)
            .putBoolean("notif_vibrate", pref.vibrateEnabled)
            .putBoolean("notif_quiet_hours", pref.quietHoursEnabled)
            .putInt("notif_quiet_start", pref.quietHoursStartHour)
            .putInt("notif_quiet_end", pref.quietHoursEndHour)
            .apply()
        _notificationPrefsFlow.value = pref
    }

    fun getUserLocation(): UserLocation {
        val isGps = prefs.getBoolean("loc_is_gps", true)
        val lat = prefs.getFloat("loc_lat", -6.2088f).toDouble()
        val lon = prefs.getFloat("loc_lon", 106.8456f).toDouble()
        val name = prefs.getString("loc_name", "Jakarta (DKI Jakarta)") ?: "Jakarta (DKI Jakarta)"
        return UserLocation(lat, lon, name, isGps)
    }

    fun saveUserLocation(location: UserLocation) {
        prefs.edit()
            .putBoolean("loc_is_gps", location.isGps)
            .putFloat("loc_lat", location.latitude.toFloat())
            .putFloat("loc_lon", location.longitude.toFloat())
            .putString("loc_name", location.locationName)
            .apply()
        _userLocationFlow.value = location
    }

    fun isDarkThemeEnabled(): Boolean {
        return prefs.getBoolean("dark_theme", true)
    }

    fun setDarkTheme(enabled: Boolean) {
        prefs.edit().putBoolean("dark_theme", enabled).apply()
    }

    fun getLastSyncTimestamp(): Long {
        return prefs.getLong("last_sync_time", 0L)
    }

    fun saveLastSyncTimestamp(time: Long) {
        prefs.edit().putLong("last_sync_time", time).apply()
    }
}
