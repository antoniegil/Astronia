package com.antoniegil.astronia.ui.page.settings.data

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.antoniegil.astronia.R
import com.antoniegil.astronia.ui.component.BackButton
import com.antoniegil.astronia.ui.component.PreferenceItem
import com.antoniegil.astronia.ui.component.PreferenceSubtitle
import com.antoniegil.astronia.util.DataManager
import com.antoniegil.astronia.util.rememberBackupExportLauncher
import com.antoniegil.astronia.util.rememberHistoryRestoreLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementPage(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val scope = rememberCoroutineScope()
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    var isBackingUp by remember { mutableStateOf(false) }
    var backupContent by remember { mutableStateOf("") }
    
    val launchExport = rememberBackupExportLauncher(backupContent) {
        isBackingUp = false
    }
    
    val (restoreLauncher, restoreMimeType) = rememberHistoryRestoreLauncher()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.data_management)) },
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
                PreferenceSubtitle(text = stringResource(R.string.data_backup))
            }
            
            item {
                PreferenceItem(
                    title = stringResource(R.string.export_data),
                    description = stringResource(R.string.export_data_desc),
                    icon = Icons.AutoMirrored.Outlined.DriveFileMove,
                    onClick = {
                        if (!isBackingUp) {
                            isBackingUp = true
                            scope.launch(Dispatchers.IO) {
                                val content = DataManager.prepareBackupContent(context)
                                withContext(Dispatchers.Main) {
                                    if (content != null) {
                                        backupContent = content
                                        launchExport()
                                    } else {
                                        isBackingUp = false
                                        Toast.makeText(
                                            context,
                                            resources.getString(R.string.no_history_to_export),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    }
                )
            }
            
            item {
                PreferenceItem(
                    title = stringResource(R.string.restore_data),
                    description = stringResource(R.string.restore_data_desc),
                    icon = Icons.Outlined.Restore,
                    onClick = {
                        restoreLauncher.launch(restoreMimeType)
                    }
                )
            }
        }
    }
}
