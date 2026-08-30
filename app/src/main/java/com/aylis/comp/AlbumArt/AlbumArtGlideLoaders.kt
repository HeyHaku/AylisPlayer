package com.aylis.comp.AlbumArt

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.AOSP.MyThumbnailUtils
import com.aylis.R
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.request.target.Target
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.ExecutionException

class AlbumArtModelLoader1(private val context: Context) : ModelLoader<Uri, InputStream> {
    override fun buildLoadData(model: Uri, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>? {
        return ModelLoader.LoadData(com.bumptech.glide.signature.ObjectKey(model), AlbumArtDataFetcher1(context, model))
    }

    override fun handles(model: Uri): Boolean {
        return model.scheme == "mycontent"
    }

    class Factory(private val context: Context) : ModelLoaderFactory<Uri, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<Uri, InputStream> {
            return AlbumArtModelLoader1(context)
        }
        override fun teardown() {}
    }
}

class AlbumArtDataFetcher1(private val context: Context, private val uri: Uri) : DataFetcher<InputStream> {
    private var stream: InputStream? = null

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        val contentUri = uri.buildUpon().scheme("content").build()
        stream = createInputStreamFromPath(contentUri, context)
        if (stream != null) {
            callback.onDataReady(stream)
        } else {
            callback.onLoadFailed(Exception("Failed to load stream for mycontent"))
        }
    }

    private fun createInputStreamFromPath(uri: Uri, context: Context): InputStream? {
        var isStream: InputStream? = null
        if ("file" == uri.scheme) {
            try {
                isStream = FileInputStream(uri.path)
            } catch (ignored: Exception) {}
        } else {
            var pfd: ParcelFileDescriptor? = null
            try {
                pfd = context.contentResolver.openFileDescriptor(uri, "r")
            } catch (ignored: Exception) {}
            if (pfd != null) {
                try {
                    val fd: FileDescriptor = pfd.fileDescriptor
                    isStream = FileInputStream(fd)
                } catch (ignored: Exception) {}
            }
        }
        return isStream
    }

    override fun cleanup() {
        try { stream?.close() } catch (ignored: Exception) {}
    }
    override fun cancel() {}
    override fun getDataClass(): Class<InputStream> = InputStream::class.java
    override fun getDataSource(): DataSource = DataSource.LOCAL
}

class AlbumArtModelLoader2(private val context: Context) : ModelLoader<Uri, Bitmap> {
    override fun buildLoadData(model: Uri, width: Int, height: Int, options: Options): ModelLoader.LoadData<Bitmap>? {
        return ModelLoader.LoadData(com.bumptech.glide.signature.ObjectKey(model), AlbumArtDataFetcher2(context, model, width, height))
    }

    override fun handles(model: Uri): Boolean {
        return model.scheme == "mycontent2"
    }

    class Factory(private val context: Context) : ModelLoaderFactory<Uri, Bitmap> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<Uri, Bitmap> {
            return AlbumArtModelLoader2(context)
        }
        override fun teardown() {}
    }
}

class AlbumArtDataFetcher2(
    private val context: Context,
    private val uri: Uri,
    private val width: Int,
    private val height: Int
) : DataFetcher<Bitmap> {

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in Bitmap>) {
        val uriDataSourceStr = uri.getQueryParameter("src")
        val uriDataSource = if (uriDataSourceStr != null) Uri.parse(uriDataSourceStr) else null
        val uri0Str = uri.getQueryParameter("0")
        val uri0 = if (uri0Str != null) Uri.parse(uri0Str) else null
        val uri1Str = uri.getQueryParameter("1")
        val uri1 = if (uri1Str != null) Uri.parse(uri1Str) else null
        val isLargeStr = uri.getQueryParameter("large")
        val generateText = uri.getQueryParameter("gentext")
        val isLarge = "1" == isLargeStr

        var bm = createBitmapFromPath(uri0, context)
        if (bm != null) {
            callback.onDataReady(bm)
            return
        }

        bm = createBitmapFromPath(uri1, context)
        if (bm != null) {
            callback.onDataReady(bm)
            return
        }

        if (uriDataSource != null) {
            val scheme = uriDataSource.scheme
            if (scheme != null && scheme.startsWith("http")) {
                try {
                    val future = Glide.with(context).asBitmap().load(uriDataSource.toString()).submit(width, height)
                    val resultBm = future.get()
                    if (resultBm != null) {
                        callback.onDataReady(resultBm)
                        return
                    }
                } catch (ignored: Exception) {}
            } else {
                val videoBitmap = MyThumbnailUtils.createVideoThumbnail(
                    uriDataSource.path,
                    if (isLarge) MediaStore.Video.Thumbnails.MINI_KIND else MediaStore.Video.Thumbnails.MICRO_KIND
                )
                if (videoBitmap != null) {
                    callback.onDataReady(videoBitmap)
                    return
                }

                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(uriDataSource.path)
                    val art = retriever.embeddedPicture
                    if (art != null) {
                        val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                        if (bitmap != null) {
                            callback.onDataReady(bitmap)
                            return
                        }
                    }
                } catch (ignored: Exception) {
                } finally {
                    try { retriever.release() } catch (ignored: Exception) {}
                }
            }
        }

        val drawableBg = ContextCompat.getDrawable(context, R.drawable.placeholderart4)
        if (!generateText.isNullOrEmpty()) {
            val ch = generateText[0]
            val hue = SimpleTextAlbumArtCreator.valueInAlphabet(ch) * 360.0f
            val textHsl = floatArrayOf(hue + 0.0f, 0.2f, 1.0f)
            val bgHsl = floatArrayOf(hue, 0.8f, 0.5f)
            val bgHsl2 = floatArrayOf(hue + 10.0f, 0.9f, 0.2f)
            val targetW = if (width > 0 && width != Target.SIZE_ORIGINAL) width else 200
            val targetH = if (height > 0 && height != Target.SIZE_ORIGINAL) height else 200
            val bitmap = SimpleTextAlbumArtCreator.textAsBitmap(
                targetW,
                targetH,
                generateText,
                ColorUtils.HSLToColor(textHsl),
                ColorUtils.HSLToColor(bgHsl),
                ColorUtils.HSLToColor(bgHsl2),
                drawableBg
            )
            callback.onDataReady(bitmap)
            return
        }

        callback.onLoadFailed(Exception("Failed to load album art for mycontent2"))
    }

    private fun createBitmapFromPath(uri: Uri?, context: Context): Bitmap? {
        if (uri == null) return null
        var bm: Bitmap? = null
        if ("file" == uri.scheme) {
            try {
                val path = uri.path
                if (path != null && path.startsWith("/android_asset/")) {
                    val assetPath = path.substring("/android_asset/".length)
                    val isStream = context.assets.open(assetPath)
                    bm = BitmapFactory.decodeStream(isStream)
                    isStream.close()
                } else {
                    bm = BitmapFactory.decodeFile(path)
                }
            } catch (ignored: Exception) {}
        } else {
            var pfd: ParcelFileDescriptor? = null
            try {
                pfd = context.contentResolver.openFileDescriptor(uri, "r")
            } catch (ignored: Exception) {}
            if (pfd != null) {
                try {
                    val fd = pfd.fileDescriptor
                    bm = BitmapFactory.decodeFileDescriptor(fd)
                } catch (ignored: Exception) {
                } finally {
                    try { pfd.close() } catch (ignored: Exception) {}
                }
            }
        }
        return bm
    }

    override fun cleanup() {}
    override fun cancel() {}
    override fun getDataClass(): Class<Bitmap> = Bitmap::class.java
    override fun getDataSource(): DataSource = DataSource.LOCAL
}
