

package com.aylis.comp.visual.core.Elements.bars.Render;

import android.graphics.PointF;
import com.aylis.Common.Vec2f;
import com.aylis.comp.visual.core.Elements.Element;
import com.aylis.comp.visual.core.Graphic.RenderState;

public class SegmentRendererLine extends SegmentRendererBase {
    public static final String typeName = "Line";
    DrawBatchDesc desc;
    DrawBatchDesc[] descs;
    float drawScale;
    DrawBatchDesc lastDesc;
    DrawBatchDesc lastLastDesc;
    float lx1;
    float lx3;
    float ly1;
    float ly3;
    DrawBatchDesc nextDesc;
    float x1;
    float x3;
    float y1;
    float y3;
    PointF lastDrawPoint = new PointF();
    PointF drawPoint = new PointF();
    PointF nextDrawPoint = new PointF();
    Vec2f normal1 = new Vec2f(0.0f, 0.0f);
    Vec2f normal2 = new Vec2f(0.0f, 0.0f);
    Vec2f vec1 = new Vec2f(0.0f, 0.0f);
    Vec2f outFixedLinePosOffset = new Vec2f(0.0f, 0.0f);
    Vec2f c0 = new Vec2f(0.0f, 0.0f);
    Vec2f c1 = new Vec2f(0.0f, 0.0f);
    Vec2f c2 = new Vec2f(0.0f, 0.0f);
    Vec2f c3 = new Vec2f(0.0f, 0.0f);
    Vec2f normal0tmp = new Vec2f(0.0f, 0.0f);
    Vec2f normal1tmp = new Vec2f(0.0f, 0.0f);
    Vec2f normal2tmp = new Vec2f(0.0f, 0.0f);
    Vec2f normal3tmp = new Vec2f(0.0f, 0.0f);
    Vec2f normal0ltmp = new Vec2f(0.0f, 0.0f);
    Vec2f normal0ntmp = new Vec2f(0.0f, 0.0f);
    Vec2f normal2ltmp = new Vec2f(0.0f, 0.0f);
    Vec2f normal2ntmp = new Vec2f(0.0f, 0.0f);
    private boolean mirror = true;
    private boolean flipEveryOther = false;
    private float tmpColorOffsetBlend = 0.0f;

    public SegmentRendererLine setMirror(boolean z) {
        this.mirror = z;
        return this;
    }

    public SegmentRendererLine setFlipEveryOther(boolean z) {
        this.flipEveryOther = z;
        return this;
    }

