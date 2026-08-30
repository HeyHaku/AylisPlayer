package com.aylis.comp.visual.core.Elements.Base.MeasureAttributes

import android.graphics.PointF

class TotalTimeBackwardAttribute : IMeasureAttribute {
    override fun process(state: MeasureState, argX: Float, argY: Float, lastMeasured: PointF, outResult: PointF) {
        outResult.x = 1.0f - ((argX * state.totalTime) % 1.0f)
        outResult.y = 1.0f - ((state.totalTime * argY) % 1.0f)
    }
}
