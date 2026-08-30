package com.aylis.comp.visual.core.Elements.Shaders

import android.graphics.Bitmap
import android.graphics.RectF
import com.aylis.comp.AlbumArt.AlbumArtRequest
import com.aylis.comp.AlbumArt.ImageLoadedListener
import com.aylis.comp.visual.core.Elements.Base.MVariableFloat
import com.aylis.comp.visual.core.Elements.Element.CustomizationData
import com.aylis.comp.visual.core.Elements.Element
import com.aylis.comp.visual.core.Elements.PreCompElement
import com.aylis.comp.visual.core.Elements.PreCompManager
import com.aylis.comp.visual.core.Graphic.AtlasTexture
import com.aylis.comp.visual.core.Graphic.RenderState
import com.aylis.comp.visual.core.Graphic.VShaderProgram
import com.aylis.comp.visual.core.Graphic.VTexture
import com.aylis.comp.visual.core.gl.mdesl.graphics.Texture
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.FrameBuffer
import com.aylis.Common.Utils
import com.aylis.Common.tlog
import kotlin.math.max

class RainDropsEffectElement : Element(), ImageLoadedListener {

    private var customImagePath = ""
    private var albumArtRequest = AlbumArtRequest("", "", "", "")
    private var bitmap: Bitmap? = null
    private var bitmapLoading = false
    private var bitmapLoadedIn = false
    private var imageLoadStrongReference: Any? = null
    
    private var tex2: Texture? = null
    private var atlasTex2: AtlasTexture? = null

    private var loadedShader: VShaderProgram? = null
    private var reloadShader = true
    private var accumulatedTime = 0f
    
    // Audio Reaction matching ParticlesElement logic
    private var bassReaction = 1.0f
    private var currentBass = 0.0f
    
    private var effectSpeed = 1.0f

    // Structural properties
    private var rainAmount = 0.8f
    private var dropSpeed = 0.75f
    private var dropDensity = 6.0f
    private var trailLength = 0.02f
    private var trailWidth = 0.23f
    private var wiggleAmount = 1.0f
    
    private var zoomAmount = 1.0f
    private var lightningAmount = 1.0f

    private var uAspectRatio = MVariableFloat.createConstantFloat(1.777f)

    init {
        setBlendMode(4)
        setScale(1.0f, 1.0f)
        setCustomImagePath("composition:1")
    }

    override fun onApplyCustomization(customizationData: CustomizationData) {
        super.onApplyCustomization(customizationData)
        setCustomImagePath(customizationData.getPropertyString("customImage", customImagePath))
        
        uAspectRatio = customizationData.getPropertyMVariableFloat("aspectRatio", uAspectRatio)
        
        effectSpeed = customizationData.getPropertyFloat("speed", effectSpeed)
        bassReaction = customizationData.getPropertyFloat("bassReaction", bassReaction)
        
        rainAmount = customizationData.getPropertyFloat("rainAmount", rainAmount)
        dropSpeed = customizationData.getPropertyFloat("dropSpeed", dropSpeed)
        dropDensity = customizationData.getPropertyFloat("dropDensity", dropDensity)
        wiggleAmount = customizationData.getPropertyFloat("wiggleAmount", wiggleAmount)
        
        zoomAmount = customizationData.getPropertyFloat("zoomAmount", zoomAmount)
        lightningAmount = customizationData.getPropertyFloat("lightningAmount", lightningAmount)
    }

