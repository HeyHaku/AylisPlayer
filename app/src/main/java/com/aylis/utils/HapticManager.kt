package com.aylis.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

import android.view.HapticFeedbackConstants
import android.view.View

object HapticManager {

    private const val PREF_HAPTIC_INTENSITY = "pref_haptic_intensity"
    private const val PREF_HAPTIC_TYPE = "pref_haptic_type"
    var hapticIntensity = 0 // 0 to 5
    var hapticType = "knock" // "knock" or "soft"
    var globalVibration = false

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
        if (!prefs.contains(PREF_HAPTIC_INTENSITY)) {
            val oldPref = prefs.getString("pref_hapticFeedback", "1")
            val newIntensity = when (oldPref) {
                "2" -> 5
                "1" -> 3
                else -> 0
            }
            prefs.edit().putInt(PREF_HAPTIC_INTENSITY, newIntensity).apply()
            hapticIntensity = newIntensity
        } else {
            hapticIntensity = prefs.getInt(PREF_HAPTIC_INTENSITY, 0).coerceAtMost(5)
            // Save the coerced value back so the settings screen doesn't crash if it reads it directly
            if (hapticIntensity != prefs.getInt(PREF_HAPTIC_INTENSITY, 0)) {
                prefs.edit().putInt(PREF_HAPTIC_INTENSITY, hapticIntensity).apply()
            }
        }
        hapticType = prefs.getString(PREF_HAPTIC_TYPE, "knock") ?: "knock"
        globalVibration = prefs.getBoolean("pref_haptic_global", false)
    }

    fun setIntensity(level: Int) {
        hapticIntensity = level
    }

    fun setType(type: String) {
        hapticType = type
    }

    fun performClick(view: View) {
        vibrateCustom(view, 1.0f)
    }

    fun performTick(view: View) {
        vibrateCustom(view, 0.5f)
    }

    fun performLightTick(view: View) {
        vibrateCustom(view, 0.2f)
    }

    fun performLongPress(view: View) {
        vibrateCustom(view, 1.5f)
    }

    // Вызывается только из MainActivity для глобальных касаний
    fun performGlobalTick(view: View) {
        vibrateCustom(view, 0.2f, true)
    }

    fun vibrateCustom(view: View, scale: Float, isGlobalEvent: Boolean = false) {
        if (hapticIntensity == 0) return

        // Если включена глобальная вибрация, игнорируем все локальные вызовы, 
        // чтобы не было "двойного удара"
        if (globalVibration && !isGlobalEvent) return

        // Теперь hapticIntensity от 1 до 5
        // scale может быть 1.0f (click), 0.5f (tick), 0.2f (light tick)
        
        // Для стука используем 5 ступеней (от 1 до 5)
        val scaledKnockStep = Math.round(hapticIntensity * scale).toInt().coerceIn(1, 5)

        if (hapticType == "knock") {
            val constant = when (scaledKnockStep) {
                1 -> HapticFeedbackConstants.CLOCK_TICK
                2 -> HapticFeedbackConstants.KEYBOARD_TAP
                3 -> HapticFeedbackConstants.VIRTUAL_KEY
                4 -> HapticFeedbackConstants.LONG_PRESS
                else -> -1 // 5 = Максимальная кастомная вибрация
            }

            if (constant != -1) {
                view.performHapticFeedback(constant)
                return
            }
        }

        // Для мягкой вибрации переводим шкалу 1-5 в 2-10 (чтобы сохранить логику)
        // 1 = 2, 2 = 4, 3 = 6, 4 = 8, 5 = 10
        val mappedSoft = hapticIntensity * 2
        val scaledSoft = (mappedSoft * scale).toInt().coerceIn(1, 10)

        val context = view.context
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val amplitude = (scaledSoft * 25.5).toInt().coerceIn(1, 255)
        
        val duration = if (hapticType == "knock") {
            40L // Для 5-й ступени стука
        } else {
            15L + scaledSoft * 3L // Мягкое нарастающее жужжание (стало слабее: от 18мс до 45мс)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (vibrator.hasAmplitudeControl()) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }
}

