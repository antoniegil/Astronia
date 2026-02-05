package com.antoniegil.astronia.ui.page.settings.appearance

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antoniegil.astronia.R
import com.antoniegil.astronia.ui.component.BackButton
import com.antoniegil.astronia.ui.component.HorizontalDivider
import com.antoniegil.astronia.ui.component.PreferenceSingleChoiceItem
import com.antoniegil.astronia.ui.component.PreferenceSubtitle
import com.antoniegil.astronia.util.SettingsManager
import com.antoniegil.astronia.util.SupportedLocales
import com.antoniegil.astronia.util.toDisplayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePage(
    onNavigateBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    
    val supportedLocales = SupportedLocales.sortedBy { it.toLanguageTag() }
    
    val currentLocale = SettingsManager.getLocaleFromPreference(context)
    var selectedLocale by remember { mutableStateOf(currentLocale) }

    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    } else {
        Intent()
    }

    val isSystemLocaleSettingsAvailable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL).isNotEmpty()
    } else {
        false
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.language)) },
                navigationIcon = { BackButton(onNavigateBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues
        ) {
            item {
                PreferenceSubtitle(text = stringResource(R.string.select_language))
            }
            
            item {
                PreferenceSingleChoiceItem(
                    text = stringResource(R.string.follow_system),
                    selected = selectedLocale == null,
                    onClick = {
                        selectedLocale = null
                        SettingsManager.saveLocalePreference(context, null)
                        com.antoniegil.astronia.util.setLanguage(null)
                    }
                )
            }
            
            items(supportedLocales.size) { index ->
                val locale = supportedLocales[index]
                PreferenceSingleChoiceItem(
                    text = locale.toDisplayName(),
                    selected = selectedLocale == locale,
                    onClick = {
                        selectedLocale = locale
                        SettingsManager.saveLocalePreference(context, locale)
                        com.antoniegil.astronia.util.setLanguage(locale)
                    }
                )
            }

            if (isSystemLocaleSettingsAvailable) {
                item {
                    HorizontalDivider()
                    Surface(
                        modifier = Modifier.clickable {
                            context.startActivity(intent)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(PaddingValues(horizontal = 12.dp, vertical = 18.dp)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.system_settings),
                                    maxLines = 1,
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
