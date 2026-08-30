

package com.aylis.comp.playback.view;

import android.app.Service;

import android.app.PendingIntent;
import android.os.Build;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.media.session.MediaSession;
import com.aylis.Design.WidgetAndNotificationDesign;
import com.aylis.MainActivity;
import com.aylis.comp.playback.MediaPlaybackService;
import com.aylis.comp.playback.MediaPlaybackServiceDefs;
import com.aylis.comp.playback.Song.PlaylistSong;
import com.aylis.R;

public class MediaAppWidgetProvider extends AppWidgetProvider {

    private static MediaAppWidgetProvider sInstance;

    public static synchronized MediaAppWidgetProvider getInstance() {
        if (sInstance == null) {
            sInstance = new MediaAppWidgetProvider();
        }
        return sInstance;
    }

    public MediaAppWidgetProvider()
    {
        WidgetAndNotificationDesign.createInstance();
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        defaultAppWidget(context, appWidgetIds, MediaPlaybackService.class);

        Intent updateIntent = new Intent(MediaPlaybackServiceDefs.APP_WIDGET_UPDATE_ACTION);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(updateIntent);
    }

    private void defaultAppWidget(Context context, int[] appWidgetIds, Class<?> intentCls) {
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.notification_bar);

        updateNotificationViews(context, views, PlaylistSong.emptyData, false, false, intentCls, null);

        pushUpdate(context, appWidgetIds, views);
    }

    void performUpdate(Context context,
                       int[] appWidgetIds,
                       PlaylistSong.Data songData,
                       boolean playing,
                       boolean wantsPlaying,
                       Class<?> intentCls) {

        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.notification_bar);

        updateNotificationViews(context, views, songData, playing, wantsPlaying, intentCls, null);

        pushUpdate(context, appWidgetIds, views);
    }

    public void updateNotificationViews(Context context,
                                        final RemoteViews views,
                                        PlaylistSong.Data songData,
                                        boolean playing,
                                        boolean wantsPlaying,
                                        Class<?> intentCls,
                                        MediaSession.Token sessionToken) {
        MediaPlaybackNotification.updateNotificationViews(context, views, songData, playing, wantsPlaying, intentCls, sessionToken);

        views.setViewVisibility(R.id.btnClose, ViewGroup.GONE);

        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent notificationAction = PendingIntent.getActivity(context, 0,
                notificationIntent, Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        views.setOnClickPendingIntent(R.id.artGroup, notificationAction);
    }

    public void notifyChange(Context context,
                             PlaylistSong.Data songData,
                             boolean playing,
                             boolean wantsPlaying,
                             Class<?> intentCls) {
        if (hasInstances(context)) {
            performUpdate(context,
                    null,
                    songData,
                    playing,
                    wantsPlaying,
                    intentCls);
        }
    }

    private void pushUpdate(Context context, int[] appWidgetIds, RemoteViews views) {

        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        if (appWidgetIds != null) {
            gm.updateAppWidget(appWidgetIds, views);
        } else {
            gm.updateAppWidget(new ComponentName(context, this.getClass()), views);
        }
    }

    private boolean hasInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, this.getClass()));
        return (appWidgetIds.length > 0);
    }
}
