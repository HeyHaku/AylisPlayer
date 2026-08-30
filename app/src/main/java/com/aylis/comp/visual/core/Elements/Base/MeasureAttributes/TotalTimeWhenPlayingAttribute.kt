package com.aylis.comp.visual.core.Elements.Base.MeasureAttributes

import android.graphics.PointF

class TotalTimeWhenPlayingAttribute : IMeasureAttribute {
    override fun process(state: MeasureState, argX: Float, argY: Float, lastMeasured: PointF, outResult: PointF) {
        outResult.x = (state.totalTimeWhenPlaying * argX) % 1.0f
        outResult.y = (state.totalTimeWhenPlaying * argY) % 1.0f
    }
}
