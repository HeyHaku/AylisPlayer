

package com.aylis.Common;

public class Vec3f {
    public float x, y, z;

    public Vec3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Vec3f cross(Vec3f v, Vec3f v2) {
        return new Vec3f(
                v.y * v2.z - v.z * v2.y,
                v.z * v2.x - v.x * v2.z,
                v.x * v2.y - v.y * v2.x);
    }

    public float length() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public void normalize() {
        float len = length();
        x /= len;
        y /= len;
        z /= len;
    }

    public Vec3f normalizedResult() {
        float len = length();
        return new Vec3f(x / len, y / len, z / len);
    }

    public Vec3f cross(Vec3f v) {
        return new Vec3f(
                y * v.z - z * v.y,
                z * v.x - x * v.z,
                x * v.y - y * v.x);
    }

    public float dot(Vec3f v1) {
        return (x * v1.x + y * v1.y + z * v1.z);
    }

}

