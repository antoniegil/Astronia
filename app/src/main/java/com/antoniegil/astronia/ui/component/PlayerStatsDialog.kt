package com.antoniegil.astronia.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.antoniegil.astronia.R
import com.antoniegil.astronia.player.Media3Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun PlayerStatsDialog(
    media3Player: Media3Player?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    var stats by remember { mutableStateOf<PlayerStats?>(null) }
    var lastCpuTime by remember { mutableLongStateOf(0L) }
    var lastCheckTime by remember { mutableLongStateOf(0L) }
    
    DisposableEffect(media3Player) {
        val job = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                media3Player?.exoPlayer?.let { player ->
                    val format = player.videoFormat
                    val bufferedMs = player.bufferedPosition - player.currentPosition
                    val bufferPercentage = if (player.duration > 0) {
                        (bufferedMs.toFloat() / player.duration * 100).coerceIn(0f, 100f)
                    } else {
                        0f
                    }
                    
                    val playbackState = when (player.playbackState) {
                        androidx.media3.common.Player.STATE_IDLE -> resources.getString(R.string.idle)
                        androidx.media3.common.Player.STATE_BUFFERING -> resources.getString(R.string.buffering)
                        androidx.media3.common.Player.STATE_READY -> if (player.isPlaying) resources.getString(R.string.playing) else resources.getString(R.string.paused)
                        androidx.media3.common.Player.STATE_ENDED -> resources.getString(R.string.ended)
                        else -> resources.getString(R.string.unknown)
                    }
                    
                    val decoderType = com.antoniegil.astronia.util.SettingsManager.getDecoderType(context)
                    val isHardwareDecoder = decoderType == 0
                    
                    val cpuUsage = try {
                        val pid = android.os.Process.myPid()
                        val stat = java.io.File("/proc/$pid/stat").readText().split(" ")
                        val utime = stat[13].toLong()
                        val stime = stat[14].toLong()
                        val currentCpuTime = utime + stime
                        val currentTime = System.currentTimeMillis()
                        
                        val usage = if (lastCpuTime > 0 && lastCheckTime > 0) {
                            val cpuDelta = currentCpuTime - lastCpuTime
                            val timeDelta = currentTime - lastCheckTime
                            if (timeDelta > 0) {
                                val cpuCount = Runtime.getRuntime().availableProcessors()
                                ((cpuDelta * 10f / timeDelta / cpuCount) * 100).toInt().coerceIn(0, 100)
                            } else 0
                        } else 0
                        
                        lastCpuTime = currentCpuTime
                        lastCheckTime = currentTime
                        usage
                    } catch (e: Exception) {
                        0
                    }
                    
                    stats = PlayerStats(
                        droppedFrames = player.videoDecoderCounters?.droppedBufferCount ?: 0,
                        totalFrames = player.videoDecoderCounters?.renderedOutputBufferCount ?: 0,
                        currentRes = "${format?.width ?: 0}x${format?.height ?: 0}@${format?.frameRate?.toInt() ?: 0}",
                        codec = format?.codecs ?: "N/A",
                        bufferHealth = bufferedMs / 1000f,
                        bufferPercentage = bufferPercentage,
                        playbackState = playbackState,
                        isHardwareDecoder = isHardwareDecoder,
                        cpuUsage = cpuUsage
                    )
                }
                delay(1000)
            }
        }
        
        onDispose {
            job.cancel()
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_stats)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                stats?.let { s ->
                    StatsRow(stringResource(R.string.state), s.playbackState)
                    StatsRow(stringResource(R.string.resolution), s.currentRes)
                    StatsRow(stringResource(R.string.frames), "${s.droppedFrames} / ${s.totalFrames}")
                    StatsRow(stringResource(R.string.codec), s.codec)
                    StatsRow(stringResource(R.string.decoder), if (s.isHardwareDecoder) stringResource(R.string.decoder_hardware) else stringResource(R.string.decoder_software))
                    StatsRow(stringResource(R.string.buffer_health), "%.2f s (%.1f%%)".format(s.bufferHealth, s.bufferPercentage))
                    StatsRow(stringResource(R.string.cpu_usage), "${s.cpuUsage}%")
                } ?: Text(stringResource(R.string.loading))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                stats?.let { s ->
                    val statsText = buildString {
                        appendLine("${resources.getString(R.string.state)}: ${s.playbackState}")
                        appendLine("${resources.getString(R.string.resolution)}: ${s.currentRes}")
                        appendLine("${resources.getString(R.string.frames)}: ${s.droppedFrames} / ${s.totalFrames}")
                        appendLine("${resources.getString(R.string.codec)}: ${s.codec}")
                        appendLine("${resources.getString(R.string.decoder)}: ${if (s.isHardwareDecoder) resources.getString(R.string.decoder_hardware) else resources.getString(R.string.decoder_software)}")
                        appendLine("${resources.getString(R.string.buffer_health)}: ${"%.2f s (%.1f%%)".format(s.bufferHealth, s.bufferPercentage)}")
                        appendLine("${resources.getString(R.string.cpu_usage)}: ${s.cpuUsage}%")
                    }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(resources.getString(R.string.clipboard_label), statsText))
                }
            }) {
                Text(stringResource(R.string.copy))
            }
        }
    )
}

@Composable
private fun StatsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}

private data class PlayerStats(
    val droppedFrames: Int,
    val totalFrames: Int,
    val currentRes: String,
    val codec: String,
    val bufferHealth: Float,
    val bufferPercentage: Float,
    val playbackState: String,
    val isHardwareDecoder: Boolean,
    val cpuUsage: Int
)
