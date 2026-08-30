package com.aylis.comp.visual.core.Elements;

import android.graphics.Color;
import com.aylis.Common.Vec2i;
import com.aylis.comp.visual.core.Graphic.RenderState;
import com.aylis.comp.visual.core.Graphic.VFrameBuffer;
import com.aylis.comp.visual.core.Graphic.VTexture;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.FrameBuffer;
import com.aylis.comp.visual.core.gl.mdesl.graphics.Texture;
import android.opengl.GLES20;

public class PreCompElement extends ElementGroup {
    private String preCompName = "Scene 1";
    private VFrameBuffer offscreenFB = null;
    private boolean renderingOnDemand = false;
    private boolean isMarkedForPreCompRendering = false;

    public PreCompElement() {
    }

    public String getPreCompName() {
        return preCompName;
    }

    public void setPreCompName(String name) {
        this.preCompName = name;
    }

    @Override
    public void onApplyCustomization(CustomizationData customizationData) {
        visible = customizationData.getPropertyBool("visible", visible);
        String oldName = preCompName;
        preCompName = customizationData.getPropertyString("preCompName", preCompName);
        if (oldName != null && !oldName.equals(preCompName)) {
            PreCompElement current = PreCompManager.get(oldName);
            if (current == this) {
                PreCompManager.unregister(oldName);
            }
        }
        // Only register immediately if this element is already active in the GL thread.
        // Otherwise, it will be registered later in onCreateGLResources.
        // This prevents dummy elements (created for UI previews) from corrupting the manager.
        if (offscreenFB != null) {
            PreCompManager.register(preCompName, this);
        }
    }

    @Override
    public void onReadCustomization(CustomizationData outCustomizationData) {
        outCustomizationData.setCustomizationName(preCompName);
        outCustomizationData.putPropertyString("preCompName", preCompName, "txt");
        outCustomizationData.putPropertyString("__type", "PreCompElement");
        outCustomizationData.putPropertyBool("visible", visible, "0_general");
    }

    @Override
    protected void onCreateGLResources(RenderState renderData) {
        super.onCreateGLResources(renderData);
        PreCompManager.register(preCompName, this);
        initFrameBuffer(renderData);
    }

    private void initFrameBuffer(RenderState renderData) {
        if (offscreenFB != null) {
            offscreenFB.dispose();
            offscreenFB = null;
        }
        try {
            Vec2i size = renderData.getSafeRenderBufferSizeTextureDim();
            offscreenFB = VFrameBuffer.createSafe(size.x, size.y, VTexture.LINEAR, VTexture.DEFAULT_WRAP, false);
            if (offscreenFB != null) {
                offscreenFB = (VFrameBuffer) offscreenFB.checkIfValid();
            }
        } catch (Exception e) {
            com.aylis.Common.tlog.w("Failed to create PreComp framebuffer: " + e.getMessage());
        }
    }

    public Texture getTexture() {
        return offscreenFB != null ? offscreenFB.getTexture() : null;
    }

    public void renderOnDemand(RenderState renderData, FrameBuffer previousFB) {
        if (!isVisible()) return;
        if (isMarkedForPreCompRendering) return;
        isMarkedForPreCompRendering = true;

        if (offscreenFB == null) {
            initFrameBuffer(renderData);
        } else {
            Vec2i currentSize = renderData.getSafeRenderBufferSizeTextureDim();
            if (offscreenFB.getWidth() != currentSize.x || offscreenFB.getHeight() != currentSize.y) {
                initFrameBuffer(renderData);
            }
        }

        if (offscreenFB != null) {
            renderingOnDemand = true;

            renderData.bindFrameBuffer(offscreenFB);

            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            super.onRender(renderData, offscreenFB);
            renderChilds(renderData, offscreenFB);

            renderData.bindFrameBuffer(previousFB);

            renderingOnDemand = false;
        }

        isMarkedForPreCompRendering = false;
    }

    @Override
    public void onRender(RenderState renderData, FrameBuffer resultFB) {
        onRenderCheckResources(renderData);
        if (!renderingOnDemand) {
            return;
        }
    }
}
