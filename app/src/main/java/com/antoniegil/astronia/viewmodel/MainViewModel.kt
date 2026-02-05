package com.antoniegil.astronia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.antoniegil.astronia.util.HistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackState(
    val playingUrl: String? = null,
    val initialChannelUrl: String? = null,
    val initialVideoTitle: String? = null,
    val initialChannelId: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    fun startPlayback(
        url: String,
        channelUrl: String? = null,
        videoTitle: String? = null,
        channelId: String? = null
    ) {
        _playbackState.value = PlaybackState(
            playingUrl = url,
            initialChannelUrl = channelUrl,
            initialVideoTitle = videoTitle,
            initialChannelId = channelId
        )
    }
    
    fun startPlaybackFromHistory(historyItem: HistoryItem) {
        _playbackState.value = PlaybackState(
            playingUrl = historyItem.url,
            initialChannelUrl = historyItem.lastChannelUrl,
            initialVideoTitle = historyItem.name,
            initialChannelId = historyItem.lastChannelId
        )
    }
    
    fun stopPlayback() {
        _playbackState.value = PlaybackState()
    }
}
