package com.antoniegil.astronia

import android.app.Application
import android.content.ComponentCallbacks2
import com.antoniegil.astronia.player.Media3Player
import com.tencent.mmkv.MMKV
import java.lang.ref.WeakReference

class Astronia : Application() {
    companion object {
        private var globalPlayerRef: WeakReference<Media3Player>? = null
        lateinit var instance: Astronia
            private set
        
        fun getOrCreatePlayer(application: Application): Media3Player {
            val existingPlayer = globalPlayerRef?.get()
            if (existingPlayer != null) {
                return existingPlayer
            }
            
            val newPlayer = Media3Player(application.applicationContext)
            globalPlayerRef = WeakReference(newPlayer)
            return newPlayer
        }
        
        fun releaseGlobalPlayer() {
            globalPlayerRef?.get()?.release()
            globalPlayerRef = null
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        MMKV.initialize(this)
    }
    
    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_MODERATE) {
            releaseGlobalPlayer()
        }
    }
}