    override fun onReadCustomization(outCustomizationData: CustomizationData) {
        super.onReadCustomization(outCustomizationData)
        outCustomizationData.setCustomizationName("Rain Drops")
        
        outCustomizationData.putPropertyString("customImage", customImagePath, "img", "1_image")
        outCustomizationData.putPropertyMVariableFloat("aspectRatio", uAspectRatio, "2_motion", 0.1f, 3.0f)
        
        outCustomizationData.putPropertyFloat("speed", effectSpeed, "f 0.0 5.0", "2_motion")
        outCustomizationData.putPropertyFloat("bassReaction", bassReaction, "f 0.0 10.0", "2_motion")
        outCustomizationData.putPropertyFloat("rainAmount", rainAmount, "f 0.0 1.0", "2_motion")
        outCustomizationData.putPropertyFloat("dropDensity", dropDensity, "f 1.0 20.0", "2_motion")
        
        outCustomizationData.putPropertyFloat("dropSpeed", dropSpeed, "f 0.0 5.0", "2_motion")
        outCustomizationData.putPropertyFloat("wiggleAmount", wiggleAmount, "f 0.0 5.0", "2_motion")
        
        outCustomizationData.putPropertyFloat("zoomAmount", zoomAmount, "f 0.1 5.0", "2_motion")
        outCustomizationData.putPropertyFloat("lightningAmount", lightningAmount, "f 0.0 5.0", "2_motion")
    }

