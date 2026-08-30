package com.aylis.comp.visual.ambient

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.content.SharedPreferences
import android.util.AttributeSet
import android.view.View
import com.aylis.R

class AmbientBackgroundView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), AmbientManager.AmbientListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private val profile: String
    private val settings: AmbientSettingsHelper

    // Instead of fixed 5 paints, we have a list of paints
    private val paints = mutableListOf<Paint>()
    private val xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)

    private var currentBaseMap: Bitmap? = null
    private var currentColors = listOf<Int>()
    private var previousColors = listOf<Int>()
    private var currentPoints = listOf<AmbientPoint>()

    private var animator: ValueAnimator? = null
    private val evaluator = ArgbEvaluator()

    init {
        var prof = "default"
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.AmbientBackgroundView)
            prof = a.getString(R.styleable.AmbientBackgroundView_ambientProfile) ?: "default"
            a.recycle()
        }
        profile = prof
        settings = AmbientSettingsHelper(context, profile)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        AmbientManager.addListener(this)
        context.getSharedPreferences("AmbientSettings", Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        AmbientManager.removeListener(this)
        context.getSharedPreferences("AmbientSettings", Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(this)
        animator?.cancel()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "ambient_global_enabled" || key?.startsWith("ambient_${profile}_") == true) {
            refreshFromSettings()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (currentColors.isNotEmpty()) {
            applyColors(currentColors)
        }
    }

    override fun onAmbientUpdated(baseMap: Bitmap) {
        currentBaseMap = baseMap
        refreshFromSettings()
    }

    // Call this if settings change (e.g., from settings menu)
    fun refreshFromSettings() {
        val prefs = context.getSharedPreferences("AmbientSettings", Context.MODE_PRIVATE)
        val globalEnabled = prefs.getBoolean("ambient_global_enabled", false)
        
        if (!globalEnabled || !settings.isEnabled || currentBaseMap == null) {
            currentColors = emptyList()
            invalidate()
            return
        }

        currentPoints = settings.getPoints()
        val newColors = currentPoints.map { point ->
            // Extract color from baseMap
            val px = (point.nx * (currentBaseMap!!.width - 1)).toInt().coerceIn(0, currentBaseMap!!.width - 1)
            val py = (point.ny * (currentBaseMap!!.height - 1)).toInt().coerceIn(0, currentBaseMap!!.height - 1)
            dimColor(currentBaseMap!!.getPixel(px, py), settings.brightness)
        }

        if (currentColors == newColors) return

        previousColors = if (currentColors.isEmpty()) {
            List(newColors.size) { Color.BLACK }
        } else if (currentColors.size == newColors.size) {
            currentColors
        } else {
            // Number of points changed, hard switch previous colors to black to avoid crash
            List(newColors.size) { Color.BLACK }
        }

        currentColors = newColors
        
        // Ensure paints list size matches points
        while (paints.size < currentColors.size) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.xfermode = xfermode
            paints.add(p)
        }
        while (paints.size > currentColors.size) {
            paints.removeLast()
        }

        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = settings.animationDuration
            addUpdateListener { anim ->
                val fraction = anim.animatedFraction
                val blendedColors = previousColors.zip(currentColors) { prev, curr ->
                    evaluator.evaluate(fraction, prev, curr) as Int
                }
                applyColors(blendedColors)
                invalidate()
            }
            start()
        }
    }
    
    private fun dimColor(color: Int, brightness: Float): Int {
        val r = (Color.red(color) * brightness).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * brightness).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * brightness).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun applyColors(colors: List<Int>) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0 || colors.size != currentPoints.size || colors.size != paints.size) return
        
        // Base radius on the maximum dimension to ensure gradients overlap well
        val radius = Math.max(w, h) * 0.8f

        for (i in colors.indices) {
            val point = currentPoints[i]
            val cx = w * point.nx
            val cy = h * point.ny
            
            paints[i].shader = RadialGradient(cx, cy, radius, colors[i], Color.TRANSPARENT, Shader.TileMode.CLAMP)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val prefs = context.getSharedPreferences("AmbientSettings", Context.MODE_PRIVATE)
        val globalEnabled = prefs.getBoolean("ambient_global_enabled", false)
        
        if (!globalEnabled || !settings.isEnabled) return
        
        val w = width.toFloat()
        val h = height.toFloat()
        
        for (paint in paints) {
            canvas.drawRect(0f, 0f, w, h, paint)
        }
    }
}
