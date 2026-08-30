

package com.aylis.comp.visual.core.Elements.Images

import android.graphics.Bitmap
import android.graphics.RectF
import com.aylis.Common.Utils
import com.aylis.Common.Vec2f
import com.aylis.Common.tlog
import com.aylis.comp.AlbumArt.AlbumArtRequest
import com.aylis.comp.AlbumArt.ImageLoadedListener
import com.aylis.comp.visual.core.Elements.Element
import com.aylis.comp.visual.core.Elements.PreCompElement
import com.aylis.comp.visual.core.Elements.PreCompManager
import com.aylis.comp.visual.core.Graphic.AtlasTexture
import com.aylis.comp.visual.core.Graphic.RenderState
import com.aylis.comp.visual.core.Graphic.VTexture
import com.aylis.comp.visual.core.gl.mdesl.graphics.Texture
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.FrameBuffer
import com.aylis.comp.visual.core.Graphic.SafeMipmapHelper
import com.yahel.FastBlur

open class ImageElement : Element, ImageLoadedListener {

    private var customImagePath = "default"
    private var albumArtRequest = AlbumArtRequest("", "", "", "")
    private var bitmap: Bitmap? = null
    private var bitmapLoading = false
    private var bitmapLoadedIn = false
    private var imageLoadStrongReference: Any? = null

    private var blurredBorder = true
    private var corners = 0
    private var keepAspectRatio = false
    private var color = 0xFFFFFFFF.toInt()

    private var tex1: Texture? = null
    private var atlasTex1: AtlasTexture? = null
    private var tex2: Texture? = null
    private var atlasTex2: AtlasTexture? = null

    private var maskImagePath = ""
    private var maskMode = 0
    private var maskAlbumArtRequest = AlbumArtRequest("", "", "", "")
    private var maskBitmap: Bitmap? = null
    private var maskBitmapLoading = false
    private var maskBitmapLoadedIn = false
    private var maskTex: Texture? = null
    private var atlasMaskTex: AtlasTexture? = null
    private var maskLockScaleRatio = false
    private var maskScale = Vec2f(1f, 1f)

    private var chromaKeyEnabled = false
    private var chromaKeyColor = 0xFF00FF00.toInt()
    private var chromaKeyTolerance = 0.4f
    private var chromaKeySmoothness = 0.1f

    constructor() : super()

    fun getSelectedPreCompName(): String? {
        return if (customImagePath.startsWith(PRECOMP_PREFIX)) {
            customImagePath.substring(PRECOMP_PREFIX.length)
        } else {
            null
        }
    }

    fun getSelectedMaskPreCompName(): String? {
        return if (maskImagePath.startsWith(PRECOMP_PREFIX)) {
            maskImagePath.substring(PRECOMP_PREFIX.length)
        } else {
            null
        }
    }

    fun setCustomImagePath(path: String?) {
        var targetPath = path ?: ""
        if (targetPath.isEmpty() || targetPath.trim().equals("default", ignoreCase = true)) {
            targetPath = "default"
        }
        if (customImagePath == targetPath) return
        customImagePath = targetPath
        if (customImagePath != "default" && !customImagePath.startsWith(PRECOMP_PREFIX)) {
            albumArtRequest = AlbumArtRequest(customImagePath, customImagePath, "", "")
        }
        markNeedReCreateGLResources()
    }

    fun setMaskImagePath(path: String?) {
        val targetPath = path ?: ""
        if (maskImagePath == targetPath) return
        maskImagePath = targetPath
        if (!maskImagePath.startsWith(PRECOMP_PREFIX)) {
            maskAlbumArtRequest = AlbumArtRequest(maskImagePath, maskImagePath, "", "")
        }
        markNeedReCreateGLResources()
    }

