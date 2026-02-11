package com.antoniegil.astronia.player

import android.content.Context
import android.view.Surface
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.antoniegil.astronia.util.ErrorHandler
import com.antoniegil.astronia.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class Media3Player(private val context: Context) {
    var exoPlayer: ExoPlayer? = null
        private set
    internal var surface: Surface? = null
    private var currentHardwareAcceleration: Boolean = true
    private var initialM3uUrl: String? = null
    private var actualPlayingUrl: String? = null
    private val state = PlayerState(context = context)

    var onPreparedListener: (() -> Unit)? = null
    var onBufferingListener: ((Boolean) -> Unit)? = null
    var onPlaybackStateChanged: ((isPlaying: Boolean, position: Long, bufferedPosition: Long, duration: Long) -> Unit)? = null
    var onErrorListener: ((error: String, isRetriable: Boolean) -> Unit)? = null

    init {
        NetworkUtils.setupAndroid7SSL()
        createPlayer(true)
    }
    
    private fun createPlayer(hardwareAcceleration: Boolean) {
        val callbacks = PlayerCallbacks(
            onPrepared = { onPreparedListener?.invoke() },
            onBuffering = { onBufferingListener?.invoke(it) },
            onStateChanged = { playing, pos, buffered, dur -> onPlaybackStateChanged?.invoke(playing, pos, buffered, dur) },
            onError = { msg, retriable -> onErrorListener?.invoke(msg, retriable) },
            onRetryWithFix = { retryWithFixedM3u8() },
            onReloadOriginal = { reloadOriginalUrl() }
        )
        
        exoPlayer = PlayerFactory.createExoPlayer(
            context = context,
            hardwareAcceleration = hardwareAcceleration,
            urlUpgradeListener = PlayerListeners.createUrlUpgradeListener(
                isInitialLoad = { state.isInitialLoad },
                initialM3uUrl = { initialM3uUrl },
                onUrlResolved = { actualPlayingUrl = it }
            ),
            latencyMonitor = PlayerFactory.createLatencyMonitor { exoPlayer },
            combinedListener = PlayerListeners.createCombinedListener({ exoPlayer }, state, callbacks)
        )
    }
    
    fun attachSurface(surface: Surface?) {
        this.surface = surface
        try {
            if (surface == null || surface.isValid) {
                exoPlayer?.setVideoSurface(surface)
            }
        } catch (_: Exception) {
        }
    }
    
    fun setDataSource(url: String) {
        initialM3uUrl = url
        state.currentMediaUrl = url
        state.isInitialLoad = true
        state.hasTriedM3u8Fix = false
        state.isFixingM3u8 = false
        actualPlayingUrl = null
        
        exoPlayer?.apply {
            stop()
            clearMediaItems()
            setMediaItem(createMediaItem(url))
            prepare()
        }
    }
    
    private fun createMediaItem(url: String): MediaItem =
        MediaItem.Builder().setUri(url).build()
    
    internal fun retryWithFixedM3u8() {
        val url = initialM3uUrl ?: return
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val fixedUrl = UrlInterceptor.fixMalformedM3u8(context, url)
                state.currentMediaUrl = fixedUrl
                
                exoPlayer?.apply {
                    setMediaItem(createMediaItem(fixedUrl))
                    prepare()
                    if (state.shouldPlayWhenReady) play()
                }
                state.isFixingM3u8 = false
            } catch (_: Exception) {
                onBufferingListener?.invoke(false)
                state.isFixingM3u8 = false
            }
        }
    }
    
    internal fun reloadOriginalUrl() {
        val url = initialM3uUrl ?: return
        state.hasTriedM3u8Fix = false
        
        exoPlayer?.apply {
            stop()
            clearMediaItems()
            setMediaItem(createMediaItem(url))
            prepare()
            if (state.shouldPlayWhenReady) play()
        }
    }
    
    fun start() {
        state.shouldPlayWhenReady = true
        exoPlayer?.let {
            if (it.playbackState == androidx.media3.common.Player.STATE_IDLE) it.prepare()
            it.play()
        }
    }
    
    fun pause() {
        state.shouldPlayWhenReady = false
        exoPlayer?.pause()
    }
    
    fun stop() = exoPlayer?.stop()

    fun setHardwareAcceleration(enabled: Boolean) {
        if (currentHardwareAcceleration != enabled) {
            currentHardwareAcceleration = enabled
            val currentUrl = exoPlayer?.currentMediaItem?.localConfiguration?.uri?.toString()
            val currentPos = exoPlayer?.currentPosition ?: 0L
            val wasPlaying = exoPlayer?.isPlaying ?: false
            
            release()
            createPlayer(enabled)
            
            currentUrl?.let {
                setDataSource(it)
                exoPlayer?.seekTo(currentPos)
                if (wasPlaying) start()
            }
        }
    }
    
    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }
    
    val isPlaying: Boolean get() = exoPlayer?.isPlaying ?: false
    val currentPosition: Long get() = exoPlayer?.currentPosition ?: 0L
    val bufferedPosition: Long get() = exoPlayer?.bufferedPosition ?: 0L
    val duration: Long get() = exoPlayer?.duration?.takeIf { it != C.TIME_UNSET } ?: 0L
    
    fun getActualPlayingUrl(): String? = actualPlayingUrl
}
