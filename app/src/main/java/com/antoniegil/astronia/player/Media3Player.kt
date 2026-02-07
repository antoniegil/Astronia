package com.antoniegil.astronia.player

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.view.Surface
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.antoniegil.astronia.util.ErrorHandler

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class Media3Player(private val context: Context) {
    var exoPlayer: ExoPlayer? = null
        private set
    internal var surface: Surface? = null
    private var currentHardwareAcceleration: Boolean = true
    private var shouldPlayWhenReady: Boolean = false
    private var errorRetryCount: Int = 0

    var onPreparedListener: (() -> Unit)? = null
    var onInfoListener: ((what: Int, extra: Int) -> Boolean)? = null
    var onBufferingListener: ((Boolean) -> Unit)? = null
    var onPlaybackStateChanged: ((isPlaying: Boolean, position: Long, bufferedPosition: Long, duration: Long) -> Unit)? = null

    init {
        createPlayer(true)
    }
    
    private fun createPlayer(hardwareAcceleration: Boolean) {
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setTunnelingEnabled(hardwareAcceleration && isTunnelingSupported())
            )
        }
        
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
            setExtensionRendererMode(
                if (hardwareAcceleration) DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF 
                else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            )
        }
        
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(2000, 10000, 1000, 1000)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        
        exoPlayer = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            .apply {
                setHandleAudioBecomingNoisy(true)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true
                )
                addAnalyticsListener(createLatencyMonitor())
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                errorRetryCount = 0
                                onPreparedListener?.invoke()
                                onInfoListener?.invoke(MEDIA_INFO_VIDEO_RENDERING_START, 0)
                                onBufferingListener?.invoke(false)
                                if (shouldPlayWhenReady && !isPlaying) {
                                    play()
                                }
                            }
                            Player.STATE_ENDED -> {
                                shouldPlayWhenReady = false
                                onBufferingListener?.invoke(false)
                            }
                            Player.STATE_BUFFERING -> {
                                onBufferingListener?.invoke(true)
                            }
                            Player.STATE_IDLE -> {
                                onBufferingListener?.invoke(false)
                                if (shouldPlayWhenReady && exoPlayer?.currentMediaItem != null) {
                                    exoPlayer?.prepare()
                                }
                            }
                        }
                        notifyPlaybackState()
                    }
                    
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_BUFFERING) {
                            shouldPlayWhenReady = isPlaying
                        }
                        if (isPlaying && playbackState == Player.STATE_READY) {
                            onBufferingListener?.invoke(false)
                        }
                        notifyPlaybackState()
                    }
                    
                    override fun onPositionDiscontinuity(
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int
                    ) {
                        notifyPlaybackState()
                    }
                    
                    override fun onPlayerError(error: PlaybackException) {
                        ErrorHandler.logError("Media3Player", "Playback error occurred", error)
                        
                        val wasPlaying = shouldPlayWhenReady
                        if (error.cause is androidx.media3.exoplayer.source.BehindLiveWindowException) {
                            exoPlayer?.let { player ->
                                player.seekToDefaultPosition()
                                player.prepare()
                                if (wasPlaying) {
                                    player.play()
                                }
                            }
                        } else {
                            exoPlayer?.let { player ->
                                if (wasPlaying && player.playbackState == Player.STATE_IDLE) {
                                    player.prepare()
                                    player.play()
                                }
                            }
                            onBufferingListener?.invoke(false)
                        }
                    }
                    
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        onInfoListener?.invoke(MEDIA_INFO_VIDEO_RENDERING_START, 0)
                    }
                })
            }
    }
    
    private fun notifyPlaybackState() {
        exoPlayer?.let { player ->
            val pos = player.currentPosition.coerceAtLeast(0L)
            val buffered = player.bufferedPosition.coerceAtLeast(0L)
            val dur = if (player.duration == C.TIME_UNSET || player.duration < 0) 0L else player.duration
            onPlaybackStateChanged?.invoke(player.isPlaying, pos, buffered, dur)
        }
    }
    
    fun attachSurface(surface: Surface?) {
        this.surface = surface
        try {
            if (surface == null || surface.isValid) {
                exoPlayer?.setVideoSurface(surface)
            }
        } catch (e: Exception) {
            ErrorHandler.logError("Media3Player", "Failed to attach surface", e)
        }
    }
    
    fun setDataSource(url: String) {
        val mediaItem = MediaItem.fromUri(url)
        exoPlayer?.apply {
            stop()
            clearMediaItems()
            setMediaItem(mediaItem)
            prepare()
        }
    }
    
    fun prepareAsync() {
        onPreparedListener?.invoke()
    }
    
    fun start() {
        shouldPlayWhenReady = true
        exoPlayer?.let { player ->
            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            }
            player.play()
            notifyPlaybackState()
        }
    }
    
    fun pause() {
        shouldPlayWhenReady = false
        exoPlayer?.let { player ->
            player.pause()
            notifyPlaybackState()
        }
    }
    
    fun stop() {
        exoPlayer?.stop()
        notifyPlaybackState()
    }
    
    fun seekTo(msec: Long) {
        exoPlayer?.seekTo(msec)
        notifyPlaybackState()
    }
    
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
                if (wasPlaying) {
                    start()
                }
            }
        }
    }
    
    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }
    
    val isPlaying: Boolean
        get() = exoPlayer?.isPlaying ?: false
    
    val currentPosition: Long
        get() = exoPlayer?.currentPosition ?: 0L
    
    val bufferedPosition: Long
        get() = exoPlayer?.bufferedPosition ?: 0L
    
    val duration: Long
        get() = exoPlayer?.duration?.takeIf { it != C.TIME_UNSET } ?: 0L
    
    private fun isTunnelingSupported(): Boolean {
        return try {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            codecList.codecInfos.any { codecInfo ->
                codecInfo.supportedTypes.any { type ->
                    type.startsWith("video/") && 
                    codecInfo.getCapabilitiesForType(type)
                        .isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_TunneledPlayback)
                }
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun createLatencyMonitor() = object : AnalyticsListener {
        private var lastCheck = 0L
        
        override fun onPlaybackStateChanged(
            eventTime: AnalyticsListener.EventTime,
            state: Int
        ) {
            if (state == Player.STATE_READY && System.currentTimeMillis() - lastCheck > 1000) {
                lastCheck = System.currentTimeMillis()
                exoPlayer?.let {
                    val latency = it.bufferedPosition - it.currentPosition
                    if (latency > 5000 && it.duration != C.TIME_UNSET && it.duration > 0) {
                        val targetPosition = it.currentPosition + 1000
                        if (targetPosition < it.duration) {
                            it.seekTo(targetPosition)
                        }
                    }
                }
            }
        }
    }
    
    companion object {
        const val MEDIA_INFO_VIDEO_RENDERING_START = 3
    }
}
