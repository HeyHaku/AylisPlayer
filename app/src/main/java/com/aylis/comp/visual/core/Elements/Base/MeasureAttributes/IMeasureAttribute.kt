package com.aylis.comp.visual.core.Elements.Base.MeasureAttributes

import android.graphics.PointF

interface IMeasureAttribute {
    fun process(state: MeasureState, argX: Float, argY: Float, lastMeasured: PointF, outResult: PointF)
}

class MeasureState(
    var totalTime: Float = 0f,
    var totalTimeWhenPlaying: Float = 0f,
    var accumulatedTimeAndBeat: Float = 0f,
    var accumulatedBeat: Float = 0f,
    var trackPosition: Float = 0f,
    var trackDuration: Float = 0f,
    var isPlaying: Boolean = false,
    var rms: Float = 0f,
    var frameTimeF: Float = 0f
)
