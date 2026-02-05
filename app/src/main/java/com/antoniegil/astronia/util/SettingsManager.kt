package com.antoniegil.astronia.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

object SettingsManager {
    
    @Volatile
    private var instance: PreferenceManagerImpl? = null
    
    internal fun getInstance(context: Context): PreferenceManagerImpl {
        return instance ?: synchronized(this) {
            instance ?: PreferenceManagerImpl(context.applicationContext).also { instance = it }
        }
    }
    
    val themeSettingsFlow: StateFlow<ThemeSettings>
        get() = instance?.themeSettingsFlow ?: MutableStateFlow(ThemeSettings()).asStateFlow()
    
    val historyFlow: StateFlow<List<HistoryItem>>
        get() = instance?.historyFlow ?: MutableStateFlow(emptyList<HistoryItem>()).asStateFlow()
    
    fun initializeThemeSettings(context: Context) {
        getInstance(context)
    }
    
    fun getAutoPlay(context: Context): Boolean = getInstance(context).getAutoPlay()
    fun setAutoPlay(context: Context, value: Boolean) = getInstance(context).setAutoPlay(value)
    
    fun getRememberPosition(context: Context): Boolean = getInstance(context).getRememberPosition()
    fun setRememberPosition(context: Context, value: Boolean) = getInstance(context).setRememberPosition(value)
    
    fun getAutoHideControls(context: Context): Boolean = getInstance(context).getAutoHideControls()
    fun setAutoHideControls(context: Context, value: Boolean) = getInstance(context).setAutoHideControls(value)
    
    fun getEnablePip(context: Context): Boolean = getInstance(context).getEnablePip()
    fun setEnablePip(context: Context, value: Boolean) = getInstance(context).setEnablePip(value)
    
    fun getBackgroundPlay(context: Context): Boolean = getInstance(context).getBackgroundPlay()
    fun setBackgroundPlay(context: Context, value: Boolean) = getInstance(context).setBackgroundPlay(value)
    
    fun getHardwareAcceleration(context: Context): Boolean = getInstance(context).getHardwareAcceleration()
    fun setHardwareAcceleration(context: Context, value: Boolean) = getInstance(context).setHardwareAcceleration(value)
    
    fun getAspectRatio(context: Context): Int = getInstance(context).getAspectRatio()
    fun setAspectRatio(context: Context, value: Int) = getInstance(context).setAspectRatio(value)
    
    fun getDecoderType(context: Context): Int = getInstance(context).getDecoderType()
    fun setDecoderType(context: Context, value: Int) = getInstance(context).setDecoderType(value)
    
    fun getMirrorFlip(context: Context): Boolean = getInstance(context).getMirrorFlip()
    fun setMirrorFlip(context: Context, value: Boolean) = getInstance(context).setMirrorFlip(value)
    
    fun getThemeMode(context: Context): Int = getInstance(context).getThemeMode()
    fun setThemeMode(context: Context, value: Int) = getInstance(context).setThemeMode(value)
    
    fun getDynamicColor(context: Context): Boolean = getInstance(context).getDynamicColor()
    fun setDynamicColor(context: Context, value: Boolean) = getInstance(context).setDynamicColor(value)
    
    fun getSeedColor(context: Context): Int = getInstance(context).getSeedColor()
    fun setSeedColor(context: Context, value: Int) = getInstance(context).setSeedColor(value)
    
    fun getPaletteStyle(context: Context): Int = getInstance(context).getPaletteStyle()
    fun setPaletteStyle(context: Context, value: Int) = getInstance(context).setPaletteStyle(value)
    
    fun getHighContrast(context: Context): Boolean = getInstance(context).getHighContrast()
    fun setHighContrast(context: Context, value: Boolean) = getInstance(context).setHighContrast(value)
    
    fun getLanguageTag(context: Context): String? = getInstance(context).getLanguageTag()
    fun setLanguageTag(context: Context, value: String?) = getInstance(context).setLanguageTag(value)
    
