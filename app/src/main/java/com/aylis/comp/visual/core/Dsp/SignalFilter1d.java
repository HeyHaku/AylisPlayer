

package com.aylis.comp.visual.core.Dsp;

import com.aylis.Common.ISimpleListDouble;
import com.aylis.Common.ISimpleListShort;

public class SignalFilter1d {
    private int radius;
    private float strength;
    private int[] softnessIndexes = new int[0];
    private float[] softnessWeight = new float[0];
    private float softnessDivider = 1.0f;

    public int getRadius() {
        if (softnessIndexes == null || softnessIndexes.length < 2) {
            return 0;
        }
        return (softnessIndexes.length - 1) / 2;
    }

    public float getStrength() {
        return this.strength;
    }

    public float getSofted(int i, float[] fArr) {
        float f = 0.0f;
        for (int i2 = 0; i2 < softnessIndexes.length; i2++) {
            f += fArr[((softnessIndexes[i2] + i) + fArr.length) % fArr.length] * softnessWeight[i2];
        }
        return f / softnessDivider;
    }

    public float getSofted(int i, ISimpleListShort iSimpleListShort) {
        float f = 0.0f;
        for (int i2 = 0; i2 < softnessIndexes.length; i2++) {
            f += iSimpleListShort.get(((softnessIndexes[i2] + i) + iSimpleListShort.size()) % iSimpleListShort.size()) * softnessWeight[i2];
        }
        return f / softnessDivider;
    }

    public double getSofted(int i, ISimpleListDouble iSimpleListDouble) {
        double f = 0.0;
        for (int i2 = 0; i2 < softnessIndexes.length; i2++) {
            double d2 = iSimpleListDouble.get(((softnessIndexes[i2] + i) + (iSimpleListDouble.size() * 100)) % iSimpleListDouble.size());
            double d3 = softnessWeight[i2];
            f += d2 * d3;
        }
        return f / softnessDivider;
    }

    public double getSoftedClamped(int i, ISimpleListDouble iSimpleListDouble) {
        double f = 0.0;
        for (int i2 = 0; i2 < softnessIndexes.length; i2++) {
            int i3 = softnessIndexes[i2] + i;
            if (i3 >= 0 && i3 < iSimpleListDouble.size()) {
                double d2 = iSimpleListDouble.get(i3);
                double d3 = softnessWeight[i2];
                f += d2 * d3;
            }
        }
        return f / softnessDivider;
    }

    public void setSoftness(int i) {
        if (i < 1) {
            this.softnessIndexes = new int[]{0};
            this.softnessWeight = new float[]{1.0f};
            this.softnessDivider = 1.0f;
            return;
        }
        int[] iArr = new int[i + 1 + i];
        this.softnessIndexes = iArr;
        this.softnessWeight = new float[iArr.length];
        this.softnessDivider = iArr.length;
        for (int i2 = 0; i2 < this.softnessIndexes.length; i2++) {
            int i3 = i2 - i;
            this.softnessIndexes[i2] = i3;
            this.softnessWeight[i2] = DspWindows.hammingWindow(((this.softnessIndexes.length + 2) / 2) + i3, this.softnessIndexes.length + 2);
        }
    }

    public SignalFilter1d createHighPass(int i, float f) {
        this.strength = f;
        this.radius = i;
        if (i < 1) {
            this.softnessIndexes = new int[]{0};
            this.softnessWeight = new float[]{1.0f};
            this.softnessDivider = 1.0f;
            return this;
        }
        int[] iArr = new int[i + 1 + i];
        this.softnessIndexes = iArr;
        this.softnessWeight = new float[iArr.length];
        this.softnessDivider = 0.0f;
        for (int i2 = 0; i2 < iArr.length; i2++) {
            iArr[i2] = i2 - i;
            this.softnessWeight[i2] = -f;
            this.softnessDivider += f;
        }
        float length = (iArr.length - 1) * 1.0f;
        this.softnessWeight[i] = length;
        this.softnessDivider += length;
        return this;
    }
}

