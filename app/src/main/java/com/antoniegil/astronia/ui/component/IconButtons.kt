package com.antoniegil.astronia.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import com.antoniegil.astronia.R
import com.antoniegil.astronia.ui.common.HapticFeedback.slightHapticFeedback

@Composable
fun PasteButton(onClick: () -> Unit = {}) {
    IconButton(onClick = onClick) {
        Icon(Icons.Outlined.ContentPaste, stringResource(R.string.paste))
    }
}

@Composable
fun BackButton(onClick: () -> Unit) {
    val view = LocalView.current
    IconButton(
        modifier = Modifier,
        onClick = {
            onClick()
            view.slightHapticFeedback()
        },
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = stringResource(R.string.back),
        )
    }
}
