

package com.aylis.comp.visual.core.Elements.Shaders;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import com.aylis.Common.Vec2f;
import com.aylis.Common.Vec2i;
import com.aylis.Common.tlog;
import com.aylis.PlayerCore;
import com.aylis.comp.AlbumArt.AlbumArtRequest;
import com.aylis.comp.AlbumArt.ImageLoadedListener;
import com.aylis.comp.AppPreferences.AppPreferences;
import com.aylis.comp.visual.core.Elements.Element;
import com.aylis.comp.visual.core.Elements.PreCompElement;
import com.aylis.comp.visual.core.Elements.PreCompManager;
import com.aylis.comp.visual.core.Graphic.AtlasTexture;
import com.aylis.comp.visual.core.Graphic.GraphicsUtils;
import com.aylis.comp.visual.core.Graphic.RenderState;
import com.aylis.comp.visual.core.Graphic.VFrameBuffer;
import com.aylis.comp.visual.core.Graphic.VTexture;
import com.aylis.comp.visual.core.gl.mdesl.graphics.Texture;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.FrameBuffer;
import com.aylis.comp.visual.core.Graphic.SafeMipmapHelper;

public class BlurElement extends Element implements ImageLoadedListener {

    public static final String PRECOMP_PREFIX = "precomp:";

    private String customImagePath = "";
    private AlbumArtRequest albumArtRequest = new AlbumArtRequest("", "", "", "");
    private Bitmap bitmap = null;
    private boolean bitmapLoading = false;
    private boolean bitmapLoadedIn = false;
    private Object imageLoadStrongReference;

    private Texture tex2 = null;
    private AtlasTexture atlasTex2 = null;

    private boolean useMipmaps = false;
    private float radius = 1.0f;
    private VFrameBuffer blurTargetA, blurTargetB, blurTargetContent;
    private int color2 = 0xffffffff;
    private Vec2f[] blurLayerScales = new Vec2f[3];
    private boolean renderContentFxaa = false;
    private boolean renderContent = false;
    private String blurType = "Gaussian";

    public BlurElement() {
        blurLayerScales[0] = new Vec2f(1.0f, 1.0f);
        for (int i = 1; i < blurLayerScales.length; i++)
            blurLayerScales[i] = new Vec2f(0.0f, 0.0f);
    }

    public String getSelectedPreCompName() {
        if (customImagePath != null && customImagePath.startsWith(PRECOMP_PREFIX)) {
            return customImagePath.substring(PRECOMP_PREFIX.length());
        }
        return null;
    }

    public void setCustomImagePath(String path) {
        if (path == null) path = "";
        if (this.customImagePath.equals(path)) return;
        this.customImagePath = path;
        if (!path.startsWith(PRECOMP_PREFIX)) {
            this.albumArtRequest = new AlbumArtRequest(path, path, "", "");
        }
        this.markNeedReCreateGLResources();
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, String dataSource, String url0, String url1) {
        if (com.aylis.Common.Utils.compareNullStrings(dataSource, albumArtRequest.videoThumbDataSource)) {
            if (com.aylis.Common.Utils.compareNullStrings(url0, albumArtRequest.path0)) {
                if (com.aylis.Common.Utils.compareNullStrings(url1, albumArtRequest.path1)) {
                    this.bitmap = bitmap;
                    bitmapLoadedIn = false;
                    super.markNeedReCreateGLResources();
                }
            }
        }
    }

    @Override
    public void setUserObject1(Object obj1) {
        imageLoadStrongReference = obj1;
    }

    public void setBlurRadius(float radius) {
        this.radius = radius;
    }

    public void setColor2(int colorARGB) {
        color2 = colorARGB;
    }

    public void setBlurLayerScale(int index, float blurLayerScaleX, float blurLayerScaleY) {
        blurLayerScales[index] = new Vec2f(blurLayerScaleX, blurLayerScaleY);
    }

    public void setBlurLayerScale(int index, Vec2f blurLayerScale) {
        blurLayerScales[index] = blurLayerScale;
    }

    public void setRenderContentOnTop(boolean renderContent) {

        renderContentFxaa = renderContent;
    }

