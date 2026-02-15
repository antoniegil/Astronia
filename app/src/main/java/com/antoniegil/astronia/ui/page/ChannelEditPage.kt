package com.antoniegil.astronia.ui.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.antoniegil.astronia.R
import com.antoniegil.astronia.ui.common.HapticFeedback.slightHapticFeedback
import com.antoniegil.astronia.ui.component.ChannelCard
import com.antoniegil.astronia.ui.component.SearchBar
import com.antoniegil.astronia.util.DataManager
import com.antoniegil.astronia.util.HistoryItem
import com.antoniegil.astronia.util.M3U8Channel
import com.antoniegil.astronia.util.rememberM3U8SaveAsLauncher
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelEditPage(
    historyItem: HistoryItem,
    channels: List<M3U8Channel>,
    onBack: () -> Unit
) {
    LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val view = LocalView.current
    val itemDeletedMsg = stringResource(R.string.item_deleted)
    val undoMsg = stringResource(R.string.undo)
    var editableChannels by remember { mutableStateOf(channels.toList()) }
    var displayedCount by remember { mutableIntStateOf(100) }
    var selectedItems by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedItems.isNotEmpty()
    var showMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(channels.isEmpty()) }
    var hasChanges by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        editableChannels = editableChannels.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        hasChanges = true
    }
    
    LaunchedEffect(channels) {
        if (channels.isNotEmpty()) {
            editableChannels = channels.toList()
            displayedCount = minOf(100, channels.size)
            isLoading = false
        }
    }
    
    val filteredChannels = remember(editableChannels, searchQuery) {
        if (searchQuery.isEmpty()) {
            editableChannels
        } else {
            editableChannels.filter { channel ->
                channel.name.contains(searchQuery, ignoreCase = true) ||
                        channel.url.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    val displayedChannels = remember(filteredChannels, displayedCount) {
        filteredChannels.take(displayedCount)
    }
    
    val checkBoxState by remember(selectedItems, filteredChannels) {
        derivedStateOf {
            when {
                selectedItems.isEmpty() -> ToggleableState.Off
                selectedItems.size == filteredChannels.size && selectedItems.isNotEmpty() -> ToggleableState.On
                else -> ToggleableState.Indeterminate
            }
        }
    }
    
    androidx.activity.compose.BackHandler(onBack = {
        if (isSelectionMode) {
            selectedItems = emptySet()
        } else if (hasChanges) {
            showExitDialog = true
        } else {
            onBack()
        }
    })    
    val isLocalFile = historyItem.url.startsWith("file://") || 
                      historyItem.url.startsWith("content://")
    
    val m3u8Content = remember(editableChannels) {
        DataManager.generateM3U8Content(editableChannels)
    }
    
    val context = LocalContext.current
    val saveSuccess = stringResource(R.string.save_success)
    val failed = stringResource(R.string.export_failed)

    val launchSaveAs = rememberM3U8SaveAsLauncher(
        m3u8Content = m3u8Content,
        onComplete = { hasChanges = false }
    )
    
    val saveToOriginalFile: () -> Unit = {
        if (historyItem.url.startsWith("file://") || historyItem.url.startsWith("content://")) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val content = DataManager.generateM3U8Content(editableChannels)
                try {
                    if (historyItem.url.startsWith("file://")) {
                        val filePath = historyItem.url.removePrefix("file://")
                        val file = java.io.File(filePath)
                        file.writeText(content)
                    } else {
                        val uri = historyItem.url.toUri()
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(content.toByteArray())
                        }
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            saveSuccess,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        hasChanges = false
                    }
                } catch (_: Exception) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            failed,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.edit_channels))
                        if (isLoading) {
                            Spacer(modifier = Modifier.width(12.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        view.slightHapticFeedback()
                        if (isSelectionMode) {
                            selectedItems = emptySet()
                        } else if (hasChanges) {
                            showExitDialog = true
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (!isSelectionMode) {
                        if (editableChannels.size > 1) {
                            IconButton(onClick = {
                                view.slightHapticFeedback()
                                isSearching = !isSearching
                                if (!isSearching) searchQuery = ""
                            }) {
                                Icon(Icons.Outlined.Search, stringResource(R.string.search))
                            }
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Outlined.MoreVert, null)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                if (isLocalFile) {
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Save, null)
                                        },
                                        text = { Text(stringResource(R.string.save)) },
                                        onClick = {
                                            showMenu = false
                                            if (editableChannels.isEmpty()) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    resources.getString(R.string.no_history_to_save),
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                saveToOriginalFile()
                                            }
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Outlined.DriveFileMove, null)
                                    },
                                    text = { Text(stringResource(R.string.export)) },
                                    onClick = {
                                        showMenu = false
                                        if (editableChannels.isEmpty()) {
                                            android.widget.Toast.makeText(
                                                context,
                                                resources.getString(R.string.no_history_to_export),
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            launchSaveAs()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                BottomAppBar {
                    val selectAllText = stringResource(R.string.select_all)
                    TriStateCheckbox(
                        modifier = Modifier.semantics {
                            this.contentDescription = selectAllText
                        },
                        state = checkBoxState,
                        onClick = {
                            view.slightHapticFeedback()
                            selectedItems = when (checkBoxState) {
                                ToggleableState.On -> emptySet()
                                else -> {
                                    filteredChannels.map { it.id }.toSet()
                                }
                            }
                        }
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = resources.getQuantityString(
                            R.plurals.multiselect_item_count,
                            selectedItems.size,
                            selectedItems.size
                        ),
                        style = MaterialTheme.typography.labelLarge
                    )
                    IconButton(
                        onClick = {
                            view.slightHapticFeedback()
                            selectedItems.forEach { id ->
                                editableChannels.find { it.id == id }?.let { channel ->
                                    editableChannels = editableChannels.toMutableList().apply {
                                        remove(channel)
                                    }
                                }
                            }
                            hasChanges = true
                            selectedItems = emptySet()
                        },
                        enabled = selectedItems.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = stringResource(R.string.delete_history_item)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedVisibility(visible = isSearching) {
                SearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    text = searchQuery,
                    placeholderText = stringResource(R.string.search_channels),
                    onValueChange = { searchQuery = it }
                )
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(displayedChannels, key = { _, item -> item.id }) { _, channel ->
                    ReorderableItem(reorderableLazyListState, key = channel.id) { _ ->
                        ChannelCard(
                            onDelete = {
                                val deleteIndex = editableChannels.indexOfFirst { it.id == channel.id }
                                if (deleteIndex >= 0) {
                                    editableChannels = editableChannels.toMutableList().apply {
                                        removeAt(deleteIndex)
                                    }
                                    hasChanges = true
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = itemDeletedMsg,
                                            actionLabel = undoMsg,
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            editableChannels = editableChannels.toMutableList().apply {
                                                add(deleteIndex.coerceAtMost(size), channel)
                                            }
                                        }
                                    }
                                }
                            },
                            onClick = {
                                if (isSelectionMode) {
                                    selectedItems = if (selectedItems.contains(channel.id)) {
                                        selectedItems - channel.id
                                    } else {
                                        selectedItems + channel.id
                                    }
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    selectedItems = setOf(channel.id)
                                }
                            },
                            name = channel.name,
                            logoUrl = channel.logoUrl,
                            url = channel.url,
                            leadingIcon = {
                                if (isSelectionMode) {
                                    Icon(
                                        imageVector = if (selectedItems.contains(channel.id)) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = if (selectedItems.contains(channel.id)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.DragHandle,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .padding(end = 12.dp)
                                            .draggableHandle(),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        )
                    }
                }

                if (displayedCount < filteredChannels.size) {
                    item {
                        LaunchedEffect(Unit) {
                            displayedCount = minOf(displayedCount + 100, filteredChannels.size)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
    
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.unsaved_changes)) },
            text = { Text(stringResource(R.string.save_changes_prompt)) },
            confirmButton = {
                TextButton(onClick = {
                    if (editableChannels.isEmpty()) {
                        android.widget.Toast.makeText(
                            context,
                            resources.getString(if (isLocalFile) R.string.no_history_to_save else R.string.no_history_to_export),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        showExitDialog = false
                    } else {
                        if (isLocalFile) {
                            saveToOriginalFile()
                        } else {
                            launchSaveAs()
                        }
                    }
                }) {
                    Text(if (isLocalFile) stringResource(R.string.save) else stringResource(R.string.export))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onBack()
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
