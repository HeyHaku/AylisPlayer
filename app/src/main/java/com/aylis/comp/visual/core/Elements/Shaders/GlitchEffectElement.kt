

package com.aylis.comp.visual.core.Elements.Shaders

import com.aylis.comp.visual.core.Elements.DummyElement

class GlitchEffectElement : DummyElement() {

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
            uniform float u_glitchIntensity;
            uniform float u_colorSplit;
            uniform float u_scanlines;
            uniform float u_noiseSpeed;

            varying vec2 vTexCoord;

            float hash(vec2 p) {
                return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
            }

            void main() {
                float timeVal = u_noiseSpeed;
                vec2 uv = vTexCoord;

                float glitchIntensity = u_glitchIntensity * 0.15;
                float blockNoise = hash(vec2(floor(uv.y * 10.0), floor(timeVal * 5.0)));
                float lineNoise = hash(vec2(floor(uv.y * 100.0), floor(timeVal * 12.0)));

                float shift = 0.0;
                if (blockNoise < glitchIntensity) {
                    shift += (hash(vec2(floor(uv.y * 8.0), timeVal)) - 0.5) * 0.05;
                }
                if (lineNoise < glitchIntensity * 0.4) {
                    shift += (hash(vec2(floor(uv.y * 80.0), timeVal)) - 0.5) * 0.02;
                }

                float splitAmount = u_colorSplit * 0.025;
                vec2 uvR = vec2(uv.x + shift + splitAmount, uv.y);
                vec2 uvG = vec2(uv.x + shift, uv.y);
                vec2 uvB = vec2(uv.x + shift - splitAmount, uv.y);

                float r = texture2D(u_texture, uvR).r;
                float g = texture2D(u_texture, uvG).g;
                float b = texture2D(u_texture, uvB).b;
                float a = texture2D(u_texture, uvG).a;

                vec4 color = vec4(r, g, b, a);

                if (u_scanlines > 0.01) {
                    float scanline = sin(uv.y * 800.0) * 0.5 + 0.5;
                    color.rgb = mix(color.rgb, color.rgb * scanline, u_scanlines * 0.4);
                }

                gl_FragColor = color;
            }
        """.trimIndent()
    }

    override fun initCustomValues() {
        addValueWithProperties("glitchIntensity", 0.3f, 0.0f, 1.0f)
        addValueWithProperties("colorSplit", 0.4f, 0.0f, 1.0f)
        addValueWithProperties("scanlines", 0.5f, 0.0f, 1.0f)
        addValueWithProperties("noiseSpeed", 1.0f, 0.0f, 10.0f)
    }

    override fun isShaderEditable(): Boolean {
        return false
    }

    companion object {
        const val typeName = "GlitchEffect"
    }
}
