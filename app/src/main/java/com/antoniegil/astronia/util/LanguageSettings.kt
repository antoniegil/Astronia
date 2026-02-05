package com.antoniegil.astronia.util

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.core.os.LocaleListCompat
import java.util.Locale

val SupportedLocales = listOf(
    Locale.forLanguageTag("ar"),
    Locale.forLanguageTag("ar-SA"),
    Locale.forLanguageTag("bn"),
    Locale.forLanguageTag("cs"),
    Locale.forLanguageTag("de"),
    Locale.forLanguageTag("en"),
    Locale.forLanguageTag("es"),
    Locale.forLanguageTag("fa"),
    Locale.forLanguageTag("fil"),
    Locale.forLanguageTag("fr"),
    Locale.forLanguageTag("he"),
    Locale.forLanguageTag("hi"),
    Locale.forLanguageTag("id"),
    Locale.forLanguageTag("it"),
    Locale.forLanguageTag("ja"),
    Locale.forLanguageTag("ko"),
    Locale.forLanguageTag("ms"),
    Locale.forLanguageTag("nl"),
    Locale.forLanguageTag("pl"),
    Locale.forLanguageTag("pt"),
    Locale.forLanguageTag("pt-BR"),
    Locale.forLanguageTag("ro"),
    Locale.forLanguageTag("ru"),
    Locale.forLanguageTag("sv"),
    Locale.forLanguageTag("th"),
    Locale.forLanguageTag("tr"),
    Locale.forLanguageTag("uk"),
    Locale.forLanguageTag("vi"),
    Locale.forLanguageTag("zh-CN"),
    Locale.forLanguageTag("zh-HK"),
    Locale.forLanguageTag("zh-TW")
)

private val customDisplayNames = mapOf(
    "zh-CN" to "简体中文",
    "zh-HK" to "粵語",
    "zh-TW" to "正體中文"
)

@Composable
fun Locale?.toDisplayName(): String {
    if (this == null) {
        val systemLocale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
        return systemLocale.getDisplayName(systemLocale)
    }
    return customDisplayNames[this.toLanguageTag()] ?: this.getDisplayName(this)
}

fun setLanguage(locale: Locale?) {
    val localeList = if (locale != null) LocaleListCompat.create(locale) else LocaleListCompat.getEmptyLocaleList()
    AppCompatDelegate.setApplicationLocales(localeList)
}

fun normalizeLocaleTag(locale: Locale): String {
    val tag = locale.toLanguageTag()
    
    SupportedLocales.sortedByDescending { it.toLanguageTag().length }.forEach { supportedLocale ->
        if (tag.startsWith(supportedLocale.toLanguageTag())) return supportedLocale.toLanguageTag()
    }
    
    return when {
        tag.startsWith("he") || tag.startsWith("iw") -> "he"
        tag == "zh-MO" -> "zh-HK"
        else -> tag
    }
}

fun Context.applyLanguage(locale: Locale?): Context {
    val config = Configuration(resources.configuration)
    
    if (locale != null) {
        Locale.setDefault(locale)
        config.setLocale(locale)
        val localeList = LocaleList(locale)
        LocaleList.setDefault(localeList)
        config.setLocales(localeList)
    }
    
    return createConfigurationContext(config)
}

class LanguageContextWrapper(base: Context) : ContextWrapper(base) {
    companion object {
        fun wrap(context: Context, locale: Locale?): ContextWrapper {
            if (locale == null) {
                return LanguageContextWrapper(context)
            }
            
            val config = Configuration(context.resources.configuration)
            Locale.setDefault(locale)
            
            config.setLocale(locale)
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)
            
            val newContext = context.createConfigurationContext(config)
            
            return LanguageContextWrapper(newContext)
        }
    }
}
