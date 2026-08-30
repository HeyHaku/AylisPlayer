#version 300 es

//http://stackoverflow.com/questions/12105330/how-does-this-simple-fxaa-work
precision mediump float;
precision mediump int;
precision lowp sampler2D;
precision lowp samplerCube;

//"in" attributes from our vertex shader
in vec4 vColor;
in vec2 vTexCoord;

//declare uniforms
uniform sampler2D u_texture;
uniform float resolutionW;
uniform float resolutionH;
uniform float radius;
uniform vec2 u_dir;

out vec4 fragColor;

void main() {

        vec2 frameBufSize = vec2(resolutionW, resolutionH);

        float FXAA_SPAN_MAX = 8.0;
        float FXAA_REDUCE_MUL = 1.0/8.0;
        float FXAA_REDUCE_MIN = 1.0/128.0;

        vec3 rgbNW=texture(u_texture,vTexCoord+(vec2(-1.0,-1.0)/frameBufSize)).xyz;
        vec3 rgbNE=texture(u_texture,vTexCoord+(vec2(1.0,-1.0)/frameBufSize)).xyz;
        vec3 rgbSW=texture(u_texture,vTexCoord+(vec2(-1.0,1.0)/frameBufSize)).xyz;
        vec3 rgbSE=texture(u_texture,vTexCoord+(vec2(1.0,1.0)/frameBufSize)).xyz;
        vec3 rgbM=texture(u_texture,vTexCoord).xyz;

        vec3 luma=vec3(0.299, 0.587, 0.114);
        float lumaNW = dot(rgbNW, luma);
        float lumaNE = dot(rgbNE, luma);
        float lumaSW = dot(rgbSW, luma);
        float lumaSE = dot(rgbSE, luma);
        float lumaM  = dot(rgbM,  luma);

        float lumaMin = min(lumaM, min(min(lumaNW, lumaNE), min(lumaSW, lumaSE)));
        float lumaMax = max(lumaM, max(max(lumaNW, lumaNE), max(lumaSW, lumaSE)));

        vec2 dir;
        dir.x = -((lumaNW + lumaNE) - (lumaSW + lumaSE));
        dir.y =  ((lumaNW + lumaSW) - (lumaNE + lumaSE));

        float dirReduce = max(
            (lumaNW + lumaNE + lumaSW + lumaSE) * (0.25 * FXAA_REDUCE_MUL),
            FXAA_REDUCE_MIN);

        float rcpDirMin = 1.0/(min(abs(dir.x), abs(dir.y)) + dirReduce);

        dir = min(vec2( FXAA_SPAN_MAX,  FXAA_SPAN_MAX),
              max(vec2(-FXAA_SPAN_MAX, -FXAA_SPAN_MAX),
              dir * rcpDirMin)) / frameBufSize;

        vec3 rgbA = (1.0/2.0) * (
            texture(u_texture, vTexCoord.xy + dir * (1.0/3.0 - 0.5)).xyz +
            texture(u_texture, vTexCoord.xy + dir * (2.0/3.0 - 0.5)).xyz);
        vec3 rgbB = rgbA * (1.0/2.0) + (1.0/4.0) * (
            texture(u_texture, vTexCoord.xy + dir * (0.0/3.0 - 0.5)).xyz +
            texture(u_texture, vTexCoord.xy + dir * (3.0/3.0 - 0.5)).xyz);
        float lumaB = dot(rgbB, luma);

        if((lumaB < lumaMin) || (lumaB > lumaMax)){
            fragColor.xyz=rgbA;
            fragColor.a=1.0;
        }else{
            fragColor.xyz=rgbB;
            fragColor.a=1.0;
        }
}