    fun getLocaleFromPreference(context: Context): Locale? = getInstance(context).getLocaleFromPreference()
    fun saveLocalePreference(context: Context, locale: Locale?) = getInstance(context).saveLocalePreference(locale)
    
    fun getProxyEnabled(context: Context): Boolean = getInstance(context).getProxyEnabled()
    fun setProxyEnabled(context: Context, value: Boolean) = getInstance(context).setProxyEnabled(value)
    
    fun getProxyHost(context: Context): String = getInstance(context).getProxyHost()
    fun setProxyHost(context: Context, value: String) = getInstance(context).setProxyHost(value)
    
    fun getProxyPort(context: Context): Int = getInstance(context).getProxyPort()
    fun setProxyPort(context: Context, value: Int) = getInstance(context).setProxyPort(value)
    
    fun getProxyUsername(context: Context): String = getInstance(context).getProxyUsername()
    fun setProxyUsername(context: Context, value: String) = getInstance(context).setProxyUsername(value)
    
    fun getProxyPassword(context: Context): String = getInstance(context).getProxyPassword()
    fun setProxyPassword(context: Context, value: String) = getInstance(context).setProxyPassword(value)
    
    fun getShowPlayerStats(context: Context): Boolean = getInstance(context).getShowPlayerStats()
    fun setShowPlayerStats(context: Context, value: Boolean) = getInstance(context).setShowPlayerStats(value)
    
    fun setPendingNavigation(context: Context, navigation: String) = getInstance(context).setPendingNavigation(navigation)
    fun getPendingNavigation(context: Context): String = getInstance(context).getPendingNavigation()
    fun clearPendingNavigation(context: Context) = getInstance(context).clearPendingNavigation()
    
    fun getAutoUpdate(context: Context): Boolean = getInstance(context).getAutoUpdate()
    fun setAutoUpdate(context: Context, value: Boolean) = getInstance(context).setAutoUpdate(value)
    
    fun getUpdateChannel(context: Context): Int = getInstance(context).getUpdateChannel()
    fun setUpdateChannel(context: Context, value: Int) = getInstance(context).setUpdateChannel(value)
    
    fun serializeHistoryToJson(list: List<HistoryItem>): String = PreferenceManagerImpl.serializeHistoryToJson(list)
    fun parseHistoryJson(jsonString: String): List<HistoryItem> = PreferenceManagerImpl.parseHistoryJson(jsonString)
}

internal class PreferenceManagerImpl(context: Context) {
    
    private val settingsPrefs: SharedPreferences = 
        context.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
    private val historyPrefs: SharedPreferences = 
        context.getSharedPreferences(PREF_HISTORY, Context.MODE_PRIVATE)
    
    private val _themeSettingsFlow = MutableStateFlow(loadThemeSettings())
    val themeSettingsFlow: StateFlow<ThemeSettings> = _themeSettingsFlow.asStateFlow()
    
    private val _historyFlow = MutableStateFlow<List<HistoryItem>>(loadHistory())
    val historyFlow: StateFlow<List<HistoryItem>> = _historyFlow.asStateFlow()
    
    private fun getBoolean(key: String, default: Boolean = false): Boolean {
        return settingsPrefs.getBoolean(key, default)
    }
    
    private fun putBoolean(key: String, value: Boolean) {
        settingsPrefs.edit { putBoolean(key, value) }
    }
    
    private fun getInt(key: String, default: Int = 0): Int {
        return settingsPrefs.getInt(key, default)
    }
    
    private fun putInt(key: String, value: Int) {
        settingsPrefs.edit { putInt(key, value) }
    }
    
    private fun getString(key: String, default: String = ""): String {
        return settingsPrefs.getString(key, default) ?: default
    }
    
    private fun putString(key: String, value: String) {
        settingsPrefs.edit { putString(key, value) }
    }
    
