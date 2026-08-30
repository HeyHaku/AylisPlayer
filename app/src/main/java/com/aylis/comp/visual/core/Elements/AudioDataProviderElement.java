

package com.aylis.comp.visual.core.Elements;

import com.aylis.comp.visual.core.Elements.bars.AudioBars.ISegmentDataProvider;
import com.aylis.comp.visual.core.audio.Providers.CavaSpectrumProvider;
import com.aylis.comp.visual.core.Graphic.RenderState;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.FrameBuffer;
import java.util.ArrayList;
import java.util.List;

public class AudioDataProviderElement extends Element {

    private ISegmentDataProvider segmentDataProvider = new CavaSpectrumProvider();

    public void setSegmentDataProvider(ISegmentDataProvider segmentDataProvider) {
        this.segmentDataProvider = segmentDataProvider;
    }

    @Override
    public void onApplyCustomization(CustomizationData customizationData) {
        if (segmentDataProvider != null) {
            segmentDataProvider.onApplyCustomization(customizationData);
        }
    }
    
    private int getProviderIndex() {
        if (parent == null) return 2;
        ElementGroup root = parent;
        while (root.parent != null) {
            root = root.parent;
        }
        List<AudioDataProviderElement> list = new ArrayList<>();
        findProviders(root, list);
        int idx = list.indexOf(this);
        return idx >= 0 ? idx + 2 : 2;
    }

    private void findProviders(ElementGroup group, List<AudioDataProviderElement> list) {
        if (group.getChildList() == null) return;
        for (Element e : group.getChildList()) {
            if (e instanceof AudioDataProviderElement) {
                list.add((AudioDataProviderElement) e);
            } else if (e instanceof ElementGroup) {
                findProviders((ElementGroup) e, list);
            }
        }
    }

    @Override
    public void onReadCustomization(CustomizationData outCustomizationData) {
        int pIndex = getProviderIndex();
        int coreIndex = pIndex - 1;
        outCustomizationData.setCustomizationName("AudioBarsCore #" + coreIndex);

        if (segmentDataProvider != null) {
            segmentDataProvider.onReadCustomization(outCustomizationData);
        }
    }

    @Override
    public void onCreateGLResources(RenderState renderData) {
        super.onCreateGLResources(renderData);
    }

    @Override
    public void onRender(RenderState renderData, FrameBuffer resultFB) {
        super.onRender(renderData, resultFB);
    }

    @Override
    public void onEarlyUpdate(RenderState renderData, FrameBuffer resultFB) {
        super.onEarlyUpdate(renderData, resultFB);

        if(segmentDataProvider != null) {
            segmentDataProvider.process(renderData.res.visualizationData);
        }

        // AudioBarsCore (Cava) naturally gets Provider 2 (index 1) or higher
        // because BackgroundElement already pushed Provider 1 (index 0).
        renderData.res.meter.addAudioDataProvider(segmentDataProvider);
    }
}

