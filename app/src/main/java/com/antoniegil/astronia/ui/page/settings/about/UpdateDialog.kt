package com.antoniegil.astronia.ui.page.settings.about

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.antoniegil.astronia.R
import com.antoniegil.astronia.util.UpdateUtil
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun UpdateDialog(
    onDismissRequest: () -> Unit,
    latestRelease: UpdateUtil.LatestRelease,
) {
    val uriHandler = LocalUriHandler.current
    
    UpdateDialogImpl(
        onDismissRequest = onDismissRequest,
        title = latestRelease.name.toString(),
        onConfirmUpdate = {
            uriHandler.openUri(latestRelease.htmlUrl ?: "https://github.com/antoniegil/Astronia/releases/latest")
            onDismissRequest()
        },
        releaseNote = latestRelease.body.toString()
    )
}

@Composable
fun UpdateDialogImpl(
    onDismissRequest: () -> Unit,
    title: String,
    onConfirmUpdate: () -> Unit,
    releaseNote: String,
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        icon = { Icon(Icons.Outlined.NewReleases, null) },
        confirmButton = {
            Button(onClick = onConfirmUpdate) {
                Text(stringResource(R.string.update))
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
