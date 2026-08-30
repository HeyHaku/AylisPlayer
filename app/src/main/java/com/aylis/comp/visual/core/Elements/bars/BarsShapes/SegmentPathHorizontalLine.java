

package com.aylis.comp.visual.core.Elements.bars.BarsShapes;

import android.graphics.PointF;
import android.graphics.RectF;
import com.aylis.comp.visual.core.Elements.Element;
import com.aylis.comp.visual.core.Graphic.RenderState;

public class SegmentPathHorizontalLine implements ISegmentPath {

    public static final String typeName = "HorizontalLine";

    boolean vertical = false;

    public SegmentPathHorizontalLine setVertical(boolean val)
    {
        vertical = val;
        return this;
    }

    @Override
    public void process(RenderState renderData) {

    }

    @Override
    public void getPointOnPath(int pointIndex, int pointsCount, RectF bounds, PointF pathPointOut, PointF pathPointVecOut) {
        if (vertical) {
            float parts = bounds.height() / pointsCount;
            float paddedBoundsTop = bounds.top + parts;
            float paddedBoundsHeight = bounds.height() - (parts * 2.0f);
            float step = Math.round(paddedBoundsHeight / (float) pointsCount);
            float miny = paddedBoundsTop + (step * 0);
            float maxy = paddedBoundsTop + (step * pointsCount);

            float centerYPadding = (bounds.height() - (maxy - miny)) * 0.5f;
            pathPointOut.x = bounds.centerX();
            pathPointOut.y = centerYPadding + paddedBoundsTop + (step * pointIndex);

            pathPointVecOut.x = 1.0f;
            pathPointVecOut.y = 0.0f;
        } else {
            float parts = bounds.width() / pointsCount;
            float paddedBoundsLeft = bounds.left + parts;
            float paddedBoundsWidth = bounds.width() - (parts * 2.0f);
            float step = Math.round(paddedBoundsWidth / (float) pointsCount);
            float minx = paddedBoundsLeft + (step * 0);
            float maxx = paddedBoundsLeft + (step * pointsCount);

            float centerXPadding = (bounds.width() - (maxx - minx)) * 0.5f;
            pathPointOut.x = centerXPadding + paddedBoundsLeft + (step * pointIndex);
            pathPointOut.y = bounds.centerY();

            pathPointVecOut.x = 0.0f;
            pathPointVecOut.y = 1.0f;
        }
    }

    @Override
    public float getPathLength(RectF bounds, int neededPointsCountHint) {
        if (vertical) {
            float parts = bounds.height() / neededPointsCountHint;
            return bounds.height() - (parts * 2.0f);
        } else {
            float parts = bounds.width() / neededPointsCountHint;
            return bounds.width() - (parts * 2.0f);
        }
    }

    @Override
    public int getPreferredPointCount(RectF bounds) {
        return 2;
    }

    @Override
    public void onApplyCustomization(Element.CustomizationData customizationData) {
        vertical = customizationData.getPropertyBool("vertical", vertical);
    }

    @Override
    public void onReadCustomization(Element.CustomizationData outCustomizationData) {
        outCustomizationData.putPropertyBool("vertical", vertical, "b");
    }
}

