package com.aylis.comp.visual.core.Elements.Base.MeasureAttributes

import android.graphics.PointF
import kotlin.math.max
import kotlin.math.min

class TrackPositionAttribute : IMeasureAttribute {
    override fun process(state: MeasureState, argX: Float, argY: Float, lastMeasured: PointF, outResult: PointF) {
        val duration = if (state.trackDuration > 0) state.trackDuration else 1f
        val fMax4 = max(min(state.trackPosition / duration, 1.0f), 0.0f)
        outResult.x = argX * fMax4
        outResult.y = fMax4 * argY
    }
}
