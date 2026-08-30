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

out vec4 fragColor;

void main() {
	vec4 color1 = texture(u_texture,vTexCoord);
	fragColor = vColor * color1;
}