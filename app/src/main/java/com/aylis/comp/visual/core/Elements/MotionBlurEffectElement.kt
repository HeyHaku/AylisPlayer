package com.aylis.comp.visual.core.Elements

import android.graphics.Bitmap
import android.graphics.RectF
import android.opengl.GLES20
import com.aylis.Common.Utils
import com.aylis.Common.Vec2f
import com.aylis.Common.tlog
import com.aylis.comp.AlbumArt.AlbumArtRequest
import com.aylis.comp.AlbumArt.ImageLoadedListener
import com.aylis.comp.visual.core.Elements.Element.CustomizationData
import com.aylis.comp.visual.core.Elements.Base.MVariableFloat
import com.aylis.comp.visual.core.Graphic.AtlasTexture
import com.aylis.comp.visual.core.Graphic.RenderState
import com.aylis.comp.visual.core.Graphic.VShaderProgram
import com.aylis.comp.visual.core.Graphic.VTexture
import com.aylis.comp.visual.core.gl.mdesl.graphics.Texture
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.FrameBuffer
import com.aylis.comp.visual.core.Graphic.SafeMipmapHelper
import com.aylis.comp.visual.core.Elements.PreCompElement
import com.aylis.comp.visual.core.Elements.PreCompManager
import com.aylis.comp.visual.core.Elements.AppBlendMode

class MotionBlurEffectElement : Element(), ImageLoadedListener {

    private var customImagePath = "precomp:1"
    private var albumArtRequest = AlbumArtRequest("", "", "", "")
    private var bitmap: Bitmap? = null
    private var bitmapLoading = false
    private var bitmapLoadedIn = false
    private var imageLoadStrongReference: Any? = null

    private var tex2: Texture? = null
    private var atlasTex2: AtlasTexture? = null

    private var blurAmountMultiplier = MVariableFloat.Companion.createConstantFloat(1.0f)
    private var posBlurX = MVariableFloat.Companion.createConstantFloat(0.5f)
    private var posBlurY = MVariableFloat.Companion.createConstantFloat(0.5f)
    private var scaleBlurAmount = MVariableFloat.Companion.createConstantFloat(0.0f)

    private var relativeMotionMode = true
    private val motionSources = arrayOf("EffectTransform", "Manual")
    private var motionSourceId = "EffectTransform"

    private var lastDrawCenterX = 0f
    private var lastDrawCenterY = 0f
    private var lastDrawWidth = 0f
    private var lastDrawHeight = 0f
    private var deltaDrawCenterX = 0f
    private var deltaDrawCenterY = 0f
    private var deltaDrawWidth = 0f
    private var deltaDrawHeight = 0f
    
    private var showUnblurredContent = false
    private var showUnblurredContentUnder = false
    private var blendModeContent = 2

    private var customShader: VShaderProgram? = null
    private var reloadShader = true

    companion object {
        const val typeName = "MotionBlurEffect"
        const val PRECOMP_PREFIX = "precomp:"
    }

    init {
        setBlendMode(4)
        setScale(1.0f, 1.0f)
    }

    open fun getElementTypeName(): String {
        return typeName
    }

    fun getSelectedPreCompName(): String? {
        if (customImagePath.startsWith(PRECOMP_PREFIX)) {
            return customImagePath.substring(PRECOMP_PREFIX.length)
        }
        return null
    }

    fun setCustomImagePath(path: String?) {
        val targetPath = path ?: ""
        if (this.customImagePath == targetPath) return
        this.customImagePath = targetPath
        if (!targetPath.startsWith(PRECOMP_PREFIX)) {
            this.albumArtRequest = AlbumArtRequest(targetPath, targetPath, "", "")
        }
        this.markNeedReCreateGLResources()
    }

    override fun onBitmapLoaded(bitmap: Bitmap?, dataSource: String?, url0: String?, url1: String?) {
        if (Utils.compareNullStrings(dataSource, albumArtRequest.videoThumbDataSource)) {
            if (Utils.compareNullStrings(url0, albumArtRequest.path0)) {
                if (Utils.compareNullStrings(url1, albumArtRequest.path1)) {
                    this.bitmap = bitmap
                    bitmapLoadedIn = false
                    super.markNeedReCreateGLResources()
                }
            }
        }
    }

    override fun setUserObject1(obj1: Any?) {
        imageLoadStrongReference = obj1
    }

    override fun markNeedReCreateGLResources() {
        bitmap = null
        bitmapLoading = false
        bitmapLoadedIn = false
        reloadShader = true
        super.markNeedReCreateGLResources()
    }

    override fun reCreateGLResources(renderData: RenderState) {
        this.markNeedReCreateGLResources()
        super.reCreateGLResources(renderData)
    }

