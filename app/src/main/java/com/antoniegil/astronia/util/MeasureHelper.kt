package com.antoniegil.astronia.util

import android.view.View

class MeasureHelper(
    private val view: View,
    private val paramsListener: MeasureFormVideoParamsListener
) {
    private var videoWidth = 0
    private var videoHeight = 0
    private var videoSarNum = 0
    private var videoSarDen = 0
    private var videoRotationDegree = 0
    
    var measuredWidth = 0
        private set
    var measuredHeight = 0
        private set
    
    private var currentAspectRatio = 0
    
    fun setVideoSize(width: Int, height: Int) {
        videoWidth = width
        videoHeight = height
    }
    
    fun setVideoSampleAspectRatio(sarNum: Int, sarDen: Int) {
        videoSarNum = sarNum
        videoSarDen = sarDen
    }
    
    fun setVideoRotation(degree: Int) {
        videoRotationDegree = degree
    }
    
    fun doMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int, aspectRatio: Int) {
        currentAspectRatio = aspectRatio
        
        if (videoHeight == 0 || videoWidth == 0) {
            measuredWidth = 1
            measuredHeight = 1
            return
        }
        
        var wSpec = widthMeasureSpec
        var hSpec = heightMeasureSpec
        
        if (videoRotationDegree == 90 || videoRotationDegree == 270) {
            val temp = wSpec
            wSpec = hSpec
            hSpec = temp
        }
        
        var realWidth = videoWidth
        if (videoSarNum != 0 && videoSarDen != 0) {
            val pixelWidthHeightRatio = videoSarNum / (videoSarDen / 1.0)
            realWidth = (pixelWidthHeightRatio * videoWidth).toInt()
        }
        
        var width = View.getDefaultSize(realWidth, wSpec)
        var height = View.getDefaultSize(videoHeight, hSpec)
        
        if (realWidth > 0 && videoHeight > 0) {
            val widthSpecMode = View.MeasureSpec.getMode(wSpec)
            val widthSpecSize = View.MeasureSpec.getSize(wSpec)
            val heightSpecMode = View.MeasureSpec.getMode(hSpec)
            val heightSpecSize = View.MeasureSpec.getSize(hSpec)
            
            if (widthSpecMode == View.MeasureSpec.AT_MOST && heightSpecMode == View.MeasureSpec.AT_MOST) {
                val specAspectRatio = widthSpecSize.toFloat() / heightSpecSize.toFloat()
                var displayAspectRatio = when (currentAspectRatio) {
                    0 -> {
                        var ratio = 16.0f / 9.0f
                        if (videoRotationDegree == 90 || videoRotationDegree == 270) {
                            ratio = 1.0f / ratio
                        }
                        ratio
                    }
                    1 -> {
                        var ratio = 4.0f / 3.0f
                        if (videoRotationDegree == 90 || videoRotationDegree == 270) {
                            ratio = 1.0f / ratio
                        }
                        ratio
                    }
                    2, 3 -> {
                        var ratio = realWidth.toFloat() / videoHeight.toFloat()
                        if (videoSarNum > 0 && videoSarDen > 0) {
                            ratio = ratio * videoSarNum / videoSarDen
                        }
                        ratio
                    }
                    else -> realWidth.toFloat() / videoHeight.toFloat()
                }
                
                val shouldBeWider = displayAspectRatio > specAspectRatio
                
                when (currentAspectRatio) {
                    0, 1, 3 -> {
                        if (shouldBeWider) {
                            width = widthSpecSize
                            height = (width / displayAspectRatio).toInt()
                        } else {
                            height = heightSpecSize
                            width = (height * displayAspectRatio).toInt()
                        }
                    }
                    2 -> {
                        width = widthSpecSize
                        height = heightSpecSize
                    }
                }
            } else if (widthSpecMode == View.MeasureSpec.EXACTLY && heightSpecMode == View.MeasureSpec.EXACTLY) {
                width = widthSpecSize
                height = heightSpecSize
            } else if (widthSpecMode == View.MeasureSpec.EXACTLY) {
                width = widthSpecSize
                height = width * videoHeight / realWidth
                if (heightSpecMode == View.MeasureSpec.AT_MOST && height > heightSpecSize) {
                    height = heightSpecSize
                }
            } else if (heightSpecMode == View.MeasureSpec.EXACTLY) {
                height = heightSpecSize
                width = height * realWidth / videoHeight
                if (widthSpecMode == View.MeasureSpec.AT_MOST && width > widthSpecSize) {
                    width = widthSpecSize
                }
            } else {
                width = realWidth
                height = videoHeight
                if (heightSpecMode == View.MeasureSpec.AT_MOST && height > heightSpecSize) {
                    height = heightSpecSize
                    width = height * realWidth / videoHeight
                }
                if (widthSpecMode == View.MeasureSpec.AT_MOST && width > widthSpecSize) {
                    width = widthSpecSize
                    height = width * videoHeight / realWidth
                }
            }
        }
        
        measuredWidth = width
        measuredHeight = height
    }
    
    fun prepareMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int, rotate: Int, aspectRatio: Int) {
        try {
            val vWidth = paramsListener.getCurrentVideoWidth()
            val vHeight = paramsListener.getCurrentVideoHeight()
            val sarNum = paramsListener.getVideoSarNum()
            val sarDen = paramsListener.getVideoSarDen()
            
            if (vWidth > 0 && vHeight > 0) {
                setVideoSampleAspectRatio(sarNum, sarDen)
                setVideoSize(vWidth, vHeight)
            }
            setVideoRotation(rotate)
            doMeasure(widthMeasureSpec, heightMeasureSpec, aspectRatio)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    interface MeasureFormVideoParamsListener {
        fun getCurrentVideoWidth(): Int
        fun getCurrentVideoHeight(): Int
        fun getVideoSarNum(): Int
        fun getVideoSarDen(): Int
    }
}
