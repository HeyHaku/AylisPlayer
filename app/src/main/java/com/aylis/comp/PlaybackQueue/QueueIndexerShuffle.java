

package com.aylis.comp.PlaybackQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class QueueIndexerShuffle implements IQueueIndexer {

    private int currentQueueIndex;
    private QueueIndexesChangedListener indexesListener;
    private List<Integer> shuffleIndices;

    public QueueIndexerShuffle() {

        currentQueueIndex = 0;
        shuffleIndices = new ArrayList<>();
        indexesListener = null;
    }

    public void init(int startSongIndex, List<Integer> shuffleIndices, QueueIndexesChangedListener indexesListener) {
        this.indexesListener = indexesListener;
        this.shuffleIndices = shuffleIndices;

        currentQueueIndex = 0;
        setQueuePosBySongIndex(startSongIndex);

        if (indexesListener != null)
            indexesListener.onQueueIndexesChanged(this, false, true);
    }

    public boolean onQueueChanged(int first, int last, int sign, boolean swap, int listSize) {
        boolean currentSongIndexChanged = false;

        for (ListIterator<Integer> it = shuffleIndices.listIterator(); it.hasNext(); ) {
            int i = it.nextIndex();
            Integer songIndex = it.next();

            int newindex = QueueCore.fixQueueIndex_(songIndex, first, last, sign, swap);
            if (newindex < 0)
            {
                it.remove();

                int newQueueIndex = QueueCore.fixQueueIndexSingle(currentQueueIndex, i, -1);
                if (newQueueIndex < 0)
                {
                    currentSongIndexChanged = true;

                    currentQueueIndex = i;

                    if (currentQueueIndex < 0) currentQueueIndex = 0;
                    if (currentQueueIndex >= shuffleIndices.size())
                        currentQueueIndex = shuffleIndices.size() - 1;
                }
            } else {
                it.set(newindex);
            }
        }

        if (indexesListener != null)
            indexesListener.onQueueIndexesChanged(this, true, currentSongIndexChanged);

        return currentSongIndexChanged;
    }

    @Override
    public boolean onQueueChanged(List<Integer> itemsIndex, int insertIndex, int removeIndex, boolean swap, int listSize) {
        boolean currentSongIndexChanged = false;

        for (ListIterator<Integer> it = shuffleIndices.listIterator(); it.hasNext(); ) {
            int i = it.nextIndex();
            Integer songIndex = it.next();

            int newindex = QueueCore.fixQueueIndex(songIndex, itemsIndex, insertIndex, removeIndex, swap);
            if (newindex < 0)
            {
                it.remove();

                int newQueueIndex = QueueCore.fixRemovedQueueIndexSingle(currentQueueIndex, i);
                if (newQueueIndex < 0)
                {
                    currentSongIndexChanged = true;

                    currentQueueIndex = i;

                    if (currentQueueIndex < 0) currentQueueIndex = 0;
                    if (currentQueueIndex >= shuffleIndices.size())
                        currentQueueIndex = shuffleIndices.size() - 1;
                }
            } else {
                it.set(newindex);
            }
        }

        if (indexesListener != null)
            indexesListener.onQueueIndexesChanged(this, true, currentSongIndexChanged);

        return currentSongIndexChanged;
    }

    public int getPrevSongIndex(boolean forced) {
        int inx = currentQueueIndex - 1;
        if (inx < 0 || inx >= shuffleIndices.size()) return -1;
        return shuffleIndices.get(inx);
    }

    public int getCurrentSongIndex(boolean forced) {
        if (currentQueueIndex < 0 || currentQueueIndex >= shuffleIndices.size()) return -1;
        return shuffleIndices.get(currentQueueIndex);
    }

    public int getNextSongIndex(boolean forced) {
        int inx = currentQueueIndex + 1;
        if (inx < 0 || inx >= shuffleIndices.size()) return -1;
        return shuffleIndices.get(inx);
    }

    public void goTo(int queueIndex) {
        currentQueueIndex = queueIndex;
    }

    public void goToStart() {
        currentQueueIndex = 0;
    }

    public boolean goToNext(int listSize) {
        currentQueueIndex = currentQueueIndex + 1;
        if (currentQueueIndex >= shuffleIndices.size()) {

            currentQueueIndex = shuffleIndices.size() - 1;

            return true;
        }

        return false;
    }

    public void goToPrev() {
        currentQueueIndex = currentQueueIndex - 1;
        if (currentQueueIndex < 0) currentQueueIndex = 0;
    }

    public int getQueueIndex() {
        return currentQueueIndex;
    }

    public void setQueuePosBySongIndex(int newSongIndex) {
        for (ListIterator<Integer> it = shuffleIndices.listIterator(); it.hasNext(); ) {
            int i = it.nextIndex();
            Integer songIndex = it.next();

            if (songIndex == newSongIndex) {
                currentQueueIndex = i;
                return;
            }
        }
    }

    @Override
    public int getQueueIndexCount(int listSize) {
        return Math.min(shuffleIndices.size(), listSize);
    }

    @Override
    public int getSongIndexByQueueIndex(int queueIndex, int listSize) {
        if (queueIndex < 0 || queueIndex >= shuffleIndices.size()) return -1;
        return shuffleIndices.get(queueIndex);
    }
}