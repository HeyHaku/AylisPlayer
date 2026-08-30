

package com.aylis.comp.visual.core.Elements.Shaders

import com.aylis.comp.visual.core.Elements.DummyElement

class BulgePinchEffectElement : DummyElement() {

    override fun getElementTypeName(): String {
        return typeName
    }

    override fun initCustomShader() {
        this.shaderVert = DEFAULT_VERT_SHADER
        this.shaderFrag = """
            // BulgePinch - Cubified xd
            // original from pixiJS : https://github.com/pixijs/filters/blob/main/filters/bulge-pinch/src/bulgePinch.frag

            precision mediump float;
            precision mediump int;
            precision lowp sampler2D;
            precision lowp samplerCube;

            uniform float u_radius;
            uniform float u_strength;
            uniform float u_centerX;
            uniform float u_centerY;
            uniform float u_aspectRatio;

            uniform sampler2D u_texture;
            varying vec2 vTexCoord;

            void main()
            {
                vec2 center = (vec2(u_centerX,u_centerY) + 1.0)/2.0;
                float strength = u_strength * 2.0;
                vec2 uv = vTexCoord.xy - center;
                float distance = length(uv* vec2(u_aspectRatio, 1.0));
                if (distance < u_radius) {
                    float percent = distance / u_radius;
                    if (strength > 0.0) {
                        uv *= mix(1.0, smoothstep(0.0, u_radius / distance, percent), strength * 0.75);
                    } else {
                        uv *= mix(1.0, pow(percent, 1.0 + strength * 0.75) * u_radius / distance, 1.0 - percent);
                    }
                }
                uv += center;
                vec2 clampedCoord = clamp(uv, 0.0, 1.0);
                vec4 color = texture2D(u_texture, clampedCoord);
                if (uv != clampedCoord) {
                    color *= max(0.0, 1.0 - length(uv - clampedCoord));
                }

                gl_FragColor = color;
            }
        """.trimIndent()
    }

    override fun initCustomValues() {
        addValueWithProperties("radius", 0.2f, 0.0f, 1.0f)
        addValueWithProperties("strength", 0.5f, -1.0f, 1.0f)
        addValueWithProperties("centerX", 0.0f, -1.0f, 1.0f)
        addValueWithProperties("centerY", 0.0f, -1.0f, 1.0f)
        addValueWithProperties("aspectRatio", 2.0f, 0.0f, 4.0f)
    }

    override fun isShaderEditable(): Boolean {
        return false
    }

    companion object {
        const val typeName = "BulgePinchEffect"
    }
}
