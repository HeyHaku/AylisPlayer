package com.aylis.comp.visual.core.Elements.Base.shakes

import android.graphics.PointF

interface IShakeCore {
    fun process(totalTime: Float, isPlaying: Boolean, rms: Float, argX: Float, argY: Float, lastMeasured: PointF, outResult: PointF)
}
