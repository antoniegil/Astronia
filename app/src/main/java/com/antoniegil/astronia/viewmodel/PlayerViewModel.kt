package com.antoniegil.astronia.viewmodel

import android.app.Application
import android.view.Surface
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antoniegil.astronia.Astronia
import com.antoniegil.astronia.data.repository.PlayerRepository
import com.antoniegil.astronia.player.Media3Player
import com.antoniegil.astronia.util.ErrorHandler
import com.antoniegil.astronia.util.HistoryManager
import com.antoniegil.astronia.util.M3U8Channel
import com.antoniegil.astronia.util.OrientationHelper
import com.antoniegil.astronia.util.WatchTimeTracker
import com.antoniegil.astronia.util.onError
import com.antoniegil.astronia.util.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPosition: Long = 0L,
    val bufferedPosition: Long = 0L,
    val duration: Long = 0L,
    val videoTitle: String = "",
    val currentChannelUrl: String = "",
    val currentChannelId: String? = null,
    val channels: List<M3U8Channel> = emptyList(),
    val isLoadingChannels: Boolean = false,
    val showControls: Boolean = true,
    val enablePip: Boolean = true,
    val backgroundPlay: Boolean = false,
    val aspectRatio: Int = 3,
    val mirrorFlip: Boolean = false,
    val autoHideControls: Boolean = true,
    val playlistUrl: String = "",
    val shouldScrollToChannel: Boolean = false,
    val isFullscreen: Boolean = false
)

