
package com.aylis.comp.playback.Song;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import com.aylis.Common.Utils;

public class PlaylistSongMetadataRetriever {

    public static PlaylistSong.DataDetails AcquireDataMediaMetadataRetrieverLocal2(Context context, Uri uri,
            PlaylistSong.Data simpleData) {
        if (context != null && uri != null) {
            String realPath = com.aylis.Common.MediaStoreUtils.getRealFilePath(context, uri);
            if (realPath != null) {
                uri = Uri.fromFile(new java.io.File(realPath));
            }
        }

        PlaylistSong.DataDetails _data = new PlaylistSong.DataDetails(simpleData);

        if (uri == null || uri == Uri.EMPTY) {
            return _data;
        }

        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        boolean metadataRetrieverSet = false;

        String trackSecondName = null;
        if ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) {
            String uriPath = uri.getPath();
            int len = uriPath.length();

            int doti = uriPath.lastIndexOf(".");
            if (doti < 0)
                doti = len - 1;
            int d1i = Math.max(uriPath.lastIndexOf('/', doti), 0) + 1;
            int d2i = uriPath.indexOf('/', doti);
            if (d2i < 0)
                d2i = (len - 1) + 1;

            try {
                trackSecondName = uriPath.substring(d1i, d2i);
                if (trackSecondName.length() < 2)
                    trackSecondName = null;
            } catch (Exception e) {
                trackSecondName = null;
            }

        } else {
            try {
                String scheme = uri.getScheme();
                if (scheme == null || "file".equals(scheme)) {
                    String filePath = uri.getPath() != null ? uri.getPath() : uri.toString();
                    java.io.File file = new java.io.File(filePath);
                    java.io.FileInputStream fis = new java.io.FileInputStream(file);
                    metadataRetriever.setDataSource(fis.getFD());
                    fis.close();
                } else if ("content".equals(scheme)) {
                    android.os.ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
                    if (pfd != null) {
                        metadataRetriever.setDataSource(pfd.getFileDescriptor());
                        pfd.close();
                    } else {
                        metadataRetriever.setDataSource(context, uri);
                    }
                } else if (!"ytsearch".equals(scheme)) {
                    metadataRetriever.setDataSource(context, uri);
                }
                if (!"ytsearch".equals(scheme)) {
                    metadataRetrieverSet = true;
                }
            } catch (Exception ignored) {
            }
        }

        if (metadataRetrieverSet) {
            _data.isStream = false;

            _data.trackName = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            _data.artistName = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);

            _data.albumName = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);

            _data.duration = Utils
                    .strToIntSafe(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            if (_data.duration <= 0) {
                _data.duration = getDurationWithExtractor(context, uri);
            }
            _data.trackNum = Utils.strToIntSafe(
                    metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER));
            _data.cdNum = Utils
                    .strToIntSafe(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER));
            _data.year = Utils
                    .strToIntSafe(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR));
            _data.albumArtist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST);

            _data.composer = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER);

            _data.bitRate = Utils
                    .strToIntSafe(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
            _data.width = Utils
                    .strToIntSafe(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            _data.height = Utils
                    .strToIntSafe(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));

        } else {
            _data.isStream = true;

            if (trackSecondName == null) {
                _data.secondName = "";
            } else {
                _data.secondName = trackSecondName;
            }
        }

        if (_data.trackName == null)
            _data.trackName = (simpleData.trackName != null ? simpleData.trackName : "");
        if (_data.albumName == null)
            _data.albumName = (simpleData.albumName != null ? simpleData.albumName : "");
        if (_data.artistName == null)
            _data.artistName = (simpleData.artistName != null ? simpleData.artistName : "");
        if (_data.albumArtist == null)
            _data.albumArtist = "";
        if (_data.composer == null)
            _data.composer = "";

        try {
            metadataRetriever.release();
        } catch (Exception ignored) {
        }

        return _data;
    }

    private static int getDurationWithExtractor(Context context, Uri uri) {
        if (uri == null || uri == Uri.EMPTY)
            return 0;
        String scheme = uri.getScheme();
        if ("http".equals(scheme) || "https".equals(scheme) || "ytsearch".equals(scheme))
            return 0;

        android.media.MediaExtractor extractor = new android.media.MediaExtractor();
        try {
            if (scheme == null || "file".equals(scheme)) {
                String filePath = uri.getPath() != null ? uri.getPath() : uri.toString();
                java.io.File file = new java.io.File(filePath);
                java.io.FileInputStream fis = new java.io.FileInputStream(file);
                extractor.setDataSource(fis.getFD());
                fis.close();
            } else if ("content".equals(scheme)) {
                android.os.ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
                if (pfd != null) {
                    extractor.setDataSource(pfd.getFileDescriptor());
                    pfd.close();
                } else {
                    extractor.setDataSource(context, uri, null);
                }
            } else {
                extractor.setDataSource(context, uri, null);
            }
            int numTracks = extractor.getTrackCount();
            for (int i = 0; i < numTracks; i++) {
                android.media.MediaFormat format = extractor.getTrackFormat(i);
                if (format.containsKey(android.media.MediaFormat.KEY_DURATION)) {
                    long durationUs = format.getLong(android.media.MediaFormat.KEY_DURATION);
                    return (int) (durationUs / 1000);
                }
            }
        } catch (Exception ignored) {
        } finally {
            try {
                extractor.release();
            } catch (Exception ignored) {
            }
        }
        return 0;
    }
}
