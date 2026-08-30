

package com.aylis.Design;

import android.content.Intent;

import android.content.Context;
import android.os.Handler;
import com.aylis.AppPermissions;
import com.aylis.Common.Events.WeakEvent1;
import com.aylis.Common.Events.WeakEvent2;
import com.aylis.Common.Events.WeakEventR;
import com.aylis.Common.MultiList;
import com.aylis.Common.Tuple2;
import com.aylis.Common.tlog;
import com.aylis.comp.AppPreferences.AppPreferences;
import com.aylis.comp.Common.IItemIdentifier;
import com.aylis.comp.PlaybackQueue.IPlaylistSongContainerIdentifier;
import com.aylis.comp.playback.EventsPlaybackService;
import com.aylis.comp.playback.MediaPlaybackService;
import com.aylis.comp.PlaybackQueue.QueueCore;
import com.aylis.MainActivity;
import com.aylis.PlayerCore;
import com.aylis.comp.playback.Song.PlaylistSong;
import java.util.LinkedList;
import java.util.List;

public class GeneralDesign {

    public static boolean isFirstLaunch = false;
    public static boolean shouldLoadInitalSongs = false;
    public static boolean shouldLoadInitalSongMediaService = false;
    private List<Object> listenerRefHolder = new LinkedList<>();
    private Handler threadHandler = new Handler();
    private boolean gotOnContext = false;

    public GeneralDesign() {

        MainActivity.onStart.subscribeWeak(new WeakEvent1.Handler<Context>() {
            @Override
            public void invoke(Context context) {
            }
        }, listenerRefHolder);

        MainActivity.onCreateEarly.subscribeWeak(new WeakEvent1.Handler<Context>() {
            @Override
            public void invoke(Context context) {
                onContext(context);
            }
        }, listenerRefHolder);

        MediaPlaybackService.onCreateEarly.subscribeWeak(new WeakEvent1.Handler<Context>() {
            @Override
            public void invoke(final Context context) {
                threadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onContext(context);
                    }
                });
            }
        }, listenerRefHolder);

        QueueCore.onRequestShouldReloadInitalSongs.subscribeWeak(new WeakEventR.Handler<Boolean>() {
            @Override
            public Boolean invoke() {
                if (!gotOnContext)
                    onContext(PlayerCore.s().getAppContext());

                boolean result = shouldLoadInitalSongs;
                shouldLoadInitalSongs = false;
                return result;
            }
        }, listenerRefHolder);

        QueueCore.onQueueStateChanged.subscribeWeak(new WeakEvent2.Handler<MultiList<PlaylistSong, IItemIdentifier>, IPlaylistSongContainerIdentifier>() {
            @Override
            public void invoke(MultiList<PlaylistSong, IItemIdentifier> list, IPlaylistSongContainerIdentifier songContainerIdentifier) {

                if(shouldLoadInitalSongMediaService) {
                    Tuple2<PlaylistSong, IItemIdentifier> queueEntry = QueueCore.createOrGetInstance().getCurrentQueueEntry();
                    if (queueEntry == null && list.size() > 0) {
                        queueEntry = list.get(0);
                    }
                    if (queueEntry != null) {
                        if (queueEntry.obj1 != null) {
                            String dataSource = queueEntry.obj1.getConstrucPath();
                            EventsPlaybackService.Receive.onPlayDataSource.invoke(dataSource, false, 0L, (Long) null);
                        }
                    }

                    shouldLoadInitalSongMediaService = false;
                }
            }
        }, listenerRefHolder);

        MainActivity.onRequestPermissionsResult.subscribeWeak(new WeakEvent1.Handler<Integer>() {
            @Override
            public void invoke(Integer request) {
                EventsPlaybackService.Receive.onRestartMediaPlayerCore.invoke();
                shouldLoadInitalSongMediaService = shouldLoadInitalSongs = isFirstLaunch;
                if (request == AppPermissions.REQUEST_STORAGE)
                    QueueCore.createOrGetInstance().reloadQueue();
            }
        }, listenerRefHolder);
    }

    private void onContext(Context context) {

        gotOnContext = true;

        if (context != null) {
            AppPreferences appPreferences = AppPreferences.createOrGetInstance();
            appPreferences.load(context);

            if (!isFirstLaunch) {
                isFirstLaunch = appPreferences.getBool(AppPreferences.PREF_Bool_firstLaunch);
                tlog.notice("isFirstLaunch: " + isFirstLaunch);
                appPreferences.setBool(AppPreferences.PREF_Bool_firstLaunch, false);

                shouldLoadInitalSongMediaService = shouldLoadInitalSongs = isFirstLaunch;
            }
        }

    }
}

