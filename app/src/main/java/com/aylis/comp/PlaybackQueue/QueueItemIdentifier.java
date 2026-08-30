

package com.aylis.comp.PlaybackQueue;

import com.aylis.comp.Common.IItemIdentifier;

public class QueueItemIdentifier implements IItemIdentifier {

    private int queueIndex = -1;

    public QueueItemIdentifier(int queueIndex) {
        this.queueIndex = queueIndex;
    }

    public QueueItemIdentifier() {
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int getQueueIndex() {
        return queueIndex;
    }

    public void setQueueIndex(int queueIndex) {
        this.queueIndex = queueIndex;
    }
}
