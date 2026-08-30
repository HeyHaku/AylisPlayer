package com.aylis.comp.visual.core.Elements

import android.graphics.RectF
import com.aylis.Common.Vec2f
import com.aylis.comp.visual.core.Graphic.RenderState
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.FrameBuffer
import com.aylis.comp.visual.core.Graphic.AtlasTexture
import com.aylis.comp.visual.core.Elements.Base.MVariableFloat
import com.aylis.comp.visual.core.Elements.Base.MeasureDefs

class RedLandscapeElement : Element() {

    private var time = MVariableFloat.createConstantFloat(1.0f)
    private var bass = MVariableFloat.createConstantFloat(1.0f)
    private var aspectRatio = 1.0f
    
    private var hue = MVariableFloat.createConstantFloat(0.0f)
    private var saturation = MVariableFloat.createConstantFloat(0.7f)
    private var brightness = MVariableFloat.createConstantFloat(1.0f)
    
    fun getElementTypeName(): String {
        return typeName
    }

    override fun onApplyCustomization(customizationData: CustomizationData) {
        super.onApplyCustomization(customizationData)
        time = customizationData.getPropertyMVariableFloat("time", time)
        bass = customizationData.getPropertyMVariableFloat("bass", bass)
        aspectRatio = customizationData.getPropertyFloat("aspectRatio", aspectRatio)
        
        hue = customizationData.getPropertyMVariableFloat("hue", hue)
        saturation = customizationData.getPropertyMVariableFloat("saturation", saturation)
        brightness = customizationData.getPropertyMVariableFloat("brightness", brightness)
    }

    override fun onReadCustomization(outCustomizationData: CustomizationData) {
        super.onReadCustomization(outCustomizationData)
        outCustomizationData.setCustomizationName("Red Landscape")
        outCustomizationData.putPropertyMVariableFloat("time", time, "1_appearance", 0f, 5f)
        outCustomizationData.putPropertyMVariableFloat("bass", bass, "1_appearance", 0f, 5f)
        outCustomizationData.putPropertyFloat("aspectRatio", aspectRatio, "f 0.1 5.0", "1_appearance")
        
        outCustomizationData.putPropertyMVariableFloat("hue", hue, "1_appearance", 0f, 1f)
        outCustomizationData.putPropertyMVariableFloat("saturation", saturation, "1_appearance", 0f, 2f)
        outCustomizationData.putPropertyMVariableFloat("brightness", brightness, "1_appearance", 0f, 3f)
    }

    override fun onRender(renderData: RenderState, resultFB: FrameBuffer?) {
        super.onRender(renderData, resultFB)

        val drawRect = measureDrawRect(renderData.res.meter)
        val w = drawRect.width()
        val h = drawRect.height()
        
        val currentBass = bass.getValueAsFloat(renderData.res.meter, 1.0f)
        val currentTime = time.getValueAsFloat(renderData.res.meter, 1.0f)
        
        val currentHue = hue.getValueAsFloat(renderData.res.meter, 0.0f)
        val currentSaturation = saturation.getValueAsFloat(renderData.res.meter, 0.7f)
        val currentBrightness = brightness.getValueAsFloat(renderData.res.meter, 1.0f)

        val shader = renderData.res.redLandscapeShader
        if (shader != null) {
            renderData.res.bufferRenderer.flush(renderData)
            renderData.bindShader(shader)
            
            shader.setUniformMatrix("u_projView", false, renderData.vpMatrix)
            
            shader.setUniformf("iResolution", w * aspectRatio, h)
            shader.setUniformf("iTime", currentTime)
            shader.setUniformf("iMouse", 0f, 0f, 0f, 0f)
            shader.setUniformf("u_bass", currentBass)
            shader.setUniformf("u_hue", currentHue)
            shader.setUniformf("u_saturation", currentSaturation)
            shader.setUniformf("u_brightness", currentBrightness)

            renderData.res.bufferRenderer.setOverrideShader(shader)
        }

        val cx = drawRect.centerX()
        val cy = drawRect.centerY()
        val r = RectF(cx - w * 0.5f, cy - h * 0.5f, cx + w * 0.5f, cy + h * 0.5f)

        val atlasTex = renderData.res.atlasTexWhite
        
        renderData.res.bufferRenderer.drawRectangleRightBottomWH(
            renderData, 
            r.left, r.top, 0f, r.width(), r.height(), 
            0xFFFFFFFF.toInt(), 
            Vec2f.zero, Vec2f.one, 
            renderData.res.atlasTexWhite
        )
        
        if (shader != null) {
            renderData.res.bufferRenderer.flush(renderData)
            renderData.res.bufferRenderer.setOverrideShader(null)
        }
    }

    companion object {
        const val typeName = "RedLandscape"
    }
}
