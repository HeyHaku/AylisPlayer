

package com.aylis.comp.visual.core.Elements.bars.Render;

import com.aylis.Common.tlog;

public class SegmentRendererFactory {
    public static final String typeNameNone = "None";
    public static final String[] typeNames = {"None", "Bars", "Line", SegmentRendererSharpBar.typeName, SegmentRendererRoundBar.typeName, SegmentRendererBarsV2.typeName, SegmentRendererDots.typeName, SegmentRendererPeakBar.typeName};

    public static ISegmentRenderer create(String str, ISegmentRenderer iSegmentRenderer) {
        if (getTypeName(iSegmentRenderer).equals(str)) {
            return iSegmentRenderer;
        }
        if (str == null || str.equals("None")) {
            return null;
        }
        if (str.equals("Bars")) {
            return new SegmentRendererBar();
        }
        if (str.equals("Line")) {
            return new SegmentRendererLine();
        }
        if (str.equals(SegmentRendererSharpBar.typeName)) {
            return new SegmentRendererSharpBar();
        }
        if (str.equals(SegmentRendererRoundBar.typeName)) {
            return new SegmentRendererRoundBar();
        }
        if (str.equals(SegmentRendererBarsV2.typeName)) {
            return new SegmentRendererBarsV2();
        }
        if (str.equals(SegmentRendererDots.typeName)) {
            return new SegmentRendererDots();
        }
        if (str.equals(SegmentRendererPeakBar.typeName)) {
            return new SegmentRendererPeakBar();
        }
        tlog.w("unknown typeName: " + str);
        return iSegmentRenderer;
    }

    public static String getTypeName(ISegmentRenderer iSegmentRenderer) {
        if (iSegmentRenderer == null) {
            return "None";
        }
        if (iSegmentRenderer instanceof SegmentRendererBar) {
            return "Bars";
        }
        if (iSegmentRenderer instanceof SegmentRendererLine) {
            return "Line";
        }
        if (iSegmentRenderer instanceof SegmentRendererSharpBar) {
            return SegmentRendererSharpBar.typeName;
        }
        if (iSegmentRenderer instanceof SegmentRendererRoundBar) {
            return SegmentRendererRoundBar.typeName;
        }
        if (iSegmentRenderer instanceof SegmentRendererBarsV2) {
            return SegmentRendererBarsV2.typeName;
        }
        if (iSegmentRenderer instanceof SegmentRendererDots) {
            return SegmentRendererDots.typeName;
        }
        if (iSegmentRenderer instanceof SegmentRendererPeakBar) {
            return SegmentRendererPeakBar.typeName;
        }
        tlog.w("unknown instance type");
        return "unk";
    }
}
