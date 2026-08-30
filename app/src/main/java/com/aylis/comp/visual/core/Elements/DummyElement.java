
package com.aylis.comp.visual.core.Elements;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.opengl.GLES20;
import com.aylis.Common.Utils;
import com.aylis.Common.Vec2f;
import com.aylis.Common.tlog;
import com.aylis.comp.AlbumArt.AlbumArtRequest;
import com.aylis.comp.AlbumArt.ImageLoadedListener;
import com.aylis.comp.visual.core.Graphic.AtlasTexture;
import com.aylis.comp.visual.core.Graphic.RenderState;
import com.aylis.comp.visual.core.Graphic.VShaderProgram;
import com.aylis.comp.visual.core.Graphic.VTexture;
import com.aylis.comp.visual.core.gl.mdesl.graphics.ITexture;
import com.aylis.comp.visual.core.gl.mdesl.graphics.Texture;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.FrameBuffer;
import com.aylis.comp.visual.core.Graphic.SafeMipmapHelper;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class DummyElement extends Element implements ImageLoadedListener {

    public static final String typeName = "DummyElement";
    public static final String PRECOMP_PREFIX = "precomp:";

    public static final String DEFAULT_VERT_SHADER = "uniform mat4 u_projView;\n" +
            "attribute vec3 Position;\n" +
            "attribute vec2 TexCoord;\n" +
            "attribute vec4 Color;\n" +
            "varying vec4 vColor;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "    vColor = Color;\n" +
            "    vTexCoord = TexCoord;\n" +
            "    gl_Position = u_projView * vec4(Position, 1.0);\n" +
            "}";

    public static final String DEFAULT_FRAG_SHADER = "precision mediump float;\n" +
            "varying vec2 vTexCoord;\n" +
            "uniform sampler2D u_texture;\n" +
            "uniform float u_value1;\n" +
            "uniform float u_value2;\n" +
            "vec2 GLCoord2TextureCoord(vec2 glCoord) {\n" +
            "    return glCoord * vec2(1.0, -1.0) / 2.0 + vec2(0.5, 0.5);\n" +
            "}\n" +
            "void main() {\n" +
            "    vec2 vPosition = (vTexCoord - vec2(0.5, 0.5));\n" +
            "    vPosition.y *= -1.0;\n" +
            "    float b = 1280.0 / 720.0;\n" +
            "    float scale = u_value1 / 2.0;\n" +
            "    float _A = 2.0;\n" +
            "    float _B = 4.0 - b;\n" +
            "    float _F = u_value2 / 2.0;\n" +
            "    float L = length(vec3(vPosition.xy / scale, _F));\n" +
            "    vec2 vMapping = vPosition.xy * _F / L;\n" +
            "    vMapping = vMapping * vec2(_A, _B);\n" +
            "    vMapping = GLCoord2TextureCoord(vMapping / scale);\n" +
            "    vec4 textureColor = texture2D(u_texture, vMapping);\n" +
            "    if (vMapping.x > 0.99 || vMapping.x < 0.01 || vMapping.y > 0.99 || vMapping.y < 0.01) {\n" +
            "        textureColor = vec4(0.0, 0.0, 0.0, 1.0);\n" +
            "    }\n" +
            "    gl_FragColor = textureColor;\n" +
            "}";

    private String customImagePath = "";
    private AlbumArtRequest albumArtRequest = new AlbumArtRequest("", "", "", "");
    private Bitmap bitmap = null;
    private boolean bitmapLoading = false;
    private boolean bitmapLoadedIn = false;
    private Object imageLoadStrongReference;

    private Texture tex2 = null;
    private AtlasTexture atlasTex2 = null;

    private VShaderProgram loadedShader;
    private boolean reloadShader = true;
    public String shaderVert;
    public String shaderFrag;

    public LinkedHashMap<String, com.aylis.comp.visual.core.Elements.Base.MVariableFloat> u_values;
    public HashMap<String, float[]> valueProperties;
    public HashMap<String, String> valueCategories;

    public DummyElement() {
        super();
        this.u_values = new LinkedHashMap<>();
        this.valueProperties = new HashMap<>();
        this.valueCategories = new HashMap<>();

        initCustomShader();
        initCustomValues();

        setBlendMode(4);
        setScale(1.0f, 1.0f);
        setCustomImagePath("composition:1");
    }

    public void initCustomShader() {
        this.shaderVert = DEFAULT_VERT_SHADER;
        this.shaderFrag = DEFAULT_FRAG_SHADER;
    }

    public boolean isShaderEditable() {
        return true;
    }

    public void addValueWithProperties(String propertyName, float defaultValue, float minValue, float maxValue) {
        addValueWithPropertiesCat(propertyName, defaultValue, minValue, maxValue, "2_motion");
    }

    public void addValueWithPropertiesCat(String propertyName, float defaultValue, float minValue, float maxValue,
            String category) {
        this.u_values.put(propertyName,
                com.aylis.comp.visual.core.Elements.Base.MVariableFloat.Companion.createConstantFloat(defaultValue));
        this.valueProperties.put(propertyName, new float[] { defaultValue, minValue, maxValue });
        this.valueCategories.put(propertyName, category != null ? category : "2_motion");
    }

    public void initCustomValues() {
        addValueWithProperties("value1", 0.0f, -1.0f, 1.0f);
        addValueWithProperties("value2", 0.0f, -1.0f, 1.0f);
        addValueWithProperties("value3", 0.0f, -1.0f, 1.0f);
        addValueWithProperties("value4", 0.0f, -1.0f, 1.0f);
        addValueWithProperties("value5", 0.0f, -1.0f, 1.0f);
        addValueWithProperties("value6", 0.0f, -1.0f, 1.0f);
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
        if (Utils.compareNullStrings(dataSource, albumArtRequest.videoThumbDataSource)) {
            if (Utils.compareNullStrings(url0, albumArtRequest.path0)) {
                if (Utils.compareNullStrings(url1, albumArtRequest.path1)) {
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

    @Override
    protected void markNeedReCreateGLResources() {
        bitmap = null;
        bitmapLoading = false;
        bitmapLoadedIn = false;
        this.reloadShader = true;
        super.markNeedReCreateGLResources();
    }

    @Override
    public void reCreateGLResources(RenderState renderData) {
        this.markNeedReCreateGLResources();
        super.reCreateGLResources(renderData);
    }

    @Override
    public void onCreateGLResources(RenderState renderData) {
        super.onCreateGLResources(renderData);

        if (customImagePath != null && customImagePath.startsWith(PRECOMP_PREFIX)) {
            tex2 = null;
            atlasTex2 = null;
            bitmap = null;
            return;
        }

        if (customImagePath != null && !customImagePath.isEmpty()) {
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
    public void onApplyCustomization(CustomizationData customizationData) {
        super.onApplyCustomization(customizationData);

        setCustomImagePath(customizationData.getPropertyString("customImage", customImagePath));
        if (isShaderEditable()) {
            this.shaderVert = customizationData.getPropertyString("shaderVertex", this.shaderVert);
            this.shaderFrag = customizationData.getPropertyString("shaderFragment", this.shaderFrag);
        }

        for (String key : u_values.keySet()) {
            com.aylis.comp.visual.core.Elements.Base.MVariableFloat val = customizationData
                    .getPropertyMVariableFloat(key, u_values.get(key));
            u_values.put(key, val);
        }

        this.reloadShader = true;
    }

    public String getElementTypeName() {
        return typeName;
    }

    @Override
    public void onReadCustomization(CustomizationData outCustomizationData) {
        super.onReadCustomization(outCustomizationData);
        outCustomizationData.setCustomizationName(getElementTypeName());

        outCustomizationData.putPropertyString("customImage", customImagePath, "img", "3_shader");
        if (isShaderEditable()) {
            outCustomizationData.putPropertyString("shaderVertex", this.shaderVert, "shader_code", "3_shader");
            outCustomizationData.putPropertyString("shaderFragment", this.shaderFrag, "shader_code", "3_shader");
        }

        for (String key : u_values.keySet()) {
            com.aylis.comp.visual.core.Elements.Base.MVariableFloat variable = u_values.get(key);
            if (variable != null) {
                float[] prop = valueProperties.get(key);
                float minVal = (prop != null) ? prop[1] : -1.0f;
                float maxVal = (prop != null) ? prop[2] : 1.0f;
                String group = valueCategories.get(key);
                if (group == null)
                    group = "0_general";

                outCustomizationData.putPropertyMVariableFloat(key, variable, group, minVal, maxVal);
            }
        }
    }

    @Override
    public void onRender(RenderState renderData, FrameBuffer resultFB) {
        Texture targetTex = null;
        AtlasTexture targetAtlasTex = null;

        String preCompName = getSelectedPreCompName();
        if (preCompName != null) {
            PreCompElement targetPreComp = PreCompManager.get(preCompName);
            if (targetPreComp != null) {
                targetPreComp.renderOnDemand(renderData, resultFB);
                Texture preCompTex = targetPreComp.getTexture();
                if (SafeMipmapHelper.isTextureReady(preCompTex)) {
                    targetTex = preCompTex;
                    targetAtlasTex = new AtlasTexture(targetTex);
                } else {
                    // FBO пре-композиции ещё не готов — используем заглушку
                    targetAtlasTex = renderData.res.getAtlasTexBlack();
                    if (targetAtlasTex != null && targetAtlasTex.getTexture2D() != null) {
                        targetTex = targetAtlasTex.getTexture2D().getTexture();
                    }
                }
            }
        } else {
            targetTex = tex2;
            targetAtlasTex = atlasTex2;
        }

        onRenderCheckResources(renderData);

        if (this.loadedShader == null || this.reloadShader) {
            this.reloadShader = false;
            if (this.loadedShader != null) {
                this.loadedShader.dispose();
                this.loadedShader = null;
            }
            try {
                this.loadedShader = new VShaderProgram(this.shaderVert, this.shaderFrag);
                if (this.loadedShader.getLog().length() != 0) {
                    tlog.w(this.loadedShader.getLog());
                }
            } catch (Exception e) {
                tlog.w("Failed to compile custom shader: " + e.getMessage());
            }
        }

        if (this.loadedShader == null) {
            super.onRender(renderData, resultFB);
            return;
        }

        renderData.bindShader(loadedShader);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        if (targetTex != null) {
            targetTex.bind();
        } else if (targetAtlasTex != null && targetAtlasTex.getTexture2D() != null
                && targetAtlasTex.getTexture2D().getTexture() != null) {
            targetAtlasTex.getTexture2D().getTexture().bind();
        } else {
            renderData.res.getAtlasTexWhite().getTexture2D().getTexture().bind();
        }

        int uTextureLoc = loadedShader.getUniformLocation("u_texture");
        if (uTextureLoc >= 0) {
            GLES20.glUniform1i(uTextureLoc, 0);
        }

        int uProjViewLoc = loadedShader.getUniformLocation("u_projView");
        if (uProjViewLoc >= 0) {
            loadedShader.setUniformMatrix(uProjViewLoc, false, renderData.getVPMatrix());
        }

        Meter meter = renderData.res.meter;
        for (String key : u_values.keySet()) {
            com.aylis.comp.visual.core.Elements.Base.MVariableFloat var = u_values.get(key);
            if (var != null) {
                int loc = loadedShader.getUniformLocation("u_" + key);
                if (loc >= 0) {
                    GLES20.glUniform1f(loc, var.getValueAsFloat(meter, 0f));
                }
            }
        }

        RectF drawRect = measureDrawRect(renderData.res.meter);
        float x1 = drawRect.left;
        float y1 = drawRect.top;
        float x2 = drawRect.right;
        float y2 = drawRect.bottom;

        float[] colorF = new float[4];
        int color = 0xffffffff;
        colorF[3] = ((color >> 24) & 0xFF) / 255.0f;
        colorF[0] = ((color >> 16) & 0xFF) / 255.0f;
        colorF[1] = ((color >> 8) & 0xFF) / 255.0f;
        colorF[2] = (color & 0xFF) / 255.0f;

        float z = 0.0f;
        float[] vertices = {
                x1, y2, z,
                x1, y1, z,
                x2, y2, z,
                x2, y1, z
        };

        float rotation = measureDrawRot(renderData.res.meter);
        if (rotation != 0.0f) {
            float cx = drawRect.centerX();
            float cy = drawRect.centerY();
            float rad = rotation * 2.0f * (float) Math.PI;
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            for (int i = 0; i < vertices.length; i += 3) {
                float vx = vertices[i] - cx;
                float vy = vertices[i + 1] - cy;
                vertices[i] = cx + (vx * cos - vy * sin);
                vertices[i + 1] = cy + (vx * sin + vy * cos);
            }
        }

        boolean isPreComp = getSelectedPreCompName() != null;
        float tu1 = 0.0f, tv1 = isPreComp ? 1.0f : 0.0f, tu2 = 1.0f, tv2 = isPreComp ? 0.0f : 1.0f;
        if (targetAtlasTex != null) {
            tu1 = targetAtlasTex.translateU(tu1);
            tv1 = targetAtlasTex.translateV(tv1);
            tu2 = targetAtlasTex.translateU(tu2);
            tv2 = targetAtlasTex.translateV(tv2);
        }
        float[] texCoords = {
                tu1, tv2,
                tu1, tv1,
                tu2, tv2,
                tu2, tv1
        };

        float[] colors = {
                colorF[0], colorF[1], colorF[2], colorF[3],
                colorF[0], colorF[1], colorF[2], colorF[3],
                colorF[0], colorF[1], colorF[2], colorF[3],
                colorF[0], colorF[1], colorF[2], colorF[3]
        };

        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        FloatBuffer vertexBuf = vbb.asFloatBuffer();
        vertexBuf.put(vertices);
        vertexBuf.position(0);

        ByteBuffer tbb = ByteBuffer.allocateDirect(texCoords.length * 4);
        tbb.order(ByteOrder.nativeOrder());
        FloatBuffer texCoordBuf = tbb.asFloatBuffer();
        texCoordBuf.put(texCoords);
        texCoordBuf.position(0);

        ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
        cbb.order(ByteOrder.nativeOrder());
        FloatBuffer colorBuf = cbb.asFloatBuffer();
        colorBuf.put(colors);
        colorBuf.position(0);

        int posLoc = loadedShader.getAttributeLocation("Position");
        int texLoc = loadedShader.getAttributeLocation("TexCoord");
        int colLoc = loadedShader.getAttributeLocation("Color");

        if (posLoc >= 0) {
            GLES20.glEnableVertexAttribArray(posLoc);
            GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuf);
        }
        if (texLoc >= 0) {
            GLES20.glEnableVertexAttribArray(texLoc);
            GLES20.glVertexAttribPointer(texLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuf);
        }
        if (colLoc >= 0) {
            GLES20.glEnableVertexAttribArray(colLoc);
            GLES20.glVertexAttribPointer(colLoc, 4, GLES20.GL_FLOAT, false, 0, colorBuf);
        }

        renderData.bindFrameBuffer(resultFB);
        renderData.setBlendMode(getBlendMode());

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        if (posLoc >= 0)
            GLES20.glDisableVertexAttribArray(posLoc);
        if (texLoc >= 0)
            GLES20.glDisableVertexAttribArray(texLoc);
        if (colLoc >= 0)
            GLES20.glDisableVertexAttribArray(colLoc);

        drawHighlightRecursive(renderData);
    }
}
