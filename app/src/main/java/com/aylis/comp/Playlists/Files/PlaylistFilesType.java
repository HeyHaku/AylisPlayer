

package com.aylis.comp.Playlists.Files;

public class PlaylistFilesType {

    public static PlaylistFilesType[] playlistFilesTypes = new PlaylistFilesType[]{
            new PlaylistFilesType("PLA Playlist", 1, "pla", true),

            new PlaylistFilesType("PLS Playlist", 3, "pls", true),
            new PlaylistFilesType("MPCPL Playlist", 4, "mpcpl", true),
            new PlaylistFilesType("PLP Playlist", 5, "plp", true),

            new PlaylistFilesType("M3U Playlist", 6, "m3u", true),
            new PlaylistFilesType("M3U8 Playlist", 7, "m3u8", true),

    };

    public final String name;
    public final String fileExtension;
    public final boolean supportSaving;

    public PlaylistFilesType(String name, int typeId, String ext, boolean supportSaving) {
        this.name = name;
        this.fileExtension = ext;
        this.supportSaving = supportSaving;
    }

    public static boolean isPlaylistFileExtension(String ext) {

        if (ext.equals("pla")) return true;

        if (ext.equals("pls")) return true;

        if (ext.equals("mpcpl")) return true;

        if (ext.equals("plp")) return true;

        if (ext.equals("m3u")) return true;
        if (ext.equals("m3u8")) return true;
        if (ext.equals("m4u")) return true;
        if (ext.equals("ram")) return true;

        return false;
    }
}
