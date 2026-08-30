

package com.aylis.Common.Events;

import android.os.Handler;

import java.util.List;

public class WeakEventR<TResult> {

    nallar.collections.ConcurrentWeakHashMap listeners = new nallar.collections.ConcurrentWeakHashMap();

    public void subscribeWeak(Handler<TResult> listener, List<Object> listenerRefHolder) {
        listenerRefHolder.add(listener);
        listeners.put(listener, this);
    }

    public Handler<TResult> subscribeHoldWeak(Handler<TResult> listener) {
        listeners.put(listener, this);
        return listener;
    }

    public TResult invoke(TResult defaultValue) {
        TResult result = defaultValue;

        for (Handler<TResult> listener : (Iterable<Handler<TResult>>) listeners.keySet()) {
            if (listener != null)
                result = listener.invoke();
        }

        return result;
    }

    public interface Handler<TResult> {
        TResult invoke();
    }

}

