

package com.aylis.Common.Events;

import android.os.Handler;

import java.lang.ref.WeakReference;
import java.util.List;

public class WeakDelegateR<TResult> {

    WeakReference<Handler<TResult>> listenerWeak = new WeakReference<>(null);

    public void clear() {
        listenerWeak = new WeakReference<>(null);
    }

    public WeakDelegateR<TResult> subscribeWeak(Handler<TResult> listener, List<Object> listenerRefHolder) {
        listenerRefHolder.add(listener);
        listenerWeak = new WeakReference<>(listener);
        return this;
    }

    public Handler subscribeHoldWeak(Handler<TResult> listener) {
        listenerWeak = new WeakReference<>(listener);
        return listener;
    }

    public TResult invoke(TResult defaultValue) {
        Handler<TResult> listener = listenerWeak.get();
        if (listener != null)
            return listener.invoke();

        return defaultValue;
    }

    public interface Handler<TResult> {
        TResult invoke();
    }

}

