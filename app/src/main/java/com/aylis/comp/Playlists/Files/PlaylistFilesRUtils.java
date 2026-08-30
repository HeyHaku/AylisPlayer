

package com.aylis.comp.Playlists.Files;

import com.aylis.Common.Utils;
import com.aylis.comp.playback.Song.PlaylistSong;
import java.io.File;
import java.io.IOException;
import java.util.List;
import christophedelory.playlist.SpecificPlaylist;
import christophedelory.playlist.m3u.Resource;

public class PlaylistFilesRUtils {

    static int readFromSpecificPlaylist(SpecificPlaylist specificPlaylist, ReadParameters parameters, List<PlaylistSong> resultList) {
        if (specificPlaylist == null)
            return 0;

        if (specificPlaylist instanceof christophedelory.playlist.pla.PLA) {
            return readFromSpecificPlaylist((christophedelory.playlist.pla.PLA) specificPlaylist, parameters, resultList);
        }

        if (specificPlaylist instanceof christophedelory.playlist.kpl.Xml) {
            return readFromSpecificPlaylist((christophedelory.playlist.kpl.Xml) specificPlaylist, parameters, resultList);
        }

        if (specificPlaylist instanceof christophedelory.playlist.pls.PLS) {
            return readFromSpecificPlaylist((christophedelory.playlist.pls.PLS) specificPlaylist, parameters, resultList);
        }

        if (specificPlaylist instanceof christophedelory.playlist.mpcpl.MPCPL) {
            return readFromSpecificPlaylist((christophedelory.playlist.mpcpl.MPCPL) specificPlaylist, parameters, resultList);
        }

        if (specificPlaylist instanceof christophedelory.playlist.plp.PLP) {
            return readFromSpecificPlaylist((christophedelory.playlist.plp.PLP) specificPlaylist, parameters, resultList);
        }

        if (specificPlaylist instanceof christophedelory.playlist.m3u.M3U) {
            return readFromSpecificPlaylist((christophedelory.playlist.m3u.M3U) specificPlaylist, parameters, resultList);
        }

        return 0;
    }

    static int readFromSpecificPlaylist(christophedelory.playlist.pla.PLA specificPlaylist, ReadParameters parameters, List<PlaylistSong> resultList) {

        List<String> list = specificPlaylist.getFilenames();

        for (String item : list) {

            item = Utils.fixIncludedNullTerminatorString(item);
            resultList.add(makePlaylistSong(-1, item, parameters));
        }

        return list.size();
    }

    static int readFromSpecificPlaylist(christophedelory.playlist.kpl.Xml specificPlaylist, ReadParameters parameters, List<PlaylistSong> resultList) {

        List<christophedelory.playlist.kpl.Entry> list = specificPlaylist.getEntries();

        for (christophedelory.playlist.kpl.Entry item : list) {
            resultList.add(makePlaylistSong(-1, item.getFilename(), parameters));
        }

        return list.size();
    }

    static int readFromSpecificPlaylist(christophedelory.playlist.pls.PLS specificPlaylist, ReadParameters parameters, List<PlaylistSong> resultList) {
        List<Resource> resourcesList = specificPlaylist.getResources();

        for (Resource r : resourcesList) {
            resultList.add(makePlaylistSong(-1, r.getLocation(), r.getName(), null, parameters));
        }

        return resourcesList.size();
    }

    static int readFromSpecificPlaylist(christophedelory.playlist.mpcpl.MPCPL specificPlaylist, ReadParameters parameters, List<PlaylistSong> resultList) {
        List<christophedelory.playlist.mpcpl.Resource> resourcesList = specificPlaylist.getResources();

        for (christophedelory.playlist.mpcpl.Resource r : resourcesList) {
            resultList.add(makePlaylistSong(-1, r.getFilename(), null, r.getSubtitle(), parameters));
        }

        return resourcesList.size();
    }

    static int readFromSpecificPlaylist(christophedelory.playlist.plp.PLP specificPlaylist, ReadParameters parameters, List<PlaylistSong> resultList) {
        List<String> list = specificPlaylist.getFilenames();

        for (String r : list) {
            resultList.add(makePlaylistSong(-1, r, parameters));
        }

        return list.size();
    }

    static int readFromSpecificPlaylist(christophedelory.playlist.m3u.M3U specificPlaylist, ReadParameters parameters, List<PlaylistSong> resultList) {
        specificPlaylist.setExtensionM3U(true);

        List<Resource> resourcesList = specificPlaylist.getResources();

        for (Resource r : resourcesList) {
            resultList.add(makePlaylistSong(-1, r.getLocation(), r.getName(), null, parameters));
        }

        return resourcesList.size();
    }

    static PlaylistSong makePlaylistSong(long audioId, String path, ReadParameters parameters) {
        return new PlaylistSong(audioId, makeSongPath(path, parameters.playlistPath));
    }

    static PlaylistSong makePlaylistSong(long audioId, String path, String providedTitle, String subtitlePath, ReadParameters parameters) {
        return new PlaylistSong(audioId, makeSongPath(path, parameters.playlistPath), providedTitle, subtitlePath);
    }

    static String makeSongPath(String songPath, String playlistPath) {
        if (songPath.startsWith("/"))
            return makeSongPathAbsolute(songPath, playlistPath);

        if (songPath.startsWith("\\"))
            return makeSongPathAbsolute(songPath, playlistPath);

        if (songPath.startsWith("..")) {
            String path = makeSongPathRelative(songPath, playlistPath);
            if (path == null) return makeSongPathAbsolute(songPath, playlistPath);
            else return path;
        }

        if (songPath.contains(":"))
            return makeSongPathAbsolute(songPath, playlistPath);

        String path = makeSongPathRelative(songPath, playlistPath);
        if (path == null) return makeSongPathAbsolute(songPath, playlistPath);
        else return path;
    }

    static String makeSongPathRelative(String path, String playlistPath) {
        File plFile = new File(playlistPath);
        if (!plFile.isDirectory()) {
            File pldir = plFile.getParentFile();
            if (pldir != null)
                plFile = pldir;
        }

        File file = new File(plFile, path);

        if (file.exists()) {
            try {
                return file.getCanonicalPath();
            } catch (IOException e) {
                return file.getAbsolutePath();
            }
        }

        return null;
    }

    static String makeSongPathAbsolute(String path, String playlistPath) {
        return path;
    }

    public static class ReadParameters {
        public String playlistPath;
    }
}
