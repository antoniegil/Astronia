package com.antoniegil.astronia.ui.page.settings.about

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ContactSupport
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.antoniegil.astronia.R
import com.antoniegil.astronia.util.ErrorHandler
import com.antoniegil.astronia.BuildConfig
import com.antoniegil.astronia.ui.component.BackButton
import com.antoniegil.astronia.ui.component.PreferenceItemLarge
import com.antoniegil.astronia.ui.component.PreferenceSwitchWithDivider
import com.antoniegil.astronia.util.SettingsManager

private const val repoUrl = "https://github.com/antoniegil/astronia"
private const val releaseUrl = "https://github.com/antoniegil/astronia/releases"
private const val issueUrl = "https://github.com/antoniegil/astronia/issues"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutPage(
    onNavigateBack: () -> Unit,
    onNavigateToLicense: () -> Unit = {},
    onNavigateToUpdate: () -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
        canScroll = { true }
    )
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    
    val unknownVersion = stringResource(R.string.unknown_version)
    val versionString = stringResource(R.string.version)
    val packageNameString = stringResource(R.string.package_name)
    val buildTypeString = stringResource(R.string.build_type)
    val debugBuildString = stringResource(R.string.debug_build)
    val releaseBuildString = stringResource(R.string.release_build)
    
    val versionName = remember {
        ErrorHandler.runCatchingWithDefault(
            "AboutPage",
            "Failed to get version name",
            unknownVersion
        ) {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }
    }
    
    val versionCode = remember {
        ErrorHandler.runCatchingWithDefault(
            "AboutPage",
            "Failed to get version code",
            0L
        ) {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        }
    }
    
    val buildType = remember { if (BuildConfig.DEBUG) debugBuildString else releaseBuildString }
    
    val info = remember(versionName, versionCode, buildType) {
        buildString {
            appendLine("Astronia")
            appendLine("$versionString: $versionName ($versionCode)")
            appendLine("$packageNameString: ${context.packageName}")
            appendLine("$buildTypeString: $buildType")
        }
    }

    fun openUrl(url: String) {
        ErrorHandler.runCatching("AboutPage", "Failed to open URL: $url") {
            uriHandler.openUri(url)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.about)) },
                navigationIcon = { BackButton { onNavigateBack() } },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues)
        ) {
            item {
                PreferenceItemLarge(
                    title = stringResource(R.string.project_homepage),
                    description = stringResource(R.string.project_homepage_desc),
                    icon = Icons.Outlined.Description
                ) { openUrl(repoUrl) }
            }
            
            item {
                PreferenceItemLarge(
                    title = stringResource(R.string.version_release),
                    description = stringResource(R.string.version_release_desc),
                    icon = Icons.Outlined.NewReleases
                ) { openUrl(releaseUrl) }
            }
            
            item {
                PreferenceItemLarge(
                    title = stringResource(R.string.issue_feedback),
                    description = stringResource(R.string.issue_feedback_desc),
                    icon = Icons.AutoMirrored.Outlined.ContactSupport
                ) { openUrl(issueUrl) }
            }
            
            item {
                PreferenceItemLarge(
                    title = stringResource(R.string.open_source_licenses_title),
                    description = stringResource(R.string.open_source_licenses_desc_about),
                    icon = Icons.Outlined.AutoAwesome
                ) { onNavigateToLicense() }
            }
            
            item {
                var autoUpdate by remember { mutableStateOf(SettingsManager.getAutoUpdate(context)) }
                PreferenceSwitchWithDivider(
                    title = stringResource(R.string.auto_check_update),
                    description = stringResource(R.string.check_for_updates_desc),
                    icon = if (autoUpdate) Icons.Outlined.Update else Icons.Outlined.UpdateDisabled,
                    isChecked = autoUpdate,
                    titleStyle = MaterialTheme.typography.titleLarge,
                    onChecked = {
                        autoUpdate = !autoUpdate
                        SettingsManager.setAutoUpdate(context, autoUpdate)
                    },
                    onClick = { onNavigateToUpdate() }
                )
            }
            
            item {
                PreferenceItemLarge(
                    title = stringResource(R.string.version),
                    description = versionName,
                    icon = Icons.Outlined.Info
                ) {
                    clipboard.setText(AnnotatedString(info))
                }
            }
        }
    }
}