    private fun remove(key: String) {
        settingsPrefs.edit { remove(key) }
    }
    
    
    private fun loadThemeSettings(): ThemeSettings {
        return ThemeSettings(
            themeMode = getInt(KEY_THEME_MODE, 0),
            dynamicColor = getBoolean(KEY_DYNAMIC_COLOR, true),
            seedColor = getInt(KEY_SEED_COLOR, 0xd40054),
            highContrast = getBoolean(KEY_HIGH_CONTRAST, false),
            paletteStyleIndex = getInt(KEY_PALETTE_STYLE, 0)
        )
    }
    
    fun updateThemeSettings(update: (ThemeSettings) -> ThemeSettings) {
        val newSettings = update(_themeSettingsFlow.value)
        _themeSettingsFlow.value = newSettings
        
        putInt(KEY_THEME_MODE, newSettings.themeMode)
        putBoolean(KEY_DYNAMIC_COLOR, newSettings.dynamicColor)
        putInt(KEY_SEED_COLOR, newSettings.seedColor)
        putBoolean(KEY_HIGH_CONTRAST, newSettings.highContrast)
        putInt(KEY_PALETTE_STYLE, newSettings.paletteStyleIndex)
    }
    
    fun getAutoPlay(): Boolean = getBoolean(KEY_AUTO_PLAY, true)
    fun setAutoPlay(value: Boolean) = putBoolean(KEY_AUTO_PLAY, value)
    
    fun getRememberPosition(): Boolean = getBoolean(KEY_REMEMBER_POSITION, true)
    fun setRememberPosition(value: Boolean) = putBoolean(KEY_REMEMBER_POSITION, value)
    
    fun getAutoHideControls(): Boolean = getBoolean(KEY_AUTO_HIDE_CONTROLS, true)
    fun setAutoHideControls(value: Boolean) = putBoolean(KEY_AUTO_HIDE_CONTROLS, value)
    
    fun getEnablePip(): Boolean = getBoolean(KEY_ENABLE_PIP, true)
    fun setEnablePip(value: Boolean) = putBoolean(KEY_ENABLE_PIP, value)
    
    fun getBackgroundPlay(): Boolean = getBoolean(KEY_BACKGROUND_PLAY, false)
    fun setBackgroundPlay(value: Boolean) = putBoolean(KEY_BACKGROUND_PLAY, value)
    
    fun getHardwareAcceleration(): Boolean = getBoolean(KEY_HARDWARE_ACCELERATION, true)
    fun setHardwareAcceleration(value: Boolean) = putBoolean(KEY_HARDWARE_ACCELERATION, value)
    
    fun getAspectRatio(): Int = getInt(KEY_ASPECT_RATIO, 3)
    fun setAspectRatio(value: Int) = putInt(KEY_ASPECT_RATIO, value)
    
    fun getDecoderType(): Int = getInt(KEY_DECODER_TYPE, 0)
    fun setDecoderType(value: Int) = putInt(KEY_DECODER_TYPE, value)
    
    fun getMirrorFlip(): Boolean = getBoolean(KEY_MIRROR_FLIP, false)
    fun setMirrorFlip(value: Boolean) = putBoolean(KEY_MIRROR_FLIP, value)
    
    fun getThemeMode(): Int = getInt(KEY_THEME_MODE, 0)
    fun setThemeMode(value: Int) = updateThemeSettings { it.copy(themeMode = value) }
    
    fun getDynamicColor(): Boolean = getBoolean(KEY_DYNAMIC_COLOR, true)
    fun setDynamicColor(value: Boolean) = updateThemeSettings { it.copy(dynamicColor = value) }
    
    fun getSeedColor(): Int = getInt(KEY_SEED_COLOR, 0xd40054)
    fun setSeedColor(value: Int) = updateThemeSettings { it.copy(seedColor = value) }
    
    fun getPaletteStyle(): Int = getInt(KEY_PALETTE_STYLE, 0)
    fun setPaletteStyle(value: Int) = updateThemeSettings { it.copy(paletteStyleIndex = value) }
    
