

package com.aylis.comp.Playlists.Files;

import android.app.Service;

import android.content.Context;
import com.aylis.Common.tlog;
import com.aylis.PlayerCore;
import com.aylis.comp.playback.Song.PlaylistSong;
import org.myapache.commons.logging.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import christophedelory.playlist.Media;
import christophedelory.playlist.Playlist;
import christophedelory.playlist.Sequence;
import christophedelory.playlist.SpecificPlaylist;
import christophedelory.playlist.SpecificPlaylistProvider;
import christophedelory.playlist.m3u.M3U;
import mychristophedelory.content.Content;
import mychristophedelory.content.type.ContentType;
import mychristophedelory.logging.LogFactory;

public class PlaylistFilesUtils {

    private static PlaylistFilesUtils instance = null;
    private final Log logger;
    private Iterable<SpecificPlaylistProvider> serviceLoader;

    public PlaylistFilesUtils() {
        logger = LogFactory.getLog(getClass());

        List<SpecificPlaylistProvider> serviceLoader = new ArrayList<>();

        serviceLoader.add(new christophedelory.playlist.pla.PLAProvider());

        serviceLoader.add(new christophedelory.playlist.pls.PLSProvider());
        serviceLoader.add(new christophedelory.playlist.mpcpl.MPCPLProvider());
        serviceLoader.add(new christophedelory.playlist.plp.PLPProvider());

        serviceLoader.add(new christophedelory.playlist.m3u.M3UProvider());

        this.serviceLoader = serviceLoader;
    }

    public static PlaylistFilesUtils s() {
        if (instance == null)
            instance = new PlaylistFilesUtils();
        return instance;
    }

    public static String makePlaylistPath(String destPath, String name, PlaylistFilesType playlistType) {
        if (destPath != null && destPath.length() > 0) {
            if (destPath.charAt(destPath.length() - 1) != '/')
                destPath += "/";
        } else {
            destPath = "//";
        }

        return destPath + name + "." + playlistType.fileExtension;
    }

    private SpecificPlaylist myReadFrom(final URL url) throws IOException {
        SpecificPlaylist ret = null;

        for (SpecificPlaylistProvider service : serviceLoader) {
            final URLConnection urlConnection = url.openConnection();
            urlConnection.setAllowUserInteraction(false);
            urlConnection.setConnectTimeout(10000);
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(false);
            urlConnection.setReadTimeout(60000);
            urlConnection.setUseCaches(true);

            urlConnection.connect();

            final String contentEncoding = urlConnection.getContentEncoding();

            final InputStream in = urlConnection.getInputStream();

            try {
                ret = service.readFrom(in, contentEncoding, logger);

                break;
            } catch (Exception e) {

                if (logger.isTraceEnabled()) {
                    logger.trace("Playlist provider " + service.getId() + " cannot unmarshal <" + url + ">", e);
                } else if (logger.isDebugEnabled()) {
                    logger.debug("Playlist provider " + service.getId() + " cannot unmarshal <" + url + ">: " + e);
                }
            } finally {
                in.close();
            }
        }

        return ret;
    }

    private SpecificPlaylistProvider findProviderByExtension(final String filename) {
        SpecificPlaylistProvider ret = null;
        final String name = filename.toLowerCase(Locale.ENGLISH);

        for (SpecificPlaylistProvider service : serviceLoader) {
            final ContentType[] types = service.getContentTypes();

            for (ContentType type : types) {
                if (type.matchExtension(name)) {
                    ret = service;
                    break;
                }
            }

            if (ret != null) {
                break;
            }
        }

        return ret;
    }

    public List<PlaylistSong> getSongsFromPlaylistFile(String filePath) {

        URL url;

        try {
            url = new URL("file://" + filePath);
        } catch (MalformedURLException e) {
            return null;
        }

        URL _url = url;
        File inputFile = new File(filePath);

        Context context = PlayerCore.s().getAppContext();
        if (context == null)
            return null;

        SpecificPlaylist specificPlaylist = null;

        try {
            specificPlaylist = myReadFrom(_url);
        } catch (IOException e) {
            tlog.w(e.getMessage());
        }

        if (specificPlaylist == null) {
            return null;
        }

        List<PlaylistSong> resultList = new ArrayList<>();

        if (inputFile.exists()) {

            PlaylistFilesRUtils.ReadParameters parameters = new PlaylistFilesRUtils.ReadParameters();
            try {
                parameters.playlistPath = inputFile.getCanonicalPath();
            } catch (Exception e) {
                parameters.playlistPath = inputFile.getAbsolutePath();
            }

            PlaylistFilesRUtils.readFromSpecificPlaylist(specificPlaylist, parameters, resultList);
        }

        return resultList;
    }

    public int createPlaylist(String filePath, PlaylistFilesType playlistType, boolean writeRelativePaths) {
        return createPlaylist(filePath, playlistType, null, writeRelativePaths);
    }

    public int createPlaylist(String filePath, PlaylistFilesType playlistType, List<String> dataSources, boolean writeRelativePaths) {
        return addDataSourceToPlaylistFile(filePath, dataSources, true, writeRelativePaths);
    }

