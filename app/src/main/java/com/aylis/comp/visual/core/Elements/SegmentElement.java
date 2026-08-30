package com.aylis.comp.visual.core.Elements;

import android.graphics.PointF;
import android.graphics.RectF;
import com.aylis.comp.visual.core.Elements.bars.AudioBars.ISegmentDataProvider;
import com.aylis.comp.visual.core.Elements.bars.BarsShapes.ISegmentPath;
import com.aylis.comp.visual.core.Elements.bars.Render.ISegmentRenderer;
import com.aylis.comp.visual.core.Elements.bars.BarsShapes.SegmentPathCircle;
import com.aylis.comp.visual.core.Elements.Segment.SegmentPathFactory;
import com.aylis.comp.visual.core.Elements.bars.Render.SegmentRendererBar;
import com.aylis.comp.visual.core.Elements.bars.Render.SegmentRendererFactory;
import com.aylis.comp.visual.core.Graphic.RenderState;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.FrameBuffer;
import java.util.ArrayList;
import java.util.List;

public class SegmentElement extends Element {

    public static final String typeName = "Bars";
    private ISegmentRenderer segmentRenderer = new SegmentRendererBar();
    private ISegmentRenderer segmentRenderer2 = null;
    private ISegmentPath segmentPath = new SegmentPathCircle();
    private int color1 = 0xffffffff;
    private float barHeightScale = 3.0f;
    private float minBarHeightScale = 0.009f;
    private float maxBarHeightScale = 1.0f;
    private boolean flipInput = false;
    private int softnessRadius = 2;
    private int reactionDelay = 0;
    private int reactionAccumulatedDelay = 0;
    private int audioProviderIndex = 2; // Default to Provider 2 (Cava)

    private boolean mirrorX = false;

    private ISegmentRenderer.DrawBatchDesc[] segmentDrawDescTmp = new ISegmentRenderer.DrawBatchDesc[0];

    public void setSegmentRenderer(ISegmentRenderer segmentRenderer) {
        this.segmentRenderer = segmentRenderer;
    }

    public void setSegmentRenderer2(ISegmentRenderer segmentRenderer) {
        segmentRenderer2 = segmentRenderer;
    }

    public void setSegmentPath(ISegmentPath segmentPath) {
        this.segmentPath = segmentPath;
    }

    public void setColor(int colorARGB) {
        color1 = colorARGB;
    }

    public void setBarHeightScale(float barHeightScale) {
        this.barHeightScale = barHeightScale;
    }

    public void setMinBarHeightScale(float barHeight) {
        minBarHeightScale = barHeight;
    }

    public void setMaxBarHeightScale(float barHeight) {
        maxBarHeightScale = barHeight;
    }

    public void setSoftness(int softness) {
        this.softnessRadius = softness;
    }

    public void setReactionDelay(int delay) {
        this.reactionDelay = delay;
    }

    public void setReactionAccumulatedDelay(int delay) {
        this.reactionAccumulatedDelay = delay;
    }

    public void setAudioProviderIndex(int index) {
        this.audioProviderIndex = index;
    }

    public void setMirrorX(boolean mirror) {
        this.mirrorX = mirror;
    }

