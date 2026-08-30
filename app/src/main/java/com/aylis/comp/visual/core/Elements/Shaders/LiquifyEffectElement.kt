

package com.aylis.comp.visual.core.Elements.Shaders

import com.aylis.comp.visual.core.Elements.DummyElement

class LiquifyEffectElement : DummyElement() {

    override fun getElementTypeName(): String {
        return typeName
    }

    override fun initCustomShader() {
        this.shaderVert = DEFAULT_VERT_SHADER
        this.shaderFrag = """
            precision highp float;
            precision mediump int;
            precision lowp sampler2D;
            precision lowp samplerCube;

            uniform sampler2D u_texture;

            uniform float u_intensity;
            uniform float u_control;
            uniform float u_zoom;
            uniform float u_complexity;
            uniform float u_noiseScale;
            uniform float u_blobs;

            varying vec2 vTexCoord;

            vec3 mod289(vec3 x) {
                return x - floor(x * (1.0 / 289.0)) * 289.0;
            }

            vec4 mod289(vec4 x) {
                return x - floor(x * (1.0 / 289.0)) * 289.0;
            }

            vec4 permute(vec4 x) {
                return mod289(((x * 34.0) + 1.0) * x);
            }

            vec4 taylorInvSqrt(vec4 r) {
                return 1.79284291400159 - 0.85373472095314 * r;
            }

            float snoise(vec3 v) {
                const vec2 C = vec2(1.0 / 6.0, 1.0 / 3.0);
                const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);
                vec3 i = floor(v + dot(v, C.yyy));
                vec3 x0 = v - i + dot(i, C.xxx);
                vec3 g = step(x0.yzx, x0.xyz);
                vec3 l = 1.0 - g;
                vec3 i1 = min(g.xyz, l.zxy);
                vec3 i2 = max(g.xyz, l.zxy);
                vec3 x1 = x0 - i1 + C.xxx;
                vec3 x2 = x0 - i2 + C.yyy;
                vec3 x3 = x0 - D.yyy;
                i = mod289(i);
                vec4 p = permute(permute(permute(i.z + vec4(0.0, i1.z, i2.z, 1.0))
                       + i.y + vec4(0.0, i1.y, i2.y, 1.0))
                       + i.x + vec4(0.0, i1.x, i2.x, 1.0));
                float n_ = 0.142857142857;
                vec3 ns = n_ * D.wyz - D.xzx;
                vec4 j = p - (49.0 + u_blobs*50.0) * floor(p * ns.z * ns.z);
                vec4 x_ = floor(j * ns.z);
                vec4 y_ = floor(j - 7.0 * x_);
                vec4 x = x_ * ns.x + ns.yyyy;
                vec4 y = y_ * ns.x + ns.yyyy;
                vec4 h = 1.0 - abs(x) - abs(y);
                vec4 b0 = vec4(x.xy, y.xy);
                vec4 b1 = vec4(x.zw, y.zw);
                vec4 s0 = floor(b0) * 2.0 + 1.0;
                vec4 s1 = floor(b1) * 2.0 + 1.0;
                vec4 sh = -step(h, vec4(0.0));
                vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;
                vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;
                vec3 p0 = vec3(a0.xy, h.x);
                vec3 p1 = vec3(a0.zw, h.y);
                vec3 p2 = vec3(a1.xy, h.z);
                vec3 p3 = vec3(a1.zw, h.w);
                vec4 norm = taylorInvSqrt(vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3)));
                p0 *= norm.x;
                p1 *= norm.y;
                p2 *= norm.z;
                p3 *= norm.w;
                vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);
                m = m * m;
                return 42.0 * dot(m * m, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));
            }

            #define scalDivt 2.1

            float FBM(vec3 v) {
                float sc1 = 1.0 / u_noiseScale * 0.1;
                float sc2 = sc1 / u_noiseScale * 0.1;
                float sc3 = sc2 / u_noiseScale * 0.1;
                float sc1t = 1.0 / scalDivt;
                float sc2t = sc1t / scalDivt;
                float sc3t = sc2t / scalDivt;
                return 1.0 * 0.5 * snoise(v * vec3(sc3, sc3, sc3t)) +
                       0.4 * 0.25 * snoise(v * vec3(sc2, sc2, sc2t)) +
                       0.15 * 0.125 * snoise(v * vec3(sc1, sc1, sc1t));
            }

            void main() {
                float _control = u_control * 1.;
                float _scale = u_zoom * 50.;
                float _complexity = u_complexity * 1000.;
                vec2 uv = vTexCoord * (1.0 - 0.02 * _scale) + (vec2(_scale, _scale) * 0.01);
                float niceNoise1 = FBM(vec3(_complexity * uv, 0.04 * 60.0 * (_control)));
                float niceNoise2 = FBM(vec3(_complexity * uv, 0.04 * 62.0 * (_control) + 300.0));
                gl_FragColor = texture2D(u_texture, uv + vec2(u_intensity * 0.1 * niceNoise1, u_intensity * 0.105 * niceNoise2));
            }
        """.trimIndent()
    }

    override fun initCustomValues() {
        addValueWithProperties("intensity", 0.3f, 0.0f, 2.0f)
        addValueWithProperties("control", 0.0f, -10.0f, 10.0f)
        addValueWithProperties("complexity", 0.3f, 0.0f, 1.0f)
        addValueWithProperties("noiseScale", 0.3f, 0.0f, 1.0f)
        addValueWithProperties("blobs", 0.0f, 0.0f, 0.5f)
        addValueWithProperties("zoom", 0.0f, -1.0f, 1.0f)
    }

    override fun isShaderEditable(): Boolean {
        return false
    }

    companion object {
        const val typeName = "LiquifyEffect"
    }
}
