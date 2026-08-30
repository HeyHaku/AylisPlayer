package com.aylis.comp.visual.core.modifiers

import com.aylis.comp.visual.core.Graphic.RenderState
import kotlin.math.abs
import kotlin.math.sin

class TimeModulator {

    companion object {
        const val MODE_CONSTANT = "Constant"
        const val MODE_TOTAL_TIME = "Total Time"
        const val MODE_BACK_AND_FORTH = "Back and Forth"
        const val MODE_TRACK_POSITION = "Track Position"

        val MODES = arrayOf(MODE_CONSTANT, MODE_TOTAL_TIME, MODE_BACK_AND_FORTH, MODE_TRACK_POSITION)
    }

    var currentMode: String = MODE_CONSTANT
    var multiplier: Float = 1.0f
    private var internalTime: Float = 0.0f

    fun calculate(renderState: RenderState): Float {

        val provider = renderState.res.meter?.audioDataProvider
        val isAudioActive = provider != null && renderState.res.visualizationData != null

        when (currentMode) {
            MODE_CONSTANT -> {
                return multiplier
            }
            MODE_TOTAL_TIME -> {
                if (isAudioActive) {
                    internalTime += renderState.frameTimeF
                }
                return internalTime * multiplier
            }
            MODE_BACK_AND_FORTH -> {
                if (isAudioActive) {
                    internalTime += renderState.frameTimeF * multiplier
                }

                return abs(sin(internalTime.toDouble())).toFloat()
            }
            MODE_TRACK_POSITION -> {
                if (isAudioActive) {

                    val posVec = renderState.res.meter.measureVec2f("trackPosition")
                    return posVec.x * multiplier
                }
                return 0.0f
            }
        }
        return multiplier
    }

    fun onReadCustomization(outData: com.aylis.comp.visual.core.Elements.Element.CustomizationData, prefix: String, group: String) {
        outData.putPropertyString(prefix + "Mode", currentMode, "sel " + MODES.joinToString(" "), group)
        outData.putPropertyFloat(prefix + "Multiplier", multiplier, "f -10.0 10.0", group)
    }

    fun onApplyCustomization(inData: com.aylis.comp.visual.core.Elements.Element.CustomizationData, prefix: String) {
        currentMode = inData.getPropertyString(prefix + "Mode", currentMode)
        multiplier = inData.getPropertyFloat(prefix + "Multiplier", multiplier)
    }
}