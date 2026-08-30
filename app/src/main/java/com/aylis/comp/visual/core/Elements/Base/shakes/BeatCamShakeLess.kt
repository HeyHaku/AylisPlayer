package com.aylis.comp.visual.core.Elements.Base.shakes

import android.graphics.PointF
import com.aylis.Common.Interpolate
import com.aylis.comp.visual.design.HandheldMotion
import kotlin.math.max
import kotlin.math.min

class BeatCamShakeLess(
    private val handheldMotion: HandheldMotion,
    private val isRot: Boolean = false
) : IShakeCore {
    override fun process(totalTime: Float, isPlaying: Boolean, rms: Float, argX: Float, argY: Float, lastMeasured: PointF, outResult: PointF) {
        val fMax = max(min(if (isPlaying) rms * 5.0f else 0.0f, 1.0f), 0.0f)
        val f3 = fMax * fMax

        if (isRot) {
            val rot = handheldMotion.getRot(((totalTime * 0.5f) * fMax * argY) % 1.0f)
            val newX = (rot / (Math.PI * 2.0)).toFloat() * argX * fMax * 0.5f * 10.0f
            val newY = (rot / (Math.PI * 2.0)).toFloat() * argX * fMax * 0.5f * 10.0f
            outResult.x = lastMeasured.x + (newX - lastMeasured.x) * 0.8f
            outResult.y = lastMeasured.y + (newY - lastMeasured.y) * 0.8f
        } else {
            val pos = handheldMotion.getPos(((totalTime * 0.5f) * fMax * argY) % 1.0f)
            val newX = (pos.x - 0.5f) * argX * f3 * 0.06f
            val newY = (pos.y - 0.5f) * argX * f3 * 0.06f
            
            outResult.x = lastMeasured.x + (newX - lastMeasured.x) * 0.8f
            outResult.y = lastMeasured.y + (newY - lastMeasured.y) * 0.8f
        }
    }
}
