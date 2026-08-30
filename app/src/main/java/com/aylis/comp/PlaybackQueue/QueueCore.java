

package com.aylis.comp.PlaybackQueue;

import android.os.Message;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import com.aylis.Common.Events.WeakEvent1;
import com.aylis.Common.Events.WeakEvent2;
import com.aylis.Common.Events.WeakEvent5;
import com.aylis.Common.Events.WeakEventR;
import com.aylis.Common.MultiList;
import com.aylis.Common.Tuple2;
import com.aylis.Common.Utils;
import com.aylis.Common.UtilsMusic;
import com.aylis.PlayerCore;
import com.aylis.comp.AppPreferences.AppPreferences;
import com.aylis.comp.Common.IItemIdentifier;
import com.aylis.comp.playback.MediaPlaybackServiceDefs;
import com.aylis.comp.playback.Song.PlaylistSong;
import com.aylis.EventsGlobal.EventsGlobalTextNotifier;
import com.aylis.R;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class QueueCore implements MediaPlaybackServiceDefs, IQueueIndexer.QueueIndexesChangedListener {

    public static WeakEventR<Boolean> onRequestShouldReloadInitalSongs = new WeakEventR<>();
    public static WeakEvent5<Tuple2<PlaylistSong, IItemIdentifier>  , Integer  , Boolean  , Boolean  , Object  > onQueuePosChanged = new WeakEvent5<>();
    public static WeakEvent2<MultiList<PlaylistSong, IItemIdentifier>  , IPlaylistSongContainerIdentifier  > onQueueStateChanged = new WeakEvent2<>();
    public static WeakEvent1<Integer  > onShuffleModeChanged = new WeakEvent1<>();

    private static final Object createInstanceLock = new Object();
    private static volatile WeakReference<QueueCore> instanceWeak = new WeakReference<>(null);
    private IPlaylistSongContainerIdentifier songContainerIdentifier = null;
    private MultiList<PlaylistSong, IItemIdentifier> playList = new MultiList<>();
    private IQueueIndexer queueIndexer = new QueueIndexerNormal();
    private int shuffleMode = -1;

    public QueueCore() {
        setShuffleMode(SHUFFLE_NONE, false);
        reloadQueue();
    }

    public static QueueCore createOrGetInstance() {
        QueueCore inst0 = instanceWeak.get();
        if (inst0 != null) return inst0;

        synchronized (createInstanceLock) {
            QueueCore inst = instanceWeak.get();
            if (inst == null) {
                inst = new QueueCore();
                instanceWeak = new WeakReference<>(inst);
            }

            return inst;
        }
    }

    private Resources getResources() {
        Context context = PlayerCore.s().getAppContext();
        if (context == null) return null;

        return context.getResources();
    }

    private void notifyMessage(final String msg) {
        EventsGlobalTextNotifier.onTextMsg.invoke(msg);
    }

    private void notifyQueueChange() {
        final MultiList<PlaylistSong, IItemIdentifier> list = getQueue();
        onQueueStateChanged.invoke(list, songContainerIdentifier);
    }

    public void setShuffleMode(int shuffleMode, boolean allowTextMessages) {
        setShuffleMode(shuffleMode, allowTextMessages, false);
    }

    public void setShuffleMode(int shuffleMode, boolean allowTextMessages, boolean reloadForce) {
        if (!reloadForce)
            if (this.shuffleMode == shuffleMode) return;

        this.shuffleMode = shuffleMode;

        int currentSongIndex = queueIndexer == null ? 0 : queueIndexer.getCurrentSongIndex(true);

        if (this.shuffleMode == SHUFFLE_NONE) {
            queueIndexer = new QueueIndexerNormal();
            ((QueueIndexerNormal) queueIndexer).init(currentSongIndex, this);

            if (allowTextMessages) {
                Context context = PlayerCore.s().getAppContext();
                if (context != null)
                    notifyMessage(context.getString(R.string.playback_shuffle_off));
            }

        } else if (this.shuffleMode == SHUFFLE_NORMAL) {
            List<Integer> shuffleIndices = new ArrayList<>(playList.size());
            List<Integer> songsToShuffle = new ArrayList<>();

            int shuffleHorizon = currentSongIndex;
            if (shuffleHorizon < 0) shuffleHorizon = 0;
            if (shuffleHorizon > playList.size()) shuffleHorizon = playList.size() - 1;

            for (int i = 0; i < shuffleHorizon; i++)
                shuffleIndices.add(i);

            shuffleIndices.add(shuffleHorizon);

            for (int i = shuffleHorizon + 1; i < playList.size(); i++)
                songsToShuffle.add(i);

            Collections.shuffle(songsToShuffle);

            for (int i = 0; i < songsToShuffle.size(); i++)
                shuffleIndices.add(songsToShuffle.get(i));

            if (shuffleIndices.size() > 0) {
                queueIndexer = new QueueIndexerShuffle();
                ((QueueIndexerShuffle) queueIndexer).init(currentSongIndex, shuffleIndices, this);

                if (allowTextMessages) {
                    Resources res = this.getResources();
                    if (res != null) {
                        int num = songsToShuffle.size();
                        final String message = this.getResources().getQuantityString(
                                R.plurals.x_items_shuffled, num, num);
                        notifyMessage(message);
                    }
                }
            }

        }

        notifyQueueChange();

        onShuffleModeChanged.invoke(this.shuffleMode);
    }

    public int getShuffleMode() {
        return shuffleMode;
    }

    public void previewOpen(List<PlaylistSong> list, int startPlayPosition) {

    }

    @Override
    public void onQueueIndexesChanged(IQueueIndexer indexer, boolean eventFromOnQueueChanged, boolean currentSongIndexChanged) {
        int count = playList.size();
        for (int i = 0; i < count; i++) {
            ((QueueItemIdentifier) playList.get2(i)).setQueueIndex(-1);
        }

        int cnt = indexer.getQueueIndexCount(playList.size());

        for (int i = 0; i < cnt; i++) {
            int songIndex = indexer.getSongIndexByQueueIndex(i, playList.size());
            if (songIndex < playList.size())
                ((QueueItemIdentifier) playList.get2(songIndex)).setQueueIndex(i);
        }

        notifyQueueChange();
        if (currentSongIndexChanged) {
            final int posFinal = queueIndexer.getCurrentSongIndex(true);
            onQueuePosChanged(posFinal, false, false, null);
        }
    }

    private int addToPlayList(Collection<PlaylistSong> list, int position, boolean clear, IPlaylistSongContainerIdentifier songContainerIdentifier) {
        if (clear) {
            playList.clear();
            position = 0;
        }

        if (position > playList.size()) {
            position = playList.size();
        }

        List<IItemIdentifier> list2 = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); ++i)
            list2.add(i, new QueueItemIdentifier());

        playList.addAll(position, list, list2);

        if (clear)
            onQueueChanged2(position, position + list.size(), 0, false, songContainerIdentifier, true);
        else
            onQueueChanged2(position, (position + list.size()) - 1, +1, false, null);

        return position;
    }

    public void enqueue(Collection<PlaylistSong> list, int action) {
        if (action == NEXT) {
            addToPlayList(list, queueIndexer.getCurrentSongIndex(true) + 1, false, null);
        } else {
            addToPlayList(list, Integer.MAX_VALUE, false, null);
            if (action == NOW) {
                queueIndexer.goTo(playList.size() - list.size());
            }
        }
    }

    public void removeQueueItems(List<IItemIdentifier> itemIdentifiers) {
        List<Integer> itemsIndexes = new ArrayList<>(itemIdentifiers.size());

        MultiList.ListIterator<PlaylistSong, IItemIdentifier> it;
        for (IItemIdentifier itemToRemove : itemIdentifiers) {

            it = playList.multiListIterator();
            while (it.hasNext()) {
                int i = it.nextIndex();
                Tuple2<PlaylistSong, IItemIdentifier> item = it.next();
                if (itemToRemove.equals(item.obj2)) {
                    itemsIndexes.add(i);
                    it.remove();
                }
            }
        }

        onQueueChanged22(itemsIndexes, -1, 0, false, null);
    }

    public int removeTracks(int first, int last) {
        if (last < first) return 0;
        if (first < 0) first = 0;
        if (last >= playList.size()) last = playList.size() - 1;

        int numRemoved;

        playList.subList(first, last + 1).clear();
        onQueueChanged2(first, last, -1, false, null);

        numRemoved = last - first + 1;

        return numRemoved;
    }

    public int removeTrack(PlaylistSong id) {
        int numRemoved = 0;

        for (final MultiList.ListIterator<PlaylistSong, IItemIdentifier> iterator = playList.multiListIterator(); iterator.hasNext(); ) {
            int index = iterator.nextIndex();
            final PlaylistSong o = iterator.next1();

            if (o.compare(id)) {
                iterator.remove();
                numRemoved++;

                onQueueChanged2(index, index, -1, false, null);
            }
        }

        return numRemoved;
    }

    public void swapQueueItem(int index1, int index2) {
        if (index1 >= playList.size()) {
            index1 = playList.size() - 1;
        }
        if (index2 >= playList.size()) {
            index2 = playList.size() - 1;
        }

        playList.swap(index1, index2);
        onQueueChanged2(index1, index2, 0, true, null);
    }

    public void moveQueueItems(int from, int to, List<Integer> itemOffsets) {
        Tuple2<PlaylistSong, IItemIdentifier>[] itemsToMove = new Tuple2[itemOffsets.size()];

        for (int i = 0; i < itemOffsets.size(); i++) {
            int index = itemOffsets.get(i);
            itemsToMove[i] = new Tuple2<>(playList.get1(from + index), playList.get2(from + index));
        }

        for (int i = itemOffsets.size() - 1; i >= 0; i--) {
            int index = itemOffsets.get(i);
            playList.remove(from + index);
        }

        for (int i = itemOffsets.size() - 1; i >= 0; i--) {
            int index = itemOffsets.get(i);
            playList.add(to + index, itemsToMove[i]);
        }

        onQueueChanged22(itemOffsets, to, from, false, null);
    }

    public List<PlaylistSong> getQueue1() {
        return playList.unmodifiableList1();
    }

    public MultiList<PlaylistSong, IItemIdentifier> getQueue() {
        return playList.unmodifiableList();
    }

    void onQueueChanged2(int first, int last, int sign, boolean swap, IPlaylistSongContainerIdentifier songContainerIdentifier) {
        onQueueChanged2(first, last, sign, swap, songContainerIdentifier, false);
    }

    void onQueueChanged2(int first, int last, int sign, boolean swap, IPlaylistSongContainerIdentifier songContainerIdentifier, boolean hintWasCleared) {
        this.songContainerIdentifier = songContainerIdentifier;

        if (first > last) return;
        if (first < 0) first = 0;
        if (last >= playList.size()) last = playList.size() - 1;

        if (hintWasCleared)
            setShuffleMode(SHUFFLE_NONE, true);

        boolean currentSongIndexChanged = queueIndexer.onQueueChanged(first, last, sign, swap, playList.size());

        if (!swap) {

            int numCount = (last - first) + 1;

            Resources res = this.getResources();

            if (res != null) {
                if (sign == 1) {
                    final String message = this.getResources().getQuantityString(
                            R.plurals.x_items_added_to_queue, numCount, numCount);
                    EventsGlobalTextNotifier.onTextMsg.invoke(message);
                } else if (sign == -1) {
                    final String message = this.getResources().getQuantityString(
                            R.plurals.x_items_removed_from_queue, numCount, numCount);
                    EventsGlobalTextNotifier.onTextMsg.invoke(message);
                }
            }
        }
        
        saveQueue(PlayerCore.s().getAppContext());
    }

    void onQueueChanged22(List<Integer> itemsIndex, int insertIndex, int removeIndex, boolean swap, IPlaylistSongContainerIdentifier songContainerIdentifier) {
        this.songContainerIdentifier = songContainerIdentifier;
        queueIndexer.onQueueChanged(itemsIndex, insertIndex, removeIndex, swap, playList.size());

    }

    public Tuple2<PlaylistSong, IItemIdentifier> getCurrentQueueEntry() {
        int currentSongIndex = queueIndexer == null ? 0 : queueIndexer.getCurrentSongIndex(true);

        return (currentSongIndex >= 0 && currentSongIndex < playList.size()) ?
                playList.get(currentSongIndex) : null;
    }

    public IPlaylistSongContainerIdentifier getSongContainerIdentifier() {
        return songContainerIdentifier;
    }

    public int getQueuePosition() {
        return queueIndexer.getCurrentSongIndex(true);
    }

    public void setQueuePosition(int pos) {
        setQueuePosition(pos, null);
    }

    public void setQueuePosition(int pos, Object params) {
        queueIndexer.setQueuePosBySongIndex(pos);
        onQueuePosChanged(pos, false, true, params);
    }

    public void setQueueItem(IItemIdentifier item, Object params) {
        if (item == null)
            return;

        int queuePos = findPlaylistEntryByItemIdent(item, item.getQueueIndex());
        setQueuePosition(queuePos, params);
    }

    private int findPlaylistEntryByItemIdent(IItemIdentifier itemIdent, int hintPossiblePos) {
        if (itemIdent == null)
            return -1;

        if (hintPossiblePos >= 0 && hintPossiblePos < playList.size()) {
            Tuple2<PlaylistSong, IItemIdentifier> item = playList.get(hintPossiblePos);
            if (item.obj2 != null && item.obj2.equals(itemIdent)) {
                return hintPossiblePos;
            }
        }

        for (MultiList.ListIterator<PlaylistSong, IItemIdentifier> it = playList.multiListIterator(); it.hasNext(); ) {
            int i = it.nextIndex();
            Tuple2<PlaylistSong, IItemIdentifier> item = it.next();

            if (item.obj2 != null && item.obj2.equals(itemIdent)) {
                return i;
            }
        }

        return -1;
    }

    public void onDataSaveTime(Context context) {
        saveQueue(context);
    }

    private void saveQueue(Context context) {
        if (playList == null || queueIndexer == null) return;
        SharedPreferences mPreferences = AppPreferences.createOrGetInstance().getPreferences(context);
        SharedPreferences.Editor ed = mPreferences.edit();

        int len = playList.size();
        org.json.JSONArray jsonQueue = new org.json.JSONArray();
        
        for (int i = 0; i < len; i++) {
            PlaylistSong song = playList.get1(i);
            String path = song.getConstrucPath();
            if (path == null) continue;
            
            org.json.JSONObject obj = new org.json.JSONObject();
            try {
                obj.put("path", path);
                obj.put("title", song.getResolvedTitle() != null ? song.getResolvedTitle() : "");
                obj.put("artist", song.getResolvedArtist() != null ? song.getResolvedArtist() : "");
                obj.put("duration", song.getResolvedDuration());
                obj.put("thumbnail", song.getResolvedThumbnail() != null ? song.getResolvedThumbnail() : "");
                jsonQueue.put(obj);
            } catch (Exception e) {}
        }

        ed.putString("queue_json", jsonQueue.toString());
        ed.putInt("curpos", queueIndexer.getQueueIndex());
        ed.putInt("shufflemode", shuffleMode);
        ed.commit();
    }

    public void reloadQueue() {
        Context context = PlayerCore.s().getAppContext();
        if (context == null) return;
        SharedPreferences mPreferences = AppPreferences.createOrGetInstance().getPreferences(context);

        playList.clear();

        String jsonQueueStr = AppPreferences.preferencesGetStringSafe(mPreferences, "queue_json", "");
        int pos = AppPreferences.preferencesGetIntSafe(mPreferences, "curpos", 0);

        if (!jsonQueueStr.isEmpty()) {
            try {
                org.json.JSONArray jsonQueue = new org.json.JSONArray(jsonQueueStr);
                for (int i = 0; i < jsonQueue.length(); i++) {
                    org.json.JSONObject obj = jsonQueue.getJSONObject(i);
                    String path = obj.optString("path", "");
                    if (path.isEmpty() || path.equals("/") || path.startsWith("file://")) {
                        if (i < pos) pos--;
                        continue;
                    }
                    PlaylistSong song;
                    String title = obj.optString("title", "");
                    String artist = obj.optString("artist", "");
                    if (!title.isEmpty() || !artist.isEmpty()) {
                        song = new PlaylistSong(-1, path, title, artist, obj.optInt("duration", 0), obj.optString("thumbnail", ""));
                    } else {
                        song = new PlaylistSong(-1, path);
                    }
                    playList.add(song, new QueueItemIdentifier());
                }
            } catch (Exception e) {}
        } else {
            // Legacy format migration
            String q = AppPreferences.preferencesGetStringSafe(mPreferences, "queue", "");
            String queueSizes = AppPreferences.preferencesGetStringSafe(mPreferences, "queueSizes", "");
            String meta = AppPreferences.preferencesGetStringSafe(mPreferences, "queue_metadata", "");

            if (q != null && !q.isEmpty() && queueSizes != null && !queueSizes.isEmpty()) {
                String[] sizesStr = queueSizes.split(",");
                org.json.JSONArray metadataArray = null;
                try {
                    if (meta != null && !meta.isEmpty()) {
                        metadataArray = new org.json.JSONArray(meta);
                    }
                } catch (Exception e) {}

                int posstart = 0;
                int skippedBeforePos = 0;
                for (int i = 0; i < sizesStr.length; i++) {
                    String sizestr = sizesStr[i];
                    if (sizestr.isEmpty()) continue;
                    int s = Utils.strToIntSafe(sizestr);
                    if (posstart + s > q.length()) break;
                    String constructPath = q.substring(posstart, posstart + s);

                    if (constructPath == null || constructPath.trim().isEmpty()) {
                        posstart += s;
                        if (i < pos) skippedBeforePos++;
                        continue;
                    }
                    String cp = constructPath.trim().replaceAll("[\\u0000-\\u001F]", "");
                    if (cp.equals("/") || (cp.startsWith("file://") && cp.length() < 12)) {
                        posstart += s;
                        if (i < pos) skippedBeforePos++;
                        continue;
                    }

                    String providedTitle = null;
                    String providedArtist = null;
                    int providedDuration = 0;
                    String providedThumbnail = null;

                    if (metadataArray != null && i < metadataArray.length()) {
                        try {
                            org.json.JSONObject metaObj = metadataArray.getJSONObject(i);
                            providedTitle = metaObj.optString("title", "");
                            providedArtist = metaObj.optString("artist", "");
                            providedDuration = metaObj.optInt("duration", 0);
                            providedThumbnail = metaObj.optString("thumbnail", "");
                        } catch (Exception e) {}
                    }

                    PlaylistSong song;
                    if ((providedTitle != null && !providedTitle.isEmpty()) || (providedArtist != null && !providedArtist.isEmpty())) {
                        song = new PlaylistSong(-1, constructPath, providedTitle, providedArtist, providedDuration, providedThumbnail);
                    } else {
                        song = new PlaylistSong(-1, constructPath);
                    }

                    playList.add(song, new QueueItemIdentifier());
                    posstart += s;
                }
                pos -= skippedBeforePos;
                if (pos < 0) pos = 0;
            }
        }

        if (playList.size() == 0) {
            List<PlaylistSong> initalSongs = UtilsMusic.getMostRecentTrackListByCount(context, -1);
            for (PlaylistSong s : initalSongs)
                playList.add(s, new QueueItemIdentifier());
        }

        if (pos >= playList.size()) pos = playList.size() > 0 ? playList.size() - 1 : 0;
        queueIndexer.goTo(pos);

        int shufmode = AppPreferences.preferencesGetIntSafe(mPreferences, "shufflemode", SHUFFLE_NONE);
        if (shufmode != SHUFFLE_NORMAL) {
            shufmode = SHUFFLE_NONE;
        }

        setShuffleMode(shufmode, false, true);

        onQueueChanged2(0, playList.size(), 0, false, null);
    }

    public void open(List<PlaylistSong> list, int startPlayPosition, int addAction, IPlaylistSongContainerIdentifier songContainerIdentifier, Object params) {
        int addPosition = -1;

        if (addAction == CLEAR) {
            addPosition = -1;
        } else if (addAction == FIRST) {
            addPosition = 0;
        } else if (addAction == NOW) {

            addPosition = Integer.MAX_VALUE;
        } else if (addAction == NEXT) {
            addPosition = queueIndexer.getCurrentSongIndex(true) + 1;
        } else if (addAction == LAST) {
            addPosition = Integer.MAX_VALUE;
        }

        int addedToPosition = addToPlayList(list, addPosition, addPosition < 0, songContainerIdentifier);

        if (startPlayPosition >= 0)
            queueIndexer.goTo(startPlayPosition + addedToPosition);

        final int posFinal = queueIndexer.getCurrentSongIndex(true);
        onQueuePosChanged(posFinal, false, true, params);
    }

    public boolean isNextPlaylistEnd() {
        int nextIndex = queueIndexer.getNextSongIndex(false);

        return nextIndex == -1 || nextIndex >= playList.size();
    }

    public void playFirst(Object params) {

        queueIndexer.goToStart();

        int pos = queueIndexer.getCurrentSongIndex(true);
        onQueuePosChanged(pos, false, true, params);

    }

    public void playCurrent(Object params) {

        int pos = queueIndexer.getCurrentSongIndex(true);
        onQueuePosChanged(pos, false, true, params);
    }

    public void prev(Object params) {
        queueIndexer.goToPrev();

        int pos = queueIndexer.getCurrentSongIndex(true);
        onQueuePosChanged(pos, false, true, params);

    }

    public void next(Object params) {

        boolean playlistEnd = queueIndexer.goToNext(playList.size());
        int pos = queueIndexer.getCurrentSongIndex(true);

        onQueuePosChanged(pos, playlistEnd, true, params);

    }

    public void nextOrFirst(Object params) {

        if (isNextPlaylistEnd())
            playFirst(params);
        else
            next(params);
    }

    public void open(List<PlaylistSong> list, int startPlayPosition, int addAction, IPlaylistSongContainerIdentifier songContainerIdentifier) {
        open(list, startPlayPosition, addAction, songContainerIdentifier, null);
    }

    public void setQueueItem(IItemIdentifier item) {
        setQueueItem(item, null);
    }

    public void playFirst() {
        playFirst(null);
    }

    public void playCurrent() {
        playCurrent(null);
    }

    public void prev() {
        prev(null);
    }

    public void next() {
        next(null);
    }

    public void nextOrFirst() {
        nextOrFirst(null);
    }

    void onQueuePosChanged(final int songIndex, final boolean playlistEnd, final boolean activeChange, final Object params) {

        final Tuple2<PlaylistSong, IItemIdentifier> queueEntry =
                (songIndex >= 0 && songIndex < playList.size()) ?
                        playList.get(songIndex) : null;

        // Pre-resolve upcoming YouTube tracks to eliminate 3-second delay on switch
        int nextTracksToResolve = 3;
        for (int i = 1; i <= nextTracksToResolve; i++) {
            int nextIndex = songIndex + i;
            if (nextIndex < playList.size()) {
                PlaylistSong nextSong = playList.get1(nextIndex);
                if (nextSong != null && nextSong.getConstrucPath() != null) {
                    Context context = PlayerCore.s().getAppContext();
                    if (context != null) {
                        com.aylis.comp.playback.ExoMediaPlayer.YoutubeResolver.preResolve(context, nextSong.getConstrucPath());
                    }
                }
            }
        }

        onQueuePosChanged.invoke(queueEntry, songIndex, playlistEnd, activeChange, params);
        saveQueue(PlayerCore.s().getAppContext());
    }

     static int fixQueueIndex_(int queueIndex, int first, int last, int sign, boolean swap) {

        if (swap) {
            if (queueIndex == last)
                return first;
            else if (queueIndex == first)
                return last;

            return queueIndex;
        }

        if (queueIndex < first) {

            return queueIndex;
        } else {

            int count = (last - first) + 1;
            int newpos = queueIndex + (sign * count);

            if (newpos <= first)
                newpos = -1;

            return newpos;
        }

    }

    static int fixQueueIndexSingle(int queueIndex, int first, int sign) {
        if (queueIndex < first) {

            return queueIndex;
        } else {
            int newpos = queueIndex + sign;

            if (newpos <= first)
                newpos = -1;

            return newpos;
        }
    }

    static int fixRemovedQueueIndexSingle(int queueIndex, int removeIndex) {
        List<Integer> itemsIndex = new ArrayList<>();
        itemsIndex.add(0);
        return fixRemovedQueueIndex(queueIndex, itemsIndex, removeIndex);
    }

    static int fixRemovedQueueIndex(int queueIndex, List<Integer> itemsIndex, int removeIndex) {
        int index;

        for (index = itemsIndex.size() - 1; index >= 0; index--) {
            if (index + removeIndex == queueIndex) {
                queueIndex--;
            }
        }

        if (queueIndex < 0) queueIndex = 0;

        return queueIndex;
    }

    static int fixQueueIndex(int queueIndex, List<Integer> itemsIndex, int insertIndex, int removeIndex, boolean swap) {

        if (swap) {
            for (Integer index : itemsIndex) {
                if (index + insertIndex == queueIndex)
                    return index + removeIndex;

                if (index + removeIndex == queueIndex)
                    return index + insertIndex;
            }

            return queueIndex;
        }

        int newPos = queueIndex;
        if (removeIndex >= 0) {
            for (Integer index : itemsIndex) {
                if (index + removeIndex < queueIndex)
                    newPos--;

                if (index + removeIndex == queueIndex) {
                    if (insertIndex >= 0)
                        return index + insertIndex;
                    else
                        return -1;
                }
            }
        }

        if (insertIndex >= 0) {
            for (Integer index : itemsIndex) {
                if (index + insertIndex <= queueIndex)
                    newPos++;
            }
        }

        return newPos;
    }
}

