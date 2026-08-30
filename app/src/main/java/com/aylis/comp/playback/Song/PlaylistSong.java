
package com.aylis.comp.playback.Song;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import com.aylis.Common.MediaStoreUtils;
import com.aylis.PlayerCore;
import com.aylis.R;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class PlaylistSong extends AsyncTask<Object, Integer, PlaylistSong.Data> {

    public static final Data emptyData = new Data(null);
    private static final Data notsetData = new Data(null);
    private static final Data loadingData = new Data(null);
    public static final PlaylistSong EmptySong = new PlaylistSong(emptyData);

    private static final String[] mCursorCols = new String[] {
            "audio._id AS _id",
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.IS_PODCAST,
            MediaStore.Audio.Media.BOOKMARK
    };

    private final static int IDCOLIDX = 0;
    private final static int PODCASTCOLIDX = 8;
    private final static int BOOKMARKCOLIDX = 9;
    private static PlaylistSongOwnSerialExecutor playlistSongOwnSerialExecutor = new PlaylistSongOwnSerialExecutor();
    private static String unknown_artist_name = null;
    private static String unknown_album_name = null;
    private Uri idPath = Uri.EMPTY;
    private String providedTitle;
    private String providedArtist;
    private int providedDuration; // in ms
    private String providedThumbnail;
    private volatile Data songData;
    private volatile AsyncLoadingData asyncLoadingData = null;

    private PlaylistSong(Data data) {
        songData = data;
    }

    public PlaylistSong(long audioId, Uri uri) {
        init(audioId, uri, null);
    }

    public PlaylistSong(File ff) {
        init(-1, Uri.fromFile(ff), null);
    }

    public PlaylistSong(long audioId, String path) {
        init(audioId, Uri.parse(path), null);
    }

    public PlaylistSong(long audioId, String path, String providedTitle, String providedArtist, int providedDuration,
            String providedThumbnail) {
        init(audioId, Uri.parse(path), providedTitle, providedArtist, providedDuration, providedThumbnail);
    }

    public PlaylistSong(long audioId, String path, String providedTitle, String subtitlePath) {
        init(audioId, Uri.parse(path), providedTitle);
    }

    public static List<PlaylistSong> makeSongListFromDataSourceList(List<String> dataSources) {
        if (dataSources == null)
            return null;

        List<PlaylistSong> songs = new ArrayList<>(dataSources.size());

        for (String s : dataSources) {
            songs.add(new PlaylistSong(-1, s));
        }

        return songs;
    }

    static String getPathFromFile(File file) {
        try {
            return file.getCanonicalPath();
        } catch (Exception ex) {
            return "";
        }
    }

    private static Data AcquireDataMediaStore(Context context, Uri dataSourceUri) {
        if (unknown_artist_name == null)
            unknown_artist_name = context.getString(R.string.unknown_artist_name);
        if (unknown_album_name == null)
            unknown_album_name = context.getString(R.string.unknown_album_name);

        if (context != null && dataSourceUri != null) {
            String realPath = com.aylis.Common.MediaStoreUtils.getRealFilePath(context, dataSourceUri);
            if (realPath != null) {
                dataSourceUri = Uri.parse(realPath);
            }
        }

        Data _data = new Data(dataSourceUri);

        if (dataSourceUri == null || dataSourceUri == Uri.EMPTY) {
            return _data;
        }

        ContentResolver resolver = context.getContentResolver();
        Uri uri;
        String where;
        String selectionArgs[];
        Cursor cursor = null;

        String trackname2 = null;
        String trackSecondName = null;

        if ("content".equals(dataSourceUri.getScheme())) {
            uri = dataSourceUri;
            cursor = MediaStoreUtils.querySafe(resolver, uri, mCursorCols, null, null, null);

        } else if ("http".equals(dataSourceUri.getScheme()) || "https".equals(dataSourceUri.getScheme())) {
            trackname2 = dataSourceUri.toString();

            String uriPath = dataSourceUri.getPath();
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

            String pathInMediaStore = dataSourceUri.getPath();
            uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            where = MediaStore.Audio.Media.DATA + "=?";
            selectionArgs = new String[] { pathInMediaStore };

            cursor = MediaStoreUtils.querySafe(resolver, uri, mCursorCols, where, selectionArgs, null);

            if (cursor == null || cursor.getCount() <= 0) {
                if (cursor != null) {
                    cursor.close();
                }

                where = MediaStore.Audio.Media.DATA + "=? COLLATE NOCASE";
                cursor = MediaStoreUtils.querySafe(resolver, uri, mCursorCols, where, selectionArgs, null);
            }

            if ((cursor == null || cursor.getCount() <= 0)) {

                if ("file".equals(dataSourceUri.getScheme()) ||
                        dataSourceUri.getScheme() == null)

                {
                    String canonicalPath = null;
                    try {
                        File file = new File(pathInMediaStore);
                        canonicalPath = file.getCanonicalPath();
                    } catch (IOException ignored) {
                    }

                    if (canonicalPath != null) {

                        if (cursor != null) {
                            cursor.close();
                        }

                        selectionArgs = new String[] { canonicalPath };
                        where = MediaStore.Audio.Media.DATA + "=?";
                        cursor = MediaStoreUtils.querySafe(resolver, uri, mCursorCols, where, selectionArgs, null);
                    }
                }
            }
        }

        if (cursor != null && cursor.getCount() > 0) {

            cursor.moveToFirst();

            _data.audioId = cursor.getLong(IDCOLIDX);
            _data.trackName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));

            _data.albumName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
            _data.albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
            _data.artistName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
            _data.artistId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID));
            _data.duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
            _data.isPodcast = (cursor.getInt(PODCASTCOLIDX) > 0);
            _data.bookmark = cursor.getLong(BOOKMARKCOLIDX);
            _data.trackNum = 0;
            _data.cdNum = 0;
            _data.year = 0;
            _data.sizeInBytes = 0;

            if (MediaStore.UNKNOWN_STRING.equals(_data.artistName)) {

                _data.artistName = unknown_artist_name;
                _data.artistId = -1;
            }

            if (MediaStore.UNKNOWN_STRING.equals(_data.albumName)) {
                _data.albumName = unknown_album_name;
                _data.albumId = -1;
            }

        } else {

            if (trackname2 == null)
                trackname2 = dataSourceUri.getLastPathSegment();
            if (trackname2 == null)
                trackname2 = dataSourceUri.toString();
            if (trackname2 == null || trackname2.equals("/") || trackname2.startsWith("file://"))
                trackname2 = "unknown";

            _data.audioId = -1;
            _data.trackName = trackname2;

            _data.albumName = unknown_album_name;
            _data.albumId = -1;

            if (trackSecondName == null) {
                _data.artistName = unknown_artist_name;
                _data.artistId = -1;
            } else {
                _data.artistName = trackSecondName;
                _data.artistId = -2;
            }
            _data.isPodcast = false;
            _data.bookmark = -1;

            android.media.MediaMetadataRetriever metadataRetriever = new android.media.MediaMetadataRetriever();
            boolean metadataRetrieverSet = false;
            try {
                String scheme = dataSourceUri.getScheme();
                if (scheme == null || "file".equals(scheme)) {
                    String filePath = dataSourceUri.getPath() != null ? dataSourceUri.getPath()
                            : dataSourceUri.toString();
                    java.io.File file = new java.io.File(filePath);
                    java.io.FileInputStream fis = new java.io.FileInputStream(file);
                    metadataRetriever.setDataSource(fis.getFD());
                    fis.close();
                } else if ("content".equals(scheme)) {
                    android.os.ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(dataSourceUri,
                            "r");
                    if (pfd != null) {
                        metadataRetriever.setDataSource(pfd.getFileDescriptor());
                        pfd.close();
                    } else {
                        metadataRetriever.setDataSource(context, dataSourceUri);
                    }
                } else if (!"ytsearch".equals(scheme)) {
                    metadataRetriever.setDataSource(context, dataSourceUri);
                }
                if (!"ytsearch".equals(scheme)) {
                    metadataRetrieverSet = true;
                }
            } catch (Exception ignored) {
            }

            if (metadataRetrieverSet) {
                try {
                    String title = metadataRetriever
                            .extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE);
                    if (title != null && !title.isEmpty()) {
                        _data.trackName = title;
                    }
                    String artist = metadataRetriever
                            .extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST);
                    if (artist != null && !artist.isEmpty()) {
                        _data.artistName = artist;
                        _data.artistId = -2;
                    }
                    String durationStr = metadataRetriever
                            .extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
                    _data.duration = com.aylis.Common.Utils.strToIntSafe(durationStr);
                } catch (Exception ignored) {
                } finally {
                    try {
                        metadataRetriever.release();
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        if (_data.duration <= 0) {
            _data.duration = getDurationWithExtractor(context, dataSourceUri);
        }

        if (_data.trackName == null)
            _data.trackName = "unknown";

        if (_data.albumName == null)
            _data.albumName = unknown_album_name;

        if (_data.artistName == null)
            _data.artistName = unknown_artist_name;

        if (cursor != null)
            cursor.close();

        return _data;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PlaylistSong && this.compare((PlaylistSong) o);
    }

    @Override
    public int hashCode() {
        return idPath.hashCode();
    }

    public void init(long audioId, final Uri path, String providedTitle, String providedArtist, int providedDuration,
            String providedThumbnail) {
        init(audioId, path, providedTitle);
        this.providedArtist = providedArtist;
        this.providedDuration = providedDuration;
        this.providedThumbnail = providedThumbnail;
    }

    public void init(long audioId, final Uri path, String providedTitle) {
        this.idPath = path;
        this.providedTitle = providedTitle;

        songData = notsetData;
    }

    private void startAcquireData() {
        Context context = PlayerCore.s().getAppContext();
        if (context == null) {
            return;
        }

        songData = loadingData;

        this.executeOnExecutor(playlistSongOwnSerialExecutor, context, idPath);
    }

    public boolean compare(PlaylistSong b) {
        return b != null && b.idPath.compareTo(idPath) == 0;
    }

    public String getDataSourceForPlaylist() {
        return idPath.toString();
    }

    public long getDataSourceForNativePlaylist() {
        Context context = PlayerCore.s().getAppContext();
        if (context == null)
            return 0;

        Data data = this.getDataBlocking(context);

        if (data.audioId > 0)
            return data.audioId;
        else {
            return 0;
        }
    }

    public String getConstrucPath() {
        return idPath.toString();
    }

    public String getProvidedTitle() {
        return providedTitle;
    }

    public String getProvidedArtist() {
        return providedArtist;
    }

    public int getProvidedDuration() {
        return providedDuration;
    }

    public String getProvidedThumbnail() {
        return providedThumbnail;
    }

    public String getResolvedTitle() {
        if (providedTitle != null && !providedTitle.isEmpty())
            return providedTitle;
        if (songData != null && songData != emptyData && songData != notsetData && songData != loadingData)
            return songData.trackName;
        return "";
    }

    public String getResolvedArtist() {
        if (providedArtist != null && !providedArtist.isEmpty())
            return providedArtist;
        if (songData != null && songData != emptyData && songData != notsetData && songData != loadingData)
            return songData.artistName;
        return "";
    }

    public int getResolvedDuration() {
        if (providedDuration > 0)
            return providedDuration;
        if (songData != null && songData != emptyData && songData != notsetData && songData != loadingData)
            return songData.duration;
        return 0;
    }

    public String getResolvedThumbnail() {
        if (providedThumbnail != null && !providedThumbnail.isEmpty())
            return providedThumbnail;
        if (songData != null && songData != emptyData && songData != notsetData && songData != loadingData)
            return songData.thumbnail;
        return "";
    }

    public Uri getUri() {
        return idPath;
    }

    public IMediaDataSource getMediaDataSource() {
        return new MediaDataSourceOther(idPath);
    }

    public Data getData(OnDataReadyListener listener, Object userData1, Object userData2) {
        Data data = songData;

        if (data == notsetData) {
            asyncLoadingData = new AsyncLoadingData(listener, userData1, userData2);
            startAcquireData();
            return emptyData;
        } else if (data == loadingData) {
            asyncLoadingData = new AsyncLoadingData(listener, userData1, userData2);
            return emptyData;
        } else {
            asyncLoadingData = null;
            return data;
        }
    }

    public Data getData() {
        Data data = songData;

        if (data == notsetData) {
            asyncLoadingData = null;
            startAcquireData();
            return emptyData;
        } else if (data == loadingData) {
            asyncLoadingData = null;
            return emptyData;
        } else {
            asyncLoadingData = null;
            return data;
        }
    }

    public Data getDataBlocking(Context context) {
        Data data = getData();
        if (data == emptyData || data == loadingData || data == notsetData) {
            data = acquireData(context, idPath);
            songData = data;
            return data;
        }

        return data;
    }

    public String getSearchArtist() {
        Data data = songData;
        if (data != null && data != notsetData && data != loadingData && data != emptyData) {
            return data.artistName != null ? data.artistName : "";
        }
        return "";
    }

    public String getSearchTitle() {
        Data data = songData;
        if (data != null && data != notsetData && data != loadingData && data != emptyData) {
            return data.trackName != null ? data.trackName : "";
        }
        if (providedTitle != null && !providedTitle.isEmpty()) {
            return providedTitle;
        }
        if (idPath != null) {
            String lastSegment = idPath.getLastPathSegment();
            if (lastSegment != null)
                return lastSegment;
            return idPath.toString();
        }
        return "";
    }

    public DataDetails getDataDetailsBlocking(Context context) {
        Data data = getData();
        if (data == emptyData || data == loadingData || data == notsetData) {
            data = acquireData(context, idPath);
        }

        return PlaylistSongMetadataRetriever.AcquireDataMediaMetadataRetrieverLocal2(context, idPath, data);
    }

    @Override
    protected Data doInBackground(Object... params) {

        Context context = (Context) params[0];
        Uri path = (Uri) params[1];

        try {

            Thread.sleep(1);
        } catch (InterruptedException ignored) {
        }

        return acquireData(context, path);
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {

    }

    @Override
    protected void onPostExecute(Data result) {

        AsyncLoadingData asyncLoadingData_ = asyncLoadingData;
        asyncLoadingData = null;
        songData = result;

        if (asyncLoadingData_ != null) {
            asyncLoadingData_.listener.onDataReady(songData, asyncLoadingData_.userData1, asyncLoadingData_.userData2);
        }

    }

    private Data acquireData(Context context, Uri uri) {
        Data data = AcquireDataMediaStore(context, uri);
        if (providedTitle != null && !providedTitle.isEmpty()) {
            data.trackName = providedTitle;
            if (providedArtist != null && !providedArtist.isEmpty()) {
                data.artistName = providedArtist;
            }
            if (providedDuration > 0) {
                data.duration = providedDuration * 1000;
            }
            if (providedThumbnail != null && !providedThumbnail.isEmpty()) {
                data.thumbnail = providedThumbnail;
            }
        }
        return data;
    }

    public interface OnDataReadyListener {
        void onDataReady(Data songData, Object userData1, Object userData2);
    }

    private static class PlaylistSongOwnSerialExecutor implements Executor {
        final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
        Runnable active;

        public synchronized void execute(@NonNull final Runnable r) {
            tasks.offer(new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } finally {
                        scheduleNext();
                    }
                }
            });
            if (active == null) {
                scheduleNext();
            }
        }

        protected synchronized void scheduleNext() {
            if ((active = tasks.poll()) != null) {
                THREAD_POOL_EXECUTOR.execute(active);
            }
        }
    }

    public static class Data {

        private static final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");

        public final Uri dataSource;
        public long audioId;
        public String trackName;
        public String albumName;
        public long albumId;
        public String artistName;
        public long artistId;
        public int duration;
        public boolean isPodcast;
        public long bookmark;
        public int width;
        public int height;
        public int trackNum;
        public int cdNum;
        public int year;
        public long sizeInBytes;
        public String thumbnail;

        Data(Uri dataSource) {
            this.dataSource = dataSource == null ? Uri.EMPTY : dataSource;
            this.thumbnail = "";
            audioId = 0;
            trackName = "";
            albumName = "";
            albumId = 0;
            artistName = "";
            artistId = -1;
            duration = 0;
            isPodcast = false;
            bookmark = 0;
            width = 0;
            height = 0;
            trackNum = 0;
            cdNum = 0;
            year = 0;
            sizeInBytes = 0;
        }

        public String getVideoThumbDataSourceAsStr() {
            if (thumbnail != null && !thumbnail.isEmpty())
                return thumbnail;
            return Uri.decode(dataSource.toString());
        }

        public boolean isArtistKnown() {
            return artistId > 0;
        }

        public boolean isAlbumKnown() {
            return artistId > 0;
        }

        public boolean isArtistKnownOrSecondName() {
            return artistId > 0 || artistId == -2;
        }

        public boolean hasValidArtistName() {
            if (isArtistKnownOrSecondName())
                return true;
            if (artistName == null || artistName.trim().isEmpty())
                return false;
            if (artistName.equals(unknown_artist_name))
                return false;
            return true;
        }

        public String getAlbumArtPath0Str() {
            if (audioId <= 0)
                return null;
            return "content://media/external/audio/media/" + audioId + "/albumart";
        }

        public String getAlbumArtPath1Str() {
            if (albumId <= 0)
                return null;
            Uri uri = ContentUris.withAppendedId(sArtworkUri, albumId);
            return Uri.decode(uri.toString());
        }

        public String getAlbumArtGenerateStr() {
            if (isArtistKnown() && artistName.length() > 1)
                return artistName;

            int indx1 = trackName.indexOf('-');
            if (indx1 < 3)
                indx1 = trackName.indexOf("_-_");

            if (indx1 < 3) {
                boolean haveSpaces = trackName.indexOf(' ') >= 0;
                if (haveSpaces) {
                    indx1 = trackName.indexOf('_');
                } else {
                    indx1 = trackName.indexOf("__");
                }
            }

            if (indx1 < 3)
                return trackName;

            if (trackName.charAt(indx1 - 1) == ' ')
                indx1--;

            return trackName.substring(0, indx1);

        }

        public int getDurationSeconds() {
            return duration / 1000;
        }
    }

    public static class DataDetails {
        public final Data data;

        public boolean isStream = false;
        public String source;
        public String secondName;
        public String trackName;
        public String albumName;
        public String artistName;
        public String albumArtist;
        public int duration;
        public int width;
        public int height;
        public int trackNum;
        public int cdNum;
        public int year;
        public int bitRate;
        public String composer;

        public DataDetails(Data data) {
            this.data = data;
        }

        public int getDuration() {
            return data.duration;
        }
    }

    class AsyncLoadingData {
        public final OnDataReadyListener listener;
        public final Object userData1;
        public final Object userData2;

        public AsyncLoadingData(OnDataReadyListener listener, Object userData1, Object userData2) {

            this.listener = listener;
            this.userData1 = userData1;
            this.userData2 = userData2;
        }
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