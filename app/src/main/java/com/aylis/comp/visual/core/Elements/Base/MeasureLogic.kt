package com.aylis.comp.visual.core.Elements.Base

import android.graphics.PointF
import com.aylis.Common.Interpolate
import com.aylis.comp.visual.design.HandheldMotion
import com.aylis.comp.visual.core.Elements.Base.shakes.BeatRandomShake
import com.aylis.comp.visual.core.Elements.Base.shakes.BeatCamShakeMore
import com.aylis.comp.visual.core.Elements.Base.shakes.BeatCamShakeLess
import com.aylis.comp.visual.core.Elements.Base.shakes.ConstantCamShake
import com.aylis.comp.visual.core.Elements.Base.MeasureAttributes.*

class MeasureLogic(
    private val handheldMotionSmooth: HandheldMotion,
    private val handheldMotionLotOfShake: HandheldMotion,
    private val shakePointSmooth: PointF
) {
    private var trackPosition: Float = 0f
    private var trackDuration: Float = 1f
    private var isPlaying: Boolean = true
    private var totalFrameTimeWhenPlaying: Float = 0f
    private var totalFrameTime: Float = 0f
    private var accumulatedTimeAndBeat: Float = 0f
    private var accumulatedBeat: Float = 0f
    private var useFixedDeltaTime: Boolean = false

    private var lastTimeNano = System.nanoTime()
    private var maxRmsInFrame = 0.0f
    
    // Переиспользуемые объекты для оптимизации GC (Android)
    private val reusableOutResult = PointF()
    private val measureState = MeasureState()

    // Реестр всех эффектов для предотвращения разрастания класса (паттерн Стратегия)
    private val processors = HashMap<String, IMeasureAttribute>()
    
    // Специальный интерфейс для адаптации IShakeCore в IMeasureAttribute
    private inner class ShakeAdapter(val core: com.aylis.comp.visual.core.Elements.Base.shakes.IShakeCore) : IMeasureAttribute {
        override fun process(state: MeasureState, argX: Float, argY: Float, lastMeasured: PointF, outResult: PointF) {
            core.process(state.totalTime, state.isPlaying, state.rms, argX, argY, lastMeasured, outResult)
        }
    }

    init {
        // Регистрация атрибутов
        processors[MeasureDefs.beat] = BeatAttribute()
        processors[MeasureDefs.totalTime] = TotalTimeAttribute()
        processors[MeasureDefs.TotalTimeAndBack] = TotalTimeAndBackAttribute()
        processors[MeasureDefs.totalTimeBackward] = TotalTimeBackwardAttribute()
        processors[MeasureDefs.totalTimeWhenPlaying] = TotalTimeWhenPlayingAttribute()
        processors[MeasureDefs.totalTimeAndBeat] = TotalTimeAndBeatAttribute()
        processors[MeasureDefs.trackPosition] = TrackPositionAttribute()

        // Регистрация шейков через адаптер
        processors[MeasureDefs.beatRandomShake] = ShakeAdapter(BeatRandomShake(shakePointSmooth))
        processors[MeasureDefs.constantCamShakeLess] = ShakeAdapter(ConstantCamShake(handheldMotionSmooth, false))
        processors[MeasureDefs.constantCamShakeMore] = ShakeAdapter(ConstantCamShake(handheldMotionLotOfShake, false))
        processors[MeasureDefs.constantCamShakeRotLess] = ShakeAdapter(ConstantCamShake(handheldMotionSmooth, true))
        processors[MeasureDefs.constantCamShakeRotMore] = ShakeAdapter(ConstantCamShake(handheldMotionLotOfShake, true))
        processors[MeasureDefs.beatCamShakeLess] = ShakeAdapter(BeatCamShakeLess(handheldMotionSmooth, false))
        processors[MeasureDefs.beatCamShakeMore] = ShakeAdapter(BeatCamShakeMore(handheldMotionLotOfShake, false))
    }

    fun updatePlaybackState(position: Float, duration: Float, playing: Boolean) {
        trackPosition = position
        trackDuration = duration
        isPlaying = playing
    }

    private fun updateTime(rms: Float) {
        if (useFixedDeltaTime) return
        maxRmsInFrame = maxOf(maxRmsInFrame, rms)
        
        val now = System.nanoTime()
        val dt = (now - lastTimeNano) / 1_000_000_000f // Время в секундах
        if (dt > 0.008f) { // ~120fps порог, чтобы не плодить микро-обновления
            updateTimeWithDt(dt, maxRmsInFrame)
            lastTimeNano = now
            maxRmsInFrame = 0.0f // reset for next frame
        }
    }

    fun updateTimeWithDt(dt: Float, rms: Float) {
        totalFrameTime += dt
        if (isPlaying) {
            totalFrameTimeWhenPlaying += dt
            
            // Защита от слишком высоких пиков RMS на экспорте
            // rms обычно от 0.0 до 1.0 (редко чуть больше), но на экспорте (из-за отсутствия сглаживания окна) 
            // может проскакивать неадекватный peak. Ограничим его для стабильности.
            val clampedRms = minOf(rms, 1.5f)
            
            // Даем пинок при басах (адекватный множитель, без мракобесия на экспорте)
            val beatAcceleration = clampedRms * clampedRms * dt * 10.0f
            accumulatedBeat += beatAcceleration 
            accumulatedTimeAndBeat += dt + beatAcceleration
        }
    }

    fun setUseFixedDeltaTime(useFixed: Boolean) {
        this.useFixedDeltaTime = useFixed
        if (!useFixed) {
            lastTimeNano = System.nanoTime()
        }
    }

    fun process(
        measure: String?,
        argPt: PointF?,
        lastMeasured: PointF?,
        frameDataRmsValue: Float?
    ): PointF {
        val rms = frameDataRmsValue ?: 0.0f
        updateTime(rms)

        val argX = argPt?.x ?: 1.0f
        val argY = argPt?.y ?: 1.0f

        if (measure.isNullOrEmpty() || measure == MeasureDefs.Nothing) {
            reusableOutResult.set(0.0f, 0.0f)
            return reusableOutResult
        }

        if (measure == MeasureDefs.Constant) {
            reusableOutResult.set(argX, argY)
            return reusableOutResult
        }

        val safeLastMeasured = lastMeasured ?: PointF(0f, 0f)

        measureState.totalTime = totalFrameTime
        measureState.totalTimeWhenPlaying = totalFrameTimeWhenPlaying
        measureState.accumulatedTimeAndBeat = accumulatedTimeAndBeat
        measureState.accumulatedBeat = accumulatedBeat
        measureState.trackPosition = trackPosition
        measureState.trackDuration = trackDuration
        measureState.isPlaying = isPlaying
        measureState.rms = rms
        
        measureState.frameTimeF = 0.016f

        val processor = processors[measure]
        if (processor != null) {
            processor.process(measureState, argX, argY, safeLastMeasured, reusableOutResult)
        } else {
            reusableOutResult.set(argX, argY)
        }

        return reusableOutResult
    }
}