    public int addDataSourceToPlaylistFile(String filePath, List<String> dataSources, boolean overwriteCurrentContent, boolean writeRelativePaths) {
        return addToPlaylistFile(filePath, PlaylistSong.makeSongListFromDataSourceList(dataSources), overwriteCurrentContent, writeRelativePaths);
    }

    public int addToPlaylistFile(String filePath, List<PlaylistSong> dataSources, boolean overwriteCurrentContent, boolean writeRelativePaths) {

        URL url;

        try {
            if (filePath.startsWith("file:"))
                url = new URL(filePath);
            else
                url = new URL("file://" + filePath);
        } catch (MalformedURLException e) {
            return 0;
        }

        final boolean _extM3U = true;

        SpecificPlaylistProvider specificPlaylistProvider = null;
        SpecificPlaylist specificPlaylist = null;

        File outputFile = new File(filePath);

        if (outputFile.exists()) {

            try {
                specificPlaylist = myReadFrom(url);
                specificPlaylistProvider = specificPlaylist.getProvider();

                if (specificPlaylist instanceof M3U) {
                    ((M3U) specificPlaylist).setExtensionM3U(_extM3U);
                }

                if (overwriteCurrentContent) {
                    try {
                        specificPlaylist = specificPlaylistProvider.toSpecificPlaylist(new Playlist());
                    } catch (Exception e) {
                        tlog.w(e.getMessage());
                    }
                }
            } catch (IOException e) {
                tlog.w(e.getMessage());
            }

            if (specificPlaylist == null || specificPlaylistProvider == null) {
                if (!overwriteCurrentContent) {

                    return 0;
                }

                specificPlaylistProvider = findProviderByExtension(filePath);
                try {
                    specificPlaylist = specificPlaylistProvider.toSpecificPlaylist(new Playlist());
                } catch (Exception e) {
                    tlog.w(e.getMessage());
                }
            }

        } else {

            overwriteCurrentContent = true;
            specificPlaylistProvider = findProviderByExtension(filePath);

            try {
                specificPlaylist = specificPlaylistProvider.toSpecificPlaylist(new Playlist());
            } catch (Exception e) {
                tlog.w(e.getMessage());
            }
        }

        if (specificPlaylist == null) {
            tlog.w("error specificPlaylist is null");
            return 0;
        }

        SpecificPlaylist outputSpecificPlaylist = specificPlaylist;

        PlaylistFilesWUtils.AppendParameters appendParameters = new PlaylistFilesWUtils.AppendParameters();
        appendParameters.writeRelativePaths = writeRelativePaths;
        try {
            appendParameters.playlistPath = outputFile.getCanonicalPath();
        } catch (Exception e) {
            appendParameters.playlistPath = outputFile.getAbsolutePath();
        }

        int addedCount = PlaylistFilesWUtils.appendToSpecificPlaylist(outputSpecificPlaylist, appendParameters, dataSources);
        OutputStream out;

        try {

            File dir = outputFile.getParentFile();
            if (dir != null && !dir.exists())
                dir.mkdirs();

            out = new FileOutputStream(outputFile);
        } catch (Exception e) {
            return 0;
        }

        try {
            outputSpecificPlaylist.writeTo(out, null);
            out.flush();
            out.close();
        } catch (Exception e) {
            tlog.w("outputSpecificPlaylist.writeTo: " + e.getMessage());
            e.printStackTrace();
        }

        return addedCount;
    }

    private void lizzyAddToPlaylistAsFile(final Sequence sequence, final File file, final boolean recurse, final File playlistFile, final boolean recursive) throws IOException {
        if (file.isDirectory())
        {
            if (recurse) {
                final File[] files = file.listFiles();

                if (files != null) {
                    for (File child : files) {
                        lizzyAddToPlaylistAsFile(sequence, child, recursive, playlistFile, recursive);
                    }
                }
            }
        } else if (file.isFile())
        {
            boolean include = true;
            String filePath = file.getPath();

            if (playlistFile != null) {
                final File canonicalFile = file.getCanonicalFile();

                if (canonicalFile.equals(playlistFile)) {
                    include = false;
                } else {

                    File parentFile = canonicalFile.getParentFile();
                    final File playlistParentFile = playlistFile.getParentFile();

                    if (parentFile.equals(playlistParentFile)) {
                        filePath = file.getName();
                    } else {
                        final StringBuilder sb = new StringBuilder(file.getName());
                        File previousFile = parentFile;
                        parentFile = previousFile.getParentFile();

                        while (parentFile != null) {
                            sb.insert(0, '/');
                            final String previousFileName = previousFile.getName();

                            if (!"/".equals(previousFileName) && !"\\".equals(previousFileName)) {
                                sb.insert(0, previousFileName);
                            }

                            if (parentFile.equals(playlistParentFile)) {
                                filePath = sb.toString();
                                break;
                            }

                            previousFile = parentFile;
                            parentFile = previousFile.getParentFile();
                        }
                    }
                }
            }

            if (include) {
                final Media media = new Media();
                final Content content = new Content(filePath);
                media.setSource(content);

                sequence.addComponent(media);
            }
        }
    }

}
