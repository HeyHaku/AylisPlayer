

package com.aylis.comp.AlbumArt;

public class AlbumArtRequest {

    public final String videoThumbDataSource;
    public final String path0;
    public final String path1;
    public final String genStr;

    public AlbumArtRequest(String videoThumbDataSource, String path0, String path1, String genStr) {
        this.videoThumbDataSource = videoThumbDataSource;
        this.path0 = path0;
        this.path1 = path1;
        this.genStr = genStr;
    }

    public AlbumArtRequest makeCopy() {
        return new AlbumArtRequest(
                videoThumbDataSource != null ? new String(videoThumbDataSource) : null,
                path0 != null ? new String(path0) : null,
                path1 != null ? new String(path1) : null,
                genStr != null ? new String(genStr) : null);
    }

}
