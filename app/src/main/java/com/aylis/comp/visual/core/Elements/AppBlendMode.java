package com.aylis.comp.visual.core.Elements;

public class AppBlendMode {

    public static String getSelectorString() {
        return "sel Alpha PreMulAlpha Screen Add AddAlpha Over";
    }

    public static int getGlMode(String modeName) {
        if (modeName == null) return 4;
        switch (modeName) {
            case "Alpha": return 0;
            case "Screen": return 1;
            case "Add": return 2;
            case "Over": return 3;
            case "PreMulAlpha": return 4;
            case "AddAlpha": return 5;
            default:
                try {
                    return Integer.parseInt(modeName);
                } catch (NumberFormatException e) {
                    return 4;
                }
        }
    }

    public static String getTypeName(int legacyInt) {
        switch (legacyInt) {
            case 0: return "Alpha";
            case 1: return "Screen";
            case 2: return "Add";
            case 3: return "Over";
            case 4: return "PreMulAlpha";
            case 5: return "AddAlpha";
            default: return "PreMulAlpha";
        }
    }

    public static String getTypeName(String legacyOrName, int defaultMode) {
        if (legacyOrName == null || legacyOrName.isEmpty()) {
            return getTypeName(defaultMode);
        }
        try {
            int legacyInt = Integer.parseInt(legacyOrName);
            return getTypeName(legacyInt);
        } catch (NumberFormatException e) {
            return legacyOrName;
        }
    }
}