    private fun updateCurrentAlbumArtId(renderData: RenderState) {
        val result = renderData.res.visualizationData.onRequestsAlbumArtPath()
        val newRequest = result ?: AlbumArtRequest("", "", "", "")

        if (Utils.compareNullStrings(albumArtRequest.videoThumbDataSource, newRequest.videoThumbDataSource)) {
            if (Utils.compareNullStrings(albumArtRequest.path0, newRequest.path0)) {
                if (Utils.compareNullStrings(albumArtRequest.path1, newRequest.path1)) {
                    if (Utils.compareNullStrings(albumArtRequest.genStr, newRequest.genStr)) {
                        return
                    }
                }
            }
        }

        this.albumArtRequest = newRequest
        this.markNeedReCreateGLResources()
    }

    override fun onBitmapLoaded(bitmap: Bitmap?, dataSource: String?, url0: String?, url1: String?) {
        var needsUpdate = false
        if (Utils.compareNullStrings(dataSource, albumArtRequest.videoThumbDataSource)) {
            if (Utils.compareNullStrings(url0, albumArtRequest.path0)) {
                if (Utils.compareNullStrings(url1, albumArtRequest.path1)) {
                    this.bitmap = bitmap
                    bitmapLoadedIn = false
                    needsUpdate = true
                }
            }
        }
        if (Utils.compareNullStrings(dataSource, maskAlbumArtRequest.videoThumbDataSource)) {
            if (Utils.compareNullStrings(url0, maskAlbumArtRequest.path0)) {
                if (Utils.compareNullStrings(url1, maskAlbumArtRequest.path1)) {
                    this.maskBitmap = bitmap
                    maskBitmapLoadedIn = false
                    needsUpdate = true
                }
            }
        }
        if (needsUpdate) {
            super.markNeedReCreateGLResources()
        }
    }

    override fun setUserObject1(obj1: Any?) {
        imageLoadStrongReference = obj1
    }

    override fun markNeedReCreateGLResources() {
        bitmap = null
        bitmapLoading = false
        bitmapLoadedIn = false
        maskBitmap = null
        maskBitmapLoading = false
        maskBitmapLoadedIn = false
        super.markNeedReCreateGLResources()
    }

    override fun reCreateGLResources(renderData: RenderState?) {
        markNeedReCreateGLResources()
        super.reCreateGLResources(renderData)
    }

    override fun onCreateGLResources(renderData: RenderState) {
        super.onCreateGLResources(renderData)

        if (customImagePath.startsWith(PRECOMP_PREFIX)) {
            tex1 = null
            atlasTex1 = null
            tex2 = null
            atlasTex2 = null
            bitmap = null
        } else {
            if (customImagePath.isNotEmpty()) {
                if (!bitmapLoading) {
                    bitmapLoading = true
                    val targetBoundsWidth = renderData.fullscreenWidth
                    val targetBoundsHeight = renderData.fullscreenHeight

                    renderData.res.visualizationData.onRequestAlbumArtPathAndBitmap(
                        this,
                        targetBoundsWidth,
                        targetBoundsHeight,
                        albumArtRequest.makeCopy()
                    )
                }

                if (!bitmapLoadedIn) {
                    bitmapLoadedIn = true
                    onAlbumArtCreateGLResources(bitmap)
                }
            } else {
                tex1 = null
                atlasTex1 = null
                tex2 = null
                atlasTex2 = null
            }
            bitmap = null
        }

        if (maskImagePath.startsWith(PRECOMP_PREFIX)) {
            maskTex = null
            atlasMaskTex = null
            maskBitmap = null
        } else {
            if (maskImagePath.isNotEmpty()) {
                if (!maskBitmapLoading) {
                    maskBitmapLoading = true
                    val targetBoundsWidth = renderData.fullscreenWidth
                    val targetBoundsHeight = renderData.fullscreenHeight

                    renderData.res.visualizationData.onRequestAlbumArtPathAndBitmap(
                        this,
                        targetBoundsWidth,
                        targetBoundsHeight,
                        maskAlbumArtRequest.makeCopy()
                    )
                }

                if (!maskBitmapLoadedIn) {
                    maskBitmapLoadedIn = true
                    onMaskCreateGLResources(maskBitmap)
                }
            } else {
                maskTex = null
                atlasMaskTex = null
            }
            maskBitmap = null
        }
    }

