package com.aylis.comp.export;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioDecoderCore {
    private static final String TAG = "AudioDecoderCore";
    private static final long TIMEOUT_US = 1000;

    private MediaExtractor extractor;
    private MediaCodec decoder;
    private MediaCodec.BufferInfo bufferInfo;
    
    private int sampleRate = 44100;
    private int channelCount = 2;
    private boolean isEOS = false;
    private boolean isOutputEOS = false;
    private long mTotalBytesDecoded = 0;
    private long mTotalSamplesDecoded = 0;
    private long mFirstSampleTimeUs = -1;
    private long startPtsOffsetUs = 0;
    private java.io.File tempAudioFile = null;
    private com.aylis.comp.export.VideoExportTask.ExportListener progressListener;

    public AudioDecoderCore(android.content.Context context, android.net.Uri uri, int startSec, com.aylis.comp.export.VideoExportTask.ExportListener listener) throws IOException {
        this.progressListener = listener;
        extractor = new MediaExtractor();
        startPtsOffsetUs = startSec * 1000000L;
        
        uri = com.aylis.comp.playback.ExoMediaPlayer.YoutubeResolver.resolveSync(uri);
        
        String scheme = uri.getScheme();
        if (scheme == null || "file".equals(scheme)) {
            String filePath = uri.getPath() != null ? uri.getPath() : uri.toString();
            extractor.setDataSource(filePath);
        } else {
            // Guarantee no Binder IPC by copying to local cache
            tempAudioFile = new java.io.File(context.getCacheDir(), "export_audio_temp_" + System.currentTimeMillis() + ".tmp");
            
            boolean useFastDownload = false;
            boolean isFullyCached = false;
            
            if ("http".equals(scheme) || "https".equals(scheme)) {
                try {
                    androidx.media3.datasource.cache.SimpleCache cache = com.aylis.comp.playback.ExoMediaPlayer.ExoCacheManager.getSimpleCache(context);
                    if (cache != null) {
                        long contentLength = cache.getContentMetadata(uri.toString()).get(androidx.media3.datasource.cache.ContentMetadata.KEY_CONTENT_LENGTH, -1L);
                        long cachedBytes = cache.getCachedBytes(uri.toString(), 0, contentLength != -1 ? contentLength : Long.MAX_VALUE);
                        if (cachedBytes > 0 && contentLength > 0 && cachedBytes >= contentLength) {
                            isFullyCached = true;
                        }
                    }
                } catch (Exception e) {}
                
                if (!isFullyCached) {
                    useFastDownload = true;
                }
            }
            
            java.io.InputStream in = null;
            java.io.OutputStream out = null;
            java.net.HttpURLConnection httpConn = null;
            
            try {
                long totalBytes = -1;
                long downloadedBytes = 0;
                boolean alreadyDownloaded = false;
                
                if (useFastDownload) {
                    java.net.URL url = new java.net.URL(uri.toString());
                    httpConn = (java.net.HttpURLConnection) url.openConnection();
                    httpConn.setRequestMethod("HEAD");
                    httpConn.setConnectTimeout(10000);
                    httpConn.setReadTimeout(10000);
                    httpConn.connect();
                    if (httpConn.getResponseCode() == java.net.HttpURLConnection.HTTP_OK || httpConn.getResponseCode() == 206) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            totalBytes = httpConn.getContentLengthLong();
                        } else {
                            totalBytes = httpConn.getContentLength();
                        }
                    }
                    httpConn.disconnect();
                    httpConn = null;
                    
                    if (totalBytes > 0) {
                        out = new java.io.FileOutputStream(tempAudioFile);
                        long chunkSize = 1024 * 1024; // 1 MB
                        for (long start = 0; start < totalBytes; start += chunkSize) {
                            long end = Math.min(start + chunkSize - 1, totalBytes - 1);
                            
                            java.net.HttpURLConnection chunkConn = (java.net.HttpURLConnection) url.openConnection();
                            chunkConn.setRequestProperty("Range", "bytes=" + start + "-" + end);
                            chunkConn.setConnectTimeout(10000);
                            chunkConn.setReadTimeout(15000);
                            chunkConn.connect();
                            
                            int responseCode = chunkConn.getResponseCode();
                            if (responseCode == 206 || responseCode == 200) {
                                java.io.InputStream chunkIn = chunkConn.getInputStream();
                                byte[] buffer = new byte[64 * 1024];
                                int read;
                                while ((read = chunkIn.read(buffer)) != -1) {
                                    out.write(buffer, 0, read);
                                    downloadedBytes += read;
                                    if (progressListener != null) {
                                        progressListener.onDownloadProgress(downloadedBytes, totalBytes);
                                    }
                                }
                                out.flush();
                                chunkIn.close();
                            }
                            chunkConn.disconnect();
                        }
                        alreadyDownloaded = true;
                    } else {
                        // fallback
                        in = context.getContentResolver().openInputStream(uri);
                    }
                } else if ("http".equals(scheme) || "https".equals(scheme)) {
                    // Fully cached, use CacheDataSource to extract it
                    androidx.media3.datasource.cache.CacheDataSource cacheDataSource = 
                        com.aylis.comp.playback.ExoMediaPlayer.ExoCacheManager.getCacheDataSourceFactory(context).createDataSource();
                    androidx.media3.datasource.DataSpec dataSpec = new androidx.media3.datasource.DataSpec(uri);
                    totalBytes = cacheDataSource.open(dataSpec);
                    in = new java.io.InputStream() {
                        @Override
                        public int read() throws IOException {
                            byte[] b = new byte[1];
                            int r = read(b, 0, 1);
                            return r == -1 ? -1 : (b[0] & 0xFF);
                        }
                        @Override
                        public int read(byte[] b, int off, int len) throws IOException {
                            return cacheDataSource.read(b, off, len);
                        }
                        @Override
                        public void close() throws IOException {
                            cacheDataSource.close();
                        }
                    };
                } else {
                    in = context.getContentResolver().openInputStream(uri);
                }
                
                if (!alreadyDownloaded) {
                    out = new java.io.FileOutputStream(tempAudioFile);
                    byte[] buffer = new byte[64 * 1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        downloadedBytes += read;
                        if (progressListener != null) {
                            progressListener.onDownloadProgress(downloadedBytes, totalBytes);
                        }
                    }
                    out.flush();
                }
            } finally {
                if (in != null) try { in.close(); } catch (Exception e) {}
                if (out != null) try { out.close(); } catch (Exception e) {}
                if (httpConn != null) try { httpConn.disconnect(); } catch (Exception e) {}
            }
            extractor.setDataSource(tempAudioFile.getAbsolutePath());
        }

        int audioTrackIndex = -1;
        MediaFormat format = null;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) {
                extractor.selectTrack(i);
                audioTrackIndex = i;
                break;
            }
        }

        if (audioTrackIndex == -1 || format == null) {
            Log.e(TAG, "No audio track found in " + uri.toString() + ". Audio sync will be disabled.");
            isOutputEOS = true;
            return;
        }

        String mime = format.getString(MediaFormat.KEY_MIME);
        if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        }
        if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        }

        decoder = MediaCodec.createDecoderByType(mime);
        decoder.configure(format, null, null, 0);
        decoder.start();

        if (startSec > 0) {
            extractor.seekTo(startPtsOffsetUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        }

        bufferInfo = new MediaCodec.BufferInfo();
    }

    public int getSampleRate() { return sampleRate; }
    public int getChannelCount() { return channelCount; }
    public boolean isEOS() { return isOutputEOS; }

    public interface AudioChunkListener {
        void onAudioChunk(ByteBuffer buffer, MediaCodec.BufferInfo info, int sampleRate, int channelCount);
    }

    /**
     * Decodes chunks until the given target time is reached.
     */
    public void decodeUntil(long targetTimeUs, AudioChunkListener listener) {
        if (isOutputEOS) return;

        while (!isOutputEOS) {
            // Feed input
            if (!isEOS) {
                int inIndex = decoder.dequeueInputBuffer(0);
                if (inIndex >= 0) {
                    ByteBuffer inputBuffer = decoder.getInputBuffer(inIndex);
                    int sampleSize = extractor.readSampleData(inputBuffer, 0);
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        isEOS = true;
                    } else {
                        decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                        extractor.advance();
                    }
                }
            }

            // Read output
            int outIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US);
            if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = decoder.getOutputFormat();
                if (newFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    sampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                }
                if (newFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    channelCount = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                }
            } else if (outIndex >= 0) {
                ByteBuffer outputBuffer = decoder.getOutputBuffer(outIndex);
                if (bufferInfo.size > 0 && outputBuffer != null) {
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    
                    if (mFirstSampleTimeUs < 0) {
                        mFirstSampleTimeUs = bufferInfo.presentationTimeUs;
                    }
                    int samples = bufferInfo.size / (channelCount * 2);
                    long exactPtsUs = mFirstSampleTimeUs + (mTotalSamplesDecoded * 1000000L) / sampleRate;
                    mTotalSamplesDecoded += samples;
                    bufferInfo.presentationTimeUs = exactPtsUs;
                    
                    if (listener != null) {
                        listener.onAudioChunk(outputBuffer, bufferInfo, sampleRate, channelCount);
                    }
                    mTotalBytesDecoded += bufferInfo.size;
                }

                decoder.releaseOutputBuffer(outIndex, false);
                
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    isOutputEOS = true;
                }
                
                // Break if we decoded far enough ahead of the video frame
                // We don't have a fixed 15.5s hard limit here anymore since it's dynamic
                if (bufferInfo.presentationTimeUs >= targetTimeUs) {
                    break;
                }
            }
        }
    }

    public void release() {
        if (decoder != null) {
            decoder.stop();
            decoder.release();
            decoder = null;
        }
        if (extractor != null) {
            extractor.release();
            extractor = null;
        }
        if (tempAudioFile != null && tempAudioFile.exists()) {
            tempAudioFile.delete();
            tempAudioFile = null;
        }
    }
}
