

package com.aylis.comp.playback;

import android.os.Handler;

import android.app.Service;

import com.aylis.comp.visual.core.playback.AudioFrameData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import android.view.SurfaceHolder;
import com.aylis.Common.Events.WeakDelegate;
import com.aylis.Common.Events.WeakDelegate1;
import com.aylis.Common.Events.WeakDelegate4;
import com.aylis.Common.Events.WeakDelegateR;
import com.aylis.Common.Events.WeakDelegateR2;
import com.aylis.PlayerCore;
import java.util.LinkedList;
import java.util.List;

public class EventsPlaybackService {

    public static class Receive {

        public static final int ACTION_Play = 1;
        public static final int ACTION_Pause = 2;
        public static final int ACTION_Stop = 3;
        public static final int ACTION_TogglePause = 4;

        static List<Object> listenerRefHolder = new LinkedList<>();

        public static WeakDelegate onRequestCloseService = new WeakDelegate().subscribeWeak(new WeakDelegate.Handler() {
            @Override
            public void invoke() {
                startService(MediaPlaybackServiceDefs.EXIT_ACTION);
            }
        }, listenerRefHolder);

        public static WeakDelegateR2<AudioFrameData  , Boolean  , AudioFrameData> getVisualizationData =
                new WeakDelegateR2<AudioFrameData, Boolean, AudioFrameData>().subscribeWeak(new WeakDelegateR2.Handler<AudioFrameData, Boolean, AudioFrameData>() {
                    @Override
                    public AudioFrameData invoke(AudioFrameData outResult, Boolean useGlobalSession) {
                        MediaPlaybackService service = MediaPlaybackService.getInstance();
                        if (service != null)
                            return service.getVisualizationData(outResult, useGlobalSession);
                        return null;
                    }
                }, listenerRefHolder);

        public static WeakDelegate4<String  , Boolean  , Long  , Long  > onPlayDataSource =
                new WeakDelegate4<String, Boolean, Long, Long>().subscribeWeak(new WeakDelegate4.Handler<String, Boolean, Long, Long>() {
                    @Override
                    public void invoke(String dataSource, Boolean start, Long seekPos, Long atTime) {
                        Intent intent = new Intent(MediaPlaybackServiceDefs.PLAY_DATA_SOURCE_ACTION);
                        intent.putExtra(MediaPlaybackServiceDefs.EXTRA_ARG_1, dataSource);
                        intent.putExtra(MediaPlaybackServiceDefs.EXTRA_ARG_2, start);
                        intent.putExtra(MediaPlaybackServiceDefs.EXTRA_ARG_3, seekPos);
                        intent.putExtra(MediaPlaybackServiceDefs.EXTRA_ARG_4, atTime == null ? ((long) 0) : atTime);

                        startService(intent);
                    }
                }, listenerRefHolder);

        public static WeakDelegate1<Integer  > onAction = new WeakDelegate1<Integer>().subscribeWeak(new WeakDelegate1.Handler<Integer>() {
            @Override
            public void invoke(Integer action) {
                switch (action) {
                    case EventsPlaybackService.Receive.ACTION_Play:
                        startService(MediaPlaybackServiceDefs.PLAY_ACTION);
                        break;

                    case EventsPlaybackService.Receive.ACTION_Pause:
                        startService(MediaPlaybackServiceDefs.PAUSE_ACTION);
                        break;

                    case EventsPlaybackService.Receive.ACTION_Stop:
                        startService(MediaPlaybackServiceDefs.STOP_ACTION);
                        break;
                    case EventsPlaybackService.Receive.ACTION_TogglePause:
                        startService(MediaPlaybackServiceDefs.TOGGLE_PAUSE_ACTION);
                        break;
                }
            }
        }, listenerRefHolder);

        public static WeakDelegate onRequestStateRefresh = new WeakDelegate().subscribeWeak(new WeakDelegate.Handler() {
            @Override
            public void invoke() {
                startService();
            }
        }, listenerRefHolder);

        public static WeakDelegate1<Boolean  > onTimeoutChange = new WeakDelegate1<Boolean>().subscribeWeak(new WeakDelegate1.Handler<Boolean>() {
            @Override
            public void invoke(Boolean enabled) {
                MediaPlaybackService service = MediaPlaybackService.getInstance();

                if (enabled) {
                    if (service != null)
                        service.setTimeoutEnableThSafe(true);
                } else {
                    startService(MediaPlaybackServiceDefs.TIMEOUT_DISABLE_ACTION);
                }
            }
        }, listenerRefHolder);
        public static WeakDelegate1<Long  > onSeekChanged = new WeakDelegate1<Long>().subscribeWeak(new WeakDelegate1.Handler<Long>() {
            @Override
            public void invoke(Long position) {
                startService(MediaPlaybackServiceDefs.SEEK_ACTION, position);
            }
        }, listenerRefHolder);

