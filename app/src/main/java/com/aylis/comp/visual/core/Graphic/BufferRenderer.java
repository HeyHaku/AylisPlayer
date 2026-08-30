

package com.aylis.comp.visual.core.Graphic;

import android.opengl.GLES20;
import com.aylis.Common.Vec2f;
import com.aylis.Common.tlog;
import junit.framework.Assert;
import com.aylis.comp.visual.core.gl.mdesl.graphics.ITexture;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.ShaderProgram;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.VertexArray;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.VertexAttrib;

public class BufferRenderer {

    private int updateParticleCount = 0;
    private VVertexBuffer vertices;
    private ITexture currentTexture = null;
    private VShaderProgram overrideShader = null;

    private float[] _colorTmp = new float[4];
    private Vertex v1 = new Vertex();
    private Vertex v2 = new Vertex();
    private Vertex v3 = new Vertex();
    private Vertex v0 = new Vertex();
    private Vec2f c0 = new Vec2f(0.0f, 0.0f);
    private Vec2f c1 = new Vec2f(0.0f, 0.0f);
    private Vec2f c2 = new Vec2f(0.0f, 0.0f);
    private Vec2f c3 = new Vec2f(0.0f, 0.0f);
    private Vec2f normal0tmp = new Vec2f(0.0f, 0.0f);
    private Vec2f normal1tmp = new Vec2f(0.0f, 0.0f);
    private Vec2f normal2tmp = new Vec2f(0.0f, 0.0f);
    private Vec2f normal3tmp = new Vec2f(0.0f, 0.0f);
    private int _colorTmpInt;

    public void setOverrideShader(VShaderProgram shader) {
        this.overrideShader = shader;
    }

    public BufferRenderer(ShaderProgram shader, int particlesMinCount) {

        int vertexCount = particlesMinCount * 3 * 2;

        final VertexAttrib[] attributes = new VertexAttrib[]{
                new VertexAttrib(shader.getAttributeLocation("Position"), "Position", 2),
                new VertexAttrib(shader.getAttributeLocation("TexCoord"),  "TexCoord", 2),
                new VertexAttrib(shader.getAttributeLocation("Color"),  "Color", 4),
        };

        for (int i = 0; i < attributes.length; i++) {
            if (attributes[i].location < 0)
                tlog.w("ERROR attribute not found " + attributes[i].name);
        }

        vertices = new VVertexBuffer(vertexCount, attributes);
    }

    public void intColorToF4Color(float[] out, int argb) {
        out[3] = ((argb >> 8 * 3) & 0xFF) / 255.0f;
        out[0] = ((argb >> 8 * 2) & 0xFF) / 255.0f;
        out[1] = ((argb >> 8) & 0xFF) / 255.0f;
        out[2] = ((argb) & 0xFF) / 255.0f;
    }

    public void dispose() {
        vertices.dispose();
    }

    protected boolean checkFlush(RenderState renderData, IAtlasTexture tex, int trianglesNeeded) {
        if (updateStreamRemainingLength() - (8 * 3 * trianglesNeeded) < 0) {
            tlog.w("buffer full");
            flush(renderData);
            currentTexture = tex.getTexture2D();
            return true;
        }

        Assert.assertNotNull(tex);

        if (currentTexture == null && tex.getTexture2D() == null) {
            return true;
        } else if (currentTexture != tex.getTexture2D()) {
            flush(renderData);
            currentTexture = tex.getTexture2D();
            return true;
        }

        return true;
    }

    public void flush(RenderState renderData) {
        if (updateParticleCount > 0) {
            VShaderProgram shader = overrideShader != null ? overrideShader : renderData.res.getAtlasBufferShader();
            shader.use();
            shader.setUniformMatrix("u_projView", false, renderData.getVPMatrix());

            vertices.flip();

            render();

            updateParticleCount = 0;
            vertices.clear();
        }
        currentTexture = null;
    }

    private void render() {
        if (currentTexture != null)
            currentTexture.getTexture().bind();
        else
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        vertices.bind();
        vertices.draw(GLES20.GL_TRIANGLES, 0, updateParticleCount * 3);
        vertices.unbind();
    }

    public void onFrameStart(RenderState renderState) {
        updateParticleCount = 0;
        currentTexture = null;
    }

    public void onFrameEnd(RenderState renderState) {
        flush(renderState);
    }

    public void updateNextParticle(RenderState renderData, Vertex v00, Vertex v10, Vertex v01, Vertex v11, IAtlasTexture tex) {
        if (!checkFlush(renderData, tex, 2)) return;

        updateStreamWrite(v00);
        updateStreamWrite(v01);
        updateStreamWrite(v10);

        updateStreamWrite(v10);
        updateStreamWrite(v01);
        updateStreamWrite(v11);

        updateParticleCount += 2;
    }

    public void drawRectangleRightBottom(RenderState renderData,
                                         float x, float y, float z,
                                         float x2, float y2,
                                         int intcolor,
                                         Vec2f tex0, Vec2f tex1,
                                         IAtlasTexture tex) {
        drawRectangleRightBottomWH(renderData,
                x, y, z,
                x2 - x, y2 - y,
                intcolor,
                tex0, tex1, tex);
    }

