package com.aylis.comp.visual.core.audio.Providers;

import com.NAudio.FastFourierTransform;
import com.aylis.Common.ISimpleListFloat;
import com.aylis.comp.visual.core.Elements.Element;
import com.aylis.comp.visual.core.Elements.IFrameDataProvider;
import com.aylis.comp.visual.core.Elements.bars.AudioBars.ISegmentDataProvider;
import com.aylis.comp.visual.core.InternalVisualizationDataProvider;
import com.aylis.comp.visual.core.playback.AudioFrameData;
import com.aylis.comp.visual.core.Dsp.DspWindows;

public class CavaSpectrumProvider implements ISegmentDataProvider, IFrameDataProvider {

    public static final String typeName = "CavaSpectrum";

    private AudioFrameData visData = null;
    // 0: 2048 (46ms), 1: 4096 (92ms), 2: 8192 (185ms), 3: 16384 (371ms)
    private int fftSizePreset = 2; // По дефолту ставим 8192
    private int dataCount = 8192;

    private void updateDataCountFromPreset(int preset) {
        this.fftSizePreset = Math.max(0, Math.min(3, preset));
        switch (this.fftSizePreset) {
            case 0: this.dataCount = 2048; break;
            case 1: this.dataCount = 4096; break;
            case 2: this.dataCount = 8192; break;
            case 3: this.dataCount = 16384; break;
        }
    }

    private final com.aylis.comp.visual.core.Elements.Base.FrameValuesAccumulator frameValuesAccumulator =
            new com.aylis.comp.visual.core.Elements.Base.FrameValuesAccumulator();

    // Reusable buffers (Zero GC Allocations per frame)
    private float[] barValues = new float[0];
    private float[] dropSpeeds = new float[0];
    private float[] rawFftBars = new float[0];
    private float[] smoothedTargets = new float[0];
    private float[] spatialBuffer = new float[0];
    private float[] fftWindowMultipliers = new float[0];
    private FastFourierTransform.Complex[] fftBuffer = new FastFourierTransform.Complex[0];

    private int lastSampleRate = -1;
    private int lastFftSize = -1;

    // Настройки (UI Properties):
    private float waveFactor = 0.75f;           // 01: Сила скругления ушек [0.0 - 1.0]
    private int waveShapeMode = 0;              // 01b: 0 = Идеально круглая капля (Gaussian), 1 = Острые крылья (Monstercat)
    private float waveRadius = 0.45f;           // 01c: Радиус/толщина ушка [0.05 - 1.0]
    private float gravityStep = 0.035f;         // 02: Скорость падения [0.002 - 0.15]
    private float minFrequency = 30.0f;         // 03: Нижний срез баса [20 - 400 Hz]
    private float maxFrequency = 4500.0f;       // 04: Верхний срез частот [300 - 18000 Hz]
    private float sensitivity = 1.0f;           // 05: Общее усиление [0.1 - 5.0]
    private int timeOffsetMs = 0;               // 06: Смещение таймлайна [-100 .. +100 ms]
    private boolean monstercatEnabled = true;   // 07: Включить ушки
    private int barsCount = 128;                // 08: Количество баров
    private float temporalSmooth = 0.65f;       // 09: Сглаживание во времени [0.0 - 0.95]
    private float amplitudeComp = 0.55f;        // 10: Компрессия амплитуды [0.1 - 1.0]
    private float noiseFloor = 0.02f;           // 11: Порог шума [0.0 - 0.5]
    private float highFreqBoost = 1.5f;         // 12: Эквализация верхов [1.0 - 5.0]

    private float currentRms = 0.0f;
    private com.aylis.comp.visual.core.Dsp.RangeBox beatRange = new com.aylis.comp.visual.core.Dsp.RangeBox(0.5f);
    private float beatBarValueSmooth = 0.0f;
    private float beatSmoothFactor = 0.4f;
    private float beatAutoGain = 1.0f;

    public CavaSpectrumProvider() {
        beatRange.setCorners(0.0f, 0.2f, 0.7f, 35.0f); // 0.7f как в старом AudioSpectrum2
        setSampleOutCount(128);
    }

    public void setSampleOutCount(int sampleOutCount) {
        this.barsCount = Math.max(1, sampleOutCount);
        if (barValues.length != barsCount) {
            barValues = new float[barsCount];
            dropSpeeds = new float[barsCount];
            rawFftBars = new float[barsCount];
            smoothedTargets = new float[barsCount];
            spatialBuffer = new float[barsCount];
        }
    }

