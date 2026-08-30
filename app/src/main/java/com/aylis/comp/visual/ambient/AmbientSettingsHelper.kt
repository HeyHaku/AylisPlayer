package com.aylis.comp.visual.ambient

import android.content.Context
import android.content.SharedPreferences

data class AmbientPoint(var nx: Float, var ny: Float)

class AmbientSettingsHelper(context: Context, val profile: String) {

    private val prefs: SharedPreferences = context.getSharedPreferences("ambient_settings", Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_POINTS_STRING = "0.0,0.0;1.0,0.0;0.0,1.0;1.0,1.0;0.5,0.5"
    }

    var isEnabled: Boolean
        get() = prefs.getBoolean("ambient_${profile}_enabled", true)
        set(value) = prefs.edit().putBoolean("ambient_${profile}_enabled", value).apply()

    var brightness: Float
        get() = prefs.getFloat("ambient_${profile}_brightness", 0.7f)
        set(value) = prefs.edit().putFloat("ambient_${profile}_brightness", value).apply()

    var animationDuration: Long
        get() = prefs.getLong("ambient_${profile}_duration", 600L)
        set(value) = prefs.edit().putLong("ambient_${profile}_duration", value).apply()

    fun getPoints(): List<AmbientPoint> {
        val pointsStr = prefs.getString("ambient_${profile}_points", DEFAULT_POINTS_STRING) ?: DEFAULT_POINTS_STRING
        if (pointsStr.isEmpty()) return emptyList()
        
        return pointsStr.split(";").mapNotNull {
            val parts = it.split(",")
            if (parts.size == 2) {
                AmbientPoint(parts[0].toFloatOrNull() ?: 0f, parts[1].toFloatOrNull() ?: 0f)
            } else null
        }
    }

    fun savePoints(points: List<AmbientPoint>) {
        val pointsStr = points.joinToString(";") { "${it.nx},${it.ny}" }
        prefs.edit().putString("ambient_${profile}_points", pointsStr).apply()
    }

    fun resetToDefault() {
        prefs.edit().remove("ambient_${profile}_points").apply()
    }
}
