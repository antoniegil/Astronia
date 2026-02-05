package com.antoniegil.astronia.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelCard(
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    deleteThreshold: Float = -200f,
    content: @Composable () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val cardShape = MaterialTheme.shapes.medium
    var showBackground by remember { mutableStateOf(false) }
    var isDeleted by remember { mutableStateOf(false) }

    LaunchedEffect(offsetX.value) {
        showBackground = offsetX.value < -50f
    }

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
                        .background(Color(0xFFD32F2F))
                        .padding(end = 24.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = Color.White,
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
                content()
            }
        }
    }
}
