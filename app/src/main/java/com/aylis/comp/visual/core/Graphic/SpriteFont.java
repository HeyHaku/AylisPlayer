

package com.aylis.comp.visual.core.Graphic;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import com.aylis.Common.Utils;
import com.aylis.Common.Vec2f;
import com.aylis.Common.Vec2i;
import com.aylis.Common.tlog;
import com.aylis.comp.visual.core.gl.mdesl.graphics.ITexture;

public class SpriteFont {

    private CharSet charSet;
    private VTexture texture;
    private AtlasTexture atlasTexture;
    private Glyph[] glyphs;
    private Vec2f textureDim;
    private int fontMetricHeight;
    private int fontMetricMaxHeight;
    private boolean valid = true;

    public SpriteFont(Typeface textFont, int textFontSize, CharSet charSet) {
        this.charSet = charSet;

        try {
            init(textFont, textFontSize);
        } catch (Exception e) {
            tlog.e("failed to create SpriteFont: " + e.getMessage());
            valid = false;
        }
        if (isValid()) return;

        try {
            init(textFont, textFontSize / 2);
        } catch (Exception e) {
            tlog.e("failed to create SpriteFont: " + e.getMessage());
            valid = false;
        }

        if (isValid()) return;

        tlog.e("failed to create SpriteFont");
    }

    public SpriteFont(String textureFilePath, int fontHeight, Rect[] charRegions, CharSet charSet) {
        this.charSet = charSet;

        fontMetricHeight = fontHeight;
        glyphs = new Glyph[this.charSet.count()];

        for (int i = 0; i < this.charSet.count(); i++) {
            glyphs[i].x = charRegions[i].left;
            glyphs[i].y = charRegions[i].top;
            glyphs[i].width = charRegions[i].width();
            glyphs[i].height = fontHeight;
            glyphs[i].spaceWidth = charRegions[i].width();
            glyphs[i].spaceHeight = fontHeight;
        }

        texture = new VTexture(textureFilePath,
                VTexture.DEFAULT_FILTER,
                VTexture.DEFAULT_FILTER,
                VTexture.DEFAULT_WRAP,
                false);

        atlasTexture = new AtlasTexture(texture);

        textureDim = new Vec2f(texture.getWidth(), texture.getHeight());
    }

    private void init(Typeface textFont, int textFontSize) {
        valid = true;

        int fontMetricTop;
        int fontMetricAscent;
        int fontMetricDescent;
        int fontMetricBottom;
        int fontMetricLeading;

        Paint textPaint = new Paint();
        textPaint.setTypeface(textFont);
        textPaint.setTextSize(textFontSize);
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.LEFT);

        Paint.FontMetricsInt fm = textPaint.getFontMetricsInt();

        fontMetricTop = fm.top;
        fontMetricAscent = fm.ascent;
        fontMetricDescent = fm.descent;
        fontMetricBottom = fm.bottom;
        fontMetricLeading = fm.leading;

        fontMetricMaxHeight = fontMetricBottom - fontMetricTop;
        fontMetricHeight = fontMetricDescent - fontMetricAscent;

        glyphs = new Glyph[this.charSet.count()];

        int columns = (int) Math.ceil(Math.sqrt(this.charSet.count()));
        int maxRows = columns;

        int maxCharWidth = fontMetricMaxHeight + 1;
        int maxCharHeight = fontMetricMaxHeight + 1;

        int textureWidth = pow2roundup(columns * maxCharWidth);
        int textureMaxHeight = pow2roundup(maxRows * maxCharWidth);

        Bitmap TextBitmapAllChars = Bitmap.createBitmap(pow2roundup(textureWidth), pow2roundup(textureMaxHeight), Bitmap.Config.ARGB_8888);
        if (TextBitmapAllChars == null) {
            valid = false;
            tlog.w("failed to create bitmap W:" + pow2roundup(textureWidth) + " H: " + pow2roundup(textureMaxHeight));
            return;
        }
        Canvas gfxAllChars = new Canvas(TextBitmapAllChars);

        float[] charWidths = new float[this.charSet.count()];
        {
            char[] allChars = new char[this.charSet.count()];
            for (int i = 0; i < allChars.length; i++) {
                allChars[i] = this.charSet.getCharByIndex(i);
            }
            int widthsReturned = textPaint.getTextWidths(allChars, 0, charWidths.length, charWidths);
            if (widthsReturned < charWidths.length) {
                tlog.w("widthsReturned < charWidths.length");
            }
        }

        int currentX = 0;
        int currentY = Math.abs(fontMetricTop);
        gfxAllChars.drawColor(Color.TRANSPARENT);