data class PlayerProgressState(
    val accumulatedWatchTime: Long = 0L,
    val estimatedProgress: Float = 0f,
    val currentCycleDuration: Float = 20f
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = PlayerRepository(application)
    private val watchTimeTracker = WatchTimeTracker()
    
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    
    private val _progressState = MutableStateFlow(PlayerProgressState())
    val progressState: StateFlow<PlayerProgressState> = _progressState.asStateFlow()
    
    private var localPlayer: Media3Player? = null
    private var isUsingGlobalPlayer = false
    private var lastSurface: Surface? = null
    private var isBackgroundRetained = false
    private var orientationHelper: OrientationHelper? = null
    
    init {
        loadPreferences()
    }
    
    private fun loadPreferences() {
        _uiState.value = _uiState.value.copy(
            enablePip = repository.getEnablePip(),
            backgroundPlay = repository.getBackgroundPlay(),
            aspectRatio = repository.getAspectRatio(),
            mirrorFlip = repository.getMirrorFlip(),
            autoHideControls = repository.getAutoHideControls()
        )
    }
    
    fun refreshPreferences() {
        loadPreferences()
    }
    
    fun loadChannels(url: String, initialChannelUrl: String?, initialChannelId: String?, initialVideoTitle: String?) {
        val currentUrl = _uiState.value.playlistUrl
        if (currentUrl == url) {
            val startChannel = when {
                initialChannelUrl != null -> _uiState.value.channels.find { it.url == initialChannelUrl }
                initialChannelId != null -> _uiState.value.channels.find { it.id == initialChannelId }
                else -> null
            }
            if (startChannel != null) {
                _uiState.value = _uiState.value.copy(
                    currentChannelUrl = startChannel.url,
                    currentChannelId = startChannel.id,
                    videoTitle = startChannel.name,
                    shouldScrollToChannel = true
                )
                return
            }
        }
        
        localPlayer?.stop()
        
        viewModelScope.launch {
            android.util.Log.d("PlayerViewModel", "loadChannels called: url=$url")
            _uiState.value = _uiState.value.copy(
                channels = emptyList(),
                isLoadingChannels = true,
                currentChannelUrl = "",
                currentChannelId = null,
                videoTitle = "",
                currentPosition = 0L,
                duration = 0L,
                bufferedPosition = 0L,
                isPlaying = false,
                isBuffering = false,
                playlistUrl = ""
            )
            _progressState.value = PlayerProgressState()
            watchTimeTracker.reset()
            android.util.Log.d("PlayerViewModel", "WatchTimeTracker reset in loadChannels")
            
            try {
                var parsedChannels: List<M3U8Channel> = emptyList()
                var isM3U8Playlist = false
                
                withContext(Dispatchers.IO) {
                    when {
                        url.startsWith("http") || url.startsWith("https") || 
                        url.startsWith("rtmp") || url.startsWith("rtsp") -> {
                            repository.parseM3U8FromUrl(url).onSuccess { channels ->
                                parsedChannels = channels
                                isM3U8Playlist = channels.size > 1
                            }.onError { _, message ->
                                ErrorHandler.logError("PlayerViewModel", "Failed to load M3U8: $message")
                            }
                        }
                        url.startsWith("content://") || url.startsWith("file://") -> {
                            try {
                                val uri = url.toUri()
                                getApplication<Application>().contentResolver.openInputStream(uri)?.use { inputStream ->
                                    val content = inputStream.bufferedReader().readText()
                                    if (content.contains("#EXTM3U")) {
                                        repository.parseM3U8FromContent(content).onSuccess { channels ->
                                            parsedChannels = channels
                                            isM3U8Playlist = channels.size > 1
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                ErrorHandler.logError("PlayerViewModel", "Failed to read local file", e)
                            }
                        }
                        url.contains("#EXTM3U") -> {
                            repository.parseM3U8FromContent(url).onSuccess { channels ->
                                parsedChannels = channels
                                isM3U8Playlist = channels.size > 1
                            }
                        }
                    }
                }
                
                val channels = parsedChannels.take(com.antoniegil.astronia.util.PlayerConstants.MAX_CHANNEL_DISPLAY)
                
                val startChannel = when {
                    initialChannelUrl != null -> channels.find { it.url == initialChannelUrl }
                        ?: channels.find { it.url.trim() == initialChannelUrl.trim() }
                    initialChannelId != null -> channels.find { it.id == initialChannelId }
                    else -> null
                } ?: channels.firstOrNull()
                
                val title = when {
                    startChannel != null -> startChannel.name
                    url.startsWith("content") || url.startsWith("file") -> getApplication<Application>().getString(com.antoniegil.astronia.R.string.local_video)
                    else -> getApplication<Application>().getString(com.antoniegil.astronia.R.string.unknown_video)
                }

                if (isM3U8Playlist) {
                    initialVideoTitle?.takeIf { it.isNotEmpty() } ?: url.substringAfterLast("/").substringBeforeLast(".").ifEmpty { 
                        url.substringAfterLast("/").ifEmpty { getApplication<Application>().getString(com.antoniegil.astronia.R.string.playlist) }
                    }
                } else {
                    ""
                }
                
                _uiState.value = _uiState.value.copy(
                    channels = channels,
                    videoTitle = title,
                    currentChannelUrl = startChannel?.url ?: url,
                    currentChannelId = startChannel?.id,
                    playlistUrl = if (isM3U8Playlist) url else "",
                    isLoadingChannels = false,
                    shouldScrollToChannel = true
                )
            } catch (e: Exception) {
                ErrorHandler.logError("PlayerViewModel", "Failed to load channels", e)
                _uiState.value = _uiState.value.copy(isLoadingChannels = false)
            }
        }
    }
    
    fun updatePlaybackState(isPlaying: Boolean, position: Long, buffered: Long, duration: Long) {
        _uiState.value = _uiState.value.copy(
            isPlaying = isPlaying,
            currentPosition = position,
            bufferedPosition = buffered,
            duration = duration
        )
        
        if (isPlaying) {
            watchTimeTracker.start()
        } else {
            watchTimeTracker.pause()
        }
    }
    
    fun updateBufferingState(isBuffering: Boolean) {
        _uiState.value = _uiState.value.copy(isBuffering = isBuffering)
    }
    
    fun updateCycleDuration(newDuration: Float) {
        _progressState.value = _progressState.value.copy(currentCycleDuration = newDuration)
    }
    
    private var watchTimeJob: kotlinx.coroutines.Job? = null
    
    fun startWatchTimeTracking() {
        watchTimeJob?.cancel()
        watchTimeJob = viewModelScope.launch(Dispatchers.Default) {
            kotlinx.coroutines.delay(300L)
            while (true) {
                val watchTime = watchTimeTracker.getAccumulatedTime()
                val cycleDuration = _progressState.value.currentCycleDuration
                val estimatedProgress = if (cycleDuration > 0) {
                    (watchTime / (cycleDuration * 1000f)).coerceIn(0f, 1f)
                } else 0f
                _progressState.value = _progressState.value.copy(
                    accumulatedWatchTime = watchTime,
                    estimatedProgress = estimatedProgress
                )
                kotlinx.coroutines.delay(1000L)
            }
        }
    }
    
    fun stopWatchTimeTracking() {
        watchTimeJob?.cancel()
        watchTimeJob = null
    }
    
    fun toggleControls() {
        _uiState.value = _uiState.value.copy(showControls = !_uiState.value.showControls)
    }
    
    fun showControls() {
        _uiState.value = _uiState.value.copy(showControls = true)
    }
    
    fun hideControls() {
        _uiState.value = _uiState.value.copy(showControls = false)
    }
    
    fun switchChannel(channel: M3U8Channel) {
        if (_uiState.value.currentChannelUrl == channel.url) {
            return
        }
        val wasPlaying = _uiState.value.isPlaying
        _uiState.value = _uiState.value.copy(
            currentChannelUrl = channel.url,
            currentChannelId = channel.id,
            videoTitle = channel.name,
            currentPosition = 0L,
            duration = 0L,
            bufferedPosition = 0L,
            isPlaying = wasPlaying,
            isBuffering = false,
            shouldScrollToChannel = false
        )
        _progressState.value = PlayerProgressState(currentCycleDuration = 20f)
        watchTimeTracker.reset()
        android.util.Log.d("PlayerViewModel", "WatchTimeTracker reset in switchChannel")
    }
    
    fun clearScrollFlag() {
        _uiState.value = _uiState.value.copy(shouldScrollToChannel = false)
    }
    
    fun getOrCreatePlayer(backgroundPlay: Boolean): Media3Player {
        val player = if (backgroundPlay) {
            isUsingGlobalPlayer = true
            Astronia.getOrCreatePlayer(getApplication<Application>().applicationContext as Astronia)
        } else {
            if (localPlayer == null) {
                isUsingGlobalPlayer = false
                localPlayer = Media3Player(getApplication())
            }
            localPlayer!!
        }
        
        player.apply {
            setHardwareAcceleration(true)
            onPreparedListener = {}
            onBufferingListener = { buffering ->
                updateBufferingState(buffering)
            }
            onPlaybackStateChanged = { playing, position, buffered, dur ->
                updatePlaybackState(playing, position, buffered, dur)
            }
        }
        
        return player
    }
    
    fun releasePlayer() {
        localPlayer?.let { player ->
            player.pause()
            player.release()
        }
        localPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        stopWatchTimeTracking()
        if (!isUsingGlobalPlayer) {
            releasePlayer()
        }
    }
    
    fun updateEnablePip(value: Boolean) {
        repository.setEnablePip(value)
        _uiState.value = _uiState.value.copy(enablePip = value)
    }
    
    fun updateBackgroundPlay(value: Boolean) {
        repository.setBackgroundPlay(value)
        _uiState.value = _uiState.value.copy(backgroundPlay = value)
    }
    
    fun updateAspectRatio(value: Int) {
        repository.setAspectRatio(value)
        _uiState.value = _uiState.value.copy(aspectRatio = value)
    }
    
    fun updateMirrorFlip(value: Boolean) {
        repository.setMirrorFlip(value)
        _uiState.value = _uiState.value.copy(mirrorFlip = value)
    }
    
    fun saveHistory() {
        val state = _uiState.value
        if (state.playlistUrl.isNotEmpty() && state.videoTitle.isNotEmpty()) {
            HistoryManager.addOrUpdateHistory(
                getApplication(),
                state.playlistUrl,
                state.videoTitle,
                state.currentChannelUrl,
                state.currentChannelId
            )
        }
    }
    
    fun getWatchTimeTracker() = watchTimeTracker
    
    fun onAppBackground() {
        if (_uiState.value.backgroundPlay) {
            isBackgroundRetained = true
        }
    }
    
    fun onAppForeground() {
        if (isBackgroundRetained) {
            isBackgroundRetained = false
            localPlayer?.exoPlayer?.let { player ->
                if (lastSurface != null && lastSurface!!.isValid) {
                    player.setVideoSurface(lastSurface)
                }
            }
        }
    }

    fun initOrientationHelper(activity: android.app.Activity) {
        if (orientationHelper == null) {
            orientationHelper = OrientationHelper(activity) { isLandscape ->
                _uiState.value = _uiState.value.copy(isFullscreen = isLandscape)
            }
            orientationHelper?.enable()
        }
    }
    
    fun toggleFullscreen() {
        orientationHelper?.resolveByClick()
    }
    
    fun backToPortrait(): Int {
        return orientationHelper?.backToPortrait() ?: 0
    }
    
    fun releaseOrientationHelper() {
        orientationHelper?.release()
        orientationHelper = null
    }
}
