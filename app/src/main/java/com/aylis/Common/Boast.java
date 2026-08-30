

package com.aylis.Common;

import android.os.Message;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.widget.Toast;

public class Boast {

    private volatile static Boast globalBoast = null;

    private Toast internalToast;

    private Boast(Toast toast) {

        if (toast == null) {
            throw new NullPointerException(
                    "Boast.Boast(Toast) requires a non-null parameter.");
        }

        internalToast = toast;
    }

    @SuppressLint("ShowToast")
    public static Boast makeText(Context context, CharSequence text,
                                 int duration) {
        return new Boast(Toast.makeText(context, text, duration));
    }

    @SuppressLint("ShowToast")
    public static Boast makeText(Context context, int resId, int duration)
            throws Resources.NotFoundException {
        return new Boast(Toast.makeText(context, resId, duration));
    }

    @SuppressLint("ShowToast")
    public static Boast makeText(Context context, CharSequence text) {
        return new Boast(Toast.makeText(context, text, Toast.LENGTH_SHORT));
    }

    @SuppressLint("ShowToast")
    public static Boast makeText(Context context, int resId)
            throws Resources.NotFoundException {
        return new Boast(Toast.makeText(context, resId, Toast.LENGTH_SHORT));
    }

    public static void showText(Context context, CharSequence text, int duration) {
        Boast.makeText(context, text, duration).show();
    }

    public static void showText(Context context, int resId, int duration)
            throws Resources.NotFoundException {
        Boast.makeText(context, resId, duration).show();
    }

    public static void showText(Context context, CharSequence text) {
        Boast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static void showText(Context context, int resId)
            throws Resources.NotFoundException {
        Boast.makeText(context, resId, Toast.LENGTH_SHORT).show();
    }

    public void cancel() {
        internalToast.cancel();
    }

    public void show() {
        show(true);
    }

    public void show(boolean cancelCurrent) {

        if (cancelCurrent && (globalBoast != null)) {
            globalBoast.cancel();
        }

        globalBoast = this;

        internalToast.show();
    }

}
