
package com.aylis.comp.visual.core.Elements;

import android.graphics.RectF;
import android.graphics.Typeface;
import com.aylis.Common.Vec2i;
import com.aylis.Common.Vec3f;
import com.aylis.comp.visual.core.Graphic.RenderState;
import com.aylis.comp.visual.core.Graphic.SpriteFont;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.FrameBuffer;
import com.aylis.Common.Vec2f;

public class TextElement extends Element {

    private SpriteFont font1;
    private String text = "";
    private int fontSize = 24;
    private int color1 = 0xffffffff;
    private int color2 = 0xffffaa00;
    private String customFont = "Default";

    private boolean lyricsMode = false;
    private float lyricsActiveScale = 1.2f;
    private float lyricsInactiveScale = 1.0f;
    private float lyricsLineSpacing = 1.5f;
    private float lyricsFadeTopDistance = 2.0f;
    private float lyricsFadeBottomDistance = 3.0f;
    private float lyricsScrollSpeed = 8.0f;
    private float lyricsWipeSpeed = 1.0f;
    private boolean lyricsKaraokeWipe = true;

    private float smoothLyricsY = 0f;
    private int lyricsMaxCharsPerLine = 0;

    private String lastArtist = "";
    private String lastTitle = "";
    private int retrySearchAction = 0;
    private LyricsFetcher lyricsFetcher = new LyricsFetcher();

    // 0: Vertical, 1: Horizontal (R-L), 2: Horizontal (L-R), 3: Scatter
    private String lyricsLayoutMode = "Vertical";
    private String textAlign = "Center";

    public TextElement() {
        super();
        setPosition(0.5f, 0.5f);
    }

    public void setText(String text) {
        if (this.text != null && this.text.equals(text))
            return;
        this.text = text;
        if (!lyricsMode) {
            lyricsFetcher.parseLyrics(text, 0);
        }
    }

    public void setFontSize(int fontSize) {
        if (this.fontSize == fontSize)
            return;
        this.fontSize = fontSize;
        this.markNeedReCreateGLResources();
    }

    public void setColor(int colorARGB) {
        color1 = colorARGB;
    }

    @Override
    public void onApplyCustomization(CustomizationData customizationData) {
        super.onApplyCustomization(customizationData);
        setColor(customizationData.getPropertyInt("TextColor", color1));
        color2 = customizationData.getPropertyInt("LyricsActiveColor", color2);
        setFontSize(customizationData.getPropertyInt("TextSize", fontSize));

        lyricsMode = customizationData.getPropertyBool("LyricsMode", lyricsMode);
        lyricsActiveScale = customizationData.getPropertyFloat("LyricsActiveScale", lyricsActiveScale);
        lyricsInactiveScale = customizationData.getPropertyFloat("LyricsInactiveScale", lyricsInactiveScale);
        lyricsLineSpacing = customizationData.getPropertyFloat("LyricsLineSpacing", lyricsLineSpacing);
        lyricsFadeTopDistance = customizationData.getPropertyFloat("LyricsFadeTopDistance", lyricsFadeTopDistance);
        lyricsFadeBottomDistance = customizationData.getPropertyFloat("LyricsFadeBottomDistance",
                lyricsFadeBottomDistance);
        lyricsScrollSpeed = customizationData.getPropertyFloat("LyricsScrollSpeed", lyricsScrollSpeed);
        lyricsWipeSpeed = customizationData.getPropertyFloat("LyricsWipeSpeed", lyricsWipeSpeed);
        lyricsKaraokeWipe = customizationData.getPropertyBool("LyricsKaraokeWipe", lyricsKaraokeWipe);
        lyricsLayoutMode = customizationData.getPropertyString("LyricsLayoutMode", lyricsLayoutMode);

        lyricsMaxCharsPerLine = customizationData.getPropertyInt("LyricsMaxCharsPerLine", lyricsMaxCharsPerLine);
        lyricsFetcher.setMaxCharsPerLine(lyricsMaxCharsPerLine);

        int newRetry = customizationData.getPropertyAction("Retry Lyrics Search", retrySearchAction);
        if (newRetry != retrySearchAction) {
            retrySearchAction = newRetry;
            lyricsFetcher.setForceRefetch(true);
        }

        textAlign = customizationData.getPropertyString("TextAlign", textAlign);
        setText(customizationData.getPropertyString("Text", text));

        String oldCustomFont = customFont;
        customFont = customizationData.getPropertyString("CustomFont", customFont);
        if (!customFont.equals(oldCustomFont)) {
            markNeedReCreateGLResources();
        }
    }

