package com.antoniegil.astronia.ui.component

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs

data class GestureControlState(
    val currentVolume: Int,
    val currentBrightness: Float,
    val showVolumeIndicator: Boolean,
    val showBrightnessIndicator: Boolean,
    val volumeIndicatorValue: Int,
    val brightnessIndicatorValue: Int
)

@Composable
fun rememberGestureControlState(
    context: Context,
    activity: Activity?
): MutableState<GestureControlState> {
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    
    return remember {
        mutableStateOf(
            GestureControlState(
                currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
                currentBrightness = activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0 } ?: 0.5f,
                showVolumeIndicator = false,
                showBrightnessIndicator = false,
                volumeIndicatorValue = 0,
                brightnessIndicatorValue = 0
            )
        )
    }
}

fun Modifier.gestureControlModifier(
    context: Context,
    activity: Activity?,
    gestureState: MutableState<GestureControlState>,
    onGestureStateChange: (GestureControlState) -> Unit
): Modifier {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    
    return this.pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
            change.consume()
            val screenWidth = size.width
            val isLeftSide = change.position.x < screenWidth / 2
            
            if (isLeftSide) {
                val volumeChange = (-dragAmount.y / size.height * maxVolume).toInt()
                if (abs(volumeChange) > 0) {
                    val newVolume = (gestureState.value.currentVolume + volumeChange).coerceIn(0, maxVolume)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                    onGestureStateChange(
                        gestureState.value.copy(
                            currentVolume = newVolume,
                            volumeIndicatorValue = (newVolume * 100 / maxVolume),
                            showVolumeIndicator = true,
                            showBrightnessIndicator = false
                        )
                    )
                }
            } else {
                val brightnessChange = -dragAmount.y / size.height
                if (abs(brightnessChange) > 0.01f) {
                    val newBrightness = (gestureState.value.currentBrightness + brightnessChange).coerceIn(0f, 1f)
                    activity?.window?.attributes = activity.window.attributes.apply {
                        screenBrightness = newBrightness
                    }
                    onGestureStateChange(
                        gestureState.value.copy(
                            currentBrightness = newBrightness,
                            brightnessIndicatorValue = (newBrightness * 100).toInt(),
                            showBrightnessIndicator = true,
                            showVolumeIndicator = false
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun GestureControlEffects(
    gestureState: MutableState<GestureControlState>,
    onGestureStateChange: (GestureControlState) -> Unit
) {
    val volumeTimestamp = remember { mutableLongStateOf(0L) }
    val brightnessTimestamp = remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(gestureState.value.showVolumeIndicator) {
        if (gestureState.value.showVolumeIndicator) {
            val currentTime = System.currentTimeMillis()
            volumeTimestamp.longValue = currentTime
            delay(1000)
            if (volumeTimestamp.longValue == currentTime) {
                onGestureStateChange(gestureState.value.copy(showVolumeIndicator = false))
            }
        }
    }
    
    LaunchedEffect(gestureState.value.showBrightnessIndicator) {
        if (gestureState.value.showBrightnessIndicator) {
            val currentTime = System.currentTimeMillis()
            brightnessTimestamp.longValue = currentTime
            delay(1000)
            if (brightnessTimestamp.longValue == currentTime) {
                onGestureStateChange(gestureState.value.copy(showBrightnessIndicator = false))
            }
        }
    }
}

@Composable
fun VolumeIndicator(
    show: Boolean,
    value: Int,
    modifier: Modifier = Modifier
) {
    if (show) {
        Box(
            modifier = modifier
                .background(
                    Color.Black.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Volume",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "$value%",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BrightnessIndicator(
    show: Boolean,
    value: Int,
    modifier: Modifier = Modifier
) {
    if (show) {
        Box(
            modifier = modifier
                .background(
                    Color.Black.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Brightness6,
                    contentDescription = "Brightness",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "$value%",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
