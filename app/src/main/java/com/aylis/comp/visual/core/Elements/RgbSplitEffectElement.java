package com.aylis.comp.visual.core.Elements;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.opengl.GLES20;

import com.aylis.Common.Vec2f;
import com.aylis.comp.AlbumArt.AlbumArtRequest;
import com.aylis.comp.AlbumArt.ImageLoadedListener;
import com.aylis.comp.visual.core.Elements.Base.MVariableFloat;
import com.aylis.comp.visual.core.Elements.Base.MeasureDefs;
import com.aylis.comp.visual.core.Elements.AppBlendMode;
import com.aylis.comp.visual.core.Graphic.AtlasTexture;
import com.aylis.comp.visual.core.Graphic.GraphicsUtils;
import com.aylis.comp.visual.core.Graphic.RenderState;
import com.aylis.comp.visual.core.Graphic.VShaderProgram;
import com.aylis.comp.visual.core.gl.mdesl.graphics.Texture;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.FrameBuffer;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.ShaderProgram;

public class RgbSplitEffectElement extends Element implements ImageLoadedListener {
    public static final String[] internalImages = { "composition:0" };
    public static final String typeName = "RgbSplitEffect";
    public static final String PRECOMP_PREFIX = "precomp:";

    private int blendModeContent = 2;
    private int color2 = -1;
    private boolean renderContent = false;
    private boolean renderContentUnder = false;
    private MVariableFloat splitMultiplierVar = MVariableFloat.Companion.createConstantFloat(6.0f);
    private MVariableFloat splitAmountX = MVariableFloat.Companion.createConstantFloat(0.5f);
    private MVariableFloat splitAmountY = MVariableFloat.Companion.createConstantFloat(0.5f);
    private final float[] splitColor0 = { 1.0f, 0.0f, 0.0f, 1.0f };
    private final float[] splitColor1 = { 0.0f, 1.0f, 0.0f, 1.0f };
    private final float[] splitColor2 = { 0.0f, 0.0f, 1.0f, 1.0f };
    private final Vec2f[] blurLayerScales = new Vec2f[3];

    private String customImagePath = "";
    private AlbumArtRequest albumArtRequest = new AlbumArtRequest("", "", "", "");
    private Bitmap bitmap = null;
    private boolean bitmapLoading = false;
    private boolean bitmapLoadedIn = false;
    private Object imageLoadStrongReference;
    private Texture tex2 = null;
    private AtlasTexture atlasTex2 = null;

    public RgbSplitEffectElement() {
        super();
        useAnimatorMeasures = false;
        setScale(1.0f, 1.0f);
        setBlendMode(4);
        blurLayerScales[0] = new Vec2f(1.0f, 1.0f);
        blurLayerScales[1] = new Vec2f(0.0f, 0.0f);
        blurLayerScales[2] = new Vec2f(0.0f, 0.0f);
        setCustomImagePath("precomp:1");
    }

    public String getSelectedPreCompName() {
        if (customImagePath != null && customImagePath.startsWith(PRECOMP_PREFIX)) {
            return customImagePath.substring(PRECOMP_PREFIX.length());
        }
        return null;
    }

