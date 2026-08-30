#version 300 es

precision mediump float;
precision mediump int;
precision lowp sampler2D;
precision lowp samplerCube;

in vec4 vColor;
in vec2 vTexCoord;

uniform sampler2D u_texture;
uniform vec2 u_offset; // (pixelOffset / resolutionW, pixelOffset / resolutionH)

out vec4 fragColor;

void main() {
    vec4 sum = vec4(0.0);
    sum += texture(u_texture, vTexCoord + vec2(-u_offset.x, -u_offset.y));
    sum += texture(u_texture, vTexCoord + vec2( u_offset.x, -u_offset.y));
    sum += texture(u_texture, vTexCoord + vec2(-u_offset.x,  u_offset.y));
    sum += texture(u_texture, vTexCoord + vec2( u_offset.x,  u_offset.y));
    fragColor = sum * 0.25;
}
