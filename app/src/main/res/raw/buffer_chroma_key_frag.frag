#version 300 es

precision mediump float;
precision mediump int;
precision lowp sampler2D;
precision lowp samplerCube;

//"in" attributes from our vertex shader
in vec4 vColor;
in vec2 vTexCoord;

//declare uniforms
uniform sampler2D u_texture;
uniform vec3 u_chromaKeyColor;
uniform float u_chromaKeyTolerance;
uniform float u_chromaKeySmoothness;

out vec4 fragColor;

void main() {
    vec4 color1 = texture(u_texture, vTexCoord);
    
    float chromaDist = distance(color1.rgb, u_chromaKeyColor);
    float chromaAlpha = smoothstep(u_chromaKeyTolerance, u_chromaKeyTolerance + u_chromaKeySmoothness + 0.0001, chromaDist);
    
    fragColor = vColor * color1;
    fragColor.a *= chromaAlpha;
}