    @Override
    public void drawSegment(RenderState renderState, DrawDesc drawDesc, float f) {
        float f2 = drawDesc.segmentHeightVal;
        float f3 = drawDesc.lastSegmentHeightVal;
        if (this.flipEveryOther) {
            if (drawDesc.valueIndex % 2 == 0) {
                f2 *= -1.0f;
            } else {
                f3 *= -1.0f;
            }
        }
        drawDesc.color1 = getBarColorBase(drawDesc.valueIndex, drawDesc.valuesCount, f);
        this.lastDrawPoint.x = drawDesc.lastDrawPointX;
        this.lastDrawPoint.y = drawDesc.lastDrawPointY;
        this.drawPoint.x = drawDesc.drawPointX;
        this.drawPoint.y = drawDesc.drawPointY;
        float fRound = Math.round((drawDesc.drawSegmentWidth * 1.0f) / (drawDesc.valuesCount + 1)) * 0.5f;
        float f4 = f3 * (-2.0f) * drawDesc.drawScale * this.barHeightMultiplier;
        float f5 = f2 * (-2.0f) * drawDesc.drawScale * this.barHeightMultiplier;
        if (this.mirror) {
            this.lastDrawPoint.x -= drawDesc.lastDrawVecX * f4;
            this.lastDrawPoint.y -= drawDesc.lastDrawVecY * f4;
            f4 = f4 * 2.0f;
            this.drawPoint.x -= drawDesc.drawVecX * f5;
            this.drawPoint.y -= drawDesc.drawVecY * f5;
            f5 = f5 * 2.0f;
        }
        this.lx3 = (Vec2f.cw90X(drawDesc.lastDrawVecX, drawDesc.lastDrawVecY) * fRound) + this.lastDrawPoint.x;
        this.ly3 = (Vec2f.cw90Y(drawDesc.lastDrawVecX, drawDesc.lastDrawVecY) * fRound) + this.lastDrawPoint.y;
        this.x3 = (Vec2f.cw90X(drawDesc.drawVecX, drawDesc.drawVecY) * fRound) + this.drawPoint.x;
        this.y3 = (Vec2f.cw90Y(drawDesc.drawVecX, drawDesc.drawVecY) * fRound) + this.drawPoint.y;
        this.lx1 = (drawDesc.lastDrawVecX * f4) + this.lx3;
        this.ly1 = (drawDesc.lastDrawVecY * f4) + this.ly3;
        this.x1 = (drawDesc.drawVecX * f5) + this.x3;
        this.y1 = (drawDesc.drawVecY * f5) + this.y3;
        if (this.useFixedLineHeight) {
            float fSignum = Math.signum(drawDesc.drawScale * (-1.0f) * this.barHeightMultiplier);
            this.lx3 = this.lx1 + (drawDesc.lastDrawVecX * fSignum * this.fixedLineHeight);
            this.ly3 = this.ly1 + (drawDesc.lastDrawVecY * fSignum * this.fixedLineHeight);
            this.x3 = this.x1 + (drawDesc.drawVecX * fSignum * this.fixedLineHeight);
            this.y3 = this.y1 + (drawDesc.drawVecY * fSignum * this.fixedLineHeight);
        }
        renderState.res.getBufferRenderer().drawRectangle(renderState, this.lx1, this.ly1, this.x1, this.y1, this.lx3, this.ly3, this.x3, this.y3, 0.0f, drawDesc.color1, Vec2f.zero(), Vec2f.one(), renderState.res.getAtlasTexWhite(), drawDesc.blendMode);
    }

    @Override
    public void drawSegmentBatch(RenderState renderState, DrawBatchDesc[] drawBatchDescArr, float f, float f2, int i, float f3) {
        this.descs = drawBatchDescArr;
        this.drawScale = f2;
        this.descs = drawBatchDescArr;
        this.drawScale = f2;
        this.tmpColorOffsetBlend = f3;
        int length = drawBatchDescArr.length;

        for (int i2 = 0; i2 < length; i2++) {
            DrawBatchDesc desc = drawBatchDescArr[i2];
            if (desc.valueIndexLastToConnect >= 0 && desc.valueIndexLastToConnect < length) {
                DrawBatchDesc lastDesc = drawBatchDescArr[desc.valueIndexLastToConnect];
                int barColorBase = getBarColorBase(i2, length, f3);
                float f4 = lastDesc.segmentHeightVal * (-2.0f) * f2 * this.barHeightMultiplier;
                float f5 = desc.segmentHeightVal * (-2.0f) * f2 * this.barHeightMultiplier;
                float lx = lastDesc.drawPointX;
                float ly = lastDesc.drawPointY;
                float cx = desc.drawPointX;
                float cy = desc.drawPointY;
                if (this.mirror) {
                    lx -= lastDesc.drawVecX * f4;
                    ly -= lastDesc.drawVecY * f4;
                    f4 *= 2.0f;
                    cx -= desc.drawVecX * f5;
                    cy -= desc.drawVecY * f5;
                    f5 *= 2.0f;
                }
                float lx1 = (lastDesc.drawVecX * f4) + lx;
                float ly1 = (lastDesc.drawVecY * f4) + ly;
                float x1 = (desc.drawVecX * f5) + cx;
                float y1 = (desc.drawVecY * f5) + cy;
                renderState.res.getBufferRenderer().drawRectangle(renderState, lx1, ly1, x1, y1, lx, ly, cx, cy, 0.0f, barColorBase, Vec2f.zero(), Vec2f.one(), renderState.res.getAtlasTexWhite(), i);
            }
        }
    }

