

package com.aylis.Common.Events;

import android.os.Handler;

import java.lang.ref.WeakReference;
import java.util.List;

public class WeakDelegate1<T1> {

    WeakReference<Handler<T1>> listenerWeak = new WeakReference<>(null);

    public void clear() {
        listenerWeak = new WeakReference<>(null);
    }

    public WeakDelegate1<T1> subscribeWeak(Handler<T1> listener, List<Object> listenerRefHolder) {
        listenerRefHolder.add(listener);
        listenerWeak = new WeakReference<>(listener);
        return this;
    }

    public Handler subscribeHoldWeak(Handler<T1> listener) {
        listenerWeak = new WeakReference<>(listener);
        return listener;
    }

    public void invoke(T1 arg1) {
        Handler<T1> listener = listenerWeak.get();
        if (listener != null)
            listener.invoke(arg1);
    }

    public interface Handler<T1> {
        void invoke(T1 t1);
    }

}

