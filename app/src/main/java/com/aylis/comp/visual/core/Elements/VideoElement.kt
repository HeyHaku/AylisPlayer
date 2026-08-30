package com.aylis.comp.visual.core.Elements

import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.view.Surface
import com.aylis.Common.Utils
import com.aylis.Common.Vec2f
import com.aylis.Common.tlog
import com.aylis.comp.AlbumArt.AlbumArtRequest
import com.aylis.comp.visual.core.Elements.Images.ImageElement
import com.aylis.comp.visual.core.Graphic.AtlasTexture
import com.aylis.comp.visual.core.Graphic.RenderState
import com.aylis.comp.visual.core.gl.mdesl.graphics.OESTexture
import com.aylis.comp.visual.core.gl.mdesl.graphics.Texture
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.FrameBuffer
import java.io.IOException

class VideoElement : Element() {

    private var customVideoPath = "default"
    private var albumArtRequest = AlbumArtRequest("", "", "", "")

    // Playback settings
    private var playbackSpeed = 1.0f
    private var volume = 1.0f
    private var isMuted = false
    private var isLooping = true
    private var scaleMode = 0 // 0: Fit, 1: Fill, 2: Crop

    // Rendering resources
    private var mediaPlayer: MediaPlayer? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null
    private var oesTexture: OESTexture? = null
    private var frameBuffer: FrameBuffer? = null
    private var atlasTexture: AtlasTexture? = null
    private var isVideoReady = false
    private var frameAvailable = false
    private var videoWidth = 0
    private var videoHeight = 0
    
    private var totalRenderTimeMs = 0L
    private var lastSeekTime = 0L
    
    private var color = 0xFFFFFFFF.toInt()

    private val surfaceTextureListener = SurfaceTexture.OnFrameAvailableListener {
        frameAvailable = true
    }

    override fun markNeedReCreateGLResources() {
        releaseVideo()
        super.markNeedReCreateGLResources()
    }

    override fun reCreateGLResources(renderData: RenderState?) {
        markNeedReCreateGLResources()
        super.reCreateGLResources(renderData)
    }

