

package com.aylis.comp.visual.core.Elements.Shaders

import com.aylis.comp.visual.core.Elements.DummyElement

class GodraysEffectElement : DummyElement() {

    override fun getElementTypeName(): String {
        return typeName
    }

    override fun initCustomShader() {
        this.shaderVert = DEFAULT_VERT_SHADER
        this.shaderFrag = """
            // Godrays Effect
            // Thanks to the Vizzy Team: vizzy.io
            precision mediump float;
            precision mediump int;
            precision lowp sampler2D;
            precision lowp samplerCube;

            uniform sampler2D u_texture;

            uniform float u_decay;
            uniform float u_density;
            uniform float u_weight;
            uniform float u_rayCenterX;
            uniform float u_rayCenterY;
            uniform float u_rayOpacity;
            uniform float u_saturation;

            uniform float u_ignoreDarkColors;
            uniform float u_renderInputOnTop;

            uniform float u_sampleCount;

            #define ignoreDark floatToBool(u_ignoreDarkColors)
            #define renderInputOnTop floatToBool(u_renderInputOnTop)
            #define nsamples floatToInt(u_sampleCount)

            varying vec2 vTexCoord;

            int floatToInt(float f) { return int(f); }

            bool floatToBool(float f) { return f > 0.5; }

            // Algorithm found in https://medium.com/community-play-3d/god-rays-whats-that-5a67f26aeac2 // https://www.shadertoy.com/view/ls2Xzd

            // https://github.com/jamieowen/glsl-blend
            float blendScreen(float base, float blend) {
                return 1.0-((1.0-base)*(1.0-blend));
            }

            vec4 blendScreen(vec4 base, vec4 blend) {
                return vec4(blendScreen(base.r,blend.r),blendScreen(base.g,blend.g),blendScreen(base.b,blend.b),blendScreen(base.a,blend.a));
            }

            vec4 blendScreen(vec4 base, vec4 blend, float opacity) {
                return (blendScreen(base, blend) * opacity + base * (1.0 - opacity));
            }

            vec3 blendScreen(vec3 base, vec3 blend) {
                return vec3(blendScreen(base.r,blend.r),blendScreen(base.g,blend.g),blendScreen(base.b,blend.b));
            }

            vec3 blendScreen(vec3 base, vec3 blend, float opacity) {
                return (blendScreen(base, blend) * opacity + base * (1.0 - opacity));
            }

            vec4 saturation(vec4 col, float adjustment)
            {
                const vec3 W = vec3(0.2125, 0.7154, 0.0721);
                vec3 color = col.rgb;
                vec3 intensityRGB = vec3(dot(color, W));
                vec4 intensityRGBA = vec4(intensityRGB, col.a);
                return mix(intensityRGBA, col, adjustment);
            }

            vec4 crepuscular_rays(vec2 texCoords, vec2 pos) {
                vec2 tc = vTexCoord;

                vec2 deltaTexCoord = tc - pos.xy;

                deltaTexCoord *= (1.0 / float(nsamples) * u_density);

                float illuminationDecay = 1.0;

                vec4 color = texture2D(u_texture, tc.xy);

                tc += deltaTexCoord * fract( sin(dot(texCoords.xy+fract(0.0), vec2(12.9898, 78.233)))* 43758.5453 );

                for (int i = 0; i < 100; i++)
                {
                    if (i >= nsamples) break;
                    tc -= deltaTexCoord;
                    vec4 sampl = texture2D(u_texture, tc.xy);

                    sampl *= illuminationDecay * u_weight;

                    if(!ignoreDark)
                      color = blendScreen(color, sampl, u_rayOpacity);
                    else {
                      color = vec4(blendScreen(color.rgb, sampl.rgb, u_rayOpacity), color.a);
                    }
                    illuminationDecay *= u_decay;
                }

                color = saturation(color, u_saturation);

                if(renderInputOnTop) {
                  color.rgb *= vec3(1.0 - texture2D(u_texture, vTexCoord).a);
                  color.rgb += texture2D(u_texture, vTexCoord).rgb;
                }

                return color;
            }

            void main(){
              vec2 uv = vTexCoord;
              vec2 pos = vec2((u_rayCenterX * -1.0 + 0.5), (u_rayCenterY * -1.0 + 0.5));
              gl_FragColor = crepuscular_rays(uv, pos.xy);
            }
        """.trimIndent()
    }

    override fun initCustomValues() {
        addValueWithProperties("decay", 0.9f, 0.0f, 1.0f)
        addValueWithProperties("density", 1.0f, 0.0f, 1.0f)
        addValueWithProperties("weight", 0.2f, 0.0f, 1.0f)
        addValueWithProperties("rayOpacity", 1.0f, 0.0f, 2.0f)
        addValueWithProperties("saturation", 1.0f, 0.0f, 2.0f)
        addValueWithProperties("ignoreDarkColors", 0.0f, 0.0f, 1.0f)
        addValueWithProperties("renderInputOnTop", 0.0f, 0.0f, 1.0f)
        addValueWithProperties("rayCenterX", 0.0f, -1.0f, 1.0f)
        addValueWithProperties("rayCenterY", 0.0f, -1.0f, 1.0f)
        addValueWithProperties("sampleCount", 50.0f, 0.0f, 100.0f)
    }

    override fun isShaderEditable(): Boolean {
        return false
    }

    companion object {
        const val typeName = "GodraysEffect"
    }
}
