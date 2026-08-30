

package com.aylis.comp.visual.core.Elements.Shaders

import com.aylis.comp.visual.core.Elements.Element.CustomizationData
import com.aylis.comp.visual.core.Elements.DummyElement

class MirrorEffectElement : DummyElement() {

    private var mirrorModeSelect = "Horizontal"
    private var flipMirrorBool = false

    override fun getElementTypeName(): String {
        return typeName
    }

    override fun initCustomShader() {
        this.shaderVert = DEFAULT_VERT_SHADER
        this.shaderFrag = """
            precision mediump float;
            precision mediump int;
            precision lowp sampler2D;

            uniform sampler2D u_texture;
            uniform float u_mirrorMode;
            uniform float u_flipMirror;

            varying vec2 vTexCoord;

            void main() {
                vec2 uv = vTexCoord;
                if (u_mirrorMode < 0.5) { // Horizontal
                    if (u_flipMirror > 0.5) {
                        uv.x = (uv.x > 0.5) ? uv.x : (1.0 - uv.x);
                    } else {
                        uv.x = (uv.x < 0.5) ? uv.x : (1.0 - uv.x);
                    }
                } else if (u_mirrorMode < 1.5) { // Vertical
                    if (u_flipMirror > 0.5) {
                        uv.y = (uv.y > 0.5) ? uv.y : (1.0 - uv.y);
                    } else {
                        uv.y = (uv.y < 0.5) ? uv.y : (1.0 - uv.y);
                    }
                } else { // Horizontal and Vertical
                    if (u_flipMirror > 0.5) {
                        uv.x = (uv.x > 0.5) ? uv.x : (1.0 - uv.x);
                        uv.y = (uv.y > 0.5) ? uv.y : (1.0 - uv.y);
                    } else {
                        uv.x = (uv.x < 0.5) ? uv.x : (1.0 - uv.x);
                        uv.y = (uv.y < 0.5) ? uv.y : (1.0 - uv.y);
                    }
                }
                gl_FragColor = texture2D(u_texture, uv);
            }
        """.trimIndent()
    }

    override fun initCustomValues() {
        addValueWithProperties("mirrorMode", 0.0f, 0.0f, 2.0f)
        addValueWithProperties("flipMirror", 0.0f, 0.0f, 1.0f)
    }

    override fun onApplyCustomization(customizationData: CustomizationData) {
        val tempMirrorMode = u_values.remove("mirrorMode")
        val tempFlipMirror = u_values.remove("flipMirror")

        super.onApplyCustomization(customizationData)

        if (tempMirrorMode != null) u_values["mirrorMode"] = tempMirrorMode
        if (tempFlipMirror != null) u_values["flipMirror"] = tempFlipMirror

        mirrorModeSelect = customizationData.getPropertyString("mirrorMode", "Horizontal")
        flipMirrorBool = customizationData.getPropertyBool("flipMirror", false)

        val modeIndex = when (mirrorModeSelect) {
            "Horizontal" -> 0.0f
            "Vertical" -> 1.0f
            "HorizontalAndVertical" -> 2.0f
            else -> 0.0f
        }
        u_values["mirrorMode"] = com.aylis.comp.visual.core.Elements.Base.MVariableFloat.createConstantFloat(modeIndex)
        u_values["flipMirror"] = com.aylis.comp.visual.core.Elements.Base.MVariableFloat.createConstantFloat(if (flipMirrorBool) 1.0f else 0.0f)
    }

    override fun onReadCustomization(outCustomizationData: CustomizationData) {
        val tempMirrorMode = u_values.remove("mirrorMode")
        val tempFlipMirror = u_values.remove("flipMirror")

        super.onReadCustomization(outCustomizationData)

        if (tempMirrorMode != null) u_values["mirrorMode"] = tempMirrorMode
        if (tempFlipMirror != null) u_values["flipMirror"] = tempFlipMirror

        outCustomizationData.setCustomizationName("Mirror Effect")

        outCustomizationData.putPropertyString("mirrorMode", mirrorModeSelect, "sel Horizontal Vertical HorizontalAndVertical", "1_appearance")
        outCustomizationData.putPropertyBool("flipMirror", flipMirrorBool, "1_appearance")
    }

    override fun isShaderEditable(): Boolean {
        return false
    }

    companion object {
        const val typeName = "MirrorEffect"
    }
}
