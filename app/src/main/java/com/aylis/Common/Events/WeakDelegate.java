

package com.aylis.Common.Events;

import android.os.Handler;

import java.lang.ref.WeakReference;
import java.util.List;

public class WeakDelegate {

    WeakReference<Handler> listenerWeak = new WeakReference<>(null);

    public void clear() {
        listenerWeak = new WeakReference<>(null);
    }

    public WeakDelegate subscribeWeak(Handler listener, List<Object> listenerRefHolder) {
        listenerRefHolder.add(listener);
        listenerWeak = new WeakReference<>(listener);
        return this;
    }

    public Handler subscribeHoldWeak(Handler listener) {
        listenerWeak = new WeakReference<>(listener);
        return listener;
    }

    public void invoke() {
        Handler listener = listenerWeak.get();
        if (listener != null)
            listener.invoke();
    }

    public interface Handler {
        void invoke();
    }

}

