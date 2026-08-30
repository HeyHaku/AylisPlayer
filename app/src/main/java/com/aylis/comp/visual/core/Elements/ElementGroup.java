

package com.aylis.comp.visual.core.Elements;

import com.aylis.Common.tlog;
import com.aylis.comp.visual.core.Graphic.RenderState;
import junit.framework.Assert;
import java.util.ArrayList;
import java.util.List;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.FrameBuffer;

public abstract class ElementGroup extends Element {

    private List<Element> childList = new ArrayList<>();

    public List<Element> getChildList() {
        return childList;
    }

    public void addChild(Element child, int location) {
        Assert.assertEquals(null, child.parent);
        childList.add(location, child);
        child.parent = this;
    }

    public void addChildAtBeginning(Element child) {
        Assert.assertEquals(null, child.parent);
        childList.add(0, child);
        child.parent = this;
    }

    public void addChildAtEnd(Element child) {
        Assert.assertEquals(null, child.parent);
        childList.add(child);
        child.parent = this;
    }

    public void removeChild(Element child) {
        Assert.assertEquals(this, child.parent);

        childList.remove(child);
        child.parent = null;
    }

    @Override
    protected void markNeedReCreateGLResources() {
        super.markNeedReCreateGLResources();
        for (Element e : childList) {
            e.markNeedReCreateGLResources();
        }
    }

    @Override
    public void reCreateGLResources(RenderState renderData) {
        super.reCreateGLResources(renderData);
        for (Element e : childList) {
            e.reCreateGLResources(renderData);
        }
    }

    @Override
    protected void onCreateGLResources(RenderState renderData) {
        for (Element e : childList) {
            e.reCreateGLResources(renderData);
        }
    }

    @Override
    public void onEarlyUpdate(RenderState renderData, FrameBuffer resultFB) {
        if (!isVisible()) return;
        super.onEarlyUpdate(renderData, resultFB);
        for (Element e : childList) {
            if (e.isVisible()) {
                e.onEarlyUpdate(renderData, resultFB);
            }
        }
    }

    protected void renderChilds(RenderState renderData, FrameBuffer resultFB) {
        for (Element e : childList) {
            if (e.isVisible()) {
                e.onRender(renderData, resultFB);
            }
        }
    }

    @Override
    public boolean getCustomization(Element.CustomizationList customization, int customizationIndex) {
        super.getCustomization(customization, 0);

        if (customization == null)
            return false;

        for (Element e : childList) {
            if (!e.getCustomization(customization, 0))
                return false;
        }

        return true;
    }

    @Override
    public boolean setCustomization(Element.CustomizationList customization, Integer[] dataCounter) {
        return setCustomization(customization, dataCounter, -1);
    }

    @Override
    public boolean setCustomization(Element.CustomizationList customization, Integer[] dataCounter, int selectedIndex) {
        if (customization == null)
            return false;

        if (!super.setCustomization(customization, dataCounter, selectedIndex))
            return false;

        for (Element e : childList) {
            if (!e.setCustomization(customization, dataCounter, selectedIndex))
                return false;
        }

        return true;
    }

    public boolean setCustomization(Element.CustomizationList customization) {
        if (customization == null)
            return false;

        Integer[] dataCounter = new Integer[1];
        dataCounter[0] = 0;
        boolean result = setCustomization(customization, dataCounter);

        if (result) {
            if (customization.dataCount() != (int)dataCounter[0])
                tlog.w("elements changed");
        } else {
            tlog.w("setCustomization failed");
        }

        return result;
    }

    public boolean setCustomization(Element.CustomizationList customization, int selectedIndex) {
        if (customization == null)
            return false;

        Integer[] dataCounter = new Integer[1];
        dataCounter[0] = 0;
        boolean result = setCustomization(customization, dataCounter, selectedIndex);

        if (result) {
            if (customization.dataCount() != (int)dataCounter[0])
                tlog.w("elements changed");
        } else {
            tlog.w("setCustomization failed");
        }

        return result;
    }

    @Override
    public void drawHighlightRecursive(RenderState renderData) {
        super.drawHighlightRecursive(renderData);
        for (Element e : getChildList()) {
            e.drawHighlightRecursive(renderData);
        }
    }

}

