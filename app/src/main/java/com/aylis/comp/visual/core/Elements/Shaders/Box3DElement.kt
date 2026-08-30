

package com.aylis.comp.visual.core.Elements.Shaders

import com.aylis.comp.visual.core.Elements.DummyElement

class Box3DElement : DummyElement() {

    override fun getElementTypeName(): String {
        return typeName
    }

    override fun initCustomShader() {
        this.shaderVert = DEFAULT_VERT_SHADER
        this.shaderFrag = """
            // Cubiq - 3d
            precision highp float;
            precision mediump int;
            precision lowp sampler2D;
            precision lowp samplerCube;

            #define texture texture2D
            #define iResolution vec2(640., 480.)

            #define fragColor gl_FragColor
            #define fragCoord gl_FragCoord.xy

            varying vec2 vTexCoord;

            uniform sampler2D u_texture;

            #define iChannel0 u_texture

            uniform float u_cubeRotX;
            uniform float u_cubeRotY;
            uniform float u_cubeRotZ;
            uniform float u_controlCubeRot;
            uniform float u_cubeTransX;
            uniform float u_cubeTransY;
            uniform float u_cubeTransZ;
            uniform float u_scaleX;
            uniform float u_scaleY;
            uniform float u_scaleZ;
            uniform float u_camRotX;
            uniform float u_camRotY;
            uniform float u_camRotZ;
            uniform float u_controlCamRot;
            uniform float u_aspectRatio;

            uniform float u_cubeTextureScale;
            uniform float u_cameraDistance;
            uniform float u_cameraPosX;
            uniform float u_cameraPosY;
            uniform float u_cameraPosZ;
            uniform float u_cameraTargetX;
            uniform float u_cameraTargetY;
            uniform float u_cameraTargetZ;
            uniform float u_cameraFov;

            uniform float u_splitTextures;
            uniform float u_textureAspect;
            uniform float u_planeTextureScale;
            uniform float u_value4;
            uniform float u_value5;

            uniform float u_postFX;
            uniform float u_shadow;
            uniform float u_showPlane;
            uniform float u_showCube;
            uniform float u_planeInfinite;

            #define SPLIT_TEXTURE floatToBool(u_splitTextures)

            #define POSTFX floatToBool(u_postFX)
            #define SHADOW floatToBool(u_shadow)

            #define PLANE floatToBool(u_showPlane)
            #define LIMIT_PLANE floatToBool(u_planeInfinite)

            #define CUBE floatToBool(u_showCube)

            #define ENV !true

            bool floatToBool(float f) { return f > 0.5; }

            // https://iquilezles.org/articles/boxfunctions
            vec4 iBox( in vec3 ro, in vec3 rd, in mat4 txx, in mat4 txi, in vec3 rad )
            {
                vec3 rdd = (txx*vec4(rd,0.0)).xyz;
                vec3 roo = (txx*vec4(ro,1.0)).xyz;

                vec3 m = 1.0/rdd;
                #if 1
                vec3 n = m*roo;
                vec3 k = abs(m)*rad;
                vec3 t1 = -n - k;
                vec3 t2 = -n + k;
                #else
                vec3 k = vec3(rdd.x>=0.0?rad.x:-rad.x, rdd.y>=0.0?rad.y:-rad.y, rdd.z>=0.0?rad.z:-rad.z);
                vec3 t1 = (-roo - k)*m;
                vec3 t2 = (-roo + k)*m;
                #endif
                float tN = max(max(t1.x,t1.y),t1.z);
                float tF = min(min(t2.x,t2.y),t2.z);

                if( tN>tF || tF<0.0 ) return vec4(-1.0);

                #if 1
                vec4 res = vec4(tN, step(tN,t1) );
                #else
                vec4 res = (tN>0.0) ? vec4( tN, step(vec3(tN),t1)) :
                                      vec4( tF, step(t2,vec3(tF)));
                #endif

                res.yzw = (txi * vec4(-sign(rdd)*res.yzw,0.0)).xyz;

                return res;
            }

            float sBox( in vec3 ro, in vec3 rd, in mat4 txx, in vec3 rad )
            {
                vec3 rdd = (txx*vec4(rd,0.0)).xyz;
                vec3 roo = (txx*vec4(ro,1.0)).xyz;

                vec3 m = 1.0/rdd;
                vec3 n = m*roo;
                vec3 k = abs(m)*rad;

                vec3 t1 = -n - k;
                vec3 t2 = -n + k;

                float tN = max( max( t1.x, t1.y ), t1.z );
                float tF = min( min( t2.x, t2.y ), t2.z );
                if( tN > tF || tF < 0.0) return -1.0;

                return tN;
            }

            mat4 rotationAxisAngle( vec3 v, float angle )
            {
                float s = sin( angle );
                float c = cos( angle );
                float ic = 1.0 - c;

                return mat4( v.x*v.x*ic + c,     v.y*v.x*ic - s*v.z, v.z*v.x*ic + s*v.y, 0.0,
                             v.x*v.y*ic + s*v.z, v.y*v.y*ic + c,     v.z*v.y*ic - s*v.x, 0.0,
                             v.x*v.z*ic - s*v.y, v.y*v.z*ic + s*v.x, v.z*v.z*ic + c,     0.0,
                             0.0,                0.0,                0.0,                1.0 );
            }

            mat4 translate( float x, float y, float z )
            {
                return mat4( 1.0, 0.0, 0.0, 0.0,
                             0.0, 1.0, 0.0, 0.0,
                             0.0, 0.0, 1.0, 0.0,
                             x,   y,   z,   1.0 );
            }

            mat4 translate( vec3 v )
            {
                return translate( v.x, v.y, v.z );
            }

            mat4 inverse( in mat4 m )
            {
                return mat4(
                    m[0][0], m[1][0], m[2][0], 0.0,
                    m[0][1], m[1][1], m[2][1], 0.0,
                    m[0][2], m[1][2], m[2][2], 0.0,
                    -dot(m[0].xyz,m[3].xyz),
                    -dot(m[1].xyz,m[3].xyz),
                    -dot(m[2].xyz,m[3].xyz),
                    1.0 );
            }

            void main()
            {
                vec2 p = vTexCoord * 2.0 - 1.0;
                p.x *= u_aspectRatio;

                float an = 0.4*u_controlCamRot;
                vec3 ro = vec3(
                    (u_cameraPosX+u_cameraDistance*cos(an*u_camRotX+0.001)),
                    (u_cameraPosY+u_cameraDistance*(sin(an*u_camRotY)+1.)/2.),
                    (u_cameraPosZ+u_cameraDistance*sin(an*u_camRotZ))
                ) * 10.;
                vec3 ta = vec3( u_cameraTargetX, u_cameraTargetY, u_cameraTargetZ ) * 5.;

                vec3 ww = normalize( ta - ro );
                vec3 uu = normalize( cross(ww,vec3(0.0,1.0,0.0) ) );
                vec3 vv = normalize( cross(uu,ww));
                vec3 rd = normalize( p.x*uu + p.y*vv + u_cameraFov*10.*ww );

                mat4 rot = rotationAxisAngle( normalize(vec3(u_cubeRotX,u_cubeRotY,u_cubeRotZ)), u_controlCubeRot );
                mat4 tra = translate( vec3(u_cubeTransX, u_cubeTransY, u_cubeTransZ) * 5. );
                vec3 box = vec3(u_scaleX,u_scaleY,u_scaleZ);

                mat4 txi = tra * rot;
                mat4 txx = inverse( txi );

                float tmin = 100000.0;
                vec3 nor = vec3(0.0);
                vec3 pos = vec3(0.0);

                float oid = 0.0;

                if (PLANE) {
                    float h = (-0.0-ro.y)/rd.y;
                    if( h>0.0 )
                    {
                        tmin = h;
                        nor = vec3(0.0,1.0,0.0);
                        oid = 1.0;
                    }
                }

                vec4 res = iBox( ro, rd, txx, txi, box);

                if (CUBE) {
                    if( res.x>0.0 && res.x<tmin )
                    {
                        tmin = res.x;
                        nor = res.yzw;
                        oid = 2.0;
                    }
                }

                vec4 col = vec4(0.0);
                if (ENV) col.xyz = vec3(0.6,0.75,0.85) - 0.97*rd.y;

                if( tmin<100.0 )
                {
                    pos = ro + tmin*rd;

                    float occ = 1.0;
                    vec4 mate = vec4(1.0);

                    vec2 aspect = vec2(u_textureAspect, 1.);

                    float cts = u_cubeTextureScale;
                    float pts = u_planeTextureScale;

                    float displacement = 0.0;
                    float splits = 1.0;

                    if (SPLIT_TEXTURE) {
                        cts = 0.5;
                        pts = 0.5;
                        aspect = vec2(1., 1.);
                        displacement = 0.25;
                        splits = 0.5;
                    }

                    vec2 disX = vec2(1., 1.) * displacement + 0.5;
                    vec2 disY = vec2(1.,-1.) * displacement + 0.5;
                    vec2 disZ = vec2(-1.,1.) * displacement + 0.5;

                    vec2 s = aspect*cts;

                    if( oid<1.5 )
                    {
                        vec2 myuv = 0.5+0.25*pos.xz*aspect*pts;

                        mate = vec4(vec3(0.35),1.0)*texture2D( iChannel0, mod(myuv*splits, vec2(splits) ));

                        if (
                            !LIMIT_PLANE &&
                            (myuv.x >= 1.0 || myuv.x <= 0.0 || myuv.y >= 1.0 || myuv.y <= 0.0)
                        ) mate = vec4(0.0);

                        occ = 0.2 + 0.8*smoothstep( 0.0, 1.5, length(pos.xz) );
                    }
                    else
                    {
                        vec3 opos = (txx*vec4(pos,1.0)).xyz;
                        vec3 onor = (txx*vec4(nor,0.0)).xyz;

                        mate = abs(onor.x)*texture2D( iChannel0, disX+s*opos.yz ) +
                               abs(onor.y)*texture2D( iChannel0, disY+s*opos.xz ) +
                               abs(onor.z)*texture2D( iChannel0, disZ+s*opos.xy );
                        mate.xyz *= 0.35;
                        occ = 0.6 + 0.4*nor.y;
                    }

                    col = mate;

                    if (POSTFX) {
                        vec3 lig = normalize(vec3(0.8,0.4,-0.6));
                        float dif = clamp( dot(nor,lig), 0.0, 1.0 );
                        vec3 hal = normalize(lig-rd);
                        float sha = 1.0;
                        if (SHADOW) sha = step( iBox( pos+0.001*nor, lig, txx, txi, box ).x, 0.0 );
                        float amb = 0.6 + 0.4*nor.y;
                        float bou = clamp(0.3-0.7*nor.y,0.0,1.0);
                        float spe = clamp(dot(nor,hal),0.0,1.0);

                        col.xyz = 4.0*vec3(1.00,0.80,0.60)*dif;
                        if (SHADOW) col.xyz *= sha;
                        col.xyz += 2.0*vec3(0.20,0.30,0.40)*amb;
                        col.xyz += 2.0*vec3(0.30,0.20,0.10)*bou;
                        col *= mate;
                        col.xyz += 0.3*pow(spe,8.0)*dif*sha*(0.04+0.96*pow(clamp(dot(lig,hal),0.0,1.0),5.0));
                    }
                }

                col.xyz *= 1.0 - 0.1*dot(p,p);
                col.xyz = pow( col.xyz, vec3(0.45) );
                col.xyz = clamp(col.xyz,0.0,1.0);
                col.xyz = col.xyz*col.xyz*(3.0-2.0*col.xyz);

                fragColor = vec4(col);
            }
        """.trimIndent()
    }

