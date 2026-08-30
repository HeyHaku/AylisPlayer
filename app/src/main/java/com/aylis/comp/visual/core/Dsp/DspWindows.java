

package com.aylis.comp.visual.core.Dsp;

public class DspWindows {

    public static float hannWindow(int n, int N) {
        return 0.5f * (1.0f - (float) Math.cos((2.0 * Math.PI * n) / (N - 1)));
    }

    public static float hammingWindow(int n, int N) {
        return 0.54f - 0.46f * (float) Math.cos((2 * Math.PI * n) / (N - 1));
    }

    static float I0(float x) {
        float y = x / 3.75f;
        y *= y;
        return 1.0f + y * (
                3.5156229f + y * (
                        3.0899424f + y * (
                                1.2067492f + y * (
                                        0.2659732f + y * (
                                                0.360768e-1f + y * 0.45813e-2f)))));
    }

    public static float kaiserWindow(int n, int length, float beta) {
        float r = ((2.0f * n) / length - 1.0f);
        float k = (float) (Math.PI) * beta * (float) Math.sqrt(1.0f - r * r);

        return I0(k) / I0((float) Math.PI * beta);
    }

    public static float blackmanHarrisWindow(int n, int N) {
        double a0 = 0.35875;
        double a1 = 0.48829;
        double a2 = 0.14128;
        double a3 = 0.01168;
        return (float) (a0 - a1 * Math.cos((2.0 * Math.PI * n) / (N - 1))
                           + a2 * Math.cos((4.0 * Math.PI * n) / (N - 1))
                           - a3 * Math.cos((6.0 * Math.PI * n) / (N - 1)));
    }

    public static float nuttallWindow(int n, int N) {
        double a0 = 0.3635819;
        double a1 = 0.4891775;
        double a2 = 0.1365995;
        double a3 = 0.0106411;
        return (float) (a0 - a1 * Math.cos((2.0 * Math.PI * n) / (N - 1))
                           + a2 * Math.cos((4.0 * Math.PI * n) / (N - 1))
                           - a3 * Math.cos((6.0 * Math.PI * n) / (N - 1)));
    }

    public static float flatTopWindow(int n, int N) {
        double a0 = 0.21557895;
        double a1 = 0.41663158;
        double a2 = 0.277263158;
        double a3 = 0.083578947;
        double a4 = 0.006947368;
        return (float) (a0 - a1 * Math.cos((2.0 * Math.PI * n) / (N - 1))
                           + a2 * Math.cos((4.0 * Math.PI * n) / (N - 1))
                           - a3 * Math.cos((6.0 * Math.PI * n) / (N - 1))
                           + a4 * Math.cos((8.0 * Math.PI * n) / (N - 1)));
    }

}

