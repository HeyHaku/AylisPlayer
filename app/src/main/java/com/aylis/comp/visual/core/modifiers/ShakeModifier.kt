package com.aylis.comp.visual.core.modifiers

import kotlin.math.sin
import kotlin.math.cos

class ShakeModifier {
    var shakeXAmount: Float = 0.0f
    var shakeYAmount: Float = 0.0f
    var shakeRotationAmount: Float = 0.0f
    var shakeSpeed: Float = 0.05f

    private var internalTime: Float = 0.0f

    class ShakeResult(val x: Float, val y: Float, val rotation: Float)

    fun update(bassValue: Float, isAudioActive: Boolean): ShakeResult {
        if (!isAudioActive || bassValue <= 0.01f) {
            return ShakeResult(0f, 0f, 0f)
        }

        internalTime += 0.016f * shakeSpeed * 100.0f

        val kick = (bassValue * bassValue)

        val rawX = sin(internalTime) * (0.6f + Math.random().toFloat() * 0.4f)
        val rawY = cos(internalTime * 1.3f) * (0.6f + Math.random().toFloat() * 0.4f)
        val rawRot = sin(internalTime * 0.7f) * (0.6f + Math.random().toFloat() * 0.4f)

        return ShakeResult(
            rawX * kick * shakeXAmount,
            rawY * kick * kick * shakeYAmount,
            rawRot * kick * shakeRotationAmount
        )
    }
}
