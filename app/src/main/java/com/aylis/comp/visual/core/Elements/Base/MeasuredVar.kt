package com.aylis.comp.visual.core.Elements.Base

import com.aylis.Common.Vec2f
import com.aylis.comp.visual.core.Elements.Element.CustomizationData
import com.aylis.comp.visual.core.Elements.Meter
import java.util.Locale

class MeasuredVar {
    var measure: String = MeasureDefs.Nothing
        private set
    val measureArg = Vec2f(0f, 0f)
    private var lastMeasured = Vec2f(0f, 0f)

    constructor()

    constructor(measureName: String, argX: Float, argY: Float) {
        this.measure = measureName
        this.measureArg.x = argX
        this.measureArg.y = argY
    }

    constructor(measure: String, arg: Float) {
        this.measure = measure
        this.measureArg.x = arg
        this.measureArg.y = arg
        validate()
    }

    constructor(other: MeasuredVar) {
        this.measure = other.measure
        this.measureArg.x = other.measureArg.x
        this.measureArg.y = other.measureArg.y
        this.lastMeasured.x = other.lastMeasured.x
        this.lastMeasured.y = other.lastMeasured.y
        validate()
    }

    private fun validate() {
        if (measure.isEmpty()) {
            measure = MeasureDefs.Nothing
        }
    }

    fun clone(): MeasuredVar {
        return MeasuredVar(this)
    }

    override fun toString(): String {
        return String.format(Locale.US, "%s %f %f", measure, measureArg.x, measureArg.y)
    }

    fun toString(sb: StringBuilder) {
        sb.append(measure).append(" ").append(measureArg.x).append(" ").append(measureArg.y)
    }

    fun toDisplayString1d(sb: StringBuilder, onlyValues: Boolean) {
        val isBUsed = MeasureDefs.getHintArgBisUsedFor1d(measure)
        if (measure != MeasureDefs.Constant && measure != MeasureDefs.Nothing) {
            sb.append(measure).append("(").append(String.format(Locale.US, "%.3f", measureArg.x))
            if (isBUsed) sb.append(" ").append(String.format(Locale.US, "%.3f", measureArg.y))
            sb.append(")")
            return
        }
        if (onlyValues) {
            sb.append(String.format(Locale.US, "%.3f", measureArg.x))
            if (isBUsed) sb.append(" ").append(String.format(Locale.US, "%.3f", measureArg.y))
            return
        }
        sb.append("(").append(String.format(Locale.US, "%.3f", measureArg.x))
        if (isBUsed) sb.append(" ").append(String.format(Locale.US, "%.3f", measureArg.y))
        sb.append(")")
    }

    fun toDisplayString(sb: StringBuilder, onlyValues: Boolean) {
        if (measure != MeasureDefs.Constant && measure != MeasureDefs.Nothing) {
            sb.append(measure).append("(").append(String.format(Locale.US, "%.3f", measureArg.x))
            sb.append(" ").append(String.format(Locale.US, "%.3f", measureArg.y)).append(")")
            return
        }
        if (onlyValues) {
            sb.append(String.format(Locale.US, "%.3f", measureArg.x)).append(" ")
                .append(String.format(Locale.US, "%.3f", measureArg.y))
        } else {
            sb.append("(").append(String.format(Locale.US, "%.3f", measureArg.x)).append(" ")
                .append(String.format(Locale.US, "%.3f", measureArg.y)).append(")")
        }
    }

    fun getMeasureArgF(): Float {
        return measureArg.x
    }

    fun getMeasureArgVec2f(): Vec2f {
        return measureArg
    }

    @JvmOverloads
    fun measure(meter: Meter, def: Float = 0f): Float {
        val result = meter.measureVec2f(measure, measureArg, lastMeasured)
        lastMeasured = result
        return result.x
    }

    fun measureVec2f(meter: Meter): Vec2f {
        val result = meter.measureVec2f(measure, measureArg, lastMeasured)
        lastMeasured = result
        return result
    }

    fun onApplyCustomization(
        customizationData: CustomizationData?,
        name: String,
        defX: Float,
        defY: Float
    ) {
        if (customizationData == null) return
        val parsed = customizationData.getPropertyMeasuredVar(name, null)
        if (parsed != null) {
            measure = parsed.measure
            measureArg.x = parsed.measureArg.x
            measureArg.y = parsed.measureArg.y
            validate()
            return
        }
        val child = customizationData.getChild(name)
        val mVal = child.getChildTypeValue()
        measure = if (mVal.isNullOrEmpty()) MeasureDefs.Nothing else mVal
        measureArg.x = child.getPropertyFloat("A", defX)
        measureArg.y = child.getPropertyFloat("B", defY)
        validate()
    }

    fun onApplyCustomization(customizationData: CustomizationData, prefix: String, defaultArg: Float) {
        onApplyCustomization(customizationData, prefix, defaultArg, defaultArg)
    }

    fun onReadCustomizationPos(customizationData: CustomizationData, name: String) {
        customizationData.putPropertyMeasuredVar(name, this, "0_general", 0f, 2f)
    }

    fun onReadCustomizationScale(customizationData: CustomizationData, name: String) {
        customizationData.putPropertyMeasuredVar(name, this, "0_general", -1f, 1f)
    }

    fun onReadCustomization1D(customizationData: CustomizationData, name: String) {
        customizationData.putPropertyMeasuredVar(name, this, "0_general", 0f, 1f)
    }

    companion object {
        fun asNothing(): MeasuredVar {
            return MeasuredVar()
        }

        fun fromString(str: String?, fallback: MeasuredVar?): MeasuredVar? {
            if (str == null || str.trim().isEmpty()) return fallback?.clone()
            try {
                val parts = str.trim().split(Regex("\\s+"))
                if (parts.size >= 3) {
                    val m = parts[0]
                    val x = parts[1].toFloat()
                    val y = parts[2].toFloat()
                    return MeasuredVar(m, x, y)
                } else if (parts.size == 2) {
                    // Sometimes format is "Measure 1.0"
                    val m = parts[0]
                    val x = parts[1].toFloat()
                    return MeasuredVar(m, x, x)
                } else if (parts.size == 1) {
                    val v = parts[0].toFloat()
                    return MeasuredVar(MeasureDefs.Constant, v, v)
                }
            } catch (e: Exception) {
            }
            return fallback?.clone()
        }
        
        fun FromString(str: String?, defaultVar: MeasuredVar?): MeasuredVar? {
            return fromString(str, defaultVar)
        }

        fun AsNothing(): MeasuredVar {
            return MeasuredVar(MeasureDefs.Nothing, 0.5f, 0.5f)
        }
    }
}
