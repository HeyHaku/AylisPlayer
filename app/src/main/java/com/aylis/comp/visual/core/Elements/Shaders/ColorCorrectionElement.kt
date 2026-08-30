

package com.aylis.comp.visual.core.Elements.Shaders

import com.aylis.comp.visual.core.Elements.DummyElement

class ColorCorrectionElement : DummyElement() {

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
            uniform float u_hueAmount;
            uniform float u_brightness;
            uniform float u_contrast;
            uniform float u_shadows;
            uniform float u_highlights;
            uniform float u_vibrance;
            uniform float u_saturation;
            uniform float u_invert;

            varying vec2 vTexCoord;

            vec4 hue (in vec3 c, in float s, in float a)
            {
                vec3 P = vec3(0.55735)*dot(vec3(0.55735),c);
                vec3 U = c-P;
                vec3 V = cross(vec3(0.55735),U);
                c = U*cos(s*6.2832) + V*sin(s*6.2832) + P;
                return vec4(c, a);
            }

            float invLerp( float a, float b, float v ) {
                return (v-a)/(b-a);
            }

            vec4 saturate(vec4 col, float adjustment)
            {
                const vec3 W = vec3(0.2125, 0.7154, 0.0721);
                vec3 color = col.rgb;
                vec3 intensityRGB = vec3(dot(color, W));
                vec4 intensityRGBA = vec4(intensityRGB, col.a);
                return mix(intensityRGBA, col, adjustment);
            }

            void main()
            {
              vec4 pixelColor = texture2D(u_texture, vTexCoord);
              float averageColor = (pixelColor.r + pixelColor.g + pixelColor.b) / 3.0;
              float maxColor = max(pixelColor.r, max(pixelColor.g, pixelColor.b));
              const vec3 luma = vec3(0.2125, 0.7154, 0.0721);
              float luminance = dot(pixelColor.rgb, luma);

              pixelColor.rgb = ((pixelColor.rgb) * max(u_contrast, 0.0));

              pixelColor.rgb += u_brightness - 1.0;
              pixelColor.rgb = ((pixelColor.rgb-vec3(0.5))*u_contrast)+vec3(0.5);
              pixelColor = hue(pixelColor.rgb, invLerp(360.0, 0.0, u_hueAmount), pixelColor.a);

              float vibrAmt = (maxColor - averageColor) * (-3.0 * u_vibrance);
              pixelColor.rgb = mix(pixelColor.rgb, vec3(maxColor), vibrAmt);

              pixelColor = saturate(pixelColor, u_saturation);

              float shadow = clamp((pow(luminance, 1.0 / (u_shadows + 1.0)) + (-0.76) * pow(luminance, 2.0 / (u_shadows + 1.0))) - luminance, 0.0, 1.0);
              float highlight = clamp((1.0 - (pow(1.0 - luminance, 1.0 / (2.0 - u_highlights)) + (-0.8) * pow(1.0 - luminance, 2.0 / (2.0 - u_highlights)))) - luminance, -1.0, 0.0);
              pixelColor.rgb = (luminance + shadow + highlight) * (pixelColor.rgb / vec3(luminance));

              if(u_invert > 0.1) { pixelColor = vec4(1.0 - pixelColor.rgb, pixelColor.a); }

              gl_FragColor = clamp(pixelColor, 0.0, 1.0);
            }
        """.trimIndent()
    }

    override fun initCustomValues() {
        addValueWithProperties("hueAmount", 0.0f, -360.0f, 360.0f)
        addValueWithProperties("brightness", 1.0f, -2.0f, 2.0f)
        addValueWithProperties("contrast", 1.0f, -1.0f, 10.0f)
        addValueWithProperties("shadows", 0.0f, 0.0f, 2.0f)
        addValueWithProperties("vibrance", 0.0f, -10.0f, 10.0f)
        addValueWithProperties("saturation", 1.0f, -10.0f, 10.0f)
        addValueWithProperties("highlights", 1.0f, -10.0f, 1.0f)
        addValueWithProperties("invert", 0.0f, 0.0f, 1.0f)
    }

    override fun isShaderEditable(): Boolean {
        return false
    }

    companion object {
        const val typeName = "ColorCorrection"
    }
}
