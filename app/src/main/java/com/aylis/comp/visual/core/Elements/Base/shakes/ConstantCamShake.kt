package com.aylis.comp.visual.core.Elements.Base.shakes

import android.graphics.PointF
import com.aylis.Common.Interpolate
import com.aylis.comp.visual.design.HandheldMotion

class ConstantCamShake(
    private val handheldMotion: HandheldMotion,
    private val isRot: Boolean = false
) : IShakeCore {
    override fun process(totalTime: Float, isPlaying: Boolean, rms: Float, argX: Float, argY: Float, lastMeasured: PointF, outResult: PointF) {
        if (isRot) {
            val rot = handheldMotion.getRot(((totalTime * 0.5f) * argY) % 1.0f)
            val fLerp = lastMeasured.x + ((rot / (Math.PI * 2.0)).toFloat() * argX * 0.5f - lastMeasured.x) * 0.1f
            outResult.x = fLerp
            outResult.y = fLerp
        } else {
            val pos = handheldMotion.getPos(((totalTime * 0.5f) * argY) % 1.0f)
            val newX = (pos.x - 0.5f) * argX * 0.06f
            val newY = (pos.y - 0.5f) * argX * 0.06f
            
            outResult.x = lastMeasured.x + (newX - lastMeasured.x) * 0.1f
            outResult.y = lastMeasured.y + (newY - lastMeasured.y) * 0.1f
        }
    }
}
