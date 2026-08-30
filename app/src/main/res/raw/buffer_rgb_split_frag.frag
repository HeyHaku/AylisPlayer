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

uniform vec2 dirAmount;

uniform vec4 splitColor0;
uniform vec4 splitColor1;
uniform vec4 splitColor2;

out vec4 fragColor;

void main() {

	vec4 color0 = texture(u_texture,vTexCoord+dirAmount);
	vec4 color1 = texture(u_texture,vTexCoord);
	vec4 color2 = texture(u_texture,vTexCoord-dirAmount);

    vec4 finalColor;

    finalColor.r = (color0.r * splitColor0.r) + (color1.r * splitColor1.r) + (color2.r * splitColor2.r);
    finalColor.g = (color0.g * splitColor0.g) + (color1.g * splitColor1.g) + (color2.g * splitColor2.g);
    finalColor.b = (color0.b * splitColor0.b) + (color1.b * splitColor1.b) + (color2.b * splitColor2.b);

    finalColor.a = (color0.a * splitColor0.a + color1.a * splitColor1.a + color2.a * splitColor2.a);

	fragColor = finalColor;
}
