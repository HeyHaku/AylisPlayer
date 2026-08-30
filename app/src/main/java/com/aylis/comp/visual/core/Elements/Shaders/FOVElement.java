

package com.aylis.comp.visual.core.Elements.Shaders;

import com.aylis.comp.visual.core.Elements.DummyElement;

public class FOVElement extends DummyElement {
    public static final String typeName = "FOV";

    @Override
    public String getElementTypeName() {
        return typeName;
    }

    @Override
    public void initCustomShader() {
        this.shaderVert = DEFAULT_VERT_SHADER;
        this.shaderFrag = "precision mediump float;\n" +
                "precision mediump int;\n" +
                "precision lowp sampler2D;\n" +
                "precision lowp samplerCube;\n" +
                "\n" +
                "varying vec2 vTexCoord;\n" +
                "uniform sampler2D u_texture;\n" +
                "\n" +
                "uniform float u_scale;\n" +
                "uniform float u_curvature;\n" +
                "uniform float u_aspectRatio;\n" +
                "uniform float u_repeatTexture;\n" +
                "\n" +
                "#define REPEAT_TEXTURE !floatToBool(u_repeatTexture)\n" +
                "\n" +
                "bool floatToBool(float f) { return f > 0.5; }\n" +
                "\n" +
                "vec2 GLCoord2TextureCoord(vec2 glCoord) {\n" +
                "    return glCoord * vec2(1.0, -1.0) / 2.0 +vec2(0.5);\n" +
                "}\n" +
                "void main() {\n" +
                "\n" +
                "    vec2 vPosition = (vTexCoord - vec2(0.5));\n" +
                "    vPosition.y *= -1.0;\n" +
                "\n" +
                "    float b = 1280./720.;\n" +
                "    float scale = u_scale; // Set your uniform value here\n" +
                "\n" +
                "    float _A = 2.0;\n" +
                "    float _B = b;\n" +
                "    float _F = u_curvature*4.;\n" +
                "    float L = length(vec3(vPosition.xy / scale, _F));\n" +
                "\n" +
                "    vec2 vMapping = vPosition.xy * _F / L;\n" +
                "    vMapping = vMapping * vec2(u_aspectRatio, 1.);\n" +
                "\n" +
                "    vMapping = GLCoord2TextureCoord(vMapping / scale);\n" +
                "\n" +
                "    vec4 textureColor = texture2D(u_texture, vMapping);\n" +
                "\n" +
                "    gl_FragColor = textureColor;\n" +
                "\n" +
                "    vec4 image2 = texture2D(u_texture, vMapping);\n" +
                "    \n" +
                "    if (REPEAT_TEXTURE &&\n" +
                "        (vMapping.x > 0.99 || vMapping.x < 0.01 || vMapping.y > 0.99 || vMapping.y < 0.01)) {\n" +
                "        image2 = vec4(0.0);\n" +
                "    }\n" +
                "    \n" +
                "    gl_FragColor = image2;\n" +
                "}\n";
    }

    @Override
    public void initCustomValues() {
        addValueWithProperties("curvature", 0.5f, 0.0f, 1.0f);
        addValueWithProperties("scale", 0.5f, 0.0f, 1.0f);
        addValueWithProperties("aspectRatio", 1.0f, 0.0f, 2.0f);
        addValueWithProperties("repeatTexture", 1.0f, 0.0f, 1.0f);
    }

    @Override
    public boolean isShaderEditable() {
        return false;
    }
}
