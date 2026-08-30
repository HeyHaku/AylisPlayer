

package com.aylis.Common.Events;

import android.os.Handler;

import java.util.List;

public class WeakEvent5<T1, T2, T3, T4, T5> {

    nallar.collections.ConcurrentWeakHashMap listeners = new nallar.collections.ConcurrentWeakHashMap();

    public void subscribeWeak(Handler<T1, T2, T3, T4, T5> listener, List<Object> listenerRefHolder) {
        listenerRefHolder.add(listener);
        listeners.put(listener, this);
    }

    public Handler<T1, T2, T3, T4, T5> subscribeHoldWeak(Handler<T1, T2, T3, T4, T5> listener) {
        listeners.put(listener, this);
        return listener;
    }

    public void invoke(T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5) {

        for (Handler<T1, T2, T3, T4, T5> listener : (Iterable<Handler<T1, T2, T3, T4, T5>>) listeners.keySet()) {
            if (listener != null)
                listener.invoke(arg1, arg2, arg3, arg4, arg5);
        }
    }

    public interface Handler<T1, T2, T3, T4, T5> {
        void invoke(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5);
    }

}

