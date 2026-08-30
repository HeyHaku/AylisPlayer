

package com.aylis.Design;

import android.os.Handler;

import androidx.fragment.app.FragmentManager;
import com.aylis.Common.Events.WeakEvent;
import com.aylis.Common.Events.WeakEvent2;
import com.aylis.Common.Events.WeakEvent3;
import com.aylis.Common.Events.WeakEventR;
import com.aylis.comp.playback.EventsPlaybackService;
import com.aylis.comp.SleepTimer.SleepTimer;
import com.aylis.comp.SleepTimer.SleepTimerConfig;
import com.aylis.comp.SleepTimer.SleepTimerDialog;
import com.aylis.ContextData;
import com.aylis.MainActivity;
import java.util.LinkedList;
import java.util.List;

public class SleepTimerDesign {

    private List<Object> listenerRefHolder = new LinkedList<>();

    public SleepTimerDesign() {

        MainActivity.onMainUIAction.subscribeWeak(new WeakEvent2.Handler<Integer, ContextData>() {
            @Override
            public void invoke(Integer id, ContextData contextData) {
                FragmentManager fragmentManager = contextData.getFragmentManager();
                if (fragmentManager == null) return;

                if (id == 1) {
                    SleepTimerDialog.createAndShowSleepTimerDialog(fragmentManager);
                }

            }
        }, listenerRefHolder);

        MainActivity.onMainUIRequestSleepTimerConfig.subscribeWeak(new WeakEventR.Handler<SleepTimerConfig>() {
            @Override
            public SleepTimerConfig invoke() {
                SleepTimer sleepTimer = SleepTimer.createOrGetInstance();
                if (sleepTimer == null) return new SleepTimerConfig();

                return sleepTimer.getConfig();
            }
        }, listenerRefHolder);

        SleepTimer.onSleepTimerConfigChanged.subscribeWeak(new WeakEvent3.Handler<Boolean, Integer, Boolean>() {
            @Override
            public void invoke(Boolean enabled, Integer minutes, Boolean playLastSongToEnd) {
                final SleepTimerConfig config = new SleepTimerConfig();
                config.enabled = enabled;
                config.minutes = minutes;
                config.playLastSongToEnd = playLastSongToEnd;

                MainActivity mainActivity = MainActivity.getInstance();
                if (mainActivity != null)
                    mainActivity.updateSleepTimerIndicator(config.enabled, false);

            }
        }, listenerRefHolder);

        SleepTimer.onSleepTimerFire.subscribeWeak(new WeakEvent.Handler() {
            @Override
            public void invoke() {
                EventsPlaybackService.Receive.onAction.invoke(EventsPlaybackService.Receive.ACTION_Stop);
            }
        }, listenerRefHolder);

        SleepTimerDialog.onSleepTimerUISubmit.subscribeWeak(new WeakEvent3.Handler<Boolean, Integer, Boolean>() {
            @Override
            public void invoke(Boolean enabled, Integer minutes, Boolean playLastSongToEnd) {
                SleepTimer sleepTimer = SleepTimer.createOrGetInstance();
                if (sleepTimer == null) return;

                sleepTimer.configure(enabled, minutes, playLastSongToEnd);
            }
        }, listenerRefHolder);

        SleepTimerDialog.onSleepTimerUIRequestSleepTimerConfig.subscribeWeak(new WeakEventR.Handler<SleepTimerConfig>() {
            @Override
            public SleepTimerConfig invoke() {
                SleepTimer sleepTimer = SleepTimer.createOrGetInstance();
                if (sleepTimer == null) return new SleepTimerConfig();

                return sleepTimer.getConfig();
            }
        }, listenerRefHolder);

        SleepTimerDialog.onSleepTimerUIRequestRemainingSeconds.subscribeWeak(new WeakEventR.Handler<Integer>() {
            @Override
            public Integer invoke() {
                SleepTimer sleepTimer = SleepTimer.createOrGetInstance();
                if (sleepTimer == null) return 0;

                return sleepTimer.getRemainingSeconds();
            }
        }, listenerRefHolder);
    }

}

