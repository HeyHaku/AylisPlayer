

package com.aylis.comp.visual.core.Elements.Base;

import com.aylis.Common.ISimpleListFloat;
import com.aylis.comp.visual.core.Dsp.DspWindows;
import java.lang.reflect.Array;

public class FrameValuesAccumulator {
    int valuesCount = 0;
    int valuesCountMultiplied = 0;
    private Entry[] entries = new Entry[0];
    ISimpleListFloat[][] cachedLists = (ISimpleListFloat[][]) Array.newInstance((Class<?>) ISimpleListFloat.class, 0, 0);

    public static class ListAccessor2 implements ISimpleListFloat {
        int iTmp;
        final int indexInPast;
        int indexWarpedTmp;
        int jTmp;
        final FrameValuesAccumulator parent;
        int parentValuesCountTmp;
        float result2Tmp;
        float resultTmp;
        private float softnessDivider;
        private int[] softnessIndexes;
        private int softnessRadius;
        private float[] softnessWeight;
        final int timeWidth;
        final float timeWidthPlus1;

        ListAccessor2(FrameValuesAccumulator frameValuesAccumulator, int i, int i2, int i3) {
            int i4 = 0;
            this.softnessRadius = 0;
            this.softnessIndexes = new int[0];
            this.softnessWeight = new float[0];
            this.softnessDivider = 1.0f;
            this.parent = frameValuesAccumulator;
            this.indexInPast = Math.max(0, Math.min(i, frameValuesAccumulator.entries.length - 1));
            this.timeWidth = i2;
            this.softnessRadius = i3;
            this.timeWidthPlus1 = i2 + 1.0f;
            if (i3 < 1) {
                this.softnessIndexes = new int[]{0};
                this.softnessWeight = new float[]{1.0f};
                this.softnessDivider = 1.0f;
                return;
            }
            int[] iArr = new int[i3 + 1 + i3];
            this.softnessIndexes = iArr;
            this.softnessWeight = new float[iArr.length];
            this.softnessDivider = Math.max(1.0f, iArr.length * 0.05f);
            while (true) {
                int[] iArr2 = this.softnessIndexes;
                if (i4 >= iArr2.length) {
                    return;
                }
                iArr2[i4] = i4 - i3;
                this.softnessWeight[i4] = Math.abs(iArr2[i4]) + 1.0f;
                i4++;
            }
        }

        @Override
        public int size() {
            return this.parent.valuesCount;
        }

        @Override
        public float get(int i) {
            if (this.parent.entries.length == 0 || this.parent.entries[0].barSmoothValues.length == 0) return 0.0f;
            this.parentValuesCountTmp = this.parent.entries[0].barSmoothValues.length;
            this.resultTmp = 0.0f;
            this.iTmp = 0;
            while (true) {
                int i2 = this.iTmp;
                int[] iArr = this.softnessIndexes;
                if (i2 < iArr.length) {
                    this.indexWarpedTmp = ((iArr[i2] + i) + this.parent.valuesCountMultiplied) % this.parentValuesCountTmp;
                    this.result2Tmp = this.parent.entries[this.indexInPast].barSmoothValues[this.indexWarpedTmp];
                    int i3 = this.indexInPast;
                    while (true) {
                        this.jTmp = i3;
                        if (this.jTmp < this.indexInPast + this.timeWidth) {
                            if (this.jTmp < this.parent.entries.length) {
                                this.result2Tmp += this.parent.entries[this.jTmp].barSmoothValues[this.indexWarpedTmp];
                            }
                            i3 = this.jTmp + 1;
                        } else {
                            break;
                        }
                    }
                    float f = this.result2Tmp / this.timeWidthPlus1;
                    this.result2Tmp = f;
                    float f2 = this.resultTmp;
                    float[] fArr = this.softnessWeight;
                    int i4 = this.iTmp;
                    this.resultTmp = f2 + ((f * 1.0f) / (fArr[i4] * 1.0f));
                    this.iTmp = i4 + 1;
                } else {
                    return this.resultTmp / this.softnessDivider;
                }
            }
        }

        public boolean compareParameters(FrameValuesAccumulator frameValuesAccumulator, int i, int i2, int i3) {
            return frameValuesAccumulator == this.parent && i == this.indexInPast && i2 == this.timeWidth && i3 == this.softnessRadius;
        }
    }

    public static class ListAccessor implements ISimpleListFloat {
        int iTmp;
        final int indexInPast;
        int indexWarpedTmp;
        int jTmp;
        final FrameValuesAccumulator parent;
        int parentValuesCountTmp;
        float result2Tmp;
        float resultTmp;
        private float softnessDivider;
        private int[] softnessIndexes;
        private int softnessRadius;
        private float[] softnessWeight;
        final int timeWidth;
        final float timeWidthPlus1;

        ListAccessor(FrameValuesAccumulator frameValuesAccumulator, int i, int i2, int i3) {
            int i4 = 0;
            this.softnessRadius = 0;
            this.softnessIndexes = new int[0];
            this.softnessWeight = new float[0];
            this.softnessDivider = 1.0f;
            this.parent = frameValuesAccumulator;
            this.indexInPast = Math.max(0, Math.min(i, frameValuesAccumulator.entries.length - 1));
            this.timeWidth = i2;
            this.softnessRadius = i3;
            this.timeWidthPlus1 = i2 + 1.0f;
            if (i3 < 1) {
                this.softnessIndexes = new int[]{0};
                this.softnessWeight = new float[]{1.0f};
                this.softnessDivider = 1.0f;
                return;
            }
            int[] iArr = new int[i3 + 1 + i3];
            this.softnessIndexes = iArr;
            this.softnessWeight = new float[iArr.length];
            this.softnessDivider = Math.max(1.0f, iArr.length * 0.05f);
            while (true) {
                int[] iArr2 = this.softnessIndexes;
                if (i4 >= iArr2.length) {
                    return;
                }
                int i5 = i4 - i3;
                iArr2[i4] = i5;
                this.softnessWeight[i4] = Math.max(DspWindows.hammingWindow(((iArr2.length + 2) / 2) + i5, iArr2.length + 2) - 0.05f, 0.0f);
                i4++;
            }
        }