    private void ensureFftBufferSize(int size) {
        if (fftBuffer.length != size) {
            fftBuffer = new FastFourierTransform.Complex[size];
            for (int i = 0; i < size; i++) {
                fftBuffer[i] = new FastFourierTransform.Complex(0.0f, 0.0f);
            }
            fftWindowMultipliers = new float[size];
            for (int i = 0; i < size; i++) {
                fftWindowMultipliers[i] = (float) DspWindows.hannWindow(i, size) * 0.00390625f;
            }
        }
    }

    @Override
    public void process(InternalVisualizationDataProvider visualisationData) {
        visData = AudioFrameData.createReuse(visData, dataCount);
        visData.captureOffsetUs = timeOffsetMs * 1000L;
        AudioFrameData visDataResult = visualisationData.onRequestSoundVisualizationData(visData);

        if (visDataResult == null || !visDataResult.valid || visDataResult.pcmBuffer == null) {
            currentRms = 0.0f;
            if (barValues.length > 0) java.util.Arrays.fill(barValues, 0.0f);
            if (dropSpeeds.length > 0) java.util.Arrays.fill(dropSpeeds, 0.0f);
            if (smoothedTargets.length > 0) java.util.Arrays.fill(smoothedTargets, 0.0f);
            frameValuesAccumulator.addFrame(barValues);
            return;
        }

        short[] buf = visDataResult.pcmBuffer;
        int sampleRate = visDataResult.sampleRate;
        int fftSize = buf.length;

        ensureFftBufferSize(fftSize);

        // 1. Оконная функция (Вычисляем только FFT)
        for (int i = 0; i < fftSize; i++) {
            float val = buf[i] * fftWindowMultipliers[i];
            fftBuffer[i].setRe(val);
            fftBuffer[i].setIm(0.0f);
        }

        // 2. FFT
        FastFourierTransform.FFT(true, fftBuffer);

        int fftOutputSize = fftSize / 2;
        float df = (float) sampleRate / fftSize;

        // 3. Непрерывная логарифмическая выборка с линейной интерполяцией между бинами (убирает ступеньки)
        for (int i = 0; i < barsCount; i++) {
            float f_low = minFrequency * (float) Math.pow(maxFrequency / minFrequency, (double) i / barsCount);
            float f_high = minFrequency * (float) Math.pow(maxFrequency / minFrequency, (double) (i + 1) / barsCount);

            float binLow = f_low / df;
            float binHigh = f_high / df;

            int i_low = (int) binLow;
            int i_high = (int) binHigh;

            float sum = 0.0f;

            if (i_high <= i_low) {
                // Если диапазон частот бара меньше ширины одного бина — делаем линейную интерполяцию
                int b0 = Math.min(i_low, fftOutputSize - 1);
                int b1 = Math.min(b0 + 1, fftOutputSize - 1);
                float frac = binLow - i_low;

                float mag0 = (float) Math.hypot(fftBuffer[b0].re(), fftBuffer[b0].im());
                float mag1 = (float) Math.hypot(fftBuffer[b1].re(), fftBuffer[b1].im());
                float interpolatedMag = mag0 * (1.0f - frac) + mag1 * frac;
                sum = (float) Math.pow(interpolatedMag, amplitudeComp);
            } else {
                // Если бар охватывает несколько бинов (на высоких частотах)
                int count = 0;
                for (int j = i_low; j <= i_high && j < fftOutputSize; j++) {
                    float mag = (float) Math.hypot(fftBuffer[j].re(), fftBuffer[j].im());
                    sum += (float) Math.pow(mag, amplitudeComp);
                    count++;
                }
                if (count > 0) sum /= count;
            }

            float eq = 1.0f + (highFreqBoost - 1.0f) * ((float) i / Math.max(1, barsCount - 1));
            float finalVal = sum * sensitivity * eq;

            if (finalVal < noiseFloor) finalVal = 0.0f;
            rawFftBars[i] = finalVal;
        }

        // 4. Формирование формы ушек
        if (monstercatEnabled && waveFactor > 0.0f) {
            if (waveShapeMode == 0) {
                // РЕЖИМ 0: Честная Гауссова свертка (Gaussian Spatial Kernel)
                // Превращает ЛЮБЫЕ плоские плато и квадраты в идеально круглые капли/ушки
                int radius = Math.max(1, (int) ((barsCount / 64.0f) * (2.0f + waveRadius * 24.0f)));
                radius = Math.min(radius, barsCount / 2);

                System.arraycopy(rawFftBars, 0, spatialBuffer, 0, barsCount);

                float sigma = radius * 0.45f;
                float twoSigmaSq = 2.0f * sigma * sigma;

                for (int i = 0; i < barsCount; i++) {
                    float sum = 0.0f;
                    float weightSum = 0.0f;

                    for (int r = -radius; r <= radius; r++) {
                        int idx = i + r;
                        // Зеркалирование на границах для сохранения формы
                        if (idx < 0) idx = -idx;
                        if (idx >= barsCount) idx = 2 * barsCount - 1 - idx;
                        if (idx >= barsCount) idx = barsCount - 1;

                        float w = (float) Math.exp(-(r * r) / twoSigmaSq);
                        sum += spatialBuffer[idx] * w;
                        weightSum += w;
                    }

                    float blurred = (weightSum > 0.0f) ? (sum / weightSum) : spatialBuffer[i];
                    // Плавное смешивание оригинального пика и купола
                    rawFftBars[i] = rawFftBars[i] * (1.0f - waveFactor) + blurred * waveFactor;
                }
            } else {
                // РЕЖИМ 1: Классический резкий Monstercat (крылья/клинья)
                for (int i = 1; i < barsCount; i++) {
                    rawFftBars[i] = Math.max(rawFftBars[i], rawFftBars[i - 1] * waveFactor);
                }
                for (int i = barsCount - 2; i >= 0; i--) {
                    rawFftBars[i] = Math.max(rawFftBars[i], rawFftBars[i + 1] * waveFactor);
                }
            }
        }

        // 5. Интеграл по времени (Сглаживание мерцания)
        for (int i = 0; i < barsCount; i++) {
            smoothedTargets[i] = (smoothedTargets[i] * temporalSmooth) + (rawFftBars[i] * (1.0f - temporalSmooth));
        }

        // 6. Гравитационное падение CAVA
        for (int i = 0; i < barsCount; i++) {
            float target = smoothedTargets[i];
            if (target > barValues[i]) {
                barValues[i] = target;
                dropSpeeds[i] = 0.0f;
            } else {
                dropSpeeds[i] += gravityStep;
                barValues[i] -= dropSpeeds[i];
                if (barValues[i] < 0.0f) {
                    barValues[i] = 0.0f;
                    dropSpeeds[i] = 0.0f;
                }
            }
        }

        // 7. Расчет RMS для шейков (как было в AudioSpectrum2)
        // Авто-нормализация (Auto Gain), чтобы RangeBox получал данные в диапазоне ~0..50
        float maxFft = 0.001f;
        for (int i = 0; i < barsCount; i++) {
            if (rawFftBars[i] > maxFft) maxFft = rawFftBars[i];
        }
        beatAutoGain = (beatAutoGain * 0.9f) + (maxFft * 0.1f);
        if (beatAutoGain < 10.0f) beatAutoGain = 10.0f; // Предотвращаем бесконечное усиление тишины
        float gainMul = 50.0f / beatAutoGain;

        beatRange.reset(barsCount);
        for (int i = 0; i < barsCount; i++) {
            // Для RangeBox нам нужны бары с хорошей амплитудой. Приводим их масштаб.
            beatRange.addValue(i, rawFftBars[i] * gainMul); 
        }
        float beatValue = beatRange.getValueNormal();
        beatBarValueSmooth = (beatBarValueSmooth * (1.0f - beatSmoothFactor)) + (beatValue * beatSmoothFactor);
        currentRms = beatBarValueSmooth;

        frameValuesAccumulator.addFrame(barValues);
    }

