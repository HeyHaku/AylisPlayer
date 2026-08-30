

package com.aylis.comp.AlbumArt;

import android.graphics.Bitmap;

public interface ImageLoadedListener {
    void onBitmapLoaded(Bitmap bitmap, String url00, String url0, String url1);
    void setUserObject1(Object obj1);
}
