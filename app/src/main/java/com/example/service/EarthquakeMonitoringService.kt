package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.repository.EarthquakeRepository
import kotlinx.coroutines.*

class EarthquakeMonitoringService : Service() {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var repository: EarthquakeRepository

    override fun onCreate() {
        super.onCreate()
        repository = EarthquakeRepository(applicationContext)
        EarthquakeNotificationHelper.createNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, EarthquakeNotificationHelper.CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("GEMPAKU Siaga Gempa")
            .setContentText("Pemantauan real-time BMKG & USGS aktif di latar belakang")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(EarthquakeNotificationHelper.SERVICE_NOTIFICATION_ID, notification)

        // Launch periodic polling loop (every 60 seconds)
        serviceScope.launch {
            while (isActive) {
                try {
                    repository.refreshAllData()
                } catch (_: Exception) {}
                delay(60_000L) // 1 minute interval to respect API limits
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, EarthquakeMonitoringService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, EarthquakeMonitoringService::class.java)
            context.stopService(intent)
        }
    }
}
