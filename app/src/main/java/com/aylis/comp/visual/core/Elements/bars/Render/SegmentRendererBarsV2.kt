

package com.aylis.comp.visual.core.Elements.bars.Render

import com.aylis.Common.Vec2f
import com.aylis.comp.visual.core.Elements.Element
import com.aylis.comp.visual.core.Graphic.RenderState

class SegmentRendererBarsV2 : SegmentRendererBase() {

    private var lastDesc: ISegmentRenderer.DrawBatchDesc? = null
    private var nextDesc: ISegmentRenderer.DrawBatchDesc? = null

    var barWidth = 0.5f
    var mirror = false
    var barWidthAffectedByNormal = false

    var glowWidth = 1.5f
    var glowAlpha = 0.3f

    fun setBarWidth(f: Float): SegmentRendererBarsV2 {
        this.barWidth = f
        return this
    }

    fun setMirror(z: Boolean): SegmentRendererBarsV2 {
        this.mirror = z
        return this
    }

    override fun drawSegment(renderState: RenderState, drawDesc: ISegmentRenderer.DrawDesc, blendMode: Float) {
        drawDesc.color1 = getBarColorBase(drawDesc.valueIndex, drawDesc.valuesCount, blendMode)
        val fRound = Math.round((drawDesc.drawSegmentWidth * 0.5f) / (drawDesc.valuesCount + 1)) * this.barWidth
        var f6 = drawDesc.drawPointX
        var f7 = drawDesc.drawPointY
        val f8 = drawDesc.segmentHeightVal * (-2.0f) * drawDesc.drawScale
        val fSignNonZero = if (f8 >= 0.0f) 1.0f else -1.0f
        var f9 = f8 * this.barHeightMultiplier

        if (this.mirror) {
            f6 -= drawDesc.drawVecX * f9
            f7 -= drawDesc.drawVecY * f9
            f9 *= 2.0f
        }

        drawRectangleWithGlow(
            renderState, f6, f7, drawDesc.drawVecX, drawDesc.drawVecY,
            fRound, f9, fSignNonZero, drawDesc.color1, drawDesc.blendMode
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

        for (i3 in drawBatchDescArr.indices) {
            val drawBatchDesc = drawBatchDescArr[i3]
            this.lastDesc = if (drawBatchDesc.valueIndexLastToConnect < 0) drawBatchDesc else drawBatchDescArr[drawBatchDesc.valueIndexLastToConnect]
            this.nextDesc = if (drawBatchDesc.valueIndexNextToConnect < 0) drawBatchDesc else drawBatchDescArr[drawBatchDesc.valueIndexNextToConnect]

            val color = getBarColorBase(i3, length, colorIndexOffset)
            var f13 = drawBatchDesc.drawPointX
            var f14 = drawBatchDesc.drawPointY
            val f15 = drawBatchDesc.segmentHeightVal * (-2.0f) * drawScale
            val fSignNonZero = if (f15 >= 0.0f) 1.0f else -1.0f
            var f16 = f15 * this.barHeightMultiplier

            if (this.mirror) {
                f13 -= drawBatchDesc.drawVecX * f16
                f14 -= drawBatchDesc.drawVecY * f16
                f16 *= 2.0f
            }

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

            drawRectangleWithGlow(
                renderState, f13, f14, dirX, dirY,
                fRound, f16, fSignNonZero, color, blendMode
            )
        }
    }

    private fun drawRectangleWithGlow(
        renderState: RenderState,
        px: Float, py: Float,
        dx: Float, dy: Float,
        halfWidth: Float, height: Float,
        sign: Float, color: Int, blendMode: Int
    ) {
        val tex = renderState.res.atlasTexWhite

        if (glowAlpha > 0.01f) {
            val glowW = halfWidth * glowWidth
            val glowH = height * (1.0f + (glowWidth - 1.0f) * 0.1f)
            val glowColor = applyAlpha(color, glowAlpha)

            val ccwX = (Vec2f.ccw90X(dx, dy) * glowW) + px
            val ccwY = (Vec2f.ccw90Y(dx, dy) * glowW) + py
            val cwX = (Vec2f.cw90X(dx, dy) * glowW) + px
            val cwY = (Vec2f.cw90Y(dx, dy) * glowW) + py
            val f10 = (dx * glowH) + ccwX
            val f11 = (dy * glowH) + ccwY
            val f12 = (dx * glowH) + cwX
            val f13 = (dy * glowH) + cwY

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
                glowColor, Vec2f.zero(), Vec2f.one(), tex, blendMode
            )
        }

        val ccwX = (Vec2f.ccw90X(dx, dy) * halfWidth) + px
        val ccwY = (Vec2f.ccw90Y(dx, dy) * halfWidth) + py
        val cwX = (Vec2f.cw90X(dx, dy) * halfWidth) + px
        val cwY = (Vec2f.cw90Y(dx, dy) * halfWidth) + py
        val f10 = (dx * height) + ccwX
        val f11 = (dy * height) + ccwY
        val f12 = (dx * height) + cwX
        val f13 = (dy * height) + cwY

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
    }

    private fun applyAlpha(color: Int, alphaFactor: Float): Int {
        val a = (((color shr 24) and 0xff) * alphaFactor).toInt() and 0xff
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
        this.glowWidth = customizationData.getPropertyFloat("glowWidth", 1.5f)
        this.glowAlpha = customizationData.getPropertyFloat("glowAlpha", 0.3f)
    }

    override fun onReadCustomization(outCustomizationData: Element.CustomizationData) {
        super.onReadCustomization(outCustomizationData)
        outCustomizationData.putPropertyFloat("barWidth", this.barWidth, "f 0.0 2.0")
        outCustomizationData.putPropertyBool("barWidthAffectedByShape", this.barWidthAffectedByNormal, "b")
        outCustomizationData.putPropertyBool("mirror", this.mirror, "b")
        outCustomizationData.putPropertyFloat("glowWidth", this.glowWidth, "f 1.0 3.0")
        outCustomizationData.putPropertyFloat("glowAlpha", this.glowAlpha, "f 0.0 1.0")
    }

    companion object {
        const val typeName = "BarsV2"
    }
}