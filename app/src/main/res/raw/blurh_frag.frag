#version 300 es

precision mediump float;
precision mediump int;
precision lowp sampler2D;
precision lowp samplerCube;

in vec4 vColor;
in vec2 vTexCoord;

uniform sampler2D u_texture;
uniform float resolutionW;
uniform float resolutionH;
uniform float radius;

out vec4 fragColor;

void main() {
	vec2 tc = vTexCoord;
	
	if (radius < 0.05) {
		fragColor = texture(u_texture, tc);
		return;
	}

	vec4 sum = vec4(0.0);
	float totalWeight = 0.0;
	
	// Используем динамическое расстояние в зависимости от радиуса
	float maxOffset = 7.38 * radius;
	// Ограничиваем количество шагов 30-ю для производительности, но при этом берем каждый пиксель
	int steps = int(clamp(maxOffset, 1.0, 30.0));
	float sigma = max(maxOffset / 2.0, 0.5);
	
	for(int i = -steps; i <= steps; ++i) {
		float offset = float(i);
		float weight = exp(-(offset * offset) / (2.0 * sigma * sigma));
		
		sum += texture(u_texture, vec2(tc.x + offset / resolutionW, tc.y)) * weight;
		totalWeight += weight;
	}
	
	fragColor = sum / totalWeight;
}