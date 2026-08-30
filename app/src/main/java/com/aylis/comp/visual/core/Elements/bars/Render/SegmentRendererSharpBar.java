

package com.aylis.comp.visual.core.Elements.bars.Render;

import android.graphics.PointF;
import com.aylis.Common.Vec2f;
import com.aylis.comp.visual.core.Elements.Element;
import com.aylis.comp.visual.core.Graphic.RenderState;

public class SegmentRendererSharpBar extends SegmentRendererBase {
    public static final String typeName = "SharpBars";
    private float barWidth = 0.5f;
    private boolean mirror = false;
    private PointF lastDrawPoint = new PointF();
    private PointF drawPoint = new PointF();

    public SegmentRendererSharpBar setBarWidth(float f) {
        this.barWidth = f;
        return this;
    }

    public SegmentRendererSharpBar setMirror(boolean z) {
        this.mirror = z;
        return this;
    }

    @Override
    public void drawSegment(RenderState renderState, DrawDesc drawDesc, float f) {
        float f2;
        float f3;
        float f4;
        float f5;
        drawDesc.color1 = getBarColorBase(drawDesc.valueIndex, drawDesc.valuesCount, f);
        this.lastDrawPoint.x = drawDesc.lastDrawPointX;
        this.lastDrawPoint.y = drawDesc.lastDrawPointY;
        this.drawPoint.x = drawDesc.drawPointX;
        this.drawPoint.y = drawDesc.drawPointY;
        float fRound = Math.round((drawDesc.drawSegmentWidth * 1.0f) / (drawDesc.valuesCount + 1)) * this.barWidth;
        float f6 = drawDesc.lastSegmentHeightVal * (-2.0f) * drawDesc.drawScale * this.barHeightMultiplier;
        float f7 = drawDesc.segmentHeightVal * (-2.0f) * drawDesc.drawScale * this.barHeightMultiplier;
        if (this.mirror) {
            this.lastDrawPoint.x -= drawDesc.lastDrawVecX * f6;
            this.lastDrawPoint.y -= drawDesc.lastDrawVecY * f6;
            f6 = f6 * 2.0f;
            this.drawPoint.x -= drawDesc.drawVecX * f7;
            this.drawPoint.y -= drawDesc.drawVecY * f7;
            f7 = f7 * 2.0f;
        }
        float fCcw90X = (Vec2f.ccw90X(drawDesc.drawVecX, drawDesc.drawVecY) * fRound) + this.drawPoint.x;
        float fCcw90Y = (Vec2f.ccw90Y(drawDesc.drawVecX, drawDesc.drawVecY) * fRound) + this.drawPoint.y;
        float fCw90X = (Vec2f.cw90X(drawDesc.drawVecX, drawDesc.drawVecY) * fRound) + this.drawPoint.x;
        float fCw90Y = (Vec2f.cw90Y(drawDesc.drawVecX, drawDesc.drawVecY) * fRound) + this.drawPoint.y;
        float f8 = (drawDesc.drawVecX * f6) + fCcw90X;
        float f9 = (drawDesc.drawVecY * f6) + fCcw90Y;
        float f10 = (drawDesc.drawVecX * f7) + fCw90X;
        float f11 = (drawDesc.drawVecY * f7) + fCw90Y;
        if (this.useFixedLineHeight) {
            float fSignum = Math.signum(f7);
            float f12 = (drawDesc.drawVecX * fSignum * this.fixedLineHeight) + f8;
            float f13 = (drawDesc.drawVecY * fSignum * this.fixedLineHeight) + f9;
            float f14 = (drawDesc.drawVecX * fSignum * this.fixedLineHeight) + f10;
            f2 = (drawDesc.drawVecY * fSignum * this.fixedLineHeight) + f11;
            f3 = f14;
            f4 = f13;
            f5 = f12;
        } else {
            f2 = fCw90Y;
            f3 = fCw90X;
            f4 = fCcw90Y;
            f5 = fCcw90X;
        }
        renderState.res.getBufferRenderer().drawRectangle(renderState, f8, f9, f10, f11, f5, f4, f3, f2, 0.0f, drawDesc.color1, Vec2f.zero(), Vec2f.one(), renderState.res.getAtlasTexWhite(), drawDesc.blendMode);
    }

