

package com.aylis.Common.Events;

import android.os.Handler;

import java.util.List;

public class WeakEvent1<T1> {

    nallar.collections.ConcurrentWeakHashMap listeners = new nallar.collections.ConcurrentWeakHashMap();

    public Handler<T1> subscribeWeak(Handler<T1> listener, List<Object> listenerRefHolder) {
        listenerRefHolder.add(listener);
        listeners.put(listener, this);
        return listener;
    }

    public Handler<T1> subscribeHoldWeak(Handler<T1> listener) {
        listeners.put(listener, this);
        return listener;
    }

    public void invoke(T1 p1) {

        for (Handler<T1> listener : (Iterable<Handler<T1>>) listeners.keySet()) {
            if (listener != null)
                listener.invoke(p1);
        }
    }

    public interface Handler<T1> {
        void invoke(T1 t1);
    }

}

