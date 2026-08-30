

package com.aylis.Common.Events;

import android.os.Handler;

import java.util.List;

public class WeakEventR3<T1, T2, T3, TResult> {

    nallar.collections.ConcurrentWeakHashMap listeners = new nallar.collections.ConcurrentWeakHashMap();

    public void subscribeWeak(Handler<T1, T2, T3, TResult> listener, List<Object> listenerRefHolder) {
        listenerRefHolder.add(listener);
        listeners.put(listener, this);
    }

    public Handler<T1, T2, T3, TResult> subscribeHoldWeak(Handler<T1, T2, T3, TResult> listener) {
        listeners.put(listener, this);
        return listener;
    }

    public TResult invoke(T1 arg1, T2 arg2, T3 arg3, TResult defaultValue) {
        TResult result = defaultValue;

        for (Handler<T1, T2, T3, TResult> listener : (Iterable<Handler<T1, T2, T3, TResult>>) listeners.keySet()) {
            if (listener != null)
                result = listener.invoke(arg1, arg2, arg3);
        }

        return result;
    }

    public interface Handler<T1, T2, T3, TResult> {
        TResult invoke(T1 t1, T2 t2, T3 t3);
    }

}

