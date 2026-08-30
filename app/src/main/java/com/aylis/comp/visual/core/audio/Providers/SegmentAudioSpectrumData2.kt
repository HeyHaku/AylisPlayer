package com.aylis.comp.visual.core.audio.Providers

import com.NAudio.FastFourierTransform
import com.aylis.Common.ISimpleListFloat
import com.aylis.comp.visual.core.Elements.Element
import com.aylis.comp.visual.core.Elements.IFrameDataProvider
import com.aylis.comp.visual.core.Elements.bars.AudioBars.ISegmentDataProvider
import com.aylis.comp.visual.core.InternalVisualizationDataProvider
import com.aylis.comp.visual.core.playback.AudioFrameData
import com.aylis.comp.visual.core.Dsp.DspWindows
import com.aylis.comp.visual.core.Dsp.DspCurves
import com.aylis.comp.visual.core.Dsp.SignalFilter1d
import kotlin.math.hypot
import kotlin.math.log
import kotlin.math.pow
import kotlin.math.abs

class SegmentAudioSpectrumData2 : ISegmentDataProvider, IFrameDataProvider {

    companion object {
        const val typeName = "Spectrum2"
    }

    // --- Оригинальные настройки Avee Player ---
    private var loFreq = 20.0f
    private var hiFreq = 18000.0f
    private var hzLinearFactors = 0.0f
    
    private var smoothFactor = 0.8f // For final bars
    private var smoothFactor2 = 1.0f // For raw FFT bins
    private var outputMultiplier = 1.0f
    private var aweightFft = 0.2f
    private var filterRadius = 1
    private var filterStrength = 1.0f

    // --- Настройки позиционирования OpenPlayer ---
    private var waveShift = 0.0f
    private var mirrorSample = false
    private var repeatSamples = 1
    private var starAndEndGap = 0

    // --- Внутренние переменные ---
    private var visData: AudioFrameData? = null
    private var dataCount = 1024
    private var barsCount = 64
    
    private var barValues = FloatArray(0)
    private var barSmoothValues = FloatArray(0)
    private var processedBarValues = FloatArray(0)
    
    private var barFreq = FloatArray(0)
    private var barFreqNext = FloatArray(0)
    
    private var rangeLoSmooth = 0.0f
    private var rangeHiSmooth = 1000.0f
    private var rangeSmoothFactor = 0.1f
    private var rangeTarget = 1000.0f
    
    private var fftMag = FloatArray(0)
    private var fftWindowMultipliers = FloatArray(0)
    
    private val fftFilter = SignalFilter1d().apply { createHighPass(1, 1.0f) }
    private val frameValuesAccumulator = com.aylis.comp.visual.core.Elements.Base.FrameValuesAccumulator()
    private var currentRms = 0.0f

    init {
        setSampleOutCount(64)
    }

    fun setSampleOutCount(sampleOutCount: Int) {
        this.barsCount = Math.max(1, sampleOutCount)
        updateBandsFreq()
    }

    private fun updateBandsFreq() {
        if (barFreq.size != barsCount) barFreq = FloatArray(barsCount)
        if (barFreqNext.size != barsCount) barFreqNext = FloatArray(barsCount)
        
        val freqScaleOff = 800.0f
        val stepLog = (log(((hiFreq + freqScaleOff) / (loFreq + freqScaleOff)).toDouble(), Math.E) / barsCount).toFloat()
        val stepMulLog = Math.E.pow(stepLog.toDouble()).toFloat()

        var currentFreqLog = loFreq + freqScaleOff
        val stepLin = (hiFreq - loFreq) / barsCount

        var prevFreq = loFreq
        for (i in 0 until barsCount) {
            val freqLog = currentFreqLog - freqScaleOff
            val freqLin = loFreq + (i * stepLin)
            val dynamicLinearFactor = hzLinearFactors * (i.toFloat() / barsCount)
            val targetFreq = (freqLog * (1f - dynamicLinearFactor)) + (freqLin * dynamicLinearFactor)

            barFreqNext[i] = prevFreq
            barFreq[i] = targetFreq
            
            prevFreq = targetFreq
            currentFreqLog *= stepMulLog
        }
    }

