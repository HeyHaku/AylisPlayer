package com.aylis.comp.visual.core.playback.exo;

import android.media.MediaCodec;
import com.aylis.comp.AppPreferences.AppPreferences;
import com.aylis.comp.visual.core.playback.AudioFrameData;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ExoVisualizerDataProvider {
    private static final int MAX_BUFFER_COUNT = 600;
    private static final int MAX_REUSE_BUFFER_COUNT = 16;

    private boolean isExportMode = false;
    private volatile boolean validBuffers = false;
    private volatile boolean isPlaying = false;

    private long positionUs = 0;
    private long timeInMillis = 0;
    private int lastIndex = -1;

    private final Lock lockBuffersList = new ReentrantLock();
    private final List<BufferEntry> buffersList = new ArrayList<>();
    private final Queue<BufferEntry> buffersReuse = new ArrayDeque<>();

    public ExoVisualizerDataProvider() {
    }

    public void setExportMode(boolean exportMode) {
        this.isExportMode = exportMode;
    }

    public void release() {
        flushBuffers();
    }

    public void onPositionDiscontinuity() {
        flushBuffers();
    }

    public void flushBuffers() {
        try {
            if (lockBuffersList.tryLock(50, TimeUnit.MILLISECONDS)) {
                try {
                    for (BufferEntry e : buffersList) {
                        putReuseBuffer(e);
                    }
                    buffersList.clear();
                    validBuffers = false;
                    lastIndex = -1;
                    timeInMillis = System.currentTimeMillis();
                } finally {
                    lockBuffersList.unlock();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void putReuseBuffer(BufferEntry e) {
        if (buffersReuse.size() < MAX_REUSE_BUFFER_COUNT) {
            buffersReuse.add(e);
        }
    }

    private BufferEntry getReuseBuffer() {
        return buffersReuse.poll();
    }

    private ByteBuffer cloneBuffer(ByteBuffer original, ByteBuffer available) {
        int length = original.remaining();
        ByteBuffer dest = available;
        if (dest == null || dest.capacity() < length) {
            dest = ByteBuffer.allocateDirect(length);
        }
        dest.clear();

        int oldPos = original.position();
        dest.put(original);
        original.position(oldPos);

        dest.flip();
        return dest;
    }

    public void onSetStarted(boolean b) {
        isPlaying = b;
    }

    public void onPcmData(ByteBuffer buffer,
                          MediaCodec.BufferInfo bufferInfo,
                          int bufferIndex,
                          int sampleRate,
                          int channelCount,
                          long currentPositionUs) {
        timeInMillis = System.currentTimeMillis();
        this.positionUs = currentPositionUs;

        if (lastIndex == bufferIndex && bufferIndex != 0) return;
        lastIndex = bufferIndex;

        try {
            if (lockBuffersList.tryLock(50, TimeUnit.MILLISECONDS)) {
                try {
                    // 1. АВТО-ДЕТЕКТ СМЕНЫ ТРЕКА ИЛИ ПЕРЕМОТКИ
                    if (!buffersList.isEmpty()) {
                        BufferEntry lastEntry = buffersList.get(buffersList.size() - 1);
                        long lastTime = lastEntry.bufferInfo.presentationTimeUs;
                        long newTime = bufferInfo.presentationTimeUs;

                        // Если время пошло назад (новый трек) или скакнуло больше чем на 1.5 сек (перемотка)
                        if (newTime < lastTime || Math.abs(newTime - lastTime) > 1500000L) {
                            for (BufferEntry e : buffersList) {
                                putReuseBuffer(e);
                            }
                            buffersList.clear();
                        }
                    }

                    // 2. Очистка буферов старше 1.2 секунды
                    long thresholdUs = bufferInfo.presentationTimeUs - 1200000L;
                    for (Iterator<BufferEntry> iterator = buffersList.iterator(); iterator.hasNext(); ) {
                        BufferEntry e = iterator.next();
                        if (e.bufferInfo.presentationTimeUs < thresholdUs) {
                            putReuseBuffer(e);
                            iterator.remove();
                        }
                    }

                    if (buffersList.size() >= MAX_BUFFER_COUNT) {
                        putReuseBuffer(buffersList.remove(0));
                    }

                    BufferEntry newEntry = getReuseBuffer();
                    if (newEntry == null) {
                        newEntry = new BufferEntry();
                    }

                    newEntry.outputBuffer = cloneBuffer(buffer, newEntry.outputBuffer);
                    newEntry.bufferInfo.size = bufferInfo.size;
                    newEntry.bufferInfo.offset = bufferInfo.offset;
                    newEntry.bufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs;
                    newEntry.sampleRate = sampleRate;
                    newEntry.channelCount = channelCount;

                    buffersList.add(newEntry);
                    validBuffers = true;

                } finally {
                    lockBuffersList.unlock();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private long bytesToFrames(long byteCount, int channelCount) {
        return byteCount / (2L * channelCount);
    }

    private long framesToDurationUs(long frameCount, int sampleRate) {
        return (frameCount * 1000000L) / sampleRate;
    }

    private void extractPcmData(short[] outPcmData, long startTimeInBufUs, int bufferIndex, float[] rmsOut) {
        int cntr = 0;
        int currentBufferIdx = bufferIndex;

        if (currentBufferIdx < 0 || currentBufferIdx >= buffersList.size()) return;

        BufferEntry entry = buffersList.get(currentBufferIdx);
        double samplesPerUs = entry.sampleRate / 1000000.0;
        int sampleOffset = (int) Math.max(0, startTimeInBufUs * samplesPerUs);

        double sumSquares = 0.0;

        while (cntr < outPcmData.length && currentBufferIdx < buffersList.size()) {
            entry = buffersList.get(currentBufferIdx);
            ByteBuffer buf = entry.outputBuffer;
            int channelCount = entry.channelCount;
            int frameSize = channelCount * 2;

            int bytePos = entry.bufferInfo.offset + (sampleOffset * frameSize);
            int maxByte = entry.bufferInfo.offset + entry.bufferInfo.size;

            while (bytePos + frameSize <= maxByte && cntr < outPcmData.length) {
                short sample;
                if (channelCount == 1) {
                    int low = buf.get(bytePos) & 0xFF;
                    int high = buf.get(bytePos + 1);
                    sample = (short) ((high << 8) | low);
                } else {
                    int lLow = buf.get(bytePos) & 0xFF;
                    int lHigh = buf.get(bytePos + 1);
                    short left = (short) ((lHigh << 8) | lLow);

                    int rLow = buf.get(bytePos + 2) & 0xFF;
                    int rHigh = buf.get(bytePos + 3);
                    short right = (short) ((rHigh << 8) | rLow);

                    sample = (short) ((left + right) / 2);
                }

                outPcmData[cntr++] = sample;
                sumSquares += (sample * sample);
                bytePos += frameSize;
            }

            sampleOffset = 0;
            currentBufferIdx++;
        }

        // Если данных не хватило до конца окна, дополняем последним значением / тишиной
        while (cntr < outPcmData.length) {
            outPcmData[cntr++] = 0;
        }

        rmsOut[0] = (float) sumSquares;
    }

    public AudioFrameData getVisData(AudioFrameData outResult) {
        if (!isPlaying) {
            return getVisData(positionUs, outResult);
        }
        // Ограничиваем экстраполяцию времени максимум 150 мс, чтобы не улетать в будущее при задержках декодера
        long timePassedMs = Math.min(150L, Math.max(0L, System.currentTimeMillis() - timeInMillis));
        return getVisData(positionUs + (timePassedMs * 1000L), outResult);
    }

    public AudioFrameData getVisData(long targetPositionUs, AudioFrameData outResult) {
        int offsetMs = 0;
        if (!isExportMode) {
            offsetMs = AppPreferences.createOrGetInstance().getInt(AppPreferences.PREF_Int_exoVisualizerOffset);
        }
        long captureOffsetTimeUs = ((long) offsetMs * 1000L) + outResult.captureOffsetUs;

        if (!validBuffers) {
            outResult.valid = false;
            return outResult;
        }

        long lookupPosUs = targetPositionUs + captureOffsetTimeUs;
        float[] rmsAcc = new float[]{0.0f};

        try {
            if (lockBuffersList.tryLock(20, TimeUnit.MILLISECONDS)) {
                try {
                    int size = buffersList.size();
                    if (size == 0) {
                        outResult.valid = false;
                        return outResult;
                    }

                    int bestIndex = -1;
                    long bestOffsetUs = 0;

                    BufferEntry firstEntry = buffersList.get(0);
                    BufferEntry lastEntry = buffersList.get(size - 1);

                    long oldestStartUs = firstEntry.bufferInfo.presentationTimeUs;
                    long newestEndUs = lastEntry.bufferInfo.presentationTimeUs + 
                            framesToDurationUs(bytesToFrames(lastEntry.bufferInfo.size, lastEntry.channelCount), lastEntry.sampleRate);

                    // КЛАМПИНГ: если запрошенное время вылезло за края — берем ближайший доступный край, а не замираем
                    if (lookupPosUs <= oldestStartUs) {
                        bestIndex = 0;
                        bestOffsetUs = 0;
                    } else if (lookupPosUs >= newestEndUs) {
                        bestIndex = Math.max(0, size - 1);
                        long durUs = framesToDurationUs(bytesToFrames(lastEntry.bufferInfo.size, lastEntry.channelCount), lastEntry.sampleRate);
                        bestOffsetUs = Math.max(0, durUs - 10000L);
                    } else {
                        // Точный поиск буфера
                        for (int i = 0; i < size; i++) {
                            BufferEntry entry = buffersList.get(i);
                            long presentationTimeUs = entry.bufferInfo.presentationTimeUs;
                            long durationUs = framesToDurationUs(bytesToFrames(entry.bufferInfo.size, entry.channelCount), entry.sampleRate);
                            long bufferStartTimeUs = presentationTimeUs;
                            long bufferEndTimeUs = presentationTimeUs + durationUs;

                            if (lookupPosUs >= bufferStartTimeUs && lookupPosUs <= bufferEndTimeUs) {
                                bestIndex = i;
                                bestOffsetUs = lookupPosUs - bufferStartTimeUs;
                                break;
                            }
                        }
                    }

                    if (bestIndex >= 0) {
                        BufferEntry chosen = buffersList.get(bestIndex);
                        outResult.sampleRate = chosen.sampleRate;
                        extractPcmData(outResult.pcmBuffer, bestOffsetUs, bestIndex, rmsAcc);
                    }

                } finally {
                    lockBuffersList.unlock();
                }
            }
        } catch (Exception ignored) {
        }

        if (outResult.pcmBuffer != null && outResult.pcmBuffer.length > 0) {
            outResult.rms = (float) Math.sqrt(rmsAcc[0] / outResult.pcmBuffer.length) / 32768.0f;
        }
        outResult.valid = true;

        return outResult;
    }

    public static class BufferEntry {
        public ByteBuffer outputBuffer = ByteBuffer.allocateDirect(0);
        public MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        public int sampleRate = 44100;
        public int channelCount = 2;
    }
}