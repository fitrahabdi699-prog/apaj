package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.TsunamiStatus
import com.example.data.model.UnifiedEarthquake

object EarthquakeNotificationHelper {
    const val CHANNEL_EARTHQUAKE = "gempaku_earthquake_channel"
    const val CHANNEL_TSUNAMI = "gempaku_tsunami_channel"
    const val CHANNEL_SERVICE = "gempaku_service_channel"

    private const val NOTIFICATION_ID_BASE = 2000
    const val SERVICE_NOTIFICATION_ID = 1001

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            // 1. Earthquake Alerts Channel
            val eqChannel = NotificationChannel(
                CHANNEL_EARTHQUAKE,
                "Peringatan Gempa Terdeteksi",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi saat terjadi gempa bumi terbaru sesuai preferensi"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
                setSound(defaultSound, audioAttributes)
            }
            manager.createNotificationChannel(eqChannel)

            // 2. Tsunami Alerts Channel
            val tsunamiChannel = NotificationChannel(
                CHANNEL_TSUNAMI,
                "Peringatan Potensi Tsunami",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi darurat peringatan potensi tsunami dari otoritas resmi"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 300, 800, 300, 1000)
                setSound(defaultSound, audioAttributes)
            }
            manager.createNotificationChannel(tsunamiChannel)

            // 3. Background Monitoring Service Channel
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Status Layanan Pemantauan",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Status pemantauan gempa bumi di latar belakang"
                setShowBadge(false)
            }
            manager.createNotificationChannel(serviceChannel)
        }
    }

    fun showEarthquakeNotification(
        context: Context,
        earthquake: UnifiedEarthquake,
        soundEnabled: Boolean = true,
        vibrateEnabled: Boolean = true
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val isTsunami = earthquake.tsunamiStatus.isWarning
        val channelId = if (isTsunami) CHANNEL_TSUNAMI else CHANNEL_EARTHQUAKE

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_EARTHQUAKE_ID", earthquake.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            earthquake.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isTsunami) {
            "🚨 PERINGATAN TSUNAMI: Gempa M ${earthquake.magnitude}"
        } else {
            "⚠️ Gempa M ${earthquake.magnitude} - ${earthquake.place}"
        }

        val distText = earthquake.distanceKmFromUser?.let { " • Jarak: ${com.example.util.GeoUtils.formatDistance(it)}" } ?: ""
        val contentText = "Kedalaman: ${earthquake.depthKm.toInt()} km$distText"

        val bigText = buildString {
            append("Lokasi: ${earthquake.place}\n")
            append("Magnitudo: M ${earthquake.magnitude} (${earthquake.magnitudeType})\n")
            append("Kedalaman: ${earthquake.depthKm} km\n")
            append("Waktu: ${earthquake.formattedTimeWib}\n")
            if (earthquake.distanceKmFromUser != null) {
                append("Jarak dari Anda: ${com.example.util.GeoUtils.formatDistance(earthquake.distanceKmFromUser!!)}")
                if (!earthquake.compassDirectionFromUser.isNullOrBlank()) {
                    append(" arah ${earthquake.compassDirectionFromUser}\n")
                } else {
                    append("\n")
                }
            }
            append("Status: ${earthquake.tsunamiDescription}\n")
            if (!earthquake.feltInfo.isNullOrBlank()) {
                append("Dirasakan: ${earthquake.feltInfo}\n")
            }
            append("Sumber: ${earthquake.source.displayName}")
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (!soundEnabled) {
            builder.setSilent(true)
        }
        if (!vibrateEnabled) {
            builder.setVibrate(longArrayOf(0))
        }

        val notifId = NOTIFICATION_ID_BASE + (earthquake.id.hashCode() % 1000)
        manager.notify(notifId, builder.build())
    }

    fun showTestNotification(context: Context) {
        val testEarthquake = UnifiedEarthquake(
            id = "test_event_${System.currentTimeMillis()}",
            source = com.example.data.model.EarthquakeSource.BMKG,
            magnitude = 5.2,
            magnitudeType = "SR",
            depthKm = 10.0,
            place = "Pusat Uji Notifikasi GEMPAKU - Selat Sunda (SIMULASI)",
            timeEpochMillis = System.currentTimeMillis(),
            formattedTimeWib = com.example.util.DateTimeUtils.formatWib(System.currentTimeMillis()),
            formattedTimeUtc = com.example.util.DateTimeUtils.formatUtc(System.currentTimeMillis()),
            latitude = -6.1,
            longitude = 105.4,
            tsunamiStatus = TsunamiStatus.NO_POTENTIAL,
            tsunamiDescription = "Uji Coba Notifikasi Sistem: Tidak Berpotensi Tsunami",
            feltInfo = "II - III Anyer, Carita",
            distanceKmFromUser = 110.0,
            compassDirectionFromUser = "Barat"
        )
        showEarthquakeNotification(context, testEarthquake)
    }
}
