

package com.aylis.comp.visual.core.Graphic;

import com.aylis.Common.Vec2f;
import com.aylis.Common.Vec2i;
import com.aylis.Common.Vec3f;
import android.graphics.RectF;

public class SpriteFontRenderer {

    private BufferRenderer bufferRenderer;

    public SpriteFontRenderer(BufferRenderer bufferRenderer) {
        this.bufferRenderer = bufferRenderer;
    }

    public Vec2i measureTextBounds(SpriteFont font, String text) {
        SpriteFont.Glyph glyph;

        float posX = 0.0f;
        int i = 0;

        glyph = font.getSpriteDescByChar(text.charAt(i));
        posX += glyph.visualXOffset;
        if (posX < 0.0f) posX = 0.0f;
        i++;
        for (; i < text.length() - 1; i++) {
            glyph = font.getSpriteDescByChar(text.charAt(i));
            posX += glyph.spaceWidth;
        }

        glyph = font.getSpriteDescByChar(text.charAt(i));
        posX += glyph.visualXOffset + glyph.spaceWidth;

        return new Vec2i((int) posX, (int) font.fontMaxHeight());
    }

    public Vec2i measureText(SpriteFont font, String text) {
        SpriteFont.Glyph glyph;
        float posX = 0.0f;

        for (int i = 0; i < text.length(); i++) {
            glyph = font.getSpriteDescByChar(text.charAt(i));
            posX += glyph.spaceWidth;
        }

        return new Vec2i((int) posX, (int) font.fontHeight());
    }

    public int measureTextY(SpriteFont font) {
        return (int) font.fontHeight();
    }

    public void drawText(RenderState renderData, SpriteFont fonts, Vec3f pos, String text, int color) {
        drawText(renderData, fonts, pos, text, color, 0, false, 0, 0, 0, 0);
    }

    public void drawText(RenderState renderData, SpriteFont fonts, Vec3f pos, String text, int color, int textsourceIndex) {
        drawText(renderData, fonts, pos, text, color, textsourceIndex, false, 0, 0, 0, 0);
    }

    public void drawText(RenderState renderData, SpriteFont fonts, Vec3f pos, String text, int color, int textsourceIndex, boolean clipEnabled, int clipX, int clipY, int clipW, int clipH) {
        drawText(renderData, fonts, fonts.getEntryTexture(), pos, text, color, textsourceIndex, clipEnabled, clipX, clipY, clipW, clipH);
    }

    public void drawText(RenderState renderData, SpriteFont fonts, IAtlasTexture tex, Vec3f pos, String text, int color, int textsourceIndex, boolean clipEnabled, int clipX, int clipY, int clipW, int clipH) {
        int len = text.length();
        float x = pos.x;
        float y = pos.y;
        float posZ = 1.0f;

        float clipX2 = clipX + clipW, clipY2 = clipY + clipH;
        float posX;
        float posY;
        float clippedX, clippedY;
        float clippedW, clippedH;

        posX = x;
        posY = y;
        SpriteFont.Glyph glyph;

        if (!clipEnabled) {
            for (int i = textsourceIndex; i < len; i++) {
                char ch = text.charAt(i);
                glyph = fonts.getSpriteDescByChar(ch);

                bufferRenderer.drawRectangleRightBottomWH(
                        renderData,
                        posX + glyph.visualXOffset, (posY + glyph.visualYOffset) - glyph.height, posZ,
                        glyph.width, glyph.height,
                        color,
                        new Vec2f(glyph.x / fonts.textureDim().x, glyph.y / fonts.textureDim().y),
                        new Vec2f((glyph.x + glyph.width) / fonts.textureDim().x, (glyph.y + glyph.height) / fonts.textureDim().y),
                        tex
                );

                posX += glyph.spaceWidth;
            }
        } else {
            for (int i = textsourceIndex; i < len; i++) {
                float posy2 = posY;
                char ch = text.charAt(i);
                glyph = fonts.getSpriteDescByChar(ch);

                clippedX = clipX - posX;
                clippedY = clipY - posY;
                clippedW = (glyph.width + posX) - clipX2;
                clippedH = (glyph.height + posY) - clipY2;

                if (clippedX < 0) clippedX = 0;
                if (clippedY < 0) clippedY = 0;
                if (clippedW < 0) clippedW = 0;
                if (clippedH < 0) clippedH = 0;

                if (clippedX > glyph.width || clippedW > glyph.width || clippedY > glyph.height || clippedH > glyph.height) {
                    posX += glyph.spaceWidth;
                    continue;
                }

                posX += clippedX;
                posy2 += clippedY;
                float glyphX = glyph.x + clippedX;
                float glyphY = glyph.y + clippedY;
                float glyphWidth = glyph.width - clippedW;
                float glyphHeight = glyph.height - clippedH;

                bufferRenderer.drawRectangleRightBottomWH(
                        renderData,
                        posX, posy2, posZ,
                        glyphWidth, glyphHeight,
                        color,
                        new Vec2f(glyphX / fonts.textureDim().x, glyphY / fonts.textureDim().y),
                        new Vec2f((glyphX + glyphWidth) / fonts.textureDim().x, (glyphY + glyphHeight) / fonts.textureDim().y),
                        tex
                );

                posX += glyph.spaceWidth;
            }
        }
    }

