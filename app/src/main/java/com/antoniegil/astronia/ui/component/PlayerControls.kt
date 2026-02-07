package com.antoniegil.astronia.ui.component

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.net.toUri
import android.os.PowerManager
import android.provider.Settings
import android.util.Rational
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.antoniegil.astronia.player.Media3Player
import com.antoniegil.astronia.util.WatchTimeTracker
import kotlinx.coroutines.delay
import com.antoniegil.astronia.R


@Composable
fun PlayerControlsOverlay(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    isFullscreen: Boolean,
    enablePip: Boolean,
    isBuffering: Boolean,
    activity: Activity?,
    media3Player: Media3Player?,
    watchTimeTracker: WatchTimeTracker,
    currentCycleDuration: Float,
    onCycleDurationChange: (Float) -> Unit,
    onPlayPauseClick: () -> Unit,
    onBackClick: () -> Unit,
    onFullscreenClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSeek: (Long) -> Unit
) {
    var currentPosition by remember { mutableLongStateOf(media3Player?.currentPosition ?: 0L) }
    var bufferedPosition by remember { mutableLongStateOf(media3Player?.bufferedPosition ?: 0L) }
    var duration by remember { mutableLongStateOf(media3Player?.duration ?: 0L) }
    
    var estimatedProgress by rememberSaveable { mutableFloatStateOf(0f) }
    var localCycleDuration by remember { mutableFloatStateOf(currentCycleDuration) }
    
    LaunchedEffect(currentCycleDuration) {
        localCycleDuration = currentCycleDuration
    }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentPosition = media3Player?.currentPosition ?: 0L
            bufferedPosition = media3Player?.bufferedPosition ?: 0L
            duration = media3Player?.duration ?: 0L
            
            watchTimeTracker.update()
            val watchSeconds = watchTimeTracker.getAccumulatedTime() / 1000f
            val baseProgress = (watchSeconds / localCycleDuration).coerceIn(0f, 0.95f)
            
            if (baseProgress >= 0.95f) {
                val bufferedSeconds = ((bufferedPosition - currentPosition) / 1000f).coerceAtLeast(0.5f)
                val newCycleDuration = localCycleDuration + bufferedSeconds
                val backtrackAmount = (bufferedSeconds / newCycleDuration * 0.2f).coerceIn(0.05f, 0.2f)
                estimatedProgress = (0.95f - backtrackAmount).coerceIn(0.75f, 0.9f)
                localCycleDuration = newCycleDuration
                onCycleDurationChange(newCycleDuration)
            } else {
                estimatedProgress = baseProgress
            }
            
            delay(100)
        }
    }
    
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.player_settings),
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onPlayPauseClick) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) stringResource(R.string.paused) else stringResource(
                            R.string.play
                        ),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                ProgressBar(
                    modifier = Modifier.weight(1f),
                    isBuffering = isBuffering,
                    estimatedProgress = estimatedProgress
                )

                Spacer(modifier = Modifier.width(8.dp))

                if (enablePip && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    IconButton(
                        onClick = {
                            activity?.let { act ->
                                if (act.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                                    val aspectRatio = Rational(16, 9)
                                    val builder = PictureInPictureParams.Builder()
                                        .setAspectRatio(aspectRatio)
                                    act.enterPictureInPictureMode(builder.build())
                                }
                            }
                        },
                        modifier = Modifier.offset(x = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPicture,
                            contentDescription = stringResource(R.string.pip),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onFullscreenClick,
                    modifier = Modifier.offset(x = if (enablePip && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) (-2).dp else 0.dp)
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullscreen) stringResource(R.string.exit_fullscreen) else stringResource(
                            R.string.fullscreen
                        ),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressBar(
    modifier: Modifier = Modifier,
    isBuffering: Boolean,
    estimatedProgress: Float
) {
    Column(modifier = modifier) {
        when {
            isBuffering -> UnifiedProgressBar(mode = ProgressBarMode.Indeterminate)
            else -> UnifiedProgressBar(
                mode = ProgressBarMode.Estimated(estimatedProgress)
            )
        }
    }
}

private sealed class ProgressBarMode {
    object Indeterminate : ProgressBarMode()
    data class Determinate(val currentPosition: Long, val duration: Long) : ProgressBarMode()
    data class Estimated(val progress: Float) : ProgressBarMode()
}

@Composable
private fun UnifiedProgressBar(
    mode: ProgressBarMode,
    modifier: Modifier = Modifier,
    onSeek: ((Long) -> Unit)? = null
) {
    when (mode) {
        is ProgressBarMode.Indeterminate -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(6.dp)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
        is ProgressBarMode.Determinate -> {
            val progress = if (mode.duration > 0) {
                (mode.currentPosition.toFloat() / mode.duration.toFloat()).coerceIn(0f, 1f)
            } else 0f
            
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .pointerInput(mode.duration) {
                        detectTapGestures { offset ->
                            if (mode.duration > 0 && onSeek != null) {
                                val clickProgress = (offset.x / size.width).coerceIn(0f, 1f)
                                val newPosition = (mode.duration * clickProgress).toLong()
                                onSeek(newPosition)
                            }
                        }
                    }
                    .pointerInput(mode.duration) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            if (mode.duration > 0 && onSeek != null) {
                                val dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                                val newPosition = (mode.duration * dragProgress).toLong()
                                onSeek(newPosition)
                            }
                        }
                    }
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
        is ProgressBarMode.Estimated -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(6.dp)
            ) {
                LinearProgressIndicator(
                    progress = { mode.progress.coerceIn(0f, 0.95f) },
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@SuppressLint("BatteryLife")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettingsBottomSheet(
    enablePip: Boolean,
    backgroundPlay: Boolean,
    aspectRatio: Int,
    mirrorFlip: Boolean,
    onEnablePipChange: (Boolean) -> Unit,
    onBackgroundPlayChange: (Boolean) -> Unit,
    onAspectRatioChange: (Int) -> Unit,
    onMirrorFlipChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val isPipSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    
    var isBatteryOptimized by remember {
        mutableStateOf(!pm.isIgnoringBatteryOptimizations(context.packageName))
    }
    
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = "package:${context.packageName}".toUri()
    }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isBatteryOptimized = !pm.isIgnoringBatteryOptimizations(context.packageName)
    }
    
    val backgroundPlayInteractionSource = remember { MutableInteractionSource() }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetMaxWidth = 640.dp,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    0 to "16:9",
                    1 to "4:3",
                    2 to R.string.aspect_ratio_fill,
                    3 to R.string.aspect_ratio_original
                ).forEach { (value, label) ->
                    FilterChip(
                        selected = aspectRatio == value,
                        onClick = { onAspectRatioChange(value) },
                        label = { 
                            Text(
                                if (label is Int) stringResource(label) else label as String
                            ) 
                        }
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            if (isPipSupported) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPicture,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 12.dp).size(24.dp)
                        )
                        Text(
                            text = stringResource(R.string.pip),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    val thumbContent: (@Composable () -> Unit)? = if (enablePip) {
                        { Icon(Icons.Outlined.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                    } else null
                    Switch(
                        checked = enablePip,
                        onCheckedChange = onEnablePipChange,
                        thumbContent = thumbContent
                    )
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .then(
                        if (isBatteryOptimized) {
                            Modifier.clickable(
                                indication = null,
                                interactionSource = backgroundPlayInteractionSource
                            ) {
                                launcher.launch(intent)
                            }
                        } else {
                            Modifier
                        }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 12.dp).size(24.dp),
                        tint = if (isBatteryOptimized) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = stringResource(R.string.background_play),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isBatteryOptimized) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                val thumbContent: (@Composable () -> Unit)? = if (backgroundPlay) {
                    { Icon(Icons.Outlined.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                } else null
                Switch(
                    checked = backgroundPlay,
                    onCheckedChange = if (isBatteryOptimized) null else onBackgroundPlayChange,
                    enabled = !isBatteryOptimized,
                    thumbContent = thumbContent
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Flip,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 12.dp).size(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.mirror_flip),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                val thumbContent: (@Composable () -> Unit)? = if (mirrorFlip) {
                    { Icon(Icons.Outlined.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                } else null
                Switch(
                    checked = mirrorFlip,
                    onCheckedChange = onMirrorFlipChange,
                    thumbContent = thumbContent
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
