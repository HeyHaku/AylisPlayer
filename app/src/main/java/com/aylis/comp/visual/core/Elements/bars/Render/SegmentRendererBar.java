

package com.aylis.comp.visual.core.Elements.bars.Render;

import com.aylis.Common.Vec2f;
import com.aylis.comp.visual.core.Elements.Element;
import com.aylis.comp.visual.core.Graphic.RenderState;

public class SegmentRendererBar extends SegmentRendererBase {
    public static final String typeName = "Bars";
    private DrawBatchDesc lastDesc;
    private DrawBatchDesc nextDesc;
    private float barWidth = 0.5f;
    private boolean mirror = false;
    protected boolean barWidthAffectedByNormal = false;

    public SegmentRendererBar setBarWidth(float f) {
        this.barWidth = f;
        return this;
    }

    public SegmentRendererBar setMirror(boolean z) {
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
        float fRound = Math.round((drawDesc.drawSegmentWidth * 0.5f) / (drawDesc.valuesCount + 1)) * this.barWidth;
        float f6 = drawDesc.drawPointX;
        float f7 = drawDesc.drawPointY;
        float f8 = drawDesc.segmentHeightVal * (-2.0f) * drawDesc.drawScale;
        float fSignNonZern = f8 >= 0.0f ? 1.0f : -1.0f;
        float f9 = f8 * this.barHeightMultiplier;
        if (this.mirror) {
            f6 -= drawDesc.drawVecX * f9;
            f7 -= drawDesc.drawVecY * f9;
            f9 = f9 * 2.0f;
        }
        float fCcw90X = (Vec2f.ccw90X(drawDesc.drawVecX, drawDesc.drawVecY) * fRound) + f6;
        float fCcw90Y = (Vec2f.ccw90Y(drawDesc.drawVecX, drawDesc.drawVecY) * fRound) + f7;
        float fCw90X = (Vec2f.cw90X(drawDesc.drawVecX, drawDesc.drawVecY) * fRound) + f6;
        float fCw90Y = (Vec2f.cw90Y(drawDesc.drawVecX, drawDesc.drawVecY) * fRound) + f7;
        float f10 = (drawDesc.drawVecX * f9) + fCcw90X;
        float f11 = (drawDesc.drawVecY * f9) + fCcw90Y;
        float f12 = (drawDesc.drawVecX * f9) + fCw90X;
        float f13 = (drawDesc.drawVecY * f9) + fCw90Y;
        if (this.useFixedLineHeight) {
            float f14 = (drawDesc.drawVecX * fSignNonZern * this.fixedLineHeight) + f10;
            f3 = (drawDesc.drawVecY * fSignNonZern * this.fixedLineHeight) + f11;
            f4 = (drawDesc.drawVecX * fSignNonZern * this.fixedLineHeight) + f12;
            f5 = (drawDesc.drawVecY * fSignNonZern * this.fixedLineHeight) + f13;
            f2 = f14;
        } else {
            f2 = fCcw90X;
            f3 = fCcw90Y;
            f4 = fCw90X;
            f5 = fCw90Y;
        }
        renderState.res.getBufferRenderer().drawRectangle(renderState, f10, f11, f12, f13, f2, f3, f4, f5, 0.0f, drawDesc.color1, Vec2f.zero(), Vec2f.one(), renderState.res.getAtlasTexWhite(), drawDesc.blendMode);
    }

