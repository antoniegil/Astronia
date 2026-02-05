package com.antoniegil.astronia.ui.page.settings.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.antoniegil.astronia.R
import com.antoniegil.astronia.ui.component.BackButton
import com.antoniegil.astronia.ui.component.PreferenceSwitch
import com.antoniegil.astronia.ui.component.PreferenceSubtitle
import com.antoniegil.astronia.util.SettingsManager

@SuppressLint("BatteryLife")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettingsPage(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = "package:${context.packageName}".toUri()
    }
    
    var isBatteryOptimized by remember {
        mutableStateOf(!pm.isIgnoringBatteryOptimizations(context.packageName))
    }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isBatteryOptimized = !pm.isIgnoringBatteryOptimizations(context.packageName)
    }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    var autoPlay by remember { mutableStateOf(SettingsManager.getAutoPlay(context)) }
    var autoHideControls by remember { mutableStateOf(SettingsManager.getAutoHideControls(context)) }
    var enablePictureInPicture by remember { mutableStateOf(SettingsManager.getEnablePip(context)) }
    var backgroundPlay by remember { mutableStateOf(SettingsManager.getBackgroundPlay(context)) }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.player)) },
                navigationIcon = { BackButton(onNavigateBack) },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues
        ) {
            item {
                PreferenceSubtitle(text = stringResource(R.string.playback_behavior))
            }
            
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.auto_play),
                    description = stringResource(R.string.auto_play_desc),
                    icon = Icons.Outlined.PlayArrow,
                    isChecked = autoPlay,
                    onCheckedChange = { 
                        autoPlay = it
                        SettingsManager.setAutoPlay(context, it)
                    }
                )
            }
            
            item {
                PreferenceSubtitle(text = stringResource(R.string.interface_control))
            }
            
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.auto_hide_controls),
                    description = stringResource(R.string.auto_hide_controls_desc),
                    icon = Icons.Outlined.VisibilityOff,
                    isChecked = autoHideControls,
                    onCheckedChange = { 
                        autoHideControls = it
                        SettingsManager.setAutoHideControls(context, it)
                    }
                )
            }
            
            item {
                val isPipSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                PreferenceSwitch(
                    title = stringResource(R.string.pip),
                    description = if (isPipSupported) {
                        stringResource(R.string.pip_desc)
                    } else {
                        stringResource(R.string.pip_unsupported)
                    },
                    icon = Icons.Outlined.PictureInPicture,
                    isChecked = enablePictureInPicture,
                    enabled = isPipSupported,
                    onCheckedChange = { 
                        enablePictureInPicture = it
                        SettingsManager.setEnablePip(context, it)
                    }
                )
            }
            
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.background_play),
                    description = if (isBatteryOptimized) {
                        stringResource(R.string.background_play_battery_hint)
                    } else {
                        stringResource(R.string.background_play_desc)
                    },
                    icon = Icons.Outlined.PlayCircle,
                    isChecked = backgroundPlay,
                    enabled = !isBatteryOptimized,
                    onClick = if (isBatteryOptimized) {
                        { launcher.launch(intent) }
                    } else null,
                    onCheckedChange = { 
                        backgroundPlay = it
                        SettingsManager.setBackgroundPlay(context, it)
                    }
                )
            }
        }
    }
}