    override fun initCustomValues() {
        addValueWithPropertiesCat("postFX", 0.0f, 0.0f, 1.0f, "enviroment")
        addValueWithPropertiesCat("shadow", 1.0f, 0.0f, 1.0f, "enviroment")
        addValueWithPropertiesCat("showCube", 1.0f, 0.0f, 1.0f, "enviroment")
        addValueWithPropertiesCat("showPlane", 1.0f, 0.0f, 1.0f, "enviroment")
        addValueWithPropertiesCat("planeInfinite", 0.0f, 0.0f, 1.0f, "enviroment")
        addValueWithPropertiesCat("splitTextures", 0.0f, 0.0f, 1.0f, "enviroment")
        addValueWithPropertiesCat("cubeTextureScale", 1.0f, 0.0f, 2.0f, "enviroment")
        addValueWithPropertiesCat("planeTextureScale", 1.0f, 0.0f, 2.0f, "enviroment")
        addValueWithPropertiesCat("textureAspect", 1.0f, 0.0f, 2.0f, "enviroment")
        addValueWithPropertiesCat("aspectRatio", 0.5f, 0.0f, 2.0f, "enviroment")
        addValueWithPropertiesCat("controlCubeRot", 0.0f, -1.0f, 1.0f, "1_Cube")
        addValueWithPropertiesCat("cubeRotX", 1.0f, -1.0f, 1.0f, "1_Cube")
        addValueWithPropertiesCat("cubeRotY", 1.0f, -1.0f, 1.0f, "1_Cube")
        addValueWithPropertiesCat("cubeRotZ", 0.0f, -1.0f, 1.0f, "1_Cube")
        addValueWithPropertiesCat("cubeTransX", 0.0f, -1.0f, 1.0f, "1_Cube")
        addValueWithPropertiesCat("cubeTransY", 0.5f, -1.0f, 1.0f, "1_Cube")
        addValueWithPropertiesCat("cubeTransZ", 0.0f, -1.0f, 1.0f, "1_Cube")
        addValueWithPropertiesCat("scaleX", 0.5f, -1.0f, 1.0f, "1_Cube")
        addValueWithPropertiesCat("scaleY", 0.5f, -1.0f, 1.0f, "1_Cube")
        addValueWithPropertiesCat("scaleZ", 0.5f, -1.0f, 1.0f, "1_Cube")
        addValueWithPropertiesCat("controlCamRot", 0.0f, -1.0f, 1.0f, "5_Camera")
        addValueWithPropertiesCat("camRotX", 1.0f, -1.0f, 1.0f, "5_Camera")
        addValueWithPropertiesCat("camRotY", 1.0f, -1.0f, 1.0f, "5_Camera")
        addValueWithPropertiesCat("camRotZ", 1.0f, -1.0f, 1.0f, "5_Camera")
        addValueWithPropertiesCat("cameraDistance", 0.5f, -1.0f, 1.0f, "5_Camera")
        addValueWithPropertiesCat("cameraPosX", 0.0f, -1.0f, 1.0f, "5_Camera")
        addValueWithPropertiesCat("cameraPosY", 0.1f, -1.0f, 1.0f, "5_Camera")
        addValueWithPropertiesCat("cameraPosZ", 0.0f, -1.0f, 1.0f, "5_Camera")
        addValueWithPropertiesCat("cameraTargetX", 0.0f, -1.0f, 1.0f, "5_Camera")
        addValueWithPropertiesCat("cameraTargetY", 0.5f, -1.0f, 1.0f, "5_Camera")
        addValueWithPropertiesCat("cameraTargetZ", 0.0f, -1.0f, 1.0f, "5_Camera")
        addValueWithPropertiesCat("cameraFov", 0.2f, -1.0f, 1.0f, "5_Camera")
    }

    override fun isShaderEditable(): Boolean {
        return false
    }

    companion object {
        const val typeName = "3DBox"
    }
}
