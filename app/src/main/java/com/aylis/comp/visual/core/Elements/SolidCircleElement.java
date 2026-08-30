

package com.aylis.comp.visual.core.Elements;

import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import com.aylis.Common.Vec2f;
import com.aylis.comp.visual.core.Graphic.GraphicsUtils;
import com.aylis.comp.visual.core.Graphic.RenderState;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.FrameBuffer;

public class SolidCircleElement extends Element {

    private String blendMeasure = null;
    private float measureMul = 1.0f;
    private int side = 3;
    private int color1 = Color.argb(0xff, 64, 128, 255);

    public void setSideCount(int side) {
        this.side = side;
    }

    public void setColor(int colorARGB) {
        color1 = colorARGB;
    }

    @Override
    public void onApplyCustomization(CustomizationData customizationData) {
        super.onApplyCustomization(customizationData);
        setColor(customizationData.getPropertyInt("color", color1));
        setSideCount(customizationData.getPropertyInt("shapeSides", side));
        blendMeasure = customizationData.getPropertyString("blendMeasure", blendMeasure);
        measureMul = customizationData.getPropertyFloat("measureMul", measureMul);
    }

    @Override
    public void onReadCustomization(CustomizationData outCustomizationData) {
        super.onReadCustomization(outCustomizationData);
        outCustomizationData.setCustomizationName("Solid");
        outCustomizationData.putPropertyInt("color", color1, "crgba");
        outCustomizationData.putPropertyInt("shapeSides", side, "i 3 50");
        outCustomizationData.putPropertyString("blendMeasure", blendMeasure == null ? "" : blendMeasure, "txt");
        outCustomizationData.putPropertyFloat("measureMul", measureMul, "f -10.0 10.0");
        outCustomizationData.putPropertyString("testImage", "", "img");
    }

    @Override
    public void onRender(RenderState renderData, FrameBuffer resultFB) {
        super.onRender(renderData, resultFB);

        RectF drawRect = measureDrawRect(renderData.res.meter);

        PointF blendMeasured = renderData.res.meter.measureVec2f(blendMeasure);
        float blend = blendMeasured.x * measureMul;
        blend *= 2.0f;
        if (blend > 1.0f) blend = 1.0f;

        float blendSmooth = blend;

        int blendInt = (int) (blendSmooth * 255.0f * GraphicsUtils.getAlphaFloatFromIntColor(color1));
        blendInt = Math.min(blendInt, 255);

        int color = Color.argb(blendInt,
                Color.red(color1),
                Color.green(color1),
                Color.blue(color1));

        float x = drawRect.centerX() - drawRect.width() * 0.5f;
        float y = drawRect.centerY() - drawRect.height() * 0.5f;
        float w = drawRect.width();
        float h = drawRect.height();

        float rotation = measureDrawRot(renderData.res.meter);

        renderData.res.getBufferRenderer().drawCircle(
                renderData,
                x, y, 0.0f,
                w, h,
                color,
                new Vec2f(0.0f, 0.0f), new Vec2f(1.0f, 1.0f),
                renderData.res.getAtlasTexWhite(), side, rotation);
    }

    public void setColorBlendMeasure(String measure, float mul) {
        blendMeasure = measure;
        measureMul = mul;
    }
}