    override fun process(visualisationData: InternalVisualizationDataProvider) {
        visData = AudioFrameData.createReuse(visData, dataCount)
        val visDataResult = visualisationData.onRequestSoundVisualizationData(visData)

        if (visDataResult == null) {
            currentRms = 0.0f
            if (barSmoothValues.isNotEmpty()) java.util.Arrays.fill(barSmoothValues, 0.0f)
            if (processedBarValues.isNotEmpty()) java.util.Arrays.fill(processedBarValues, 0.0f)
            frameValuesAccumulator.addFrame(barSmoothValues)
            return
        }

        val buf = visDataResult.pcmBuffer
        val sampleRate = visDataResult.sampleRate
        val fftSize = buf.size

        // 1. Hann Window (Анти-дребезг)
        if (fftWindowMultipliers.size != fftSize) {
            fftWindowMultipliers = FloatArray(fftSize)
            for (i in 0 until fftSize) {
                fftWindowMultipliers[i] = DspWindows.hannWindow(i, fftSize).toFloat() * 0.00390625f
            }
        }

        val fftResult = Array(fftSize) { i ->
            FastFourierTransform.Complex(buf[i].toFloat() * (1.0f / 256.0f) * fftWindowMultipliers[i], 0.0f)
        }
        FastFourierTransform.FFT(true, fftResult)
        
        val fftOutputSize = fftSize / 2

        if (barValues.size != barsCount) barValues = FloatArray(barsCount)
        if (barSmoothValues.size != barsCount) barSmoothValues = FloatArray(barsCount)
        if (fftMag.size != fftOutputSize) fftMag = FloatArray(fftOutputSize)

        // 2. HighPass Filter & A-Weighting (как в оригинальном Avee)
        // Для SignalFilter1d нужен доступ к массиву
        val fftResultAccess = object : com.aylis.Common.ISimpleListDouble {
            override fun size(): Int = fftOutputSize
            override fun get(i: Int): Double {
                if (i < 0 || i >= fftOutputSize) return 0.0
                return hypot(fftResult[i].re().toDouble(), fftResult[i].im().toDouble())
            }
        }

        for (i in 0 until fftOutputSize) {
            var fMax = Math.max(fftFilter.getSoftedClamped(i, fftResultAccess).toFloat(), 0.0f)
            
            if (aweightFft > 0.0f) {
                val fMyAWeight1000 = DspCurves.myAWeight1000(DspCurves.freqd(i.toDouble(), fftOutputSize.toDouble(), sampleRate.toDouble())).toFloat()
                fMax *= (1.0f - aweightFft) + (aweightFft * fMyAWeight1000 * fMyAWeight1000 * fMyAWeight1000)
            }
            
            // Временное сглаживание ДО биннинга
            fftMag[i] = (fftMag[i] * (1.0f - smoothFactor2)) + (fMax * smoothFactor2)
        }

        // 3. Распределение частот (Logarithmic vs Linear) - AVERAGE вместо MAX!
        val df = sampleRate.toFloat() / fftSize.toFloat()
        
        var minBar = 9999990.0f
        var maxBar = -9999990.0f
        
        for (i in 0 until barsCount) {
            val fAbs = ((Math.abs(barFreq[i] - barFreqNext[i]) / 50.0f) * 0.3f) + 0.7f
            val i5 = (barFreq[i] / df).toInt()
            val i6 = (barFreqNext[i] / df).toInt()
            
            val iMin = Math.min(i5, fftMag.size)
            
            if (iMin > i6) {
                // Усреднение бинов (если полоса широкая)
                var sum = 0.0f
                var count = 0
                for (j in i6 until iMin) {
                    if (j >= 0 && j < fftMag.size) {
                        sum += fftMag[j]
                        count++
                    }
                }
                val avgMag = if (count > 0) sum / count else 0.0f
                barValues[i] = avgMag * fAbs * outputMultiplier
            } else if (i6 < fftMag.size) {
                // Линейная интерполяция (если полоса очень узкая, например при barsCount = 512)
                val exactBin = barFreq[i] / df
                val fract = exactBin - i6
                
                val magA = if (i6 >= 0) fftMag[i6] else 0.0f
                val magB = if (i6 + 1 < fftMag.size) fftMag[i6 + 1] else magA
                
                val interpMag = (magA * (1.0f - fract)) + (magB * fract)
                barValues[i] = interpMag * fAbs * outputMultiplier
            } else {
                barValues[i] = 0.0f
            }
            
            if (minBar > barValues[i]) minBar = barValues[i]
            if (maxBar < barValues[i]) maxBar = barValues[i]
        }
        
        // Auto Gain Control (AGC) - вытягивает громкость
        val rangeMax = 1000.0f
        if (minBar < -rangeMax) minBar = -rangeMax
        if (minBar > rangeMax) minBar = rangeMax
        if (maxBar < -rangeMax) maxBar = -rangeMax
        if (maxBar > rangeMax) maxBar = rangeMax

        rangeLoSmooth = (rangeLoSmooth * (1.0f - rangeSmoothFactor)) + (minBar * rangeSmoothFactor)
        rangeHiSmooth = (rangeHiSmooth * (1.0f - rangeSmoothFactor)) + (maxBar * rangeSmoothFactor)

        var rangeMul = rangeHiSmooth - rangeLoSmooth
        if (rangeMul < 5.0f) rangeMul = 5.0f
        rangeMul = rangeTarget / rangeMul

        for (i in 0 until barsCount) {
            val valLocal = barValues[i] * rangeMul
            
            // Сглаживание баров (smoothFactor)
            val diff = valLocal - barSmoothValues[i]
            barSmoothValues[i] += diff * smoothFactor
            if (barSmoothValues[i] < 0.0f) barSmoothValues[i] = 0.0f
        }

        currentRms = barSmoothValues.average().toFloat()

        // 4. OpenPlayer Pipeline: Gap, Shift, Mirror, Repeat
        val baseCount = barsCount
        val withGapCount = baseCount + (starAndEndGap * 2)
        val withGap = FloatArray(withGapCount)
        System.arraycopy(barSmoothValues, 0, withGap, starAndEndGap, baseCount)

        val shiftedWithGap = FloatArray(withGapCount)
        val shiftElements = (waveShift * withGapCount).toInt()

        for (i in 0 until withGapCount) {
            var newIndex = (i + shiftElements) % withGapCount
            if (newIndex < 0) newIndex += withGapCount
            shiftedWithGap[newIndex] = withGap[i]
        }

        val mirroredCount = if (mirrorSample) (withGapCount * 2) else withGapCount
        val finalCount = mirroredCount * repeatSamples

        if (processedBarValues.size != finalCount) {
            processedBarValues = FloatArray(finalCount)
        }

        val mirrored: FloatArray
        if (mirrorSample) {
            mirrored = FloatArray(mirroredCount)
            for (i in 0 until withGapCount) {
                mirrored[withGapCount - 1 - i] = shiftedWithGap[i]
                mirrored[withGapCount + i] = shiftedWithGap[i]
            }
        } else {
            mirrored = shiftedWithGap
        }

        for (r in 0 until repeatSamples) {
            System.arraycopy(mirrored, 0, processedBarValues, r * mirroredCount, mirroredCount)
        }

        // 5. Отдаем в аккумулятор
        frameValuesAccumulator.addFrame(if (processedBarValues.isNotEmpty()) processedBarValues else barSmoothValues)
    }

