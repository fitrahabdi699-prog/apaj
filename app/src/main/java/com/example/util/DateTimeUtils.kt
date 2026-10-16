package com.example.util

import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtils {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val wibOutputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm:ss 'WIB'", Locale("id", "ID")).apply {
        timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    }

    private val utcOutputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm:ss 'UTC'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun parseBmkgDateTime(dateTimeStr: String?, tanggal: String?, jam: String?): Long {
        if (!dateTimeStr.isNullOrBlank()) {
            try {
                // Try ISO 8601 e.g. 2026-10-16T03:21:55+00:00
                val parsed = isoFormat.parse(dateTimeStr)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {}
        }

        // Fallback parse from Tanggal & Jam e.g. "16 Okt 2026" "10:21:55 WIB"
        if (!tanggal.isNullOrBlank() && !jam.isNullOrBlank()) {
            try {
                val cleanJam = jam.replace(" WIB", "").trim()
                val fullStr = "$tanggal $cleanJam"
                val df = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale("id", "ID")).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Jakarta")
                }
                val date = df.parse(fullStr)
                if (date != null) return date.time
            } catch (_: Exception) {}
        }

        return System.currentTimeMillis()
    }

    fun formatWib(epochMillis: Long): String {
        return wibOutputFormat.format(Date(epochMillis))
    }

    fun formatUtc(epochMillis: Long): String {
        return utcOutputFormat.format(Date(epochMillis))
    }

    fun formatRelativeTime(epochMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - epochMillis
        if (diff < 0) return "Baru saja"

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Baru saja"
            minutes < 60 -> "$minutes menit lalu"
            hours < 24 -> "$hours jam lalu"
            days < 30 -> "$days hari lalu"
            else -> formatWib(epochMillis)
        }
    }
}
