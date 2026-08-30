
package com.aylis.comp.visual.core.Elements;

import android.opengl.GLES20;
import com.aylis.comp.visual.core.Graphic.GraphicsUtils;
import com.aylis.comp.visual.core.Graphic.RenderState;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.FrameBuffer;
import com.aylis.comp.visual.core.Elements.bars.AudioBars.ISegmentDataProvider;
import com.aylis.comp.visual.core.audio.Providers.SegmentAudioSpectrumData2;

public class BackgroundElement extends ElementGroup {

    private float bgR, bgG, bgB, bgA = 0.0f;
    private ISegmentDataProvider segmentDataProvider = new SegmentAudioSpectrumData2();

    public BackgroundElement() {
    }

    public void setBackgroundColor(float red,
            float green,
            float blue,
            float alpha) {
        bgR = red;
        bgG = green;
        bgB = blue;
        bgA = alpha;
    }

    @Override
    public void onApplyCustomization(CustomizationData customizationData) {
        super.onApplyCustomization(customizationData);

        if (segmentDataProvider != null) {
            segmentDataProvider.onApplyCustomization(customizationData);
        }

        float[] rgbaF4Color = new float[4];
        GraphicsUtils.intColorToF4Color(rgbaF4Color, customizationData.getPropertyInt("color",
                GraphicsUtils.f4ColorToIntColor(new float[] { bgR, bgG, bgB, bgA })));

        bgR = rgbaF4Color[0];
        bgG = rgbaF4Color[1];
        bgB = rgbaF4Color[2];
        bgA = rgbaF4Color[3];
    }

    @Override
    public void onReadCustomization(CustomizationData outCustomizationData) {
        // We intentionally DO NOT call super.onReadCustomization(outCustomizationData) 
        // to hide the standard "General", "Color", and "Modifier" properties from the Master Scene.
        outCustomizationData.setCustomizationName("Master Scene");
                
        if (segmentDataProvider != null) {
            segmentDataProvider.onReadCustomization(outCustomizationData);
        }
    }

    @Override
    protected void onCreateGLResources(RenderState renderData) {
        super.onCreateGLResources(renderData);
    }
    
    @Override
    public void onEarlyUpdate(RenderState renderData, FrameBuffer resultFB) {
        boolean isPlaying = renderData.res.visualizationData != null;

        if (segmentDataProvider != null) {
            segmentDataProvider.process(renderData.res.visualizationData);
            // Master Scene (BackgroundElement) handles shakes, so we update GlobalAudioTrigger here
            com.aylis.comp.visual.core.audio.GlobalAudioTrigger.INSTANCE.update(segmentDataProvider, isPlaying);
            
            // Force Spectrum 2 to be Provider 1 (index 0)
            renderData.res.meter.addAudioDataProvider(0, segmentDataProvider);
        }

        super.onEarlyUpdate(renderData, resultFB);
    }

    @Override
    public void onRender(RenderState renderData, FrameBuffer resultFB) {
        super.onRender(renderData, resultFB);

        GLES20.glClearColor(bgR, bgG, bgB, bgA);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glViewport(0, 0, renderData.getScreenWidth(), renderData.getScreenHeight());

        renderChilds(renderData, resultFB);
    }
}
