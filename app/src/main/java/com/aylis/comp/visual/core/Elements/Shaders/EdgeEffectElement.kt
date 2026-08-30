

package com.aylis.comp.visual.core.Elements.Shaders

import com.aylis.comp.visual.core.Elements.DummyElement

class EdgeEffectElement : DummyElement() {

    override fun getElementTypeName(): String {
        return typeName
    }

    override fun initCustomShader() {
        this.shaderVert = DEFAULT_VERT_SHADER
        this.shaderFrag = """
            // Cubiqued - https://www.shadertoy.com/view/td2yDm
            precision mediump float;
            precision mediump int;
            precision lowp sampler2D;
            precision lowp samplerCube;

            uniform float u_amount;
            uniform sampler2D u_texture;

            varying vec2 vTexCoord;

            void main()
            {
                float h = u_amount / 200.0;

                vec4 o = texture2D(u_texture, vTexCoord);
                vec4 n = texture2D(u_texture, vTexCoord + vec2(0.0, h));
                vec4 e = texture2D(u_texture, vTexCoord + vec2(h, 0.0));
                vec4 s = texture2D(u_texture, vTexCoord - vec2(0.0, h));
                vec4 w = texture2D(u_texture, vTexCoord - vec2(h, 0.0));

                vec4 dy = (n - s) * 0.5;
                vec4 dx = (e - w) * 0.5;

                vec4 edge = sqrt(dx * dx + dy * dy);
                vec4 angle = atan(dy, dx);

                gl_FragColor = vec4(edge * 5.0);
            }
        """.trimIndent()
    }

    override fun initCustomValues() {
        addValueWithProperties("amount", 0.5f, 0.0f, 1.0f)
    }

    override fun isShaderEditable(): Boolean {
        return false
    }

    companion object {
        const val typeName = "EdgeEffect"
    }
}
