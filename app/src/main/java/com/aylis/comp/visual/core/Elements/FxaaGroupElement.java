

package com.aylis.comp.visual.core.Elements;

import android.opengl.GLES20;
import com.aylis.Common.Vec2i;
import com.aylis.Common.tlog;
import com.aylis.comp.visual.core.Graphic.RenderState;
import com.aylis.comp.visual.core.Graphic.VFrameBuffer;
import com.aylis.comp.visual.core.Graphic.VTexture;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.FrameBuffer;

public class FxaaGroupElement extends ElementGroup {

    private VFrameBuffer blurTargetContent;

    @Override
    public void onApplyCustomization(CustomizationData customizationData) {
        super.onApplyCustomization(customizationData);
    }

    @Override
    public void onReadCustomization(CustomizationData outCustomizationData) {
        super.onReadCustomization(outCustomizationData);
        outCustomizationData.setCustomizationName("Anti-aliasing (FXAA)");
    }

    @Override
    protected void onCreateGLResources(RenderState renderData) {
        try {
            Vec2i frameBufferSize = renderData.getSafeRenderBufferSizeTextureDim();
            blurTargetContent = VFrameBuffer.createSafe(frameBufferSize.x, frameBufferSize.y, VTexture.LINEAR, VTexture.DEFAULT_WRAP, false);
            if(blurTargetContent != null) blurTargetContent = blurTargetContent.checkIfValid();
        } catch (Exception e) {
            tlog.w(e.getMessage());
        }

        super.onCreateGLResources(renderData);
    }

    @Override
    public void onRender(RenderState renderData, FrameBuffer resultFB) {
        if (blurTargetContent == null) {

            super.onRender(renderData, resultFB);
            renderChilds(renderData, resultFB);
            return;
        }

        this.onRenderCheckResources(renderData);

        renderData.bindFrameBuffer(blurTargetContent);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        setupFrameBuffer();

        renderChilds(renderData, blurTargetContent);

        super.onRender(renderData, resultFB);

        renderData.bindShader(renderData.res.getFxaaShader());
        renderData.res.getFxaaShader().setUniformf("resolutionW", blurTargetContent.getTexture().getWidth());
        renderData.res.getFxaaShader().setUniformf("resolutionH", blurTargetContent.getTexture().getHeight());
        blurTargetContent.getTexture().bind();
        renderData.res.getFullQuad().drawShader(renderData.res.getFxaaShader(), "Position");
    }

    private void setupFrameBuffer() {
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
    }

}

