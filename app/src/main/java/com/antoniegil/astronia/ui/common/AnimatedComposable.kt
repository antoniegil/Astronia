package com.antoniegil.astronia.ui.common

import android.os.Build
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.antoniegil.astronia.ui.common.motion.EmphasizedAccelerate
import com.antoniegil.astronia.ui.common.motion.EmphasizedDecelerate
import com.antoniegil.astronia.ui.common.motion.materialSharedAxisXIn
import com.antoniegil.astronia.ui.common.motion.materialSharedAxisXOut

const val DURATION_ENTER = 400
const val DURATION_EXIT = 200
const val initialOffset = 0.10f

private val fadeTween = tween<Float>(durationMillis = DURATION_EXIT)

fun NavGraphBuilder.animatedComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    usePredictiveBack: Boolean = Build.VERSION.SDK_INT >= 34,
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit,
) {
    if (usePredictiveBack) {
        animatedComposablePredictiveBack(route, arguments, deepLinks, content)
    } else {
        animatedComposableLegacy(route, arguments, deepLinks, content)
    }
}

fun NavGraphBuilder.animatedComposablePredictiveBack(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit,
) =
    composable(
        route = route,
        arguments = arguments,
        deepLinks = deepLinks,
        enterTransition = { materialSharedAxisXIn(initialOffsetX = { (it * 0.15f).toInt() }) },
        exitTransition = {
            materialSharedAxisXOut(targetOffsetX = { -(it * initialOffset).toInt() })
        },
        popEnterTransition = {
            scaleIn(
                animationSpec = tween(durationMillis = 350, easing = EmphasizedDecelerate),
                initialScale = 0.9f,
            ) + materialSharedAxisXIn(initialOffsetX = { -(it * initialOffset).toInt() })
        },
        popExitTransition = {
            materialSharedAxisXOut(targetOffsetX = { (it * initialOffset).toInt() }) +
                scaleOut(
                    targetScale = 0.9f,
                    animationSpec = tween(durationMillis = 350, easing = EmphasizedAccelerate),
                )
        },
        content = content,
    )

fun NavGraphBuilder.animatedComposableLegacy(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit,
) =
    composable(
        route = route,
        arguments = arguments,
        deepLinks = deepLinks,
        enterTransition = {
            materialSharedAxisXIn(initialOffsetX = { (it * initialOffset).toInt() })
        },
        exitTransition = {
            materialSharedAxisXOut(targetOffsetX = { -(it * initialOffset).toInt() })
        },
        popEnterTransition = {
            materialSharedAxisXIn(initialOffsetX = { -(it * initialOffset).toInt() })
        },
        popExitTransition = {
            materialSharedAxisXOut(targetOffsetX = { (it * initialOffset).toInt() })
        },
        content = content,
    )


