package com.antoniegil.astronia.player

import android.content.Context
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.antoniegil.astronia.util.ErrorHandler
import com.antoniegil.astronia.util.NetworkUtils

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal object PlayerListeners {
    
    fun createUrlUpgradeListener(
        isInitialLoad: () -> Boolean,
        initialM3uUrl: () -> String?,
        onUrlResolved: (String) -> Unit
    ) = object : AnalyticsListener {
        private var lastUrl: String? = null
        
        @Deprecated("Deprecated in Java")
        override fun onLoadStarted(
            eventTime: AnalyticsListener.EventTime,
            loadEventInfo: androidx.media3.exoplayer.source.LoadEventInfo,
            mediaLoadData: androidx.media3.exoplayer.source.MediaLoadData
        ) {
            val loadUrl = loadEventInfo.dataSpec.uri.toString()
            if (isInitialLoad() && loadUrl != lastUrl) {
                lastUrl = loadUrl
                onUrlResolved(NetworkUtils.upgradeToHttps(initialM3uUrl(), loadUrl))
            }
        }
    }
    
    fun createCombinedListener(
        getPlayer: () -> androidx.media3.exoplayer.ExoPlayer?,
        state: PlayerState,
        callbacks: PlayerCallbacks
    ) = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val player = getPlayer()
            when (playbackState) {
                Player.STATE_READY -> {
                    if (state.isInitialLoad) {
                        state.isInitialLoad = false
                        callbacks.onPrepared()
                    }
                    callbacks.onBuffering(false)
                    if (state.shouldPlayWhenReady && player?.isPlaying == false) player.play()
                }
                Player.STATE_ENDED -> {
                    state.shouldPlayWhenReady = false
                    callbacks.onBuffering(false)
                }
                Player.STATE_BUFFERING -> callbacks.onBuffering(true)
                Player.STATE_IDLE -> {
                    callbacks.onBuffering(false)
                    if (state.shouldPlayWhenReady && player?.currentMediaItem != null) player.prepare()
                }
            }
            notifyState(player, callbacks.onStateChanged)
        }
        
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val player = getPlayer()
            val playbackState = player?.playbackState ?: Player.STATE_IDLE
            if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_BUFFERING) {
                state.shouldPlayWhenReady = isPlaying
            }
            if (isPlaying && playbackState == Player.STATE_READY) callbacks.onBuffering(false)
            notifyState(player, callbacks.onStateChanged)
        }
        
        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
            notifyState(getPlayer(), callbacks.onStateChanged)
        }
        
        override fun onPlayerError(error: PlaybackException) {
            if (state.isFixingM3u8) {
                return
            }
            
            val player = getPlayer()
            val httpError = error.cause as? androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
            val httpCode = httpError?.responseCode ?: 0
            
            val isRetriableNetworkError = error.errorCode in listOf(
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
            )
            
            val isManifestParsingError = error.errorCode in listOf(
                PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED
            )
            
            val isPlaylistStuck = error.cause?.javaClass?.simpleName == "PlaylistStuckException"
            
            when {
                error.cause is androidx.media3.exoplayer.source.BehindLiveWindowException -> {
                    player?.seekToDefaultPosition()
                    player?.prepare()
                    if (state.shouldPlayWhenReady) player?.play()
                }
                isPlaylistStuck && state.hasTriedM3u8Fix -> {
                    state.context?.cacheDir?.listFiles()?.filter { it.name.startsWith("m3u8_") }?.forEach { it.delete() }
                    callbacks.onReloadOriginal()
                }
                isManifestParsingError && !state.hasTriedM3u8Fix -> {
                    state.isFixingM3u8 = true
                    state.hasTriedM3u8Fix = true
                    player?.stop()
                    player?.clearMediaItems()
                    callbacks.onBuffering(true)
                    callbacks.onRetryWithFix()
                }
                isManifestParsingError && state.hasTriedM3u8Fix -> {
                    state.shouldPlayWhenReady = false
                    player?.stop()
                    callbacks.onBuffering(false)
                    val errorMsg = when {
                        error.message?.contains("None of the available extractors") == true -> 
                            "Unsupported video format"
                        error.cause?.message?.contains("sniff failures") == true -> 
                            "Unable to detect video format"
                        else -> "Format not supported"
                    }
                    callbacks.onError(errorMsg, false)
                }
                else -> {
                    val isExtractorError = error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
                                          error.message?.contains("None of the available extractors") == true
                    
                    if (!isExtractorError) {
                        state.shouldPlayWhenReady = false
                        player?.stop()
                        callbacks.onBuffering(false)
                        
                        val errorMsg = when {
                            NetworkUtils.isPermanentHttpError(httpCode) -> "HTTP $httpCode: ${httpError?.responseMessage ?: "Error"}"
                            error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> 
                                "HTTP $httpCode: ${httpError?.responseMessage ?: "Error"}"
                            isRetriableNetworkError -> "Network connection failed"
                            isManifestParsingError -> "Format error"
                            else -> "Playback error: ${error.errorCodeName}"
                        }
                        callbacks.onError(errorMsg, false)
                    }
                }
            }
        }
    }
    
    private fun notifyState(player: androidx.media3.exoplayer.ExoPlayer?, callback: (Boolean, Long, Long, Long) -> Unit) {
        player?.let {
            val dur = if (it.duration == androidx.media3.common.C.TIME_UNSET || it.duration < 0) 0L else it.duration
            callback(it.isPlaying, it.currentPosition.coerceAtLeast(0L), it.bufferedPosition.coerceAtLeast(0L), dur)
        }
    }
}

internal data class PlayerState(
    var shouldPlayWhenReady: Boolean = false,
    var isInitialLoad: Boolean = false,
    var currentMediaUrl: String? = null,
    var context: Context? = null,
    var hasTriedM3u8Fix: Boolean = false,
    var isFixingM3u8: Boolean = false
)

internal data class PlayerCallbacks(
    val onPrepared: () -> Unit,
    val onBuffering: (Boolean) -> Unit,
    val onStateChanged: (Boolean, Long, Long, Long) -> Unit,
    val onError: (String, Boolean) -> Unit,
    val onRetryWithFix: () -> Unit = {},
    val onReloadOriginal: () -> Unit = {}
)