    @Override
    public void drawSegmentBatch(RenderState renderState, DrawBatchDesc[] drawBatchDescArr, float f, float f2, int i, float f3) {
        int i2;
        float fCcw90Y;
        float fCw90X;
        float fCw90Y;
        float f4;
        float f5;
        float f6;
        float f7;
        float f8;
        float f9;
        float f10;
        float f11;
        float f12;
        DrawBatchDesc[] drawBatchDescArr2 = drawBatchDescArr;
        int length = drawBatchDescArr2.length;
        float fRound = Math.round((0.5f * f) / (length + 1)) * this.barWidth;
        Vec2f vec2f = new Vec2f(0.0f, 0.0f);
        Vec2f vec2f2 = new Vec2f(0.0f, 0.0f);
        boolean renderEdges = renderState.getRenderEdges();
        int i3 = 0;
        while (i3 < drawBatchDescArr2.length) {
            DrawBatchDesc drawBatchDesc = drawBatchDescArr2[i3];
            this.lastDesc = drawBatchDesc.valueIndexLastToConnect < 0 ? drawBatchDesc : drawBatchDescArr2[drawBatchDesc.valueIndexLastToConnect];
            this.nextDesc = drawBatchDesc.valueIndexNextToConnect < 0 ? drawBatchDesc : drawBatchDescArr2[drawBatchDesc.valueIndexNextToConnect];
            int barColorBase = getBarColorBase(i3, length, f3);
            float f13 = drawBatchDesc.drawPointX;
            float f14 = drawBatchDesc.drawPointY;
            float f15 = drawBatchDesc.segmentHeightVal * (-2.0f) * f2;
            float fSignNonZern = f15 >= 0.0f ? 1.0f : -1.0f;
            float f16 = f15 * this.barHeightMultiplier;
            if (this.mirror) {
                f13 -= drawBatchDesc.drawVecX * f16;
                f14 -= drawBatchDesc.drawVecY * f16;
                i2 = length;
                f16 = f16 * 2.0f;
            } else {
                i2 = length;
            }
            if (this.barWidthAffectedByNormal) {
                vec2f.x = drawBatchDesc.drawVecX + this.lastDesc.drawVecX;
                vec2f.y = drawBatchDesc.drawVecY + this.lastDesc.drawVecY;
                vec2f2.x = drawBatchDesc.drawVecX + this.nextDesc.drawVecX;
                vec2f2.y = drawBatchDesc.drawVecY + this.nextDesc.drawVecY;
                vec2f2.normalizeSafe();
                vec2f.normalizeSafe();
                float fCcw90X = (Vec2f.ccw90X(drawBatchDesc.drawVecX, drawBatchDesc.drawVecY) * fRound) + f13;
                fCcw90Y = (Vec2f.ccw90Y(drawBatchDesc.drawVecX, drawBatchDesc.drawVecY) * fRound) + f14;
                fCw90X = (Vec2f.cw90X(drawBatchDesc.drawVecX, drawBatchDesc.drawVecY) * fRound) + f13;
                fCw90Y = (Vec2f.cw90Y(drawBatchDesc.drawVecX, drawBatchDesc.drawVecY) * fRound) + f14;
                f4 = (vec2f.x * f16) + fCcw90X;
                f5 = (vec2f.y * f16) + fCcw90Y;
                f6 = fCcw90X;
                f7 = (vec2f2.x * f16) + fCw90X;
                f8 = vec2f2.y;
            } else {
                float fCcw90X2 = (Vec2f.ccw90X(drawBatchDesc.drawVecX, drawBatchDesc.drawVecY) * fRound) + f13;
                fCcw90Y = (Vec2f.ccw90Y(drawBatchDesc.drawVecX, drawBatchDesc.drawVecY) * fRound) + f14;
                fCw90X = (Vec2f.cw90X(drawBatchDesc.drawVecX, drawBatchDesc.drawVecY) * fRound) + f13;
                fCw90Y = (Vec2f.cw90Y(drawBatchDesc.drawVecX, drawBatchDesc.drawVecY) * fRound) + f14;
                f4 = (drawBatchDesc.drawVecX * f16) + fCcw90X2;
                f5 = (drawBatchDesc.drawVecY * f16) + fCcw90Y;
                f6 = fCcw90X2;
                f7 = (drawBatchDesc.drawVecX * f16) + fCw90X;
                f8 = drawBatchDesc.drawVecY;
            }
            float f17 = (f8 * f16) + fCw90Y;
            float f18 = f5;
            float f19 = f7;
            float f20 = f6;
            float f21 = f4;
            if (this.useFixedLineHeight) {
                float f22 = f21 + (drawBatchDesc.drawVecX * fSignNonZern * this.fixedLineHeight);
                float f23 = f18 + (drawBatchDesc.drawVecY * fSignNonZern * this.fixedLineHeight);
                float f24 = f19 + (drawBatchDesc.drawVecX * fSignNonZern * this.fixedLineHeight);
                f9 = f22;
                f10 = f23;
                f12 = f17 + (drawBatchDesc.drawVecY * fSignNonZern * this.fixedLineHeight);
                f11 = f24;
            } else {
                f9 = f20;
                f10 = fCcw90Y;
                f11 = fCw90X;
                f12 = fCw90Y;
            }
            renderState.res.getBufferRenderer().drawRectangle(renderState, f21, f18, f19, f17, f9, f10, f11, f12, 0.0f, barColorBase, Vec2f.zero(), Vec2f.one(), renderState.res.getAtlasTexWhite(), i);
            i3++;
            drawBatchDescArr2 = drawBatchDescArr;
            length = i2;
        }
    }

    @Override
    public void onApplyCustomization(Element.CustomizationData customizationData) {
        super.onApplyCustomization(customizationData);
        this.barWidth = customizationData.getPropertyFloat("barWidth", 0.5f);
        this.barWidthAffectedByNormal = customizationData.getPropertyBool("barWidthAffectedByShape", false);
        this.mirror = customizationData.getPropertyBool("mirror", false);
    }

    @Override
    public void onReadCustomization(Element.CustomizationData outCustomizationData) {
        super.onReadCustomization(outCustomizationData);
        outCustomizationData.putPropertyFloat("barWidth", this.barWidth, "f 0.0 2.0");
        outCustomizationData.putPropertyBool("barWidthAffectedByShape", this.barWidthAffectedByNormal, "b");
        outCustomizationData.putPropertyBool("mirror", this.mirror, "b");
    }
}
