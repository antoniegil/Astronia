package com.antoniegil.astronia.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antoniegil.astronia.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelCard(
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onEdit: (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    deleteThreshold: Float = -200f,
    name: String? = null,
    logoUrl: String? = null,
    url: String? = null,
    tags: List<String> = emptyList(),
    leadingIcon: @Composable (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val cardShape = MaterialTheme.shapes.medium
    var showBackground by remember { mutableStateOf(false) }
    var isDeleted by remember { mutableStateOf(false) }

    LaunchedEffect(offsetX.value) {
        showBackground = offsetX.value < -50f
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = !isDeleted,
            exit = fadeOut(animationSpec = tween(durationMillis = 200))
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                AnimatedVisibility(
                    visible = showBackground,
                    enter = fadeIn(animationSpec = tween(durationMillis = 100)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 100)),
                    modifier = Modifier.matchParentSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(cardShape)
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(end = 24.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationX = offsetX.value
                        }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    scope.launch {
                                        if (offsetX.value < deleteThreshold) {
                                            offsetX.animateTo(
                                                targetValue = -size.width.toFloat(),
                                                animationSpec = tween(durationMillis = 200)
                                            )
                                            isDeleted = true
                                            kotlinx.coroutines.delay(200)
                                            onDelete()
                                        } else {
                                            offsetX.animateTo(
                                                targetValue = 0f,
                                                animationSpec = tween(durationMillis = 250)
                                            )
                                        }
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    scope.launch {
                                        val newValue = (offsetX.value + dragAmount).coerceAtMost(0f)
                                        offsetX.snapTo(newValue)
                                    }
                                }
                            )
                        }
                        .combinedClickable(
                            onClick = onClick,
                            onLongClick = onLongClick
                        ),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = cardShape,
                    tonalElevation = 1.dp
                ) {
                    Box {
                        if (content != null) {
                            content()
                        } else if (name != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .padding(end = if (onEdit != null) 96.dp else 48.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (leadingIcon != null) {
                                    leadingIcon()
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (!logoUrl.isNullOrEmpty()) {
                                            ChannelLogo(
                                                logoUrl = logoUrl,
                                                contentDescription = name
                                            )
                                        }
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    
                                    if (!url.isNullOrEmpty()) {
                                        Text(
                                            text = url,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    
                                    if (tags.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            tags.forEach { tag ->
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                                ) {
                                                    Text(
                                                        text = tag,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Row(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (trailingIcon != null) {
                                trailingIcon()
                            }
                            if (onEdit != null) {
                                IconButton(onClick = onEdit) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = stringResource(R.string.edit),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
