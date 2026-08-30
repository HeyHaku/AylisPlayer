package com.aylis.comp.AlbumArt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Looper;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import com.aylis.PlayerCore;
import com.aylis.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;

public class AlbumArtCore {

    private static final Object createInstanceLock = new Object();
    private static volatile WeakReference<AlbumArtCore> instanceWeak = new WeakReference<>(null);
    private boolean glideInitialized = false;

    public static AlbumArtCore createInstance() {
        AlbumArtCore inst0 = instanceWeak.get();
        if (inst0 != null) return inst0;

        synchronized (createInstanceLock) {
            AlbumArtCore inst = instanceWeak.get();
            if (inst == null) {
                inst = new AlbumArtCore();
                instanceWeak = new WeakReference<>(inst);
            }
            return inst;
        }
    }

    public static AlbumArtCore getInstance() {
        return instanceWeak.get();
    }

    public void initGlide(Context context) {
        if (!glideInitialized) {
            Glide.get(context).getRegistry().prepend(Uri.class, InputStream.class, new AlbumArtModelLoader1.Factory(context));
            Glide.get(context).getRegistry().prepend(Uri.class, Bitmap.class, new AlbumArtModelLoader2.Factory(context));
            glideInitialized = true;
        }
    }

    public void cancelRequest(final ImageView imageView) {
        Context context = PlayerCore.s().getAppContext();
        if (context == null) {
            return;
        }
        Glide.with(context).clear(imageView);
    }

    public void loadAlbumArt(String url, final ImageView imageView) {
        Context context = PlayerCore.s().getAppContext();
        if (context == null) {
            return;
        }
        initGlide(context);

        if (url != null && url.length() > 0) {
            if (url.charAt(0) == '/')
                url = "file://" + url;
        }

        Glide.with(context)
                .load(url)
                .placeholder(R.drawable.placeholderart4)
                .error(R.drawable.placeholderart4)
                .into(imageView);
    }

    public void loadAlbumArt(String dataSource,
                             String url0,
                             String url1,
                             String generateText,
                             final ImageView imageView,
                             boolean fitCenterInside,
                             boolean preferLarge) {
        Context context = PlayerCore.s().getAppContext();
        if (context == null) {
            return;
        }
        initGlide(context);

        if (url0 != null && url0.length() > 0) {
            if (url0.charAt(0) == '/')
                url0 = "file://" + url0;
        }

        if (url1 != null && url1.length() > 0) {
            if (url1.charAt(0) == '/')
                url1 = "file://" + url1;
        }

        Uri uri = new Uri.Builder()
                .scheme("mycontent2")
                .appendQueryParameter("src", dataSource)
                .appendQueryParameter("0", url0)
                .appendQueryParameter("1", url1)
                .appendQueryParameter("large", preferLarge ? "1" : "0")
                .appendQueryParameter("gentext", generateText)
                .build();

        if (fitCenterInside) {
            Glide.with(context)
                    .load(uri)
                    .placeholder(R.drawable.placeholderart4)
                    .error(R.drawable.placeholderart4)
                    .into(imageView);
        }
    }

    public void loadAlbumArtLarge(final String dataSource,
                                  final String url0,
                                  final String url1,
                                  final String generateText,
                                  final ImageLoadedListener loadedListener,
                                  int targetBoundsWidth,
                                  int targetBoundsHeight) {
        boolean mainThread = Looper.myLooper() == Looper.getMainLooper();
        if (mainThread)
            loadASyncAlbumArtLarge(dataSource, url0, url1, generateText, loadedListener, targetBoundsWidth, targetBoundsHeight);
        else
            loadSyncAlbumArtLarge(dataSource, url0, url1, generateText, loadedListener, targetBoundsWidth, targetBoundsHeight);
    }

    private void loadSyncAlbumArtLarge(final String dataSource,
                                       final String url0,
                                       final String url1,
                                       final String generateText,
                                       final ImageLoadedListener loadedListener,
                                       int targetBoundsWidth,
                                       int targetBoundsHeight) {
        Context context = PlayerCore.s().getAppContext();
        if (context == null) {
            return;
        }
        initGlide(context);

        String url0Fixed = url0;
        String url1Fixed = url1;

        if (url0Fixed != null && url0Fixed.length() > 0) {
            if (url0Fixed.charAt(0) == '/')
                url0Fixed = "file://" + url0;
        }

        if (url1Fixed != null && url1Fixed.length() > 0) {
            if (url1Fixed.charAt(0) == '/')
                url1Fixed = "file://" + url1;
        }

        Uri uri = new Uri.Builder()
                .scheme("mycontent2")
                .appendQueryParameter("src", dataSource)
                .appendQueryParameter("0", url0Fixed)
                .appendQueryParameter("1", url1Fixed)
                .appendQueryParameter("large", "1")
                .appendQueryParameter("gentext", generateText)
                .build();

        Bitmap bitmap = null;
        try {
            int w = targetBoundsWidth > 0 ? targetBoundsWidth : com.bumptech.glide.request.target.Target.SIZE_ORIGINAL;
            int h = targetBoundsHeight > 0 ? targetBoundsHeight : com.bumptech.glide.request.target.Target.SIZE_ORIGINAL;
            bitmap = Glide.with(context).asBitmap().load(uri).submit(w, h).get();
        } catch (Exception ignored) {
        }

        loadedListener.onBitmapLoaded(bitmap, dataSource, url0, url1);
    }

    public void loadASyncAlbumArtLarge(final String dataSource,
                                        final String url0,
                                        final String url1,
                                        final String generateText,
                                        final ImageLoadedListener loadedListener,
                                        int targetBoundsWidth,
                                        int targetBoundsHeight) {
        Context context = PlayerCore.s().getAppContext();
        if (context == null) {
            return;
        }
        initGlide(context);

        String url0Fixed = url0;
        String url1Fixed = url1;

        if (url0Fixed != null && url0Fixed.length() > 0) {
            if (url0Fixed.charAt(0) == '/')
                url0Fixed = "file://" + url0;
        }

        if (url1Fixed != null && url1Fixed.length() > 0) {
            if (url1Fixed.charAt(0) == '/')
                url1Fixed = "file://" + url1;
        }

        Uri uri = new Uri.Builder()
                .scheme("mycontent2")
                .appendQueryParameter("src", dataSource)
                .appendQueryParameter("0", url0Fixed)
                .appendQueryParameter("1", url1Fixed)
                .appendQueryParameter("large", "1")
                .appendQueryParameter("gentext", generateText)
                .build();

        int w = targetBoundsWidth > 0 ? targetBoundsWidth : com.bumptech.glide.request.target.Target.SIZE_ORIGINAL;
        int h = targetBoundsHeight > 0 ? targetBoundsHeight : com.bumptech.glide.request.target.Target.SIZE_ORIGINAL;

        CustomTarget<Bitmap> imageLoad = new CustomTarget<Bitmap>(w, h) {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                loadedListener.onBitmapLoaded(resource, dataSource, url0, url1);
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                loadedListener.onBitmapLoaded(null, dataSource, url0, url1);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        };

        loadedListener.setUserObject1(imageLoad);

        Glide.with(context)
                .asBitmap()
                .load(uri)
                .placeholder(R.drawable.placeholderart4)
                .error(R.drawable.placeholderart4)
                .into(imageLoad);
    }
}
