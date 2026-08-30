

package com.aylis.Common.Events;

import android.os.Handler;

import java.util.List;

public class WeakEventR1<T1, TResult> {

    nallar.collections.ConcurrentWeakHashMap listeners = new nallar.collections.ConcurrentWeakHashMap();

    public void subscribeWeak(Handler<T1, TResult> listener, List<Object> listenerRefHolder) {
        listenerRefHolder.add(listener);
        listeners.put(listener, this);
    }

    public Handler<T1, TResult> subscribeHoldWeak(Handler<T1, TResult> listener) {
        listeners.put(listener, this);
        return listener;
    }

    public TResult invoke(T1 arg1, TResult defaultValue) {
        TResult result = defaultValue;

        for (Handler<T1, TResult> listener : (Iterable<Handler<T1, TResult>>) listeners.keySet()) {
            if (listener != null)
                result = listener.invoke(arg1);
        }

        return result;
    }

    public interface Handler<T1, TResult> {
        TResult invoke(T1 t1);
    }

}

