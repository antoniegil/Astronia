package com.antoniegil.astronia.viewmodel

import androidx.compose.runtime.Stable

@Stable
data class PlayerState(
    val videoTitle: String = "",
    val currentChannelUrl: String = "",
    val currentChannelId: String? = null,
    val estimatedProgress: Float = 0f
)

@Stable
data class PlayerRuntimeState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val bufferedPosition: Long = 0L,
    val duration: Long = 0L,
    val accumulatedWatchTime: Long = 0L,
    val isBuffering: Boolean = false,
    val isFullscreen: Boolean = false,
    val showControls: Boolean = true,
    val showPlayerSettings: Boolean = false
)