    @Override
    public void onApplyCustomization(CustomizationData customizationData) {
        super.onApplyCustomization(customizationData);
        setColor2(customizationData.getPropertyInt("color", color2));
        setBlurRadius(customizationData.getPropertyFloat("blurRadius", radius));
        setRenderContentOnTop(customizationData.getPropertyBool("showUnblurred", renderContentFxaa));
        blurType = customizationData.getPropertyString("blurType", blurType);

        blurLayerScales[0] = customizationData.getPropertyVec2f("blurScale", blurLayerScales[0]);
        blurLayerScales[0] = customizationData.getPropertyVec2f("1layerScale", blurLayerScales[0]);
        blurLayerScales[1] = customizationData.getPropertyVec2f("2layerScale", blurLayerScales[1]);
        blurLayerScales[2] = customizationData.getPropertyVec2f("3layerScale", blurLayerScales[2]);

        String imagePath = customizationData.getPropertyString("customImage", "");
        CustomizationData preCompChild = customizationData.getChild("preCompSelection");
        if (preCompChild != null) {
            String legacyPreComp = preCompChild.getChildTypeValue();
            if (legacyPreComp != null && !legacyPreComp.isEmpty() && !"None".equals(legacyPreComp)) {
                if (imagePath.isEmpty() || !imagePath.startsWith(PRECOMP_PREFIX)) {
                    imagePath = PRECOMP_PREFIX + legacyPreComp;
                }
            }
        }
        setCustomImagePath(imagePath);
    }

    @Override
    public void onReadCustomization(CustomizationData outCustomizationData) {
        super.onReadCustomization(outCustomizationData);
        outCustomizationData.setCustomizationName("Blur Effect");
        outCustomizationData.putPropertyString("customImage", customImagePath, "img", "1_image");
        outCustomizationData.putPropertyInt("color", color2, "crgb", "1_image");
        outCustomizationData.putPropertyFloat("blurRadius", radius, "f 0.0 2.0", "1_image");
        outCustomizationData.putPropertyBool("showUnblurred", renderContentFxaa, "1_image");
        outCustomizationData.putPropertyVec2f("1layerScale", blurLayerScales[0], "f2 0.0 2.0", "1_image");
        outCustomizationData.putPropertyVec2f("2layerScale", blurLayerScales[1], "f2 0.0 2.0", "1_image");
        outCustomizationData.putPropertyVec2f("3layerScale", blurLayerScales[2], "f2 0.0 2.0", "1_image");
        outCustomizationData.putPropertyString("blendMode", com.aylis.comp.visual.core.Elements.AppBlendMode.getTypeName(getBlendMode()), com.aylis.comp.visual.core.Elements.AppBlendMode.getSelectorString(), "1_image");
        outCustomizationData.putPropertyString("blurType", blurType, "sel Gaussian Kawase", "1_image");
    }

    @Override
    protected void markNeedReCreateGLResources() {
        bitmap = null;
        bitmapLoading = false;
        bitmapLoadedIn = false;
        super.markNeedReCreateGLResources();
    }

    @Override
    public void reCreateGLResources(RenderState renderData) {
        this.markNeedReCreateGLResources();
        super.reCreateGLResources(renderData);
    }

    protected void onAlbumArtCreateGLResources(Bitmap bitmap) {
        if (bitmap == null) {
            tex2 = null;
            atlasTex2 = null;
            return;
        }

        tex2 = new VTexture(bitmap,
                VTexture.DEFAULT_FILTER,
                VTexture.DEFAULT_FILTER,
                VTexture.DEFAULT_WRAP,
                false);

        atlasTex2 = new AtlasTexture(tex2);
    }

