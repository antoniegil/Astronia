package com.antoniegil.astronia.ui.common.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

private const val ProgressThreshold = 0.35f

private val Int.ForOutgoing: Int
    get() = (this * ProgressThreshold).toInt()

private val Int.ForIncoming: Int
    get() = this - this.ForOutgoing

fun materialSharedAxisXIn(
    initialOffsetX: (fullWidth: Int) -> Int,
    durationMillis: Int = 400,
): EnterTransition =
    slideInHorizontally(
        animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
        initialOffsetX = initialOffsetX,
    ) +
        fadeIn(
            animationSpec =
                tween(
                    durationMillis = durationMillis.ForIncoming,
                    delayMillis = durationMillis.ForOutgoing,
                    easing = LinearOutSlowInEasing,
                )
        )

fun materialSharedAxisXOut(
    targetOffsetX: (fullWidth: Int) -> Int,
    durationMillis: Int = 400,
): ExitTransition =
    slideOutHorizontally(
        animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
        targetOffsetX = targetOffsetX,
    ) +
        fadeOut(
            animationSpec =
                tween(
                    durationMillis = durationMillis.ForOutgoing,
                    delayMillis = 0,
                    easing = FastOutLinearInEasing,
                )
        )
