

package com.aylis.Common.Events;

import android.os.Handler;

import java.lang.ref.WeakReference;
import java.util.List;

public class WeakDelegateR1<T1, TResult> {

    WeakReference<Handler<T1, TResult>> listenerWeak = new WeakReference<>(null);

    public void clear() {
        listenerWeak = new WeakReference<>(null);
    }

    public WeakDelegateR1<T1, TResult> subscribeWeak(Handler<T1, TResult> listener, List<Object> listenerRefHolder) {
        listenerRefHolder.add(listener);
        listenerWeak = new WeakReference<>(listener);
        return this;
    }

    public Handler subscribeHoldWeak(Handler<T1, TResult> listener) {
        listenerWeak = new WeakReference<>(listener);
        return listener;
    }

    public TResult invoke(T1 arg1, TResult defaultValue) {
        Handler<T1, TResult> listener = listenerWeak.get();
        if (listener != null)
            return listener.invoke(arg1);

        return defaultValue;
    }

    public interface Handler<T1, TResult> {
        TResult invoke(T1 t1);
    }

}

