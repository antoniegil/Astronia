package com.antoniegil.astronia.ui.common.motion

import androidx.compose.animation.core.CubicBezierEasing


val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 1f, 1f)
