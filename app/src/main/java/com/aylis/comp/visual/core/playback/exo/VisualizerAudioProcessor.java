

package com.aylis.comp.visual.core.playback.exo;

import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.audio.BaseAudioProcessor;
import java.nio.ByteBuffer;

public class VisualizerAudioProcessor extends BaseAudioProcessor {

    private IVisualizerDataCapturer visualizerData;
    private long totalFramesHandled = 0;
    private int sampleRate = 44100;
    private int channelCount = 2;

    public VisualizerAudioProcessor(IVisualizerDataCapturer visualizerData) {
        this.visualizerData = visualizerData;
    }

    @Override
    protected AudioFormat onConfigure(AudioFormat inputAudioFormat) throws UnhandledAudioFormatException {
        this.sampleRate = inputAudioFormat.sampleRate;
        this.channelCount = inputAudioFormat.channelCount;
        return inputAudioFormat;
    }

    @Override
    public void queueInput(ByteBuffer inputBuffer) {
        if (!inputBuffer.hasRemaining()) return;

        int size = inputBuffer.remaining();

        long positionUs = (totalFramesHandled * 1000000L) / sampleRate;

        if (visualizerData != null) {

            android.media.MediaCodec.BufferInfo bufferInfo = new android.media.MediaCodec.BufferInfo();
            bufferInfo.set(inputBuffer.position(), size, positionUs, 0);

            int oldPosition = inputBuffer.position();
            visualizerData.onPcmData(inputBuffer, bufferInfo, 0, sampleRate, channelCount, positionUs);
            inputBuffer.position(oldPosition);
        }

        int bytesPerFrame = 2 * channelCount;
        totalFramesHandled += size / bytesPerFrame;

        replaceOutputBuffer(size).put(inputBuffer).flip();
    }

    @Override
    protected void onFlush() {
        totalFramesHandled = 0;
    }

    @Override
    protected void onReset() {
        totalFramesHandled = 0;
    }
}

