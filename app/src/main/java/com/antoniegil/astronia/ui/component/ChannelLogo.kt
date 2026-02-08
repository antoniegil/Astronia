package com.antoniegil.astronia.ui.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun ChannelLogo(
    logoUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    if (logoUrl.isNotEmpty()) {
        var isVisible by remember(logoUrl) { mutableStateOf(true) }
        
        if (isVisible) {
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(logoUrl)
                    .crossfade(true)
                    .memoryCacheKey(logoUrl)
                    .diskCacheKey(logoUrl)
                    .listener(
                        onError = { _, _ -> isVisible = false }
                    )
                    .build(),
                contentDescription = contentDescription,
                modifier = modifier
                    .heightIn(max = 18.dp)
                    .widthIn(max = 34.dp),
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart,
                onError = { isVisible = false }
            )
        }
    }
}