    override fun getFrameValues(): FloatArray {
        return barSmoothValues
    }

    override fun getRms(): Float {
        return currentRms
    }

    override fun createFrameValuesAccessorList(
        reactionDelay: Int,
        reactionAccumulatedDelay: Int,
        softnessRadius: Int,
        barVals: ISimpleListFloat?
    ): ISimpleListFloat {
        return frameValuesAccumulator.createFrameValuesAccessorList(
            reactionDelay,
            reactionAccumulatedDelay,
            softnessRadius,
            barVals
        )
    }

    override fun onApplyCustomization(customizationData: Element.CustomizationData) {
        loFreq = customizationData.getPropertyFloat("01_loFreq", loFreq)
        hiFreq = customizationData.getPropertyFloat("02_hiFreq", hiFreq)
        hzLinearFactors = customizationData.getPropertyFloat("03_hzLinearFactors", hzLinearFactors)
        
        smoothFactor = customizationData.getPropertyFloat("04_smoothFactor", smoothFactor)
        smoothFactor2 = customizationData.getPropertyFloat("05_smoothFactor2 (FFT)", smoothFactor2)
        outputMultiplier = customizationData.getPropertyFloat("06_outputMultiplier", outputMultiplier)
        aweightFft = customizationData.getPropertyFloat("07_aWeight", aweightFft)
        
        filterRadius = customizationData.getPropertyInt("08_filterRadius", filterRadius)
        filterStrength = customizationData.getPropertyFloat("09_filterStrength", filterStrength)
        fftFilter.createHighPass(filterRadius, filterStrength)

        rangeTarget = customizationData.getPropertyFloat("15_agcTarget", rangeTarget)
        rangeSmoothFactor = customizationData.getPropertyFloat("16_agcSmoothFactor", rangeSmoothFactor)
        
        waveShift = customizationData.getPropertyFloat("10_waveShift", waveShift)
        mirrorSample = customizationData.getPropertyBool("11_mirrorSample", mirrorSample)
        repeatSamples = customizationData.getPropertyInt("12_repeatSamples", repeatSamples)
        starAndEndGap = customizationData.getPropertyInt("13_starAndEndGap", starAndEndGap)
        
        val newBarsCount = customizationData.getPropertyInt("14_barsCount", barsCount)
        if (newBarsCount != barsCount) {
            setSampleOutCount(newBarsCount)
        } else {
            updateBandsFreq()
        }
    }