    public void drawRectangleRightBottomWH(RenderState renderData,
                                           float x, float y, float z,
                                           float hsizex, float hsizey,
                                           int intcolor,
                                           Vec2f tex0, Vec2f tex1,
                                           IAtlasTexture tex) {
        if (!checkFlush(renderData, tex, 2)) return;

        intColorToF4Color(_colorTmp, intcolor);

        tex0.x = tex.translateU(tex0.x);
        tex0.y = tex.translateV(tex0.y);

        tex1.x = tex.translateU(tex1.x);
        tex1.y = tex.translateV(tex1.y);

        v1.posW = 1.0f;
        v1.texZ = tex.translateW();

        v1.posX = x;
        v1.posY = y + hsizey;
        v1.posZ = z;
        v1.color = _colorTmp;
        v1.texX = tex0.x;
        v1.texY = tex1.y;
        updateStreamWrite(v1);

        v1.posX = x;
        v1.posY = y;
        v1.posZ = z;
            v1.color = _colorTmp;
        v1.texX = tex0.x;
        v1.texY = tex0.y;
        updateStreamWrite(v1);

        v1.posX = x + hsizex;
        v1.posY = y + hsizey;
        v1.posZ = z;
            v1.color = _colorTmp;
        v1.texX = tex1.x;
        v1.texY = tex1.y;
        updateStreamWrite(v1);

        v1.posX = x + hsizex;
        v1.posY = y + hsizey;
        v1.posZ = z;
            v1.color = _colorTmp;
        v1.texX = tex1.x;
        v1.texY = tex1.y;
        updateStreamWrite(v1);

        v1.posX = x;
        v1.posY = y;
        v1.posZ = z;
            v1.color = _colorTmp;
        v1.texX = tex0.x;
        v1.texY = tex0.y;
        updateStreamWrite(v1);

        v1.posX = x + hsizex;
        v1.posY = y;
        v1.posZ = z;
            v1.color = _colorTmp;
        v1.texX = tex1.x;
        v1.texY = tex0.y;
        updateStreamWrite(v1);

        updateParticleCount += 2;
    }

    public void drawRectangle(RenderState renderData,
                              float x0, float y0,
                              float x1, float y1,
                              float x2, float y2,
                              float x3, float y3,
                              float z,
                              int intcolor,
                              Vec2f tex0, Vec2f tex1,
                              IAtlasTexture tex) {
        if (!checkFlush(renderData, tex, 2)) return;

        intColorToF4Color(_colorTmp, intcolor);

        tex0.x = tex.translateU(tex0.x);
        tex0.y = tex.translateV(tex0.y);

        tex1.x = tex.translateU(tex1.x);
        tex1.y = tex.translateV(tex1.y);

        v1.posW = 1.0f;
        v1.texZ = tex.translateW();

        v1.posX = x2;
        v1.posY = y2;
        v1.posZ = z;
        v1.color = _colorTmp;
        v1.texX = tex0.x;
        v1.texY = tex1.y;
        updateStreamWrite(v1);

        v1.posX = x0;
        v1.posY = y0;
        v1.posZ = z;
        v1.color = _colorTmp;
        v1.texX = tex0.x;
        v1.texY = tex0.y;
        updateStreamWrite(v1);

        v1.posX = x3;
        v1.posY = y3;
        v1.posZ = z;
            v1.color = _colorTmp;
        v1.texX = tex1.x;
        v1.texY = tex1.y;
        updateStreamWrite(v1);

        v1.posX = x3;
        v1.posY = y3;
        v1.posZ = z;
            v1.color = _colorTmp;
        v1.texX = tex1.x;
        v1.texY = tex1.y;
        updateStreamWrite(v1);

        v1.posX = x0;
        v1.posY = y0;
        v1.posZ = z;
            v1.color = _colorTmp;
        v1.texX = tex0.x;
        v1.texY = tex0.y;
        updateStreamWrite(v1);

        v1.posX = x1;
        v1.posY = y1;
        v1.posZ = z;
            v1.color = _colorTmp;
        v1.texX = tex1.x;
        v1.texY = tex0.y;
        updateStreamWrite(v1);

        updateParticleCount += 2;
    }

    public void drawRectangle(RenderState renderData,
                              float x0, float y0,
                              float x1, float y1,
                              float x2, float y2,
                              float x3, float y3,
                              float z,
                              int intcolor,
                              Vec2f tex0, Vec2f tex1,
                              IAtlasTexture tex,
                              int blendMode) {
        drawRectangle(renderData, x0, y0, x1, y1, x2, y2, x3, y3, z, intcolor, tex0, tex1, tex);
    }

    public void drawRectangleEdges(RenderState renderData,
                                   float x0, float y0,
                                   float x1, float y1,
                                   float x2, float y2,
                                   float x3, float y3,
                                   float z,
                                   int intcolor,
                                   Vec2f tex0, Vec2f tex1,
                                   IAtlasTexture tex,
                                   int blendMode,
                                   Object unused) {
        drawRectangle(renderData, x0, y0, x1, y1, x2, y2, x3, y3, z, intcolor, tex0, tex1, tex);
    }

