package com.antoniegil.astronia.ui.page.settings.data

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
    
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val (success, count) = withContext(Dispatchers.IO) {
                    DataManager.restoreHistory(context, it)
                }
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(
                            context,
                            resources.getQuantityString(R.plurals.restore_success, count, count),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            resources.getString(R.string.restore_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

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
                    title = stringResource(R.string.backup_data),
                    description = stringResource(R.string.backup_data_desc),
                    icon = Icons.AutoMirrored.Outlined.DriveFileMove,
                    onClick = {
                        if (!isBackingUp) {
                            isBackingUp = true
                            scope.launch {
                                val (success, path) = withContext(Dispatchers.IO) {
                                    DataManager.backupHistory(context)
                                }
                                withContext(Dispatchers.Main) {
                                    isBackingUp = false
                                    if (success && path != null) {
                                        Toast.makeText(
                                            context,
                                            resources.getString(R.string.backup_success, path),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else if (path == null && !success) {
                                        Toast.makeText(
                                            context,
                                            resources.getString(R.string.no_history_to_backup),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            resources.getString(R.string.backup_failed),
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
                        restoreLauncher.launch("application/json")
                    }
                )
            }
        }
    }
}
