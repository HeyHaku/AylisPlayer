

package com.aylis.comp.LibraryQueueUI;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class MyView extends View {

    private OnLayoutChangeListener onSizeChnagedListener = null;

    public MyView(Context context) {
        super(context);
    }

    public MyView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (onSizeChnagedListener != null)
            onSizeChnagedListener.onLayoutChange(this, 0, 0, w, h, 0, 0, 0, 0);

    }

    public void setOnSizeChangeListener(OnLayoutChangeListener listener) {
        onSizeChnagedListener = listener;
    }
}
