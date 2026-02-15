package com.antoniegil.astronia.ui.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.antoniegil.astronia.R
import com.antoniegil.astronia.ui.common.HapticFeedback.slightHapticFeedback
import com.antoniegil.astronia.ui.component.SearchBar
import com.antoniegil.astronia.ui.component.ChannelCard
import com.antoniegil.astronia.util.HistoryItem
import com.antoniegil.astronia.util.HistoryManager
import com.antoniegil.astronia.util.DataManager
import com.antoniegil.astronia.util.formatDateTime
import com.antoniegil.astronia.util.rememberBackupExportLauncher
import com.antoniegil.astronia.util.rememberHistoryRestoreLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryPage(
    onBack: () -> Unit,
    onPlay: (HistoryItem) -> Unit,
    onEdit: ((HistoryItem) -> Unit)? = null
) {
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val view = androidx.compose.ui.platform.LocalView.current
    val scope = rememberCoroutineScope()
    val historyList by HistoryManager.historyFlow.collectAsState()
    var selectedItems by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedItems.isNotEmpty()
    var showMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val filteredHistoryList = remember(historyList, searchQuery) {
        if (searchQuery.isEmpty()) {
            historyList
        } else {
            historyList.filter { item ->
                item.name.contains(searchQuery, ignoreCase = true) ||
                        item.url.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val checkBoxState by remember(selectedItems, filteredHistoryList) {
        derivedStateOf {
            when {
                selectedItems.isEmpty() -> ToggleableState.Off
                selectedItems.size == filteredHistoryList.size && selectedItems.isNotEmpty() -> ToggleableState.On
                else -> ToggleableState.Indeterminate
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        HistoryManager.getHistory(context)
    }

    LaunchedEffect(historyList.isEmpty()) {
        if (historyList.isEmpty()) {
            isSearching = false
            searchQuery = ""
        }
    }

    var backupContent by remember { mutableStateOf("") }

    val launchExport = rememberBackupExportLauncher(backupContent)
    
    val (restoreLauncher, restoreMimeType) = rememberHistoryRestoreLauncher { success, count ->
        if (success) {
            HistoryManager.getHistory(context)
        }
    }

    val scrollBehavior = if (historyList.isNotEmpty()) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
            canScroll = { true }
        )
    } else {
        TopAppBarDefaults.pinnedScrollBehavior()
    }

    androidx.activity.compose.BackHandler(onBack = {
        if (isSelectionMode) {
            selectedItems = emptySet()
        } else {
            onBack()
        }
    })

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(stringResource(R.string.history))
                },
                navigationIcon = {
                    IconButton(onClick = {
                        view.slightHapticFeedback()
                        if (isSelectionMode) {
                            selectedItems = emptySet()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (!isSelectionMode) {
                        if (historyList.isNotEmpty()) {
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
                                if (historyList.isNotEmpty()) {
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(Icons.AutoMirrored.Outlined.DriveFileMove, null)
                                        },
                                        text = { Text(stringResource(R.string.export)) },
                                        onClick = {
                                            showMenu = false
                                            scope.launch(Dispatchers.IO) {
                                                val content = DataManager.prepareBackupContent(context)
                                                withContext(Dispatchers.Main) {
                                                    if (content != null) {
                                                        backupContent = content
                                                        launchExport()
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            resources.getString(R.string.no_history_to_export),
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Restore, null)
                                    },
                                    text = { Text(stringResource(R.string.imports)) },
                                    onClick = {
                                        showMenu = false
                                        restoreLauncher.launch(restoreMimeType)
                                    }
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior
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
                                    filteredHistoryList.map { it.url }.toSet()
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
                            selectedItems.forEach { url ->
                                historyList.find { it.url == url }?.let { item ->
                                    HistoryManager.deleteHistoryItem(context, item)
                                }
                            }
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
                    placeholderText = stringResource(R.string.search_history),
                    onValueChange = { searchQuery = it }
                )
            }

            if (filteredHistoryList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_play_history),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredHistoryList, key = { it.url }) { item ->
                        ChannelCard(
                            onDelete = {
                                scope.launch {
                                    HistoryManager.deleteHistoryItemWithUndo(
                                        context,
                                        item,
                                        snackbarHostState,
                                        resources
                                    )
                                }
                            },
                            onClick = {
                                if (isSelectionMode) {
                                    selectedItems = if (selectedItems.contains(item.url)) {
                                        selectedItems - item.url
                                    } else {
                                        selectedItems + item.url
                                    }
                                } else {
                                    onPlay(item)
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    selectedItems = setOf(item.url)
                                }
                            },
                            onEdit = if (!isSelectionMode && onEdit != null) {
                                { onEdit(item) }
                            } else null,
                            name = item.name,
                            logoUrl = item.logoUrl,
                            url = item.url,
                            tags = listOf("${stringResource(R.string.last_played)}: ${item.timestamp.formatDateTime()}"),
                            leadingIcon = if (isSelectionMode) {
                                {
                                    Icon(
                                        imageVector = if (selectedItems.contains(item.url)) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = if (selectedItems.contains(item.url)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                }
                            } else null,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.play),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}