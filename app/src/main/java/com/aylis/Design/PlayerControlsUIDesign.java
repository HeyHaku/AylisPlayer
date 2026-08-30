

package com.aylis.Design;

import android.os.Handler;

import android.content.Context;
import android.view.View;
import com.aylis.comp.LibraryQueueUI.LibraryQueueFragmentBase;
import com.aylis.Common.Events.WeakDelegate2;
import com.aylis.Common.Events.WeakDelegate3;
import com.aylis.Common.Events.WeakEvent;
import com.aylis.Common.Events.WeakEvent1;
import com.aylis.Common.Events.WeakEventR;
import com.aylis.comp.MediaControlsUI.MediaControlsUI;
import com.aylis.comp.playback.EventsPlaybackService;
import com.aylis.comp.PlaybackQueue.QueueCore;
import com.aylis.MainActivity;
import com.aylis.PlayerCore;
import java.util.LinkedList;
import java.util.List;

public class PlayerControlsUIDesign {

    private List<Object> listenerRefHolder = new LinkedList<>();

    public PlayerControlsUIDesign() {

        MediaControlsUI.onPlaybackPrev.subscribeWeak(new WeakEvent.Handler() {
            @Override
            public void invoke() {
                QueueCore playbackQueue = QueueCore.createOrGetInstance();
                if (playbackQueue != null)
                    playbackQueue.prev();
            }
        }, listenerRefHolder);

        MediaControlsUI.onPlaybackNext.subscribeWeak(new WeakEvent.Handler() {
            @Override
            public void invoke() {
                QueueCore playbackQueue = QueueCore.createOrGetInstance();
                if (playbackQueue != null)
                    playbackQueue.nextOrFirst();
            }
        }, listenerRefHolder);

        MediaControlsUI.onPlaybackTogglePause.subscribeWeak(new WeakEvent.Handler() {
            @Override
            public void invoke() {
                EventsPlaybackService.Receive.onAction.invoke(EventsPlaybackService.Receive.ACTION_TogglePause);
            }
        }, listenerRefHolder);

        MediaControlsUI.onRequestTrackPosition.subscribeWeak(new WeakEventR.Handler<Long>() {
            @Override
            public Long invoke() {
                return PlaybackDesign.trackPosition;
            }
        }, listenerRefHolder);

        MediaControlsUI.onSetTrackPosition.subscribeWeak(new WeakEvent1.Handler<Long>() {
            @Override
            public void invoke(final Long trackPosition) {
                EventsPlaybackService.Receive.onSeekChanged.invoke(trackPosition);
            }
        }, listenerRefHolder);

        MediaControlsUI.onRequestShowState.subscribeWeak(new WeakEventR.Handler<Integer>() {
            @Override
            public Integer invoke() {

                MainActivity mainActivity = MainActivity.getInstance();
                int pagePosition = MainActivity.LIBRARY_PAGE_INDEX;
                if (mainActivity != null)
                    pagePosition = mainActivity.currentFragmentPage;

                return getPlayerControlsShowState(true, pagePosition);
            }
        }, listenerRefHolder);

        MediaControlsUI.onRequestShuffleMode.subscribeWeak(new WeakEventR.Handler<Integer>() {
            @Override
            public Integer invoke() {
                QueueCore playbackQueue = QueueCore.createOrGetInstance();
                if (playbackQueue != null)
                    return playbackQueue.getShuffleMode();
                return 0;
            }
        }, listenerRefHolder);

        MediaControlsUI.onSetShuffleMode.subscribeWeak(new WeakEvent1.Handler<Integer>() {
            @Override
            public void invoke(final Integer shuffleMode) {

                QueueCore playbackQueue = QueueCore.createOrGetInstance();
                if (playbackQueue != null)
                    playbackQueue.setShuffleMode(shuffleMode, true);
            }
        }, listenerRefHolder);

        QueueCore.onShuffleModeChanged.subscribeWeak(new WeakEvent1.Handler<Integer>() {
            @Override
            public void invoke(Integer shuffleMode) {

                MediaControlsUI mediaControlsUI = MediaControlsUI.getInstance();
                if (mediaControlsUI != null)
                    mediaControlsUI.onShuffleModeChanged(shuffleMode);

                LibraryQueueFragmentBase.onShuffleModeChanged(shuffleMode);

            }
        }, listenerRefHolder);

        MainActivity.onFullscreenChanged.subscribeWeak(new WeakEvent1.Handler<Boolean>() {
            @Override
            public void invoke(Boolean fullScreen) {

                MediaControlsUI mediaControlsUI = MediaControlsUI.getInstance();

                if (!fullScreen) {

                    MainActivity mainActivity = MainActivity.getInstance();
                    int pagePosition = MainActivity.LIBRARY_PAGE_INDEX;
                    if (mainActivity != null)
                        pagePosition = mainActivity.currentFragmentPage;

                    if (mediaControlsUI != null) mediaControlsUI.animateShow(2);

                } else {
                    if (mediaControlsUI != null) mediaControlsUI.animateShow(0);
                }

            }
        }, listenerRefHolder);

        MainActivity.onCreateView.subscribeWeak(new WeakDelegate2.Handler<View, View>() {
            @Override
            public void invoke(View view, View viewBg) {

                MediaControlsUI mediaControlsUI = MediaControlsUI.getInstance();
                if (mediaControlsUI != null) {
                    mediaControlsUI.onCreateView(view, viewBg);
                }
            }
        }, listenerRefHolder);
    }

    int getPlayerControlsShowState(boolean show, int pagePosition) {

        Context context = PlayerCore.s().getAppContext();
        if (context == null) return 2;

        if (show) {
            return 2;
        } else {
            return 0;
        }
    }
}

