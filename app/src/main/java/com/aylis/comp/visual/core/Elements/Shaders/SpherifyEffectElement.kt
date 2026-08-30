

package com.aylis.comp.visual.core.Elements.Shaders

import com.aylis.comp.visual.core.Elements.DummyElement

class SpherifyEffectElement : DummyElement() {

    override fun getElementTypeName(): String {
        return typeName
    }

    override fun initCustomShader() {
        this.shaderVert = DEFAULT_VERT_SHADER
        this.shaderFrag = """
            // Cubiq - Spherify
            precision highp float;
            precision mediump int;
            precision lowp sampler2D;
            precision lowp samplerCube;

            float focalLength = 50.0;
            float sphereRotation = 0.0;

            varying vec2 vTexCoord;

            uniform sampler2D u_texture;
            uniform float u_radius;
            uniform float u_rotation;
            uniform float u_aspectRatio;
            uniform float u_textureAspect;
            uniform float u_camDistance;

            const int MAX_STEPS = 500;
            const float MAX_TRACE_DISTANCE = 1000.0;
            const float MIN_HIT_DISTANCE = 0.001;

            #define PI 3.1415
            #define TWO_PI (PI*2.0)

            int floatToInt(float f) { return int(f); }

            vec3 rotatePointAbout(in vec3 p, in vec3 o, in float r) {
                float cosTheta = cos(r);
                float sinTheta = sin(r);
                vec3 relativePosition = p - o;
                vec3 rotatedPoint = vec3(relativePosition.x * cosTheta + relativePosition.z * sinTheta, relativePosition.y, relativePosition.x * sinTheta - relativePosition.z * cosTheta);
                return rotatedPoint + o;
            }

            float sdSphere(in vec3 p, in vec3 o, in float r) {
                return length(p - o) - r;
            }

            vec4 rayMarch(in vec3 ro, in vec3 rd) {
                float distanceTraveled = 0.0;
                for (int iterations=0; iterations < MAX_STEPS; ++iterations) {
                    vec3 sphereOrigin = vec3(0.0, 0.0, -1000.0*u_camDistance);
                    float sphereRadius = u_radius*4.0;
                    vec3 currentPosition = ro + rd * distanceTraveled;
                    vec3 relativePosition = rotatePointAbout(currentPosition, sphereOrigin, sphereRotation);
                    float distanceToClosest = sdSphere(relativePosition, sphereOrigin, sphereRadius);
                    if (distanceToClosest < MIN_HIT_DISTANCE) {
                        relativePosition -= sphereOrigin;
                        vec3 normal = normalize(currentPosition - sphereOrigin);
                        float phi = atan(relativePosition.z, relativePosition.x);
                        float theta = atan(relativePosition.y, sqrt(pow(relativePosition.z, 2.0) + pow(relativePosition.x, 2.0)));
                        vec2 uv = vec2((phi + PI) / TWO_PI, (theta + PI/2.0) / PI);
                        vec4 color = texture2D(u_texture, uv*vec2(u_textureAspect*2.0,1.0));

                        return color;
                    }
                    if (distanceTraveled > MAX_TRACE_DISTANCE) {
                        break;
                    }
                    distanceTraveled += distanceToClosest;
                }
                return vec4(0.0);
            }

            void main() {
                vec2 xy = vTexCoord - 0.5;
                xy *= vec2(u_aspectRatio, 1.0);

                sphereRotation = u_rotation * TWO_PI;
                vec3 rayOrigin = vec3(0.0, 0.0, 0.0);
                vec3 rayDirection = normalize(vec3(xy, -focalLength));
                gl_FragColor = rayMarch(rayOrigin, rayDirection);
            }
        """.trimIndent()
    }

    override fun initCustomValues() {
        addValueWithProperties("radius", 1.0f, 0.0f, 1.0f)
        addValueWithProperties("rotation", 1.0f, 0.0f, 1.0f)
        addValueWithProperties("camDistance", 1.0f, 0.0f, 1.0f)
        addValueWithProperties("textureAspect", 0.5f, 0.0f, 2.0f)
        addValueWithProperties("aspectRatio", 2.0f, 0.0f, 4.0f)
    }

    override fun isShaderEditable(): Boolean {
        return false
    }

    companion object {
        const val typeName = "Spherify"
    }
}
