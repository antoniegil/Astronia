package com.antoniegil.astronia.ui.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antoniegil.astronia.ui.common.HapticFeedback.slightHapticFeedback

@Composable
fun SettingItem(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else if (iconPainter != null) {
                Icon(
                    painter = iconPainter,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (icon == null && iconPainter == null) 12.dp else 0.dp)
            ) {
                Text(
                    text = title,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    overflow = TextOverflow.Ellipsis
                )
                if (description != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        style = MaterialTheme.typography.bodyMedium,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun PreferenceSwitch(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    isChecked: Boolean,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit
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
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        indication = LocalIndication.current,
                        interactionSource = interactionSource
                    ) {
                        view.slightHapticFeedback()
                        onClick()
                    }
                } else {
                    Modifier.toggleable(
                        value = isChecked,
                        enabled = enabled,
                        onValueChange = {
                            view.slightHapticFeedback()
                            onCheckedChange(it)
                        },
                        indication = LocalIndication.current,
                        interactionSource = interactionSource,
                    )
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .padding(start = if (icon == null) 12.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp, end = 16.dp).size(24.dp),
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    }
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Switch(
                checked = isChecked,
                onCheckedChange = null,
                interactionSource = interactionSource,
                modifier = Modifier.padding(start = 20.dp, end = 6.dp),
                enabled = enabled,
                thumbContent = thumbContent
            )
        }
    }
}

@Composable
fun PreferenceSingleChoiceItem(
    text: String,
    selected: Boolean,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 18.dp),
    onClick: () -> Unit
) {
    val view = LocalView.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = {
                view.slightHapticFeedback()
                onClick()
            })
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            ) {
                Text(
                    text = text,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    overflow = TextOverflow.Ellipsis
                )
            }
            RadioButton(
                selected = selected,
                onClick = onClick
            )
        }
    }
}

@Composable
fun PreferenceSubtitle(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(start = 20.dp, top = 24.dp, bottom = 12.dp),
    text: String
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding)
    )
}

@Composable
fun PreferenceItem(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit = {}
) {
    val view = LocalView.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                view.slightHapticFeedback()
                onClick()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp, end = 16.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = if (icon == null) 12.dp else 0.dp)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (description != null) {
                    Text(
                        text = description,
                        modifier = Modifier.padding(top = 2.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun PreferenceItemLarge(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit = {}
) {
    val view = LocalView.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                view.slightHapticFeedback()
                onClick()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp, end = 16.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = if (icon == null) 12.dp else 0.dp)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (description != null) {
                    Text(
                        text = description,
                        modifier = Modifier.padding(top = 2.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun PreferencesHintCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit = {}
) {
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(com.antoniegil.astronia.ui.theme.FixedAccentColors.secondaryFixed)
            .clickable {
                view.slightHapticFeedback()
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(start = 8.dp, end = 16.dp)
                .size(24.dp),
            tint = com.antoniegil.astronia.ui.theme.FixedAccentColors.onSecondaryFixed
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                maxLines = 1,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                color = com.antoniegil.astronia.ui.theme.FixedAccentColors.onSecondaryFixed,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = description,
                color = com.antoniegil.astronia.ui.theme.FixedAccentColors.onSecondaryFixed,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun PreferenceSwitchWithDivider(
    title: String,
    description: String,
    icon: ImageVector,
    isChecked: Boolean,
    titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
    onChecked: (Boolean) -> Unit,
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
    
    Surface(
        modifier = Modifier.clickable {
            view.slightHapticFeedback()
            onClick()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp, end = 16.dp).size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = titleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            VerticalDivider(
                modifier = Modifier
                    .height(32.dp)
                    .padding(horizontal = 8.dp)
                    .width(1.dp)
                    .align(Alignment.CenterVertically),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Switch(
                checked = isChecked,
                onCheckedChange = {
                    view.slightHapticFeedback()
                    onChecked(it)
                },
                modifier = Modifier.padding(horizontal = 6.dp),
                thumbContent = thumbContent
            )
        }
    }
}

@Composable
fun PreferenceSwitchVariant(
    title: String,
    description: String? = null,
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
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = isChecked,
                onValueChange = {
                    view.slightHapticFeedback()
                    onClick()
                },
                indication = LocalIndication.current,
                interactionSource = interactionSource,
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .padding(start = if (icon == null) 12.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp, end = 16.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Switch(
                checked = isChecked,
                onCheckedChange = null,
                interactionSource = interactionSource,
                modifier = Modifier.padding(start = 20.dp, end = 6.dp),
                thumbContent = thumbContent
            )
        }
    }
}

@Composable
fun PreferenceInfo(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector = Icons.Outlined.Info,
    applyPaddings: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .run {
                if (applyPaddings) padding(horizontal = 16.dp, vertical = 16.dp)
                else this
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null
        )
        Text(
            text = text,
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
