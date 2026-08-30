package com.aylis.comp.export;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.view.Surface;
import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoEncoderCore {
    private static final String MIME_TYPE = "video/avc";
    private static final int IFRAME_INTERVAL = 1;

    private Surface mInputSurface;
    private MuxerWrapper mMuxerWrapper;
    private MediaCodec mEncoder;
    private MediaCodec.BufferInfo mBufferInfo;
    private int mVideoTrackIndex;
    private boolean mFormatChanged;

    public VideoEncoderCore(int width, int height, int bitRate, int frameRate, MuxerWrapper muxerWrapper) throws IOException {
        mBufferInfo = new MediaCodec.BufferInfo();
        mMuxerWrapper = muxerWrapper;
        mVideoTrackIndex = -1;
        mFormatChanged = false;

        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = mEncoder.createInputSurface();
        mEncoder.start();
    }

    public Surface getInputSurface() {
        return mInputSurface;
    }

    public void release() {
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
    }

    public void drainEncoder(boolean endOfStream) {
        if (endOfStream) {
            mEncoder.signalEndOfInputStream();
        }

        int tryAgainCount = 0;
        while (true) {
            int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, endOfStream ? 10000 : 0);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) {
                    break;
                }
                tryAgainCount++;
                if (tryAgainCount > 20) {
                    android.util.Log.w("ExportDebug", "Video EOS drain timed out, breaking loop");
                    break; // prevent infinite loop
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                tryAgainCount = 0;
                if (mFormatChanged) {
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat newFormat = mEncoder.getOutputFormat();
                mVideoTrackIndex = mMuxerWrapper.addTrack(newFormat);
                mFormatChanged = true;
            } else if (encoderStatus >= 0) {
                tryAgainCount = 0;
                ByteBuffer encodedData = mEncoder.getOutputBuffer(encoderStatus);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                    mMuxerWrapper.writeSampleData(mVideoTrackIndex, encodedData, mBufferInfo);
                }

                mEncoder.releaseOutputBuffer(encoderStatus, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;
                }
            }
        }
    }
}
