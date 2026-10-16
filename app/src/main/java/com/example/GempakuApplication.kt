package com.example

import android.app.Application
import com.example.data.local.PreferenceManager
import com.example.data.repository.EarthquakeRepository
import com.example.service.EarthquakeMonitoringService
import com.example.service.EarthquakeNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GempakuApplication : Application() {
    lateinit var repository: EarthquakeRepository
    lateinit var preferenceManager: PreferenceManager

    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
        repository = EarthquakeRepository(this, prefManager = preferenceManager)

        EarthquakeNotificationHelper.createNotificationChannels(this)

        CoroutineScope(Dispatchers.IO).launch {
            repository.initializeDefaultChecklistIfEmpty()
            if (preferenceManager.getNotificationPreferences().enabled) {
                EarthquakeMonitoringService.startService(this@GempakuApplication)
            }
        }
    }
}