    @Override
    protected void onCreateGLResources(RenderState renderData) {

        if (customImagePath != null && customImagePath.startsWith(PRECOMP_PREFIX)) {
            tex2 = null;
            atlasTex2 = null;
            bitmap = null;
        } else if (customImagePath != null && !customImagePath.isEmpty()) {
            if (!bitmapLoading) {
                bitmapLoading = true;
                final int targetBoundsWidth = renderData.getFullscreenWidth();
                final int targetBoundsHeight = renderData.getFullscreenHeight();

                renderData.res.visualizationData.onRequestAlbumArtPathAndBitmap(
                        this,
                        targetBoundsWidth,
                        targetBoundsHeight,
                        albumArtRequest.makeCopy());
            }

            if (!bitmapLoadedIn) {
                bitmapLoadedIn = true;
                onAlbumArtCreateGLResources(bitmap);
            }
        } else {
            tex2 = null;
            atlasTex2 = null;
        }

        bitmap = null;

        Context context = PlayerCore.s().getAppContext();
        useMipmaps = context != null && AppPreferences.createOrGetInstance().preferencesGetBoolSafe(context, "pref_highQualityBlur", false);

        try {
            Vec2i frameBufferSize = renderData.getSafeRenderBufferSizeTextureDim();

            blurTargetContent = VFrameBuffer.createSafe(frameBufferSize.x, frameBufferSize.y, VTexture.LINEAR, VTexture.DEFAULT_WRAP, useMipmaps);
            if(blurTargetContent != null) blurTargetContent = blurTargetContent.checkIfValid();

            float downScaledSizeX = frameBufferSize.x / 4.1f;
            float downScaledSizeY = frameBufferSize.y / 4.1f;

            blurTargetA = VFrameBuffer.createSafe((int) downScaledSizeX, (int) downScaledSizeY, VTexture.LINEAR, VTexture.DEFAULT_WRAP, false);
            if(blurTargetA != null) blurTargetA = blurTargetA.checkIfValid();

            blurTargetB = VFrameBuffer.createSafe((int) downScaledSizeX, (int) downScaledSizeY, VTexture.LINEAR, VTexture.DEFAULT_WRAP, false);
            if(blurTargetB != null) blurTargetB = blurTargetB.checkIfValid();

        } catch (Exception e) {
            tlog.w(e.getMessage());
        }

        super.onCreateGLResources(renderData);
    }

    @Override
    public void onRender(RenderState renderData, FrameBuffer resultFB) {

        if (blurTargetContent == null || blurTargetA == null || blurTargetB == null) {
            super.onRender(renderData, resultFB);
            return;
        }

        this.onRenderCheckResources(renderData);

        Texture targetTex = null;
        AtlasTexture targetAtlasTex = null;

        String preCompName = getSelectedPreCompName();
        if (preCompName != null) {
            PreCompElement targetPreComp = PreCompManager.get(preCompName);
            if (targetPreComp != null) {
                targetPreComp.renderOnDemand(renderData, resultFB);
                targetTex = targetPreComp.getTexture();
                if (targetTex != null) {
                    targetAtlasTex = new AtlasTexture(targetTex);
                }
            }
        } else {
            targetTex = tex2;
            targetAtlasTex = atlasTex2;
        }

        if (targetAtlasTex == null) {

            return;
        }

        renderData.bindFrameBuffer(blurTargetContent);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        setupFrameBuffer();

        {
            renderData.drawFullscreenQuad(0xffffffff, targetAtlasTex);

            if ("Kawase".equals(blurType)) {
                kawaseBlur(renderData, blurTargetContent.getTexture());
            } else {

                horizontalBlur(renderData, blurTargetA, blurTargetContent.getTexture());

                verticalBlur2(renderData, blurTargetB, blurTargetA.getTexture());
            }

            super.onRender(renderData, resultFB);

            for(int j = blurLayerScales.length-1; j >=0 ; j--)
            {
                Vec2f textureScale = blurLayerScales[j];
                if (textureScale.x != 0.0f && textureScale.y != 0.0f) {
                    float texw = 1.0f / textureScale.x * 0.5f;
                    float texh = 1.0f / textureScale.y * 0.5f;
                    Vec2f tex0 = new Vec2f(0.5f - texw, 0.5f - texh);
                    Vec2f tex1 = new Vec2f(0.5f + texw, 0.5f + texh);

                    renderData.drawFullscreenQuad(0xffffffff, blurTargetB.getTexture(), tex0, tex1);
                }
            }
        }

        if (renderContentFxaa) {

            renderData.bindShader(renderData.res.getFxaaShader());
            renderData.res.getFxaaShader().setUniformf("resolutionW", blurTargetContent.getTexture().getWidth());
            renderData.res.getFxaaShader().setUniformf("resolutionH", blurTargetContent.getTexture().getHeight());
            blurTargetContent.getTexture().bind();
            renderData.res.getFullQuad().drawShader(renderData.res.getFxaaShader(), "Position");

        } else if (renderContent) {

            renderData.drawFullscreenQuad(0xffffffff, blurTargetContent.getTexture());
        }
    }