        public static WeakDelegate1<Integer  > onRepeatModeChange = new WeakDelegate1<Integer>().subscribeWeak(new WeakDelegate1.Handler<Integer>() {
            @Override
            public void invoke(Integer repeatMode) {
                startService(MediaPlaybackServiceDefs.REPEAT_MODE_ACTION, repeatMode);
            }
        }, listenerRefHolder);

        public static WeakDelegate1<Integer  > setVideoScalingMode = new WeakDelegate1<Integer>().subscribeWeak(new WeakDelegate1.Handler<Integer>() {
            @Override
            public void invoke(Integer mode) {
                startService(MediaPlaybackServiceDefs.VIDEO_SCALING_MODE_ACTION, mode);
            }
        }, listenerRefHolder);
        public static WeakDelegate1<SurfaceHolder  > setVideoSurfaceHolder = new WeakDelegate1<SurfaceHolder>().subscribeWeak(new WeakDelegate1.Handler<SurfaceHolder>() {
            @Override
            public void invoke(SurfaceHolder surfaceHolder) {
                MediaPlaybackService service = MediaPlaybackService.getInstance();
                if (service != null)
                    service.setVideoSurfaceHolderThSafe(surfaceHolder);
            }
        }, listenerRefHolder);

        public static WeakDelegate1<Float  > setVolumePercentage = new WeakDelegate1<Float>().subscribeWeak(new WeakDelegate1.Handler<Float>() {
            @Override
            public void invoke(Float value) {
                startService(MediaPlaybackServiceDefs.VOLUME_PERCENTAGE_ACTION, value);
            }
        }, listenerRefHolder);
        public static WeakDelegate1<Integer  > setVolume = new WeakDelegate1<Integer>().subscribeWeak(new WeakDelegate1.Handler<Integer>() {
            @Override
            public void invoke(Integer value) {
                startService(MediaPlaybackServiceDefs.VOLUME_ACTION, value);
            }
        }, listenerRefHolder);

        public static WeakDelegateR<Float> getVolumePercentage = new WeakDelegateR<Float>().subscribeWeak(new WeakDelegateR.Handler<Float>() {
            @Override
            public Float invoke() {
                MediaPlaybackService service = MediaPlaybackService.getInstance();
                if (service != null)
                    return service.getVolumePercentage();
                else
                    return 0.0f;
            }
        }, listenerRefHolder);
        public static WeakDelegateR<Integer> getVolume = new WeakDelegateR<Integer>().subscribeWeak(new WeakDelegateR.Handler<Integer>() {
            @Override
            public Integer invoke() {
                MediaPlaybackService service = MediaPlaybackService.getInstance();
                if (service != null)
                    return service.getVolume();
                else
                    return 0;
            }
        }, listenerRefHolder);

        public static WeakDelegate1<Boolean  > setVolumeMute = new WeakDelegate1<Boolean>().subscribeWeak(new WeakDelegate1.Handler<Boolean>() {
            @Override
            public void invoke(Boolean muted) {
                startService(MediaPlaybackServiceDefs.SET_MUTE_ACTION, muted);
            }
        }, listenerRefHolder);
        public static WeakDelegate toggleVolumeMute = new WeakDelegate().subscribeWeak(new WeakDelegate.Handler() {
            @Override
            public void invoke() {
                startService(MediaPlaybackServiceDefs.TOGGLE_MUTE_ACTION);
            }
        }, listenerRefHolder);

        public static WeakDelegate1<Float  > setVolumeStereoBalance = new WeakDelegate1<Float>().subscribeWeak(new WeakDelegate1.Handler<Float>() {
            @Override
            public void invoke(Float value) {
                startService(MediaPlaybackServiceDefs.VOLUME_STEREO_BALANCE_ACTION, value);
            }
        }, listenerRefHolder);
        public static WeakDelegate1<Float  > setCrossfadeValue = new WeakDelegate1<Float>().subscribeWeak(new WeakDelegate1.Handler<Float>() {
            @Override
            public void invoke(Float value) {
                startService(MediaPlaybackServiceDefs.CROSS_FADE_VALUE_ACTION, value);
            }
        }, listenerRefHolder);

        public static WeakDelegate onRestartMediaPlayerCore = new WeakDelegate().subscribeWeak(new WeakDelegate.Handler() {
            @Override
            public void invoke() {
                MediaPlaybackService service = MediaPlaybackService.getInstance();
                if (service != null)
                    service.restartMediaPlayerCoreThSafe();
            }
        }, listenerRefHolder);

