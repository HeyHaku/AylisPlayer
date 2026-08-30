package com.aylis.comp.visual.ambient

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

class AmbientPointEditorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), AmbientManager.AmbientListener {

    var points = mutableListOf<AmbientPoint>()
    var onPointsChanged: ((List<AmbientPoint>) -> Unit)? = null

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private var activePointIndex = -1
    private var baseMap: Bitmap? = null
    
    var brightness: Float = 0.7f
        set(value) {
            field = value
            invalidate()
        }

    private val paints = mutableListOf<Paint>()
    private val xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        AmbientManager.addListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        AmbientManager.removeListener(this)
    }

    override fun onAmbientUpdated(baseMap: Bitmap) {
        this.baseMap = baseMap
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return false

        val touchX = event.x
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Find touched point
                activePointIndex = -1
                for (i in points.indices.reversed()) {
                    val px = points[i].nx * w
                    val py = points[i].ny * h
                    if (hypot(touchX - px, touchY - py) < 120f) { // Increased hit radius for easier grabbing
                        activePointIndex = i
                        break
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (activePointIndex != -1) {
                    points[activePointIndex].nx = (touchX / w).coerceIn(0f, 1f)
                    points[activePointIndex].ny = (touchY / h).coerceIn(0f, 1f)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (activePointIndex != -1) {
                    onPointsChanged?.invoke(points)
                    activePointIndex = -1
                }
            }
        }
        return true
    }

    // Call from external UI to remove the last added point
    fun removeLastPoint() {
        if (points.size > 1) {
            points.removeLast()
            onPointsChanged?.invoke(points)
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.BLACK)

        val w = width.toFloat()
        val h = height.toFloat()
        
        baseMap?.let { map ->
            val radius = Math.max(w, h) * 0.8f

            while (paints.size < points.size) {
                paints.add(Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = this@AmbientPointEditorView.xfermode })
            }

            for (i in points.indices) {
                val point = points[i]
                val px = (point.nx * (map.width - 1)).toInt().coerceIn(0, map.width - 1)
                val py = (point.ny * (map.height - 1)).toInt().coerceIn(0, map.height - 1)
                
                val color = map.getPixel(px, py)
                val r = (Color.red(color) * brightness).toInt().coerceIn(0, 255)
                val g = (Color.green(color) * brightness).toInt().coerceIn(0, 255)
                val b = (Color.blue(color) * brightness).toInt().coerceIn(0, 255)
                val dimColor = Color.rgb(r, g, b)

                val cx = w * point.nx
                val cy = h * point.ny

                paints[i].shader = RadialGradient(cx, cy, radius, dimColor, Color.TRANSPARENT, Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, w, h, paints[i])
            }
        }
        
        for (point in points) {
            val cx = w * point.nx
            val cy = h * point.ny
            canvas.drawCircle(cx, cy, 30f, pointPaint)
            canvas.drawCircle(cx, cy, 30f, strokePaint)
        }
    }
}
