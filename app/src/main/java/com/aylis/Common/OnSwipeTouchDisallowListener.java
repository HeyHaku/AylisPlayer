

package com.aylis.Common;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewParent;

public class OnSwipeTouchDisallowListener implements OnTouchListener {

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        attemptClaimDrag(v);
        return v.onTouchEvent(event);
    }

    private void attemptClaimDrag(View v) {
        if (v.getParent() != null) {
            v.getParent().requestDisallowInterceptTouchEvent(true);
        }
    }
}

class OnSwipeTouchDisallowParentListener implements OnTouchListener {

    ViewParent disallowParent;

    public OnSwipeTouchDisallowParentListener(ViewParent disallowParent) {
        this.disallowParent = disallowParent;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        disallowParent.requestDisallowInterceptTouchEvent(true);
        return v.onTouchEvent(event);
    }

}

