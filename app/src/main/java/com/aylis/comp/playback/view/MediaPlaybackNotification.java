

package com.aylis.comp.playback.view;

import android.app.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaSession;
import android.graphics.Bitmap;
import android.os.Build;
import android.provider.MediaStore;
import android.widget.RemoteViews;
import com.aylis.Common.Events.WeakEvent4;
import com.aylis.MainActivity;
import com.aylis.comp.AlbumArt.AlbumArtCore;
import com.aylis.comp.AlbumArt.AlbumArtRequest;
import com.aylis.comp.AlbumArt.ImageLoadedListener;
import com.aylis.comp.playback.MediaPlaybackServiceDefs;
import com.aylis.comp.playback.Song.PlaylistSong;
import com.aylis.R;
import java.lang.ref.WeakReference;

public class MediaPlaybackNotification {

    public static WeakEvent4<AlbumArtRequest  , ImageLoadedListener  , Integer  , Integer  > onRequestAlbumArtLarge = new WeakEvent4<>();

    private static WeakReference<Notification> currentNotification = new WeakReference<>(null);

    public static Notification getOrCreateNotification(Context context,
                                                       String notificationChannelId,
                                                       PlaylistSong.Data songData,
                                                       boolean playing,
                                                       boolean wantsPlaying,
                                                       Class<?> intentCls,
                                                       MediaSession.Token sessionToken) {
        Notification notification = createNotificationInternal(context, notificationChannelId, songData, playing, wantsPlaying, intentCls, sessionToken);
        currentNotification = new WeakReference<>(null);
        return notification;
    }

    public static void updateNotification(int id,
                                          Context context,
                                          String notificationChannelId,
                                          PlaylistSong.Data songData,
                                          boolean playing,
                                          boolean wantsPlaying,
                                          Class<?> intentCls,
                                          MediaSession.Token sessionToken) {

        Notification notification = currentNotification.get();
        if (notification == null) {
            notification = getOrCreateNotification(context, notificationChannelId, songData, playing, wantsPlaying, intentCls, sessionToken);
            currentNotification = new WeakReference<>(null);

            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(id, notification);
        } else {
            updateNotificationViews(context, notification.contentView, songData, playing, wantsPlaying, intentCls, sessionToken);
        }
    }

