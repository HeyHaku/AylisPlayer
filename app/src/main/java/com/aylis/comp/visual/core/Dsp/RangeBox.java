

package com.aylis.comp.visual.core.Dsp;

public class RangeBox {
    private float maxVal;
    private float maxValModeMax;
    private float modeMaxFactor;
    private float val;
    private float valModeMax;
    private int x0i;
    private int x1i;
    private float x1 = 0.0f;
    private float x0 = 0.0f;
    private float y1 = 0.0f;
    private float y0 = 0.0f;

    public RangeBox(float f) {
        this.modeMaxFactor = f;
        reset(100);
        setCorners(0.0f, 1.0f, 0.0f, 1.0f);
    }

    public float getX0() {
        return this.x0;
    }

    public float getX1() {
        return this.x1;
    }

    public float getY0() {
        return this.y0;
    }

    public float getY1() {
        return this.y1;
    }

    public void setCorners(float f, float f2, float f3, float f4) {
        this.x0 = Math.min(f, f2);
        this.y0 = Math.min(f3, f4);
        this.x1 = Math.max(f + 0.01f, f2);
        this.y1 = Math.max(f3 + 1.0f, f4);
        this.x0i = 0;
        this.x1i = 0;
        this.maxVal = 1.0E-4f;
    }

    public void reset(int i) {
        this.val = 0.0f;
        this.valModeMax = 0.0f;
        float f = i;
        this.x0i = Math.round(this.x0 * f);
        this.x1i = Math.round(f * this.x1);
        this.maxValModeMax = y1 - y0;
        this.maxVal = Math.max((y1 - y0) * ((x1i - x0i) + 1), 1.0E-4f);
    }

    public void addValue(int i, float f) {
        if (i < this.x0i || i > this.x1i) {
            return;
        }
        float fMax = Math.max(0.0f, Math.min(f - y0, y1 - y0));
        this.valModeMax = Math.max(this.valModeMax, fMax);
        this.val += fMax;
    }

    public float getValueNormal() {
        float f = this.val / this.maxVal;
        return (f * (1.0f - modeMaxFactor)) + ((this.valModeMax / maxValModeMax) * modeMaxFactor);
    }
}

