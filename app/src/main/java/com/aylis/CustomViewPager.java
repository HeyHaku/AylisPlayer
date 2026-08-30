

package com.aylis;

import android.content.Context;

import androidx.core.view.MotionEventCompat;
import androidx.viewpager.widget.ViewPager;
import android.view.MotionEvent;
import com.aylis.R;

public class CustomViewPager extends ViewPager {

    float startDragX;
    OnSwipeOutListener listener;
    boolean eventFired = false;
    float swipeDistMin = 0.0f;
    float lastProgress = 0.0f;
    float maxProgress = 0.0f;

    public CustomViewPager(android.content.Context context) {
        super(context);
        initScroller();
        swipeDistMin = context.getResources().getDimension(R.dimen.out_of_bound_swipe_dist);
    }

    public CustomViewPager(android.content.Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        initScroller();
        swipeDistMin = context.getResources().getDimension(R.dimen.out_of_bound_swipe_dist);

    }

    private void initScroller() {
        try {
            java.lang.reflect.Field scroller = ViewPager.class.getDeclaredField("mScroller");
            scroller.setAccessible(true);
            FixedSpeedScroller customScroller = new FixedSpeedScroller(getContext(), new android.view.animation.DecelerateInterpolator());
            scroller.set(this, customScroller);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOnSwipeOutListener(OnSwipeOutListener listener) {
        this.listener = listener;
    }

    private boolean swipeEnabled = false; // Отключаем свайпы по умолчанию

    public void setSwipeEnabled(boolean enabled) {
        this.swipeEnabled = enabled;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!swipeEnabled) return false;

        final int action = ev.getAction();
        float x = ev.getX();
        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                startDragX = x;
                eventFired = false;
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                break;
        }

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!swipeEnabled) return false;

        final int action = ev.getAction();
        float x = ev.getX();

        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                startDragX = x;
                eventFired = false;
                lastProgress = 0.0f;
                maxProgress = 0.0f;
                listener.onSwipeProgressUpdate(maxProgress);

                break;
            case MotionEvent.ACTION_MOVE:
                if (eventFired) break;

                float progress = 0.0f;
                if (getCurrentItem() == 0) {
                    progress = (x - startDragX) / swipeDistMin;
                } else if (getCurrentItem() == getAdapter().getCount() - 1) {
                    progress = (startDragX - x) / swipeDistMin;
                }

                maxProgress = Math.max(maxProgress, progress);

                listener.onSwipeProgressUpdate(maxProgress);

                if ((x - startDragX) > swipeDistMin && getCurrentItem() == 0) {
                    maxProgress = 0.0f;
                    listener.onSwipeProgressUpdate(maxProgress);
                    listener.onSwipeOutAtStart();
                    eventFired = true;
                } else if ((startDragX - x) > swipeDistMin && getCurrentItem() == getAdapter().getCount() - 1) {
                    maxProgress = 0.0f;
                    listener.onSwipeProgressUpdate(maxProgress);
                    listener.onSwipeOutAtEnd();

                    eventFired = true;
                }

                break;
            case MotionEvent.ACTION_UP:
                if (eventFired) break;
                maxProgress = 0.0f;
                listener.onSwipeProgressUpdate(maxProgress);
                break;
        }
        return super.onTouchEvent(ev);
    }

    public interface OnSwipeOutListener {
        void onSwipeOutAtStart();

        void onSwipeOutAtEnd();

        void onSwipeProgressUpdate(float val);
    }

}