    protected fun onMaskCreateGLResources(bitmap: Bitmap?) {
        if (bitmap == null) {
            maskTex = null
            atlasMaskTex = null
            return
        }
        maskTex = VTexture(
            bitmap,
            VTexture.DEFAULT_FILTER,
            VTexture.DEFAULT_FILTER,
            VTexture.DEFAULT_WRAP,
            false
        )
        atlasMaskTex = AtlasTexture(maskTex)
    }

    protected fun onAlbumArtCreateGLResources(bitmap: Bitmap?) {
        if (bitmap == null) {
            tex1 = null
            atlasTex1 = null
            tex2 = null
            atlasTex2 = null
            return
        }

        var safeBitmap = bitmap
        if (safeBitmap.config != Bitmap.Config.ARGB_8888) {
            safeBitmap = safeBitmap.copy(Bitmap.Config.ARGB_8888, false)
        }

        var bitmapSmall: Bitmap? = null
        if (blurredBorder) {
            bitmapSmall = Bitmap.createScaledBitmap(safeBitmap, 32, 32, true)
            try {
                bitmapSmall = FastBlur.fastBlur(bitmapSmall, 7)
            } catch (e: Exception) {
                tlog.w("Art blurring failed: " + e.message)
            }
        }

        if (bitmapSmall != null) {
            tex1 = VTexture(
                bitmapSmall,
                VTexture.DEFAULT_FILTER,
                VTexture.DEFAULT_FILTER,
                VTexture.DEFAULT_WRAP,
                false
            )
            atlasTex1 = AtlasTexture(tex1)
        } else {
            tex1 = null
            atlasTex1 = null
        }

        tex2 = VTexture(
            safeBitmap,
            VTexture.DEFAULT_FILTER,
            VTexture.DEFAULT_FILTER,
            VTexture.DEFAULT_WRAP,
            false
        )
        atlasTex2 = AtlasTexture(tex2)
    }

