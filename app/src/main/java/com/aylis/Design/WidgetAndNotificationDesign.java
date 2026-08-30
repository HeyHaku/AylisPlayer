

package com.aylis.Design;

import android.os.Handler;

import com.aylis.Common.Events.WeakEvent4;
import com.aylis.comp.AlbumArt.AlbumArtCore;
import com.aylis.comp.AlbumArt.AlbumArtRequest;
import com.aylis.comp.AlbumArt.ImageLoadedListener;
import com.aylis.comp.playback.view.MediaPlaybackNotification;
import java.util.ArrayList;
import java.util.List;

public class WidgetAndNotificationDesign {

    static WidgetAndNotificationDesign instance = new WidgetAndNotificationDesign();
    private List<Object> listenerRefHolder = new ArrayList<>();

    public WidgetAndNotificationDesign() {

        MediaPlaybackNotification.onRequestAlbumArtLarge.subscribeWeak(new WeakEvent4.Handler<AlbumArtRequest, ImageLoadedListener, Integer, Integer>() {
            @Override
            public void invoke(AlbumArtRequest albumArtRequest, ImageLoadedListener imageLoadedListener, Integer targetW, Integer targetH) {
                AlbumArtCore albumArtCore = AlbumArtCore.createInstance();
                if (albumArtCore != null)
                    albumArtCore.loadAlbumArtLarge(
                            albumArtRequest.videoThumbDataSource,
                            albumArtRequest.path0,
                            albumArtRequest.path1,
                            albumArtRequest.genStr,
                            imageLoadedListener,
                            targetW,
                            targetH);
            }
        }, listenerRefHolder);

    }

    public static WidgetAndNotificationDesign createInstance() {
        return instance;
    }

}

