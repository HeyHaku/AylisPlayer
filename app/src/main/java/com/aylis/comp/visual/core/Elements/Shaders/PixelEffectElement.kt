

package com.aylis.comp.visual.core.Elements.Shaders

import com.aylis.comp.visual.core.Elements.DummyElement

class PixelEffectElement : DummyElement() {

    override fun getElementTypeName(): String {
        return typeName
    }

    override fun initCustomShader() {
        this.shaderVert = DEFAULT_VERT_SHADER
        this.shaderFrag = """
            precision mediump float;
            precision mediump int;
            precision lowp sampler2D;
            precision lowp samplerCube;

            uniform sampler2D u_texture;
            uniform float u_amount;
            uniform float u_aspectRatio;
            varying vec2 vTexCoord;

            void main() {
                vec2 resolution = vec2(u_aspectRatio * 2.0, 1.0);
                vec2 coordinates = vTexCoord;
                float amount = u_amount / 10.0;
                vec2 pixelSize = vec2(amount / resolution.x, amount / resolution.y);
                vec2 position = (pixelSize.x == 0.0) ? vTexCoord : floor(coordinates / pixelSize) * pixelSize;

                vec4 finalColor = texture2D(u_texture, position);
                gl_FragColor = finalColor;
            }
        """.trimIndent()
    }

    override fun initCustomValues() {
        addValueWithProperties("amount", 0.5f, 0.0f, 1.0f)
        addValueWithProperties("aspectRatio", 0.5f, 0.0f, 2.0f)
    }

    override fun isShaderEditable(): Boolean {
        return false
    }

    companion object {
        const val typeName = "PixelEffect"
    }
}
