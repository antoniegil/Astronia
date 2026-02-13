package com.antoniegil.astronia.data.repository

import android.content.Context
import com.antoniegil.astronia.util.M3U8Channel
import com.antoniegil.astronia.util.M3U8Parser
import com.antoniegil.astronia.util.Result
import com.antoniegil.astronia.util.SettingsManager

class PlayerRepository(private val context: Context) {
    
    suspend fun parseM3U8FromUrl(url: String): Result<List<M3U8Channel>> {
        return M3U8Parser.parseM3U8FromUrl(url)
    }
    
    suspend fun parseM3U8FromContent(content: String): Result<List<M3U8Channel>> {
        return M3U8Parser.parseM3U8(content)
    }

    fun getAutoHideControls(): Boolean = SettingsManager.getAutoHideControls(context)
    
    fun getEnablePip(): Boolean = SettingsManager.getEnablePip(context)
    
    fun getBackgroundPlay(): Boolean = SettingsManager.getBackgroundPlay(context)

    fun getAspectRatio(): Int = SettingsManager.getAspectRatio(context)
    
    fun getMirrorFlip(): Boolean = SettingsManager.getMirrorFlip(context)
    
    fun getQualityPreference(): Int = SettingsManager.getQualityPreference(context)
    
    fun setEnablePip(value: Boolean) = SettingsManager.setEnablePip(context, value)
    
    fun setBackgroundPlay(value: Boolean) = SettingsManager.setBackgroundPlay(context, value)
    
    fun setAspectRatio(value: Int) = SettingsManager.setAspectRatio(context, value)
    
    fun setMirrorFlip(value: Boolean) = SettingsManager.setMirrorFlip(context, value)
}
