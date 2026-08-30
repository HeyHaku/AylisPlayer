package com.aylis.comp.playback.ExoMediaPlayer;

import android.net.Uri;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.ResolvingDataSource;
import java.io.IOException;
import android.content.Context;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class YoutubeResolver implements ResolvingDataSource.Resolver {
    private static final ConcurrentHashMap<String, Uri> cache = new ConcurrentHashMap<>();
    private static final ExecutorService preResolveExecutor = Executors.newFixedThreadPool(2);
    private Context context;

    public YoutubeResolver(Context context) {
        this.context = context;
    }

    public static void preResolve(Context context, String uriStr) {
        if (uriStr == null || !uriStr.startsWith("ytsearch://"))
            return;
        final Uri uri = Uri.parse(uriStr);
        String videoIdRaw = uri.getHost();
        if (videoIdRaw == null)
            videoIdRaw = uri.getLastPathSegment();
        final String videoId = videoIdRaw;

        if (videoId == null || cache.containsKey(videoId))
            return;

        preResolveExecutor.submit(() -> {
            try {
                org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor extractor = (org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor) org.schabi.newpipe.extractor.NewPipe
                        .getService(org.schabi.newpipe.extractor.ServiceList.YouTube.getServiceId())
                        .getStreamExtractor("https://youtube.com/watch?v=" + videoId);

                extractor.fetchPage();

                java.util.List<org.schabi.newpipe.extractor.stream.AudioStream> audioStreams = extractor
                        .getAudioStreams();
                if (audioStreams != null && !audioStreams.isEmpty()) {
                    org.schabi.newpipe.extractor.stream.AudioStream bestAudio = audioStreams.get(0);
                    for (org.schabi.newpipe.extractor.stream.AudioStream stream : audioStreams) {
                        if (stream.getAverageBitrate() > bestAudio.getAverageBitrate()) {
                            bestAudio = stream;
                        }
                    }
                    Uri resolvedUri = Uri.parse(bestAudio.getContent());
                    cache.put(videoId, resolvedUri);
                    ExoCacheManager.preCache(context, resolvedUri);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static Uri resolveSync(Uri uri) {
        if (uri == null || !"ytsearch".equals(uri.getScheme()))
            return uri;
        String videoId = uri.getHost();
        if (videoId == null)
            videoId = uri.getLastPathSegment();
        if (videoId != null && cache.containsKey(videoId)) {
            return cache.get(videoId);
        }
        try {
            org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor extractor = (org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor) org.schabi.newpipe.extractor.NewPipe
                    .getService(org.schabi.newpipe.extractor.ServiceList.YouTube.getServiceId())
                    .getStreamExtractor("https://youtube.com/watch?v=" + videoId);

            extractor.fetchPage();

            java.util.List<org.schabi.newpipe.extractor.stream.AudioStream> audioStreams = extractor.getAudioStreams();
            if (audioStreams != null && !audioStreams.isEmpty()) {
                org.schabi.newpipe.extractor.stream.AudioStream bestAudio = audioStreams.get(0);
                for (org.schabi.newpipe.extractor.stream.AudioStream stream : audioStreams) {
                    if (stream.getAverageBitrate() > bestAudio.getAverageBitrate()) {
                        bestAudio = stream;
                    }
                }
                Uri resolvedUri = Uri.parse(bestAudio.getContent());
                if (videoId != null)
                    cache.put(videoId, resolvedUri);
                return resolvedUri;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uri;
    }

    @Override
    public DataSpec resolveDataSpec(DataSpec dataSpec) throws IOException {
        Uri uri = dataSpec.uri;
        if ("ytsearch".equals(uri.getScheme())) {
            Uri resolved = resolveSync(uri);
            if (resolved != uri) {
                return dataSpec.withUri(resolved);
            }
            throw new IOException("Failed to resolve YouTube stream for " + uri);
        }
        return dataSpec;
    }
}
