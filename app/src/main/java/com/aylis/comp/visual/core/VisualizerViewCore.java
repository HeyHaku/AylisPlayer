

package com.aylis.comp.visual.core;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.EGL14;
import android.opengl.EGLExt;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Choreographer;
import com.aylis.comp.AppPreferences.AppPreferences;
import com.aylis.Common.Events.WeakEvent4;
import com.aylis.Common.Events.WeakEventR4;
import com.aylis.Common.Events.WeakEventR;
import com.aylis.Common.Events.WeakEventR1;
import com.aylis.Common.Events.WeakEventR2;
import com.aylis.Common.Events.WeakEventR3;
import com.aylis.comp.AlbumArt.AlbumArtRequest;
import com.aylis.comp.AlbumArt.ImageLoadedListener;
import com.aylis.comp.visual.core.playback.AudioFrameData;
import com.aylis.comp.visual.core.Elements.Element;
import com.aylis.comp.visual.core.Elements.RootElement;
import com.aylis.comp.visual.core.Graphic.RendererCore;
import java.util.ArrayList;
import java.util.List;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

public class VisualizerViewCore extends GLSurfaceView {

    public static WeakEventR1<AudioFrameData  , AudioFrameData> onRequestsSoundVisualizationData = new WeakEventR1<>();
    public static WeakEventR2<String  , VisualizerViewCore  , String> onRequestMeasureText = new WeakEventR2<>();
    public static WeakEventR4<String  , PointF  , PointF, Float  , PointF> onRequestMeasureVec2f = new WeakEventR4<>();
    public static WeakEventR<AlbumArtRequest> onRequestsAlbumArtPath = new WeakEventR<>();
    public static WeakEvent4<
            ImageLoadedListener  ,
                Integer  ,
                Integer  ,
                AlbumArtRequest
                > onRequestAlbumArtPathAndBitmap = new WeakEvent4<>();
    public static WeakEventR<RootElement> onRequestSelectedSkinThemePreset = new WeakEventR<>();

    private int EGLContextClientVersion = 2;
    private RendererCore renderer;

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    InternalVisualizationDataProvider internalDataProvider = new InternalVisualizationDataProvider() {
        @Override
        public AudioFrameData onRequestSoundVisualizationData(AudioFrameData outResult) {
            return VisualizerViewCore.onRequestsSoundVisualizationData.invoke(outResult, null);
        }

        @Override
        public String onRequestsMeasureText(String val) {
            return VisualizerViewCore.onRequestMeasureText.invoke(val, VisualizerViewCore.this, val);
        }

        @Override
        public PointF onRequestMeasureVec2f(String val, PointF argVec, PointF lastMeasured, Float frameDataRmsValue) {
            return VisualizerViewCore.onRequestMeasureVec2f.invoke(val, argVec, lastMeasured, frameDataRmsValue, argVec);
        }

        @Override
        public AlbumArtRequest onRequestsAlbumArtPath() {
            return VisualizerViewCore.onRequestsAlbumArtPath.invoke(null);
        }

        @Override
        public void onRequestAlbumArtPathAndBitmap(ImageLoadedListener loadedListener, Integer targetBoundsWidth, Integer targetBoundsHeight, AlbumArtRequest albumartRequest) {
            VisualizerViewCore.onRequestAlbumArtPathAndBitmap.invoke(loadedListener, targetBoundsWidth, targetBoundsHeight, albumartRequest);
        }
    };

