package com.aylis.comp.visual.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class ColorWheelView extends View {
    private Paint selectorPaint;
    private float radius;
    private float centerX;
    private float centerY;
    private float dotRadius;
    private float[] currentHsv = new float[]{0f, 0f, 1f};
    private OnColorSelectedListener listener;
    private List<ColorDot> dots = new ArrayList<>();
    private int numRings = 12;
    private float currentBrightness = 1f;

    public void setNumRings(int numRings) {
        this.numRings = numRings;
        populateDots();
        invalidate();
    }

    public void setBrightness(float brightness) {
        this.currentBrightness = brightness;
        for (ColorDot dot : dots) {
            dot.hsv[2] = brightness;
            dot.color = Color.HSVToColor(dot.hsv);
        }
        currentHsv[2] = brightness;
        invalidate();
    }

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    private static class ColorDot {
        float x, y;
        int color;
        float[] hsv;

        ColorDot(float x, float y, int color, float[] hsv) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.hsv = hsv;
        }
    }

    public ColorWheelView(Context context) {
        super(context);
        init();
    }

    public ColorWheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorWheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorPaint.setStyle(Paint.Style.STROKE);
        selectorPaint.setStrokeWidth(4f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        radius = Math.min(centerX, centerY) - 20f;

        populateDots();

        double angle = Math.toRadians(currentHsv[0]);
        float r = currentHsv[1] * radius;
        float targetX = centerX + (float) (Math.cos(angle) * r);
        float targetY = centerY + (float) (Math.sin(angle) * r);
        ColorDot closest = findClosestDot(targetX, targetY);
        if (closest != null) {
            currentHsv[0] = closest.hsv[0];
            currentHsv[1] = closest.hsv[1];
            currentHsv[2] = closest.hsv[2];
        }
    }

    private void populateDots() {
        dots.clear();
        if (radius <= 0) return;

        float ringSpacing = radius / (float) numRings;
        dotRadius = ringSpacing * 0.44f;

        dots.add(new ColorDot(centerX, centerY, Color.HSVToColor(new float[]{0f, 0f, currentBrightness}), new float[]{0f, 0f, currentBrightness}));

        for (int i = 1; i <= numRings; i++) {
            float r = radius * (i / (float) numRings);
            int numDots = i * 6;
            for (int j = 0; j < numDots; j++) {
                double angleRad = j * (2.0 * Math.PI / numDots);
                float x = centerX + (float) (Math.cos(angleRad) * r);
                float y = centerY + (float) (Math.sin(angleRad) * r);

                float hue = (float) Math.toDegrees(angleRad);
                float saturation = i / (float) numRings;
                float[] hsv = new float[]{hue, saturation, currentBrightness};
                int color = Color.HSVToColor(hsv);

                dots.add(new ColorDot(x, y, color, hsv));
            }
        }
    }

    private ColorDot findClosestDot(float x, float y) {
        ColorDot closest = null;
        float minDistance = Float.MAX_VALUE;
        for (ColorDot dot : dots) {
            float dx = dot.x - x;
            float dy = dot.y - y;
            float dist = dx * dx + dy * dy;
            if (dist < minDistance) {
                minDistance = dist;
                closest = dot;
            }
        }
        return closest;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (radius <= 0 || dots.isEmpty()) return;

        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (ColorDot dot : dots) {
            dotPaint.setColor(dot.color);
            canvas.drawCircle(dot.x, dot.y, dotRadius, dotPaint);
        }

        double angle = Math.toRadians(currentHsv[0]);
        float r = currentHsv[1] * radius;
        float targetX = centerX + (float) (Math.cos(angle) * r);
        float targetY = centerY + (float) (Math.sin(angle) * r);
        ColorDot selectedDot = findClosestDot(targetX, targetY);

        if (selectedDot != null) {
            selectorPaint.setStyle(Paint.Style.STROKE);
            selectorPaint.setStrokeWidth(4f);

            selectorPaint.setColor(Color.BLACK);
            canvas.drawCircle(selectedDot.x, selectedDot.y, dotRadius + 4f, selectorPaint);

            selectorPaint.setColor(Color.WHITE);
            canvas.drawCircle(selectedDot.x, selectedDot.y, dotRadius + 2f, selectorPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (dots.isEmpty()) return super.onTouchEvent(event);

        float touchX = event.getX();
        float touchY = event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            ColorDot closest = findClosestDot(touchX, touchY);
            if (closest != null) {
                currentHsv[0] = closest.hsv[0];
                currentHsv[1] = closest.hsv[1];
                currentHsv[2] = closest.hsv[2];

                invalidate();

                if (listener != null) {
                    listener.onColorSelected(closest.color);
                }
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    public void setColor(int color) {
        Color.colorToHSV(color, currentHsv);
        this.currentBrightness = currentHsv[2];
        if (!dots.isEmpty()) {
            for (ColorDot dot : dots) {
                dot.hsv[2] = currentBrightness;
                dot.color = Color.HSVToColor(dot.hsv);
            }
            double angle = Math.toRadians(currentHsv[0]);
            float r = currentHsv[1] * radius;
            float targetX = centerX + (float) (Math.cos(angle) * r);
            float targetY = centerY + (float) (Math.sin(angle) * r);
            ColorDot closest = findClosestDot(targetX, targetY);
            if (closest != null) {
                currentHsv[0] = closest.hsv[0];
                currentHsv[1] = closest.hsv[1];
                currentHsv[2] = closest.hsv[2];
            }
        }
        invalidate();
    }

    public void setOnColorSelectedListener(OnColorSelectedListener listener) {
        this.listener = listener;
    }
}
