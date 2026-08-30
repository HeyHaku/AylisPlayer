

package com.aylis.Common.Events;

import android.os.Handler;

import java.util.List;

public class WeakEvent2<T1, T2> {

    nallar.collections.ConcurrentWeakHashMap listeners = new nallar.collections.ConcurrentWeakHashMap();

    public void subscribeWeak(Handler<T1, T2> listener, List<Object> listenerRefHolder) {
        listenerRefHolder.add(listener);
        listeners.put(listener, this);
    }

    public Handler<T1, T2> subscribeHoldWeak(Handler<T1, T2> listener) {
        listeners.put(listener, this);
        return listener;
    }

    public void invoke(T1 arg1, T2 arg2) {

        for (Handler<T1, T2> listener : (Iterable<Handler<T1, T2>>) listeners.keySet()) {
            if (listener != null)
                listener.invoke(arg1, arg2);
        }
    }

    public interface Handler<T1, T2> {
        void invoke(T1 t1, T2 t2);
    }

}

