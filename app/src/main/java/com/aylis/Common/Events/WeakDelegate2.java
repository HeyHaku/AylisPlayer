

package com.aylis.Common.Events;

import android.os.Handler;

import java.lang.ref.WeakReference;
import java.util.List;

public class WeakDelegate2<T1, T2> {

    WeakReference<Handler<T1, T2>> listenerWeak = new WeakReference<>(null);

    public void clear() {
        listenerWeak = new WeakReference<>(null);
    }

    public WeakDelegate2<T1, T2> subscribeWeak(Handler<T1, T2> listener, List<Object> listenerRefHolder) {
        listenerRefHolder.add(listener);
        listenerWeak = new WeakReference<>(listener);
        return this;
    }

    public Handler subscribeHoldWeak(Handler<T1, T2> listener) {
        listenerWeak = new WeakReference<>(listener);
        return listener;
    }

    public void invoke(T1 arg1, T2 arg2) {
        Handler<T1, T2> listener = listenerWeak.get();
        if (listener != null)
            listener.invoke(arg1, arg2);

    }

    public interface Handler<T1, T2> {
        void invoke(T1 t1, T2 t2);
    }

}

