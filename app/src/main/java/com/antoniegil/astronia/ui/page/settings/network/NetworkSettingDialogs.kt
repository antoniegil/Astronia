package com.antoniegil.astronia.ui.page.settings.network

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.antoniegil.astronia.R
import com.antoniegil.astronia.util.SettingsManager

@Composable
fun ProxyConfigurationDialog(
    onDismissRequest: () -> Unit = {}
) {
    val context = LocalContext.current
    var proxyUrl by remember { 
        val host = SettingsManager.getProxyHost(context)
        val port = SettingsManager.getProxyPort(context)
        mutableStateOf(if (host.isNotEmpty()) "$host:$port" else "")
    }
    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.VpnKey, null) },
        title = { Text(stringResource(R.string.proxy)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.proxy_desc),
                    style = MaterialTheme.typography.bodyLarge
                )
                OutlinedTextField(
                    value = proxyUrl,
                    onValueChange = { proxyUrl = it },
                    label = { Text(stringResource(R.string.proxy)) },
                    placeholder = { Text(stringResource(R.string.proxy)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parts = proxyUrl.split(":")
                if (parts.size == 2) {
                    SettingsManager.setProxyHost(context, parts[0])
                    parts[1].toIntOrNull()?.let { SettingsManager.setProxyPort(context, it) }
                } else if (proxyUrl.isNotEmpty()) {
                    SettingsManager.setProxyHost(context, proxyUrl)
                }
                onDismissRequest()
            }) {
                Text(stringResource(R.string.settings))
            }
        }
    )
}
