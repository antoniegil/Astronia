package com.antoniegil.astronia.util

import android.util.Log

object ErrorHandler {
    
    private const val TAG = "Astronia"
    
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, "[$tag] $message", throwable)
        } else {
            Log.e(TAG, "[$tag] $message")
        }
    }
    
    fun logWarning(tag: String, message: String) {
        Log.w(TAG, "[$tag] $message")
    }
    
    fun logInfo(tag: String, message: String) {
        Log.i(TAG, "[$tag] $message")
    }
    
    inline fun <T> runCatching(
        tag: String,
        errorMessage: String,
        block: () -> T
    ): T? {
        return try {
            block()
        } catch (e: Exception) {
            logError(tag, errorMessage, e)
            null
        }
    }
    
    inline fun <T> runCatchingWithDefault(
        tag: String,
        errorMessage: String,
        default: T,
        block: () -> T
    ): T {
        return try {
            block()
        } catch (e: Exception) {
            logError(tag, errorMessage, e)
            default
        }
    }
}
