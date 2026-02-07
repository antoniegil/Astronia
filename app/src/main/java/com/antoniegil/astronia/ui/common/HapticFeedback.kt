package com.antoniegil.astronia.ui.common

import android.view.HapticFeedbackConstants
import android.view.View

object HapticFeedback {
    fun View.slightHapticFeedback() = this.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

}
