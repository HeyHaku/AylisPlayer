

package com.aylis.comp.visual.core.Elements.bars.Render

import com.aylis.Common.Vec2f
import com.aylis.comp.visual.core.Elements.Element
import com.aylis.comp.visual.core.Graphic.AtlasTexture
import com.aylis.comp.visual.core.Graphic.RenderState

class SegmentRendererDots : SegmentRendererBase() {

    private var lastDesc: ISegmentRenderer.DrawBatchDesc? = null
    private var nextDesc: ISegmentRenderer.DrawBatchDesc? = null

    var barWidth = 0.5f
    var mirror = false
    var barWidthAffectedByNormal = false
    var dotSizeMultiplier = 1.0f

    fun setBarWidth(f: Float): SegmentRendererDots {
        this.barWidth = f
        return this
    }

    fun setMirror(z: Boolean): SegmentRendererDots {
        this.mirror = z
        return this
    }

    override fun drawSegment(renderState: RenderState, drawDesc: ISegmentRenderer.DrawDesc, blendMode: Float) {
        drawDesc.color1 = getBarColorBase(drawDesc.valueIndex, drawDesc.valuesCount, blendMode)
        val fRound = Math.round((drawDesc.drawSegmentWidth * 0.5f) / (drawDesc.valuesCount + 1)) * this.barWidth * this.dotSizeMultiplier
        val px = drawDesc.drawPointX
        val py = drawDesc.drawPointY
        val h = drawDesc.segmentHeightVal * (-2.0f) * drawDesc.drawScale * this.barHeightMultiplier

        drawPeakDot(renderState, px, py, drawDesc.drawVecX, drawDesc.drawVecY, fRound, h, drawDesc.color1, drawDesc.blendMode)
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
        val fRound = Math.round((0.5f * drawSegmentWidth) / (length + 1)) * this.barWidth * this.dotSizeMultiplier
        val vec2f = Vec2f(0.0f, 0.0f)
        val vec2f2 = Vec2f(0.0f, 0.0f)

        for (i3 in drawBatchDescArr.indices) {
            val drawBatchDesc = drawBatchDescArr[i3]
            this.lastDesc = if (drawBatchDesc.valueIndexLastToConnect < 0) drawBatchDesc else drawBatchDescArr[drawBatchDesc.valueIndexLastToConnect]
            this.nextDesc = if (drawBatchDesc.valueIndexNextToConnect < 0) drawBatchDesc else drawBatchDescArr[drawBatchDesc.valueIndexNextToConnect]

            val color = getBarColorBase(i3, length, colorIndexOffset)
            val px = drawBatchDesc.drawPointX
            val py = drawBatchDesc.drawPointY
            val h = drawBatchDesc.segmentHeightVal * (-2.0f) * drawScale * this.barHeightMultiplier

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

            drawPeakDot(renderState, px, py, dirX, dirY, fRound, h, color, blendMode)
        }
    }

    private fun drawPeakDot(
        renderState: RenderState,
        px: Float, py: Float,
        dx: Float, dy: Float,
        fRound: Float, h: Float,
        color: Int, blendMode: Int
    ) {
        val tex = renderState.res.atlasTexWhite

        if (this.mirror) {

            val posCx = px + dx * h
            val posCy = py + dy * h
            drawDotAt(renderState, posCx, posCy, dx, dy, fRound, color, tex, blendMode)

            val negCx = px - dx * h
            val negCy = py - dy * h
            drawDotAt(renderState, negCx, negCy, dx, dy, fRound, color, tex, blendMode)
        } else {
            val cx = px + dx * h
            val cy = py + dy * h
            drawDotAt(renderState, cx, cy, dx, dy, fRound, color, tex, blendMode)
        }
    }

    private fun drawDotAt(
        renderState: RenderState,
        cx: Float, cy: Float,
        dx: Float, dy: Float,
        fRound: Float, color: Int,
        tex: AtlasTexture, blendMode: Int
    ) {
        val ccwX = (Vec2f.ccw90X(dx, dy) * fRound) + cx
        val ccwY = (Vec2f.ccw90Y(dx, dy) * fRound) + cy
        val cwX = (Vec2f.cw90X(dx, dy) * fRound) + cx
        val cwY = (Vec2f.cw90Y(dx, dy) * fRound) + cy
        val f10 = (dx * fRound) + ccwX
        val f11 = (dy * fRound) + ccwY
        val f12 = (dx * fRound) + cwX
        val f13 = (dy * fRound) + cwY
        val f2 = (dx * -fRound) + ccwX
        val f3 = (dy * -fRound) + ccwY
        val f4 = (dx * -fRound) + cwX
        val f5 = (dy * -fRound) + cwY

        renderState.res.bufferRenderer.drawRectangle(
            renderState, f10, f11, f12, f13, f2, f3, f4, f5, 0.0f,
            color, Vec2f.zero(), Vec2f.one(), tex, blendMode
        )
    }

    override fun onApplyCustomization(customizationData: Element.CustomizationData) {
        super.onApplyCustomization(customizationData)
        this.barWidth = customizationData.getPropertyFloat("barWidth", 0.5f)
        this.barWidthAffectedByNormal = customizationData.getPropertyBool("barWidthAffectedByShape", false)
        this.mirror = customizationData.getPropertyBool("mirror", false)
        this.dotSizeMultiplier = customizationData.getPropertyFloat("dotSizeMultiplier", 1.0f)
    }

    override fun onReadCustomization(outCustomizationData: Element.CustomizationData) {
        super.onReadCustomization(outCustomizationData)
        outCustomizationData.putPropertyFloat("barWidth", this.barWidth, "f 0.0 2.0")
        outCustomizationData.putPropertyBool("barWidthAffectedByShape", this.barWidthAffectedByNormal, "b")
        outCustomizationData.putPropertyBool("mirror", this.mirror, "b")
        outCustomizationData.putPropertyFloat("dotSizeMultiplier", this.dotSizeMultiplier, "f 0.1 5.0")
    }

    companion object {
        const val typeName = "Dots"
    }
}