package com.antoniegil.astronia.util

class WatchTimeTracker {
    private var accumulatedTime: Long = 0L
    private var lastUpdateTime: Long = 0L
    private var isTracking: Boolean = false
    
    fun start() {
        if (!isTracking) {
            isTracking = true
            lastUpdateTime = System.currentTimeMillis()
        }
    }
    
    fun pause() {
        if (isTracking) {
            update()
            isTracking = false
            lastUpdateTime = 0L
        }
    }
    
    fun update() {
        if (isTracking && lastUpdateTime > 0) {
            val now = System.currentTimeMillis()
            val elapsed = now - lastUpdateTime
            accumulatedTime += elapsed
            lastUpdateTime = now
        }
    }
    
    fun reset() {
        accumulatedTime = 0L
        lastUpdateTime = 0L
        isTracking = false
    }
    
    fun getAccumulatedTime(): Long = accumulatedTime
}