    override fun onRender(renderData: RenderState, resultFB: FrameBuffer?) {
        super.onRender(renderData, resultFB)

        if (customImagePath == "default" || customImagePath.isEmpty()) {
            updateCurrentAlbumArtId(renderData)
        }

        val drawRect = measureDrawRect(renderData.res.meter)

        var targetTex: Texture? = null
        var targetAtlasTex: AtlasTexture? = null

        val preCompName = getSelectedPreCompName()
        var targetPreComp: PreCompElement? = null
        if (preCompName != null) {
            targetPreComp = PreCompManager.get(preCompName)
        }

        if (targetPreComp != null) {
            targetPreComp.renderOnDemand(renderData, resultFB)
            val preCompTex = targetPreComp.texture
            if (SafeMipmapHelper.isTextureReady(preCompTex)) {
                targetTex = preCompTex
                targetAtlasTex = AtlasTexture(targetTex)
            } else {
                // FBO пре-композиции ещё не готов — используем заглушку
                targetAtlasTex = renderData.res.atlasTexBlack
                if (targetAtlasTex?.texture2D?.texture != null) {
                    targetTex = targetAtlasTex.texture2D.texture
                }
            }
        } else {
            targetTex = tex2
            targetAtlasTex = atlasTex2
        }

        if (targetAtlasTex != null) {
            val drawWHRatio = drawRect.width() / drawRect.height()
            var w = targetAtlasTex.getWidth().toFloat()
            var h = targetAtlasTex.getHeight().toFloat()
            val artWHRatio = if (h > 0) w / h else 1.0f

            var borderNeeded = 0

            if (keepAspectRatio) {
                if (artWHRatio > drawWHRatio) {
                    w = drawRect.width()
                    h = w / artWHRatio
                    borderNeeded++
                } else if (artWHRatio < drawWHRatio) {
                    h = drawRect.height()
                    w = artWHRatio * h
                    borderNeeded++
                } else {
                    w = drawRect.width()
                    h = drawRect.height()
                }
            } else {
                w = drawRect.width()
                h = drawRect.height()
            }

            val maskPreCompName = getSelectedMaskPreCompName()
            var maskPreComp: PreCompElement? = null
            if (maskPreCompName != null) {
                maskPreComp = PreCompManager.get(maskPreCompName)
            }

            var activeMaskAtlasTex: AtlasTexture? = null
            if (maskPreComp != null) {
                maskPreComp.renderOnDemand(renderData, resultFB)
                val preCompTex = maskPreComp.texture
                if (SafeMipmapHelper.isTextureReady(preCompTex)) {
                    activeMaskAtlasTex = AtlasTexture(preCompTex)
                } else {
                    activeMaskAtlasTex = renderData.res.atlasTexBlack
                }
            } else {
                activeMaskAtlasTex = atlasMaskTex
            }

            val hasMask = maskImagePath.isNotEmpty() && activeMaskAtlasTex != null

            if (targetPreComp == null && atlasTex1 != null && borderNeeded > 0 && blurredBorder) {
                drawRotatedTexture(renderData, drawRect, 0.0f, color, Vec2f.zero, Vec2f.one, atlasTex1)
            }

            if (hasMask || chromaKeyEnabled) {
                renderData.res.bufferRenderer.flush(renderData)
                val activeShader = if (hasMask) {
                    if (chromaKeyEnabled) renderData.res.atlasBufferMaskChromaKeyShader else renderData.res.atlasBufferMaskShader
                } else {
                    renderData.res.atlasBufferChromaKeyShader
                }
                renderData.bindShader(activeShader)
                activeShader.setUniformMatrix("u_projView", false, renderData.vpMatrix)
                activeShader.setUniformi("u_texture", 0)

                if (chromaKeyEnabled) {
                    val r = ((chromaKeyColor shr 16) and 0xFF) / 255.0f
                    val g = ((chromaKeyColor shr 8) and 0xFF) / 255.0f
                    val b = (chromaKeyColor and 0xFF) / 255.0f
                    activeShader.setUniformf("u_chromaKeyColor", r, g, b)
                    activeShader.setUniformf("u_chromaKeyTolerance", chromaKeyTolerance)
                    activeShader.setUniformf("u_chromaKeySmoothness", chromaKeySmoothness)
                }

                if (hasMask) {
                    activeShader.setUniformi("u_texture2", 1)
                    
                    when (maskMode) {
                        0 -> { // Transparency
                            activeShader.setUniformf("maskadd", 0.0f)
                            activeShader.setUniformf("maskmul", 1.0f)
                            activeShader.setUniformf("mask_l_add", 1.0f)
                            activeShader.setUniformf("mask_l_mul", 0.0f)
                        }
                        1 -> { // TransparencyAndBlacks
                            activeShader.setUniformf("maskadd", 0.0f)
                            activeShader.setUniformf("maskmul", 1.0f)
                            activeShader.setUniformf("mask_l_add", 0.0f)
                            activeShader.setUniformf("mask_l_mul", 1.0f)
                        }
                        2 -> { // TransparencyAndWhites
                            activeShader.setUniformf("maskadd", 0.0f)
                            activeShader.setUniformf("maskmul", 1.0f)
                            activeShader.setUniformf("mask_l_add", 1.0f)
                            activeShader.setUniformf("mask_l_mul", -1.0f)
                        }
                        3 -> { // InvertedTransparency
                            activeShader.setUniformf("maskadd", 1.0f)
                            activeShader.setUniformf("maskmul", -1.0f)
                            activeShader.setUniformf("mask_l_add", 1.0f)
                            activeShader.setUniformf("mask_l_mul", 0.0f)
                        }
                        else -> {
                            activeShader.setUniformf("maskadd", 0.0f)
                            activeShader.setUniformf("maskmul", 1.0f)
                            activeShader.setUniformf("mask_l_add", 1.0f)
                            activeShader.setUniformf("mask_l_mul", 0.0f)
                        }
                    }

                    val tex2_x_mul = 1.0f / maskScale.x
                    val tex2_x_add = 0.5f * (1.0f - tex2_x_mul)
                    activeShader.setUniformf("tex2_x_add", tex2_x_add)
                    activeShader.setUniformf("tex2_x_mul", tex2_x_mul)

                    val finalMaskScaleY = if (maskLockScaleRatio) maskScale.x else maskScale.y

                    if (maskPreComp != null) {
                        val tex2_y_mul = -1.0f / finalMaskScaleY
                        val tex2_y_add = 0.5f + 0.5f / finalMaskScaleY
                        activeShader.setUniformf("tex2_y_add", tex2_y_add)
                        activeShader.setUniformf("tex2_y_mul", tex2_y_mul)
                    } else {
                        val tex2_y_mul = 1.0f / finalMaskScaleY
                        val tex2_y_add = 0.5f * (1.0f - tex2_y_mul)
                        activeShader.setUniformf("tex2_y_add", tex2_y_add)
                        activeShader.setUniformf("tex2_y_mul", tex2_y_mul)
                    }

                    android.opengl.GLES20.glActiveTexture(android.opengl.GLES20.GL_TEXTURE1)
                    activeMaskAtlasTex?.texture2D?.texture?.bind()
                    android.opengl.GLES20.glActiveTexture(android.opengl.GLES20.GL_TEXTURE0)
                }

                renderData.res.bufferRenderer.setOverrideShader(activeShader)
            }

            val isPreComp = targetPreComp != null
            val tex0 = if (isPreComp) Vec2f(0.0f, 1.0f) else Vec2f.zero
            val tex1 = if (isPreComp) Vec2f(1.0f, 0.0f) else Vec2f.one

            if (corners == 0) {
                val x = drawRect.centerX()
                val y = drawRect.centerY()
                val rotatedDrawRect = RectF(x - w * 0.5f, y - h * 0.5f, x + w * 0.5f, y + h * 0.5f)
                drawRotatedTexture(renderData, rotatedDrawRect, 0.0f, color, tex0, tex1, targetAtlasTex)
            } else {
                val actualCorners = if (corners >= 9) 0 else corners + 2
                renderCircle(renderData, drawRect, targetAtlasTex, isPreComp, actualCorners)
            }

            if (hasMask || chromaKeyEnabled) {
                renderData.res.bufferRenderer.flush(renderData)
                renderData.res.bufferRenderer.setOverrideShader(null)
            }
        }
    }

