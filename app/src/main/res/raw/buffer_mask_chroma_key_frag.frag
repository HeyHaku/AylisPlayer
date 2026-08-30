#version 300 es

precision mediump float;
precision mediump int;
precision lowp sampler2D;
precision lowp samplerCube;

in vec4 vColor;
in vec2 vTexCoord;

uniform sampler2D u_texture;
uniform sampler2D u_texture2;
uniform float maskadd;
uniform float maskmul;
uniform float mask_l_add;
uniform float mask_l_mul;

uniform float tex2_x_add;
uniform float tex2_x_mul;
uniform float tex2_y_add;
uniform float tex2_y_mul;

uniform vec3 u_chromaKeyColor;
uniform float u_chromaKeyTolerance;
uniform float u_chromaKeySmoothness;

out vec4 fragColor;

void main() {
    vec4 color1 = texture(u_texture, vTexCoord);
    
    vec2 texCoord2 = vTexCoord;
    texCoord2.x = tex2_x_add + (texCoord2.x * tex2_x_mul);
    texCoord2.y = tex2_y_add + (texCoord2.y * tex2_y_mul);
    vec4 color2 = texture(u_texture2, texCoord2);
    float inBounds = step(0.0, texCoord2.x) * step(texCoord2.x, 1.0) * step(0.0, texCoord2.y) * step(texCoord2.y, 1.0);
    color2 *= inBounds;

    float chromaDist = distance(color1.rgb, u_chromaKeyColor);
    float chromaAlpha = smoothstep(u_chromaKeyTolerance, u_chromaKeyTolerance + u_chromaKeySmoothness + 0.0001, chromaDist);

    color1 = vColor * color1;
    
    float alphaMask = maskadd + (color2.a * maskmul);
    float lumaMask = mask_l_add + (((color2.r + color2.g + color2.b) / 3.0) * mask_l_mul);
    
    color1.a = color1.a * alphaMask * lumaMask * chromaAlpha;

    fragColor = color1;
}
