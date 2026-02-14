package com.antoniegil.astronia.util

import java.text.SimpleDateFormat
import java.util.*

fun Long.formatDateTime(): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(this))
}

fun Long.formatTimestamp(): String {
    return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(this))
}

fun generateTimestampFilename(prefix: String, extension: String): String {
    return "${prefix}_${System.currentTimeMillis().formatTimestamp()}.$extension"
}

object PlayerConstants {
    const val MAX_CHANNEL_DISPLAY = 500
    const val MAX_HISTORY_SIZE = 50
    const val AUTO_HIDE_CONTROLS_DELAY_MS = 3000L
    const val M3U8_MAX_SIZE_BYTES = 10 * 1024 * 1024
    const val M3U8_CONNECTION_TIMEOUT_MS = 10000
    const val M3U8_READ_TIMEOUT_MS = 10000
}
