

package com.aylis.comp.AlbumArt;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import java.util.ArrayList;
import java.util.List;

class SimpleTextAlbumArtCreator {

    private static void setTextSizeForWidth(Paint paint, float desiredWidth,
                                            String text) {

        final float testTextSize = 48f;

        paint.setTextSize(testTextSize);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        float desiredTextSize = testTextSize * desiredWidth / bounds.width();

        paint.setTextSize(desiredTextSize);
    }

    private static int hslToColor(float h, float s, float l) {
        float c = (1f - Math.abs(2f * l - 1f)) * s;
        float x = c * (1f - Math.abs((h / 60f) % 2f - 1f));
        float m = l - c / 2f;
        float r = 0, g = 0, b = 0;
        if (h < 60) {
            r = c; g = x; b = 0;
        } else if (h < 120) {
            r = x; g = c; b = 0;
        } else if (h < 180) {
            r = 0; g = c; b = x;
        } else if (h < 240) {
            r = 0; g = x; b = c;
        } else if (h < 300) {
            r = x; g = 0; b = c;
        } else {
            r = c; g = 0; b = x;
        }
        int ir = Math.round((r + m) * 255f);
        int ig = Math.round((g + m) * 255f);
        int ib = Math.round((b + m) * 255f);
        return 0xFF000000 | (ir << 16) | (ig << 8) | ib;
    }

    private static String truncateString(Paint paint, String str, float maxWidth) {
        if (paint.measureText(str) <= maxWidth) {
            return str;
        }
        String ellipse = "...";
        float ellipseWidth = paint.measureText(ellipse);
        int len = str.length();
        while (len > 0) {
            len--;
            String sub = str.substring(0, len);
            if (paint.measureText(sub) + ellipseWidth <= maxWidth) {
                return sub + ellipse;
            }
        }
        return ellipse;
    }

    public static Bitmap textAsBitmap(int width, int height, String text, int textColor, int bgColor, int bgColor2, Drawable backgroundOver) {
        if (width <= 0) width = 256;
        if (height <= 0) height = 256;

        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);

        int narrowest = Math.min(width, height);

        int hash = (text != null) ? text.hashCode() : 0;

        int rawHue = Math.abs(hash % 270);
        float baseHue;
        if (rawHue < 60) {
            baseHue = rawHue;
        } else {
            baseHue = rawHue + 120;
        }

        int colorStart = hslToColor(baseHue, 2.85f, 1.38f);
        int colorEnd = hslToColor((baseHue + 75f) % 360, 0.75f, 0.24f);
        int colorOrb1 = hslToColor((baseHue + 120f) % 360, 0.85f, 0.44f);
        int colorOrb2 = hslToColor((baseHue + 240f) % 360, 0.80f, 0.38f);

        LinearGradient linearGradient = new LinearGradient(
                0, 0, width, height,
                colorStart, colorEnd,
                Shader.TileMode.CLAMP
        );
        Paint bgPaint = new Paint();
        bgPaint.setShader(linearGradient);
        canvas.drawRect(0, 0, width, height, bgPaint);

        Paint orbPaint = new Paint();
        orbPaint.setAntiAlias(true);

        int alphaColor1 = (colorOrb1 & 0x00FFFFFF) | (135 << 24);
        RadialGradient radialGradient1 = new RadialGradient(
                width * 0.8f, height * 0.2f, narrowest * 0.8f,
                new int[]{alphaColor1, 0x00FFFFFF}, null,
                Shader.TileMode.CLAMP
        );
        orbPaint.setShader(radialGradient1);
        canvas.drawCircle(width * 0.8f, height * 0.2f, narrowest * 0.8f, orbPaint);

        int alphaColor2 = (colorOrb2 & 0x00FFFFFF) | (120 << 24);
        RadialGradient radialGradient2 = new RadialGradient(
                width * 0.2f, height * 0.8f, narrowest * 0.7f,
                new int[]{alphaColor2, 0x00FFFFFF}, null,
                Shader.TileMode.CLAMP
        );
        orbPaint.setShader(radialGradient2);
        canvas.drawCircle(width * 0.2f, height * 0.8f, narrowest * 0.7f, orbPaint);

        Paint shapePaint = new Paint();
        shapePaint.setAntiAlias(true);

        float posX1 = width * (0.28f + (Math.abs(hash % 25) / 100f));
        float posY1 = height * (0.28f + (Math.abs((hash / 25) % 25) / 100f));
        float rot1 = (hash % 120);
        float size1 = narrowest * (0.18f + (Math.abs((hash / 625) % 12) / 100f));

