package com.antoniegil.astronia.ui.page


import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antoniegil.astronia.R
import com.antoniegil.astronia.ui.common.HapticFeedback.slightHapticFeedback
import com.antoniegil.astronia.ui.component.ChannelCard
import com.antoniegil.astronia.util.ErrorHandler
import com.antoniegil.astronia.util.HistoryItem
import com.antoniegil.astronia.util.HistoryManager
import com.antoniegil.astronia.util.formatDateTime
import kotlinx.coroutines.launch

@Composable
private fun HistoryCardContent(item: HistoryItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${stringResource(R.string.last_played)}: ${item.timestamp.formatDateTime()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = stringResource(R.string.play),
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onPlayUrl: (String) -> Unit = {},
    onPlayHistoryItem: (HistoryItem) -> Unit = {}
) {
    var url by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = androidx.compose.ui.platform.LocalResources.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, flag)
            } catch (e: Exception) {
                ErrorHandler.logError("HomePage", "Failed to take persistable URI permission", e)
            }
            url = it.toString()
        }
    }

    val view = LocalView.current

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {},
                    modifier = Modifier.padding(horizontal = 8.dp),
                    navigationIcon = {
                        IconButton(onClick = {
                            view.slightHapticFeedback()
                            onNavigateToSettings()
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.settings)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            view.slightHapticFeedback()
                            onNavigateToHistory()
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Subscriptions,
                                contentDescription = stringResource(R.string.history)
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                Column(
                    modifier = Modifier.padding(6.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                val clipEntry = clipboard.getClipEntry()
                                val clipData = clipEntry?.clipData
                                if (clipData != null && clipData.itemCount > 0) {
                                    val text = clipData.getItemAt(0).text?.toString()
                                    if (!text.isNullOrBlank()) {
                                        url = text
                                    }
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Icon(Icons.Outlined.ContentPaste, stringResource(R.string.paste))
                    }

                    FloatingActionButton(
                        onClick = {
                            if (url.isNotBlank()) {
                                keyboardController?.hide()
                                onPlayUrl(url)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Icon(Icons.Outlined.PlayArrow, stringResource(R.string.play))
                    }
                }
            }
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
                Column(modifier = Modifier.padding(start = 12.dp, top = 24.dp)) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(top = 12.dp, bottom = 3.dp)
                    ) {
                        Text(
                            text = "Astronia",
                            style = MaterialTheme.typography.displaySmall
                        )
                        AnimatedVisibility(visible = isLoading) {
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp)
                ) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(stringResource(R.string.link_hint)) },
                        modifier = Modifier
                            .padding(0.dp, 16.dp)
                            .fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        trailingIcon = {
                            Row {
                                IconButton(onClick = {
                                    filePickerLauncher.launch(arrayOf("audio/x-mpegurl", "application/vnd.apple.mpegurl", "audio/mpegurl"))
                                }) {
                                    Icon(Icons.Outlined.FolderOpen, stringResource(R.string.local_import))
                                }
                                if (url.isNotEmpty()) {
                                    IconButton(onClick = { url = "" }) {
                                        Icon(Icons.Outlined.Cancel, stringResource(R.string.clear))
                                    }
                                }
                            }
                        },
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (url.isNotBlank()) {
                                    keyboardController?.hide()
                                    onPlayUrl(url)
                                }
                            }
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )

                    val historyList by HistoryManager.historyFlow.collectAsState()

                    LaunchedEffect(Unit) {
                        HistoryManager.getHistory(context)
                    }

                    if (historyList.take(3).isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            historyList.take(3).forEach { item ->
                                key(item.url) {
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
                                        onClick = { onPlayHistoryItem(item) }
                                    ) {
                                        HistoryCardContent(item = item)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(160.dp))
                }
            }
        }
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .imePadding()
        )
    }
}
