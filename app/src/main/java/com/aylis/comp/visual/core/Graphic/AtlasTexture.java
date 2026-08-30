

package com.aylis.comp.visual.core.Graphic;

import com.aylis.Common.tlog;
import com.aylis.comp.visual.core.gl.mdesl.graphics.ITexture;

public class AtlasTexture implements IAtlasTexture {

    static class ErrorTexture implements IAtlasTexture {
        public int getWidth() {
            return 1;
        }

        public int getHeight() {
            return 1;
        }

        public void dispose() {
        }

        public float translateU(float u) {
            return 1.0f;
        }

        public float translateV(float v) {
            return 1.0f;
        }

        public float translateW(float w) {
            return 0.0f;
        }

        public float translateW() {
            return 0.0f;
        }

        public ITexture getTexture2D() {
            return null;
        }

        public IAtlasTexture getSub(int x, int y, int w, int h) {
            return this;
        }

        public IAtlasTexture getSub(float u0, float v0, float uw, float vh) {
            return this;
        }
    }

    public static final IAtlasTexture errorTexture = new ErrorTexture();
    private ITexture texture;

    private int atlasX;
    private int atlasY;
    private int width;
    private int height;
    private int framesCount = 1;

    public AtlasTexture(ITexture texture) {
        this.texture = texture;
        atlasX = 0;
        atlasY = 0;
        width = texture.getWidth();
        height = texture.getHeight();

        if (this.texture.getHeight() < 1 || this.texture.getWidth() < 1)
            tlog.w("texture invalid dimensions");
    }

    public AtlasTexture(ITexture texture, int x, int y, int w, int h) {
        this.texture = texture;

        if (texture.getHeight() < 1 || texture.getWidth() < 1)
            tlog.w("texture invalid dimensions");

        atlasX = x;
        atlasY = y;
        width = w;
        height = h;
    }

    public void dispose() {
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float translateU(float u) {
        float u0 = (float) this.atlasX / (float) texture.getWidth();
        float ud = (float) this.width / (float) texture.getWidth();

        return u0 + (ud * u);
    }

    public float translateV(float v) {
        float v0 = (float) this.atlasY / (float) texture.getHeight();
        float vd = (float) this.height / (float) texture.getHeight();

        return v0 + (vd * v);
    }

    public float translateW(float w) {
        return translateW();
    }

    public float translateW() {
        return 0.0f;
    }

    public ITexture getTexture2D() {
        return texture;
    }

    public IAtlasTexture getSub(int x, int y, int w, int h) {
        w = Math.min(getWidth(), w);
        h = Math.min(getHeight(), h);
        x = Math.min((getWidth() - w), atlasX);
        y = Math.min((getHeight() - h), atlasY);

        return new AtlasTexture(texture, atlasX + x, atlasY + y, w, h);
    }

    public IAtlasTexture getSub(float u0, float v0, float uw, float vh) {
        return new AtlasTexture(texture,
                (int) (atlasX + (getWidth() * u0)),
                (int) (atlasY + (getHeight() * v0)),
                (int) (getWidth() * uw),
                (int) (getHeight() * vh)
        );
    }

    public int getFramesCount() {
        return framesCount;
    }
}