    override fun onCreateGLResources(renderData: RenderState) {
        super.onCreateGLResources(renderData)

        if (customVideoPath.isNotEmpty() && customVideoPath != "default") {
            try {
                mediaPlayer = MediaPlayer()
                mediaPlayer?.setDataSource(customVideoPath)
                mediaPlayer?.isLooping = isLooping
                
                updateVolume()
                
                mediaPlayer?.setOnPreparedListener { mp ->
                    isVideoReady = true
                    videoWidth = mp.videoWidth
                    videoHeight = mp.videoHeight
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
                        }
                    } catch (e: Exception) {}
                }
                
                oesTexture = OESTexture(1, 1)
                surfaceTexture = SurfaceTexture(oesTexture!!.id)
                surfaceTexture?.setOnFrameAvailableListener(surfaceTextureListener)
                surface = Surface(surfaceTexture)
                
                mediaPlayer?.setSurface(surface)
                mediaPlayer?.prepareAsync()
                
            } catch (e: IOException) {
                tlog.w("Failed to load video: " + e.message)
            }
        }
    }

    private fun releaseVideo() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        surface?.release()
        surface = null
        surfaceTexture?.release()
        surfaceTexture = null
        oesTexture?.dispose()
        oesTexture = null
        frameBuffer?.dispose()
        frameBuffer = null
        atlasTexture = null
        isVideoReady = false
        frameAvailable = false
    }

    private fun updateVolume() {
        val finalVolume = if (isMuted) 0f else volume
        mediaPlayer?.setVolume(finalVolume, finalVolume)
    }

    private fun updateSync(renderData: RenderState) {
        if (!isVideoReady || mediaPlayer == null) return

        val duration = mediaPlayer!!.duration
        if (duration <= 0) return

        val rawTrackPositionMs = com.aylis.Design.PlaybackDesign.trackPosition.toInt()
        val targetVideoPosMs = if (isLooping) {
            rawTrackPositionMs % duration
        } else {
            Math.min(rawTrackPositionMs, duration)
        }

        if (renderData.isExportMode) {
            // In export mode, MediaPlayer's real-time nature breaks synchronization.
            // Force seek to exact track time to prevent dropped/desynced frames.
            if (mediaPlayer!!.isPlaying) {
                mediaPlayer?.pause()
            }
            mediaPlayer?.seekTo(targetVideoPosMs)
            return
        }

        val isMusicPlaying = com.aylis.Design.PlaybackDesign.isPlaying
        val currentTime = System.currentTimeMillis()
        val canSeek = (currentTime - lastSeekTime) > 1000
        
        if (isMusicPlaying) {
            val isAtEndAndNotLooping = !isLooping && targetVideoPosMs >= duration

            if (isAtEndAndNotLooping) {
                 if (mediaPlayer!!.isPlaying) mediaPlayer?.pause()
            } else {
                 if (!mediaPlayer!!.isPlaying) mediaPlayer?.start()
            }
            
            // Check drift between video player position and track position
            val currentPos = mediaPlayer!!.currentPosition
            val drift = Math.abs(currentPos - targetVideoPosMs)
            
            if (canSeek && drift > 1500) {
                mediaPlayer?.seekTo(targetVideoPosMs)
                lastSeekTime = currentTime
            }
        } else {
            if (mediaPlayer!!.isPlaying) {
                mediaPlayer?.pause()
            }
            // When paused, if the user scrubs the track, the video should follow
            val currentPos = mediaPlayer!!.currentPosition
            val drift = Math.abs(currentPos - targetVideoPosMs)
            
            if (canSeek && drift > 200) {
                mediaPlayer?.seekTo(targetVideoPosMs)
                lastSeekTime = currentTime
            }
        }
    }

    override fun onEarlyUpdate(renderData: RenderState, resultFB: FrameBuffer?) {
        super.onEarlyUpdate(renderData, resultFB)
        if (!visible) return
        totalRenderTimeMs += renderData.frameTime
        updateSync(renderData)
    }

    override fun onRender(renderData: RenderState, resultFB: FrameBuffer?) {
        super.onRender(renderData, resultFB)
        if (!visible) return

        if (customVideoPath == "default" || customVideoPath.isEmpty()) {
            return // Wait for user to select a video
        }

        if (isVideoReady && frameBuffer == null) {
            val dim = renderData.safeScreenSizeTextureDim
            val fw = Math.min(dim.x, Math.max(1, videoWidth))
            val fh = Math.min(dim.y, Math.max(1, videoHeight))
            
            frameBuffer = FrameBuffer(fw, fh, Texture.LINEAR, Texture.CLAMP_TO_EDGE)
            atlasTexture = AtlasTexture(frameBuffer?.texture)
        }

        if (isVideoReady && frameAvailable && surfaceTexture != null) {
            surfaceTexture?.updateTexImage()
            frameAvailable = false
            
            // Render OES to 2D FrameBuffer
            if (frameBuffer != null && oesTexture != null) {
                renderData.bindFrameBuffer(frameBuffer)
                GLES20.glClearColor(0f, 0f, 0f, 0f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                
                val shader = renderData.res.videoOesShader
                if (shader != null) {
                    renderData.bindShader(shader)
                    
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTexture!!.id)
                    shader.setUniformi("u_texture", 0)
                    
                    // Draw full screen quad directly, bypassing BufferRenderer
                    renderData.res.fullQuad.drawShader(shader, "a_position")
                }
                renderData.bindFrameBuffer(resultFB)
            }
        }

        if (atlasTexture != null && frameBuffer != null) {
            val drawRect = measureDrawRect(renderData.res.meter)
            
            val drawWHRatio = drawRect.width() / drawRect.height()
            val artWHRatio = if (videoHeight > 0) videoWidth.toFloat() / videoHeight.toFloat() else 1.0f
            
            var w = drawRect.width()
            var h = drawRect.height()

            if (scaleMode == 0) { // Fit
                if (artWHRatio > drawWHRatio) {
                    h = w / artWHRatio
                } else {
                    w = artWHRatio * h
                }
            } else if (scaleMode == 2) { // Crop
                if (artWHRatio > drawWHRatio) {
                    w = artWHRatio * h
                } else {
                    h = w / artWHRatio
                }
            }
            
            val x = drawRect.centerX()
            val y = drawRect.centerY()
            val finalDrawRect = RectF(x - w * 0.5f, y - h * 0.5f, x + w * 0.5f, y + h * 0.5f)
            
            updateRenderStates(renderData, resultFB)
            drawRotatedTexture(renderData, finalDrawRect, 0.0f, color, Vec2f.zero, Vec2f.one, atlasTexture)
        }
    }

    override fun onApplyCustomization(customizationData: CustomizationData) {
        super.onApplyCustomization(customizationData)

        val newPath = customizationData.getPropertyString("customVideo", "default")
        if (newPath != customVideoPath) {
            customVideoPath = newPath
            markNeedReCreateGLResources()
        }
        
        color = customizationData.getPropertyInt("color", 0xFFFFFFFF.toInt())

        val newSpeed = customizationData.getPropertyFloat("playbackSpeed", 1.0f)
        if (newSpeed != playbackSpeed) {
            playbackSpeed = newSpeed
            if (isVideoReady) {
                try {
                    mediaPlayer?.playbackParams = mediaPlayer?.playbackParams?.setSpeed(playbackSpeed)!!
                } catch (e: Exception) {}
            }
        }

        val newVolume = customizationData.getPropertyFloat("volume", 1.0f)
        val newMuted = customizationData.getPropertyBool("isMuted", false)
        if (newVolume != volume || newMuted != isMuted) {
            volume = newVolume
            isMuted = newMuted
            updateVolume()
        }

        val newLoop = customizationData.getPropertyBool("isLooping", true)
        if (newLoop != isLooping) {
            isLooping = newLoop
            mediaPlayer?.isLooping = isLooping
        }

        val scaleModeStr = customizationData.getPropertyString("scaleMode", "Fit")
        scaleMode = when (scaleModeStr) {
            "Fill" -> 1
            "Crop" -> 2
            else -> 0 // Fit
        }
    }

    override fun onReadCustomization(outCustomizationData: CustomizationData) {
        super.onReadCustomization(outCustomizationData)
        outCustomizationData.setCustomizationName("video")

        outCustomizationData.putPropertyString("customVideo", customVideoPath, "vid", "1_video")
        outCustomizationData.putPropertyInt("color", color, "crgba", "1_video")
        outCustomizationData.putPropertyFloat("playbackSpeed", playbackSpeed, "f 0.1 4.0", "1_video")
        outCustomizationData.putPropertyFloat("volume", volume, "f 0.0 1.0", "1_video")
        outCustomizationData.putPropertyBool("isMuted", isMuted, "1_video")
        outCustomizationData.putPropertyBool("isLooping", isLooping, "1_video")

        val scaleModes = arrayOf("Fit", "Fill", "Crop")
        outCustomizationData.putPropertyString("scaleMode", scaleModes[scaleMode], "sel Fit Fill Crop", "1_video")
    }

    companion object {
        const val typeName = "VideoElement"
    }
}