    @Override
    public void onReadCustomization(CustomizationData outCustomizationData) {
        super.onReadCustomization(outCustomizationData);
        outCustomizationData.setCustomizationName("Text");
        outCustomizationData.putPropertyInt("TextColor", color1, "crgba", "1_appearance");
        outCustomizationData.putPropertyInt("TextSize", fontSize, "i 8 70", "1_appearance");
        outCustomizationData.putPropertyString("TextAlign", textAlign, "align", "1_appearance");
        outCustomizationData.putPropertyString("Text", text, "txt", "1_appearance");
        outCustomizationData.putPropertyString("CustomFont", customFont,
                com.aylis.comp.visual.core.CustomFontManager.getUIString(), "1_appearance");

        outCustomizationData.putPropertyBool("LyricsMode", lyricsMode, "1_lyrics");
        outCustomizationData.putPropertyInt("LyricsActiveColor", color2, "crgba", "1_lyrics");
        outCustomizationData.putPropertyFloat("LyricsActiveScale", lyricsActiveScale, "f 0.1 3.0", "1_lyrics");
        outCustomizationData.putPropertyFloat("LyricsInactiveScale", lyricsInactiveScale, "f 0.1 3.0", "1_lyrics");
        outCustomizationData.putPropertyFloat("LyricsLineSpacing", lyricsLineSpacing, "f 0.5 5.0", "1_lyrics");
        outCustomizationData.putPropertyFloat("LyricsFadeTopDistance", lyricsFadeTopDistance, "f 0.1 10.0", "1_lyrics");
        outCustomizationData.putPropertyFloat("LyricsFadeBottomDistance", lyricsFadeBottomDistance, "f 0.0 10.0",
                "1_lyrics");
        outCustomizationData.putPropertyFloat("LyricsScrollSpeed", lyricsScrollSpeed, "f 1.0 20.0", "1_lyrics");
        outCustomizationData.putPropertyFloat("LyricsWipeSpeed", lyricsWipeSpeed, "f 0.1 5.0", "1_lyrics");
        outCustomizationData.putPropertyBool("LyricsKaraokeWipe", lyricsKaraokeWipe, "1_lyrics");
        outCustomizationData.putPropertyString("LyricsLayoutMode", lyricsLayoutMode,
                "sel Vertical Horizontal_Left Horizontal_Right Scatter Carousel Circle", "1_lyrics");
        outCustomizationData.putPropertyInt("LyricsMaxCharsPerLine", lyricsMaxCharsPerLine, "i 0 100", "1_lyrics");
        outCustomizationData.putPropertyAction("Retry Lyrics Search", retrySearchAction, "1_lyrics");
    }

    @Override
    public void onCreateGLResources(RenderState renderData) {
        Typeface tf = com.aylis.comp.visual.core.CustomFontManager.getTypeface(customFont);
        if (font1 != null) {
            font1.dispose();
        }
        font1 = new SpriteFont(tf, fontSize, SpriteFont.CharSet.createAscii32to126AndCyrillic());
        super.onCreateGLResources(renderData);
    }