    @Override
    public float[] getFrameValues() {
        return barValues;
    }

    @Override
    public float getRms() {
        return currentRms;
    }

    @Override
    public ISimpleListFloat createFrameValuesAccessorList(int reactionDelay, int reactionAccumulatedDelay, int softnessRadius, ISimpleListFloat barVals) {
        return frameValuesAccumulator.createFrameValuesAccessorList(reactionDelay, reactionAccumulatedDelay, softnessRadius, barVals);
    }

    @Override
    public void onApplyCustomization(Element.CustomizationData customizationData) {
        // 1. Buffer & Timing
        int newPreset = customizationData.getPropertyInt("01_fftSizePreset", fftSizePreset);
        if (newPreset != fftSizePreset) {
            updateDataCountFromPreset(newPreset);
        }
        timeOffsetMs = customizationData.getPropertyInt("02_timeOffsetMs", timeOffsetMs);

        // 2. Band Shaping & Spread
        monstercatEnabled = customizationData.getPropertyBool("03_monstercatEnabled", monstercatEnabled);
        waveShapeMode = customizationData.getPropertyInt("04_waveShapeMode", waveShapeMode);
        waveFactor = customizationData.getPropertyFloat("05_waveFactor", waveFactor);
        waveRadius = customizationData.getPropertyFloat("06_waveRadius", waveRadius);

        // 3. Dynamics & Falloff
        gravityStep = customizationData.getPropertyFloat("07_gravityStep", gravityStep);
        temporalSmooth = customizationData.getPropertyFloat("08_temporalSmooth", temporalSmooth);
        int newBarsCount = customizationData.getPropertyInt("09_barsCount", barsCount);
        if (newBarsCount != barsCount) {
            setSampleOutCount(newBarsCount);
        }

        // 4. Frequency Range & Response
        float newMinF = customizationData.getPropertyFloat("10_minFrequency", minFrequency);
        float newMaxF = customizationData.getPropertyFloat("11_maxFrequency", maxFrequency);
        if (newMinF != minFrequency || newMaxF != maxFrequency) {
            minFrequency = newMinF;
            maxFrequency = newMaxF;
        }
        sensitivity = customizationData.getPropertyFloat("12_sensitivity", sensitivity);
        amplitudeComp = customizationData.getPropertyFloat("13_amplitudeComp", amplitudeComp);
        highFreqBoost = customizationData.getPropertyFloat("14_highFreqBoost", highFreqBoost);
        noiseFloor = customizationData.getPropertyFloat("15_noiseFloor", noiseFloor);
    }

