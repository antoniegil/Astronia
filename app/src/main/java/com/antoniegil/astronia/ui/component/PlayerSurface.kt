package com.antoniegil.astronia.ui.component

import android.content.Context
import android.graphics.Matrix
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import com.antoniegil.astronia.player.Media3Player
import com.antoniegil.astronia.util.MeasureHelper

private class PlayerTextureView @JvmOverloads constructor(
    context: Context,
    private val player: Media3Player? = null,
    initialAspectRatio: Int = 0,
    initialMirrorFlip: Boolean = false,
    private val isBackgroundRetained: Boolean = false,
    private val onSurfaceReady: () -> Unit = {}
) : TextureView(context), MeasureHelper.MeasureFormVideoParamsListener {
    
    private val measureHelper = MeasureHelper(this)
    private var currentAspectRatio = initialAspectRatio
    private var currentMirrorFlip = initialMirrorFlip
    
    init {
        isOpaque = true
        layoutParams = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.Gravity.CENTER
        )
        
        surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surfaceTexture: android.graphics.SurfaceTexture,
                width: Int,
                height: Int
            ) {
                val surf = android.view.Surface(surfaceTexture)
                player?.attachSurface(surf)
                post { 
                    applyTransform()
                    requestLayout()
                }
                onSurfaceReady()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: android.graphics.SurfaceTexture,
                width: Int,
                height: Int
            ) {
                post { applyTransform() }
            }

            override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
                if (!isBackgroundRetained) {
                    player?.attachSurface(null)
                }
                return true
            }

            override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
        }
    }
    
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            applyTransform()
        }
    }
    
    private fun applyTransform() {
        if (width > 0) {
            val matrix = Matrix()
            if (currentMirrorFlip) {
                matrix.setScale(-1f, 1f, width / 2f, 0f)
            } else {
                matrix.setScale(1f, 1f, width / 2f, 0f)
            }
            setTransform(matrix)
        }
    }
    
    fun updateMirrorFlip(flip: Boolean) {
        if (currentMirrorFlip != flip) {
            currentMirrorFlip = flip
            applyTransform()
        }
    }
    
    fun updateAspectRatio(ratio: Int) {
        if (currentAspectRatio != ratio) {
            currentAspectRatio = ratio
            requestLayout()
        }
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val vWidth = player?.exoPlayer?.videoSize?.width ?: 0
        val vHeight = player?.exoPlayer?.videoSize?.height ?: 0
        
        if (vWidth == 0 || vHeight == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        } else {
            measureHelper.prepareMeasure(widthMeasureSpec, heightMeasureSpec, 0, currentAspectRatio)
            setMeasuredDimension(measureHelper.measuredWidth, measureHelper.measuredHeight)
        }
    }
    
    override fun getCurrentVideoWidth(): Int {
        return player?.exoPlayer?.videoSize?.width ?: 0
    }
    
    override fun getCurrentVideoHeight(): Int {
        return player?.exoPlayer?.videoSize?.height ?: 0
    }
    
    override fun getVideoSarNum(): Int = 0
    
    override fun getVideoSarDen(): Int = 0
}

@Composable
fun PlayerSurface(
    player: Media3Player?,
    aspectRatio: Int,
    modifier: Modifier = Modifier,
    mirrorFlip: Boolean = false,
    onSurfaceReady: () -> Unit = {},
    isBackgroundRetained: Boolean = false,
    currentChannelUrl: String = ""
) {
    val textureViewRef = remember { mutableStateOf<TextureView?>(null) }
    var surfaceReady by remember { mutableStateOf(true) }
    var lastChannelUrl by remember { mutableStateOf(currentChannelUrl) }
    
    LaunchedEffect(currentChannelUrl) {
        if (currentChannelUrl.isNotEmpty() && currentChannelUrl != lastChannelUrl) {
            lastChannelUrl = currentChannelUrl
            surfaceReady = false
            player?.attachSurface(null)
            kotlinx.coroutines.delay(50)
            surfaceReady = true
        } else if (lastChannelUrl.isEmpty() && currentChannelUrl.isNotEmpty()) {
            lastChannelUrl = currentChannelUrl
        }
    }
    
    LaunchedEffect(aspectRatio) {
        textureViewRef.value?.post {
            (textureViewRef.value as? PlayerTextureView)?.updateAspectRatio(aspectRatio)
        }
    }
    
    LaunchedEffect(mirrorFlip) {
        textureViewRef.value?.post {
            (textureViewRef.value as? PlayerTextureView)?.updateMirrorFlip(mirrorFlip)
        }
    }
    
    LaunchedEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                textureViewRef.value?.post { 
                    textureViewRef.value?.requestLayout()
                }
            }
        }
        player?.exoPlayer?.addListener(listener)
    }
    
    DisposableEffect(player) {
        onDispose {
            player?.attachSurface(null)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (surfaceReady) {
            AndroidView(
                factory = { ctx ->
                    PlayerTextureView(ctx, player, aspectRatio, mirrorFlip, isBackgroundRetained, onSurfaceReady).also {
                        textureViewRef.value = it
                    }
                },
                onRelease = {
                    player?.attachSurface(null)
                },
                modifier = Modifier
            )
        }
        

    }
}
