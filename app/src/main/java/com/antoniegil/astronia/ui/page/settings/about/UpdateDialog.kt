package com.antoniegil.astronia.ui.page.settings.about

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.antoniegil.astronia.R
import com.antoniegil.astronia.util.UpdateUtil
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun UpdateDialog(
    onDismissRequest: () -> Unit,
    latestRelease: UpdateUtil.LatestRelease,
) {
    var currentDownloadStatus by remember { 
        mutableStateOf(UpdateUtil.DownloadStatus.NotYet as UpdateUtil.DownloadStatus) 
    }
    var pendingInstall by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateFailedMsg = stringResource(R.string.app_update_failed)
    
    androidx.compose.runtime.DisposableEffect(Unit) {
        val lifecycleOwner = context as? androidx.lifecycle.LifecycleOwner
        val observer = object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) {
                if (pendingInstall && currentDownloadStatus is UpdateUtil.DownloadStatus.Finished) {
                    pendingInstall = false
                    if (UpdateUtil.installLatestApk(context)) {
                        currentDownloadStatus = UpdateUtil.DownloadStatus.NotYet
                    }
                }
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(observer)
        }
    }
    
    UpdateDialogImpl(
        onDismissRequest = onDismissRequest,
        title = latestRelease.name.toString(),
        onConfirmUpdate = {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    UpdateUtil.downloadApk(context, latestRelease)
                        .collect { downloadStatus ->
                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                currentDownloadStatus = downloadStatus
                                if (downloadStatus is UpdateUtil.DownloadStatus.Finished) {
                                    if (!UpdateUtil.installLatestApk(context)) {
                                        pendingInstall = true
                                    }
                                }
                            }
                        }
                }.onFailure {
                    it.printStackTrace()
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        currentDownloadStatus = UpdateUtil.DownloadStatus.NotYet
                        android.widget.Toast.makeText(
                            context,
                            updateFailedMsg,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        },
        releaseNote = latestRelease.body.toString(),
        downloadStatus = currentDownloadStatus
    )
}

@Composable
fun UpdateDialogImpl(
    onDismissRequest: () -> Unit,
    title: String,
    onConfirmUpdate: () -> Unit,
    releaseNote: String,
    downloadStatus: UpdateUtil.DownloadStatus,
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        icon = { Icon(Icons.Outlined.NewReleases, null) },
        confirmButton = {
            Button(
                onClick = { 
                    if (downloadStatus !is UpdateUtil.DownloadStatus.Progress) {
                        onConfirmUpdate() 
                    }
                }
            ) {
                Text(
                    when (downloadStatus) {
                        is UpdateUtil.DownloadStatus.Progress -> "${downloadStatus.percent} %"
                        else -> stringResource(R.string.update)
                    },
                    modifier = Modifier.animateContentSize(),
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                MarkdownText(
                    markdown = releaseNote,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    syntaxHighlightColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    syntaxHighlightTextColor = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    )
}