    @Override
    public void onRender(RenderState renderData, FrameBuffer resultFB) {
        super.onRender(renderData, resultFB);

        if (font1 == null || !font1.isValid())
            return;

        java.util.List<LrcLine> linesToRender;
        int activeIdx = -1;
        long currentMs = 0;

        if (lyricsMode) {
            String artist = renderData.res.meter.measureText("$artist");
            String title = renderData.res.meter.measureText("$title");
            String album = renderData.res.meter.measureText("$album");
            String durationStr = renderData.res.meter.measureText("$durationSec");
            long durationSec = 0;
            try {
                if (durationStr != null && !durationStr.isEmpty()) {
                    durationSec = Long.parseLong(durationStr);
                }
            } catch (Exception e) {
            }

            android.util.Log.d("LyricsFetcher", "Requesting lyrics for artist: [" + artist + "], title: [" + title + "]");
            lyricsFetcher.checkAndFetchLyrics(artist, title, album, durationSec, this.text);
            linesToRender = lyricsFetcher.getParsedLyrics();

            if (!linesToRender.isEmpty()) {
                String timeStr = renderData.res.meter.measureText("$trackPositionMs");
                try {
                    currentMs = Long.parseLong(timeStr);
                } catch (Exception e) {
                }
                for (int i = 0; i < linesToRender.size(); i++) {
                    if (currentMs >= linesToRender.get(i).timeMs) {
                        activeIdx = i;
                    } else {
                        break;
                    }
                }
            }
        } else {
            linesToRender = new java.util.ArrayList<>();
            String[] splitText = text.split("\\n");
            for (String s : splitText) {
                if (lyricsMaxCharsPerLine > 0) {
                    String[] words = s.split(" ");
                    StringBuilder currentLine = new StringBuilder();
                    for (String word : words) {
                        if (currentLine.length() + word.length() > lyricsMaxCharsPerLine && currentLine.length() > 0) {
                            linesToRender.add(new LrcLine(0, currentLine.toString().trim()));
                            currentLine = new StringBuilder();
                        }
                        currentLine.append(word).append(" ");
                    }
                    if (currentLine.length() > 0) {
                        linesToRender.add(new LrcLine(0, currentLine.toString().trim()));
                    }
                } else {
                    linesToRender.add(new LrcLine(0, s));
                }
            }
        }

        if (linesToRender.isEmpty())
            return;

        float lineHeight = renderData.res.getFontRenderer().measureTextY(font1)
                * (lyricsMode ? lyricsLineSpacing : 1.2f);

        // Measure max width for alignment
        float maxWidth = 0f;
        float[] lineWidths = new float[linesToRender.size()];
        String[] measuredTexts = new String[linesToRender.size()];

        for (int i = 0; i < linesToRender.size(); i++) {
            measuredTexts[i] = renderData.res.meter.measureText(linesToRender.get(i).text);
            float w = renderData.res.getFontRenderer().measureText(font1, measuredTexts[i]).x;
            lineWidths[i] = w;
            if (w > maxWidth)
                maxWidth = w;
        }

        float targetY = activeIdx >= 0 ? -activeIdx * lineHeight : 0f;
        if (lyricsMode) {
            smoothLyricsY += (targetY - smoothLyricsY) * renderData.getFrameTimeF() * lyricsScrollSpeed;
        } else {
            smoothLyricsY = 0f;
        }

        float baseRotation = measureDrawRot(renderData.res.meter);

        // For non-lyrics mode, center the whole block vertically
        float blockStartY = lyricsMode ? 0f : (linesToRender.size() - 1) * lineHeight * 0.5f;

        for (int i = 0; i < linesToRender.size(); i++) {
            float fade = 1.0f;
            float currentScale = 1.0f;
            float lineRotation = 0f;
            float layoutOffsetX = 0f;
            float layoutOffsetY = 0f;

            if (lyricsMode) {
                float indexRelative = i + (smoothLyricsY / lineHeight);
                LyricsLayout.LayoutResult layout = LyricsLayout.calculate(
                        lyricsLayoutMode, i, indexRelative, lineHeight,
                        lyricsFadeTopDistance, lyricsFadeBottomDistance,
                        lyricsActiveScale, lyricsInactiveScale);

                if (layout.fade <= 0f)
                    continue;
                fade = layout.fade;
                currentScale = layout.scale;
                layoutOffsetX = layout.offsetX;
                layoutOffsetY = layout.offsetY;
                lineRotation = layout.rotation;
            } else {
                layoutOffsetY = blockStartY - (i * lineHeight);
            }

            boolean useMaxWidth = !"None".equals(textAlign);
            if (lyricsMode && "Center".equals(textAlign)) {
                useMaxWidth = false;
            }
            float actualWidth = useMaxWidth ? maxWidth : lineWidths[i];

            // Alignment offset
            float alignOffset = 0f;
            if (useMaxWidth) {
                if ("Left".equals(textAlign)) {
                    alignOffset = 0f;
                } else if ("Right".equals(textAlign)) {
                    alignOffset = maxWidth - lineWidths[i];
                } else { // Center
                    alignOffset = (maxWidth - lineWidths[i]) / 2.0f;
                }
            }

            int origA1 = (color1 >> 24) & 0xFF;
            int origR1 = (color1 >> 16) & 0xFF;
            int origG1 = (color1 >> 8) & 0xFF;
            int origB1 = color1 & 0xFF;
            int c1 = ((int) (origA1 * fade) << 24) | ((int) (origR1 * fade) << 16) | ((int) (origG1 * fade) << 8)
                    | (int) (origB1 * fade);

            int origA2 = (color2 >> 24) & 0xFF;
            int origR2 = (color2 >> 16) & 0xFF;
            int origG2 = (color2 >> 8) & 0xFF;
            int origB2 = color2 & 0xFF;
            int c2 = ((int) (origA2 * fade) << 24) | ((int) (origR2 * fade) << 16) | ((int) (origG2 * fade) << 8)
                    | (int) (origB2 * fade);

            Vec2i dim = new Vec2i(0, 0);
            if (localPosX != 0.0f) {
                // Use maxWidth so the whole block pivots as one unit!
                dim = new Vec2i((int) actualWidth, (int) renderData.res.getFontRenderer().measureTextY(font1));
            } else {
                dim.x = (int) actualWidth;
                dim.y = renderData.res.getFontRenderer().measureTextY(font1);
            }

            RectF drawRect = measureDrawRect(renderData.res.meter, dim);
            drawRect.offset(layoutOffsetX + alignOffset, layoutOffsetY);

            float finalRotation = baseRotation + lineRotation;

            float pivotX = drawRect.left + (lineWidths[i] / 2.0f);

            if (lyricsMode && i == activeIdx) {
                float progress = 1.0f;
                if (i < linesToRender.size() - 1) {
                    long start = linesToRender.get(i).timeMs;
                    long end = linesToRender.get(i + 1).timeMs;
                    long duration = end - start;
                    if (duration > 0) {
                        progress = ((float) (currentMs - start) / (float) duration) * lyricsWipeSpeed;
                        if (progress > 1.0f)
                            progress = 1.0f;
                        if (progress < 0.0f)
                            progress = 0.0f;
                    }
                }

                if (lyricsKaraokeWipe) {
                    renderData.res.getFontRenderer().drawTextRotatedWipe(renderData, font1, font1.getEntryTexture(),
                            drawRect, measuredTexts[i], c1, c2, progress, currentScale, finalRotation,
                            new com.aylis.Common.Vec2f(pivotX, drawRect.centerY()));
                } else {
                    int activeC = (progress > 0) ? c2 : c1;
                    renderData.res.getFontRenderer().drawTextRotatedWipe(renderData, font1, font1.getEntryTexture(),
                            drawRect, measuredTexts[i], activeC, activeC, 1.0f, currentScale, finalRotation,
                            new com.aylis.Common.Vec2f(pivotX, drawRect.centerY()));
                }
            } else {
                renderData.res.getFontRenderer().drawTextRotatedWipe(renderData, font1, font1.getEntryTexture(),
                        drawRect, measuredTexts[i], c1, c2, 0.0f, currentScale, finalRotation,
                        new com.aylis.Common.Vec2f(pivotX, drawRect.centerY()));
            }
        }
    }
}