    fun getHighContrast(): Boolean = getBoolean(KEY_HIGH_CONTRAST, false)
    fun setHighContrast(value: Boolean) = updateThemeSettings { it.copy(highContrast = value) }
    
    fun getLanguageTag(): String? {
        return getString(KEY_LANGUAGE_TAG, "").takeIf { it.isNotEmpty() }
    }
    
    fun setLanguageTag(value: String?) {
        if (value != null) {
            putString(KEY_LANGUAGE_TAG, value)
        } else {
            remove(KEY_LANGUAGE_TAG)
        }
    }
    
    fun getLocaleFromPreference(): Locale? {
        return getLanguageTag()?.let { Locale.forLanguageTag(it) }
    }
    
    fun saveLocalePreference(locale: Locale?) {
        if (locale != null) {
            setLanguageTag(normalizeLocaleTag(locale))
        } else {
            setLanguageTag(null)
        }
    }
    
    fun getProxyEnabled(): Boolean = getBoolean(KEY_PROXY_ENABLED, false)
    fun setProxyEnabled(value: Boolean) = putBoolean(KEY_PROXY_ENABLED, value)
    
    fun getProxyHost(): String = getString(KEY_PROXY_HOST, "")
    fun setProxyHost(value: String) = putString(KEY_PROXY_HOST, value)
    
    fun getProxyPort(): Int = getInt(KEY_PROXY_PORT, 8080)
    fun setProxyPort(value: Int) = putInt(KEY_PROXY_PORT, value)
    
    fun getProxyUsername(): String = getString(KEY_PROXY_USERNAME, "")
    fun setProxyUsername(value: String) = putString(KEY_PROXY_USERNAME, value)
    
    fun getProxyPassword(): String = getString(KEY_PROXY_PASSWORD, "")
    fun setProxyPassword(value: String) = putString(KEY_PROXY_PASSWORD, value)
    
    fun getShowPlayerStats(): Boolean = getBoolean(KEY_SHOW_PLAYER_STATS, false)
    fun setShowPlayerStats(value: Boolean) = putBoolean(KEY_SHOW_PLAYER_STATS, value)
    
    fun setPendingNavigation(navigation: String) = putString(KEY_PENDING_NAVIGATION, navigation)
    fun getPendingNavigation(): String = getString(KEY_PENDING_NAVIGATION, "")
    fun clearPendingNavigation() = remove(KEY_PENDING_NAVIGATION)
    
    fun getAutoUpdate(): Boolean = getBoolean(KEY_AUTO_UPDATE, false)
    fun setAutoUpdate(value: Boolean) = putBoolean(KEY_AUTO_UPDATE, value)
    
    fun getUpdateChannel(): Int = getInt(KEY_UPDATE_CHANNEL, 0)
    fun setUpdateChannel(value: Int) = putInt(KEY_UPDATE_CHANNEL, value)
    
    private fun loadHistory(): List<HistoryItem> {
        val jsonString = historyPrefs.getString(KEY_HISTORY, "[]") ?: "[]"
        return parseHistoryJson(jsonString)
    }
    
    fun getHistory(): List<HistoryItem> = _historyFlow.value
    
