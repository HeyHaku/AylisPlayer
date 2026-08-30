package com.aylis.comp.visual.core.Elements.Base

object MeasureDefs {
    const val Constant = "Constant"
    const val beat = "Beat"
    const val totalTime = "TotalTime"
    const val totalTimeBackward = "TotalTimeBackward"
    const val totalTimeWhenPlaying = "TotalTimeWhenPlaying"
    const val totalTimeAndBeat = "TotalTimeAndBeat"
    const val trackPosition = "TrackPosition"
    const val beatRandomShake = "BeatRandomShake"
    
    const val beatCamShakeMore = "BeatCamShakeMore"
    const val beatCamShakeLess = "BeatCamShakeLess"
    const val constantCamShakeMore = "ConstantShakeMore"
    const val constantCamShakeLess = "ConstantShake"
    const val constantCamShakeRotMore = "ConstantShakeRotMore"
    const val constantCamShakeRotLess = "ConstantShakeRotLess"
    
    const val TotalTimeAndBack = "TotalTimeAndBack"
    const val Nothing = "Nothing"
    
    // Arrays for customizer UI (dropdown options)
    val measures1d = arrayOf(
        Nothing, Constant, beat, totalTime, totalTimeBackward, totalTimeWhenPlaying, 
        totalTimeAndBeat, trackPosition, beatRandomShake, beatCamShakeMore, beatCamShakeLess,
        constantCamShakeRotMore, constantCamShakeRotLess, TotalTimeAndBack
    )
    
    val measures2dMVar = arrayOf(
        Constant, beat, totalTime, totalTimeBackward, totalTimeWhenPlaying, 
        totalTimeAndBeat, trackPosition, beatRandomShake, beatCamShakeMore, beatCamShakeLess,
        constantCamShakeMore, constantCamShakeLess, TotalTimeAndBack
    )
    
    val measures1dMVar = arrayOf(
        Constant, beat, totalTime, totalTimeBackward, totalTimeWhenPlaying, 
        totalTimeAndBeat, trackPosition, beatRandomShake, beatCamShakeMore, beatCamShakeLess,
        constantCamShakeRotMore, constantCamShakeRotLess, TotalTimeAndBack
    )
    
    class MeasureDef(
        val name: String,
        val argAHint: String,
        val argBHint: String,
        var isBUsedFor1d: Boolean = false
    )
    
    val measures = arrayOf(
        MeasureDef(Constant, "X", "Y"),
        MeasureDef(Nothing, "X", "Y"),
        MeasureDef(beat, "X Amount", "Y Amount"),
        MeasureDef(totalTime, "X Speed", "Y Speed"),
        MeasureDef(totalTimeBackward, "X Speed", "Y Speed"),
        MeasureDef(totalTimeWhenPlaying, "X Speed", "Y Speed"),
        MeasureDef(totalTimeAndBeat, "Speed", "Beat Amount", true),
        MeasureDef(trackPosition, "Amount", "Amount"),
        MeasureDef(beatRandomShake, "X Amount", "Y Amount", true),
        MeasureDef(beatCamShakeLess, "X Amount", "Y Amount", true),
        MeasureDef(beatCamShakeMore, "X Amount", "Y Amount", true),
        MeasureDef(constantCamShakeLess, "X Amount", "Y Amount", true),
        MeasureDef(constantCamShakeMore, "X Amount", "Y Amount", true),
        MeasureDef(constantCamShakeRotMore, "X Amount", "Y Amount", true),
        MeasureDef(constantCamShakeRotLess, "X Amount", "Y Amount", true),
        MeasureDef(TotalTimeAndBack, "X Speed", "Y Speed")
    )
    
    fun getMeasureDefByName(str: String?): MeasureDef? {
        if (str == null) return null
        return measures.find { it.name == str }
    }
    
    fun getHintArgA(str: String?): String? {
        return getMeasureDefByName(str)?.argAHint
    }
    
    fun getHintArgB(str: String?): String? {
        return getMeasureDefByName(str)?.argBHint
    }
    
    fun getHintArgBisUsedFor1d(str: String?): Boolean {
        return getMeasureDefByName(str)?.isBUsedFor1d ?: false
    }
}