    private void setupFrameBuffer0() {
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR_MIPMAP_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
    }

    private void setupFrameBuffer() {
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
    }

    private void horizontalBlur(RenderState renderData, FrameBuffer resultFB, Texture content) {
        renderData.bindShader(renderData.res.getBlurShader());

        renderData.res.getBlurShader().setUniformf("resolutionW", resultFB.getWidth());
        renderData.res.getBlurShader().setUniformf("resolutionH", resultFB.getHeight());
        renderData.res.getBlurShader().setUniformf("radius", radius);

        renderData.bindFrameBuffer(resultFB);
        renderData.setBlendMode(3);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        content.bind();

        if (useMipmaps) {
            setupFrameBuffer0();
            if (SafeMipmapHelper.isTextureReady(content)) {
                SafeMipmapHelper.applyNpotSafeWrap();
                SafeMipmapHelper.generateMipmapSafe(GLES20.GL_TEXTURE_2D, "BlurElement");
            } else {
                SafeMipmapHelper.fallbackToLinearFilter(GLES20.GL_TEXTURE_2D);
            }
        } else {
            setupFrameBuffer();
        }

        renderData.res.getFullQuad().drawShader(renderData.res.getBlurShader(), "Position");
    }

    private void verticalBlur2(RenderState renderData, FrameBuffer resultFB, Texture contentHBlured) {

        renderData.bindShader(renderData.res.getBlurShader2());
        renderData.res.getBlurShader2().setUniformf("resolutionW", resultFB.getWidth());
        renderData.res.getBlurShader2().setUniformf("resolutionH", resultFB.getHeight());

        renderData.res.getBlurShader2().setUniformf("radius", radius);
        float[] color2f = new float[4];
        GraphicsUtils.intColorToF4Color(color2f, color2);
        renderData.res.getBlurShader2().setUniformf("Color2", color2f[0], color2f[1], color2f[2], color2f[3]);

        renderData.bindFrameBuffer(resultFB);
        renderData.setBlendMode(3);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        setupFrameBuffer();

        contentHBlured.bind();
        renderData.res.getFullQuad().drawShader(renderData.res.getBlurShader2(), "Position");
    }

    private void kawaseBlur(RenderState renderData, Texture content) {
        renderData.bindShader(renderData.res.getKawaseShader());

        float currentRadius = 0.5f * radius;
        renderData.res.getKawaseShader().setUniformf("u_offset", currentRadius / blurTargetA.getWidth(), currentRadius / blurTargetA.getHeight());
        renderData.bindFrameBuffer(blurTargetA);
        renderData.setBlendMode(3);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        content.bind();
        setupFrameBuffer();
        renderData.res.getFullQuad().drawShader(renderData.res.getKawaseShader(), "Position");

        currentRadius = 1.5f * radius;
        renderData.res.getKawaseShader().setUniformf("u_offset", currentRadius / blurTargetB.getWidth(), currentRadius / blurTargetB.getHeight());
        renderData.bindFrameBuffer(blurTargetB);
        renderData.setBlendMode(3);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        blurTargetA.getTexture().bind();
        setupFrameBuffer();
        renderData.res.getFullQuad().drawShader(renderData.res.getKawaseShader(), "Position");

        currentRadius = 2.5f * radius;
        renderData.res.getKawaseShader().setUniformf("u_offset", currentRadius / blurTargetA.getWidth(), currentRadius / blurTargetA.getHeight());
        renderData.bindFrameBuffer(blurTargetA);
        renderData.setBlendMode(3);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        blurTargetB.getTexture().bind();
        setupFrameBuffer();
        renderData.res.getFullQuad().drawShader(renderData.res.getKawaseShader(), "Position");

        currentRadius = 3.5f * radius;
        renderData.res.getKawaseShader().setUniformf("u_offset", currentRadius / blurTargetB.getWidth(), currentRadius / blurTargetB.getHeight());
        renderData.bindFrameBuffer(blurTargetB);
        renderData.setBlendMode(3);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        blurTargetA.getTexture().bind();
        setupFrameBuffer();
        renderData.res.getFullQuad().drawShader(renderData.res.getKawaseShader(), "Position");
    }
}