    private fun renderCircle(renderData: RenderState, drawRect: RectF, targetAtlasTex: AtlasTexture, isPreComp: Boolean = false, corners: Int = 0) {
        val drawWHRatio = drawRect.width() / drawRect.height()
        val artw = targetAtlasTex.getWidth().toFloat()
        val arth = targetAtlasTex.getHeight().toFloat()
        val artWHRatio = if (arth > 0) artw / arth else 1.0f

        val w2: Float
        val h2: Float
        if (artWHRatio > drawWHRatio) {
            w2 = drawRect.width()
            h2 = w2 / artWHRatio
        } else if (artWHRatio < drawWHRatio) {
            h2 = drawRect.height()
            w2 = artWHRatio * h2
        } else {
            w2 = drawRect.width()
            h2 = drawRect.height()
        }

        var x = drawRect.centerX()
        var y = drawRect.centerY()

        var texXMul: Float
        var texYMul: Float

        if (w2 > h2) {
            texXMul = 1.0f / artWHRatio
            texYMul = 1.0f
        } else {
            texXMul = 1.0f
            texYMul = 1.0f * artWHRatio
        }

        texXMul *= 0.5f
        texYMul *= 0.5f

        val smallest = if (w2 < h2) h2 else w2

        x -= smallest * 0.5f
        y -= smallest * 0.5f

        val v0 = if (isPreComp) 0.5f + texYMul else 0.5f - texYMul
        val v1 = if (isPreComp) 0.5f - texYMul else 0.5f + texYMul

        if (corners == 0) {
            renderData.res.bufferRenderer.drawCircleSegmentW(
                renderData,
                x, y, 0.0f,
                smallest, smallest,
                color,
                Vec2f(0.5f - texXMul, v0), Vec2f(0.5f + texXMul, v1),
                targetAtlasTex, 18.0f
            )
        } else {
            val rotOffset = if (corners == 4) 0.125f else 0.0f
            renderData.res.bufferRenderer.drawCircle(
                renderData,
                x, y, 0.0f,
                smallest, smallest,
                color,
                Vec2f(0.5f - texXMul, v0), Vec2f(0.5f + texXMul, v1),
                targetAtlasTex, corners, rotOffset
            )
        }
    }

