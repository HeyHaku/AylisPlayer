package com.aylis.comp.visual.core.Elements.Base.shakes

import android.graphics.PointF
import kotlin.math.max
import kotlin.math.min

class BeatRandomShake(private val shakePointSmooth: PointF) : IShakeCore {
    override fun process(totalTime: Float, isPlaying: Boolean, rms: Float, argX: Float, argY: Float, lastMeasured: PointF, outResult: PointF) {
        val fMax3 = max(min(if (isPlaying) rms * 5.0f else 0.0f, 1.0f), 0.0f)
        val f5 = fMax3 * fMax3 * fMax3
        outResult.x = shakePointSmooth.x * f5 * argX * 0.025f
        outResult.y = shakePointSmooth.y * f5 * argY * 0.025f
    }
}
