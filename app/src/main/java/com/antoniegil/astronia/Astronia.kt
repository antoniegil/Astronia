package com.antoniegil.astronia

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.antoniegil.astronia.player.Media3Player
import com.antoniegil.astronia.util.NetworkUtils
import com.tencent.mmkv.MMKV
import java.lang.ref.WeakReference

class Astronia : Application(), ImageLoaderFactory {
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
        NetworkUtils.setupAndroid7SSL()
    }
    
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N) {
                    NetworkUtils.createHttpClient(15000, 30000, true)
                } else {
                    NetworkUtils.createHttpClient(15000, 30000, false)
                }
            }
            .build()
    }
    
    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_MODERATE) {
            releaseGlobalPlayer()
        }
    }
}
