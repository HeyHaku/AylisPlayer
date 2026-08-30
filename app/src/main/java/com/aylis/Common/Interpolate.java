

package com.aylis.Common;

import android.graphics.Color;

public class Interpolate {

    public static void Lerp(Vec2f result, Vec2f a, Vec2f b, double timeNormal) {
        result.x = a.x + (b.x - a.x) * (float) timeNormal;
        result.y = a.y + (b.y - a.y) * (float) timeNormal;
    }

    public static float Lerp(float a, float b, double timeNormal) {
        return a + (b - a) * (float) timeNormal;
    }

    public static double Lerp(double a, double b, double timeNormal) {
        return a + (b - a) * timeNormal;
    }

    public static int LerpColor(int a, int b, float t) {

        float aL = Math.max(Color.red(a), Math.max(Color.green(a), Color.blue(a)));
        float bL = Math.max(Color.red(b), Math.max(Color.green(b), Color.blue(b)));
        float oL = aL + (bL - aL) * t;

        float cr = Color.red(a) + (Color.red(b) - Color.red(a)) * t;
        float cg = Color.green(a) + (Color.green(b) - Color.green(a)) * t;
        float cb = Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t;
        float ca = Color.alpha(a) + (Color.alpha(b) - Color.alpha(a)) * t;

        float len = (float) Math.sqrt(cr * cr + cg * cg + cb * cb);
        cr /= len;
        cg /= len;
        cb /= len;
        return Color.argb((int) ca, (int) (cr * oL), (int) (cg * oL), (int) (cb * oL));
    }
}