    override fun onApplyCustomization(customizationData: CustomizationData) {
        super.onApplyCustomization(customizationData)

        var imagePath = customizationData.getPropertyString("customImage", "default")
        val preCompChild = customizationData.getChild("preCompSelection")
        if (preCompChild != null) {
            val legacyPreComp = preCompChild.childTypeValue
            if (legacyPreComp != null && legacyPreComp.isNotEmpty() && "None" != legacyPreComp) {
                if (imagePath.isEmpty()) {
                    imagePath = PRECOMP_PREFIX + legacyPreComp
                }
            }
        }

        setCustomImagePath(imagePath)
        color = customizationData.getPropertyInt("color", 0xFFFFFFFF.toInt())
        keepAspectRatio = customizationData.getPropertyBool("keepAspectRatio", keepAspectRatio)
        blurredBorder = customizationData.getPropertyBool("blurredBorder", blurredBorder)
        val legacyCircleShape = customizationData.getPropertyBool("circleShape", corners >= 9)
        corners = customizationData.getPropertyInt("corners", if (legacyCircleShape) 9 else 0)

        val maskPath = customizationData.getPropertyString("MaskImage", "")
        setMaskImagePath(maskPath)
        val maskModeStr = customizationData.getPropertyString("maskMode", ImageMask.maskModes[0])
        maskMode = ImageMask.maskModes.indexOf(maskModeStr).takeIf { it >= 0 } ?: 0
        maskLockScaleRatio = customizationData.getPropertyBool("maskLockScaleRatio", maskLockScaleRatio)
        maskScale = customizationData.getPropertyVec2f("maskScale", maskScale)
        
        chromaKeyEnabled = customizationData.getPropertyBool("chromaKeyEnabled", chromaKeyEnabled)
        chromaKeyColor = customizationData.getPropertyInt("chromaKeyColor", chromaKeyColor)
        chromaKeyTolerance = customizationData.getPropertyFloat("chromaKeyTolerance", chromaKeyTolerance)
        chromaKeySmoothness = customizationData.getPropertyFloat("chromaKeySmoothness", chromaKeySmoothness)
    }

    override fun onReadCustomization(outCustomizationData: CustomizationData) {
        super.onReadCustomization(outCustomizationData)

        outCustomizationData.setCustomizationName("media")
        outCustomizationData.putPropertyString("customImage", customImagePath, "img", "1_image")
        outCustomizationData.putPropertyInt("color", color, "crgba", "2_color")
        outCustomizationData.putPropertyBool("keepAspectRatio", keepAspectRatio, "1_image")
        outCustomizationData.putPropertyBool("blurredBorder", blurredBorder, "1_image")
        outCustomizationData.putPropertyInt("corners", corners, "i 0 10", "1_image")

        outCustomizationData.putPropertyString("MaskImage", maskImagePath, "img", "1_image")
        outCustomizationData.putPropertyString("maskMode", ImageMask.maskModes.getOrNull(maskMode) ?: ImageMask.maskModes[0], "sel " + ImageMask.maskModes.joinToString(" "), "1_image")
        outCustomizationData.putPropertyBool("maskLockScaleRatio", maskLockScaleRatio, "1_image")
        outCustomizationData.putPropertyVec2f("maskScale", maskScale, "f2 0.0 10.0", "1_image")
        
        outCustomizationData.putPropertyBool("chromaKeyEnabled", chromaKeyEnabled, "2_color")
        outCustomizationData.putPropertyInt("chromaKeyColor", chromaKeyColor, "crgba", "2_color")
        outCustomizationData.putPropertyFloat("chromaKeyTolerance", chromaKeyTolerance, "f 0.0 1.0", "2_color")
        outCustomizationData.putPropertyFloat("chromaKeySmoothness", chromaKeySmoothness, "f 0.0 1.0", "2_color")
    }

    companion object {
        const val typeName = "Image"
        const val PRECOMP_PREFIX = "precomp:"
    }
}