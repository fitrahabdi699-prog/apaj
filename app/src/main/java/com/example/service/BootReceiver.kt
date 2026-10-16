package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.PreferenceManager

class BootReceiver : BroadcastReceiver {
    constructor() : super()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefManager = PreferenceManager(context)
            if (prefManager.getNotificationPreferences().enabled) {
                EarthquakeMonitoringService.startService(context)
            }
        }
    }
}