    private void getFixedLinePosOffset(DrawBatchDesc drawBatchDesc, Vec2f vec2f, float f, int i) {
        if (drawBatchDesc.valueIndexNextToConnect < 0) {
            this.nextDesc = drawBatchDesc;
            this.normal2.x = 0.0f;
            this.normal2.y = 0.0f;
        } else {
            DrawBatchDesc drawBatchDesc2 = this.descs[drawBatchDesc.valueIndexNextToConnect];
            this.nextDesc = drawBatchDesc2;
            this.normal2.x = -(drawBatchDesc2.getEndPointY2(i) - drawBatchDesc.getEndPointY2(i));
            this.normal2.y = this.nextDesc.getEndPointX2(i) - drawBatchDesc.getEndPointX2(i);
            this.normal2.normalize();
        }
        if (drawBatchDesc.valueIndexLastToConnect < 0) {
            drawBatchDesc.neighborMiddleVecX = this.normal2.x;
            drawBatchDesc.neighborMiddleVecY = this.normal2.y;
            vec2f.x = drawBatchDesc.neighborMiddleVecX * f;
            vec2f.y = drawBatchDesc.neighborMiddleVecY * f;
            return;
        }
        DrawBatchDesc drawBatchDesc3 = this.descs[drawBatchDesc.valueIndexLastToConnect];
        this.normal1.x = -(drawBatchDesc.getEndPointY2(i) - drawBatchDesc3.getEndPointY2(i));
        this.normal1.y = drawBatchDesc.getEndPointX2(i) - drawBatchDesc3.getEndPointX2(i);
        this.normal1.normalize();
        this.vec1.x = this.normal1.x + this.normal2.x;
        this.vec1.y = this.normal1.y + this.normal2.y;
        this.vec1.normalize();
        drawBatchDesc.neighborMiddleVecX = this.vec1.x;
        drawBatchDesc.neighborMiddleVecY = this.vec1.y;
        Vec2f vec2f2 = this.normal1;
        vec2f2.x = -vec2f2.x;
        Vec2f vec2f3 = this.normal1;
        vec2f3.y = -vec2f3.y;
        float fMax = f / Math.max(Math.abs(Vec2f.dot(drawBatchDesc.neighborMiddleVecX, drawBatchDesc.neighborMiddleVecY, this.normal1.x, this.normal1.y)), 0.25f);
        vec2f.x = drawBatchDesc.neighborMiddleVecX * 1.0f * fMax;
        vec2f.y = drawBatchDesc.neighborMiddleVecY * 1.0f * fMax;
    }

    private void getFixedLinePosOffset(float f, float f2, float f3, float f4, Vec2f vec2f, float f5, boolean z) {
        this.normal2.x = f3;
        this.normal2.y = f4;
        if (!z) {
            float f6 = this.normal2.x;
            float f7 = this.normal2.y;
            vec2f.x = f6 * f5;
            vec2f.y = f7 * f5;
            return;
        }
        this.normal1.x = f;
        this.normal1.y = f2;
        this.normal1.normalize();
        this.vec1.x = this.normal1.x + this.normal2.x;
        this.vec1.y = this.normal1.y + this.normal2.y;
        this.vec1.normalize();
        float f8 = this.vec1.x;
        float f9 = this.vec1.y;
        Vec2f vec2f2 = this.normal1;
        vec2f2.x = -vec2f2.x;
        Vec2f vec2f3 = this.normal1;
        vec2f3.y = -vec2f3.y;
        float fMax = f5 / Math.max(Math.abs(Vec2f.dot(f8, f9, this.normal1.x, this.normal1.y)), 0.25f);
        vec2f.x = f8 * 1.0f * fMax;
        vec2f.y = f9 * 1.0f * fMax;
    }