    public void setCustomImagePath(String path) {
        if (path == null)
            path = "";
        if (this.customImagePath.equals(path))
            return;
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

    private void onAlbumArtCreateGLResources(Bitmap bitmap) {
        if (tex2 != null)
            tex2.dispose();
        tex2 = null;
        atlasTex2 = null;
        if (bitmap == null)
            return;

        tex2 = new com.aylis.comp.visual.core.Graphic.VTexture(
                bitmap,
                com.aylis.comp.visual.core.Graphic.VTexture.DEFAULT_FILTER,
                com.aylis.comp.visual.core.Graphic.VTexture.DEFAULT_FILTER,
                com.aylis.comp.visual.core.Graphic.VTexture.DEFAULT_WRAP,
                false);

        atlasTex2 = new AtlasTexture(tex2);
    }

    private void setColor2(int i) {
        color2 = i;
    }

    private void setSplitMultiplier(MVariableFloat mVariableFloat) {
        splitMultiplierVar = mVariableFloat;
    }

    private void setSplitAmountX(MVariableFloat mVariableFloat) {
        splitAmountX = mVariableFloat;
    }

    private void setSplitAmountY(MVariableFloat mVariableFloat) {
        splitAmountY = mVariableFloat;
    }

    private void setSplitColor0(int i) {
        GraphicsUtils.intColorToF4Color(splitColor0, i);
    }

    private void setSplitColor1(int i) {
        GraphicsUtils.intColorToF4Color(splitColor1, i);
    }

    private void setSplitColor2(int i) {
        GraphicsUtils.intColorToF4Color(splitColor2, i);
    }

    private int getSplitColor0() {
        return GraphicsUtils.f4ColorToIntColor(splitColor0);
    }

    private int getSplitColor1() {
        return GraphicsUtils.f4ColorToIntColor(splitColor1);
    }

    private int getSplitColor2() {
        return GraphicsUtils.f4ColorToIntColor(splitColor2);
    }

    private void setRenderContentOnTop(boolean z) {
        renderContent = z;
    }

    private boolean getRenderContent() {
        return renderContent;
    }

    private void setRenderContentUnder(boolean z) {
        renderContentUnder = z;
    }

    private boolean getRenderContentUnder() {
        return renderContentUnder;
    }

    private void setBlendModeContent(int i) {
        blendModeContent = i;
    }

    @Override
    public void onApplyCustomization(CustomizationData customizationData) {
        super.onApplyCustomization(customizationData);

        String bm = customizationData.getPropertyString("blendModeContent", AppBlendMode.getTypeName(blendModeContent));
        if (bm != null && !bm.isEmpty()) {
            setBlendModeContent(AppBlendMode.getGlMode(bm));
        }

        setColor2(customizationData.getPropertyInt("color", -1));

        String imagePath = customizationData.getPropertyString("targetImage", "");
        CustomizationData preCompChild = customizationData.getChild("preCompSelection");
        if (preCompChild != null) {
            String legacyPreComp = preCompChild.getChildTypeValue();
            if (legacyPreComp != null && !legacyPreComp.isEmpty() && !"None".equals(legacyPreComp)) {
                if (imagePath.isEmpty() || !imagePath.startsWith(PRECOMP_PREFIX)) {
                    imagePath = PRECOMP_PREFIX + legacyPreComp;
                }
            }
        }
        if (imagePath.isEmpty())
            imagePath = "precomp:1";
        setCustomImagePath(imagePath);

        setRenderContentOnTop(customizationData.getPropertyBool("showUnblurredContent", false));
        setRenderContentUnder(customizationData.getPropertyBool("showUnblurredContentUnder", false));
        setSplitMultiplier(customizationData.getPropertyMVariableFloat("splitMultiplier", splitMultiplierVar));
        setSplitAmountX(customizationData.getPropertyMVariableFloat("splitAmountX", splitAmountX));
        setSplitAmountY(customizationData.getPropertyMVariableFloat("splitAmountY", splitAmountY));
        setSplitColor0(customizationData.getPropertyInt("splitColor0", 0xffff0000));
        setSplitColor1(customizationData.getPropertyInt("splitColor1", 0xff00ff00));
        setSplitColor2(customizationData.getPropertyInt("splitColor2", 0xff0000ff));

        blurLayerScales[0] = new Vec2f(1.0f, 1.0f);
    }

    @Override
    public void onReadCustomization(CustomizationData outCustomizationData) {
        super.onReadCustomization(outCustomizationData);

        outCustomizationData.setCustomizationName("Rgb Split");
        outCustomizationData.putPropertyString("blendModeContent", AppBlendMode.getTypeName(blendModeContent),
                AppBlendMode.getSelectorString(), "1_appearance");
        outCustomizationData.putPropertyInt("color", color2, "crgb", "1_appearance");
        outCustomizationData.putPropertyString("targetImage", customImagePath, "img", "1_appearance");
        outCustomizationData.putPropertyBool("showUnblurredContent", getRenderContent(), "1_appearance");
        outCustomizationData.putPropertyBool("showUnblurredContentUnder", getRenderContentUnder(), "1_appearance");
        outCustomizationData.putPropertyMVariableFloat("splitMultiplier", splitMultiplierVar, "2_splitEffect", 0.0f,
                6.0f);
        outCustomizationData.putPropertyMVariableFloat("splitAmountX", splitAmountX, "2_splitEffect", -0.5f, 1.0f);
        outCustomizationData.putPropertyMVariableFloat("splitAmountY", splitAmountY, "2_splitEffect", -0.5f, 1.0f);
        outCustomizationData.putPropertyInt("splitColor0", getSplitColor0(), "crgba", "2_splitEffect");
        outCustomizationData.putPropertyInt("splitColor1", getSplitColor1(), "crgba", "2_splitEffect");
        outCustomizationData.putPropertyInt("splitColor2", getSplitColor2(), "crgba", "2_splitEffect");
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
        super.onCreateGLResources(renderData);
    }

    @Override
    public void onRender(RenderState renderState, FrameBuffer frameBuffer) {
        onRenderCheckResources(renderState);

        Texture targetTex = null;
        AtlasTexture targetAtlasTex = null;

        String preCompName = getSelectedPreCompName();
        if (preCompName != null) {
            PreCompElement targetPreComp = PreCompManager.get(preCompName);
            if (targetPreComp != null) {
                targetPreComp.renderOnDemand(renderState, frameBuffer);
                targetTex = targetPreComp.getTexture();
                if (targetTex != null) {
                    targetAtlasTex = new AtlasTexture(targetTex);
                }
            }
        } else {
            targetTex = tex2;
            targetAtlasTex = atlasTex2;
        }

        if (targetAtlasTex == null || targetTex == null) {
            super.onRender(renderState, frameBuffer);
            return;
        }

        RectF rectFMeasureDrawRect = measureDrawRect(renderState.res.meter);
        super.onRender(renderState, frameBuffer);

        if (renderContentUnder) {
            renderState.setBlendMode(blendModeContent);
            renderState.drawFullscreenQuad(-1, targetTex);
        }

        for (int i = blurLayerScales.length - 1; i >= 0; i--) {
            Vec2f scale = blurLayerScales[i];
            if (scale.x != 0.0f && scale.y != 0.0f) {
                float f = (1.0f / scale.x) * 0.5f;
                float f2 = (1.0f / scale.y) * 0.5f;

                VShaderProgram shader = renderState.res.getRgbSplitShader();
                if (shader != null) {
                    renderState.bindShader(shader);
                    float valueAsFloat = splitMultiplierVar.getValueAsFloat(renderState.res.meter, 0f);
                    float dirX = splitAmountX.getValueAsFloat(renderState.res.meter, 0f);
                    float dirY = splitAmountY.getValueAsFloat(renderState.res.meter, 0f);

                    renderState.res.getBufferRenderer().setOverrideShader(shader);

                    float uniformX = (dirX * valueAsFloat) * 0.05f;
                    float uniformY = (dirY * valueAsFloat) * 0.05f;

                    shader.setUniformf("dirAmount", uniformX, uniformY);
                    shader.setUniformf("splitColor0", splitColor0[0], splitColor0[1], splitColor0[2], splitColor0[3]);
                    shader.setUniformf("splitColor1", splitColor1[0], splitColor1[1], splitColor1[2], splitColor1[3]);
                    shader.setUniformf("splitColor2", splitColor2[0], splitColor2[1], splitColor2[2], splitColor2[3]);

                    renderState.setBlendMode(getBlendMode());
                    targetTex.bind();

                    Vec2f tex0 = new Vec2f(0.5f - f, 0.5f - f2);
                    Vec2f tex1 = new Vec2f(f + 0.5f, f2 + 0.5f);
                    renderState.drawFullscreenQuad(color2, targetTex, tex0, tex1);
                    renderState.res.getBufferRenderer().flush(renderState);
                    renderState.res.getBufferRenderer().setOverrideShader(null);
                }
            }
        }

        if (renderContent) {
            renderState.setBlendMode(blendModeContent);
            renderState.drawFullscreenQuad(-1, targetTex);
        }
    }
}
