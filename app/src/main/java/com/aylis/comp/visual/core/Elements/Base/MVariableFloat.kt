package com.aylis.comp.visual.core.Elements.Base

import android.text.TextUtils
import com.aylis.Common.Vec2f

class MVariableFloat {
    val measures: MutableList<MeasuredVar> = ArrayList()
    private var tmpResult = 0f

    constructor()

    constructor(measuredVar: MeasuredVar) {
        measures.add(measuredVar)
    }

    constructor(measuresList: List<MeasuredVar>) {
        measures.addAll(measuresList)
    }

    constructor(other: MVariableFloat) {
        for (m in other.measures) {
            measures.add(MeasuredVar(m))
        }
    }

    override fun toString(): String {
        if (measures.isEmpty()) return ""
        val sb = StringBuilder()
        for (i in measures.indices) {
            measures[i].toString(sb)
            if (i < measures.size - 1) {
                sb.append(";")
            }
        }
        return sb.toString()
    }

    fun getValueAsFloat(meter: com.aylis.comp.visual.core.Elements.Meter, def: Float = 0f): Float {
        tmpResult = 0f
        for (m in measures) {
            tmpResult += m.measure(meter)
        }
        return tmpResult
    }
    
    // For measureVec2f style calculations if needed
    fun getValueAsVec2f(meter: com.aylis.comp.visual.core.Elements.Meter, vec2f: Vec2f): Vec2f {
        vec2f.x = 0f
        vec2f.y = 0f
        for (m in measures) {
            val res = m.measureVec2f(meter)
            vec2f.x += res.x
            vec2f.y += res.y
        }
        return vec2f
    }

    companion object {
        fun asNothing(): MVariableFloat {
            return MVariableFloat(MeasuredVar.asNothing())
        }

        fun createConstantFloat(f: Float): MVariableFloat {
            return MVariableFloat(MeasuredVar(MeasureDefs.Constant, f, 0f))
        }

        fun createConstantVec2f(f: Float, f2: Float): MVariableFloat {
            return MVariableFloat(MeasuredVar(MeasureDefs.Constant, f, f2))
        }

        fun fromString(str: String?, fallback: MVariableFloat?): MVariableFloat {
            if (str == null) return fallback?.let { MVariableFloat(it) } ?: MVariableFloat()
            val split = TextUtils.split(str, ";")
            val list = ArrayList<MeasuredVar>()
            for (s in split) {
                val m = MeasuredVar.fromString(s, null)
                if (m != null) {
                    list.add(m)
                }
            }
            return if (list.isEmpty()) {
                fallback?.let { MVariableFloat(it) } ?: MVariableFloat()
            } else {
                MVariableFloat(list)
            }
        }
    }
}
