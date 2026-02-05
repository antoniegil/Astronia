package com.antoniegil.astronia.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun LegacyThemeBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        val isDark = isSystemInDarkTheme()
        val backgroundColor = if (isDark) Color(0xFF1C1B1F) else Color(0xFFFFFBFE)
        
        androidx.compose.foundation.layout.Box(
            modifier = modifier.background(backgroundColor)
        ) {
            content()
        }
    } else {
        androidx.compose.foundation.layout.Box(modifier = modifier) {
            content()
        }
    }
}
