package com.aylis.comp.visual.core.modifiers

import com.aylis.Common.Vec2f
import com.aylis.comp.visual.core.Elements.Element
import com.aylis.comp.visual.core.Graphic.RenderState
import com.aylis.comp.visual.core.audio.GlobalAudioTrigger

class ElementAnimator {

    var shakeAmount = Vec2f(0.0f, 0.0f)
    var shakeRotationAmount = 0.0f
    var shakeSpeed = 1.0f
    var bassSensitivity = 0.1f
    var pulseAmount = 0.1f

    private val shakeModifier = ShakeModifier()
    val generalTimeModulator = TimeModulator()

    var shakeOffset = Vec2f(0.0f, 0.0f)
        private set
    var shakeOffsetRotation = 0.0f
        private set
    var shakeOffsetScale = 1.0f
        private set
    var animatedTimeSignal = 0.0f
        private set

    fun onApplyCustomization(customizationData: Element.CustomizationData) {
        generalTimeModulator.onApplyCustomization(customizationData, "generalTime")
        shakeAmount = customizationData.getPropertyVec2f("shakeAmount", Vec2f(
            customizationData.getPropertyFloat("shakeXAmount", 0.0f),
            customizationData.getPropertyFloat("shakeYAmount", 0.0f)
        ))
        shakeRotationAmount = customizationData.getPropertyFloat("shakeRotationAmount", 0.0f)
        shakeSpeed = customizationData.getPropertyFloat("shakeSpeed", 0.05f)
        bassSensitivity = customizationData.getPropertyFloat("bassSensitivity", 1.0f)
        pulseAmount = customizationData.getPropertyFloat("pulseAmount", 0.0f)
    }

    fun onReadCustomization(outCustomizationData: Element.CustomizationData) {
        outCustomizationData.putPropertyVec2f("shakeAmount", shakeAmount, "f2 0.0 100.0", "6_modifier")
        outCustomizationData.putPropertyFloat("shakeRotationAmount", shakeRotationAmount, "f 0.0 1.0", "6_modifier")
        outCustomizationData.putPropertyFloat("shakeSpeed", shakeSpeed, "f 0.0 1.0", "6_modifier")
        outCustomizationData.putPropertyFloat("bassSensitivity", bassSensitivity, "f 0.1 5.0", "6_modifier")
        outCustomizationData.putPropertyFloat("pulseAmount", pulseAmount, "f 0.0 1.0", "6_modifier")
    }

    fun onRender(renderData: RenderState) {
        shakeModifier.shakeXAmount = this.shakeAmount.x
        shakeModifier.shakeYAmount = this.shakeAmount.y
        shakeModifier.shakeRotationAmount = this.shakeRotationAmount
        shakeModifier.shakeSpeed = this.shakeSpeed

        val isAudioActive = renderData.res.visualizationData != null && GlobalAudioTrigger.isPlaying

        var finalSignal = 0.0f

        if (isAudioActive) {
            val provider = renderData.res.meter.audioDataProvider
            if (provider != null) {
                val frequencies = provider.frameValues
                if (frequencies != null && frequencies.size > 4) {
                    val bassSum = frequencies[0] + frequencies[1] + frequencies[2] + frequencies[3]
                    finalSignal = (bassSum / 4.0f) * this.bassSensitivity
                    if (finalSignal > 1.0f) finalSignal = 1.0f
                    if (finalSignal < 0.0f) finalSignal = 0.0f
                }
            }
        }

        val shake = shakeModifier.update(finalSignal, isAudioActive)

        shakeOffset = Vec2f(shake.x, shake.y)
        shakeOffsetRotation = shake.rotation
        shakeOffsetScale = 1.0f + (finalSignal * this.pulseAmount)
        animatedTimeSignal = generalTimeModulator.calculate(renderData)
    }
}
