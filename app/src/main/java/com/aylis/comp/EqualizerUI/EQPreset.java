

package com.aylis.comp.EqualizerUI;

import java.util.Locale;

import com.aylis.Common.Utils;
import com.aylis.Common.UtilsSerialize;

public class EQPreset {

    public static final EQPreset empty = new EQPreset("Unnamed", 0);
    public final String name;
    public Point[] points;

    public EQPreset(String name, int pointCount) {
        this.name = name;
        points = new Point[pointCount];
    }

    public void resize(int pointCount) {
        points = new Point[pointCount];
    }

    public static EQPreset clone(EQPreset obj)
    {
        EQPreset result = new EQPreset(obj.name, obj.points.length);
        for(int i=0;i<obj.points.length;i++)
            result.points[i] = new Point(obj.points[i].freq, obj.points[i].value);

        return result;
    }

    public static EQPreset deserialize(String string) {
        String[] strObjs = UtilsSerialize.deserializeIterable(";", string);

        EQPreset result = new EQPreset("Default", strObjs.length);
        for(int i=0;i<strObjs.length;i++) {
            result.points[i] = Point.fromString(strObjs[i]);
        }

        return result;
    }

    public static String serialize(EQPreset preset) {
        return UtilsSerialize.serializeArray(";", preset.points);
    }

    public void normalizeValues(float maxAbs)
    {
        for(Point p : points)
            p.value = p.value / maxAbs;
    }

    public static class Point {
        public float freq;
        public float value;
        public Point(float freqHz, float val) {
            freq = freqHz;
            value = val;
        }

        @Override
        public String toString() {

            return String.format(java.util.Locale.US, "%.3f:%.3f", freq, value);
        }

        public static Point fromString(String s)
        {
            Point result = new Point(0.0f, 0.0f);
            int index = s.indexOf(":");
            if (index < 0) return result;
            result.freq = Utils.strToFloatSafe( s.substring(0, index) );
            result.value = Utils.strToFloatSafe( s.substring(index + 1) );

            return result;
        }
    }
}

