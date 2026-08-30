

package com.aylis.comp.visual.core.Elements.bars.BarsShapes;

import android.graphics.PointF;
import android.graphics.RectF;
import com.aylis.comp.visual.core.Elements.Element;
import com.aylis.comp.visual.core.Graphic.RenderState;

public class SegmentPathRoundedRect implements ISegmentPath {
    public static final String typeName = "RoundedRect";
    SegmentPathSided rect = new SegmentPathSided();
    SegmentPathCircle circle = new SegmentPathCircle();

    public SegmentPathRoundedRect() {
        this.rect.setSides(4);
    }

    @Override
    public int getPreferredPointCount(RectF rectF) {
        return 4;
    }

    @Override
    public void onApplyCustomization(Element.CustomizationData customizationData) {
    }

    @Override
    public void onReadCustomization(Element.CustomizationData outCustomizationData) {
    }

    public SegmentPathRoundedRect setRadius(float f) {
        this.circle.setRadius(0.5f);
        return this;
    }

    @Override
    public void process(RenderState renderState) {
        this.rect.process(renderState);
    }

    @Override
    public void getPointOnPath(int pointIndex, int pointsCount, RectF bounds, PointF pathPointOut, PointF pathPointVecOut) {
        this.rect.getPointOnPath(pointIndex, pointsCount, bounds, pathPointOut, pathPointVecOut);
    }

    @Override
    public float getPathLength(RectF rectF, int i) {
        return this.rect.getPathLength(rectF, i);
    }
}