    public static String createNotificationChannel(final Context context){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = context.getString(R.string.playback_service_notif_channel_name);
            String channelName = MediaPlaybackServiceDefs.NOTIFICATION_CHANNEL_NAME;
            NotificationChannel chan = new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW);

            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager service = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if(service!=null)
                service.createNotificationChannel(chan);
            return channelId;
        } else {
            return null;
        }
    }

    private static Notification createNotificationInternal(Context context,
                                                           final String notificationChannelId,
                                                           PlaylistSong.Data songData,
                                                           boolean playing,
                                                           boolean wantsPlaying,
                                                           Class<?> intentCls,
                                                           MediaSession.Token sessionToken) {

        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent notificationAction = PendingIntent.getActivity(context, 0,
                notificationIntent, Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        final Notification.Builder notificationBuilder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder = new Notification.Builder(context, notificationChannelId);
        } else {
            notificationBuilder = new Notification.Builder(context);
        }

        notificationBuilder.setSmallIcon(R.drawable.ic_launcher_mono_xs)
                .setContentTitle(songData.trackName)
                .setContentText(songData.artistName)
                .setContentIntent(notificationAction);

        if (sessionToken != null) {
            notificationBuilder.setStyle(new Notification.MediaStyle()
                    .setMediaSession(sessionToken)
                    .setShowActionsInCompactView(0, 1, 2));
        }

        ComponentName service = new ComponentName(context, intentCls);
        int pendingIntentFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;

        Intent prev = new Intent(MediaPlaybackServiceDefs.PREVIOUS_ACTION);
        prev.setComponent(service);
        PendingIntent prevPending = PendingIntent.getService(context, 0, prev, pendingIntentFlags);

        Intent playPause = new Intent(MediaPlaybackServiceDefs.TOGGLE_PAUSE_ACTION);
        playPause.setComponent(service);
        PendingIntent playPausePending = PendingIntent.getService(context, 0, playPause, pendingIntentFlags);

        Intent next = new Intent(MediaPlaybackServiceDefs.NEXT_ACTION);
        next.setComponent(service);
        PendingIntent nextPending = PendingIntent.getService(context, 0, next, pendingIntentFlags);

        notificationBuilder.addAction(R.drawable.ic_ctrl_fb_s, "Previous", prevPending);
        notificationBuilder.addAction(wantsPlaying ? R.drawable.ic_ctrl_pause_s : R.drawable.ic_ctrl_play_s, "Play/Pause", playPausePending);
        notificationBuilder.addAction(R.drawable.ic_ctrl_ff_s, "Next", nextPending);

        Intent close = new Intent(MediaPlaybackServiceDefs.ACTIVITY_AND_SERVICE_EXIT_ACTION);
        close.setComponent(service);
        PendingIntent closePending = PendingIntent.getService(context, 0, close, pendingIntentFlags);
        notificationBuilder.addAction(R.drawable.ic_close, "Close", closePending);

        return notificationBuilder.build();
    }

    public static void updateNotificationViews(Context context,
                                               final RemoteViews views,
                                               PlaylistSong.Data songData,
                                               boolean playing,
                                               boolean wantsPlaying,
                                               Class<?> intentCls,
                                               MediaSession.Token sessionToken) {

        views.setImageViewResource(R.id.imgArt, R.drawable.placeholderart4);

        AlbumArtCore albumArtCore = AlbumArtCore.getInstance();
        if (albumArtCore != null) {
            ImageLoadedListener imageLoadedListener;
            imageLoadedListener = new ImageLoadedListener() {
                Object object1;

                @Override
                public void onBitmapLoaded(Bitmap bitmap, String url00, String url0, String url1) {
                    if (bitmap != null)
                        views.setImageViewBitmap(R.id.imgArt, bitmap);
                    else
                        views.setImageViewResource(R.id.imgArt, R.drawable.placeholderart4);
                }

                @Override
                public void setUserObject1(Object obj1) {
                    object1 = obj1;
                }
            };

            onRequestAlbumArtLarge.invoke(new AlbumArtRequest(songData.getVideoThumbDataSourceAsStr(),
                    songData.getAlbumArtPath0Str(),
                    songData.getAlbumArtPath1Str(),
                    songData.getAlbumArtGenerateStr()),
                    imageLoadedListener,
                    200, 200);
        }

        String title;
        String artist;
        String album;

        {
            title = songData.trackName;
            artist = songData.artistName;
            album = songData.albumName;

            if (artist == null || artist.equals(MediaStore.UNKNOWN_STRING))
                artist = context.getString(R.string.unknown_artist_name);
            if (album == null || album.equals(MediaStore.UNKNOWN_STRING))
                album = context.getString(R.string.unknown_album_name);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                int playButton = wantsPlaying ? R.drawable.ic_ctrl_pause_s : R.drawable.ic_ctrl_play_s;
                views.setImageViewResource(R.id.btnPlayPause, playButton);

                ComponentName service = new ComponentName(context, intentCls);

                Intent prev = new Intent(MediaPlaybackServiceDefs.PREVIOUS_ACTION);
                prev.setComponent(service);
                int pendingIntentFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
                views.setOnClickPendingIntent(R.id.btnPrev, PendingIntent.getService(context, 0, prev, pendingIntentFlags));

                Intent playPause = new Intent(MediaPlaybackServiceDefs.TOGGLE_PAUSE_ACTION);
                playPause.setComponent(service);
                views.setOnClickPendingIntent(R.id.btnPlayPause, PendingIntent.getService(context, 0, playPause, pendingIntentFlags));

                Intent next = new Intent(MediaPlaybackServiceDefs.NEXT_ACTION);
                next.setComponent(service);
                views.setOnClickPendingIntent(R.id.btnNext, PendingIntent.getService(context, 0, next, pendingIntentFlags));

                Intent close = new Intent(MediaPlaybackServiceDefs.ACTIVITY_AND_SERVICE_EXIT_ACTION);
                close.setComponent(service);
                views.setOnClickPendingIntent(R.id.btnClose, PendingIntent.getService(context, 0, close, pendingIntentFlags));
            }
        }

        views.setTextViewText(R.id.txtSongTitle, title);
        views.setTextViewText(R.id.txtSongArtist, artist);

    }
}

