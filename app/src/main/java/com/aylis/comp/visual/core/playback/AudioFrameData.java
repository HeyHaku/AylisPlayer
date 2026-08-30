

package com.aylis.comp.visual.core.playback;

public class AudioFrameData {

    public boolean valid = false;
    public short[] pcmBuffer;
    public int sampleRate;
    public float rms;
    public long captureOffsetUs = 0;

    private AudioFrameData(int bufferSize) {
        valid = false;
        pcmBuffer = new short[bufferSize];
        sampleRate = 44100;
        rms = 0.0f;
    }

    public static AudioFrameData createReuse(AudioFrameData old, int bufferSize) {
        if (old == null || old.pcmBuffer.length != bufferSize) {
            old = new AudioFrameData(bufferSize);
        }
        return old;
    }
}

