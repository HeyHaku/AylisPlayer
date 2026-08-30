package com.aylis.comp.visual.core.Elements.Base.MeasureAttributes

import android.graphics.PointF

class TotalTimeAttribute : IMeasureAttribute {
    override fun process(state: MeasureState, argX: Float, argY: Float, lastMeasured: PointF, outResult: PointF) {
        outResult.x = (argX * state.totalTime) % 1.0f
        outResult.y = (state.totalTime * argY) % 1.0f
    }
}
