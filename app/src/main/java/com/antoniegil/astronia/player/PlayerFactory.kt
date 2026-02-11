package com.antoniegil.astronia.player

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal object PlayerFactory {
    
    fun createExoPlayer(
        context: Context,
        hardwareAcceleration: Boolean,
        urlUpgradeListener: AnalyticsListener,
        latencyMonitor: AnalyticsListener,
        combinedListener: androidx.media3.common.Player.Listener
    ): ExoPlayer {
        val tunnelingSupported = try {
            MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.any { codecInfo ->
                codecInfo.supportedTypes.any { type ->
                    type.startsWith("video/") && codecInfo.getCapabilitiesForType(type)
                        .isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_TunneledPlayback)
                }
            }
        } catch (e: Exception) { false }
        
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setTunnelingEnabled(hardwareAcceleration && tunnelingSupported))
        }
        
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
            setExtensionRendererMode(
                if (hardwareAcceleration) DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF 
                else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            )
        }
        
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(30000)
            .setAllowCrossProtocolRedirects(true)
        
        val compositeDataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(
            context,
            dataSourceFactory
        )
        
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(compositeDataSourceFactory)
        
        return ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setLoadControl(DefaultLoadControl.Builder()
                .setBufferDurationsMs(3000, 15000, 2000, 2000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build())
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
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
                addAnalyticsListener(urlUpgradeListener)
                addAnalyticsListener(latencyMonitor)
                addListener(combinedListener)
            }
    }
    
    fun createLatencyMonitor(getPlayer: () -> ExoPlayer?): AnalyticsListener {
        return object : AnalyticsListener {
            private var lastCheck = 0L
            
            override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
                if (state == androidx.media3.common.Player.STATE_READY && System.currentTimeMillis() - lastCheck > 1000) {
                    lastCheck = System.currentTimeMillis()
                    getPlayer()?.let {
                        val latency = it.bufferedPosition - it.currentPosition
                        if (latency > 5000 && it.duration != C.TIME_UNSET && it.duration > 0) {
                            val targetPosition = it.currentPosition + 1000
                            if (targetPosition < it.duration) it.seekTo(targetPosition)
                        }
                    }
                }
            }
        }
    }
}
