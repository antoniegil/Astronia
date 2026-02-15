package com.antoniegil.astronia.ui.page.settings.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import com.antoniegil.astronia.R
import com.antoniegil.astronia.ui.component.BackButton

data class Credit(
    val title: String = "",
    val license: String? = null,
    val url: String = ""
)

private const val GPL_V3 = "GNU General Public License v3.0"
private const val LGPL_V2_1 = "GNU Lesser General Public License, version 2.1"
private const val APACHE_V2 = "Apache License, Version 2.0"
private const val BSD_3 = "BSD 3-Clause License"
private const val MIT = "MIT License"

fun Color.applyOpacity(enabled: Boolean): Color {
    return if (enabled) this else this.copy(alpha = 0.62f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensePage(onNavigateBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val creditsList = listOf(
        Credit("Seal", GPL_V3, "https://github.com/JunkFood02/Seal"),
        Credit("Android Jetpack", APACHE_V2, "https://github.com/androidx/androidx"),
        Credit("Kotlin", APACHE_V2, "https://kotlinlang.org/"),
        Credit("AndroidX Media3", APACHE_V2, "https://github.com/androidx/media"),
        Credit("Jellyfin Media3 FFmpeg Decoder", LGPL_V2_1, "https://github.com/jellyfin/jellyfin-android"),
        Credit("FFmpeg", LGPL_V2_1, "https://ffmpeg.org/"),
        Credit("Material Design 3", APACHE_V2, "https://m3.material.io/"),
        Credit("Material Icons", APACHE_V2, "https://fonts.google.com/icons"),
        Credit("Monet", APACHE_V2, "https://github.com/Kyant0/Monet"),
        Credit("Material color utilities", APACHE_V2, "https://github.com/material-foundation/material-color-utilities"),
        Credit("MMKV", BSD_3, "https://github.com/Tencent/MMKV"),
        Credit("Coil", APACHE_V2, "https://github.com/coil-kt/coil"),
        Credit("OkHttp", APACHE_V2, "https://github.com/square/okhttp"),
        Credit("Koin", APACHE_V2, "https://github.com/InsertKoinIO/koin"),
        Credit("Jetpack Compose", APACHE_V2, "https://github.com/androidx/androidx"),
        Credit("AndroidX Core KTX", APACHE_V2, "https://github.com/androidx/androidx"),
        Credit("AndroidX Activity Compose", APACHE_V2, "https://github.com/androidx/androidx"),
        Credit("AndroidX AppCompat", APACHE_V2, "https://github.com/androidx/androidx"),
        Credit("AndroidX Lifecycle", APACHE_V2, "https://github.com/androidx/androidx"),
        Credit("AndroidX Navigation Compose", APACHE_V2, "https://github.com/androidx/androidx"),
        Credit("AndroidX ConstraintLayout Compose", APACHE_V2, "https://github.com/androidx/androidx"),
        Credit("AndroidX Graphics Shapes", APACHE_V2, "https://github.com/androidx/androidx"),
        Credit("Kotlinx Coroutines", APACHE_V2, "https://github.com/Kotlin/kotlinx.coroutines"),
        Credit("Kotlinx Serialization", APACHE_V2, "https://github.com/Kotlin/kotlinx.serialization"),
        Credit("Kotlinx DateTime", APACHE_V2, "https://github.com/Kotlin/kotlinx-datetime"),
        Credit("Accompanist", APACHE_V2, "https://github.com/google/accompanist"),
        Credit("Compose Markdown", MIT, "https://github.com/jeziellago/compose-markdown"),
        Credit("Reorderable", APACHE_V2, "https://github.com/Calvin-LL/Reorderable"),
        Credit("LeakCanary", APACHE_V2, "https://github.com/square/leakcanary"),
    )

    val uriHandler = LocalUriHandler.current
    fun openUrl(url: String) {
        uriHandler.openUri(url)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.open_source_licenses_title)) },
                navigationIcon = { BackButton(onNavigateBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues)
        ) {
            item {
                Surface(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .clip(MaterialTheme.shapes.large)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = MaterialTheme.shapes.large
                        ),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 6.dp
                ) {
                    val painter = rememberVectorPainter(image = Icons.Outlined.Code)
                    Image(
                        painter = painter,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.outline),
                        modifier = Modifier.padding(horizontal = 72.dp, vertical = 48.dp),
                    )
                }
            }
            
            items(creditsList) { item ->
                CreditItem(
                    title = item.title,
                    license = item.license
                ) { 
                    openUrl(item.url) 
                }
            }
        }
    }
}

@Composable
private fun CreditItem(
    title: String,
    license: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            ) {
                with(MaterialTheme) {
                    Text(
                        text = title,
                        maxLines = 1,
                        style = typography.titleMedium,
                        color = colorScheme.onSurface.applyOpacity(enabled),
                    )
                    license?.let {
                        Text(
                            text = it,
                            color = colorScheme.onSurfaceVariant.applyOpacity(enabled),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}