    public VisualizerViewCore(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.setEGLContextClientVersion(3);
        EGLContextClientVersion = 3;
        this.setPreserveEGLContextOnPause(true);
        // Disable MSAA (samples=0, sampleBuffers=0) to prevent BLASTBufferQueue errors on some devices/emulators
        this.setEGLConfigChooser(new MyEGLConfigChooser(8, 8, 8, 8, 16, 0, 0, 0));

        renderer = new RendererCore(context.getResources(), internalDataProvider, false);
        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    public void setThemeElements(RootElement root) {
        renderer.setThemeElements(root);
    }

    public RootElement getThemeElements() {
        return renderer.getThemeElements();
    }

    public int getFps() {
        return renderer.getFps();
    }

    public int getFrameTimeMs() {
        return renderer.getFrameTimeMs();
    }

    public void setThemeCustomizationData(int rootIdentifier, Element.CustomizationList customization) {
        renderer.setThemeCustomizationData(rootIdentifier, customization);
    }

    public void setThemeCustomizationData(int rootIdentifier, Element.CustomizationList customization, int selectedIndex) {
        renderer.setThemeCustomizationData(rootIdentifier, customization, selectedIndex);
    }

    public void forceUpdateViewport(final int width, final int height) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (renderer != null) {
                    renderer.onSurfaceChanged(null, width, height);
                }
            }
        });
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Как только Android реально изменил ширину/высоту View на экране,
        // мы насильно пихаем новые w и h в поток OpenGL!
        if (w > 0 && h > 0) {
            forceUpdateViewport(w, h);
        }
    }
    public void triggerSaveIndicator() {
        renderer.triggerSaveIndicator();
    }

    public int readThemeCustomizationData(Element.CustomizationList customization) {
        return renderer.readThemeCustomizationData(customization);
    }

    private abstract class BaseConfigChooser
            implements EGLConfigChooser {
        protected int[] configSpec;

        public BaseConfigChooser(int[] configSpec) {
            this.configSpec = filterConfigSpec(configSpec);
        }

        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
            int[] num_config = new int[1];
            if (!egl.eglChooseConfig(display, configSpec, null, 0,
                    num_config)) {
                throw new IllegalArgumentException("eglChooseConfig failed");
            }

            int numConfigs = num_config[0];

            if (numConfigs <= 0) {
                throw new IllegalArgumentException(
                        "No configs match configSpec");
            }

            EGLConfig[] configs = new EGLConfig[numConfigs];
            if (!egl.eglChooseConfig(display, configSpec, configs, numConfigs,
                    num_config)) {
                throw new IllegalArgumentException("eglChooseConfig#2 failed");
            }
            EGLConfig config = chooseConfig(egl, display, configs);
            if (config == null) {
                throw new IllegalArgumentException("No config chosen");
            }
            return config;
        }

        abstract EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
                                        EGLConfig[] configs);

        private int[] filterConfigSpec(int[] configSpec) {
            if (EGLContextClientVersion != 2 && EGLContextClientVersion != 3) {
                return configSpec;
            }

            if (EGLContextClientVersion == 2 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                int len = configSpec.length;
                int[] newConfigSpec = new int[len + 2];
                System.arraycopy(configSpec, 0, newConfigSpec, 0, len - 1);
                newConfigSpec[len - 1] = EGL10.EGL_RENDERABLE_TYPE;
                newConfigSpec[len] = EGL14.EGL_OPENGL_ES2_BIT;
                newConfigSpec[len + 1] = EGL10.EGL_NONE;
                return newConfigSpec;

            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {

                int len = configSpec.length;
                int[] newConfigSpec = new int[len + 2];
                System.arraycopy(configSpec, 0, newConfigSpec, 0, len - 1);
                newConfigSpec[len - 1] = EGL10.EGL_RENDERABLE_TYPE;
                newConfigSpec[len] = EGLExt.EGL_OPENGL_ES3_BIT_KHR;
                newConfigSpec[len + 1] = EGL10.EGL_NONE;
                return newConfigSpec;

            } else {
                return configSpec;
            }
        }
    }

    private class MyEGLConfigChooser extends BaseConfigChooser {

        protected int redSize;
        protected int greenSize;
        protected int blueSize;
        protected int alphaSize;
        protected int depthSize;
        protected int stencilSize;
        protected int sampleBuffers;
        protected int samples;
        private int[] value;

        public MyEGLConfigChooser(int redSize, int greenSize, int blueSize,
                                  int alphaSize, int depthSize, int stencilSize,
                                  int sampleBuffers, int samples) {
            super(new int[]{
                    EGL10.EGL_RED_SIZE, redSize,
                    EGL10.EGL_GREEN_SIZE, greenSize,
                    EGL10.EGL_BLUE_SIZE, blueSize,
                    EGL10.EGL_ALPHA_SIZE, alphaSize,
                    EGL10.EGL_DEPTH_SIZE, depthSize,
                    EGL10.EGL_STENCIL_SIZE, stencilSize,
                    EGL10.EGL_NONE});

            value = new int[1];
            this.redSize = redSize;
            this.greenSize = greenSize;
            this.blueSize = blueSize;
            this.alphaSize = alphaSize;
            this.depthSize = depthSize;
            this.stencilSize = stencilSize;
            this.sampleBuffers = sampleBuffers;
            this.samples = samples;
        }

        @Override
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
                                      EGLConfig[] configs) {
            List<EGLConfig> configs1 = new ArrayList<>();

            for (EGLConfig config : configs) {
                int d = findConfigAttrib(egl, display, config,
                        EGL10.EGL_DEPTH_SIZE, 0);
                int s = findConfigAttrib(egl, display, config,
                        EGL10.EGL_STENCIL_SIZE, 0);
                if ((d >= depthSize) && (s >= stencilSize)) {
                    int r = findConfigAttrib(egl, display, config,
                            EGL10.EGL_RED_SIZE, 0);
                    int g = findConfigAttrib(egl, display, config,
                            EGL10.EGL_GREEN_SIZE, 0);
                    int b = findConfigAttrib(egl, display, config,
                            EGL10.EGL_BLUE_SIZE, 0);
                    int a = findConfigAttrib(egl, display, config,
                            EGL10.EGL_ALPHA_SIZE, 0);
                    if ((r == redSize) && (g == greenSize)
                            && (b == blueSize) && (a == alphaSize)) {
                        configs1.add(config);
                    }
                }
            }

            EGLConfig resultConfing = configs1.size() > 0 ? configs1.get(0) : null;
            for (EGLConfig config : configs1) {
                int sampleBuff = findConfigAttrib(egl, display, config,
                        EGL10.EGL_SAMPLE_BUFFERS, 0);
                int samples = findConfigAttrib(egl, display, config,
                        EGL10.EGL_SAMPLES, 0);
                if ((sampleBuff >= sampleBuffers) && (samples >= this.samples)) {
                    return config;
                }
            }

            return resultConfing;
        }

        private int findConfigAttrib(EGL10 egl, EGLDisplay display,
                                     EGLConfig config, int attribute, int defaultValue) {
            if (egl.eglGetConfigAttrib(display, config, attribute, value))
                return value[0];

            return defaultValue;
        }
    }
}