    @Override
    public void onReadCustomization(Element.CustomizationData outCustomizationData) {
        // Trapcode: Buffer & Delay
        outCustomizationData.putPropertyInt("01_fftSizePreset", fftSizePreset, "i 0 3", "Sound Buffer", "Sample Window (0:2k, 1:4k, 2:8k, 3:16k)");
        outCustomizationData.putPropertyInt("02_timeOffsetMs", timeOffsetMs, "i -100 100", "Sound Buffer", "Audio Delay Offset (ms)");

        // Trapcode: Band Shaping & Dome Spread
        outCustomizationData.putPropertyBool("03_monstercatEnabled", monstercatEnabled, "Band Shaping", "Enable Dome Filter");
        outCustomizationData.putPropertyInt("04_waveShapeMode", waveShapeMode, "i 0 1", "Band Shaping", "Dome Profile (0: Gaussian, 1: Linear Wing)");
        outCustomizationData.putPropertyFloat("05_waveFactor", waveFactor, "f 0.0 1.0", "Band Shaping", "Band Spread Factor");
        outCustomizationData.putPropertyFloat("06_waveRadius", waveRadius, "f 0.05 1.0", "Band Shaping", "Spread Radius (Width)");

        // Trapcode: Falloff & Decay (Физика движения)
        outCustomizationData.putPropertyFloat("07_gravityStep", gravityStep, "f 0.002 0.15", "Falloff & Decay", "Falloff Rate (Gravity)");
        outCustomizationData.putPropertyFloat("08_temporalSmooth", temporalSmooth, "f 0.0 0.95", "Falloff & Decay", "Response Damping (Smooth)");
        outCustomizationData.putPropertyInt("09_barsCount", barsCount, "i 8 512", "Falloff & Decay", "Quantize Bands (Bar Count)");

        // Trapcode: Frequency Range & Sound Keys Calibration
        outCustomizationData.putPropertyFloat("10_minFrequency", minFrequency, "f 20.0 400.0", "Sound Keys Calibration", "Range Min Frequency (Hz)");
        outCustomizationData.putPropertyFloat("11_maxFrequency", maxFrequency, "f 300.0 18000.0", "Sound Keys Calibration", "Range Max Frequency (Hz)");
        outCustomizationData.putPropertyFloat("12_sensitivity", sensitivity, "f 0.1 5.0", "Sound Keys Calibration", "Master Sound Gain");
        outCustomizationData.putPropertyFloat("13_amplitudeComp", amplitudeComp, "f 0.1 1.0", "Sound Keys Calibration", "Dynamic Range Compression");
        outCustomizationData.putPropertyFloat("14_highFreqBoost", highFreqBoost, "f 1.0 10.0", "Sound Keys Calibration", "High Shelf Boost (Treble EQ)");
        outCustomizationData.putPropertyFloat("15_noiseFloor", noiseFloor, "f 0.0 0.5", "Sound Keys Calibration", "Gate Threshold (Noise Floor)");
    }
}