    private com.aylis.comp.visual.core.Elements.Base.Transform2D textTransform = new com.aylis.comp.visual.core.Elements.Base.Transform2D();
    private float[] textTransformResult = new float[8];

    public void drawTextRotated(RenderState renderData, SpriteFont fonts, IAtlasTexture tex, RectF drawRect, String text, int color, float rotation, Vec2f rotationCenter) {
        textTransform.reset();
        textTransform.rotate(rotation * 360.0f, rotationCenter.x, rotationCenter.y);

        int len = text.length();
        float posZ = 1.0f;
        float posX = drawRect.left;
        float posY = drawRect.bottom;
        SpriteFont.Glyph glyph;
        RectF glyphRect = new RectF();

        for (int i = 0; i < len; i++) {
            char ch = text.charAt(i);
            glyph = fonts.getSpriteDescByChar(ch);

            float gLeft = posX + glyph.visualXOffset;
            float gTop = (posY + glyph.visualYOffset) - glyph.height;
            float gRight = gLeft + glyph.width;
            float gBottom = gTop + glyph.height;

            glyphRect.set(gLeft, gTop, gRight, gBottom);
            textTransform.mapRectToVertices(glyphRect, textTransformResult);

            bufferRenderer.drawRectangle(
                    renderData,
                    textTransformResult[0], textTransformResult[1],
                    textTransformResult[2], textTransformResult[3],
                    textTransformResult[4], textTransformResult[5],
                    textTransformResult[6], textTransformResult[7],
                    posZ,
                    color,
                    new Vec2f(glyph.x / fonts.textureDim().x, glyph.y / fonts.textureDim().y),
                    new Vec2f((glyph.x + glyph.width) / fonts.textureDim().x, (glyph.y + glyph.height) / fonts.textureDim().y),
                    tex
            );

            posX += glyph.spaceWidth;
        }
    }

    public void drawTextRotatedWipe(RenderState renderData, SpriteFont fonts, IAtlasTexture tex, RectF drawRect, String text, int color1, int color2, float progress, float scale, float rotation, Vec2f rotationCenter) {
        textTransform.reset();
        if (scale != 1.0f) {
            textTransform.scale(scale, scale, rotationCenter.x, rotationCenter.y);
        }
        textTransform.rotate(rotation * 360.0f, rotationCenter.x, rotationCenter.y);

        int len = text.length();
        float posZ = 1.0f;
        float posX = drawRect.left;
        float posY = drawRect.bottom;
        SpriteFont.Glyph glyph;
        RectF glyphRect = new RectF();

        float totalWidth = 0;
        for (int i = 0; i < len; i++) {
            totalWidth += fonts.getSpriteDescByChar(text.charAt(i)).spaceWidth;
        }
        
        float currentX = 0;

        for (int i = 0; i < len; i++) {
            char ch = text.charAt(i);
            glyph = fonts.getSpriteDescByChar(ch);
            
            float charStart = totalWidth > 0 ? currentX / totalWidth : 0f;
            float charEnd = totalWidth > 0 ? (currentX + glyph.spaceWidth) / totalWidth : 0f;
            
            float ratio = 0f;
            if (charEnd > charStart) {
                ratio = (progress - charStart) / (charEnd - charStart);
                if (ratio < 0f) ratio = 0f;
                if (ratio > 1f) ratio = 1f;
            } else {
                ratio = (progress >= charStart) ? 1f : 0f;
            }
            
            int alpha1 = (color1 >> 24) & 0xFF;
            int r1 = (color1 >> 16) & 0xFF;
            int g1 = (color1 >> 8) & 0xFF;
            int b1 = color1 & 0xFF;
            
            int r2 = (color2 >> 16) & 0xFF;
            int g2 = (color2 >> 8) & 0xFF;
            int b2 = color2 & 0xFF;
            
            int cA = alpha1; // Use alpha1 for both to maintain fade out
            int cR = (int)(r1 + (r2 - r1) * ratio);
            int cG = (int)(g1 + (g2 - g1) * ratio);
            int cB = (int)(b1 + (b2 - b1) * ratio);
            
            int c = (cA << 24) | (cR << 16) | (cG << 8) | cB;

            float gLeft = posX + glyph.visualXOffset;
            float gTop = (posY + glyph.visualYOffset) - glyph.height;
            float gRight = gLeft + glyph.width;
            float gBottom = gTop + glyph.height;

            glyphRect.set(gLeft, gTop, gRight, gBottom);
            textTransform.mapRectToVertices(glyphRect, textTransformResult);

            bufferRenderer.drawRectangle(
                    renderData,
                    textTransformResult[0], textTransformResult[1],
                    textTransformResult[2], textTransformResult[3],
                    textTransformResult[4], textTransformResult[5],
                    textTransformResult[6], textTransformResult[7],
                    posZ,
                    c,
                    new Vec2f(glyph.x / fonts.textureDim().x, glyph.y / fonts.textureDim().y),
                    new Vec2f((glyph.x + glyph.width) / fonts.textureDim().x, (glyph.y + glyph.height) / fonts.textureDim().y),
                    tex
            );

            posX += glyph.spaceWidth;
            currentX += glyph.spaceWidth;
        }
    }
}

