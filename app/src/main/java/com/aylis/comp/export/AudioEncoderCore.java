package com.aylis.comp.export;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioEncoderCore {
    private static final String TAG = "AudioEncoderCore";

    private MediaCodec mEncoder;
    private MediaCodec.BufferInfo mBufferInfo;
    private MuxerWrapper mMuxerWrapper;
    private int mAudioTrackIndex = -1;
    private boolean mFormatChanged = false;

    private long mMaxPtsUs = -1;

    public AudioEncoderCore(int sampleRate, int channelCount, int bitRate, long maxPtsUs) throws IOException {
        mMaxPtsUs = maxPtsUs;
        mSampleRate = sampleRate;
        mChannelCount = channelCount;
        mBufferInfo = new MediaCodec.BufferInfo();
        MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);

        mEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mEncoder.start();
    }

    public void setMuxerWrapper(MuxerWrapper muxerWrapper) {
        mMuxerWrapper = muxerWrapper;
    }

    private int mSampleRate;
    private int mChannelCount;
    private long mTotalSamplesEncoded = 0;

    public void encodeChunk(ByteBuffer pcmData, long presentationTimeUs) {
        if (pcmData == null) return;

        if (mTotalSamplesEncoded == 0 && presentationTimeUs > 0) {
            mTotalSamplesEncoded = (presentationTimeUs * mSampleRate) / 1000000L;
        }

        while (pcmData.hasRemaining()) {
            int inIndex = mEncoder.dequeueInputBuffer(1000);
            if (inIndex >= 0) {
                ByteBuffer inputBuffer = mEncoder.getInputBuffer(inIndex);
                inputBuffer.clear();

                int chunkSize = Math.min(pcmData.remaining(), inputBuffer.capacity());
                int oldLimit = pcmData.limit();
                pcmData.limit(pcmData.position() + chunkSize);
                inputBuffer.put(pcmData);
                pcmData.limit(oldLimit);

                long exactPtsUs = (mTotalSamplesEncoded * 1000000L) / mSampleRate;

                if (mMaxPtsUs > 0 && exactPtsUs > mMaxPtsUs) {
                    // skip
                } else {
                    mEncoder.queueInputBuffer(inIndex, 0, chunkSize, exactPtsUs, 0);
                }
                
                int samplesInChunk = chunkSize / (mChannelCount * 2);
                mTotalSamplesEncoded += samplesInChunk;
                
                drainEncoder(false);
            }
        }
    }

    public void drainEncoder(boolean endOfStream) {
        if (endOfStream) {
            int inIndex = -1;
            int attempts = 0;
            while (inIndex < 0 && attempts < 50) {
                inIndex = mEncoder.dequeueInputBuffer(10000);
                attempts++;
            }
            if (inIndex >= 0) {
                long exactPtsUs = (mTotalSamplesEncoded * 1000000L) / mSampleRate;
                mEncoder.queueInputBuffer(inIndex, 0, 0, exactPtsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            }
        }

        int tryAgainCount = 0;
        while (true) {
            int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, endOfStream ? 10000 : 0);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break;
                tryAgainCount++;
                if (tryAgainCount > 20) {
                    android.util.Log.w("ExportDebug", "Audio EOS drain timed out, breaking loop");
                    break; // prevent infinite loop
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                tryAgainCount = 0;
                if (mMuxerWrapper != null) {
                    MediaFormat newFormat = mEncoder.getOutputFormat();
                    mAudioTrackIndex = mMuxerWrapper.addTrack(newFormat);
                    mFormatChanged = true;
                }
            } else if (encoderStatus >= 0) {
                tryAgainCount = 0;
                ByteBuffer encodedData = mEncoder.getOutputBuffer(encoderStatus);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0 && mMuxerWrapper != null) {
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                    mMuxerWrapper.writeSampleData(mAudioTrackIndex, encodedData, mBufferInfo);
                }

                mEncoder.releaseOutputBuffer(encoderStatus, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;
                }
            }
        }
    }

    public void release() {
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
    }
}
