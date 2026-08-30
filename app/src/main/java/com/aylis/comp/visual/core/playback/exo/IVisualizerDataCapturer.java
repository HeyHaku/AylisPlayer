

package com.aylis.comp.visual.core.playback.exo;

import java.nio.ByteBuffer;

public interface IVisualizerDataCapturer {

    void onSetStarted(boolean b);

    void onSetEnabled(boolean b);

    void onPcmData(ByteBuffer buffer,
                   android.media.MediaCodec.BufferInfo bufferInfo,
                   int bufferIndex,
                   int sampleRate,
                   int channelCount,
                   long positionUs);

    void onAudioSessionId(int audioSessionId);
}

