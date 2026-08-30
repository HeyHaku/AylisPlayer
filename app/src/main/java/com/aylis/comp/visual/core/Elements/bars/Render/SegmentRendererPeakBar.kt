

package com.aylis.comp.visual.core.Elements.bars.Render

import com.aylis.Common.Vec2f
import com.aylis.comp.visual.core.Elements.Element
import com.aylis.comp.visual.core.Graphic.AtlasTexture
import com.aylis.comp.visual.core.Graphic.RenderState

class SegmentRendererPeakBar : SegmentRendererBase() {

    private var lastDesc: ISegmentRenderer.DrawBatchDesc? = null
    private var nextDesc: ISegmentRenderer.DrawBatchDesc? = null

    var barWidth = 0.5f
    var mirror = false
    var barWidthAffectedByNormal = false

    private var peaks = FloatArray(0)
    private var fallSpeeds = FloatArray(0)
    private var lastTimeMs: Long = 0

    var peakGravity = 25.0f
    var peakThicknessMultiplier = 1.0f
    var peakColorMultiplier = 0.8f

    fun setBarWidth(f: Float): SegmentRendererPeakBar {
        this.barWidth = f
        return this
    }

    fun setMirror(z: Boolean): SegmentRendererPeakBar {
        this.mirror = z
        return this
    }

    override fun drawSegment(renderState: RenderState, drawDesc: ISegmentRenderer.DrawDesc, blendMode: Float) {
        drawDesc.color1 = getBarColorBase(drawDesc.valueIndex, drawDesc.valuesCount, blendMode)
        val fRound = Math.round((drawDesc.drawSegmentWidth * 0.5f) / (drawDesc.valuesCount + 1)) * this.barWidth
        val px = drawDesc.drawPointX
        val py = drawDesc.drawPointY
        val h = drawDesc.segmentHeightVal * (-2.0f) * drawDesc.drawScale * this.barHeightMultiplier
        val fSignNonZero = if (h >= 0.0f) 1.0f else -1.0f

        val currentTime = System.currentTimeMillis()
        var dt = if (lastTimeMs > 0) (currentTime - lastTimeMs) / 1000.0f else 0.016f
        if (dt <= 0.0f || dt > 0.1f) dt = 0.016f
        lastTimeMs = currentTime

        if (peaks.size != 1) {
            peaks = FloatArray(1)
            fallSpeeds = FloatArray(1)
        }

        updatePeak(0, h, dt)

        drawBarAndPeak(
            renderState, px, py, drawDesc.drawVecX, drawDesc.drawVecY,
            fRound, h, fSignNonZero, drawDesc.color1, drawDesc.blendMode, 0, drawDesc.drawScale
        )
    }

