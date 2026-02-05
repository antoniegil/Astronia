package com.antoniegil.astronia.util

import java.text.SimpleDateFormat
import java.util.*

fun Long.formatDateTime(): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(this))
}

fun Long.formatDuration(): String {
    val seconds = (this / 1000) % 60
    val minutes = (this / (1000 * 60)) % 60
    val hours = this / (1000 * 60 * 60)
    
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

object PlayerConstants {
    const val MAX_CHANNEL_DISPLAY = 500
    const val MAX_HISTORY_SIZE = 50
    const val AUTO_HIDE_CONTROLS_DELAY_MS = 3000L
    const val WATCH_TIME_UPDATE_INTERVAL_MS = 500L
    const val PING_TIMEOUT_MS = 2000
    const val M3U8_MAX_SIZE_BYTES = 10 * 1024 * 1024
    const val M3U8_CONNECTION_TIMEOUT_MS = 10000
    const val M3U8_READ_TIMEOUT_MS = 10000
}
