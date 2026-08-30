package com.aylis.comp.visual.ambient

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

object AmbientManager {

    private const val TARGET_SIZE = 64
    private var baseMap: Bitmap? = null
    private val listeners = mutableListOf<WeakReference<AmbientListener>>()
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    // Насыщенность (1.3f - 1.4f дает идеальный сочный эмбиент без пережога)
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        val cm = ColorMatrix().apply {
            setSaturation(1.35f)
        }
        colorFilter = ColorMatrixColorFilter(cm)
    }

    interface AmbientListener {
        fun onAmbientUpdated(baseMap: Bitmap)
    }

    fun addListener(listener: AmbientListener) {
        listeners.removeAll { it.get() == null }
        if (!listeners.any { it.get() == listener }) {
            listeners.add(WeakReference(listener))
        }
        baseMap?.let { map ->
            listener.onAmbientUpdated(map)
        }
    }

    fun removeListener(listener: AmbientListener) {
        listeners.removeAll { it.get() == listener || it.get() == null }
    }

    fun updateCover(cover: Bitmap?) {
        if (cover == null || cover.isRecycled) {
            baseMap = null
            return
        }

        executor.execute {
            try {
                // 1. Двухэтапный даунскейл для сохранения контраста и мелких цветовых акцентов
                val intermediateSize = 160
                val intermediate = if (cover.width > intermediateSize && cover.height > intermediateSize) {
                    Bitmap.createScaledBitmap(cover, intermediateSize, intermediateSize, true)
                } else {
                    cover
                }

                // 2. Отрисовка финального 64x64 с бустом насыщенности
                val finalMap = Bitmap.createBitmap(TARGET_SIZE, TARGET_SIZE, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(finalMap)

                val srcRect = android.graphics.Rect(0, 0, intermediate.width, intermediate.height)
                val dstRect = android.graphics.Rect(0, 0, TARGET_SIZE, TARGET_SIZE)
                canvas.drawBitmap(intermediate, srcRect, dstRect, paint)

                if (intermediate != cover) {
                    intermediate.recycle()
                }

                mainHandler.post {
                    val oldMap = baseMap
                    baseMap = finalMap
                    notifyListeners(finalMap)

                    // Освобождаем старую карту после уведомления
                    if (oldMap != null && !oldMap.isRecycled && oldMap != finalMap) {
                        oldMap.recycle()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun notifyListeners(map: Bitmap) {
        listeners.removeAll { it.get() == null }
        for (ref in listeners) {
            ref.get()?.onAmbientUpdated(map)
        }
    }
}