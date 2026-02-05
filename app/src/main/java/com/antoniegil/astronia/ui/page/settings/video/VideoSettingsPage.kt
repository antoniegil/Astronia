package com.antoniegil.astronia.ui.page.settings.video

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.antoniegil.astronia.R
import com.antoniegil.astronia.ui.component.BackButton
import com.antoniegil.astronia.ui.component.PreferenceSwitch
import com.antoniegil.astronia.ui.component.PreferenceSubtitle
import com.antoniegil.astronia.ui.component.PreferenceItem
import com.antoniegil.astronia.ui.component.RadioButtonDialog
import com.antoniegil.astronia.util.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSettingsPage(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    var decoderType by remember { mutableIntStateOf(SettingsManager.getDecoderType(context)) }
    var aspectRatio by remember { mutableIntStateOf(SettingsManager.getAspectRatio(context)) }
    var mirrorFlip by remember { mutableStateOf(SettingsManager.getMirrorFlip(context)) }
    
    var showAspectRatioDialog by remember { mutableStateOf(false) }
    var showDecoderTypeDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.video)) },
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
                PreferenceSubtitle(text = stringResource(R.string.display_settings))
            }
            
            item {
                PreferenceItem(
                    title = stringResource(R.string.aspect_ratio),
                    description = when(aspectRatio) {
                        0 -> "16:9"
                        1 -> "4:3"
                        2 -> stringResource(R.string.aspect_ratio_fill)
                        3 -> stringResource(R.string.aspect_ratio_original)
                        else -> "16:9"
                    },
                    icon = Icons.Outlined.AspectRatio,
                    onClick = { showAspectRatioDialog = true }
                )
            }
            
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.mirror_flip),
                    description = stringResource(R.string.mirror_flip_desc),
                    icon = Icons.Outlined.Flip,
                    isChecked = mirrorFlip,
                    onCheckedChange = { 
                        mirrorFlip = it
                        SettingsManager.setMirrorFlip(context, it)
                    }
                )
            }
            
            item {
                PreferenceSubtitle(text = stringResource(R.string.decoder_settings))
            }
            
            item {
                PreferenceItem(
                    title = stringResource(R.string.decoder),
                    description = when(decoderType) {
                        0 -> stringResource(R.string.decoder_hardware)
                        else -> stringResource(R.string.decoder_software)
                    },
                    icon = Icons.Outlined.Speed,
                    onClick = { showDecoderTypeDialog = true }
                )
            }
        }
    }
    
    if (showAspectRatioDialog) {
        RadioButtonDialog(
            title = stringResource(R.string.aspect_ratio),
            options = listOf(
                0 to "16:9",
                1 to "4:3",
                2 to stringResource(R.string.aspect_ratio_fill),
                3 to stringResource(R.string.aspect_ratio_original)
            ),
            selectedValue = aspectRatio,
            onDismiss = { showAspectRatioDialog = false },
            onSelect = { value ->
                aspectRatio = value
                SettingsManager.setAspectRatio(context, value)
            }
        )
    }
    
    if (showDecoderTypeDialog) {
        RadioButtonDialog(
            title = stringResource(R.string.decoder),
            options = listOf(
                0 to stringResource(R.string.decoder_hardware),
                1 to stringResource(R.string.decoder_software)
            ),
            selectedValue = decoderType,
            onDismiss = { showDecoderTypeDialog = false },
            onSelect = { value ->
                decoderType = value
                SettingsManager.setDecoderType(context, value)
            }
        )
    }
}
