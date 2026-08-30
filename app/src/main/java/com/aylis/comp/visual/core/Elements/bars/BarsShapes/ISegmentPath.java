

package com.aylis.comp.visual.core.Elements.bars.BarsShapes;

import android.graphics.PointF;
import android.graphics.RectF;
import com.aylis.comp.visual.core.Elements.ICustomizable;
import com.aylis.comp.visual.core.Graphic.RenderState;

public interface ISegmentPath extends ICustomizable {

    void process(RenderState renderData);

    void getPointOnPath(int pointIndex, int pointsCount, RectF bounds, PointF pathPointOut, PointF pathPointVecOut);

    float getPathLength(RectF bounds, int neededPointsCountHint);

    int getPreferredPointCount(RectF bounds);
}