    fun setCustomImagePath(path: String) {
        var p = path
        if (p == null) p = ""
        if (this.customImagePath == p) return
        this.customImagePath = p
        if (!p.startsWith("precomp:")) {
            this.albumArtRequest = AlbumArtRequest(p, p, "", "")
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

        if (customImagePath.startsWith("precomp:")) {
            tex2 = null
            atlasTex2 = null
            bitmap = null
        } else if (customImagePath.isNotEmpty()) {
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

    fun getSelectedPreCompName(): String? {
        if (customImagePath.startsWith("precomp:")) {
            return customImagePath.substring("precomp:".length)
        }
        return null
    }

    override fun onRender(renderData: RenderState, resultFB: FrameBuffer?) {
        var targetTex: Texture? = null
        var targetAtlasTex: AtlasTexture? = null

        val preCompName = getSelectedPreCompName()
        if (preCompName != null) {
            val targetPreComp = PreCompManager.get(preCompName)
            if (targetPreComp != null) {
                targetPreComp.renderOnDemand(renderData, resultFB)
                val preCompTex = targetPreComp.texture
                if (preCompTex != null) {
                    targetTex = preCompTex
                    targetAtlasTex = AtlasTexture(targetTex)
                }
            }
        } else {
            targetTex = tex2
            targetAtlasTex = atlasTex2
        }

        onRenderCheckResources(renderData)

        if (loadedShader == null || reloadShader) {
            reloadShader = false
            loadedShader?.dispose()
            loadedShader = null
            try {
                loadedShader = VShaderProgram(SHADER_VERT, SHADER_FRAG)
                if (loadedShader?.log?.isNotEmpty() == true) {
                    tlog.w(loadedShader?.log)
                }
            } catch (e: Exception) {
                tlog.w("Failed to compile custom shader: \${e.message}")
            }
        }

        if (loadedShader == null || targetTex == null) {
            super.onRender(renderData, resultFB)
            return
        }
        
        // Physics and Time accumulation (like ParticlesElement)
        val dt = renderData.frameTimeSmooth
        accumulatedTime += dt * effectSpeed
        
        val rms = renderData.res.meter.frameDataRmsValue
        // Jelly-like smoothing: smoothly approach the target RMS
        currentBass += (rms - currentBass) * dt * 15.0f

        val shader = loadedShader!!
        renderData.bindShader(shader)

        val aspectRatioVal = uAspectRatio.getValueAsFloat(renderData.res.meter, 0f)

        shader.setUniformf("u_time", accumulatedTime)
        shader.setUniformf("u_bassPulse", currentBass * bassReaction)
        shader.setUniformf("u_rainAmount", rainAmount)
        shader.setUniformf("u_aspectRatio", aspectRatioVal)
        shader.setUniformf("u_dropSpeed", dropSpeed)
        shader.setUniformf("u_dropDensity", dropDensity)
        shader.setUniformf("u_trailLength", trailLength)
        shader.setUniformf("u_trailWidth", trailWidth)
        shader.setUniformf("u_wiggleAmount", wiggleAmount)
        shader.setUniformf("u_zoomAmount", zoomAmount)
        shader.setUniformf("u_lightningAmount", lightningAmount)

        renderData.res.bufferRenderer.setOverrideShader(shader)
        renderData.setBlendMode(blendMode)
        targetTex.bind()
        
        renderData.drawFullscreenQuad(-1, targetTex)
        
        renderData.res.bufferRenderer.flush(renderData)
        renderData.res.bufferRenderer.setOverrideShader(null)
    }

    companion object {
        const val typeName = "RainDropsEffect"
        
        const val SHADER_VERT = "" +
            "uniform mat4 u_projView;\n" +
            "attribute vec3 Position;\n" +
            "attribute vec2 TexCoord;\n" +
            "attribute vec4 Color;\n" +
            "varying vec4 vColor;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "    vColor = Color;\n" +
            "    vTexCoord = TexCoord;\n" +
            "    gl_Position = u_projView * vec4(Position, 1.0);\n" +
            "}"

        const val SHADER_FRAG = "" +
            "precision highp float;\n" +
            "precision highp int;\n" +
            "precision lowp sampler2D;\n" +
            "uniform sampler2D u_texture;\n" +
            "varying vec2 vTexCoord;\n" +
            "uniform float u_time;\n" +
            "uniform float u_bassPulse;\n" +
            "uniform float u_rainAmount;\n" +
            "uniform float u_aspectRatio;\n" +
            "uniform float u_dropSpeed;\n" +
            "uniform float u_dropDensity;\n" +
            "uniform float u_trailLength;\n" +
            "uniform float u_trailWidth;\n" +
            "uniform float u_wiggleAmount;\n" +
            "uniform float u_zoomAmount;\n" +
            "uniform float u_lightningAmount;\n" +
            "#define S(a, b, t) smoothstep(a, b, t)\n" +
            "vec3 N13(float p) {\n" +
            "   vec3 p3 = fract(vec3(p) * vec3(.1031,.11369,.13787));\n" +
            "   p3 += dot(p3, p3.yzx + 19.19);\n" +
            "   return fract(vec3((p3.x + p3.y)*p3.z, (p3.x+p3.z)*p3.y, (p3.y+p3.z)*p3.x));\n" +
            "}\n" +
            "float N(float t) {\n" +
            "    return fract(sin(t*12345.564)*7658.76);\n" +
            "}\n" +
            "float Saw(float b, float t) {\n" +
            "    return S(0., b, t)*S(1., b, t);\n" +
            "}\n" +
            "vec2 DropLayer2(vec2 uv, float t) {\n" +
            "    vec2 UV = uv;\n" +
            "    uv.y += t * u_dropSpeed;\n" +
            "    vec2 a = vec2(u_dropDensity, 1.);\n" +
            "    vec2 grid = a*2.;\n" +
            "    vec2 id = floor(uv*grid);\n" +
            "    float colShift = N(id.x);\n" +
            "    uv.y += colShift;\n" +
            "    id = floor(uv*grid);\n" +
            "    vec3 n = N13(id.x*35.2+id.y*2376.1);\n" +
            "    vec2 st = fract(uv*grid)-vec2(.5, 0.0);\n" +
            "    float x = n.x-.5;\n" +
            "    float y = UV.y*20.;\n" +
            "    float wiggle = sin(y+sin(y)) * u_wiggleAmount;\n" +
            "    x += wiggle*(.5-abs(x))*(n.z-.5);\n" +
            "    x *= .7;\n" +
            "    float ti = fract(t+n.z);\n" +
            "    y = (Saw(.85, ti)-.5)*.9+.5;\n" +
            "    vec2 p = vec2(x, y);\n" +
            "    float d = length((st-p)*a.yx);\n" +
            "    float mainDrop = S(.4, .0, d);\n" +
            "    float r = sqrt(S(1., y, st.y));\n" +
            "    float cd = abs(st.x-x);\n" +
            "    float trail = S(.23*r / max(0.001, u_trailWidth), .15*r*r / max(0.001, u_trailWidth), cd);\n" +
            "    float trailFront = S(-.02 / max(0.001, u_trailLength), .02 / max(0.001, u_trailLength), st.y-y);\n" +
            "    trail *= trailFront*r*r;\n" +
            "    y = UV.y;\n" +
            "    float trail2 = S(.2*r, .0, cd);\n" +
            "    float droplets = max(0., (sin(y*(1.-y)*120.)-st.y))*trail2*trailFront*n.z;\n" +
            "    y = fract(y*10.)+(st.y-.5);\n" +
            "    float dd = length(st-vec2(x, y));\n" +
            "    droplets = S(.3, 0., dd);\n" +
            "    float m = mainDrop+droplets*r*trailFront;\n" +
            "    return vec2(m, trail);\n" +
            "}\n" +
            "float StaticDrops(vec2 uv, float t) {\n" +
            "    uv *= 40.;\n" +
            "    vec2 id = floor(uv);\n" +
            "    uv = fract(uv)-.5;\n" +
            "    vec3 n = N13(id.x*107.45+id.y*3543.654);\n" +
            "    vec2 p = (n.xy-.5)*.7;\n" +
            "    float d = length(uv-p);\n" +
            "    float fade = Saw(.025, fract(t+n.z));\n" +
            "    float c = S(.3, 0., d)*fract(n.z*10.)*fade;\n" +
            "    return c;\n" +
            "}\n" +
            "vec2 Drops(vec2 uv, float t, float l0, float l1, float l2) {\n" +
            "    float s = StaticDrops(uv, t)*l0;\n" +
            "    vec2 m1 = DropLayer2(uv, t)*l1;\n" +
            "    vec2 m2 = DropLayer2(uv*1.85, t)*l2;\n" +
            "    float c = s+m1.x+m2.x;\n" +
            "    c = S(.3, 1., c);\n" +
            "    return vec2(c, max(m1.y*l0, m2.y*l1));\n" +
            "}\n" +
            "void main() {\n" +
            "    vec2 UV = vTexCoord;\n" +
            "    vec2 uv = UV - 0.5;\n" +
            "    uv.x *= u_aspectRatio;\n" +
            "    float T = u_time * 2.0;\n" +
            "    float t = T*.2;\n" +
            "    float activeRainAmount = u_rainAmount + u_bassPulse * 0.5;\n" +
            "    float zoom = -cos(T*.2);\n" +
            "    float scaleFactor = (0.85 + zoom * 0.15) / u_zoomAmount;\n" +
            "    uv *= scaleFactor;\n" +
            "    UV = (UV-.5) * scaleFactor + .5;\n" +
            "    float staticDrops = S(-.5, 1., activeRainAmount)*2.;\n" +
            "    float layer1 = S(.25, .75, activeRainAmount);\n" +
            "    float layer2 = S(.0, .5, activeRainAmount);\n" +
            "    vec2 c = Drops(uv, t, staticDrops, layer1, layer2);\n" +
            "    vec2 e = vec2(.001, 0.);\n" +
            "    float cx = Drops(uv+e, t, staticDrops, layer1, layer2).x;\n" +
            "    float cy = Drops(uv+e.yx, t, staticDrops, layer1, layer2).x;\n" +
            "    vec2 n = vec2(cx-c.x, cy-c.x);\n" +
            "    vec3 col = texture2D(u_texture, UV+n).rgb;\n" +
            "    t = (T+3.)*.5;\n" +
            "    float colFade = sin(t*.2)*.5+.5;\n" +
            "    col *= mix(vec3(1.), vec3(.8, .9, 1.3), colFade);\n" +
            "    float fade = 1.0;\n" +
            "    float lightning = sin(t*sin(t*10.));\n" +
            "    lightning *= pow(max(0., sin(t+sin(t))), 10.);\n" +
            "    lightning *= u_lightningAmount;\n" +
            "    float audioFlash = u_bassPulse * u_lightningAmount;\n" +
            "    col *= 1. + max(lightning, audioFlash) * fade;\n" +
            "    col *= 1.-dot(UV-0.5, UV-0.5);\n" +
            "    col *= fade;\n" +
            "    gl_FragColor = vec4(col, 1.);\n" +
            "}\n"
    }
}
