

package com.aylis.comp.visual.core.Elements.Shaders

import com.aylis.comp.visual.core.Elements.DummyElement

class Kaleidoscope : DummyElement() {

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

            varying vec2 vTexCoord;
            uniform sampler2D u_texture;
            uniform float u_segments;
            uniform float u_offset;

            const float PI = 3.14159265359;
            const float TAU = 2.0 * PI;

            void main() {
               if (u_segments == 0.0) {
                  gl_FragColor = texture2D(u_texture, vTexCoord);
               } else {
                  vec2 centered = vTexCoord - 0.5;
                  // to pol
                  float r = length(centered);
                  float theta = atan(centered.y, centered.x);
                  theta = mod(theta, TAU / u_segments);
                  theta = abs(theta - PI / u_segments);
                  // back to cartesi
                  vec2 newCoords = r * vec2(cos(theta), sin(theta)) + 0.5;
                  gl_FragColor = texture2D(u_texture, mod(newCoords - u_offset, 1.0));
               }
            }
        """.trimIndent()
    }

    override fun initCustomValues() {
        addValueWithProperties("segments", 3.0f, 1.0f, 10.0f)
        addValueWithProperties("offset", 0.0f, -1.0f, 1.0f)
    }

    override fun isShaderEditable(): Boolean {
        return false
    }

    companion object {
        const val typeName = "Kaleidoscope"
    }
}