    fun addOrUpdateHistory(url: String, name: String, lastChannelUrl: String? = null, lastChannelId: String? = null) {
        val currentList = _historyFlow.value.toMutableList()
        val iterator = currentList.iterator()
        var existingItem: HistoryItem? = null
        
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.url == url) {
                existingItem = item
                iterator.remove()
                break
            }
        }

        val newItem = HistoryItem(
            url = url,
            name = name,
            lastChannelUrl = lastChannelUrl ?: existingItem?.lastChannelUrl,
            lastChannelId = lastChannelId ?: existingItem?.lastChannelId,
            timestamp = System.currentTimeMillis()
        )
        
        currentList.add(0, newItem)
        
        if (currentList.size > PlayerConstants.MAX_HISTORY_SIZE) {
            currentList.removeAt(currentList.size - 1)
        }

        _historyFlow.value = currentList
        saveHistory(currentList)
    }
    
    private fun saveHistory(list: List<HistoryItem>) {
        val jsonString = serializeHistoryToJson(list)
        historyPrefs.edit { putString(KEY_HISTORY, jsonString) }
    }
    
    fun clearHistory() {
        _historyFlow.value = emptyList()
        historyPrefs.edit { remove(KEY_HISTORY) }
    }
    
    fun deleteHistoryItem(item: HistoryItem) {
        val currentList = _historyFlow.value.toMutableList()
        currentList.removeAll { it.url == item.url }
        _historyFlow.value = currentList
        saveHistory(currentList)
    }
    
    companion object {
        private const val PREF_SETTINGS = "astronia_settings"
        private const val PREF_HISTORY = "astronia_history"
        private const val KEY_AUTO_PLAY = "auto_play"
        private const val KEY_REMEMBER_POSITION = "remember_position"
        private const val KEY_AUTO_HIDE_CONTROLS = "auto_hide_controls"
        private const val KEY_ENABLE_PIP = "enable_pip"
        private const val KEY_BACKGROUND_PLAY = "background_play"
        private const val KEY_HARDWARE_ACCELERATION = "hardware_acceleration"
        private const val KEY_ASPECT_RATIO = "aspect_ratio"
        private const val KEY_DECODER_TYPE = "decoder_type"
        private const val KEY_MIRROR_FLIP = "mirror_flip"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LANGUAGE_TAG = "language_tag"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_SEED_COLOR = "seed_color"
        private const val KEY_PALETTE_STYLE = "palette_style"
        private const val KEY_HIGH_CONTRAST = "high_contrast"
        private const val KEY_PROXY_ENABLED = "proxy_enabled"
        private const val KEY_PROXY_HOST = "proxy_host"
        private const val KEY_PROXY_PORT = "proxy_port"
        private const val KEY_PROXY_USERNAME = "proxy_username"
        private const val KEY_PROXY_PASSWORD = "proxy_password"
        private const val KEY_SHOW_PLAYER_STATS = "show_player_stats"
        private const val KEY_PENDING_NAVIGATION = "pending_navigation"
        private const val KEY_AUTO_UPDATE = "auto_update"
        private const val KEY_UPDATE_CHANNEL = "update_channel"
        private const val KEY_HISTORY = "history_list"
        
        fun serializeHistoryToJson(list: List<HistoryItem>): String {
            val jsonArray = JSONArray()
            list.forEach { item ->
                val obj = JSONObject()
                obj.put("url", item.url)
                obj.put("name", item.name)
                obj.put("lastChannelUrl", item.lastChannelUrl ?: "")
                obj.put("lastChannelId", item.lastChannelId ?: "")
                obj.put("timestamp", item.timestamp)
                jsonArray.put(obj)
            }
            return jsonArray.toString()
        }
        
        fun parseHistoryJson(jsonString: String): List<HistoryItem> {
            val list = mutableListOf<HistoryItem>()
            try {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        HistoryItem(
                            url = obj.getString("url"),
                            name = obj.optString("name", "Unknown"),
                            lastChannelUrl = obj.optString("lastChannelUrl").takeIf { it.isNotEmpty() },
                            lastChannelId = obj.optString("lastChannelId").takeIf { it.isNotEmpty() },
                            timestamp = obj.optLong("timestamp", 0)
                        )
                    )
                }
            } catch (e: Exception) {
                ErrorHandler.logError("PreferenceManager", "Failed to parse history JSON", e)
            }
            return list.sortedByDescending { it.timestamp }
        }
    }
}

data class ThemeSettings(
    val themeMode: Int = 0,
    val dynamicColor: Boolean = true,
    val seedColor: Int = 0xd40054,
    val highContrast: Boolean = false,
    val paletteStyleIndex: Int = 0
)

data class HistoryItem(
    val url: String,
    val name: String,
    val lastChannelUrl: String? = null,
    val lastChannelId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
