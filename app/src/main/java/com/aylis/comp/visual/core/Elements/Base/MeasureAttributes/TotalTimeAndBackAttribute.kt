package com.aylis.comp.visual.core.Elements.Base.MeasureAttributes

import android.graphics.PointF

class TotalTimeAndBackAttribute : IMeasureAttribute {
    override fun process(state: MeasureState, argX: Float, argY: Float, lastMeasured: PointF, outResult: PointF) {
        outResult.x = argX * Math.sin(state.totalTime.toDouble()).toFloat()
        outResult.y = argY * Math.cos(state.totalTime.toDouble()).toFloat()
    }
}
