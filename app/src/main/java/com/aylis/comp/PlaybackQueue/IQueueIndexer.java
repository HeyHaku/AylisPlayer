

package com.aylis.comp.PlaybackQueue;

import java.util.List;

public interface IQueueIndexer {

    boolean onQueueChanged(int first, int last, int sign, boolean swap, int listSize);

    boolean onQueueChanged(List<Integer> itemsIndex, int insertIndex, int removeIndex, boolean swap, int listSize);

    int getPrevSongIndex(boolean forced);

    int getCurrentSongIndex(boolean forced);

    int getNextSongIndex(boolean forced);

    void goTo(int queueIndex);

    void goToStart();

    boolean goToNext(int listSize);

    void goToPrev();

    int getQueueIndex();

    void setQueuePosBySongIndex(int songIndex);

    int getQueueIndexCount(int listSize);

    int getSongIndexByQueueIndex(int queueIndex, int listSize);

    interface QueueIndexesChangedListener {
        void onQueueIndexesChanged(IQueueIndexer queueIndexer, boolean eventFromOnQueueChanged, boolean currentSongIndexChanged);
    }
}
