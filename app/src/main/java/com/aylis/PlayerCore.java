

package com.aylis;

import android.content.Context;
import com.aylis.Common.UtilsUI;
import com.aylis.Design.AudioEffectsDesign;
import com.aylis.Design.CompositeSearchDesign;
import com.aylis.Design.ContextualActionModeDesign;
import com.aylis.Design.GeneralDesign;
import com.aylis.Design.LibraryQueueUIDesign;
import com.aylis.Design.MainUIDesign;
import com.aylis.Design.PlaybackControlsDesign;
import com.aylis.Design.PlayerControlsUIDesign;
import com.aylis.Design.PlaylistsDesign;
import com.aylis.Design.SleepTimerDesign;
import com.aylis.Design.SortDesign;
import com.aylis.comp.visual.design.VisualizerDesign;
import com.aylis.Design.WidgetAndNotificationDesign;
import com.aylis.comp.ContextualActionBar.ContextualActionBar;
import com.aylis.comp.MediaControlsUI.MediaControlsUI;
import com.aylis.Common.tlog;
import com.aylis.comp.AlbumArt.AlbumArtCore;
import com.aylis.comp.AppPreferences.AppPreferences;
import com.aylis.comp.GlobalSearch.GlobalSearchCore;
import com.aylis.comp.playback.MediaPlaybackService;
import com.aylis.comp.PlaybackQueue.QueueCore;
import com.aylis.comp.SleepTimer.SleepTimer;
import com.aylis.Design.AdsDesign;
import com.aylis.Design.PlaybackDesign;

public class PlayerCore {

    static PlayerCore instance = new PlayerCore();

    private  AppPreferences appPreferences;
    private AlbumArtCore albumArtCore;
    private GlobalSearchCore globalSearchCore;
    private ContextualActionBar contextualActionBar;
    private SleepTimer sleepTimer;
    private QueueCore playbackQueue;
    private MediaControlsUI mediaControlsUI;
    private Object[] design = new Object[16];

    private PlayerCore() {
        instance = this;
        init();
    }

    public static PlayerCore s() {
        return instance;
    }

    private void init() {
        UtilsUI.AssertIsUiThread();

        appPreferences = AppPreferences.createOrGetInstance();

        design[11] = new GeneralDesign();
        design[0] = new SleepTimerDesign();
        design[1] = new LibraryQueueUIDesign();
        design[2] = new VisualizerDesign();
        design[3] = new PlaybackControlsDesign();
        design[4] = new PlaybackDesign();
        design[5] = new MainUIDesign();
        design[6] = new CompositeSearchDesign();
        design[7] = new SortDesign();
        design[8] = new PlaylistsDesign();
        design[9] = new PlayerControlsUIDesign();
        design[10] = new ContextualActionModeDesign();
        design[12] = new AdsDesign();
        design[13] = new AudioEffectsDesign();
        design[15] = WidgetAndNotificationDesign.createInstance();

        playbackQueue = QueueCore.createOrGetInstance();
        contextualActionBar = ContextualActionBar.createInstance(MainActivity.getInstance());
        sleepTimer = SleepTimer.createOrGetInstance();
        albumArtCore = AlbumArtCore.createInstance();
        globalSearchCore = GlobalSearchCore.createInstance();
        mediaControlsUI = MediaControlsUI.createOrGetInstance();

        // Initialize Glide eagerly and asynchronously
        new Thread(() -> {
            AlbumArtCore.getInstance().initGlide(getAppContext());
        }).start();

    }

    public Context getAppContext() {
        Context context = null;
        MainActivity mainActivity = MainActivity.getInstance();
        if (mainActivity != null)
            context = mainActivity.getApplicationContext();
        if (context != null)
            return context;

        MediaPlaybackService mediaPlaybackService = MediaPlaybackService.getInstance();
        if (mediaPlaybackService != null)
            context = mediaPlaybackService.getApplicationContext();
        if (context != null)
            return context;

        tlog.w("app context is null");

        return null;
    }

}

