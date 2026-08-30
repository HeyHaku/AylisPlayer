

package com.aylis.comp.playback;

import android.app.Service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import androidx.core.content.ContextCompat;

public class MediaEventReceiver extends BroadcastReceiver {

    public MediaEventReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
            notifyEvent(context, MediaPlaybackServiceDefs.AUDIO_BECOMING_NOISY_ACTION);
        }
    }

    void notifyEvent(Context context, String action) {
        Intent playPause = new Intent(action);
        ComponentName service = new ComponentName(context, MediaPlaybackServiceDefs.MediaServiceClass);
        playPause.setComponent(service);

        ContextCompat.startForegroundService(context, playPause);
    }
}