    @Override
    public void onApplyCustomization(CustomizationData customizationData) {
        super.onApplyCustomization(customizationData);
        setColor(customizationData.getPropertyInt("color", color1));
        setBarHeightScale(customizationData.getPropertyFloat("heightScale", barHeightScale));
        setMinBarHeightScale(customizationData.getPropertyFloat("minHeightScale", minBarHeightScale));
        setMaxBarHeightScale(customizationData.getPropertyFloat("maxHeightScale", maxBarHeightScale));
        flipInput = customizationData.getPropertyBool("flipInput", flipInput);
        setSoftness(customizationData.getPropertyInt("softness", softnessRadius));
        setReactionDelay(customizationData.getPropertyInt("reactionDelay", reactionDelay));
        setReactionAccumulatedDelay(customizationData.getPropertyInt("reactionAccumulatedDelay", reactionAccumulatedDelay));
        
        String providerStr = customizationData.getPropertyString("audioProviderIndex", String.valueOf(audioProviderIndex));
        try {
            if (providerStr.contains("#")) {
                int coreIdx = Integer.parseInt(providerStr.split("#")[1].replace("]", "").trim());
                setAudioProviderIndex(coreIdx + 1);
            } else {
                int idx = Integer.parseInt(providerStr.split(" ")[0]);
                setAudioProviderIndex(idx);
            }
        } catch (Exception e) {}

        setMirrorX(customizationData.getPropertyBool("mirrorX", mirrorX));

        {
            CustomizationData segmentRendererCutom1 = customizationData.getChild("ShapePath");
            segmentPath = SegmentPathFactory.create(segmentRendererCutom1.getChildTypeValue(), segmentPath);
            if (segmentPath != null)
                segmentPath.onApplyCustomization(segmentRendererCutom1);
        }

        CustomizationData segmentRendererCutom1 = customizationData.getChild("Segment1");
        segmentRenderer = SegmentRendererFactory.create(segmentRendererCutom1.getChildTypeValue(), segmentRenderer);
        if (segmentRenderer != null) segmentRenderer.onApplyCustomization(segmentRendererCutom1);

        CustomizationData segmentRendererCutom2 = customizationData.getChild("Segment2");
        segmentRenderer2 = SegmentRendererFactory.create(segmentRendererCutom2.getChildTypeValue(), segmentRenderer2);
        if (segmentRenderer2 != null) segmentRenderer2.onApplyCustomization(segmentRendererCutom2);
    }

    private int countAudioProviders() {
        if (parent == null) return 0;
        ElementGroup root = parent;
        while (root.parent != null) {
            root = root.parent;
        }
        List<AudioDataProviderElement> list = new ArrayList<>();
        findProviders(root, list);
        return list.size();
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
        super.onReadCustomization(outCustomizationData);
        outCustomizationData.setCustomizationName("Bars");
        outCustomizationData.putPropertyInt("color", color1, "crgba", "1_bars");
        outCustomizationData.putPropertyFloat("heightScale", barHeightScale, "f -300.0 300.0", "1_bars");
        outCustomizationData.putPropertyFloat("minHeightScale", minBarHeightScale, "f -0.05 0.05", "1_bars");
        outCustomizationData.putPropertyFloat("maxHeightScale", maxBarHeightScale, "f 0.1 1.0", "1_bars");
        outCustomizationData.putPropertyBool("flipInput", flipInput, "1_bars");

        outCustomizationData.putPropertyBool("mirrorX", mirrorX, "1_bars");

        outCustomizationData.putPropertyInt("softness", softnessRadius, "i 0 20", "2_Behavior");
        outCustomizationData.putPropertyInt("reactionDelay", reactionDelay, "i 0 9", "2_Behavior");
        outCustomizationData.putPropertyInt("reactionAccumulatedDelay", reactionAccumulatedDelay, "i 0 9", "2_Behavior");
        
        int coreCount = countAudioProviders();
        String providerOptions = "sel";
        if (coreCount == 0) {
            providerOptions += "|No AudioBarsCore found";
        } else {
            for (int i = 1; i <= coreCount; i++) {
                providerOptions += "|AudioBarsCore #" + i;
            }
        }
        
        String currProvider;
        if (coreCount == 0) {
            currProvider = "No AudioBarsCore found";
        } else {
            currProvider = "AudioBarsCore #" + (audioProviderIndex - 1);
        }
        outCustomizationData.putPropertyString("audioProviderIndex", currProvider, providerOptions, "2_Behavior", "Audio Provider");

        {
            CustomizationData segmentRendererCutom1 = outCustomizationData.putChild("ShapePath", SegmentPathFactory.getTypeName(segmentPath), SegmentPathFactory.typeNames, "1_bars");
            if (segmentPath != null) segmentPath.onReadCustomization(segmentRendererCutom1);
        }

        CustomizationData segmentRendererCutom1 = outCustomizationData.putChild("Segment1", SegmentRendererFactory.getTypeName(segmentRenderer), SegmentRendererFactory.typeNames, "1_bars");
        if (segmentRenderer != null) segmentRenderer.onReadCustomization(segmentRendererCutom1);
        CustomizationData segmentRendererCutom2 = outCustomizationData.putChild("Segment2", SegmentRendererFactory.getTypeName(segmentRenderer2), SegmentRendererFactory.typeNames, "1_bars");
        if (segmentRenderer2 != null) segmentRenderer2.onReadCustomization(segmentRendererCutom2);
    }

