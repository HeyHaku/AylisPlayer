package com.aylis.comp.visual.core.Elements.Base.MeasureAttributes

import android.graphics.PointF

class TotalTimeAndBeatAttribute : IMeasureAttribute {
    override fun process(state: MeasureState, argX: Float, argY: Float, lastMeasured: PointF, outResult: PointF) {
        var result = lastMeasured.x
        if (state.isPlaying) {
            result += (state.frameTimeF * argX * 0.5f) + (state.rms * argY * 0.5f)
        }
        outResult.x = result
        outResult.y = result
    }
}
