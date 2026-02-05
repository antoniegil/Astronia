package com.antoniegil.astronia.ui.page.settings.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.UpdateDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.antoniegil.astronia.R
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.outlined.Check
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.sp
import com.antoniegil.astronia.ui.component.BackButton
import com.antoniegil.astronia.ui.component.HorizontalDivider
import com.antoniegil.astronia.ui.component.PreferenceInfo
import com.antoniegil.astronia.ui.component.PreferenceSingleChoiceItem
import com.antoniegil.astronia.ui.component.PreferenceSubtitle
import com.antoniegil.astronia.util.SettingsManager
import com.antoniegil.astronia.util.UpdateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val STABLE = 0
private const val PRE_RELEASE = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatePage(onNavigateBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
        canScroll = { true }
    )
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val alreadyLatestMsg = stringResource(R.string.already_latest_version)
    val checkFailedMsg = stringResource(R.string.check_update_failed)
    
    var autoUpdate by remember { mutableStateOf(SettingsManager.getAutoUpdate(context)) }
    var updateChannel by remember { mutableIntStateOf(SettingsManager.getUpdateChannel(context)) }
    var isLoading by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var latestRelease by remember { mutableStateOf(UpdateUtil.LatestRelease()) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.auto_update)) },
                navigationIcon = { BackButton(onNavigateBack) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddings ->
        LazyColumn(modifier = Modifier.padding(paddings)) {
            item {
                PreferenceSwitchWithContainer(
                    title = stringResource(R.string.enable_auto_update),
                    icon = if (autoUpdate) Icons.Outlined.Update else Icons.Outlined.UpdateDisabled,
                    isChecked = autoUpdate
                ) {
                    autoUpdate = !autoUpdate
                    SettingsManager.setAutoUpdate(context, autoUpdate)
                }
            }
            item {
                PreferenceSubtitle(text = stringResource(R.string.update_channel))
            }
            item {
                PreferenceSingleChoiceItem(
                    text = stringResource(R.string.stable_channel),
                    selected = updateChannel == STABLE,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    onClick = {
                        updateChannel = STABLE
                        SettingsManager.setUpdateChannel(context, updateChannel)
                    }
                )
            }

            item {
                PreferenceSingleChoiceItem(
                    text = stringResource(R.string.pre_release_channel),
                    selected = updateChannel == PRE_RELEASE,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    onClick = {
                        updateChannel = PRE_RELEASE
                        SettingsManager.setUpdateChannel(context, updateChannel)
                    }
                )
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ProgressIndicatorButton(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(top = 6.dp)
                            .padding(bottom = 12.dp),
                        text = stringResource(R.string.check_for_updates),
                        icon = Icons.Outlined.Update,
                        isLoading = isLoading
                    ) {
                        if (!isLoading) {
                            scope.launch {
                                isLoading = true
                                withContext(Dispatchers.IO) {
                                    runCatching {
                                        UpdateUtil.checkForUpdate(context)?.let {
                                            latestRelease = it
                                            showUpdateDialog = true
                                        } ?: run {
                                            withContext(Dispatchers.Main) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    alreadyLatestMsg,
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }.onFailure {
                                        it.printStackTrace()
                                        withContext(Dispatchers.Main) {
                                            android.widget.Toast.makeText(
                                                context,
                                                checkFailedMsg,
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                                isLoading = false
                            }
                        }
                    }
                }
                HorizontalDivider()
            }
            item {
                PreferenceInfo(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    text = stringResource(R.string.update_channel_desc)
                )
            }
        }
    }
    
    if (showUpdateDialog) {
        UpdateDialog(
            onDismissRequest = { showUpdateDialog = false },
            latestRelease = latestRelease
        )
    }
}

@Composable
fun ProgressIndicatorButton(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        modifier = modifier,
        onClick = onClick,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
    ) {
        if (isLoading)
            Box(modifier = Modifier.size(18.dp)) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.Center),
                    strokeWidth = 3.dp
                )
            }
        else Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun PreferenceSwitchWithContainer(
    title: String,
    icon: ImageVector? = null,
    isChecked: Boolean,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val thumbContent: (@Composable () -> Unit)? = remember(isChecked) {
        if (isChecked) {
            {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        } else {
            null
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .toggleable(
                value = isChecked,
                onValueChange = { onClick() },
                interactionSource = interactionSource,
                indication = LocalIndication.current
            )
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 8.dp, end = 16.dp)
                    .size(24.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (icon == null) 12.dp else 0.dp, end = 12.dp)
        ) {
            Text(
                text = title,
                maxLines = 2,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Switch(
            checked = isChecked,
            interactionSource = interactionSource,
            onCheckedChange = null,
            modifier = Modifier.padding(start = 12.dp, end = 6.dp),
            thumbContent = thumbContent,
        )
    }
}