    override fun drawSegmentBatch(
        renderState: RenderState,
        drawBatchDescArr: Array<ISegmentRenderer.DrawBatchDesc>,
        drawSegmentWidth: Float,
        drawScale: Float,
        blendMode: Int,
        colorIndexOffset: Float
    ) {
        val length = drawBatchDescArr.size
        val fRound = Math.round((0.5f * drawSegmentWidth) / (length + 1)) * this.barWidth
        val vec2f = Vec2f(0.0f, 0.0f)
        val vec2f2 = Vec2f(0.0f, 0.0f)

        val currentTime = System.currentTimeMillis()
        var dt = if (lastTimeMs > 0) (currentTime - lastTimeMs) / 1000.0f else 0.016f
        if (dt <= 0.0f || dt > 0.1f) dt = 0.016f
        lastTimeMs = currentTime

        if (peaks.size != length) {
            peaks = FloatArray(length)
            fallSpeeds = FloatArray(length)
        }

        for (i3 in drawBatchDescArr.indices) {
            val drawBatchDesc = drawBatchDescArr[i3]
            this.lastDesc = if (drawBatchDesc.valueIndexLastToConnect < 0) drawBatchDesc else drawBatchDescArr[drawBatchDesc.valueIndexLastToConnect]
            this.nextDesc = if (drawBatchDesc.valueIndexNextToConnect < 0) drawBatchDesc else drawBatchDescArr[drawBatchDesc.valueIndexNextToConnect]

            val color = getBarColorBase(i3, length, colorIndexOffset)
            val px = drawBatchDesc.drawPointX
            val py = drawBatchDesc.drawPointY
            val h = drawBatchDesc.segmentHeightVal * (-2.0f) * drawScale * this.barHeightMultiplier
            val fSignNonZero = if (h >= 0.0f) 1.0f else -1.0f

            var dirX = drawBatchDesc.drawVecX
            var dirY = drawBatchDesc.drawVecY

            if (this.barWidthAffectedByNormal && lastDesc != null && nextDesc != null) {
                vec2f.x = drawBatchDesc.drawVecX + lastDesc!!.drawVecX
                vec2f.y = drawBatchDesc.drawVecY + lastDesc!!.drawVecY
                vec2f2.x = drawBatchDesc.drawVecX + nextDesc!!.drawVecX
                vec2f2.y = drawBatchDesc.drawVecY + nextDesc!!.drawVecY
                vec2f2.normalizeSafe()
                vec2f.normalizeSafe()
                dirX = (vec2f.x + vec2f2.x) * 0.5f
                dirY = (vec2f.y + vec2f2.y) * 0.5f
            }

            updatePeak(i3, h, dt)

            drawBarAndPeak(
                renderState, px, py, dirX, dirY,
                fRound, h, fSignNonZero, color, blendMode, i3, drawScale
            )
        }
    }

    private fun updatePeak(index: Int, currentHeight: Float, dt: Float) {
        val currentAbs = Math.abs(currentHeight)
        if (currentAbs >= peaks[index]) {
            peaks[index] = currentAbs
            fallSpeeds[index] = 0.0f
        } else {
            fallSpeeds[index] += peakGravity * dt
            peaks[index] -= fallSpeeds[index] * dt
            if (peaks[index] < 0.0f) {
                peaks[index] = 0.0f
                fallSpeeds[index] = 0.0f
            }
        }
    }

    private fun drawBarAndPeak(
        renderState: RenderState,
        px: Float, py: Float,
        dx: Float, dy: Float,
        fRound: Float, h: Float,
        sign: Float, color: Int, blendMode: Int,
        index: Int, drawScale: Float
    ) {
        val tex = renderState.res.atlasTexWhite

        var startX = px
        var startY = py
        var barHeight = h

        if (this.mirror) {
            startX -= dx * h
            startY -= dy * h
            barHeight = h * 2.0f
        }

        val ccwX = (Vec2f.ccw90X(dx, dy) * fRound) + startX
        val ccwY = (Vec2f.ccw90Y(dx, dy) * fRound) + startY
        val cwX = (Vec2f.cw90X(dx, dy) * fRound) + startX
        val cwY = (Vec2f.cw90Y(dx, dy) * fRound) + startY
        val f10 = (dx * barHeight) + ccwX
        val f11 = (dy * barHeight) + ccwY
        val f12 = (dx * barHeight) + cwX
        val f13 = (dy * barHeight) + cwY

        val f2: Float
        val f3: Float
        val f4: Float
        val f5: Float
        if (this.useFixedLineHeight) {
            f2 = f10 + (dx * sign * this.fixedLineHeight)
            f3 = f11 + (dy * sign * this.fixedLineHeight)
            f4 = f12 + (dx * sign * this.fixedLineHeight)
            f5 = f13 + (dy * sign * this.fixedLineHeight)
        } else {
            f2 = ccwX
            f3 = ccwY
            f4 = cwX
            f5 = cwY
        }

        renderState.res.bufferRenderer.drawRectangle(
            renderState, f10, f11, f12, f13, f2, f3, f4, f5, 0.0f,
            color, Vec2f.zero(), Vec2f.one(), tex, blendMode
        )

        val tickHeight = peaks[index]
        val tickThickness = 2.0f * drawScale * 0.03f * peakThicknessMultiplier
        val tickColor = applyAlpha(color, peakColorMultiplier)

        if (this.mirror) {
            val posTickPx = px + dx * (tickHeight - tickThickness)
            val posTickPy = py + dy * (tickHeight - tickThickness)
            drawTickAt(renderState, posTickPx, posTickPy, dx, dy, fRound, tickThickness, tickColor, tex, blendMode)

            val negTickPx = px - dx * tickHeight
            val negTickPy = py - dy * tickHeight
            drawTickAt(renderState, negTickPx, negTickPy, dx, dy, fRound, tickThickness, tickColor, tex, blendMode)
        } else {
            val tickPx = px + dx * (tickHeight - tickThickness)
            val tickPy = py + dy * (tickHeight - tickThickness)
            drawTickAt(renderState, tickPx, tickPy, dx, dy, fRound, tickThickness, tickColor, tex, blendMode)
        }
    }

