

package com.aylis.comp.playback.Song;

import android.net.Uri;
import com.aylis.comp.playback.ExoMediaPlayer.Defines;

public interface IMediaDataSource {

    int TYPE_DASH = Defines.TYPE_DASH;
    int TYPE_SS = Defines.TYPE_SS;
    int TYPE_HLS = Defines.TYPE_HLS;
    int TYPE_OTHER = Defines.TYPE_OTHER;
    int TYPE_DEFAULT = Defines.TYPE_DEFAULT;

    int getContentType();

    Uri getContentUri();

    String getContentId();

    String getProviderDASH();
}
