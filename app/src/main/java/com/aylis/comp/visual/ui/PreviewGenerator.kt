package com.aylis.comp.visual.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.aylis.comp.AppPreferences.AppPreferences
import com.aylis.comp.visual.core.Elements.BackgroundElement
import com.aylis.comp.visual.core.Elements.Element
import com.aylis.comp.visual.core.Elements.ElementGroup
import com.aylis.comp.visual.core.Elements.Images.ImageElement
import com.aylis.comp.visual.core.Elements.ParticlesElement
import com.aylis.comp.visual.core.Elements.RootElement
import com.aylis.comp.visual.core.Elements.SegmentElement
import com.aylis.comp.visual.design.VisualizerThemes
import java.util.Random

object PreviewGenerator {
    class RenderFeatures {
        var hasCircleSegment = false
        var hasLineSegment = false
        var hasParticles = false
        var hasAlbumArt = false
        var albumArtIsCircle = false
        val segmentColors = mutableListOf<Int>()
        var bgColor = 0xFF121318.toInt()
    }

    fun generateVisualizerPreview(context: Context, themeId: Int, width: Int, height: Int): Bitmap? {
        try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            var root: RootElement? = null
            try {
                val scene = AppPreferences.createOrGetInstance().getPrefThemeScene(themeId)
                if (scene != null) {
                    root = com.aylis.comp.visual.scene.SceneBuilder.buildFromScene(themeId, scene)
                } else {
                    root = VisualizerThemes.s().getThemeObject(themeId)
                    if (root != null) {
                        val customizationList = AppPreferences.createOrGetInstance().getPrefThemeCustomizationData(root.identifier)
                        if (customizationList != null) {
                            root.setCustomization(customizationList)
                        }
                    }
                }
            } catch (ignored: Exception) {}

            val features = RenderFeatures()
            if (root != null) {
                scanElement(root, features)
            }

            var backgroundPaintColor = features.bgColor
            if (android.graphics.Color.alpha(backgroundPaintColor) < 50 || backgroundPaintColor == 0) {
                backgroundPaintColor = 0xFF121318.toInt()
            }
            backgroundPaintColor = (backgroundPaintColor and 0x00FFFFFF) or 0xFF000000.toInt()
            
            // Draw gradient background
            val bgPaint = Paint().apply {
                isAntiAlias = true
                shader = android.graphics.RadialGradient(
                    width / 2f, height / 2f, width / 1.2f,
                    intArrayOf(backgroundPaintColor, 0xFF0A0A0E.toInt()),
                    null, android.graphics.Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            val cx = width / 2.0f
            val cy = height / 2.0f

            if (features.hasParticles) {
                val particlePaint = Paint().apply {
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                val rand = Random(42)
                var pColor = 0x80FFFFFF.toInt()
                if (features.segmentColors.isNotEmpty()) {
                    pColor = (features.segmentColors[0] and 0x00FFFFFF) or 0x60000000
                }
                particlePaint.color = pColor
                particlePaint.setShadowLayer(6f, 0f, 0f, pColor)
                for (p in 0..24) {
                    val px = 15f + rand.nextFloat() * (width - 30f)
                    val py = 15f + rand.nextFloat() * (height - 30f)
                    val pr = 1.5f + rand.nextFloat() * 3.5f
                    canvas.drawCircle(px, py, pr, particlePaint)
                }
            }

            if (features.hasCircleSegment) {
                var circleIndex = 0
                for (i in features.segmentColors.indices) {
                    val color = features.segmentColors[i]
                    val radius = (width * 0.35f) - circleIndex * 10f
                    if (radius < 10f) break

                    val circlePaint = Paint().apply { isAntiAlias = true }

                    circlePaint.style = Paint.Style.STROKE
                    circlePaint.strokeWidth = 1.5f
                    circlePaint.color = (color and 0x00FFFFFF) or 0x40000000
                    canvas.drawCircle(cx, cy, radius, circlePaint)

                    circlePaint.style = Paint.Style.FILL
                    circlePaint.color = color
                    circlePaint.strokeWidth = 2.5f
                    circlePaint.strokeCap = Paint.Cap.ROUND
                    circlePaint.setShadowLayer(8f, 0f, 0f, color)
                    val barCount = 28 + circleIndex * 8
                    for (b in 0 until barCount) {
                        val angle = (b * 2 * Math.PI) / barCount
                        val t = angle
                        val wave = Math.abs(Math.sin(t * 2.0)) + Math.abs(Math.cos(t * 5.0)) * 0.5 + Math.abs(Math.sin(t * 11.0)) * 0.25
                        val h = 3f + (wave.toFloat() * (18f - circleIndex * 3f) / 1.75f)
                        val rStart = radius
                        val rEnd = radius + h

                        val xStart = cx + (Math.cos(angle) * rStart).toFloat()
                        val yStart = cy + (Math.sin(angle) * rStart).toFloat()
                        val xEnd = cx + (Math.cos(angle) * rEnd).toFloat()
                        val yEnd = cy + (Math.sin(angle) * rEnd).toFloat()

                        canvas.drawLine(xStart, yStart, xEnd, yEnd, circlePaint)
                    }
                    circleIndex++
                }
            }

            if (features.hasLineSegment) {
                var lineIndex = 0
                for (i in features.segmentColors.indices) {
                    if (features.hasCircleSegment && lineIndex >= 1) break

                    val color = features.segmentColors[i]
                    val barPaint = Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.FILL
                        this.color = color
                        strokeWidth = 3.5f
                        strokeCap = Paint.Cap.ROUND
                        setShadowLayer(8f, 0f, 0f, color)
                    }

                    val startX = width * 0.10f
                    val endX = width * 0.90f
                    val barCount = 18
                    val spacing = (endX - startX) / (barCount - 1)
                    var baseLineY = cy + (if (features.hasCircleSegment) (width * 0.38f) else (lineIndex * 15f - 5f))
                    if (baseLineY > height - 10f || baseLineY < 10f) baseLineY = cy + 15f

                    for (b in 0 until barCount) {
                        val bx = startX + b * spacing
                        val t = b.toDouble() / barCount * Math.PI
                        val wave = Math.abs(Math.sin(t)) * 1.5 + Math.abs(Math.cos(t * 3.0)) * 0.5 + Math.abs(Math.sin(t * 7.0)) * 0.3
                        val bh = 3f + (wave.toFloat() * 12f)
                        canvas.drawLine(bx, baseLineY, bx, baseLineY - bh, barPaint)
                    }
                    lineIndex++
                }
            }

            if (features.hasAlbumArt) {
                val artPaint = Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.FILL
                    }

                    var artColor = 0xFF2A2D36.toInt()
                    if (features.segmentColors.isNotEmpty()) {
                        artColor = (features.segmentColors[0] and 0x00FFFFFF) or 0x30000000
                    }
                    
                    val artRadius = width * 0.22f
                    
                    // Add gradient to album art
                    artPaint.shader = android.graphics.RadialGradient(
                        cx, cy, artRadius,
                        intArrayOf(artColor, 0x10FFFFFF),
                        null, android.graphics.Shader.TileMode.CLAMP
                    )

                    if (features.albumArtIsCircle) {
                        canvas.drawCircle(cx, cy, artRadius, artPaint)

                        artPaint.shader = null
                        artPaint.style = Paint.Style.STROKE
                        artPaint.strokeWidth = 1.5f
                        artPaint.color = 0x40FFFFFF
                        canvas.drawCircle(cx, cy, artRadius - 5f, artPaint)
                        canvas.drawCircle(cx, cy, artRadius - 10f, artPaint)

                        artPaint.style = Paint.Style.FILL
                        artPaint.color = backgroundPaintColor
                        canvas.drawCircle(cx, cy, 3.5f, artPaint)
                    } else {
                        val rect = RectF(cx - artRadius, cy - artRadius, cx + artRadius, cy + artRadius)
                        canvas.drawRoundRect(rect, 8f, 8f, artPaint)

                        artPaint.shader = null
                        artPaint.style = Paint.Style.STROKE
                        artPaint.strokeWidth = 1.5f
                        artPaint.color = 0x40FFFFFF
                        canvas.drawRoundRect(rect, 8f, 8f, artPaint)
                        
                        // Draw inner placeholder mountains
                        artPaint.style = Paint.Style.FILL
                        artPaint.color = 0x20FFFFFF
                        val m1 = android.graphics.Path()
                        m1.moveTo(cx - artRadius + 4f, cy + artRadius - 4f)
                        m1.lineTo(cx - 5f, cy - 5f)
                        m1.lineTo(cx + 10f, cy + 10f)
                        m1.lineTo(cx + artRadius - 4f, cy + artRadius - 4f)
                        canvas.drawPath(m1, artPaint)
                        canvas.drawCircle(cx + 10f, cy - 10f, 4f, artPaint)
                    }
            } // Close hasAlbumArt
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun scanElement(element: Element?, features: RenderFeatures) {
        if (element == null) return

        if (element is SegmentElement) {
            try {
                val pathField = element.javaClass.getDeclaredField("segmentPath")
                pathField.isAccessible = true
                val pathObj = pathField.get(element)
                if (pathObj != null) {
                    val className = pathObj.javaClass.simpleName
                    if (className.contains("Circle")) {
                        features.hasCircleSegment = true
                    } else {
                        features.hasLineSegment = true
                    }
                } else {
                    features.hasLineSegment = true
                }
            } catch (e: Exception) {
                features.hasLineSegment = true
            }

            try {
                val colorField = element.javaClass.getDeclaredField("color1")
                colorField.isAccessible = true
                val color = colorField.getInt(element)
                features.segmentColors.add(color)
            } catch (e: Exception) {
                features.segmentColors.add(0xFFFFFFFF.toInt())
            }
        } else if (element is ParticlesElement) {
            features.hasParticles = true
            try {
                val colorField = element.javaClass.getDeclaredField("color1")
                colorField.isAccessible = true
                val color = colorField.getInt(element)
                features.segmentColors.add(color)
            } catch (ignored: Exception) {}
        } else if (element is ImageElement) {
            features.hasAlbumArt = true
            try {
                val circleField = element.javaClass.getDeclaredField("circleShape")
                circleField.isAccessible = true
                features.albumArtIsCircle = circleField.getBoolean(element)
            } catch (e: Exception) {
                features.albumArtIsCircle = false
            }
        } else if (element is BackgroundElement) {
            try {
                val bgRField = element.javaClass.getDeclaredField("bgR").apply { isAccessible = true }
                val bgGField = element.javaClass.getDeclaredField("bgG").apply { isAccessible = true }
                val bgBField = element.javaClass.getDeclaredField("bgB").apply { isAccessible = true }
                val bgAField = element.javaClass.getDeclaredField("bgA").apply { isAccessible = true }
                val r = bgRField.getFloat(element)
                val g = bgGField.getFloat(element)
                val b = bgBField.getFloat(element)
                val a = bgAField.getFloat(element)
                features.bgColor = android.graphics.Color.argb((a * 255).toInt(), (r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
            } catch (ignored: Exception) {}
        }

        if (element is ElementGroup) {
            val children = element.getChildList()
            if (children != null) {
                for (child in children) {
                    if (child is Element) {
                        scanElement(child, features)
                    }
                }
            }
        }
    }
}
