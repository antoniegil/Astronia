package com.antoniegil.astronia.ui.page.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.core.net.toUri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.antoniegil.astronia.R
import com.antoniegil.astronia.ui.component.BackButton
import com.antoniegil.astronia.ui.component.SettingItem
import com.antoniegil.astronia.ui.component.PreferencesHintCard

@SuppressLint("BatteryLife")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit = {},
    onNavigateToVideo: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToDataManagement: () -> Unit = {},
    onNavigateToProxy: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {}
) {
    val context = LocalContext.current
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    var showBatteryHint by remember {
        mutableStateOf(!pm.isIgnoringBatteryOptimizations(context.packageName))
    }
    
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = "package:${context.packageName}".toUri()
    }
    val isActivityAvailable: Boolean = if (Build.VERSION.SDK_INT < 33) {
        context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL).isNotEmpty()
    } else {
        context.packageManager.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_SYSTEM_ONLY.toLong())
        ).isNotEmpty()
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            showBatteryHint = !pm.isIgnoringBatteryOptimizations(context.packageName)
        }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    val typography = MaterialTheme.typography

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val overrideTypography = remember(typography) { 
                typography.copy(headlineMedium = typography.displaySmall) 
            }
            
            MaterialTheme(typography = overrideTypography) {
                LargeTopAppBar(
                    title = { Text(text = stringResource(R.string.settings)) },
                    navigationIcon = { BackButton(onNavigateBack) },
                    scrollBehavior = scrollBehavior,
                    expandedHeight = TopAppBarDefaults.LargeAppBarExpandedHeight + 24.dp,
                )
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier,
            contentPadding = it
        ) {
            item {
                AnimatedVisibility(
                    visible = showBatteryHint && isActivityAvailable,
                    exit = shrinkVertically() + fadeOut()
                ) {
                    PreferencesHintCard(
                        title = stringResource(R.string.battery_configuration),
                        icon = Icons.Rounded.EnergySavingsLeaf,
                        description = stringResource(R.string.battery_configuration_desc)
                    ) {
                        launcher.launch(intent)
                        showBatteryHint = !pm.isIgnoringBatteryOptimizations(context.packageName)
                    }
                }
            }
            
            item {
                SettingItem(
                    title = stringResource(R.string.player),
                    description = stringResource(R.string.player_desc),
                    icon = Icons.Rounded.PlayArrow,
                    onClick = onNavigateToPlayer
                )
            }
            item {
                SettingItem(
                    title = stringResource(R.string.video),
                    description = stringResource(R.string.video_desc),
                    icon = Icons.Rounded.VideoSettings,
                    onClick = onNavigateToVideo
                )
            }
            item {
                SettingItem(
                    title = stringResource(R.string.appearance),
                    description = stringResource(R.string.appearance_desc),
                    icon = Icons.Rounded.Palette,
                    onClick = onNavigateToAppearance
                )
            }
            item {
                SettingItem(
                    title = stringResource(R.string.data_management),
                    description = stringResource(R.string.data_management_desc),
                    icon = Icons.Rounded.Storage,
                    onClick = onNavigateToDataManagement
                )
            }
            item {
                SettingItem(
                    title = stringResource(R.string.network),
                    description = stringResource(R.string.network_desc),
                    icon = Icons.Rounded.SignalWifi4Bar,
                    onClick = onNavigateToProxy
                )
            }
            item {
                SettingItem(
                    title = stringResource(R.string.about),
                    description = stringResource(R.string.about_desc),
                    icon = Icons.Rounded.Info,
                    onClick = onNavigateToAbout
                )
            }
        }
    }
}