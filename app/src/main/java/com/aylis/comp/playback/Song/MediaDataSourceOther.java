

package com.aylis.comp.playback.Song;

import android.net.Uri;

public class MediaDataSourceOther implements IMediaDataSource {

    private final Uri uriString;
    private final int contentType;

    public MediaDataSourceOther(Uri uriString)
    {
        android.content.Context context = com.aylis.PlayerCore.s().getAppContext();
        Uri resolved = uriString;
        if (context != null && uriString != null) {
            String realPath = com.aylis.Common.MediaStoreUtils.getRealFilePath(context, uriString);
            if (realPath != null) {
                resolved = Uri.fromFile(new java.io.File(realPath));
            }
        }
        this.uriString = resolved;
        contentType = detectContentType(resolved);
    }

    public static int detectContentType(Uri uri) {

        String ext = uri.getPath();

        if (ext != null) {
            int index = ext.lastIndexOf(".");
            try {
                ext = ext.substring(index + 1);
            } catch (Exception e) {
                ext = "";
            }
            ext = ext.toLowerCase();
        } else {
            return TYPE_OTHER;
        }

        if (ext.startsWith("mpd")) return TYPE_DASH;
        if (ext.startsWith("ism")) return TYPE_SS;

        if (ext.equals("flv")) return TYPE_OTHER;

        if (ext.equals("m3u8")) return TYPE_HLS;

        if (ext.equals("wav")) return TYPE_DEFAULT;

        return TYPE_OTHER;
    }

    @Override
    public int getContentType() {
        return contentType;
    }

    @Override
    public Uri getContentUri() {
        return uriString;
    }

    @Override
    public String getContentId() {
        return null;
    }

    @Override
    public String getProviderDASH() {
        return "widevine_test";
    }
}