    @Override
    public void drawSegmentBatch(RenderState renderState, DrawBatchDesc[] drawBatchDescArr, float f, float f2, int i, float f3) {
        float fCcw90Y;
        float f4;
        float f5;
        float fCcw90X;
        float fCw90Y;
        float f6;
        float f7;
        float fCw90X;
        float f8;
        float f9;
        float f10;
        float f11;
        DrawBatchDesc[] drawBatchDescArr2 = drawBatchDescArr;
        int length = drawBatchDescArr2.length;
        float fRound = Math.round((1.0f * f) / (length + 1)) * this.barWidth;
        int i2 = 0;
        while (i2 < drawBatchDescArr2.length) {
            DrawBatchDesc drawBatchDesc = drawBatchDescArr2[i2];
            if (drawBatchDesc.valueIndexLastToConnect >= 0) {
                DrawBatchDesc drawBatchDesc2 = drawBatchDescArr2[drawBatchDesc.valueIndexLastToConnect];
                int barColorBase = getBarColorBase(i2, length, f3);
                this.lastDrawPoint.x = drawBatchDesc2.drawPointX;
                this.lastDrawPoint.y = drawBatchDesc2.drawPointY;
                this.drawPoint.x = drawBatchDesc.drawPointX;
                this.drawPoint.y = drawBatchDesc.drawPointY;
                float f12 = drawBatchDesc2.segmentHeightVal * (-2.0f) * f2 * this.barHeightMultiplier;
                float f13 = drawBatchDesc.segmentHeightVal * (-2.0f) * f2 * this.barHeightMultiplier;
                if (this.mirror) {
                    this.lastDrawPoint.x -= drawBatchDesc2.drawVecX * f12;
                    this.lastDrawPoint.y -= drawBatchDesc2.drawVecY * f12;
                    f12 = f12 * 2.0f;
                    this.drawPoint.x -= drawBatchDesc.drawVecX * f13;
                    this.drawPoint.y -= drawBatchDesc.drawVecY * f13;
                    f13 = f13 * 2.0f;
                }
                if (f12 < 0.0f) {
                    fCcw90X = (Vec2f.ccw90X(drawBatchDesc.drawVecX, drawBatchDesc.drawVecY) * fRound) + this.drawPoint.x;
                    float fCcw90Y2 = (Vec2f.ccw90Y(drawBatchDesc.drawVecX, drawBatchDesc.drawVecY) * fRound) + this.drawPoint.y;
                    f5 = (drawBatchDesc.drawVecX * f12) + fCcw90X;
                    float f14 = (drawBatchDesc.drawVecY * f12) + fCcw90Y2;
                    f4 = fCcw90Y2;
                    fCcw90Y = f14;
                } else {
                    float fCcw90X2 = (Vec2f.ccw90X(drawBatchDesc.drawVecX, drawBatchDesc.drawVecY) * fRound) + this.drawPoint.x;
                    fCcw90Y = (Vec2f.ccw90Y(drawBatchDesc.drawVecX, drawBatchDesc.drawVecY) * fRound) + this.drawPoint.y;
                    float f15 = (drawBatchDesc.drawVecX * f12) + fCcw90X2;
                    f4 = (drawBatchDesc.drawVecY * f12) + fCcw90Y;
                    f5 = fCcw90X2;
                    fCcw90X = f15;
                }
                if (f13 < 0.0f) {
                    fCw90X = (Vec2f.cw90X(drawBatchDesc.drawVecX, drawBatchDesc.drawVecY) * fRound) + this.drawPoint.x;
                    float fCw90Y2 = (Vec2f.cw90Y(drawBatchDesc.drawVecX, drawBatchDesc.drawVecY) * fRound) + this.drawPoint.y;
                    f7 = (drawBatchDesc.drawVecX * f13) + fCw90X;
                    f6 = fCw90Y2;
                    fCw90Y = (drawBatchDesc.drawVecY * f13) + fCw90Y2;
                } else {
                    float fCw90X2 = (Vec2f.cw90X(drawBatchDesc.drawVecX, drawBatchDesc.drawVecY) * fRound) + this.drawPoint.x;
                    fCw90Y = (Vec2f.cw90Y(drawBatchDesc.drawVecX, drawBatchDesc.drawVecY) * fRound) + this.drawPoint.y;
                    float f16 = (drawBatchDesc.drawVecX * f13) + fCw90X2;
                    f6 = (drawBatchDesc.drawVecY * f13) + fCw90Y;
                    f7 = fCw90X2;
                    fCw90X = f16;
                }
                if (this.useFixedLineHeight) {
                    float fSignum = Math.signum(f13);
                    f9 = (drawBatchDesc.drawVecX * fSignum * this.fixedLineHeight) + f5;
                    float f17 = (drawBatchDesc.drawVecY * fSignum * this.fixedLineHeight) + fCcw90Y;
                    float f18 = (drawBatchDesc.drawVecX * fSignum * this.fixedLineHeight) + f7;
                    f10 = (drawBatchDesc.drawVecY * fSignum * this.fixedLineHeight) + fCw90Y;
                    f11 = f17;
                    f8 = f18;
                } else {
                    f8 = fCw90X;
                    f9 = fCcw90X;
                    f10 = f6;
                    f11 = f4;
                }
                renderState.res.getBufferRenderer().drawRectangle(renderState, f5, fCcw90Y, f7, fCw90Y, f9, f11, f8, f10, 0.0f, barColorBase, Vec2f.zero(), Vec2f.one(), renderState.res.getAtlasTexWhite(), i);
            }
            i2++;
            drawBatchDescArr2 = drawBatchDescArr;
        }
    }

    @Override
    public void onApplyCustomization(Element.CustomizationData customizationData) {
        super.onApplyCustomization(customizationData);
        this.barWidth = customizationData.getPropertyFloat("barWidth", 0.5f);
        this.mirror = customizationData.getPropertyBool("mirror", false);
    }

    @Override
    public void onReadCustomization(Element.CustomizationData outCustomizationData) {
        super.onReadCustomization(outCustomizationData);
        outCustomizationData.putPropertyFloat("barWidth", this.barWidth, "f 0.0 2.0");
        outCustomizationData.putPropertyBool("mirror", this.mirror, "misc");
    }
}
