

package com.aylis.comp.visual.core.Elements.bars.BarsShapes;

import android.graphics.PointF;
import android.graphics.RectF;
import com.aylis.comp.visual.core.Elements.Element;
import com.aylis.comp.visual.core.Graphic.RenderState;

public class SegmentPathCircle implements ISegmentPath {

    public static final String typeName = "Circle";

    private float radius = 0.5f;

    public SegmentPathCircle setRadius(float val)
    {
        radius = val;
        return this;
    }

    @Override
    public void process(RenderState renderData) {

    }

    @Override
    public void getPointOnPath(int pointIndex, int pointsCount, RectF bounds, PointF pathPointOut, PointF pathPointVecOut) {
        float progress = (float) pointIndex / (float) pointsCount;
        double length = 1.0;
        double angle = 2.0 * Math.PI * progress;

        pathPointVecOut.x = (float) (length * -Math.sin(angle));
        pathPointVecOut.y = (float) (length * Math.cos(angle));

        float drawRadius;
        if (bounds.width() < bounds.height())
            drawRadius = bounds.width();
        else
            drawRadius = bounds.height();

        drawRadius = drawRadius * 0.5f * radius;

        pathPointOut.x = bounds.centerX() + (-pathPointVecOut.x * drawRadius);
        pathPointOut.y = bounds.centerY() + (-pathPointVecOut.y * drawRadius);
    }

    @Override
    public float getPathLength(RectF bounds, int neededPointsCountHint) {

        float drawRadius;
        if (bounds.width() < bounds.height())
            drawRadius = bounds.width();
        else
            drawRadius = bounds.height();

        double circumference = 2.0 * Math.PI * radius;

        return (float) circumference * drawRadius * 0.5f;
    }

    @Override
    public int getPreferredPointCount(RectF bounds) {
        float segmentW = 18.0f;

        float drawRadius;
        if (bounds.width() < bounds.height())
            drawRadius = bounds.width();
        else
            drawRadius = bounds.height();

        float circumference = (float) (2.0 * Math.PI * drawRadius);
        int num = (int) ((circumference / segmentW) + 0.5f);

        return Math.max(num, 18);
    }

    @Override
    public void onApplyCustomization(Element.CustomizationData customizationData) {
        radius = customizationData.getPropertyFloat("radius", radius);
    }

    @Override
    public void onReadCustomization(Element.CustomizationData outCustomizationData) {
        outCustomizationData.putPropertyFloat("radius", radius, "f 0.1 3.0");
    }
}