        @Override
        public int size() {
            return this.parent.valuesCount;
        }

        @Override
        public float get(int i) {
            if (this.parent.entries.length == 0 || this.parent.entries[0].barSmoothValues.length == 0) return 0.0f;
            this.parentValuesCountTmp = this.parent.entries[0].barSmoothValues.length;
            this.resultTmp = 0.0f;
            this.iTmp = 0;
            while (true) {
                int i2 = this.iTmp;
                int[] iArr = this.softnessIndexes;
                if (i2 < iArr.length) {
                    this.indexWarpedTmp = ((iArr[i2] + i) + this.parent.valuesCountMultiplied) % this.parentValuesCountTmp;
                    this.result2Tmp = this.parent.entries[this.indexInPast].barSmoothValues[this.indexWarpedTmp];
                    int i3 = this.indexInPast;
                    while (true) {
                        this.jTmp = i3;
                        if (this.jTmp < this.indexInPast + this.timeWidth) {
                            if (this.jTmp < this.parent.entries.length) {
                                this.result2Tmp += this.parent.entries[this.jTmp].barSmoothValues[this.indexWarpedTmp];
                            }
                            i3 = this.jTmp + 1;
                        } else {
                            break;
                        }
                    }
                    float f = this.result2Tmp / this.timeWidthPlus1;
                    this.result2Tmp = f;
                    float f2 = this.resultTmp;
                    float[] fArr = this.softnessWeight;
                    int i4 = this.iTmp;
                    this.resultTmp = f2 + (f * fArr[i4]);
                    this.iTmp = i4 + 1;
                } else {
                    return this.resultTmp / this.softnessDivider;
                }
            }
        }

        public boolean compareParameters(FrameValuesAccumulator frameValuesAccumulator, int i, int i2, int i3) {
            return frameValuesAccumulator == this.parent && i == this.indexInPast && i2 == this.timeWidth && i3 == this.softnessRadius;
        }
    }

    class Entry {
        float[] barSmoothValues = new float[0];

        Entry() {
        }
    }

    public FrameValuesAccumulator() {
        setFramesCount(10);
    }

    public void setFramesCount(int i) {
        if (this.entries.length == i) {
            return;
        }
        this.entries = new Entry[i];
        int i2 = 0;
        while (true) {
            Entry[] entryArr = this.entries;
            if (i2 < entryArr.length) {
                entryArr[i2] = new Entry();
                i2++;
            } else {
                int i3 = this.valuesCount;
                this.valuesCount = 0;
                this.valuesCountMultiplied = 0;
                setValuesCount(i3);
                this.cachedLists = (ISimpleListFloat[][]) Array.newInstance((Class<?>) ISimpleListFloat.class, this.entries.length, this.valuesCount);
                return;
            }
        }
    }

    public void setValuesCount(int i) {
        if (this.valuesCount == i) {
            return;
        }
        this.valuesCount = i;
        this.valuesCountMultiplied = i * 100;
        int i2 = 0;
        while (true) {
            Entry[] entryArr = this.entries;
            if (i2 >= entryArr.length) {
                this.cachedLists = (ISimpleListFloat[][]) Array.newInstance((Class<?>) ISimpleListFloat.class, entryArr.length, i);
                return;
            } else {
                entryArr[i2].barSmoothValues = new float[i];
                i2++;
            }
        }
    }

    public void addFrame(float[] fArr) {
        setValuesCount(fArr.length);
        Entry[] entryArr = this.entries;
        Entry entry = entryArr[entryArr.length - 1];
        for (int length = entryArr.length - 1; length > 0; length--) {
            Entry[] entryArr2 = this.entries;
            entryArr2[length] = entryArr2[length - 1];
        }
        Entry[] entryArr3 = this.entries;
        entryArr3[0] = entry;
        System.arraycopy(fArr, 0, entryArr3[0].barSmoothValues, 0, fArr.length);
    }

    public float[] getFrameValues(int i, int i2) {
        if (i < 0 || i >= entries.length) return new float[0];
        return this.entries[i].barSmoothValues;
    }

    public float getFrameValue(int i, int i2, int i3) {
        if (i2 < 0 || i2 >= entries.length) return 0.0f;
        float f = this.entries[i2].barSmoothValues[i];
        for (int i4 = i2; i4 < i2 + i3; i4++) {
            if (i4 < entries.length) {
                f += this.entries[i4].barSmoothValues[i];
            }
        }
        return f / (i3 + 1.0f);
    }

    public ISimpleListFloat createFrameValuesAccessorList(int i, int i2, int i3, ISimpleListFloat iSimpleListFloat) {
        if (entries.length == 0) return iSimpleListFloat;
        int iMax = Math.max(0, Math.min(i, this.entries.length - 1));
        int iMin = Math.min(i2, (this.entries.length - 1) - iMax);
        return (iSimpleListFloat != null && (iSimpleListFloat instanceof ListAccessor) && ((ListAccessor) iSimpleListFloat).compareParameters(this, iMax, iMin, i3)) ? iSimpleListFloat : new ListAccessor(this, iMax, iMin, i3);
    }
}
