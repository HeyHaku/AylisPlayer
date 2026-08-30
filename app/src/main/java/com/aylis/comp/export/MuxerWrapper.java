package com.aylis.comp.export;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import java.nio.ByteBuffer;

public class MuxerWrapper {
    private MediaMuxer muxer;
    private int expectedTracks;
    private int addedTracks = 0;
    private boolean started = false;

    public MuxerWrapper(MediaMuxer muxer, int expectedTracks) {
        this.muxer = muxer;
        this.expectedTracks = expectedTracks;
    }

    public synchronized int addTrack(MediaFormat format) {
        int trackIndex = muxer.addTrack(format);
        addedTracks++;
        if (addedTracks == expectedTracks) {
            muxer.start();
            started = true;
        }
        return trackIndex;
    }

    public synchronized boolean isStarted() {
        return started;
    }

    public synchronized void writeSampleData(int trackIndex, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
        if (started) {
            muxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
        }
    }
}
