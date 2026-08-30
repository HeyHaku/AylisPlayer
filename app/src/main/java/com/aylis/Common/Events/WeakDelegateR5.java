

package com.aylis.Common.Events;

import android.os.Handler;

import java.lang.ref.WeakReference;
import java.util.List;

public class WeakDelegateR5<T1, T2, T3, T4, T5, TResult> {

    WeakReference<Handler<T1, T2, T3, T4, T5, TResult>> listenerWeak = new WeakReference<>(null);

    public void clear() {
        listenerWeak = new WeakReference<>(null);
    }

    public WeakDelegateR5<T1, T2, T3, T4, T5, TResult> subscribeWeak(Handler<T1, T2, T3, T4, T5, TResult> listener, List<Object> listenerRefHolder) {
        listenerRefHolder.add(listener);
        listenerWeak = new WeakReference<>(listener);
        return this;
    }

    public Handler subscribeHoldWeak(Handler<T1, T2, T3, T4, T5, TResult> listener) {
        listenerWeak = new WeakReference<>(listener);
        return listener;
    }

    public TResult invoke(T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, TResult defaultValue) {
        Handler<T1, T2, T3, T4, T5, TResult> listener = listenerWeak.get();
        if (listener != null)
            return listener.invoke(arg1, arg2, arg3, arg4, arg5);

        return defaultValue;
    }

    public interface Handler<T1, T2, T3, T4, T5, TResult> {
        TResult invoke(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5);
    }

}