    public Boolean Invoke(Integer num, float[] fArr, int[] iArr) {
        DrawBatchDesc drawBatchDesc = this.descs[num.intValue()];
        this.desc = drawBatchDesc;
        if (drawBatchDesc.valueIndexLastToConnect >= 0) {
            this.lastDesc = this.descs[this.desc.valueIndexLastToConnect];
            int barColorBase = getBarColorBase(this.desc.valueIndexLastToConnect, this.descs.length, this.tmpColorOffsetBlend);
            int barColorBase2 = getBarColorBase(num.intValue(), this.descs.length, this.tmpColorOffsetBlend);
            iArr[0] = barColorBase;
            iArr[1] = barColorBase2;
            iArr[2] = iArr[0];
            iArr[3] = iArr[1];
            this.lx1 = this.lastDesc.render1X;
            this.ly1 = this.lastDesc.render1Y;
            this.lx3 = this.lastDesc.render2X;
            this.ly3 = this.lastDesc.render2Y;
            this.x1 = this.desc.render1X;
            this.y1 = this.desc.render1Y;
            this.x3 = this.desc.render2X;
            float f = this.desc.render2Y;
            this.y3 = f;
            fArr[0] = this.lx1;
            fArr[1] = this.ly1;
            fArr[2] = this.x1;
            fArr[3] = this.y1;
            fArr[4] = this.lx3;
            fArr[5] = this.ly3;
            fArr[6] = this.x3;
            fArr[7] = f;
            return true;
        }
        return false;
    }

