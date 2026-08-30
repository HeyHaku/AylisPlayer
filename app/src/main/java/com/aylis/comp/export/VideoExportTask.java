package com.aylis.comp.export;

import android.content.Context;
import android.graphics.PointF;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.aylis.comp.visual.core.Elements.RootElement;
import com.aylis.comp.visual.core.Graphic.RendererCore;
import com.aylis.comp.visual.core.InternalVisualizationDataProvider;
import com.aylis.comp.visual.core.playback.AudioFrameData;
import com.aylis.comp.visual.core.playback.exo.ExoVisualizerDataProvider;
import com.aylis.comp.AlbumArt.AlbumArtRequest;
import com.aylis.comp.AlbumArt.ImageLoadedListener;

import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VideoExportTask {

    public interface ExportListener {
        void onDownloadProgress(long downloadedBytes, long totalBytes);
        void onProgress(int progress);
        void onFinished(String path);
        void onError(String error);
    }

    private Context context;
    private ExportListener listener;
    private String trackName;
    private String outputFolderUri;
    private int width, height, fps, bitRate, startSec, endSec;
    private volatile boolean isCancelled = false;

    public VideoExportTask(Context context, String trackName, String outputFolderUri, int width, int height, int fps, int bitrate, int startSec, int endSec, ExportListener listener) {
        this.context = context;
        this.trackName = (trackName != null && !trackName.isEmpty()) ? trackName : "Export";
        this.outputFolderUri = outputFolderUri;
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.bitRate = bitrate;
        this.startSec = startSec;
        this.endSec = endSec;
        this.listener = listener;
    }

    public void cancel() {
        isCancelled = true;
    }

    public void start(Uri audioUri, String themeJson) {
        new Thread(() -> {
            VideoEncoderCore videoEncoder = null;
            AudioEncoderCore audioEncoder = null;
            MuxerWrapper muxerWrapper = null;
            EglCore eglCore = null;
            WindowSurface windowSurface = null;
            RendererCore rendererCore = null;
            AudioDecoderCore audioDecoder = null;

            try {
                // Sanitize track name: keep only safe characters
                String safeName = trackName.replaceAll("[^a-zA-Zа-яА-ЯёЁ0-9 _-]", "").trim();
                if (safeName.isEmpty()) safeName = "Export";

                // Format: TrackName, 2024-07-26, 14-35
                String timestamp = new SimpleDateFormat("yyyy-MM-dd, HH-mm", Locale.getDefault()).format(new Date());
                String fileName = safeName + ", " + timestamp + ".mp4";

                File outputFile;
                boolean useSaf = outputFolderUri != null && !outputFolderUri.isEmpty();
                
                if (useSaf) {
                    outputFile = new File(context.getCacheDir(), fileName);
                } else {
                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
                    if (!downloadsDir.exists()) downloadsDir.mkdirs();
                    outputFile = new File(downloadsDir, fileName);
                }

                int durationSecs = endSec - startSec;
                if (durationSecs <= 0) durationSecs = 15;
                int totalFrames = fps * durationSecs;

                MediaMuxer muxer = new MediaMuxer(outputFile.toString(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                muxerWrapper = new MuxerWrapper(muxer, 2);

                int videoBitRate = bitRate;
                videoEncoder = new VideoEncoderCore(width, height, videoBitRate, fps, muxerWrapper);

                eglCore = new EglCore();
                windowSurface = new WindowSurface(eglCore, videoEncoder.getInputSurface(), true);
                windowSurface.makeCurrent();

                audioDecoder = new AudioDecoderCore(context, audioUri, startSec, listener);

                int audioSampleRate = audioDecoder.getSampleRate();
                int audioChannelCount = audioDecoder.getChannelCount();
                int audioBitRate = bitRate > 10000000 ? 256000 : 128000;
                long maxPtsUs = (endSec * 1000000L) + 500000L;

                audioEncoder = new AudioEncoderCore(audioSampleRate, audioChannelCount, audioBitRate, maxPtsUs);
                audioEncoder.setMuxerWrapper(muxerWrapper);

                // 1. ПРЕДВАРИТЕЛЬНОЕ ДЕКОДИРОВАНИЕ (OFFLINE RENDER ARCHITECTURE)
                final java.util.List<byte[]> pcmChunks = new java.util.ArrayList<>();
                final int[] totalPcmBytes = {0};
                
                audioDecoder.decodeUntil(maxPtsUs, new AudioDecoderCore.AudioChunkListener() {
                    @Override
                    public void onAudioChunk(ByteBuffer buffer, MediaCodec.BufferInfo info, int sampleRate, int channelCount) {
                        int size = buffer.remaining();
                        if (size > 0) {
                            byte[] chunk = new byte[size];
                            buffer.get(chunk);
                            pcmChunks.add(chunk);
                            totalPcmBytes[0] += size;
                        }
                    }
                });
                
                byte[] allPcmDataRaw = new byte[totalPcmBytes[0]];
                int offset = 0;
                for (byte[] chunk : pcmChunks) {
                    System.arraycopy(chunk, 0, allPcmDataRaw, offset, chunk.length);
                    offset += chunk.length;
                }
                pcmChunks.clear();
                
                long targetSampleCount = (long) durationSecs * audioSampleRate * audioChannelCount;
                int targetByteLength = (int) (targetSampleCount * 2);
                final byte[] allPcmData = new byte[targetByteLength];
                if (allPcmDataRaw.length < targetByteLength) {
                    System.arraycopy(allPcmDataRaw, 0, allPcmData, 0, allPcmDataRaw.length);
                } else {
                    System.arraycopy(allPcmDataRaw, 0, allPcmData, 0, targetByteLength);
                }

                final long[] currentRenderPtsUs = new long[]{startSec * 1000000L};

                final com.aylis.comp.visual.core.Elements.Base.MeasureLogic measureLogic = new com.aylis.comp.visual.core.Elements.Base.MeasureLogic(
                    new com.aylis.comp.visual.design.HandheldMotion(com.aylis.comp.visual.design.HandheldMotion.Jarles_Presets_MoreMovement_Smoothest),
                    new com.aylis.comp.visual.design.HandheldMotion(com.aylis.comp.visual.design.HandheldMotion.Jarles_Presets_LotsOshake),
                    new android.graphics.PointF(1f, 1f)
                );

                InternalVisualizationDataProvider proxyProvider = new InternalVisualizationDataProvider() {
                    @Override
                    public AudioFrameData onRequestSoundVisualizationData(AudioFrameData outResult) {
                        outResult.valid = true;
                        outResult.sampleRate = audioSampleRate;
                        
                        long frameTimeUs = currentRenderPtsUs[0] - (startSec * 1000000L);
                        if (frameTimeUs < 0) frameTimeUs = 0;
                        
                        int centerSample = (int) ((frameTimeUs * audioSampleRate) / 1000000L);
                        int outLength = outResult.pcmBuffer.length;
                        int startSample = centerSample - (outLength / 2);
                        
                        float sumSquares = 0.0f;
                        
                        for (int k = 0; k < outLength; k++) {
                            int sIdx = startSample + k;
                            int bytePos = sIdx * audioChannelCount * 2;
                            if (sIdx >= 0 && bytePos + (audioChannelCount * 2) <= allPcmData.length) {
                                short sample;
                                if (audioChannelCount == 1) {
                                    int low = allPcmData[bytePos] & 0xFF;
                                    int high = allPcmData[bytePos + 1];
                                    sample = (short) ((high << 8) | low);
                                } else {
                                    int lLow = allPcmData[bytePos] & 0xFF;
                                    int lHigh = allPcmData[bytePos + 1];
                                    short left = (short) ((lHigh << 8) | lLow);

                                    int rLow = allPcmData[bytePos + 2] & 0xFF;
                                    int rHigh = allPcmData[bytePos + 3];
                                    short right = (short) ((rHigh << 8) | rLow);

                                    sample = (short) ((left + right) / 2);
                                }
                                outResult.pcmBuffer[k] = sample;
                                sumSquares += (sample * sample);
                            } else {
                                outResult.pcmBuffer[k] = 0;
                            }
                        }
                        
                        outResult.rms = (float) Math.sqrt(sumSquares / outLength) / 32768.0f;
                        return outResult;
                    }
                    @Override
                    public String onRequestsMeasureText(String val) { 
                        if ("$trackPositionMs".equals(val) || "{trackPositionMs}".equals(val)) {
                            return String.valueOf(currentRenderPtsUs[0] / 1000L);
                        }
                        return com.aylis.comp.visual.core.VisualizerViewCore.onRequestMeasureText.invoke(val, null, val); 
                    }
                    @Override
                    public PointF onRequestMeasureVec2f(String val, PointF argVec, PointF lastMeasured, Float frameDataRmsValue) {
                        return measureLogic.process(val, argVec, lastMeasured, frameDataRmsValue);
                    }
                    @Override
                    public AlbumArtRequest onRequestsAlbumArtPath() {
                        return com.aylis.comp.visual.core.VisualizerViewCore.onRequestsAlbumArtPath.invoke(null);
                    }
                    @Override
                    public void onRequestAlbumArtPathAndBitmap(ImageLoadedListener loadedListener, Integer targetBoundsWidth, Integer targetBoundsHeight, AlbumArtRequest albumartRequest) {
                        com.aylis.comp.visual.core.VisualizerViewCore.onRequestAlbumArtPathAndBitmap.invoke(loadedListener, targetBoundsWidth, targetBoundsHeight, albumartRequest);
                    }
                };

                rendererCore = new RendererCore(context.getResources(), proxyProvider, true);
                rendererCore.onSurfaceCreated(null, null);
                rendererCore.onSurfaceChanged(null, width, height);
                float frameTimeF = 1.0f / fps;
                rendererCore.setOverrideFrameTime(frameTimeF);

                try {
                    RootElement root = new RootElement(0);
                    if (themeJson != null && !themeJson.isEmpty()) {
                        com.aylis.comp.visual.scene.VisualizerScene scene = com.aylis.comp.visual.scene.SceneSerializer.INSTANCE.fromJson(themeJson);
                        if (scene != null) {
                            root = com.aylis.comp.visual.scene.SceneBuilder.INSTANCE.buildFromScene(0, scene);
                        }
                    }
                    rendererCore.setThemeElements(root);
                } catch (Exception e) {}

                final AudioEncoderCore finalAudioEncoder = audioEncoder;
                long audioBytesSentToEncoder = 0;

                try {
                    Thread.sleep(1500); // Give ImageFactory time to load textures
                } catch (InterruptedException e) {}

                for (int i = 0; i < totalFrames; i++) {
                    if (isCancelled) break;

                    long currentRenderTimeUs = (startSec * 1000000L) + ((i * 1000000L) / fps);
                    long currentEncoderTimeUs = (i * 1000000L) / fps;
                    currentRenderPtsUs[0] = currentRenderTimeUs;

                    // 2. ИДЕАЛЬНОЕ МУЛЬТИПЛЕКСИРОВАНИЕ (АУДИО ЧАНКИ ДЛЯ ТЕКУЩЕГО КАДРА)
                    long targetAudioBytes = (i * (long)audioSampleRate * audioChannelCount * 2) / fps;
                    int frameSize = audioChannelCount * 2;
                    targetAudioBytes = (targetAudioBytes / frameSize) * frameSize;

                    int bytesToSend = (int)(targetAudioBytes - audioBytesSentToEncoder);
                    if (bytesToSend > 0) {
                        if (audioBytesSentToEncoder + bytesToSend > allPcmData.length) {
                            bytesToSend = allPcmData.length - (int)audioBytesSentToEncoder;
                        }
                        if (bytesToSend > 0) {
                            ByteBuffer pcmForEncoder = ByteBuffer.wrap(allPcmData, (int)audioBytesSentToEncoder, bytesToSend);
                            finalAudioEncoder.encodeChunk(pcmForEncoder, currentEncoderTimeUs);
                            audioBytesSentToEncoder += bytesToSend;
                        }
                    }

                    AudioFrameData dummyData = AudioFrameData.createReuse(null, 2048);
                    proxyProvider.onRequestSoundVisualizationData(dummyData);
                    float currentRms = dummyData.rms;

                    measureLogic.setUseFixedDeltaTime(true);
                    measureLogic.updatePlaybackState(currentRenderTimeUs / 1000000.0f, durationSecs, true);
                    measureLogic.updateTimeWithDt(1.0f / fps, currentRms);

                    rendererCore.onDrawFrame(null);

                    windowSurface.setPresentationTime(currentEncoderTimeUs * 1000L);
                    windowSurface.swapBuffers();

                    videoEncoder.drainEncoder(false);

                    if (listener != null && i % (fps / 2) == 0) {
                        final int progress = (int)((i / (float)totalFrames) * 100);
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> listener.onProgress(progress));
                    }
                }

                Log.d("ExportDebug", "1. Frame loop finished");
                if (!isCancelled) {
                    if (audioBytesSentToEncoder < allPcmData.length) {
                        int remaining = allPcmData.length - (int)audioBytesSentToEncoder;
                        long finalPtsUs = (totalFrames * 1000000L) / fps;
                        ByteBuffer pcmForEncoder = ByteBuffer.wrap(allPcmData, (int)audioBytesSentToEncoder, remaining);
                        finalAudioEncoder.encodeChunk(pcmForEncoder, finalPtsUs);
                    }
                    videoEncoder.drainEncoder(true);
                    Log.d("ExportDebug", "2. Video drain finished");
                    audioEncoder.drainEncoder(true);
                    Log.d("ExportDebug", "3. Audio drain finished");
                }
                
                try {
                    if (muxerWrapper.isStarted()) {
                        muxer.stop();
                    }
                } catch (Exception ignored) {}
                
                try {
                    muxer.release();
                } catch (Exception ignored) {}
                Log.d("ExportDebug", "4. Muxer released successfully");

                if (!isCancelled && listener != null) {
                    String finalPath = outputFile.getAbsolutePath();
                    if (useSaf) {
                        try {
                            androidx.documentfile.provider.DocumentFile dir = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, Uri.parse(outputFolderUri));
                            if (dir != null) {
                                androidx.documentfile.provider.DocumentFile newFile = dir.createFile("video/mp4", fileName);
                                if (newFile != null) {
                                    java.io.InputStream in = new java.io.FileInputStream(outputFile);
                                    java.io.OutputStream out = context.getContentResolver().openOutputStream(newFile.getUri());
                                    byte[] buf = new byte[8192];
                                    int len;
                                    while ((len = in.read(buf)) > 0) {
                                        out.write(buf, 0, len);
                                    }
                                    in.close();
                                    out.close();
                                    finalPath = newFile.getUri().toString();
                                }
                            }
                        } catch (Exception e) {
                            Log.e("Export", "Error copying to SAF", e);
                        } finally {
                            outputFile.delete(); // Delete temp file
                        }
                    }
                    if (listener != null) {
                        final String fp = finalPath;
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> listener.onFinished(fp));
                    }
                }
            } catch (Exception e) {
                Log.e("Export", "Error", e);
                if (listener != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> listener.onError(e.getMessage()));
                }
            } finally {
                if (windowSurface != null) windowSurface.release();
                if (eglCore != null) eglCore.release();
                if (videoEncoder != null) videoEncoder.release();
                if (audioEncoder != null) audioEncoder.release();
                if (audioDecoder != null) audioDecoder.release();
            }
        }).start();
    }
}
