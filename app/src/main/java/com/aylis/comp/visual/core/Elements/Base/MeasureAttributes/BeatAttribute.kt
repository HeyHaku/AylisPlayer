package com.aylis.comp.visual.core.Elements.Base.MeasureAttributes

import android.graphics.PointF

class BeatAttribute : IMeasureAttribute {
    override fun process(state: MeasureState, argX: Float, argY: Float, lastMeasured: PointF, outResult: PointF) {
        // Увеличиваем чувствительность обычного Beat в 2 раза
        outResult.x = state.rms * argX * 1.5f
        outResult.y = state.rms * argY * 1.5f
    }
}