        public static WeakDelegate1<Integer  > PlaybackControls_selectMediaPlayerCoreIndex = new WeakDelegate1<Integer>().subscribeWeak(new WeakDelegate1.Handler<Integer>() {
            @Override
            public void invoke(Integer index) {
                MediaPlaybackService service = MediaPlaybackService.getInstance();
                if (service != null)
                    service.selectMediaPlayerCoreIndexThSafe(index);
            }
        }, listenerRefHolder);

        public static WeakDelegate onResetVisualizer = new WeakDelegate().subscribeWeak(new WeakDelegate.Handler() {
            @Override
            public void invoke() {
                MediaPlaybackService service = MediaPlaybackService.getInstance();
                if (service != null)
                    service.resetVisualizerThSafe();
            }
        }, listenerRefHolder);

        public static WeakDelegateR<BaseEqualizerEffect.EqualizerDesc> onRequestEqualizerDesc = new WeakDelegateR<BaseEqualizerEffect.EqualizerDesc>().subscribeWeak(new WeakDelegateR.Handler<BaseEqualizerEffect.EqualizerDesc>() {
            @Override
            public BaseEqualizerEffect.EqualizerDesc invoke() {
                MediaPlaybackService service = MediaPlaybackService.getInstance();
                if (service != null)
                    return service.getEqualizerDescThSafe();
                else
                    return null;
            }
        }, listenerRefHolder);

        public static WeakDelegate1<BaseEqualizerEffect.EqualizerSettings  > setEqualizerSettings = new WeakDelegate1<BaseEqualizerEffect.EqualizerSettings>().subscribeWeak(new WeakDelegate1.Handler<BaseEqualizerEffect.EqualizerSettings>() {
            @Override
            public void invoke(BaseEqualizerEffect.EqualizerSettings equalizerSettings) {
                MediaPlaybackService service = MediaPlaybackService.getInstance();
                if (service != null)
                    service.setEqualizerSettingsThSafe(equalizerSettings);
            }
        }, listenerRefHolder);

        static void startService() {
            Context context = PlayerCore.s().getAppContext();
            if (context == null) return;

            ComponentName service = new ComponentName(context, MediaPlaybackService.class);
            Intent close = new Intent(MediaPlaybackServiceDefs.NONE_ACTION);
            close.setComponent(service);
            ContextCompat.startForegroundService(context, close);
        }

        static void startService(String action) {
            Context context = PlayerCore.s().getAppContext();
            if (context == null) return;

            ComponentName service = new ComponentName(context, MediaPlaybackService.class);
            Intent close = new Intent(action);
            close.setComponent(service);
            ContextCompat.startForegroundService(context, close);
        }

        static void startService(String action, long param) {
            Context context = PlayerCore.s().getAppContext();
            if (context == null) return;

            ComponentName service = new ComponentName(context, MediaPlaybackService.class);
            Intent intent = new Intent(action);
            intent.putExtra(MediaPlaybackServiceDefs.EXTRA_ARG_1, param);
            intent.setComponent(service);
            ContextCompat.startForegroundService(context, intent);
        }

        static void startService(String action, int param) {
            Context context = PlayerCore.s().getAppContext();
            if (context == null) return;

            ComponentName service = new ComponentName(context, MediaPlaybackService.class);
            Intent intent = new Intent(action);
            intent.putExtra(MediaPlaybackServiceDefs.EXTRA_ARG_1, param);
            intent.setComponent(service);
            ContextCompat.startForegroundService(context, intent);
        }

        static void startService(String action, float param) {
            Context context = PlayerCore.s().getAppContext();
            if (context == null) return;

            ComponentName service = new ComponentName(context, MediaPlaybackService.class);
            Intent intent = new Intent(action);
            intent.putExtra(MediaPlaybackServiceDefs.EXTRA_ARG_1, param);
            intent.setComponent(service);
            ContextCompat.startForegroundService(context, intent);
        }

        static void startService(String action, boolean param) {
            Context context = PlayerCore.s().getAppContext();
            if (context == null) return;

            ComponentName service = new ComponentName(context, MediaPlaybackService.class);
            Intent intent = new Intent(action);
            intent.putExtra(MediaPlaybackServiceDefs.EXTRA_ARG_1, param);
            intent.setComponent(service);
            ContextCompat.startForegroundService(context, intent);
        }

        static void startService(Intent intent) {
            Context context = PlayerCore.s().getAppContext();
            if (context == null) return;

            ComponentName service = new ComponentName(context, MediaPlaybackService.class);
            intent.setComponent(service);
            ContextCompat.startForegroundService(context, intent);
        }

    }
}