    public void drawRectangle2Patch(RenderState renderData,
                                    float x0, float y0,
                                    float x1, float y1,
                                    float x2, float y2,
                                    float x3, float y3,
                                    float z,
                                    int intcolor,
                                    Vec2f tex0, Vec2f tex1,
                                    Object renderPassData) {
        drawRectangle(renderData, x0, y0, x1, y1, x2, y2, x3, y3, z, intcolor, tex0, tex1, renderData.res.getAtlasTexWhite());
    }

    public void drawCircleSegmentW(RenderState renderData,
                                   float x, float y, float z,
                                   float hsizex, float hsizey,
                                   int intcolor,
                                   Vec2f tex0, Vec2f tex1,
                                   IAtlasTexture tex,
                                   float segmentW) {
        float drawRadius;
        if (hsizex < hsizey)
            drawRadius = hsizex * 0.5f;
        else
            drawRadius = hsizey * 0.5f;

        float circumference = (float) (2.0 * Math.PI * drawRadius);

        int num = (int) ((circumference / segmentW) + 0.5f);

        int num_segments = Math.max(num, 18);

        drawCircle(renderData, x, y, z, hsizex, hsizey,
                intcolor,
                tex0, tex1,
                tex, num_segments);
    }

    public void drawCircle(RenderState renderData,
                           float x, float y, float z,
                           float halfSizeX, float halfSizeY,
                           int intColor,
                           Vec2f tex0, Vec2f tex1,
                           IAtlasTexture tex,
                           int numSegments) {
        drawCircle(renderData, x, y, z, halfSizeX, halfSizeY, intColor, tex0, tex1, tex, numSegments, 0.0f);
    }

    public void drawCircle(RenderState renderData,
                           float x, float y, float z,
                           float halfSizeX, float halfSizeY,
                           int intColor,
                           Vec2f tex0, Vec2f tex1,
                           IAtlasTexture tex,
                           int numSegments,
                           float rotation) {
        if (!checkFlush(renderData, tex, numSegments)) return;

        x += halfSizeX * 0.5f;
        y += halfSizeY * 0.5f;

        intColorToF4Color(_colorTmp, intColor);

        float r = 0.5f;

        double theta = 2.0 * Math.PI / (double) (numSegments);
        double tangential_factor = Math.tan(theta);

        double radial_factor = Math.cos(theta);

        double startAngle = rotation * 2.0 * Math.PI;
        float cx = (float) (r * Math.sin(startAngle));
        float cy = -(float) (r * Math.cos(startAngle));

        float lastX = cx;
        float lastY = cy;

        tex0.x = tex.translateU(tex0.x);
        tex0.y = tex.translateV(tex0.y);

        tex1.x = tex.translateU(tex1.x);
        tex1.y = tex.translateV(tex1.y);

        float texMidX = (tex0.x + tex1.x) * 0.5f;
        float texMidY = (tex0.y + tex1.y) * 0.5f;
        float texWhalf = (tex1.x - tex0.x);
        float texHhalf = (tex1.y - tex0.y);

        for (int ii = 0; ii < numSegments; ii++) {

            float tx = -cy;
            float ty = cx;

            cx += tx * tangential_factor;
            cy += ty * tangential_factor;

            cx *= radial_factor;
            cy *= radial_factor;

            v1.posW = 1.0f;
            v1.texZ = tex.translateW();

            v1.posX = x + (lastX * halfSizeX);
            v1.posY = y + (lastY * halfSizeY);
            v1.posZ = z;
            v1.color = _colorTmp;
            v1.texX = texMidX + lastX * texWhalf;
            v1.texY = texMidX + lastY * texHhalf;
            updateStreamWrite(v1);

            v1.posX = x;
            v1.posY = y;
            v1.posZ = z;
            v1.color = _colorTmp;
            v1.texX = texMidX;
            v1.texY = texMidY;
            updateStreamWrite(v1);

            v1.posX = x + (cx * halfSizeX);
            v1.posY = y + (cy * halfSizeY);
            v1.posZ = z;
            v1.color = _colorTmp;
            v1.texX = texMidX + cx * texWhalf;
            v1.texY = texMidX + cy * texHhalf;
            updateStreamWrite(v1);

            updateParticleCount += 1;

            lastX = cx;
            lastY = cy;
        }
    }

    private int updateStreamRemainingLength() {
        return vertices.remaining();
    }

    private void updateStreamWrite(Vertex v1) {
        v1.writeToStream(vertices);
    }

    public static class Vertex {

        public static final int Size = 8 + 8 + 16;

        public float posX, posY, posZ, posW;
        public float texX, texY, texZ;
        public float[] color;

        public Vertex() {
        }

        public void writeToStream(VertexArray vertices) {
            vertices.put(posX);
            vertices.put(posY);
            vertices.put(texX);
            vertices.put(texY);
            vertices.put(color[0]);
            vertices.put(color[1]);
            vertices.put(color[2]);
            vertices.put(color[3]);
        }
    }
}