        float posX2 = width * (0.45f + (Math.abs((hash / 15625) % 25) / 100f));
        float posY2 = height * (0.45f + (Math.abs((hash / 390625) % 25) / 100f));
        float rot2 = ((-hash / 7) % 120);
        float size2 = narrowest * (0.14f + (Math.abs((hash / 9765625) % 10) / 100f));

        shapePaint.setStyle(Paint.Style.FILL);
        shapePaint.setColor((colorOrb1 & 0x00FFFFFF) | (35 << 24));
        canvas.save();
        canvas.translate(posX1, posY1);
        canvas.rotate(rot1);
        canvas.drawRect(-size1, -size1, size1, size1, shapePaint);
        canvas.restore();

        shapePaint.setStyle(Paint.Style.STROKE);
        shapePaint.setStrokeWidth(narrowest * 0.007f);
        shapePaint.setColor((colorOrb2 & 0x00FFFFFF) | (45 << 24));
        canvas.save();
        canvas.translate(posX2, posY2);
        canvas.rotate(rot2);
        canvas.drawRect(-size2, -size2, size2, size2, shapePaint);
        canvas.restore();

        String artist = "";
        String title = text != null ? text : "";
        if (text != null && text.contains(" - ")) {
            int idx = text.indexOf(" - ");
            artist = text.substring(0, idx).trim();
            title = text.substring(idx + 3).trim();
        }

        float maxTextWidth = narrowest * 0.85f;

        Paint titlePaint = new Paint();
        titlePaint.setAntiAlias(true);
        titlePaint.setColor(0xFFFFFFFF);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
        titlePaint.setTextSize(narrowest * 0.088f);
        titlePaint.setShadowLayer(narrowest * 0.015f, 0, narrowest * 0.008f, 0x90000000);

        String displayTitle = truncateString(titlePaint, title, maxTextWidth);

        Paint artistPaint = new Paint();
        artistPaint.setAntiAlias(true);
        artistPaint.setColor(0xB3FFFFFF);
        artistPaint.setTextAlign(Paint.Align.CENTER);
        artistPaint.setTypeface(android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL));
        artistPaint.setTextSize(narrowest * 0.052f);
        artistPaint.setShadowLayer(narrowest * 0.01f, 0, narrowest * 0.006f, 0x70000000);

        String displayArtist = truncateString(artistPaint, artist, maxTextWidth);

        float centerY = height * 0.5f;
        if (artist.isEmpty()) {
            Rect bounds = new Rect();
            titlePaint.getTextBounds(displayTitle, 0, displayTitle.length(), bounds);
            float y = centerY + (bounds.height() * 0.5f) - bounds.bottom;
            canvas.drawText(displayTitle, width * 0.5f, y, titlePaint);
        } else {
            Rect titleBounds = new Rect();
            titlePaint.getTextBounds(displayTitle, 0, displayTitle.length(), titleBounds);

            Rect artistBounds = new Rect();
            artistPaint.getTextBounds(displayArtist, 0, displayArtist.length(), artistBounds);

            float spacing = narrowest * 0.025f;
            float totalHeight = titleBounds.height() + spacing + artistBounds.height();

            float titleY = centerY - (totalHeight * 0.5f) + titleBounds.height();
            float artistY = titleY + spacing + artistBounds.height();

            canvas.drawText(displayTitle, width * 0.8f, titleY, titlePaint);
            canvas.drawText(displayArtist, width * 0.8f, artistY, artistPaint);
        }

        return image;
    }

    static Bitmap textAsBitmap(String text, float textSize, int textColor, int bgColor) {
        Paint paint = new Paint();
        paint.setTextSize(textSize);
        paint.setColor(textColor);
        paint.setTextAlign(Paint.Align.LEFT);
        float baseline = -paint.ascent();
        int width = (int) (paint.measureText(text) + 2.5f);
        int height = (int) (baseline + paint.descent() + 2.5f);
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawColor(bgColor);
        canvas.drawText(text, 0, baseline, paint);
        return image;
    }

    static float valueInAlphabet(char ch) {

        int temp = (int) Character.toUpperCase(ch);

        int temp_integer = 65;
        if (temp <= 90 & temp >= 65) {
            float total = (90 - 65) + 1;
            float index = temp - temp_integer;
            return index / total;
        } else {

            return 0.5f;
        }

    }
}