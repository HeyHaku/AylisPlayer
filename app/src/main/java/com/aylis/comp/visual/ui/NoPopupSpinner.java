package com.aylis.comp.visual.ui;

import android.content.Context;
import android.util.AttributeSet;

public class NoPopupSpinner extends androidx.appcompat.widget.AppCompatSpinner {
    public NoPopupSpinner(Context context) {
        super(context);
    }

    public NoPopupSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NoPopupSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean performClick() {
        // Отключаем нативный вызов super.performClick(), чтобы не открывалось окно Spinner.
        // Вместо этого вызываем обычный onClickListener.
        callOnClick();
        return true;
    }
}
