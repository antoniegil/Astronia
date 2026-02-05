package com.antoniegil.astronia.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.antoniegil.astronia.ui.theme.monet.LocalTonalPalettes
import com.antoniegil.astronia.ui.theme.monet.dynamicColorScheme
import com.kyant.monet.PaletteStyle
import com.kyant.monet.TonalPalettes.Companion.toTonalPalettes

val LocalBackgroundPlay = compositionLocalOf { false }
val LocalProxyEnabled = compositionLocalOf { false }
val LocalProxyHost = compositionLocalOf { "" }
val LocalProxyPort = compositionLocalOf { 8080 }

val LocalThemeMode = staticCompositionLocalOf { 0 }
val LocalSeedColor = compositionLocalOf { 0xd40054 }
val LocalPaletteStyleIndex = compositionLocalOf { 0 }
val LocalDynamicColorSwitch = compositionLocalOf { false }
val LocalFixedColorRoles = staticCompositionLocalOf {
    FixedColorRoles.fromColorSchemes(
        lightColors = androidx.compose.material3.lightColorScheme(),
        darkColors = androidx.compose.material3.darkColorScheme(),
    )
}

val paletteStyles = listOf(
    PaletteStyle.TonalSpot,
    PaletteStyle.Spritz,
    PaletteStyle.FruitSalad,
    PaletteStyle.Vibrant,
    PaletteStyle.Monochrome
)

@Composable
fun SettingsProvider(
    themeMode: Int = 0,
    dynamicColor: Boolean = true,
    seedColor: Int = 0xd40054,
    paletteStyleIndex: Int = 0,
    isHighContrastModeEnabled: Boolean = false,
    backgroundPlay: Boolean = false,
    proxyEnabled: Boolean = false,
    proxyHost: String = "",
    proxyPort: Int = 8080,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemInDarkTheme = isSystemInDarkTheme()
    
    val isDarkTheme = when (themeMode) {
        1 -> false
        2 -> true
        else -> systemInDarkTheme
    }
    
    val tonalPalettes = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDarkTheme) {
            androidx.compose.material3.dynamicDarkColorScheme(context)
        } else {
            androidx.compose.material3.dynamicLightColorScheme(context)
        }.toTonalPalettes()
    } else {
        Color(seedColor).toTonalPalettes(
            paletteStyles.getOrElse(paletteStyleIndex) { PaletteStyle.TonalSpot }
        )
    }
    
    val fixedColorRoles = FixedColorRoles.fromTonalPalettes(tonalPalettes)
    
    CompositionLocalProvider(
        LocalThemeMode provides themeMode,
        LocalSeedColor provides seedColor,
        LocalPaletteStyleIndex provides paletteStyleIndex,
        LocalDynamicColorSwitch provides dynamicColor,
        LocalTonalPalettes provides tonalPalettes,
        LocalFixedColorRoles provides fixedColorRoles,
        LocalBackgroundPlay provides backgroundPlay,
        LocalProxyEnabled provides proxyEnabled,
        LocalProxyHost provides proxyHost,
        LocalProxyPort provides proxyPort
    ) {
        AstroniaTheme(
            isDarkTheme = isDarkTheme,
            isHighContrastModeEnabled = isHighContrastModeEnabled,
            content = content
        )
    }
}

@Composable
fun AstroniaTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    isHighContrastModeEnabled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current

    LaunchedEffect(isDarkTheme) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (isDarkTheme) {
                view.windowInsetsController?.setSystemBarsAppearance(
                    0,
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                )
            } else {
                view.windowInsetsController?.setSystemBarsAppearance(
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                )
            }
        }
    }

    val tonalPalettes = LocalTonalPalettes.current
    
    val colorScheme = dynamicColorScheme(!isDarkTheme).run {
        if (isHighContrastModeEnabled && isDarkTheme) {
            copy(
                surface = Color.Black,
                background = Color.Black,
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = surfaceContainerLowest,
                surfaceContainer = surfaceContainerLow,
                surfaceContainerHigh = surfaceContainerLow,
                surfaceContainerHighest = surfaceContainer,
            )
        } else {
            this
        }
    }

    val textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
        lineBreak = androidx.compose.ui.text.style.LineBreak.Paragraph,
        textDirection = androidx.compose.ui.text.style.TextDirection.Content,
    )

    CompositionLocalProvider(
        LocalFixedColorRoles provides FixedColorRoles.fromTonalPalettes(tonalPalettes),
        androidx.compose.material3.LocalTextStyle provides textStyle,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content,
        )
    }
}
