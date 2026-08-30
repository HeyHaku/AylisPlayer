

package com.aylis.comp.visual.core.Elements.bars.Render;

import com.aylis.Common.Utils;
import com.aylis.comp.visual.core.Elements.Element;
import com.aylis.comp.visual.core.Graphic.GraphicsUtils;

public abstract class SegmentRendererBase implements ISegmentRenderer {
    float tmpNormalIndex;
    float[] barColorHsla = new float[4];
    private float[] hslaColorFrom = {1.0f, 1.0f, 1.0f, 1.0f};
    private float[] hslaColorMiddle = {1.0f, 1.0f, 1.0f, 1.0f};
    protected boolean useFixedLineHeight = false;
    protected float fixedLineHeight = 0.0f;
    protected float barHeightMultiplier = 1.0f;

    public SegmentRendererBase setColor(int i) {
        return setColor(i, i, i);
    }

    public SegmentRendererBase setColor(int i, int i2) {
        return setColor(i, i2, i2);
    }

    public SegmentRendererBase setColor(int i, int i2, int i3) {
        GraphicsUtils.intColorToHlsa360(this.hslaColorFrom, i);
        GraphicsUtils.intColorToHlsa360(this.hslaColorMiddle, i2);
        return this;
    }

    public SegmentRendererBase setFixedLineHeight(float f) {
        this.useFixedLineHeight = Math.abs(f) > 0.01f;
        this.fixedLineHeight = f;
        return this;
    }

    public SegmentRendererBase setFixedBarHeight(boolean use, float val) {
        this.useFixedLineHeight = use;
        this.fixedLineHeight = val;
        return this;
    }

    public SegmentRendererBase setBarHeightMultiplier(float f) {
        this.barHeightMultiplier = f;
        return this;
    }

    @Override
    public void onApplyCustomization(Element.CustomizationData customizationData) {
        setColor(customizationData.getPropertyInt("colorFrom", -1), customizationData.getPropertyInt("colorTo", -1), -1);
        setFixedLineHeight(customizationData.getPropertyFloat("fixedHeight", 0.0f));
        this.barHeightMultiplier = customizationData.getPropertyFloat("barHeightMultiplier", 1.0f);
    }

    @Override
    public void onReadCustomization(Element.CustomizationData outCustomizationData) {
        outCustomizationData.putPropertyInt("colorFrom", GraphicsUtils.hlsa360ColorToInt(this.hslaColorFrom), "crgba");
        outCustomizationData.putPropertyInt("colorTo", GraphicsUtils.hlsa360ColorToInt(this.hslaColorMiddle), "crgba");
        outCustomizationData.putPropertyFloat("fixedHeight", this.fixedLineHeight, "f -2.0 2.0");
        outCustomizationData.putPropertyFloat("barHeightMultiplier", this.barHeightMultiplier, "f -2.0 2.0");
    }

    protected int getBarColorBase(int i, int i2, float f) {
        float f2 = (float) i / (float) i2;
        this.tmpNormalIndex = f2;
        float f3 = (f2 + (10.0f - f)) % 1.0f;
        this.tmpNormalIndex = f3;
        if (f3 <= 0.5f) {
            Utils.lerpHsla(this.hslaColorFrom, this.hslaColorMiddle, this.barColorHsla, f3 * 2.0f);
        } else {
            Utils.lerpHsla(this.hslaColorMiddle, this.hslaColorFrom, this.barColorHsla, (f3 - 0.5f) * 2.0f);
        }
        return GraphicsUtils.hlsa360ColorToInt(this.barColorHsla);
    }

    protected void getBarColorBase(float[] fArr, int i, int i2) {
        float f = (float) i / (float) i2;
        this.tmpNormalIndex = f;
        if (f <= 0.5f) {
            Utils.lerpHsla(this.hslaColorFrom, this.hslaColorMiddle, this.barColorHsla, f * 2.0f);
        } else {
            Utils.lerpHsla(this.hslaColorMiddle, this.hslaColorFrom, this.barColorHsla, (f - 0.5f) * 2.0f);
        }
        GraphicsUtils.hlsa360ColorToF4Color(this.barColorHsla, fArr);
    }

    protected int getBarColorBase(float f) {
        if (f <= 0.5f) {
            Utils.lerpHsla(this.hslaColorFrom, this.hslaColorMiddle, this.barColorHsla, f * 2.0f);
        } else {
            Utils.lerpHsla(this.hslaColorMiddle, this.hslaColorFrom, this.barColorHsla, (f - 0.5f) * 2.0f);
        }
        return GraphicsUtils.hlsa360ColorToInt(this.barColorHsla);
    }
}
