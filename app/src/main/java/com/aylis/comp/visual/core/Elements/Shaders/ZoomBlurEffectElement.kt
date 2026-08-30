

package com.aylis.comp.visual.core.Elements.Shaders

import com.aylis.comp.visual.core.Elements.DummyElement

class ZoomBlurEffectElement : DummyElement() {

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

            uniform float u_strength;
            uniform float u_innerRadius;
            uniform float u_radius;
            uniform float u_passes;
            uniform float u_gradientLength;
            uniform float u_centerX;
            uniform float u_centerY;
            uniform float u_aspectRatio;

            #define resolution vec2(u_aspectRatio,1.0)

            varying vec2 vTexCoord;
            uniform sampler2D u_texture;

            float rand(vec2 co, float seed) {
              const float a = 12.9898, b = 78.233, c = 43758.5453;
              float dt = dot(co + seed, vec2(a, b));
              float sn = mod(dt, 3.14159);
              return fract(sin(sn) * c + seed);
            }

            void main()
            {
              vec2 iCenter = (vec2(u_centerX, u_centerY)+1.0)/2.0;
              float iStrength = u_strength;
              float iInnerRadius = u_innerRadius;
              float iRadius = u_radius/0.9;
              float iMaxKernelSize = u_passes*100.0;

              iCenter *= resolution.xy;
              iInnerRadius *= min(resolution.x, resolution.y);
              iRadius *= min(resolution.x, resolution.y);

              float minGradient = iInnerRadius * 0.3 * u_gradientLength*5.0;
              float u_innerRadius_local = (iInnerRadius + minGradient * 0.5) / resolution.x;

              float gradient = iRadius * 0.3;
              float u_radius_local = (iRadius - gradient * 0.5) / resolution.x;

              float countLimit = iMaxKernelSize;

              vec2 dir = vec2(iCenter.xy / resolution.xy - vTexCoord.xy);
              float dist = length(vec2(dir.x, dir.y * resolution.y / resolution.x));

              float u_strength_local = iStrength;

              float delta = 0.0;
              float gap = 0.0;
              if (dist < u_innerRadius_local) {
                  delta = u_innerRadius_local - dist;
                  gap = minGradient;
              } else if (u_radius_local >= 0.0 && dist > u_radius_local) {
                  delta = dist - u_radius_local;
                  gap = gradient;
              }

              if (delta > 0.0) {
                  float normalCount = gap / resolution.x;
                  delta = (normalCount - delta) / normalCount;
                  countLimit *= delta;
                  u_strength_local *= delta;
                  if (countLimit < 1.0)
                  {
                      gl_FragColor = texture2D(u_texture, vTexCoord.xy);
                      return;
                  }
              }

              float offset = rand(vTexCoord.xy, 0.0);

              float total = 0.0;
              vec4 color = vec4(0.0);

              dir *= u_strength_local;

              for (int t = 0; t < 100; t++) {
                  if (float(t) >= iMaxKernelSize) break;
                  float percent = (float(t) + offset) / iMaxKernelSize;
                  float weight = 4.0 * (percent - percent * percent);
                  vec2 p = vTexCoord.xy + dir * percent;
                  vec4 mysample = texture2D(u_texture, p);

                  color += mysample * weight;
                  total += weight;

                  if (float(t) > countLimit){
                      break;
                  }
              }

              color /= total;
              gl_FragColor = color;
            }
        """.trimIndent()
    }

    override fun initCustomValues() {
        addValueWithProperties("strength", 0.5f, 0.0f, 1.0f)
        addValueWithProperties("innerRadius", 0.3f, 0.0f, 1.0f)
        addValueWithProperties("radius", 1.0f, 0.0f, 1.0f)
        addValueWithProperties("passes", 0.5f, 0.0f, 1.0f)
        addValueWithProperties("gradientLength", 0.5f, 0.0f, 1.0f)
        addValueWithProperties("centerX", 0.0f, -1.0f, 1.0f)
        addValueWithProperties("centerY", 0.0f, -1.0f, 1.0f)
        addValueWithProperties("aspectRatio", 0.5f, 0.0f, 2.0f)
    }

    override fun isShaderEditable(): Boolean {
        return false
    }

    companion object {
        const val typeName = "ZoomBlurEffect"
    }
}