        for (int i = 0; i < this.charSet.count(); i++) {
            glyphs[i] = new Glyph();

            char[] ch = new char[1];
            ch[0] = this.charSet.getCharByIndex(i);
            float charWidth = charWidths[i];
            Rect rect = new Rect();

            textPaint.getTextBounds(ch, 0, 1, rect);

            if (currentX + rect.width() >= TextBitmapAllChars.getWidth()) {
                currentX = 0;
                currentY += fontMetricMaxHeight;
            }

            gfxAllChars.drawText(ch, 0, 1, currentX - rect.left, currentY, textPaint);

            glyphs[i].x = currentX;
            glyphs[i].y = currentY - Math.abs(fontMetricTop);
            glyphs[i].width = rect.width();
            glyphs[i].height = fontMetricMaxHeight;
            glyphs[i].visualXOffset = rect.left;
            glyphs[i].visualYOffset = fontMetricBottom;
            glyphs[i].spaceWidth = charWidth;
            glyphs[i].spaceHeight = fontMetricHeight;

            currentX += (rect.width()) + 2;
        }

        int charsBitmapUsedWidth = currentY == 0 ? currentX : TextBitmapAllChars.getWidth();
        int charsBitmapUsedHeight = currentY + Math.abs(fontMetricBottom) + 1;

        charsBitmapUsedWidth = pow2roundup(charsBitmapUsedWidth);
        charsBitmapUsedHeight = pow2roundup(charsBitmapUsedHeight);

        Bitmap finalTextBitmapAllChars = Bitmap.createBitmap(TextBitmapAllChars, 0, 0, charsBitmapUsedWidth, charsBitmapUsedHeight);

        textureDim = new Vec2f(charsBitmapUsedWidth, charsBitmapUsedHeight);

        texture = new VTexture(finalTextBitmapAllChars,
                VTexture.DEFAULT_FILTER,
                VTexture.DEFAULT_FILTER,
                VTexture.DEFAULT_WRAP,
                false);
        atlasTexture = new AtlasTexture(texture);

        TextBitmapAllChars.recycle();
    }

    public boolean isValid() {
        return valid;
    }

    static int pow2roundup(int a) {
        return Utils.pow2roundup(a, 4096);
    }

    public Vec2f textureDim() {
        return textureDim;
    }

    public float fontHeight() {
        return fontMetricHeight;
    }

    public float fontMaxHeight() {
        return fontMetricMaxHeight;
    }

    public void dispose() {
        texture.dispose();
    }

    public Glyph getSpriteDescByChar(char ch) {
        return glyphs[charSet.getIndexByChar(ch)];
    }

    public ITexture getTexture() {
        return texture;
    }

    public IAtlasTexture getEntryTexture() {
        return atlasTexture;
    }

    public Vec2i measureText(String text) {
        Glyph glyph;

        float posx = 0.0f;

        for (int i = 0; i < text.length(); i++) {
            glyph = this.getSpriteDescByChar(text.charAt(i));
            posx += glyph.width;
        }

        return new Vec2i((int) posx, (int) this.fontHeight());
    }

    public int measureTextY() {
        return (int) this.fontHeight();
    }

    public static class CharSet {
        private final String chars;
        private final int count;

        public CharSet(String chars) {
            this.chars = chars;
            this.count = chars.length();
        }

        public CharSet(char first, char last) {
            StringBuilder sb = new StringBuilder();
            for (char c = first; c <= last; c++) {
                sb.append(c);
            }
            this.chars = sb.toString();
            this.count = this.chars.length();
        }

        public static CharSet createAscii32to126() {
            return new CharSet(' ', '~');
        }

        public static CharSet createAscii32to126AndCyrillic() {
            StringBuilder sb = new StringBuilder();
            for (char c = ' '; c <= '~'; c++) sb.append(c);
            for (char c = 'А'; c <= 'я'; c++) sb.append(c);
            sb.append('Ё');
            sb.append('ё');
            sb.append('«');
            sb.append('»');
            sb.append('—');
            sb.append('…');
            return new CharSet(sb.toString());
        }

        public static CharSet createAsciiNumbers() {
            return new CharSet('0', '9');
        }

        public static CharSet createAsciiNumbersAnd1() {
            return new CharSet('0', ':');
        }

        public static CharSet createAsciiCapitals() {
            return new CharSet('A', 'Z');
        }

        public int count() {
            return count;
        }

        public int getIndexByChar(char ch) {
            int idx = chars.indexOf(ch);
            if (idx >= 0) return idx;
            idx = chars.indexOf('?');
            return idx >= 0 ? idx : 0;
        }

        public int tryGetIndexByChar(char ch) {
            return chars.indexOf(ch);
        }

        public char getCharByIndex(int index) {
            if (index < 0 || index >= count) index = 0;
            return chars.charAt(index);
        }
    }

    public class Glyph {
        public float x;
        public float y;
        public float width;
        public float height;

        public float visualXOffset;
        public float visualYOffset;

        public float spaceWidth;
        public float spaceHeight;

    }

}

