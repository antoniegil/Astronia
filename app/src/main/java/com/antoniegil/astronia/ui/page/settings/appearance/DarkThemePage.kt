package com.antoniegil.astronia.ui.page.settings.appearance

import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.antoniegil.astronia.R
import com.antoniegil.astronia.ui.component.BackButton
import com.antoniegil.astronia.ui.component.PreferenceSingleChoiceItem
import com.antoniegil.astronia.ui.component.PreferenceSubtitle
import com.antoniegil.astronia.ui.component.PreferenceSwitchVariant
import com.antoniegil.astronia.util.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkThemePage(
    onNavigateBack: () -> Unit,
    onThemeChanged: (Int) -> Unit = {},
    onHighContrastChanged: (Boolean) -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val currentTheme = remember { mutableIntStateOf(SettingsManager.getThemeMode(context)) }
    var highContrastEnabled by remember { mutableStateOf(SettingsManager.getHighContrast(context)) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.dark_theme)) },
                navigationIcon = { BackButton(onNavigateBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                item {
                    PreferenceSingleChoiceItem(
                        text = stringResource(R.string.follow_system),
                        selected = currentTheme.intValue == 0,
                        onClick = {
                            currentTheme.intValue = 0
                            onThemeChanged(0)
                        }
                    )
                }
            }

            item {
                PreferenceSingleChoiceItem(
                    text = stringResource(R.string.open),
                    selected = currentTheme.intValue == 2,
                    onClick = {
                        currentTheme.intValue = 2
                        onThemeChanged(2)
                    }
                )
            }

            item {
                PreferenceSingleChoiceItem(
                    text = stringResource(R.string.close),
                    selected = currentTheme.intValue == 1,
                    onClick = {
                        currentTheme.intValue = 1
                        onThemeChanged(1)
                    }
                )
            }

            item {
                PreferenceSubtitle(text = stringResource(R.string.additional_settings))
            }

            item {
                PreferenceSwitchVariant(
                    title = stringResource(R.string.high_contrast),
                    icon = Icons.Outlined.Contrast,
                    isChecked = highContrastEnabled,
                    onClick = {
                        highContrastEnabled = !highContrastEnabled
                        onHighContrastChanged(highContrastEnabled)
                    }
                )
            }
        }
    }
}