    private fun drawTickAt(
        renderState: RenderState,
        px: Float, py: Float,
        dx: Float, dy: Float,
        fRound: Float, thickness: Float,
        color: Int, tex: AtlasTexture, blendMode: Int
    ) {
        val ccwX = (Vec2f.ccw90X(dx, dy) * fRound) + px
        val ccwY = (Vec2f.ccw90Y(dx, dy) * fRound) + py
        val cwX = (Vec2f.cw90X(dx, dy) * fRound) + px
        val cwY = (Vec2f.cw90Y(dx, dy) * fRound) + py
        val f10 = (dx * thickness) + ccwX
        val f11 = (dy * thickness) + ccwY
        val f12 = (dx * thickness) + cwX
        val f13 = (dy * thickness) + cwY

        renderState.res.bufferRenderer.drawRectangle(
            renderState, f10, f11, f12, f13, ccwX, ccwY, cwX, cwY, 0.0f,
            color, Vec2f.zero(), Vec2f.one(), tex, blendMode
        )
    }

    private fun applyAlpha(color: Int, factor: Float): Int {
        val a = (((color shr 24) and 0xff) * factor).toInt() and 0xff
        val r = (color shr 16) and 0xff
        val g = (color shr 8) and 0xff
        val b = color and 0xff
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    override fun onApplyCustomization(customizationData: Element.CustomizationData) {
        super.onApplyCustomization(customizationData)
        this.barWidth = customizationData.getPropertyFloat("barWidth", 0.5f)
        this.barWidthAffectedByNormal = customizationData.getPropertyBool("barWidthAffectedByShape", false)
        this.mirror = customizationData.getPropertyBool("mirror", false)
        this.peakGravity = customizationData.getPropertyFloat("peakGravity", 25.0f)
        this.peakThicknessMultiplier = customizationData.getPropertyFloat("peakThicknessMultiplier", 1.0f)
        this.peakColorMultiplier = customizationData.getPropertyFloat("peakColorAlpha", 0.8f)
    }

    override fun onReadCustomization(outCustomizationData: Element.CustomizationData) {
        super.onReadCustomization(outCustomizationData)
        outCustomizationData.putPropertyFloat("barWidth", this.barWidth, "f 0.0 2.0")
        outCustomizationData.putPropertyBool("barWidthAffectedByShape", this.barWidthAffectedByNormal, "b")
        outCustomizationData.putPropertyBool("mirror", this.mirror, "b")
        outCustomizationData.putPropertyFloat("peakGravity", this.peakGravity, "f 1.0 100.0")
        outCustomizationData.putPropertyFloat("peakThicknessMultiplier", this.peakThicknessMultiplier, "f 0.1 5.0")
        outCustomizationData.putPropertyFloat("peakColorAlpha", this.peakColorMultiplier, "f 0.0 1.0")
    }

    companion object {
        const val typeName = "PeakBar"
    }
}