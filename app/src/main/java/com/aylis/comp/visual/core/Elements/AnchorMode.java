package com.aylis.comp.visual.core.Elements;

import com.aylis.Common.tlog;

public class AnchorMode {
    public static final int Anchor_Center = 2;
    public static final int Anchor_End = 3;
    public static final int Anchor_Na = 0;
    public static final int Anchor_Start = 1;
    public static final String typeName1 = "start";
    public static final String typeName2 = "center";
    public static final String typeName3 = "end";
    public static final String typeName0 = "unset";
    public static final String[] modes = {typeName0, "start", "center", "end"};

    public static int create(String name, int defaultValue) {
        if (name == null) return defaultValue;
        switch (name.toLowerCase().trim()) {
            case "unset":
            case "na":
                return Anchor_Na;
            case "start":
                return Anchor_Start;
            case "center":
                return Anchor_Center;
            case "end":
                return Anchor_End;
            default:
                tlog.w("unknown typeName: " + name);
                return defaultValue;
        }
    }

    public static String getTypeName(int mode, int defaultMode) {
        if (mode == 0) return typeName0;
        if (mode == 1) return "start";
        if (mode == 2) return "center";
        if (mode == 3) return "end";

        if (defaultMode == 0) return typeName0;
        if (defaultMode == 1) return "start";
        if (defaultMode == 2) return "center";
        if (defaultMode == 3) return "end";

        return typeName0;
    }
}
