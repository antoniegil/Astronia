package com.antoniegil.astronia.ui.page.settings.about

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import com.antoniegil.astronia.R
import com.antoniegil.astronia.util.UpdateUtil
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateFailedMsg = stringResource(R.string.app_update_failed)
    
    UpdateDialogImpl(
        onDismissRequest = onDismissRequest,
        title = latestRelease.name.toString(),
        onConfirmUpdate = {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    UpdateUtil.downloadApk(context, latestRelease)
                        .collect { downloadStatus ->
                            currentDownloadStatus = downloadStatus
                            if (downloadStatus is UpdateUtil.DownloadStatus.Finished) {
                                UpdateUtil.installLatestApk(context)
                            }
                        }
                }.onFailure {
                    it.printStackTrace()
                    currentDownloadStatus = UpdateUtil.DownloadStatus.NotYet
                    android.widget.Toast.makeText(
                        context,
                        updateFailedMsg,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
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
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(releaseNote)
            }
        }
    )
}
