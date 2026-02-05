package com.antoniegil.astronia.ui.page.settings.network

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.antoniegil.astronia.R
import com.antoniegil.astronia.ui.component.BackButton
import com.antoniegil.astronia.ui.component.PreferenceSwitch
import com.antoniegil.astronia.ui.component.PreferenceSwitchWithDivider
import com.antoniegil.astronia.util.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkPage(onNavigateBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    
    var proxyEnabled by remember { mutableStateOf(SettingsManager.getProxyEnabled(context)) }
    var showPlayerStats by remember { mutableStateOf(SettingsManager.getShowPlayerStats(context)) }
    var showProxyDialog by remember { mutableStateOf(false) }
    
    val proxyDesc = if (proxyEnabled) {
        val host = SettingsManager.getProxyHost(context)
        val port = SettingsManager.getProxyPort(context)
        if (host.isNotEmpty()) "$host:$port" else stringResource(R.string.proxy_desc)
    } else {
        stringResource(R.string.proxy_desc)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.network)) },
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
                PreferenceSwitchWithDivider(
                    title = stringResource(R.string.proxy),
                    description = proxyDesc,
                    icon = Icons.Outlined.VpnKey,
                    isChecked = proxyEnabled,
                    onChecked = {
                        proxyEnabled = it
                        SettingsManager.setProxyEnabled(context, it)
                    },
                    onClick = { showProxyDialog = true }
                )
            }
            
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.player_stats),
                    description = stringResource(R.string.player_stats_desc),
                    icon = Icons.Outlined.Info,
                    isChecked = showPlayerStats,
                    onCheckedChange = {
                        showPlayerStats = it
                        SettingsManager.setShowPlayerStats(context, it)
                    }
                )
            }
        }
    }
    
    if (showProxyDialog) {
        ProxyConfigurationDialog(
            onDismissRequest = { showProxyDialog = false }
        )
    }
}
