

package com.aylis.comp.visual.core.Graphic;

import android.content.res.Resources;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import com.aylis.Common.tlog;
import com.aylis.PlayerCore;
import com.aylis.comp.visual.core.InternalVisualizationDataProvider;
import com.aylis.comp.visual.core.VisualizerViewCore;
import com.aylis.comp.visual.core.Elements.Element;
import com.aylis.comp.visual.core.Elements.IFrameDataProvider;
import com.aylis.comp.visual.core.Elements.RootElement;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.FrameBuffer;

public class RendererCore implements GLSurfaceView.Renderer {

    private final RenderState renderState;
    private RootElement newRootElement = null;
    private RootElement rootElement = null;
    private volatile long saveIndicatorEndTime = 0;
    private float resolutionScale = 1.0f;
    private FrameBuffer scaledFbo = null;

    private boolean isExportMode = false;

    public RendererCore(Resources resources, InternalVisualizationDataProvider internalDataProvider, boolean isExportMode) {
        this.isExportMode = isExportMode;
        renderState = new RenderState(internalDataProvider, isExportMode);
        renderState.onResources(resources);

        RootElement newSkinThemePreset = VisualizerViewCore.onRequestSelectedSkinThemePreset.invoke(null);
        setThemeElements(newSkinThemePreset);
    }

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        renderState.onSurfaceCreated();
        if (rootElement != null)
            rootElement.reCreateGLResources(renderState);
    }

    public void setResolutionScale(float scale) {
        this.resolutionScale = scale;
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        renderState.onSurfaceChanged(PlayerCore.s().getAppContext(), width, height);
        GLES20.glViewport(0, 0, width, height);
        
        if (scaledFbo != null) {
            scaledFbo.dispose();
            scaledFbo = null;
        }
        
        String scaleStr = com.aylis.comp.AppPreferences.AppPreferences.createOrGetInstance().getString(com.aylis.comp.AppPreferences.AppPreferences.PREF_String_visualizerResolutionScale);
        float scale = com.aylis.Common.Utils.strToFloatSafe(scaleStr, 1.5f);
        if (scale < 1.0f) scale = 1.0f;
        if (isExportMode) scale = 1.0f;
        resolutionScale = scale;

        if (resolutionScale > 1.05f) {
            try {
                scaledFbo = new FrameBuffer((int)(width / resolutionScale), (int)(height / resolutionScale), GLES20.GL_NEAREST, GLES20.GL_CLAMP_TO_EDGE);
            } catch (Exception e) {
                tlog.w("Failed to create scaled FBO: " + e.getMessage());
            }
        }

        if (rootElement != null)
            rootElement.reCreateGLResources(renderState);
    }

    private long lastFrameTimeMs = 0;
    private float overrideFrameTimeF = -1.0f;

    public void setOverrideFrameTime(float timeF) {
        this.overrideFrameTimeF = timeF;
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        if (overrideFrameTimeF > 0.0f) {
            renderState.setOverrideFrameTime(overrideFrameTimeF);
        } else {
            long currentTime = System.currentTimeMillis();
            int limit = com.aylis.comp.AppPreferences.AppPreferences.createOrGetInstance().getInt(com.aylis.comp.AppPreferences.AppPreferences.PREF_Int_visualizerFrameRateLimit);
            if (limit <= 0) limit = 60;
            if (limit > 120) limit = 120;
            
            long targetDurationMs = 1000L / limit;
            long elapsed = currentTime - lastFrameTimeMs;
            if (elapsed < targetDurationMs) {
                try {
                    Thread.sleep(targetDurationMs - elapsed);
                } catch (InterruptedException ignored) {}
                currentTime = System.currentTimeMillis();
            }
            lastFrameTimeMs = currentTime;
        }

        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

        String scaleStr = com.aylis.comp.AppPreferences.AppPreferences.createOrGetInstance().getString(com.aylis.comp.AppPreferences.AppPreferences.PREF_String_visualizerResolutionScale);
        float scale = com.aylis.Common.Utils.strToFloatSafe(scaleStr, 1.5f);
        if (scale < 1.0f) scale = 1.0f;
        if (isExportMode) scale = 1.0f;
        if (Math.abs(resolutionScale - scale) > 0.05f) {
            onSurfaceChanged(null, (int)renderState.getScreenWidth(), (int)renderState.getScreenHeight());
        }

        renderState.onFrameStart();

        if (scaledFbo != null) {
            renderState.bindFrameBuffer(scaledFbo);
            GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        }

        if (rootElement != null) {
            IFrameDataProvider iframeDataProvider = rootElement.getFrameDataProvider();
            if (iframeDataProvider != null)
                renderState.res.meter.setFrameDataRmsValue(iframeDataProvider.getRms());

            rootElement.onEarlyUpdate(renderState, null);
        }

        if (rootElement != newRootElement) {
            rootElement = newRootElement;
            if (rootElement != null)
                rootElement.reCreateGLResources(renderState);
        }

        if (rootElement != null) {
            rootElement.onRender(renderState, scaledFbo);
            rootElement.drawHighlightRecursive(renderState);
        }

        long now = System.currentTimeMillis();
        if (now < saveIndicatorEndTime) {
            long remaining = saveIndicatorEndTime - now;
            int alpha = (int) (180 * remaining / 1000);
            if (alpha > 0) {
                int color = (alpha << 24) | 0x004CAF50;
                float screenW = renderState.getScreenWidth();
                float screenH = renderState.getScreenHeight();
                float size = Math.min(screenW, screenH) * 0.08f;
                float centerX = screenW / 2f;
                float centerY = screenH / 2f;
                float thickness = 8f;

                drawLine(renderState, centerX - size, centerY - size, centerX + size, centerY + size, thickness, color);
                drawLine(renderState, centerX - size, centerY + size, centerX + size, centerY - size, thickness, color);
            }
        }

        if (scaledFbo != null) {
            renderState.bindFrameBuffer(null);
            
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            
            renderState.setBlendModeForce(0); // normal blend
            renderState.drawFullscreenQuad(0xffffffff, scaledFbo.getTexture());
        }

        renderState.onFrameEnd();
        renderState.bindFrameBuffer(null);
    }

    public int getFps() {
        return renderState.getFps();
    }

    public int getFrameTimeMs() {
        return renderState.getFrameTime();
    }

    public void setThemeElements(RootElement root) {
        newRootElement = root;
    }

    public RootElement getThemeElements() {
        return rootElement != null ? rootElement : newRootElement;
    }

    public void setThemeCustomizationData(int rootIdentifier, Element.CustomizationList customization) {
        if (rootElement != null) {
            if (rootElement.getIdentifier() == rootIdentifier)
                rootElement.setCustomization(customization);
            else
                tlog.w("rootElement identifier not match");
        }
    }

    public void setThemeCustomizationData(int rootIdentifier, Element.CustomizationList customization, int selectedIndex) {
        if (rootElement != null) {
            if (rootElement.getIdentifier() == rootIdentifier)
                rootElement.setCustomization(customization, selectedIndex);
            else
                tlog.w("rootElement identifier not match");
        }
    }

    public void triggerSaveIndicator() {
        saveIndicatorEndTime = System.currentTimeMillis() + 1000;
    }

    private void drawLine(RenderState renderState, float xA, float yA, float xB, float yB, float thickness, int color) {
        float dx = xB - xA;
        float dy = yB - yA;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len == 0) return;

        float ux = dx / len;
        float uy = dy / len;

        float px = -uy;
        float py = ux;

        float halfT = thickness / 2f;

        float x0 = xA + px * halfT;
        float y0 = yA + py * halfT;

        float x1 = xB + px * halfT;
        float y1 = yB + py * halfT;

        float x2 = xA - px * halfT;
        float y2 = yA - py * halfT;

        float x3 = xB - px * halfT;
        float y3 = yB - py * halfT;

        renderState.res.getBufferRenderer().drawRectangle(
            renderState,
            x0, y0,
            x1, y1,
            x2, y2,
            x3, y3,
            0f,
            color,
            com.aylis.Common.Vec2f.zero,
            com.aylis.Common.Vec2f.one,
            renderState.res.getAtlasTexWhite()
        );
    }

    public int readThemeCustomizationData(Element.CustomizationList customization) {
        if (rootElement != null) {
            if (rootElement.getCustomization(customization, 0))
                return rootElement.getIdentifier();
        }
        return -1;
    }

}