    public Boolean Invoke(Integer num, float[] fArr, int[] iArr, float[] fArr2) {
        DrawBatchDesc drawBatchDesc = this.descs[num.intValue()];
        this.desc = drawBatchDesc;
        if (drawBatchDesc.valueIndexLastToConnect >= 0) {
            this.lastDesc = this.descs[this.desc.valueIndexLastToConnect];
            iArr[1] = getBarColorBase(num.intValue(), this.descs.length, this.tmpColorOffsetBlend);
            iArr[0] = getBarColorBase(this.desc.valueIndexLastToConnect, this.descs.length, this.tmpColorOffsetBlend);
            iArr[2] = iArr[0];
            iArr[3] = iArr[1];
            this.lx1 = this.lastDesc.render1X;
            this.ly1 = this.lastDesc.render1Y;
            this.lx3 = this.lastDesc.render2X;
            this.ly3 = this.lastDesc.render2Y;
            this.x1 = this.desc.render1X;
            this.y1 = this.desc.render1Y;
            this.x3 = this.desc.render2X;
            float f = this.desc.render2Y;
            this.y3 = f;
            fArr[0] = this.lx1;
            fArr[1] = this.ly1;
            fArr[2] = this.x1;
            fArr[3] = this.y1;
            fArr[4] = this.lx3;
            fArr[5] = this.ly3;
            fArr[6] = this.x3;
            fArr[7] = f;
            if (this.lastDesc.valueIndexLastToConnect >= 0) {
                DrawBatchDesc drawBatchDesc2 = this.descs[this.lastDesc.valueIndexLastToConnect];
                this.lastLastDesc = drawBatchDesc2;
                float f2 = drawBatchDesc2.render1X;
                float f3 = this.lastLastDesc.render1Y;
                float f4 = this.lastLastDesc.render2X;
                float f5 = this.lastLastDesc.render2Y;
                if (this.desc.valueIndexNextToConnect >= 0) {
                    DrawBatchDesc drawBatchDesc3 = this.descs[this.desc.valueIndexNextToConnect];
                    this.nextDesc = drawBatchDesc3;
                    float f6 = drawBatchDesc3.render1X;
                    float f7 = this.nextDesc.render1Y;
                    float f8 = this.nextDesc.render2X;
                    float f9 = this.nextDesc.render2Y;
                    float f10 = this.ly1;
                    float f11 = -(f10 - f3);
                    float f12 = this.lx1;
                    float f13 = f12 - f2;
                    float f14 = this.y1;
                    float f15 = -(f14 - f10);
                    float f16 = this.x1;
                    float f17 = f16 - f12;
                    float f18 = -(f7 - f14);
                    float f19 = f6 - f16;
                    float f20 = this.ly3;
                    float f21 = -(f20 - f5);
                    float f22 = this.lx3;
                    float f23 = f22 - f4;
                    float f24 = this.y3;
                    float f25 = -(f24 - f20);
                    float f26 = this.x3;
                    makeEdgeCorners(f11, f13, f15, f17, f18, f19, f21, f23, f25, f26 - f22, -(f9 - f24), f8 - f26, fArr2);
                } else {
                    float f27 = this.ly1;
                    float f28 = this.lx1;
                    float f29 = f28 - f2;
                    float f30 = this.y1;
                    float f31 = this.x1;
                    float f32 = this.ly3;
                    float f33 = -(f32 - f5);
                    float f34 = this.lx3;
                    float f35 = f34 - f4;
                    float f36 = this.y3;
                    float f37 = this.x3;
                    makeEdgeCorners(-(f27 - f3), f29, -(f30 - f27), f31 - f28, -(f30 - f27), f31 - f28, f33, f35, -(f36 - f32), f37 - f34, -(f36 - f32), f37 - f34, fArr2);
                }
            } else if (this.desc.valueIndexNextToConnect >= 0) {
                DrawBatchDesc drawBatchDesc4 = this.descs[this.desc.valueIndexNextToConnect];
                this.nextDesc = drawBatchDesc4;
                float f38 = drawBatchDesc4.render1X;
                float f39 = this.nextDesc.render1Y;
                float f40 = this.nextDesc.render2X;
                float f41 = this.nextDesc.render2Y;
                float f42 = this.y1;
                float f43 = this.ly1;
                float f44 = this.x1;
                float f45 = this.lx1;
                float f46 = f44 - f45;
                float f47 = -(f39 - f42);
                float f48 = f38 - f44;
                float f49 = this.y3;
                float f50 = this.ly3;
                float f51 = this.x3;
                float f52 = this.lx3;
                makeEdgeCorners(-(f42 - f43), f44 - f45, -(f42 - f43), f46, f47, f48, -(f49 - f50), f51 - f52, -(f49 - f50), f51 - f52, -(f41 - f42), f40 - f51, fArr2);
            } else {
                float f53 = this.y1;
                float f54 = this.ly1;
                float f55 = this.x1;
                float f56 = this.lx1;
                float f57 = this.y3;
                float f58 = this.ly3;
                float f59 = this.x3;
                float f60 = this.lx3;
                makeEdgeCorners(-(f53 - f54), f55 - f56, -(f53 - f54), f55 - f56, -(f53 - f54), f55 - f56, -(f57 - f58), f59 - f60, -(f57 - f58), f59 - f60, -(f57 - f58), f59 - f60, fArr2);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onApplyCustomization(Element.CustomizationData customizationData) {
        super.onApplyCustomization(customizationData);
        this.mirror = customizationData.getPropertyBool("mirror", false);
        this.flipEveryOther = customizationData.getPropertyBool("flipEveryOther", false);
    }

    @Override
    public void onReadCustomization(Element.CustomizationData outCustomizationData) {
        super.onReadCustomization(outCustomizationData);
        outCustomizationData.putPropertyBool("mirror", this.mirror, "b");
        outCustomizationData.putPropertyBool("flipEveryOther", this.flipEveryOther, "b");
    }

    void makeEdgeCorners(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float[] fArr) {
        this.normal0tmp.x = -(f4 - f2);
        this.normal0tmp.y = f3 - f;
        this.normal0tmp.normalizeSafe();
        this.normal1tmp.x = -(f8 - f4);
        this.normal1tmp.y = f7 - f3;
        this.normal1tmp.normalizeSafe();
        this.normal2tmp.x = -(f6 - f8);
        this.normal2tmp.y = f5 - f7;
        this.normal2tmp.normalizeSafe();
        this.normal3tmp.x = -(f2 - f6);
        this.normal3tmp.y = f - f5;
        this.normal3tmp.normalizeSafe();
        this.c0.x = this.normal0tmp.x + this.normal3tmp.x;
        this.c0.y = this.normal0tmp.y + this.normal3tmp.y;
        this.c0.multiplyByValueDividedByDotCapped(this.normal0tmp, 1.5f);
        this.c1.x = this.normal0tmp.x + this.normal1tmp.x;
        this.c1.y = this.normal0tmp.y + this.normal1tmp.y;
        this.c1.multiplyByValueDividedByDotCapped(this.normal1tmp, 1.5f);
        this.c2.x = this.normal1tmp.x + this.normal2tmp.x;
        this.c2.y = this.normal1tmp.y + this.normal2tmp.y;
        this.c2.multiplyByValueDividedByDotCapped(this.normal2tmp, 1.5f);
        this.c3.x = this.normal2tmp.x + this.normal3tmp.x;
        this.c3.y = this.normal2tmp.y + this.normal3tmp.y;
        this.c3.multiplyByValueDividedByDotCapped(this.normal3tmp, 1.5f);
        fArr[0] = this.c0.x;
        fArr[1] = this.c0.y;
        fArr[2] = this.c1.x;
        fArr[3] = this.c1.y;
        fArr[4] = this.c2.x;
        fArr[5] = this.c2.y;
        fArr[6] = this.c3.x;
        fArr[7] = this.c3.y;
    }

    void makeEdgeCorners(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, float f11, float f12, float[] fArr) {
        double dSqrt = Math.sqrt((f * f) + (f2 * f2));
        float f13 = (float) (f / dSqrt);
        float f14 = (float) (f2 / dSqrt);
        double dSqrt2 = Math.sqrt((f3 * f3) + (f4 * f4));
        float f15 = (float) (f3 / dSqrt2);
        float f16 = (float) (f4 / dSqrt2);
        double dSqrt3 = Math.sqrt((f5 * f5) + (f6 * f6));
        float f17 = (float) (f5 / dSqrt3);
        float f18 = (float) (f6 / dSqrt3);
        double dSqrt4 = Math.sqrt((f7 * f7) + (f8 * f8));
        float f19 = (float) (f7 / dSqrt4);
        float f20 = (float) (f8 / dSqrt4);
        double dSqrt5 = Math.sqrt((f9 * f9) + (f10 * f10));
        float f21 = (float) (f9 / dSqrt5);
        float f22 = (float) (f10 / dSqrt5);
        double dSqrt6 = Math.sqrt((f11 * f11) + (f12 * f12));
        this.c0.x = f13 + f15;
        this.c0.y = f14 + f16;
        this.c0.multiplyByValueDividedByDotCapped(f15, f16, 1.5f);
        this.c1.x = f17 + f15;
        this.c1.y = f18 + f16;
        this.c1.multiplyByValueDividedByDotCapped(f15, f16, 1.5f);
        this.c2.x = ((float) (f11 / dSqrt6)) + f21;
        this.c2.y = ((float) (f12 / dSqrt6)) + f22;
        this.c2.multiplyByValueDividedByDotCapped(f21, f22, 1.5f);
        this.c3.x = f19 + f21;
        this.c3.y = f20 + f22;
        this.c3.multiplyByValueDividedByDotCapped(f21, f22, 1.5f);
        fArr[0] = this.c0.x;
        fArr[1] = this.c0.y;
        fArr[2] = this.c1.x;
        fArr[3] = this.c1.y;
        fArr[4] = -this.c2.x;
        fArr[5] = -this.c2.y;
        fArr[6] = -this.c3.x;
        fArr[7] = -this.c3.y;
    }
}
