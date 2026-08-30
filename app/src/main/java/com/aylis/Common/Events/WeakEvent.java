

package com.aylis.Common.Events;

import android.os.Handler;

import java.util.List;

public class WeakEvent {

    nallar.collections.ConcurrentWeakHashMap listeners = new nallar.collections.ConcurrentWeakHashMap();

    public void subscribeWeak(Handler listener, List<Object> listenerRefHolder) {
        listenerRefHolder.add(listener);
        listeners.put(listener, this);
    }

    public Handler subscribeHoldWeak(Handler listener) {
        listeners.put(listener, this);
        return listener;
    }

    public void invoke() {
        for (Handler listener : (Iterable<Handler>) listeners.keySet()) {
            if (listener != null)
                listener.invoke();
        }
    }

    public interface Handler {
        void invoke();
    }

}

