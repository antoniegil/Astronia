package com.antoniegil.astronia.ui.page

import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antoniegil.astronia.player.PlaybackService
import com.antoniegil.astronia.util.PlayerConstants
import com.antoniegil.astronia.ui.component.*
import com.antoniegil.astronia.ui.theme.LegacyThemeBackground
import com.antoniegil.astronia.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerPage(
    url: String,
    initialChannelUrl: String? = null,
    initialVideoTitle: String? = null,
    initialChannelId: String? = null,
    onBack: () -> Unit = {},
    viewModel: PlayerViewModel = viewModel(key = url)
) {
    PlayerPageContent(
        url = url,
        initialChannelUrl = initialChannelUrl,
        initialVideoTitle = initialVideoTitle,
        initialChannelId = initialChannelId,
        onBack = onBack,
        viewModel = viewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerPageContent(
    url: String,
    initialChannelUrl: String? = null,
    initialVideoTitle: String? = null,
    initialChannelId: String? = null,
    onBack: () -> Unit = {},
    viewModel: PlayerViewModel
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    DisposableEffect(Unit) {
        activity?.window?.setSustainedPerformanceMode(true)
        onDispose {
            activity?.window?.setSustainedPerformanceMode(false)
        }
    }
    
    val uiState by viewModel.uiState.collectAsState()
    val progressState by viewModel.progressState.collectAsState()
    val watchTimeTracker = viewModel.getWatchTimeTracker()
    
    var settingsVersion by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        activity?.let { viewModel.initOrientationHelper(it) }
    }
    
    val isBackgroundPlayEnabled = remember(uiState.backgroundPlay) { uiState.backgroundPlay }
    
    var media3Player by remember { 
        mutableStateOf(viewModel.getOrCreatePlayer(isBackgroundPlayEnabled))
    }
    
    LaunchedEffect(media3Player, settingsVersion) {
        val decoderType = com.antoniegil.astronia.util.SettingsManager.getDecoderType(context)
        media3Player.setHardwareAcceleration(decoderType == 0)
    }
    
    val isFullscreen = uiState.isFullscreen
    val listState = rememberLazyListState()
    var showPlayerSettings by remember { mutableStateOf(false) }
    
    val gestureState = rememberGestureControlState(context, activity)
    
    val originalBrightness = remember {
        activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0 } ?: -1f
    }
    
    val configuration = LocalConfiguration.current
    var isInPictureInPictureMode by remember { mutableStateOf(false) }
    
    var pendingAutoPlay by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.refreshPreferences()
    }
    
    LaunchedEffect(url) {
        media3Player.apply {
            stop()
            exoPlayer?.clearMediaItems()
        }
        viewModel.loadChannels(url, initialChannelUrl, initialChannelId, initialVideoTitle)
    }
    
    LaunchedEffect(url, uiState.currentChannelUrl) {
        if (uiState.currentChannelUrl.isNotEmpty()) {
            val currentMediaUrl = media3Player.exoPlayer?.currentMediaItem?.localConfiguration?.uri?.toString()
            val autoPlayEnabled = com.antoniegil.astronia.util.SettingsManager.getAutoPlay(context)
            
            if (currentMediaUrl != uiState.currentChannelUrl) {
                media3Player.setDataSource(uiState.currentChannelUrl)
                media3Player.updateMediaTitle(uiState.videoTitle)
                if (autoPlayEnabled || uiState.isPlaying) {
                    pendingAutoPlay = true
                    media3Player.start()
                }
            } else if (autoPlayEnabled && !media3Player.isPlaying) {
                media3Player.start()
            }
        }
    }
    
    LaunchedEffect(configuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val isPip = activity?.isInPictureInPictureMode == true
            if (isPip != isInPictureInPictureMode) {
                isInPictureInPictureMode = isPip
            }
        }
    }
    
    LaunchedEffect(uiState.shouldScrollToChannel, uiState.channels, uiState.currentChannelUrl) {
        if (uiState.shouldScrollToChannel && uiState.channels.isNotEmpty() && uiState.currentChannelUrl.isNotEmpty()) {
            val currentIndex = uiState.channels.indexOfFirst { it.url == uiState.currentChannelUrl }
            if (currentIndex >= 0) {
                listState.scrollToItem(currentIndex)
            }
            viewModel.clearScrollFlag()
        }
    }

    LaunchedEffect(uiState.videoTitle, uiState.currentChannelUrl, uiState.currentChannelId) {
        viewModel.saveHistory()
    }

    LaunchedEffect(uiState.isPlaying) {
        if (uiState.isPlaying) {
            viewModel.startWatchTimeTracking()
        } else {
            viewModel.stopWatchTimeTracking()
        }
    }

    LaunchedEffect(uiState.showControls, uiState.autoHideControls, uiState.isPlaying, uiState.isBuffering) {
        if (uiState.autoHideControls && uiState.showControls && uiState.isPlaying && !uiState.isBuffering) {
            delay(PlayerConstants.AUTO_HIDE_CONTROLS_DELAY_MS)
            viewModel.hideControls()
        }
    }

    LaunchedEffect(uiState.autoHideControls) {
        viewModel.showControls()
    }

    LaunchedEffect(uiState.isBuffering) {
        if (uiState.isBuffering) {
            viewModel.showControls()
        }
    }

    var wasPlayingBeforePause by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, uiState.enablePip) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val isPip = activity?.isInPictureInPictureMode == true
                        isInPictureInPictureMode = isPip
                    }
                    viewModel.onAppBackground()
                }

                Lifecycle.Event.ON_RESUME -> {
                    val oldSettingsVersion = settingsVersion
                    viewModel.refreshPreferences()
                    settingsVersion = oldSettingsVersion + 1
                    
                    isInPictureInPictureMode = activity?.isInPictureInPictureMode == true
                    
                    viewModel.onAppForeground()
                    
                    val currentPos = media3Player.currentPosition
                    val buffered = media3Player.bufferedPosition
                    val dur = media3Player.duration
                    val isPlaying = media3Player.isPlaying
                    
                    viewModel.updatePlaybackState(isPlaying, currentPos, buffered, dur)
                }

                Lifecycle.Event.ON_STOP -> {
                    val backgroundPlay = com.antoniegil.astronia.util.SettingsManager.getBackgroundPlay(context)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val isPip = activity?.isInPictureInPictureMode == true
                        if (isPip && !backgroundPlay) {
                            media3Player.pause()
                            wasPlayingBeforePause = false
                            return@LifecycleEventObserver
                        }
                    }
                    if (!backgroundPlay && !isInPictureInPictureMode) {
                        wasPlayingBeforePause = media3Player.isPlaying
                        media3Player.pause()
                    } else if (backgroundPlay) {
                        media3Player.exoPlayer?.let { player ->
                            PlaybackService.currentPlayer = player
                            PlaybackService.currentTitle = uiState.videoTitle
                            val serviceIntent = Intent(context, PlaybackService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                        }
                    }
                }

                Lifecycle.Event.ON_START -> {
                    val backgroundPlay = com.antoniegil.astronia.util.SettingsManager.getBackgroundPlay(context)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val isPip = activity?.isInPictureInPictureMode == true
                        if (!isPip && isInPictureInPictureMode) {
                            wasPlayingBeforePause = false
                        }
                    }
                    if (!backgroundPlay && !isInPictureInPictureMode && wasPlayingBeforePause) {
                        pendingAutoPlay = true
                        wasPlayingBeforePause = false
                    } else if (backgroundPlay) {
                        val serviceIntent = Intent(context, PlaybackService::class.java)
                        context.stopService(serviceIntent)
                    }
                }

                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            media3Player.pause()
            media3Player.attachSurface(null)
            viewModel.releaseOrientationHelper()
            activity?.window?.attributes = activity.window.attributes.apply {
                screenBrightness = originalBrightness
            }
        }
    }

    BackHandler(enabled = true) {
        if (isFullscreen) {
            val delay = viewModel.backToPortrait()
            if (delay > 0) {
                scope.launch {
                    delay(delay.toLong())
                }
            }
        } else {
            media3Player.pause()
            onBack()
        }
    }

    LegacyThemeBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isFullscreen) PaddingValues(0.dp) else paddingValues)
            ) {
                GestureControlEffects(
                    gestureState = gestureState,
                    onGestureStateChange = { gestureState.value = it }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isFullscreen) {
                                Modifier.fillMaxHeight()
                            } else {
                                Modifier.aspectRatio(16f / 9f)
                            }
                        )
                        .gestureControlModifier(
                                context = context,
                                activity = activity,
                                gestureState = gestureState,
                                onGestureStateChange = { gestureState.value = it }
                            )
                            .clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) {
                                viewModel.toggleControls()
                            }
                    ) {
                        LaunchedEffect(media3Player) {
                            media3Player.surface?.let { surface ->
                                if (surface.isValid) {
                                    media3Player.attachSurface(surface)
                                }
                            }
                        }
                        
                        val onSurfaceReady = remember {
                            {
                                if (pendingAutoPlay) {
                                    pendingAutoPlay = false
                                    media3Player.start()
                                }
                            }
                        }
                        
                        if (uiState.currentChannelUrl.isNotEmpty()) {
                            PlayerSurface(
                                player = media3Player,
                                aspectRatio = uiState.aspectRatio,
                                mirrorFlip = uiState.mirrorFlip,
                                isBackgroundRetained = false,
                                onSurfaceReady = onSurfaceReady,
                                currentChannelUrl = uiState.currentChannelUrl
                            )
                        }
                        
                        val onPlayPauseClick = remember(uiState.isPlaying) {
                            {
                                if (uiState.isPlaying) {
                                    media3Player.pause()
                                } else {
                                    media3Player.start()
                                }
                            }
                        }
                        
                        val onBackClick = remember(isFullscreen) {
                            {
                                if (isFullscreen) {
                                    val delay = viewModel.backToPortrait()
                                    if (delay > 0) {
                                        scope.launch {
                                            delay(delay.toLong())
                                        }
                                    }
                                } else {
                                    onBack()
                                }
                            }
                        }
                        
                        val onFullscreenClick = remember {
                            { viewModel.toggleFullscreen() }
                        }
                        
                        val onSettingsClick = remember {
                            { showPlayerSettings = true }
                        }
                        
                        androidx.compose.animation.AnimatedVisibility(
                            visible = uiState.showControls && !isInPictureInPictureMode,
                            enter = fadeIn(tween(100)),
                            exit = fadeOut(tween(100))
                        ) {
                            PlayerControlsOverlay(
                                modifier = Modifier.fillMaxSize(),
                                isPlaying = uiState.isPlaying,
                                isFullscreen = isFullscreen,
                                enablePip = uiState.enablePip,
                                isBuffering = uiState.isBuffering,
                                activity = activity,
                                media3Player = media3Player,
                                watchTimeTracker = watchTimeTracker,
                                currentCycleDuration = progressState.currentCycleDuration,
                                onCycleDurationChange = { viewModel.updateCycleDuration(it) },
                                onPlayPauseClick = onPlayPauseClick,
                                onBackClick = onBackClick,
                                onFullscreenClick = onFullscreenClick,
                                onSettingsClick = onSettingsClick
                            )
                        }

                        VolumeIndicator(
                            show = gestureState.value.showVolumeIndicator,
                            value = gestureState.value.volumeIndicatorValue,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        BrightnessIndicator(
                            show = gestureState.value.showBrightnessIndicator,
                            value = gestureState.value.brightnessIndicatorValue,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                if (!isFullscreen && !isInPictureInPictureMode) {
                    if (uiState.videoTitle.isNotEmpty()) {
                        Text(
                            text = uiState.videoTitle,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    ChannelListSection(
                        channels = uiState.channels,
                        currentChannelUrl = uiState.currentChannelUrl,
                        actualPlayingUrl = uiState.actualPlayingUrl,
                        isLoadingChannels = uiState.isLoadingChannels,
                        listState = listState,
                        media3Player = media3Player,
                        onChannelClick = { channel ->
                            viewModel.switchChannel(channel)
                        },
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }
        }

        if (showPlayerSettings && !isInPictureInPictureMode) {
            PlayerSettingsBottomSheet(
                enablePip = uiState.enablePip,
                backgroundPlay = uiState.backgroundPlay,
                aspectRatio = uiState.aspectRatio,
                mirrorFlip = uiState.mirrorFlip,
                onEnablePipChange = { viewModel.updateEnablePip(it) },
                onBackgroundPlayChange = { viewModel.updateBackgroundPlay(it) },
                onAspectRatioChange = { viewModel.updateAspectRatio(it) },
                onMirrorFlipChange = { viewModel.updateMirrorFlip(it) },
                onDismiss = { showPlayerSettings = false }
            )
        }
    }
}