    override fun onCreateGLResources(renderData: RenderState) {
        super.onCreateGLResources(renderData)

        if (customImagePath.startsWith(PRECOMP_PREFIX)) {
            tex2 = null
            atlasTex2 = null
            bitmap = null
            return
        }

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
            tex2 = null
            atlasTex2 = null
        }
        bitmap = null
    }

    private fun onAlbumArtCreateGLResources(bitmap: Bitmap?) {
        if (bitmap == null) {
            tex2 = null
            atlasTex2 = null
            return
        }
        tex2 = VTexture(
            bitmap,
            VTexture.DEFAULT_FILTER,
            VTexture.DEFAULT_FILTER,
            VTexture.DEFAULT_WRAP,
            false
        )
        atlasTex2 = AtlasTexture(tex2)
    }

    override fun onApplyCustomization(customizationData: CustomizationData) {
        super.onApplyCustomization(customizationData)

        var imagePath = customizationData.getPropertyString("customImage", customImagePath)
        val preCompChild = customizationData.getChild("preCompSelection")
        if (preCompChild != null) {
            val legacyPreComp = preCompChild.childTypeValue
            if (legacyPreComp != null && legacyPreComp.isNotEmpty() && "None" != legacyPreComp) {
                if (imagePath.isEmpty() || !imagePath.startsWith(PRECOMP_PREFIX)) {
                    imagePath = PRECOMP_PREFIX + legacyPreComp
                }
            }
        }
        setCustomImagePath(imagePath)

        blurAmountMultiplier = customizationData.getPropertyMVariableFloat("blurMultiplier", blurAmountMultiplier)
        
        val motionSourceChild = customizationData.getChild("motionSource")
        if (motionSourceChild != null) {
            val childType = motionSourceChild.childTypeValue
            if (childType != null && childType.isNotEmpty()) {
                motionSourceId = childType
            }
        }
        
        posBlurX = customizationData.getPropertyMVariableFloat("posBlurX", posBlurX)
        posBlurY = customizationData.getPropertyMVariableFloat("posBlurY", posBlurY)
        scaleBlurAmount = customizationData.getPropertyMVariableFloat("scaleBlurAmount", scaleBlurAmount)
        
        relativeMotionMode = customizationData.getPropertyBool("relativeMotionMode", relativeMotionMode)
        
        showUnblurredContent = customizationData.getPropertyBool("showUnblurredContent", showUnblurredContent)
        showUnblurredContentUnder = customizationData.getPropertyBool("showUnblurredContentUnder", showUnblurredContentUnder)
        blendModeContent = AppBlendMode.getGlMode(customizationData.getPropertyString("blendModeContent", AppBlendMode.getTypeName(blendModeContent)))
        
        reloadShader = true
    }

    override fun onReadCustomization(outCustomizationData: CustomizationData) {
        super.onReadCustomization(outCustomizationData)
        outCustomizationData.setCustomizationName("Motion Blur Effect")
        
        outCustomizationData.putPropertyString("customImage", customImagePath, "img", "1_appearance")
        outCustomizationData.putPropertyMVariableFloat("blurMultiplier", blurAmountMultiplier, "2_motionBlur", 0.0f, 2.0f)
        
        outCustomizationData.putChild("motionSource", motionSourceId, motionSources, "2_motionBlur")
        outCustomizationData.putPropertyMVariableFloat("posBlurX", posBlurX, "2_motionBlur", 0.0f, 2.0f)
        outCustomizationData.putPropertyMVariableFloat("posBlurY", posBlurY, "2_motionBlur", 0.0f, 2.0f)
        outCustomizationData.putPropertyMVariableFloat("scaleBlurAmount", scaleBlurAmount, "2_motionBlur", -1.0f, 1.0f)
        
        outCustomizationData.putPropertyBool("relativeMotionMode", relativeMotionMode, "2_motionBlur")
        
        outCustomizationData.putPropertyBool("showUnblurredContent", showUnblurredContent, "1_appearance")
        outCustomizationData.putPropertyBool("showUnblurredContentUnder", showUnblurredContentUnder, "1_appearance")
        outCustomizationData.putPropertyString("blendModeContent", AppBlendMode.getTypeName(blendModeContent), AppBlendMode.getSelectorString(), "1_appearance")
    }

    private fun initShader(renderData: RenderState) {
        if (customShader != null && !reloadShader) return
        
        reloadShader = false
        customShader?.dispose()
        
        val shaderVert = """
            uniform mat4 u_projView;
            attribute vec3 Position;
            attribute vec2 TexCoord;
            attribute vec4 Color;
            varying vec4 vColor;
            varying vec2 vTexCoord;
            void main() {
                vColor = Color;
                vTexCoord = TexCoord;
                gl_Position = u_projView * vec4(Position, 1.0);
            }
        """.trimIndent()
        
        val shaderFrag = """
            precision highp float;
            varying vec2 vTexCoord;
            uniform sampler2D u_texture;
            uniform float u_posDeltaX;
            uniform float u_posDeltaY;
            uniform float u_scaleDeltaX;
            uniform float u_scaleDeltaY;
            void main() {
                vec4 sum = vec4(0.0);
                vec2 scaleMul = vec2((vTexCoord.x - 0.5) * 2.0, (vTexCoord.y - 0.5) * 2.0);
                
                vec2 dirAmount2 = vec2(
                    u_posDeltaX + (abs(u_scaleDeltaX) * scaleMul.x),
                    u_posDeltaY + (abs(u_scaleDeltaY) * scaleMul.y)
                );
                
                // Sigma 3.0; Size 9 - Exact Donor Weights
                sum += texture2D(u_texture, vTexCoord + (-4.0 * dirAmount2)) * 0.063327;
                sum += texture2D(u_texture, vTexCoord + (-3.0 * dirAmount2)) * 0.093095;
                sum += texture2D(u_texture, vTexCoord + (-2.0 * dirAmount2)) * 0.122589;
                sum += texture2D(u_texture, vTexCoord + (-1.0 * dirAmount2)) * 0.144599;
                sum += texture2D(u_texture, vTexCoord) * 0.152781;
                sum += texture2D(u_texture, vTexCoord + (1.0 * dirAmount2)) * 0.144599;
                sum += texture2D(u_texture, vTexCoord + (2.0 * dirAmount2)) * 0.122589;
                sum += texture2D(u_texture, vTexCoord + (3.0 * dirAmount2)) * 0.093095;
                sum += texture2D(u_texture, vTexCoord + (4.0 * dirAmount2)) * 0.063327;
                
                gl_FragColor = sum;
            }
        """.trimIndent()
        
        try {
            customShader = VShaderProgram(shaderVert, shaderFrag)
        } catch (e: Exception) {
            tlog.w("MotionBlurEffectElement failed to compile shader: " + e.message)
            customShader = null
        }
    }

    override fun onRender(renderData: RenderState, resultFB: FrameBuffer?) {
        initShader(renderData)
        
        var targetTex: Texture? = null
        var targetAtlasTex: AtlasTexture? = null

        val preCompName = getSelectedPreCompName()
        if (preCompName != null) {
            val targetPreComp = PreCompManager.get(preCompName)
            if (targetPreComp != null) {
                targetPreComp.renderOnDemand(renderData, resultFB)
                val preCompTex = targetPreComp.texture
                if (SafeMipmapHelper.isTextureReady(preCompTex)) {
                    targetTex = preCompTex
                    targetAtlasTex = AtlasTexture(targetTex)
                } else {
                    targetAtlasTex = renderData.res.atlasTexBlack
                    if (targetAtlasTex?.texture2D?.texture != null) {
                        targetTex = targetAtlasTex.texture2D.texture
                    }
                }
            }
        } else {
            targetTex = tex2
            targetAtlasTex = atlasTex2
        }

        onRenderCheckResources(renderData)

        if (targetAtlasTex == null || customShader == null) {
            super.onRender(renderData, resultFB)
            return
        }

        val meter = renderData.res.meter
        val drawRect = measureDrawRect(meter)

        val isPreComp = preCompName != null
        
        val multiplier = blurAmountMultiplier.getValueAsFloat(meter)

        val color = 0xFFFFFFFF.toInt()
        val w = drawRect.width()
        val h = drawRect.height()
        val x = drawRect.centerX()
        val y = drawRect.centerY()
        val rotatedDrawRect = RectF(x - w * 0.5f, y - h * 0.5f, x + w * 0.5f, y + h * 0.5f)

        // True Motion Blur Logic (Exact Donor Frame Delta)
        var varCenterX: Float
        var varCenterY: Float
        var varWidth: Float
        var varHeight: Float
        var isRelative = false

        if (motionSources[1] == motionSourceId) {
            val posX = posBlurX.getValueAsFloat(meter)
            val posY = posBlurY.getValueAsFloat(meter)
            
            val fMeasureScreenSpaceX = meter.measureScreenSpaceX(0.5f, true) - meter.measureScreenSpaceX(posX, true)
            val fMeasureScreenSpaceY = meter.measureScreenSpaceY(0.5f, true) - meter.measureScreenSpaceY(posY, true)
            
            val scale = scaleBlurAmount.getValueAsFloat(meter)
            val fMeasureScreenScaleX = meter.measureScreenScaleX(scale, false)
            val fMeasureScreenScaleY = meter.measureScreenScaleY(scale, false)
            
            val fMeasureLocalSpaceX = fMeasureScreenSpaceX - meter.measureLocalSpaceX(0.5f, false, fMeasureScreenScaleX, fMeasureScreenScaleY)
            val fMeasureLocalSpaceY = fMeasureScreenSpaceY - meter.measureLocalSpaceY(0.5f, false, fMeasureScreenScaleX, fMeasureScreenScaleY)
            varWidth = fMeasureScreenScaleX
            varHeight = fMeasureScreenScaleY
            varCenterX = fMeasureLocalSpaceX + (fMeasureScreenScaleX * 0.5f)
            varCenterY = fMeasureLocalSpaceY + (fMeasureScreenScaleY * 0.5f)
        } else {
            varWidth = w
            varHeight = h
            varCenterX = x
            varCenterY = y
            isRelative = true 
        }

        if (isRelative) {
            deltaDrawCenterX = lastDrawCenterX - varCenterX
            deltaDrawCenterY = lastDrawCenterY - varCenterY
            deltaDrawWidth = lastDrawWidth - varWidth
            deltaDrawHeight = lastDrawHeight - varHeight
        } else {
            deltaDrawCenterX = varCenterX
            deltaDrawCenterY = varCenterY
            deltaDrawWidth = varWidth
            deltaDrawHeight = varHeight
        }
        
        lastDrawCenterX = varCenterX
        lastDrawCenterY = varCenterY
        lastDrawWidth = varWidth
        lastDrawHeight = varHeight
        
        if (showUnblurredContentUnder) {
            renderData.res.bufferRenderer.flush(renderData)
            renderData.setBlendMode(blendModeContent)
            drawRotatedTexture(renderData, rotatedDrawRect, 0.0f, color, if (isPreComp) Vec2f(0.0f, 1.0f) else Vec2f(0.0f, 0.0f), if (isPreComp) Vec2f(1.0f, 0.0f) else Vec2f(1.0f, 1.0f), targetAtlasTex)
            renderData.res.bufferRenderer.flush(renderData)
        }
        
        // Inject custom shader and draw
        renderData.res.bufferRenderer.flush(renderData)
        renderData.bindShader(customShader)
        
        customShader?.setUniformMatrix("u_projView", false, renderData.vpMatrix)
        customShader?.setUniformi("u_texture", 0)
        
        // Match donor shader uniform logic
        val texWidth = targetAtlasTex.texture2D?.texture?.width?.toFloat() ?: 1.0f
        val texHeight = targetAtlasTex.texture2D?.texture?.height?.toFloat() ?: 1.0f
        val widthMult = 1.0f / texWidth
        val heightMult = 1.0f / texHeight
        
        val valueAsFloat = blurAmountMultiplier.getValueAsFloat(meter)
        
        var vecPosX = deltaDrawCenterX * valueAsFloat * widthMult
        var vecPosY = deltaDrawCenterY * valueAsFloat * heightMult
        
        val posLen = kotlin.math.sqrt((vecPosX * vecPosX + vecPosY * vecPosY).toDouble()).toFloat()
        if (posLen > 4.0f) {
            val inv = 1.0f / posLen
            vecPosX *= inv
            vecPosY *= inv
            vecPosX *= 4.0f
            vecPosY *= 4.0f
        }
        
        customShader?.setUniformf("u_posDeltaX", -vecPosX * 2.0f)
        customShader?.setUniformf("u_posDeltaY", vecPosY * 2.0f)
        
        val vecScaleX = deltaDrawWidth * valueAsFloat * widthMult
        val vecScaleY = deltaDrawHeight * valueAsFloat * heightMult
        
        customShader?.setUniformf("u_scaleDeltaX", -vecScaleX * 2.0f)
        customShader?.setUniformf("u_scaleDeltaY", vecScaleY * 2.0f)
        
        renderData.res.bufferRenderer.setOverrideShader(customShader)
        
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        targetAtlasTex.texture2D?.texture?.bind()
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        
        renderData.setBlendMode(blendMode)
        drawRotatedTexture(renderData, rotatedDrawRect, 0.0f, color, if (isPreComp) Vec2f(0.0f, 1.0f) else Vec2f(0.0f, 0.0f), if (isPreComp) Vec2f(1.0f, 0.0f) else Vec2f(1.0f, 1.0f), targetAtlasTex)
        
        renderData.res.bufferRenderer.flush(renderData)
        renderData.res.bufferRenderer.setOverrideShader(null)
        
        if (showUnblurredContent) {
            renderData.setBlendMode(blendModeContent)
            drawRotatedTexture(renderData, rotatedDrawRect, 0.0f, color, if (isPreComp) Vec2f(0.0f, 1.0f) else Vec2f(0.0f, 0.0f), if (isPreComp) Vec2f(1.0f, 0.0f) else Vec2f(1.0f, 1.0f), targetAtlasTex)
        }
        
        super.onRender(renderData, resultFB)
    }
}
