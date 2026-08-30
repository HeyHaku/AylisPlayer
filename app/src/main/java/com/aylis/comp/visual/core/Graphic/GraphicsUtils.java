

package com.aylis.comp.visual.core.Graphic;

import android.content.res.Resources;
import android.opengl.GLES20;
import com.aylis.Common.tlog;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class GraphicsUtils {

    public static String readResource(Resources resources, int id) {
        StringBuilder content = new StringBuilder(128);
        BufferedReader br = new BufferedReader(new InputStreamReader(resources.openRawResource(id)));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                content.append(line);
                content.append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return content.toString();
    }

    public static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);

        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    public static void checkGlError(String glOperation) {
        int error;
        if ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            tlog.w(glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    public static int f4ColorToIntColor(float[] argb) {
        int argbInt = 0;

        argbInt |= (int) (argb[3] * 255.0f) << 8 * 3;
        argbInt |= (int) (argb[0] * 255.0f) << 8 * 2;
        argbInt |= (int) (argb[1] * 255.0f) << 8;
        argbInt |= (int) (argb[2] * 255.0f);

        return argbInt;
    }

    public static void intColorToF4Color(float[] out, int argb) {
        out[3] = ((argb >> 8 * 3) & 0xFF) / 255.0f;
        out[0] = ((argb >> 8 * 2) & 0xFF) / 255.0f;
        out[1] = ((argb >> 8) & 0xFF) / 255.0f;
        out[2] = ((argb) & 0xFF) / 255.0f;
    }

    public static float getAlphaFloatFromIntColor(int argb) {
        return ((argb >> 8 * 3) & 0xFF) / 255.0f;
    }

    public static int intColorMultiply(int argb1, int argb2) {
        float[] _resultArgb = new float[4];

        _resultArgb[3] = ((argb1 >> 8 * 3) & 0xFF) / 255.0f;
        _resultArgb[0] = ((argb1 >> 8 * 2) & 0xFF) / 255.0f;
        _resultArgb[1] = ((argb1 >> 8) & 0xFF) / 255.0f;
        _resultArgb[2] = ((argb1) & 0xFF) / 255.0f;

        _resultArgb[3] *= ((argb2 >> 8 * 3) & 0xFF) / 255.0f;
        _resultArgb[0] *= ((argb2 >> 8 * 2) & 0xFF) / 255.0f;
        _resultArgb[1] *= ((argb2 >> 8) & 0xFF) / 255.0f;
        _resultArgb[2] *= ((argb2) & 0xFF) / 255.0f;

        return f4ColorToIntColor(_resultArgb);
    }

    public static void intColorToHlsa360(float[] outHlsa, int color) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float h = 0.0f, s = 0.0f, l = (max + min) / 2.0f;

        if (max != min) {
            float d = max - min;
            s = l > 0.5f ? d / (2.0f - max - min) : d / (max + min);
            if (max == r) {
                h = (g - b) / d + (g < b ? 6.0f : 0.0f);
            } else if (max == g) {
                h = (b - r) / d + 2.0f;
            } else {
                h = (r - g) / d + 4.0f;
            }
            h /= 6.0f;
        }

        outHlsa[0] = h * 360.0f;
        outHlsa[1] = s;
        outHlsa[2] = l;
        outHlsa[3] = a;
    }

    public static int hlsa360ColorToInt(float[] hlsa) {
        float h = hlsa[0] / 360.0f;
        float s = hlsa[1];
        float l = hlsa[2];
        float a = hlsa[3];

        float r, g, b;
        if (s == 0.0f) {
            r = g = b = l;
        } else {
            float q = l < 0.5f ? l * (1.0f + s) : l + s - l * s;
            float p = 2.0f * l - q;
            r = hueToRgb(p, q, h + 1.0f / 3.0f);
            g = hueToRgb(p, q, h);
            b = hueToRgb(p, q, h - 1.0f / 3.0f);
        }

        int ir = Math.min(255, Math.max(0, (int) (r * 255.0f)));
        int ig = Math.min(255, Math.max(0, (int) (g * 255.0f)));
        int ib = Math.min(255, Math.max(0, (int) (b * 255.0f)));
        int ia = Math.min(255, Math.max(0, (int) (a * 255.0f)));

        return (ia << 24) | (ir << 16) | (ig << 8) | ib;
    }

    private static float hueToRgb(float p, float q, float t) {
        if (t < 0.0f) t += 1.0f;
        if (t > 1.0f) t -= 1.0f;
        if (t < 1.0f / 6.0f) return p + (q - p) * 6.0f * t;
        if (t < 1.0f / 2.0f) return q;
        if (t < 2.0f / 3.0f) return p + (q - p) * (2.0f / 3.0f - t) * 6.0f;
        return p;
    }

    public static void hlsa360ColorToF4Color(float[] hlsa, float[] outF4) {
        int color = hlsa360ColorToInt(hlsa);
        intColorToF4Color(outF4, color);
    }
}
