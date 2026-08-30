

package com.aylis.Design;

import android.os.Handler;

import com.aylis.Common.Events.WeakEvent5;
import com.aylis.Common.Tuple2;
import com.aylis.comp.Common.IItemIdentifier;
import com.aylis.comp.LibraryQueueUI.FragmentLibrary;
import com.aylis.comp.playback.EventsPlaybackService;
import com.aylis.comp.playback.Song.PlaylistSong;
import com.aylis.comp.PlaybackQueue.QueueCore;
import com.aylis.MainActivity;
import com.aylis.PlayerCore;
import java.util.LinkedList;
import java.util.List;

public class PlaybackControlsDesign {

    public static volatile PlaylistSong currentTrack = PlaylistSong.EmptySong;
    public static volatile IItemIdentifier currentItemIdent = null;
    public static volatile PlaylistSong.Data fieldsongData = PlaylistSong.emptyData;
    public static volatile int fieldQueuePosition = -1;

    private List<Object> listenerRefHolder = new LinkedList<>();

    public PlaybackControlsDesign() {

        fieldQueuePosition = QueueCore.createOrGetInstance().getQueuePosition();
        Tuple2<PlaylistSong, IItemIdentifier> queueEntry = QueueCore.createOrGetInstance().getCurrentQueueEntry();
        if (queueEntry != null) {
            currentTrack = queueEntry.obj1;
            currentItemIdent = queueEntry.obj2;
            fieldsongData = currentTrack.getDataBlocking(PlayerCore.s().getAppContext());
        }

        QueueCore.onQueuePosChanged.subscribeWeak(new WeakEvent5.Handler<Tuple2<PlaylistSong, IItemIdentifier>, Integer, Boolean, Boolean, Object>() {
            @Override
            public void invoke(Tuple2<PlaylistSong, IItemIdentifier> queueEntry, Integer songIndex, Boolean playlistEnd, Boolean activeChange, Object params) {
                PlaylistSong song = null;
                IItemIdentifier itemIdentifier = null;
                if (queueEntry != null) {
                    song = queueEntry.obj1;
                    itemIdentifier = queueEntry.obj2;
                }
                if (song == null) song = PlaylistSong.EmptySong;

                PlaylistSong.Data data = song.getDataBlocking(PlayerCore.s().getAppContext());

                currentItemIdent = itemIdentifier;
                fieldQueuePosition = songIndex;
                currentTrack = song;
                fieldsongData = data;

                FragmentLibrary FragmentLibrary = MainActivity.getFragmentLibraryInstance();
                if (FragmentLibrary != null) FragmentLibrary.updateTrackInfo();

                if (activeChange) {
                    if (!playlistEnd) {
                        String dataSource = "";
                        if (queueEntry != null) {
                            if (queueEntry.obj1 != null)
                                dataSource = queueEntry.obj1.getConstrucPath();
                        }
                        if (dataSource != null && !dataSource.trim().isEmpty()) {
                            EventsPlaybackService.Receive.onPlayDataSource.invoke(dataSource, true, 0L, (Long) params);
                        }
                    } else {
                        EventsPlaybackService.Receive.onAction.invoke(EventsPlaybackService.Receive.ACTION_Stop);
                    }
                }
            }
        }, listenerRefHolder);
    }

}

