#version 300 es

//combined projection and view matrix
uniform mat4 u_projView;

//"in" attributes
in vec2 Position;
in vec2 TexCoord;
in vec4 Color;

//"out" varyings to our fragment shader
out vec4 vColor;
out vec2 vTexCoord;
 
void main() {
	vColor = Color;
	vTexCoord = TexCoord;
	gl_Position = u_projView * vec4(Position, 0.0, 1.0);
}