

package com.aylis.comp.visual.core.Elements.Shaders

import com.aylis.comp.visual.core.Elements.DummyElement

class VignetteElement : DummyElement() {

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
            uniform float u_radius;
            uniform float u_softness;
            uniform float u_colorR;
            uniform float u_colorG;
            uniform float u_colorB;

            varying vec2 vTexCoord;

            void main() {
                vec4 texColor = texture2D(u_texture, vTexCoord);

                vec2 center = vec2(0.5, 0.5);
                float dist = distance(vTexCoord, center);

                float vignette = smoothstep(u_radius, u_radius - u_softness, dist);

                vec3 vignetteColor = vec3(u_colorR, u_colorG, u_colorB);
                vec3 finalRGB = mix(vignetteColor, texColor.rgb, vignette);

                gl_FragColor = vec4(finalRGB, texColor.a);
            }
        """.trimIndent()
    }

    override fun initCustomValues() {
        addValueWithProperties("radius", 0.8f, 0.0f, 1.5f)
        addValueWithProperties("softness", 0.5f, 0.0f, 1.0f)
        addValueWithProperties("colorR", 0.0f, 0.0f, 1.0f)
        addValueWithProperties("colorG", 0.0f, 0.0f, 1.0f)
        addValueWithProperties("colorB", 0.0f, 0.0f, 1.0f)
    }

    override fun isShaderEditable(): Boolean {
        return false
    }

    companion object {
        const val typeName = "Vignette"
    }
}
