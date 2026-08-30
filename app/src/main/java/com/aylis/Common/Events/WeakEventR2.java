

package com.aylis.Common.Events;

import android.os.Handler;

import java.util.List;

public class WeakEventR2<T1, T2, TResult> {

    nallar.collections.ConcurrentWeakHashMap listeners = new nallar.collections.ConcurrentWeakHashMap();

    public void subscribeWeak(Handler<T1, T2, TResult> listener, List<Object> listenerRefHolder) {
        listenerRefHolder.add(listener);
        listeners.put(listener, this);
    }

    public Handler<T1, T2, TResult> subscribeHoldWeak(Handler<T1, T2, TResult> listener) {
        listeners.put(listener, this);
        return listener;
    }

    public TResult invoke(T1 arg1, T2 arg2, TResult defaultValue) {
        TResult result = defaultValue;

        for (Handler<T1, T2, TResult> listener : (Iterable<Handler<T1, T2, TResult>>) listeners.keySet()) {
            if (listener != null) {
                result = listener.invoke(arg1, arg2);
            }
        }

        return result;
    }

    public interface Handler<T1, T2, TResult> {
        TResult invoke(T1 t1, T2 t2);
    }

}