    @Override
    protected void onCreateGLResources(RenderState renderData) {
        super.onCreateGLResources(renderData);
    }

    @Override
    public void onRender(RenderState renderData, FrameBuffer resultFB) {
        super.onRender(renderData, resultFB);

        ISegmentDataProvider segmentDataProvider = renderData.res.meter.getAudioDataProvider(audioProviderIndex - 1);

        if (segmentDataProvider == null ||
                (segmentRenderer == null && segmentRenderer2 == null) ||
                segmentPath == null) return;

        RectF drawRect = measureDrawRect(renderData.res.meter);
        PointF drawScale = measureDrawScaleRect(renderData.res.meter);

        segmentDataProvider.process(renderData.res.visualizationData);
        segmentPath.process(renderData);

        com.aylis.Common.ISimpleListFloat barValsList = segmentDataProvider.createFrameValuesAccessorList(this.reactionDelay, this.reactionAccumulatedDelay, this.softnessRadius, null);
        int valuesCount = barValsList != null ? barValsList.size() : 0;
        if (valuesCount < 1) return;

        if (valuesCount != segmentDrawDescTmp.length) {
            segmentDrawDescTmp = new ISegmentRenderer.DrawBatchDesc[valuesCount];
            for (int i = 0; i < valuesCount; i++) {
                segmentDrawDescTmp[i] = new ISegmentRenderer.DrawBatchDesc();
                segmentDrawDescTmp[i].valueIndexNextToConnectTmp = -1;
            }
        }

        PointF pathPointOut = new PointF();
        PointF pathPointVecOut = new PointF();

        float rotation = measureDrawRot(renderData.res.meter);
        boolean hasRotation = (rotation != 0.0f);
        float cos = 1.0f, sin = 0.0f;
        float cx = drawRect.centerX();
        float cy = drawRect.centerY();

        if (hasRotation) {
            float rad = rotation * 2.0f * (float) Math.PI;
            cos = (float) Math.cos(rad);
            sin = (float) Math.sin(rad);
        }

        float minBarHeightScaled = renderData.res.meter.measureScreenScaleX(minBarHeightScale, true);
        float pathLength = segmentPath.getPathLength(drawRect, valuesCount);
        float lastBarVal = valuesCount > 0 ? barValsList.get(valuesCount - 1) : 0.0f;
        PointF lastpathPointVecOut = new PointF();
        PointF lastdrawPoint = new PointF();
        segmentPath.getPointOnPath(valuesCount - 1, valuesCount, drawRect, lastdrawPoint, lastpathPointVecOut);

        if (hasRotation) {
            float lpx = lastdrawPoint.x - cx;
            float lpy = lastdrawPoint.y - cy;
            lastdrawPoint.x = cx + (lpx * cos - lpy * sin);
            lastdrawPoint.y = cy + (lpx * sin + lpy * cos);

            float lvx = lastpathPointVecOut.x;
            float lvy = lastpathPointVecOut.y;
            lastpathPointVecOut.x = lvx * cos - lvy * sin;
            lastpathPointVecOut.y = lvx * sin + lvy * cos;
        }

        ISegmentRenderer.DrawDesc drawDesc = new ISegmentRenderer.DrawDesc();
        drawDesc.blendMode = getBlendMode();
        drawDesc.color1 = color1;
        drawDesc.drawScale = drawScale.y;
        drawDesc.drawSegmentWidth = pathLength;
        drawDesc.valuesCount = valuesCount;

        for (int i = 0; i < valuesCount; i++) {
            segmentPath.getPointOnPath(i, valuesCount, drawRect, pathPointOut, pathPointVecOut);

            if (hasRotation) {
                float px = pathPointOut.x - cx;
                float py = pathPointOut.y - cy;
                pathPointOut.x = cx + (px * cos - py * sin);
                pathPointOut.y = cy + (px * sin + py * cos);

                float vx = pathPointVecOut.x;
                float vy = pathPointVecOut.y;
                pathPointVecOut.x = vx * cos - vy * sin;
                pathPointVecOut.y = vx * sin + vy * cos;
            }

            float currentVal = barValsList.get(i);
            drawDesc.valueIndex = i;
            drawDesc.lastSegmentHeightVal = (lastBarVal * barHeightScale) + minBarHeightScaled;
            drawDesc.segmentHeightVal = (currentVal * barHeightScale) + minBarHeightScaled;
            drawDesc.lastDrawPointX = lastdrawPoint.x;
            drawDesc.lastDrawPointY = lastdrawPoint.y;
            drawDesc.lastDrawVecX = lastpathPointVecOut.x;
            drawDesc.lastDrawVecY = lastpathPointVecOut.y;
            drawDesc.drawPointX = pathPointOut.x;
            drawDesc.drawPointY = pathPointOut.y;
            drawDesc.drawVecX = pathPointVecOut.x;
            drawDesc.drawVecY = pathPointVecOut.y;

            if (segmentRenderer != null)
                segmentRenderer.drawSegment(renderData, drawDesc, 0.0f);

            if (segmentRenderer2 != null)
                segmentRenderer2.drawSegment(renderData, drawDesc, 0.0f);

            if (mirrorX) {
                ISegmentRenderer.DrawDesc mirrorDesc = new ISegmentRenderer.DrawDesc();
                mirrorDesc.blendMode = drawDesc.blendMode;
                mirrorDesc.color1 = drawDesc.color1;
                mirrorDesc.drawScale = drawDesc.drawScale;
                mirrorDesc.drawSegmentWidth = drawDesc.drawSegmentWidth;
                mirrorDesc.valuesCount = drawDesc.valuesCount;
                mirrorDesc.valueIndex = drawDesc.valueIndex;
                mirrorDesc.lastSegmentHeightVal = drawDesc.lastSegmentHeightVal;
                mirrorDesc.segmentHeightVal = drawDesc.segmentHeightVal;

                mirrorDesc.drawPointX = (cx * 2.0f) - drawDesc.drawPointX;
                mirrorDesc.drawPointY = drawDesc.drawPointY;
                mirrorDesc.lastDrawPointX = (cx * 2.0f) - drawDesc.lastDrawPointX;
                mirrorDesc.lastDrawPointY = drawDesc.lastDrawPointY;

                mirrorDesc.drawVecX = -drawDesc.drawVecX;
                mirrorDesc.drawVecY = drawDesc.drawVecY;
                mirrorDesc.lastDrawVecX = -drawDesc.lastDrawVecX;
                mirrorDesc.lastDrawVecY = drawDesc.lastDrawVecY;

                if (segmentRenderer != null)
                    segmentRenderer.drawSegment(renderData, mirrorDesc, 0.0f);

                if (segmentRenderer2 != null)
                    segmentRenderer2.drawSegment(renderData, mirrorDesc, 0.0f);
            }

            lastBarVal = currentVal;
            lastdrawPoint.x = pathPointOut.x;
            lastdrawPoint.y = pathPointOut.y;
            lastpathPointVecOut.x = pathPointVecOut.x;
            lastpathPointVecOut.y = pathPointVecOut.y;
        }
    }
}