    override fun onReadCustomization(outCustomizationData: Element.CustomizationData) {
        outCustomizationData.putPropertyFloat("01_loFreq", loFreq, "f 0.0 300.0", "Spectrum 1", "Min Frequency (Bass)")
        outCustomizationData.putPropertyFloat("02_hiFreq", hiFreq, "f 300.0 18000.0", "Spectrum 1", "Max Frequency (Treble)")
        outCustomizationData.putPropertyFloat("03_hzLinearFactors", hzLinearFactors, "f 0.0 1.0", "Spectrum 1", "Frequency Scale (Log/Lin)")
        
        outCustomizationData.putPropertyFloat("04_smoothFactor", smoothFactor, "f 0.0 1.0", "Spectrum 1", "Bar Smoothness")
        outCustomizationData.putPropertyFloat("05_smoothFactor2 (FFT)", smoothFactor2, "f 0.0 1.0", "Spectrum 1", "Signal Smoothing")
        outCustomizationData.putPropertyFloat("06_outputMultiplier", outputMultiplier, "f 0.1 5.0", "Spectrum 1", "Sensitivity")
        outCustomizationData.putPropertyFloat("07_aWeight", aweightFft, "f 0.0 1.0", "Spectrum 1", "Treble Boost (A-Weight)")
        
        outCustomizationData.putPropertyInt("08_filterRadius", filterRadius, "i 0 20", "Spectrum 1", "Noise Filter Radius")
        outCustomizationData.putPropertyFloat("09_filterStrength", filterStrength, "f 0.0 2.0", "Spectrum 1", "Noise Filter Strength")
        
        outCustomizationData.putPropertyFloat("15_agcTarget", rangeTarget, "f 10.0 5000.0", "Spectrum 1", "AGC Target Volume")
        outCustomizationData.putPropertyFloat("16_agcSmoothFactor", rangeSmoothFactor, "f 0.01 1.0", "Spectrum 1", "AGC Smoothness")

        outCustomizationData.putPropertyFloat("10_waveShift", waveShift, "f -1.0 1.0", "Spectrum 2", "Wave Offset")
        outCustomizationData.putPropertyBool("11_mirrorSample", mirrorSample, "Spectrum 2", "Mirror Symmetry")
        outCustomizationData.putPropertyInt("12_repeatSamples", repeatSamples, "i 1 6", "Spectrum 2", "Pattern Repeats")
        outCustomizationData.putPropertyInt("13_starAndEndGap", starAndEndGap, "i 0 30", "Spectrum 2", "Edge Gap")
        outCustomizationData.putPropertyInt("14_barsCount", barsCount, "i 8 512", "Spectrum 2", "Bar Count")
    }
}
