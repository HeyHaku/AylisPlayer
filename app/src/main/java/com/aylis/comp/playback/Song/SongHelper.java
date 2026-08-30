package com.aylis.comp.playback.Song;

import android.net.Uri;

public class SongHelper {
    public static PlaylistSong.Data createData(long audioId, String dataSource, String title, String artist, int duration) {
        PlaylistSong.Data data = new PlaylistSong.Data(Uri.parse(dataSource != null ? dataSource : ""));
        data.audioId = audioId;
        data.trackName = title != null ? title : "";
        data.artistName = artist != null ? artist : "";
        data.duration = duration;
        return data;
    }
}
