package com.antoniegil.astronia.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.antoniegil.astronia.R
import com.antoniegil.astronia.ui.common.HapticFeedback.slightHapticFeedback

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    text: String,
    placeholderText: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit = {}
) {
    val view = LocalView.current

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            AutoFocusTextField(
                value = text,
                onValueChange = onValueChange,
                placeholder = { Text(placeholderText) },
                modifier = Modifier.weight(1f),
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton(onClick = {
                            view.slightHapticFeedback()
                            onValueChange("")
                            onClear()
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = stringResource(R.string.clear),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    }
}
