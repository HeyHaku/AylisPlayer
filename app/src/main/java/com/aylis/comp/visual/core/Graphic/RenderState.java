

package com.aylis.comp.visual.core.Graphic;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.view.Display;
import android.view.WindowManager;
import com.aylis.Common.Vec2f;
import com.aylis.Common.Vec2i;
import com.aylis.Common.tlog;
import com.aylis.comp.visual.core.InternalVisualizationDataProvider;
import com.aylis.comp.visual.core.Elements.Meter;
import com.aylis.R;
import javax.microedition.khronos.opengles.GL10;
import com.aylis.comp.visual.core.gl.mdesl.graphics.Texture;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.FrameBuffer;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.ShaderProgram;

public class RenderState {

    private final float[] vPMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];

    private float frameTimeF = 0.0f;
    private float frameTimeSmooth = 0.0f;
    private int fps = 0;
    private int frameTime = 0;
    private int fpsAcc = 0;
    private long fpsTimeAcc = 0;
    private long lastTimeMs = 0;

    private int fullscreenWidth, fullscreenHeight;
    private int screenWidth, screenHeight;
    private int currentBlendMode = -1;

    public final RenderResources res;

    private boolean isExportMode = false;

    public boolean isExportMode() {
        return isExportMode;
    }

    public RenderState(InternalVisualizationDataProvider internalDataProvider, boolean isExportMode) {
        this.isExportMode = isExportMode;
        res = new RenderResources(this, internalDataProvider);
        Matrix.setIdentityM(viewMatrix, 0);
        Matrix.translateM(viewMatrix, 0, 0.0f, 0.0f, -10.0f);
    }

    public float[] getVPMatrix() {
        return vPMatrix;
    }

    public float getFrameTimeF() {
        return frameTimeF;
    }

    public float getFrameTimeSmooth() {
        return frameTimeSmooth;
    }

    public int getFps() {
        return fps;
    }

    public int getFrameTime() {
        return frameTime;
    }

    public int getFullscreenWidth() {
        return fullscreenWidth;
    }

    public int getFullscreenHeight() {
        return fullscreenHeight;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public static boolean checkOGLError(String tag) {
        int err = GLES30.glGetError();
        if (err != GLES30.GL_NO_ERROR) {
            tlog.w("OGL Error (" + tag + ") : " + err);
            return false;
        }

        return true;
    }

    public boolean isVisibleOnScreen(Vec2f pos, float radiusMargin) {

        return true;
    }

    public void onResources(Resources resources) {
        res.onResources(resources);
    }

    public void onSurfaceCreated() {
        unsetBlendMode();
        GLES30.glEnable(GL10.GL_BLEND);
        res.onSurfaceCreated();
    }

    public void onSurfaceChanged(Context context, int width, int height) {
        screenWidth = width;
        screenHeight = height;

        fullscreenWidth = screenWidth;
        fullscreenHeight = screenHeight;

        if (context != null) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();

            Point size = new Point();
            display.getSize(size);
            fullscreenWidth = size.x;
            fullscreenHeight = size.y;

            tlog.notice("fullscreen size: " + fullscreenWidth + "; " + fullscreenHeight);
        }
    }

    private float overrideFrameTimeF = -1.0f;
    public void setOverrideFrameTime(float timeF) {
        this.overrideFrameTimeF = timeF;
    }

    public void onFrameStart() {
        long timeMs = SystemClock.uptimeMillis();
        
        if (overrideFrameTimeF > 0.0f) {
            frameTimeF = overrideFrameTimeF;
            frameTimeSmooth = overrideFrameTimeF;
            frameTime = (int)(overrideFrameTimeF * 1000f);
        } else {
            frameTime = (int) (timeMs - lastTimeMs);
            fpsAcc++;

            if (timeMs - fpsTimeAcc >= 1000) {
                fps = fpsAcc;
                fpsAcc = 0;
                fpsTimeAcc = timeMs;
            }

            lastTimeMs = timeMs;
            if (frameTime < 0) frameTime = 0;
            if (frameTime > 1000) frameTime = 1000;
            frameTimeF = frameTime * 0.001f;
            frameTimeSmooth = (frameTimeSmooth * 0.9f) + (frameTimeF * 0.1f);
        }

        res.bufferRenderer.onFrameStart(this);
        res.meter.onFrameStart();
    }

    public void onFrameEnd() {
        res.bufferRenderer.onFrameEnd(this);
    }

    public void unsetBlendMode() {
        currentBlendMode = -1;
    }

    public void setBlendModeForce(int mode) {
        currentBlendMode = -1;
        setBlendMode(mode);
    }

    public void setBlendMode(int mode) {
        if (currentBlendMode == mode) return;

        res.bufferRenderer.onFrameEnd(this);

        switch (mode) {
            case 0:
                GLES30.glBlendFunc(770, 771);
                break;
            case 1:
                GLES30.glBlendFunc(1, 769);
                break;
            case 2:
                GLES30.glBlendFunc(1, 1);
                break;
            case 3:
                GLES30.glBlendFunc(1, 0);
                break;
            case 4:
                GLES30.glBlendFunc(1, 771);
                break;
            case 5:
                GLES30.glBlendFunc(770, 1);
                break;
        }

        currentBlendMode = mode;
    }

    public void bindFrameBuffer(FrameBuffer fb) {
        res.bufferRenderer.onFrameEnd(this);

        if (fb == null) {
            GLES30.glViewport(0, 0, screenWidth, screenHeight);
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
            Matrix.orthoM(projectionMatrix, 0, 0, screenWidth, screenHeight, 0, 0.01f, 100f);
            Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        } else {
            fb.begin();
        }
    }

    public void bindShader(ShaderProgram shader) {
        if (res.atlasBufferShader != shader)
            res.bufferRenderer.onFrameEnd(this);

        shader.use();
    }

    public Vec2i getSafeRenderBufferSizeTextureDim() {
        int[] out_container = new int[1];
        GLES30.glGetIntegerv(GLES30.GL_MAX_RENDERBUFFER_SIZE, out_container, 0);

        int maxTextureSize = out_container[0];

        tlog.notice("OGL Max render buffer size: " + maxTextureSize);
        tlog.notice("fullscreen size: " + fullscreenWidth + "; " + fullscreenHeight);

        String scaleStr = com.aylis.comp.AppPreferences.AppPreferences.createOrGetInstance().getString(com.aylis.comp.AppPreferences.AppPreferences.PREF_String_visualizerResolutionScale);
        float scale = com.aylis.Common.Utils.strToFloatSafe(scaleStr, 1.5f);
        if (scale < 1.0f) scale = 1.0f;
        if (isExportMode) scale = 1.0f;

        int w = Math.max(1, (int)(fullscreenWidth / scale));
        int h = Math.max(1, (int)(fullscreenHeight / scale));

        return new Vec2i(Math.min(w, maxTextureSize), Math.min(h, maxTextureSize));
    }

    public Vec2i getSafeFullScreenSizeTextureDim() {
        int[] out_container = new int[1];
        GLES30.glGetIntegerv(GLES30.GL_MAX_TEXTURE_SIZE, out_container, 0);

        int maxTextureSize = out_container[0];

        tlog.notice("OGL Max texture size: " + maxTextureSize);
        tlog.notice("fullscreen size: " + fullscreenWidth + "; " + fullscreenHeight);

        int w = fullscreenWidth;
        int h = fullscreenHeight;

        return new Vec2i(Math.min(w, maxTextureSize), Math.min(h, maxTextureSize));
    }

    public Vec2i getSafeScreenSizeTextureDim() {

        int[] out_container = new int[1];
        GLES30.glGetIntegerv(GLES30.GL_MAX_TEXTURE_SIZE, out_container, 0);

        int maxTextureSize = out_container[0];

        tlog.notice("OGL Max texture size: " + maxTextureSize);

        int w = screenWidth;
        int h = screenHeight;

        return new Vec2i(Math.min(w, maxTextureSize), Math.min(h, maxTextureSize));
    }

    public void drawFullscreenQuad(int color1, Texture texture) {
        this.res.bufferRenderer.drawRectangleRightBottomWH(
                this,
                0.0f, screenHeight, 0.0f,
                screenWidth, -screenHeight,
                color1,
                Vec2f.zero, Vec2f.one,
                new AtlasTexture(texture));
    }

    public void drawFullscreenQuad(int color1, Texture texture, Vec2f tex0, Vec2f tex1) {
        this.res.bufferRenderer.drawRectangleRightBottomWH(
                this,
                0.0f, screenHeight, 0.0f,
                screenWidth, -screenHeight,
                color1,
                tex0, tex1,
                new AtlasTexture(texture));
    }

    public void drawFullscreenQuad(int color1, AtlasTexture entryTexture) {
        this.res.bufferRenderer.drawRectangleRightBottomWH(
                this,
                0.0f, screenHeight, 0.0f,
                screenWidth, -screenHeight,
                color1,
                Vec2f.zero, Vec2f.one,
                entryTexture);

    }

    public void drawFullscreenQuadNonAtlasBuffer() {
        res.fullQuad.draw();
    }

    public static class RenderResources {

        public final InternalVisualizationDataProvider visualizationData;
        public final Meter meter;

        private String blurVERT,blurFRAG, blurFRAG2, kawaseFRAG;
        private String bufferVERT, bufferFRAG;
        private String bufferMaskFRAG;
        private String bufferChromaKeyFRAG, bufferMaskChromaKeyFRAG;
        private String redLandscapeFRAG;
        private String rgbSplitFRAG;
        private String fxaaShaderVERT, fxaaShaderFRAG;
        private String videoOesVERT, videoOesFRAG;

        private VShaderProgram blurShader, blurShader2, kawaseShader;
        private VShaderProgram fxaaShader;
        private VShaderProgram atlasBufferShader;
        private VShaderProgram atlasBufferMaskShader;
        private VShaderProgram atlasBufferChromaKeyShader;
        private VShaderProgram atlasBufferMaskChromaKeyShader;
        private VShaderProgram redLandscapeShader;
        private VShaderProgram rgbSplitShader;
        private VShaderProgram videoOesShader;

        private BufferRenderer bufferRenderer;
        private SpriteFontRenderer fontRenderer;
        private FullscreenQuad fullQuad;

        private Texture texWhite, texBlack;
        private AtlasTexture atlasTexWhite, atlasTexBlack;
        private Texture texParticle0;
        private AtlasTexture atlasTexParticle0;
        private Bitmap bitmapParticle0;

        RenderResources(RenderState renderState, InternalVisualizationDataProvider internalDataProvider)
        {
            visualizationData = internalDataProvider;
            meter = new Meter(renderState);
        }

        public VShaderProgram getAtlasBufferShader() {
            return atlasBufferShader;
        }

        public VShaderProgram getAtlasBufferMaskShader() {
            return atlasBufferMaskShader;
        }

        public VShaderProgram getAtlasBufferChromaKeyShader() {
            return atlasBufferChromaKeyShader;
        }

        public VShaderProgram getAtlasBufferMaskChromaKeyShader() {
            return atlasBufferMaskChromaKeyShader;
        }

        public VShaderProgram getRedLandscapeShader() {
            return redLandscapeShader;
        }

        public VShaderProgram getRgbSplitShader() {
            return rgbSplitShader;
        }

        public VShaderProgram getFxaaShader() {
            return fxaaShader;
        }
        
        public VShaderProgram getVideoOesShader() {
            return videoOesShader;
        }

        public VShaderProgram getBlurShader2() {
            return blurShader2;
        }

        public VShaderProgram getBlurShader() {
            return blurShader;
        }

        public VShaderProgram getKawaseShader() {
            return kawaseShader;
        }

        public BufferRenderer getBufferRenderer() {
            return bufferRenderer;
        }

        public SpriteFontRenderer getFontRenderer() {
            return fontRenderer;
        }

        public FullscreenQuad getFullQuad() {
            return fullQuad;
        }

        public AtlasTexture getAtlasTexWhite() {
            return atlasTexWhite;
        }

        public AtlasTexture getAtlasTexBlack() {
            return atlasTexBlack;
        }

        public AtlasTexture getAtlasTexParticle0() {
            return atlasTexParticle0;
        }

        public void onSurfaceCreated() {

            fullQuad = new FullscreenQuad();

            texWhite = new VTexture(0xffffffff,
                    2, 2,
                    VTexture.DEFAULT_FILTER,
                    VTexture.DEFAULT_FILTER,
                    VTexture.DEFAULT_WRAP,
                    false);

            texBlack = new VTexture(0xff000000,
                    2, 2,
                    VTexture.DEFAULT_FILTER,
                    VTexture.DEFAULT_FILTER,
                    VTexture.DEFAULT_WRAP,
                    false);

            atlasTexWhite = new AtlasTexture(texWhite);
            atlasTexBlack = new AtlasTexture(texBlack);

            texParticle0 = new VTexture(bitmapParticle0,
                    VTexture.DEFAULT_FILTER,
                    VTexture.DEFAULT_FILTER,
                    VTexture.DEFAULT_WRAP,
                    false);

            atlasTexParticle0 = new AtlasTexture(texParticle0);

            try {

                {
                    blurShader = new VShaderProgram(blurVERT, blurFRAG);

                    if (blurShader.getLog().length() != 0)
                        tlog.w(blurShader.getLog());

                }
                {
                    blurShader2 = new VShaderProgram(blurVERT, blurFRAG2);

                    if (blurShader2.getLog().length() != 0)
                        tlog.w(blurShader2.getLog());
                }

                {
                    kawaseShader = new VShaderProgram(blurVERT, kawaseFRAG);

                    if (kawaseShader.getLog().length() != 0)
                        tlog.w(kawaseShader.getLog());
                }

                {
                    fxaaShader = new VShaderProgram(fxaaShaderVERT, fxaaShaderFRAG);

                    if (fxaaShader.getLog().length() != 0)
                        tlog.w(fxaaShader.getLog());
                }

                atlasBufferShader = new VShaderProgram(bufferVERT, bufferFRAG);
                if (atlasBufferShader.getLog().length() != 0)
                    tlog.w(atlasBufferShader.getLog());

                atlasBufferMaskShader = new VShaderProgram(bufferVERT, bufferMaskFRAG);
                if (atlasBufferMaskShader.getLog().length() != 0)
                    tlog.w(atlasBufferMaskShader.getLog());

                atlasBufferChromaKeyShader = new VShaderProgram(bufferVERT, bufferChromaKeyFRAG);
                if (atlasBufferChromaKeyShader.getLog().length() != 0)
                    tlog.w(atlasBufferChromaKeyShader.getLog());

                atlasBufferMaskChromaKeyShader = new VShaderProgram(bufferVERT, bufferMaskChromaKeyFRAG);
                if (atlasBufferMaskChromaKeyShader.getLog().length() != 0)
                    tlog.w(atlasBufferMaskChromaKeyShader.getLog());

                redLandscapeShader = new VShaderProgram(bufferVERT, redLandscapeFRAG);
                if (redLandscapeShader.getLog().length() != 0)
                    tlog.w(redLandscapeShader.getLog());

                rgbSplitShader = new VShaderProgram(bufferVERT, rgbSplitFRAG);
                if (rgbSplitShader.getLog().length() != 0)
                    tlog.w(rgbSplitShader.getLog());

                videoOesShader = new VShaderProgram(videoOesVERT, videoOesFRAG);
                if (videoOesShader != null && videoOesShader.getLog().length() != 0)
                    tlog.w(videoOesShader.getLog());

                bufferRenderer = new BufferRenderer(atlasBufferShader, 1200);

                fontRenderer = new SpriteFontRenderer(bufferRenderer);
            } catch (Exception e) {
                tlog.w("RenderState error: " + e.getMessage());
            }
        }

        public void onResources(Resources resources) {
            blurVERT = GraphicsUtils.readResource(resources, R.raw.blur_vert);
            blurFRAG = GraphicsUtils.readResource(resources, R.raw.blurh_frag);
            blurFRAG2 = GraphicsUtils.readResource(resources, R.raw.blurv_frag);
            kawaseFRAG = GraphicsUtils.readResource(resources, R.raw.kawase_frag);

            bufferVERT = GraphicsUtils.readResource(resources, R.raw.buffer_vert);
            bufferFRAG = GraphicsUtils.readResource(resources, R.raw.buffer_frag);
            bufferMaskFRAG = GraphicsUtils.readResource(resources, R.raw.buffer_mask_frag);
            bufferChromaKeyFRAG = GraphicsUtils.readResource(resources, R.raw.buffer_chroma_key_frag);
            bufferMaskChromaKeyFRAG = GraphicsUtils.readResource(resources, R.raw.buffer_mask_chroma_key_frag);
            redLandscapeFRAG = GraphicsUtils.readResource(resources, R.raw.red_landscape_frag);
            rgbSplitFRAG = GraphicsUtils.readResource(resources, R.raw.buffer_rgb_split_frag);

            fxaaShaderVERT = GraphicsUtils.readResource(resources, R.raw.fxaa_vert);
            fxaaShaderFRAG = GraphicsUtils.readResource(resources, R.raw.fxaa_frag);

            videoOesVERT = GraphicsUtils.readResource(resources, R.raw.video_oes_vert);
            videoOesFRAG = GraphicsUtils.readResource(resources, R.raw.video_oes_frag);

            bitmapParticle0 = BitmapFactory.decodeResource(resources,
                    R.drawable.particle_blur0);
        }
    }

    public boolean getRenderEdges() {
        return false;
    }
}

