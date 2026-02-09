package com.antoniegil.astronia.util

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CoroutineUtils {
    
    fun runOnMainThread(delayMs: Long = 0, action: () -> Unit) {
        if (delayMs > 0) {
            Handler(Looper.getMainLooper()).postDelayed(action, delayMs)
        } else {
            Handler(Looper.getMainLooper()).post(action)
        }
    }
    
    suspend fun <T> runOnIoThread(block: suspend () -> T): T {
        return withContext(Dispatchers.IO) {
            block()
        }
    }
}
