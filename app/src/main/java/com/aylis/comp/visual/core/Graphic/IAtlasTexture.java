

package com.aylis.comp.visual.core.Graphic;

import com.aylis.comp.visual.core.gl.mdesl.graphics.ITexture;

public interface IAtlasTexture {

    void dispose();

    int getWidth();

    int getHeight();

    float translateU(float u);

    float translateV(float v);

    float translateW(float tw);

    float translateW();

    ITexture getTexture2D();

    IAtlasTexture getSub(int x, int y, int w, int h);

    IAtlasTexture getSub(float u0, float v0, float uw, float vh);

}

