

package com.aylis.comp.visual.core.Elements;

import android.graphics.PointF;
import com.aylis.Common.tlog;
import com.aylis.comp.visual.core.Graphic.RenderState;
import com.aylis.comp.visual.core.Elements.bars.AudioBars.ISegmentDataProvider;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class Meter {

    private RenderState renderState;
    private float frameDataRmsValue;
    private List<WeakReference<ISegmentDataProvider>> audioDataProviderWeakList = new ArrayList<>();

    public Meter(RenderState renderState) {
        this.renderState = renderState;
    }

    float getCenterAligmentX() {
        return 0.0f;
    }

    float getCenterAligmentY() {
        return 0.0f;
    }

    public float measureScreenSpaceX(float val, boolean uniform) {
        if (uniform && renderState.getScreenHeight() < renderState.getScreenWidth())
            return (val * renderState.getScreenHeight()) + getCenterAligmentX();

        return (int) (val * renderState.getScreenWidth()) + getCenterAligmentX();
    }

    public float measureScreenSpaceY(float val, boolean uniform) {
        if (uniform && renderState.getScreenWidth() < renderState.getScreenHeight())
            return (val * renderState.getScreenWidth()) + getCenterAligmentY();

        return (int) (val * renderState.getScreenHeight()) + getCenterAligmentY();
    }

    public float measureScreenSpaceAnchorX(int i) {
        float f;
        int screenWidth;
        if (i == 1) {
            return this.renderState.getScreenWidth() * 0.0f;
        }
        if (i == 2) {
            f = 0.5f;
            screenWidth = this.renderState.getScreenWidth();
        } else {
            if (i != 3) {
                return 0.0f;
            }
            f = 1.0f;
            screenWidth = this.renderState.getScreenWidth();
        }
        return screenWidth * f;
    }

    public float measureScreenSpaceAnchorY(int i) {
        float f;
        int screenHeight;
        if (i == 1) {
            return this.renderState.getScreenHeight() * 0.0f;
        }
        if (i == 2) {
            f = 0.5f;
            screenHeight = this.renderState.getScreenHeight();
        } else {
            if (i != 3) {
                return 0.0f;
            }
            f = 1.0f;
            screenHeight = this.renderState.getScreenHeight();
        }
        return screenHeight * f;
    }

    public float measureLocalSpaceX(float val, boolean uniform, float localW, float localH) {
        if (uniform && localH < localW)
            return (val * localH);

        return (int) (val * localW);
    }

    public float measureLocalSpaceY(float val, boolean uniform, float localW, float localH) {
        if (uniform && localW < localH)
            return (val * localW);

        return (int) (val * localH);
    }

    public float measureScreenScaleX(float val, boolean uniform) {
        if (uniform && renderState.getScreenHeight() < renderState.getScreenWidth())
            return (val * renderState.getScreenHeight());

        return (int) (val * renderState.getScreenWidth());
    }

    public float measureScreenScaleY(float val, boolean uniform) {
        if (uniform && renderState.getScreenWidth() < renderState.getScreenHeight())
            return (val * renderState.getScreenWidth());

        return (int) (val * renderState.getScreenHeight());
    }

    public float measureScaleX(float val, boolean uniform) {
        if (uniform && renderState.getScreenHeight() < renderState.getScreenWidth())
            return val * (renderState.getScreenHeight() / renderState.getScreenWidth());

        return val;
    }

    public float measureScaleY(float val, boolean uniform) {
        if (uniform && renderState.getScreenWidth() < renderState.getScreenHeight())
            return val * (renderState.getScreenWidth() / renderState.getScreenHeight());

        return val;
    }

    public float measureScaleZ(float val, boolean uniform) {
        if (uniform && renderState.getScreenWidth() < renderState.getScreenHeight())
            return val * (renderState.getScreenWidth() / renderState.getScreenHeight());

        return val;
    }

    public String measureText(final String val) {
        String result = renderState.res.visualizationData.onRequestsMeasureText(val);

        if (result == null) {
            tlog.w("result null, " + val);
            return val;
        }

        return result;
    }

    public float getFrameDataRmsValue() {
        ISegmentDataProvider provider = getAudioDataProvider();
        if (provider != null) {
            return provider.getRms();
        }
        return frameDataRmsValue;
    }

    public PointF measureVec2f(final String val) {
        if (val == null)
            return new PointF(0.0f, 0.0f);

        PointF result = renderState.res.visualizationData.onRequestMeasureVec2f(val, null, null, getFrameDataRmsValue());

        if (result == null) {
            tlog.w("result null, " + val);
            return new PointF(0.0f, 0.0f);
        }

        return result;
    }

    private PointF tempDefaultPt = new PointF(1.0f, 1.0f);

    public com.aylis.Common.Vec2f measureVec2f(final String val, com.aylis.Common.Vec2f argVec, com.aylis.Common.Vec2f lastMeasured) {
        if (val == null || val.isEmpty() || "Nothing".equals(val))
            return new com.aylis.Common.Vec2f(0.0f, 0.0f);

        if (argVec != null) {
            tempDefaultPt.set(argVec.x, argVec.y);
        } else {
            tempDefaultPt.set(1.0f, 1.0f);
        }
        
        PointF lastMeasuredPt = new PointF(0.0f, 0.0f);
        if (lastMeasured != null) {
            lastMeasuredPt.set(lastMeasured.x, lastMeasured.y);
        }

        PointF result = renderState.res.visualizationData.onRequestMeasureVec2f(val, tempDefaultPt, lastMeasuredPt, getFrameDataRmsValue());

        if (result == null) {
            return new com.aylis.Common.Vec2f(0.0f, 0.0f);
        }

        return new com.aylis.Common.Vec2f(result.x, result.y);
    }

    public void setFrameDataRmsValue(float rmsValue) {
        this.frameDataRmsValue = (this.frameDataRmsValue * 0.5f) + (rmsValue * 0.5f);
    }

    public void onFrameStart() {
        this.audioDataProviderWeakList.clear();
    }

    public void addAudioDataProvider(int index, ISegmentDataProvider audioDataProvider) {
        while (this.audioDataProviderWeakList.size() <= index) {
            this.audioDataProviderWeakList.add(new WeakReference<>(null));
        }
        this.audioDataProviderWeakList.set(index, new WeakReference<>(audioDataProvider));
    }

    public void addAudioDataProvider(ISegmentDataProvider audioDataProvider) {
        this.audioDataProviderWeakList.add(new WeakReference<>(audioDataProvider));
    }

    public void setAudioDataProvider(ISegmentDataProvider audioDataProvider) {
        this.audioDataProviderWeakList.clear();
        this.audioDataProviderWeakList.add(new WeakReference<>(audioDataProvider));
    }

    public ISegmentDataProvider getAudioDataProvider(int index) {
        if (index < 0 || index >= this.audioDataProviderWeakList.size()) {
            return null;
        }
        return this.audioDataProviderWeakList.get(index).get();
    }

    public ISegmentDataProvider getAudioDataProvider()
    {
        return getAudioDataProvider(0);
    }
}

