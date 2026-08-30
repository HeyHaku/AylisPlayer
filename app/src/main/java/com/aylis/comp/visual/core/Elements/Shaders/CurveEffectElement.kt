

package com.aylis.comp.visual.core.Elements.Shaders

import com.aylis.comp.visual.core.Elements.DummyElement

class CurveEffectElement : DummyElement() {

    override fun getElementTypeName(): String {
        return typeName
    }

    override fun initCustomShader() {
        this.shaderVert = DEFAULT_VERT_SHADER
        this.shaderFrag = """
            // Cubiqued, https://www.shadertoy.com/view/4ttXRH
            precision highp float;
            precision mediump int;
            precision lowp sampler2D;
            precision lowp samplerCube;

            uniform sampler2D u_texture;

            varying vec2 vTexCoord;

            #define PI acos(-1.)

            uniform float u_innerRadius;
            uniform float u_outerRadius;
            uniform float u_aspectRatio;

            void main()
            {
                vec2 uv = vTexCoord * 2.0 - 1.0;
                uv.x *= u_aspectRatio;
                uv.y *= -1.;

                float theta = atan( uv.y, uv.x );
                float dist = length( uv );

                //Make center circle and edge black
                float a = smoothstep( u_innerRadius - 0.001, u_innerRadius + 0.001, dist );
                a *= 1.0 - smoothstep( u_outerRadius - 0.001, u_outerRadius + 0.001, dist );

                uv = vec2((theta + PI) / (PI * 2.0),
                          (dist - u_innerRadius) / (u_outerRadius - u_innerRadius)
                         );

                gl_FragColor = texture2D(u_texture , uv) * a;
            }
        """.trimIndent()
    }

    override fun initCustomValues() {
        addValueWithProperties("innerRadius", 0.0f, -1.0f, 1.0f)
        addValueWithProperties("outerRadius", 1.0f, 0.0f, 3.0f)
        addValueWithProperties("aspectRatio", 2.0f, 0.0f, 4.0f)
    }

    override fun isShaderEditable(): Boolean {
        return false
    }

    companion object {
        const val typeName = "CurveEffect"
    }
}
