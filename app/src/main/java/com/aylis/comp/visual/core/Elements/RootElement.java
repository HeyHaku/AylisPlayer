

package com.aylis.comp.visual.core.Elements;

import com.aylis.comp.visual.core.Graphic.RenderState;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.FrameBuffer;

public class RootElement extends ElementGroup {

    private int compareIdentifier;
    private IFrameDataProvider frameDataProvider;

    public RootElement(int compareIdentifier) {
        this.compareIdentifier = compareIdentifier;
    }

    public RootElement(int compareIdentifier, Element childToAdd) {
        this.compareIdentifier = compareIdentifier;
        this.addChildAtEnd(childToAdd);
    }

    public int getIdentifier() {
        return compareIdentifier;
    }

    public IFrameDataProvider getFrameDataProvider() {
        return frameDataProvider;
    }

    public RootElement setFrameDataProvider(IFrameDataProvider frameDataProvider) {
        this.frameDataProvider = frameDataProvider;
        return this;
    }

    @Override
    public int hashCode() {
        return (compareIdentifier * 45) + 47;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RootElement)) return false;
        RootElement ob = (RootElement) o;
        return this.compareIdentifier == ob.compareIdentifier;
    }

    @Override
    protected void onCreateGLResources(RenderState renderData) {
        super.onCreateGLResources(renderData);
    }

    @Override
    public void onRender(RenderState renderData, FrameBuffer resultFB) {
        super.onRender(renderData, resultFB);
        renderChilds(renderData, resultFB);
    }

    @Override
    public void onApplyCustomization(CustomizationData customizationData) {
    }

    @Override
    public void onReadCustomization(CustomizationData outCustomizationData) {
        outCustomizationData.setCustomizationName("");
        outCustomizationData.putPropertyString("__type", "RootElement");
    }

    public int readThemeCustomizationData(Element.CustomizationList customization) {
        if (getCustomization(customization, 0))
            return getIdentifier();

        return -1;
    }
}

