package com.antoniegil.astronia.util

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.provider.Settings
import android.view.OrientationEventListener
import android.view.Surface
import java.lang.ref.WeakReference

class OrientationHelper(
    activity: Activity,
    private val onOrientationChanged: (isLandscape: Boolean) -> Unit
) {
    companion object {
        private const val LAND_TYPE_NULL = 0
        private const val LAND_TYPE_NORMAL = 1
        private const val LAND_TYPE_REVERSE = 2
        
        private const val NORMAL_PORTRAIT_ANGLE_START = 30
        private const val NORMAL_PORTRAIT_ANGLE_END = 330
        private const val NORMAL_LAND_ANGLE_START = 240
        private const val NORMAL_LAND_ANGLE_END = 300
        private const val REVERSE_LAND_ANGLE_START = 60
        private const val REVERSE_LAND_ANGLE_END = 120
    }
    
    private val activityRef = WeakReference(activity)
    private val orientationEventListener: OrientationEventListener
    
    private var screenType = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    private var isLand = LAND_TYPE_NULL
    
    private var isClick = false
    private var isClickLand = false
    private var isClickPort = false
    private var isEnable = true
    private var rotateWithSystem = true
    private var isPause = false
    
    init {
        initGravity(activity)
        
        val context = activity.applicationContext
        orientationEventListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(rotation: Int) {
                if (rotation == ORIENTATION_UNKNOWN) return
                
                val autoRotateOn = try {
                    Settings.System.getInt(
                        context.contentResolver,
                        Settings.System.ACCELEROMETER_ROTATION,
                        0
                    ) == 1
                } catch (e: Exception) {
                    true
                }
                
                if (!autoRotateOn && rotateWithSystem) {
                    if (isLand == LAND_TYPE_NULL) {
                        return
                    }
                }
                
                if (isPause) {
                    return
                }
                
                handleOrientationChange(rotation)
            }
        }
    }
    
    private fun initGravity(activity: Activity) {
        if (isLand == LAND_TYPE_NULL) {
            val defaultRotation = activity.windowManager.defaultDisplay.rotation
            when (defaultRotation) {
                Surface.ROTATION_0 -> {
                    isLand = LAND_TYPE_NULL
                    screenType = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                Surface.ROTATION_270 -> {
                    isLand = LAND_TYPE_REVERSE
                    screenType = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                }
                else -> {
                    isLand = LAND_TYPE_NORMAL
                    screenType = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            }
        }
    }
    
    private fun handleOrientationChange(rotation: Int) {
        if ((rotation in 0..NORMAL_PORTRAIT_ANGLE_START) || 
            (rotation >= NORMAL_PORTRAIT_ANGLE_END)) {
            if (isClick) {
                if (isLand > LAND_TYPE_NULL && !isClickLand) {
                    return
                } else {
                    isClickPort = true
                    isClick = false
                    isLand = LAND_TYPE_NULL
                }
            } else {
                if (isLand > LAND_TYPE_NULL) {
                    screenType = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                    isLand = LAND_TYPE_NULL
                    isClick = false
                    onOrientationChanged(false)
                }
            }
        }
        else if (rotation in NORMAL_LAND_ANGLE_START..NORMAL_LAND_ANGLE_END) {
            if (isClick) {
                if (isLand != LAND_TYPE_NORMAL && !isClickPort) {
                    return
                } else {
                    isClickLand = true
                    isClick = false
                    isLand = LAND_TYPE_NORMAL
                }
            } else {
                if (isLand != LAND_TYPE_NORMAL) {
                    screenType = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                    isLand = LAND_TYPE_NORMAL
                    isClick = false
                    onOrientationChanged(true)
                }
            }
        }
        else if (rotation in REVERSE_LAND_ANGLE_START..REVERSE_LAND_ANGLE_END) {
            if (isClick) {
                if (isLand != LAND_TYPE_REVERSE && !isClickPort) {
                    return
                } else {
                    isClickLand = true
                    isClick = false
                    isLand = LAND_TYPE_REVERSE
                }
            } else if (isLand != LAND_TYPE_REVERSE) {
                screenType = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                isLand = LAND_TYPE_REVERSE
                isClick = false
                onOrientationChanged(true)
            }
        }
    }
    
    private fun setRequestedOrientation(requestedOrientation: Int) {
        val activity = activityRef.get() ?: return
        try {
            activity.requestedOrientation = requestedOrientation
        } catch (e: IllegalStateException) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O || 
                Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1) {
                android.util.Log.e("OrientationHelper", "setRequestedOrientation error", e)
            } else {
                e.printStackTrace()
            }
        }
    }
    
    fun resolveByClick() {
        isClick = true
        
        val activity = activityRef.get() ?: return
        
        if (isLand == LAND_TYPE_NULL) {
            screenType = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
            isLand = LAND_TYPE_NORMAL
            isClickLand = false
            onOrientationChanged(true)
        } else {
            screenType = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
            isLand = LAND_TYPE_NULL
            isClickPort = false
            onOrientationChanged(false)
        }
    }
    
    fun backToPortrait(): Int {
        return if (isLand > LAND_TYPE_NULL) {
            isClick = true
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
            isLand = LAND_TYPE_NULL
            isClickPort = false
            onOrientationChanged(false)
            500
        } else {
            0
        }
    }
    
    fun enable() {
        isEnable = true
        orientationEventListener.enable()
    }
    
    fun disable() {
        isEnable = false
        orientationEventListener.disable()
    }
    
    fun release() {
        orientationEventListener.disable()
    }
    
    fun setRotateWithSystem(rotate: Boolean) {
        rotateWithSystem = rotate
    }
    
    fun setPause(pause: Boolean) {
        isPause = pause
    }
    
    fun getIsLandscape(): Boolean {
        return isLand > LAND_TYPE_NULL
    }
}
