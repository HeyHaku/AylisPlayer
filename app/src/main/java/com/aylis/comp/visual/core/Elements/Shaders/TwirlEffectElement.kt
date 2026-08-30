

package com.aylis.comp.visual.core.Elements.Shaders

import com.aylis.comp.visual.core.Elements.DummyElement

class TwirlEffectElement : DummyElement() {

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
            uniform float u_centerX;
            uniform float u_centerY;
            uniform float u_radius;
            uniform float u_aspectRatio;

            varying vec2 vTexCoord;

            void main() {
                float effectRadius = u_radius;
                float effectAngle = u_amount;

                vec2 u_center = vec2(u_centerX, u_centerY);
                vec2 center = (u_center + 1.0)/2.0;

                vec2 uv = vTexCoord.xy - center;
                float len = length(uv * vec2(u_aspectRatio, 1.0));
                float angle = atan(uv.y, uv.x) + effectAngle * smoothstep(effectRadius, 0.0, len);
                float radius = length(uv);

                vec2 newUV = vec2(radius * cos(angle), radius * sin(angle)) + center;

                gl_FragColor = texture2D(u_texture, newUV);
            }
        """.trimIndent()
    }

    override fun initCustomValues() {
        addValueWithProperties("amount", 2.0f, -10.0f, 10.0f)
        addValueWithProperties("radius", 0.5f, 0.0f, 1.0f)
        addValueWithProperties("centerX", 0.0f, -1.0f, 1.0f)
        addValueWithProperties("centerY", 0.0f, -1.0f, 1.0f)
        addValueWithProperties("aspectRatio", 0.5f, 0.0f, 2.0f)
    }

    override fun isShaderEditable(): Boolean {
        return false
    }

    companion object {
        const val typeName = "TwirlEffect"
    }
}
