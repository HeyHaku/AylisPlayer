

package com.aylis.comp.playback.ExoMediaPlayer;

import com.aylis.comp.visual.core.playback.nativeplayer.NativeVisualizerDataProvider;
import com.aylis.comp.visual.core.playback.exo.IVisualizerDataCapturer;
import com.aylis.comp.visual.core.playback.exo.VisualizerAudioProcessor;
import com.aylis.comp.visual.core.playback.exo.ExoVisualizerDataProvider;
import android.content.Context;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Surface;
import android.view.SurfaceHolder;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.VideoSize;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.DefaultAudioSink;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.util.EventLogger;
import com.aylis.Common.Events.WeakEvent;
import com.aylis.Common.tlog;
import com.aylis.comp.visual.core.playback.AudioFrameData;
import com.aylis.comp.playback.BaseEqualizerEffect;
import com.aylis.comp.playback.IMediaPlayerCore;
import com.aylis.comp.playback.Song.IMediaDataSource;
import com.aylis.EventsGlobal.EventsGlobalApp;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

@UnstableApi
public class ExoMediaPlayerCore implements
        IMediaPlayerCore,
        BaseEqualizerEffect.IEqualizerEffectListener,
        SurfaceHolder.Callback {

    private final Object visualizerLock = new Object();
    private Context context;
    private PlayerEntry[] players = new PlayerEntry[2];
    private int currentPlayer = 0;
    private int nextPlayer = 1;
    private IMediaDataSource nextDataSource;
    private float volume = 1.0f;
    private boolean muted = false;
    private WeakReference<SurfaceHolder> surfaceHolderWeak = new WeakReference<>(null);
    private long visualizerLastTimeUsed = 0;
    private ExoVisualizerDataProvider visualizerData = null;
    private List<Object> listenerRefHolder = new LinkedList<>();
    private OnNotifyListener onNotifyListener;
    private EventLogger eventLogger;
    private ExoEqualizerEffect equalizerEffect = new ExoEqualizerEffect(this);

    IVisualizerDataCapturer visualizerDataCapturer = new IVisualizerDataCapturer() {
        boolean enabled = false;

        @Override
        public void onSetStarted(boolean b) {
            ExoVisualizerDataProvider vis = visualizerData;
            if (vis != null)
                vis.onSetStarted(b);
        }

        @Override
        public void onSetEnabled(boolean b) {
            enabled = b;
        }

        @Override
        public void onPcmData(ByteBuffer buffer, android.media.MediaCodec.BufferInfo bufferInfo, int bufferIndex, int sampleRate, int channelCount, long positionUs) {
            if (!enabled) return;
            ExoVisualizerDataProvider vis = visualizerData;
            if (vis != null)
                vis.onPcmData(buffer, bufferInfo, bufferIndex, sampleRate, channelCount, positionUs);
        }

        @Override
        public void onAudioSessionId(int audioSessionId) {
            if (equalizerEffect != null)
                equalizerEffect.onAudioSessionChanged(audioSessionId);
        }
    };

    public ExoMediaPlayerCore(Context context, String playerName, OnNotifyListener onNotifyListener) {
        this.context = context;
        this.onNotifyListener = onNotifyListener;
        tlog.d("Media3: ExoMediaPlayerCore created");

        eventLogger = new EventLogger();

        for (int i = 0; i < players.length; i++)
            players[i] = new PlayerEntry();

        EventsGlobalApp.onUITick10.subscribeWeak(new WeakEvent.Handler() {
            @Override
            public void invoke() {
                checkVisualizerLife();
            }
        }, listenerRefHolder);
    }

    @Override
    public void setNotifyListener(OnNotifyListener onNotifyListener) {
        this.onNotifyListener = onNotifyListener;
    }

    public void release() {
        if (equalizerEffect != null) {
            equalizerEffect.release();
            equalizerEffect = null;
        }

        setVideoSurfaceHolder(null);

        for (PlayerEntry mPlayer : players) {
            releasePlayer(mPlayer);
        }
    }

    private void restartPlayers() {
        for (PlayerEntry mPlayer : players) {
            long seekPos = 0;
            IMediaDataSource dataSource;
            if (mPlayer.player != null)
                seekPos = mPlayer.player.getCurrentPosition();
            dataSource = mPlayer.dataSource;
            releasePlayer(mPlayer);
            preparePlayer(mPlayer, dataSource, 1.0f, seekPos);
        }
    }

    void checkVisualizerLife() {
        synchronized (visualizerLock) {
            ExoVisualizerDataProvider vis = visualizerData;
            if (vis != null && (SystemClock.elapsedRealtime() - visualizerLastTimeUsed) > 8000) {
                vis.release();
                visualizerData = null;
            }
        }
    }

    public void setNextDataSource(IMediaDataSource path) {
        nextDataSource = path;
    }

    public void playNext(boolean killCurrent, boolean start, float fadeVolume, long seekPos) {
        if (!killCurrent) {
            if (isPreparingOrAbove()) {
                cycleNextPlayer();
            }
        } else {
            if (currentPlayer != nextPlayer) {
                releasePlayer(getNextPlayerEntry());
            }
        }

        if (nextDataSource == null || nextDataSource.getContentUri() == null || nextDataSource.getContentUri().equals(Uri.EMPTY)) {
            tlog.w("nextDataSource is null");
            if (start) {
                start();
            } else {
                getPlayerEntry().setAutoPlay(false);
                pause();
            }
            return;
        }

        preparePlayer(getPlayerEntry(), nextDataSource, fadeVolume, seekPos);

        if (start) {
            start();
        } else {
            getPlayerEntry().setAutoPlay(false);
            pause();
        }
    }

    public void start() {
        PlayerEntry entry = getPlayerEntry();
        if (entry.player == null) return;

        if (onNotifyListener.onRequestAudioFocus()) {
            entry.setAutoPlay(true);
            surfaceCreated(onNotifyListener.onRequestVideoSurfaceHolder());
            entry.updateVolume();
            tlog.d("Media3: Starting playback for: " + (entry.dataSource != null ? entry.dataSource.getContentUri() : "null"));
            entry.player.play();
        }

        onNotifyListener.onMpPlaystateOrMetaChanged(false, null);
    }

    public void pause() {
        if (getPlayer() == null) return;
        getPlayer().pause();
        onNotifyListener.onMpPlaystateOrMetaChanged(false, null);
    }

    public void stop() {
        if (getPlayer() == null) return;
        getPlayer().setPlayWhenReady(false);
        onNotifyListener.onMpPlaystateOrMetaChanged(false, null);
    }

    public boolean isPreparingOrAbove() {
        if (getPlayer() == null) return false;
        int state = getPlayer().getPlaybackState();
        return state == Player.STATE_BUFFERING || state == Player.STATE_READY;
    }

    public boolean isPreparingOrStared() {
        if (getPlayer() == null) return false;
        int state = getPlayer().getPlaybackState();
        return (state == Player.STATE_BUFFERING || state == Player.STATE_READY) && getPlayer().getPlayWhenReady();
    }

    @Override
    public boolean containsVideoTrack() {
        return getPlayer() != null;
    }

    @Override
    public void setVideoScalingMode(int mode) {
        if (getPlayer() != null) {
            int scalingMode = (mode == IMediaPlayerCore.MP_VIDEO_SCALING_MODE_SCALE_TO_FIT)
                ? C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                : C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING;
            getPlayer().setVideoScalingMode(scalingMode);
        }
    }

    @Override
    public void setVideoSurfaceHolder(SurfaceHolder surfaceHolder) {
        if (surfaceHolder != null) {
            surfaceHolderWeak = new WeakReference<>(surfaceHolder);
            surfaceHolder.removeCallback(this);
            surfaceHolder.addCallback(this);
            this.surfaceCreated(surfaceHolder);
        } else {
            SurfaceHolder surfaceHolder_ = surfaceHolderWeak.get();
            if (surfaceHolder_ != null) {
                surfaceHolder_.removeCallback(this);
                surfaceHolderWeak = new WeakReference<>(null);
            }
            this.surfaceDestroyed(null);
        }
    }

    public long duration() {
        if (getPlayer() == null) return 0;
        long dur = getPlayer().getDuration();
        return dur == C.TIME_UNSET ? 0 : dur;
    }

    public long position() {
        if (getPlayer() == null) return 0;
        return getPlayer().getCurrentPosition();
    }

    public void seek(long timeMillis) {
        getPlayerEntry().setFadeVolume(1.0f);
        if (getPlayer() != null)
            getPlayer().seekTo(timeMillis);
    }

    @Override
    public void setMute(boolean state) {
        setVolume(state, volume);
        onNotifyListener.onVolumeMuteStateChanged(state);
    }

    @Override
    public boolean isMuted() {
        return muted;
    }

    @Override
    public void setVolume(float volume) {
        setVolume(muted, volume);
    }

    public void setVolume(boolean muted, float volume) {
        this.volume = volume;
        this.muted = muted;
        float finalVol = muted ? 0.0f : volume;
        for (PlayerEntry mPlayer : players) {
            if (mPlayer != null)
                mPlayer.setVolume(finalVol);
        }
    }

    @Override
    public void setFadeVolume(float fadeVolume, int index) {
        int idx = (index == 0) ? currentPlayer : nextPlayer;
        if (players[idx] != null)
            players[idx].setFadeVolume(fadeVolume);
    }

    @Override
    public boolean setFadeVolumeRelative(float volumePlus, int index) {
        int idx = (index == 0) ? currentPlayer : nextPlayer;
        return players[idx] == null || players[idx].setFadeVolumeRelative(volumePlus);
    }

    @Override
    public void setVolumeStereoBalance(float balance) {
        for (PlayerEntry mPlayer : players) {
            if (mPlayer != null)
                mPlayer.setVolumeStereoBalance(balance);
        }
    }

    @Override
    public void destroy(int index) {
        if (index != 0) {
            releasePlayer(players[nextPlayer]);
        }
    }

    @Override
    public void resetVisualizer() {
    }

    @Override
    public AudioFrameData getVisualizationData(AudioFrameData outResult, boolean useGlobalSession) {
        if (outResult == null) return null;
        synchronized (visualizerLock) {
            ExoVisualizerDataProvider vis = visualizerData;
            if (vis == null) {
                vis = new ExoVisualizerDataProvider();
                visualizerDataCapturer.onSetEnabled(true);
            }
            PlayerEntry entry = getPlayerEntry();
            vis.onSetStarted(entry.isPlayingCached);
            visualizerLastTimeUsed = SystemClock.elapsedRealtime();
            AudioFrameData result = vis.getVisData(outResult);
            visualizerData = vis;
            return result;
        }
    }

    private ExoPlayer getPlayer() {
        return getPlayerEntry().player;
    }

    PlayerEntry getPlayerEntry() {
        return players[currentPlayer];
    }

    PlayerEntry getNextPlayerEntry() {
        return players[nextPlayer];
    }

    void cycleNextPlayer() {
        int old = currentPlayer;
        currentPlayer = nextPlayer;
        nextPlayer = old;
    }

    private void releasePlayer(PlayerEntry playerEntry) {
        if (playerEntry.player != null) {
            playerEntry.player.release();
            playerEntry.player = null;
        }
    }

    private void preparePlayer(final PlayerEntry entry, IMediaDataSource dataSource, final float fadeStartVolume, long seekPos) {
        entry.dataSource = dataSource;

        if (entry.player == null) {
            final VisualizerAudioProcessor visualizerProcessor = new VisualizerAudioProcessor(visualizerDataCapturer);

            DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context) {
                @Override
                protected AudioSink buildAudioSink(Context context, boolean enableFloatOutput, boolean enableAudioTrackPlaybackParams, boolean enableOffload) {
                    return new DefaultAudioSink.Builder(context)
                            .setAudioProcessors(new AudioProcessor[]{visualizerProcessor})
                            .build();
                }
            };

            String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"; // Util.getUserAgent(context, "AveePlayer");
            DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(context, ExoCacheManager.getCacheDataSourceFactory(context));
            dataSourceFactory = new androidx.media3.datasource.ResolvingDataSource.Factory(dataSourceFactory, new YoutubeResolver(context));

            entry.player = new ExoPlayer.Builder(context, renderersFactory)
                    .setMediaSourceFactory(new DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory))
                    .build();
            entry.player.addListener(entry);
            // entry.player.addAnalyticsListener(eventLogger); // Removed to prevent background "crashes" / aggressive loadError logging

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build();
            entry.player.setAudioAttributes(audioAttributes, false);
        }

        entry.setFadeVolume(fadeStartVolume);
        entry.setStartFadeVolume(fadeStartVolume);

        setVideoSurfaceHolder(onNotifyListener.onRequestVideoSurfaceHolder());

        Uri uri = dataSource.getContentUri();
        if (uri == null || uri.equals(Uri.EMPTY)) {
            tlog.w("Media3: preparePlayer skipped because uri is null or empty");
            return;
        }

        if (uri != null && (uri.getScheme() == null || uri.getScheme().isEmpty())) {
            uri = Uri.fromFile(new java.io.File(uri.toString()));
        }

        String mimeType = null;
        int contentType = dataSource.getContentType();

        if (uri != null && uri.getScheme() != null && uri.getScheme().equals("content")) {
            mimeType = context.getContentResolver().getType(uri);
            tlog.d("Media3: ContentResolver returned MimeType: " + mimeType);
        }

        if (mimeType != null && (mimeType.equalsIgnoreCase("audio/opus") || mimeType.equalsIgnoreCase("audio/x-opus+ogg"))) {
            mimeType = "audio/ogg";
        }
        if (mimeType == null && uri != null) {
            String pathLower = uri.toString().toLowerCase();
            if (pathLower.endsWith(".opus")) {
                mimeType = "audio/ogg";
            } else if (pathLower.contains("googlevideo.com")) {
                // Подсказка для ExoPlayer: это аудиопоток YouTube (обычно m4a/mp4)
                mimeType = "audio/mp4";
            }
        }

        if (mimeType == null) {
            switch (contentType) {
                case Defines.TYPE_DASH:
                    mimeType = MimeTypes.APPLICATION_MPD;
                    break;
                case Defines.TYPE_SS:
                    mimeType = MimeTypes.APPLICATION_SS;
                    break;
                case Defines.TYPE_HLS:
                    mimeType = MimeTypes.APPLICATION_M3U8;
                    break;
                default:

                    break;
            }
        }

        MediaItem.Builder mediaItemBuilder = new MediaItem.Builder().setUri(uri);
        if (mimeType != null) {
            mediaItemBuilder.setMimeType(mimeType);
            tlog.d("Media3: Detected MimeType: " + mimeType + " for " + uri);
        } else {
            tlog.d("Media3: No MimeType detected for " + uri);
        }

        boolean audioOnly = false;
        if (mimeType != null) {
            if (mimeType.startsWith("audio/")) {
                audioOnly = true;
            } else if (mimeType.startsWith("video/")) {
                audioOnly = false;
            }
        } else {
            String path = uri.toString().toLowerCase();
            if (path.endsWith(".mp3") || path.endsWith(".wav") || path.endsWith(".flac") ||
                path.endsWith(".aac") || path.endsWith(".m4a") || path.endsWith(".ogg") ||
                path.endsWith(".oga") || path.endsWith(".wma") || path.endsWith(".opus")) {
                audioOnly = true;
            }
        }

        entry.player.setTrackSelectionParameters(
            entry.player.getTrackSelectionParameters()
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, audioOnly)
                .build()
        );

        entry.player.setMediaItem(mediaItemBuilder.build());
        entry.player.prepare();
        if (seekPos > 0)
            entry.player.seekTo(seekPos);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder != null) {
            getNextPlayerEntry().setVideoSurface(null);
            getPlayerEntry().setVideoSurface(holder.getSurface());
        } else {
            getNextPlayerEntry().setVideoSurface(null);
            getPlayerEntry().setVideoSurface(null);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        getNextPlayerEntry().setVideoSurface(null);
        getPlayerEntry().setVideoSurface(null);
    }

    @Override
    public BaseEqualizerEffect.EqualizerSettings getEqualizerSettings(String name) {
        return onNotifyListener.getEqualizerSettings(name);
    }

    @Override
    public boolean isEqualizerEnabled(String name) {
        return onNotifyListener.getEqualizerEnabled(name);
    }

    @Override
    public void onEqualizerDescChanged(BaseEqualizerEffect.EqualizerDesc desc) {
        onNotifyListener.onEqualizerDescChanged(desc);
    }

    @Override
    public BaseEqualizerEffect.EqualizerDesc getEqualizerDesc() {
        return equalizerEffect != null ? equalizerEffect.getEqualizerDesc() : null;
    }

    @Override
    public void setEqualizerSettings(BaseEqualizerEffect.EqualizerSettings equalizerSettings) {
        if (equalizerEffect != null)
            equalizerEffect.setEqualizerSettings(equalizerSettings);
    }

    class PlayerEntry implements Player.Listener {

        IMediaDataSource dataSource;
        private float volume = 1.0f;
        private float fadeVolume = 0.0f;
        private float volumeStereoBalance = 0.0f;
        private ExoPlayer player;
        private float startFadeVolume = 0.0f;
        private boolean autoPlay = false;

        public boolean isPlayingCached = false;

        public void setAutoPlay(boolean val) {
            autoPlay = val;
            if (player != null) player.setPlayWhenReady(val);
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            this.isPlayingCached = isPlaying;
            onNotifyListener.onMpPlaystateOrMetaChanged(false, null);
        }

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (equalizerEffect != null)
                equalizerEffect.onCheckEqualizerLife();

            if (playbackState == Player.STATE_READY) {
                this.setFadeVolume(startFadeVolume);
                onNotifyListener.onMpPlaystateOrMetaChanged(true, null);
            } else if (playbackState == Player.STATE_ENDED) {
                onCompletion();
            }
        }

        @Override
        public void onPlayerError(PlaybackException error) {
            tlog.w("Media3: Player Error: " + error.getMessage());
            // android.util.Log.e("ExoMediaPlayerCore", "Media3: Player Error", error); // Removed to prevent false "crash" reports in Logcat
            String str = error.getMessage();
            if (str == null) str = "Unknown error";
            onNotifyListener.onMpPlaystateOrMetaChanged(false, str);
            // Automatically skip to the next track on error
            onNotifyListener.requestNextDataDelay();
        }

        @Override
        public void onVideoSizeChanged(VideoSize videoSize) {
            float widthHeightRatio = 1.0f;
            if (videoSize.width > 0 && videoSize.height > 0)
                widthHeightRatio = (videoSize.width * videoSize.pixelWidthHeightRatio) / videoSize.height;
            onNotifyListener.onNotifyVideoSizeChanged(videoSize.width, videoSize.height, widthHeightRatio);
        }

        @Override
        public void onAudioSessionIdChanged(int audioSessionId) {
            if (equalizerEffect != null)
                equalizerEffect.onAudioSessionChanged(audioSessionId);
            visualizerDataCapturer.onAudioSessionId(audioSessionId);
        }

        public void onCompletion() {
            if (ExoMediaPlayerCore.this.getPlayer() == player) {
                onNotifyListener.requestNextDataNow();
            }
        }

        public void setVideoSurface(Surface surface) {
            if (player != null) player.setVideoSurface(surface);
        }

        public void setVolume(float volume) {
            this.volume = volume;
            updateVolume();
        }

        public void setFadeVolume(float fadeVolume) {
            this.fadeVolume = fadeVolume;
            updateVolume();
        }

        public boolean setFadeVolumeRelative(float fadeVolumePlus) {
            boolean reachedMax = false;
            fadeVolume += fadeVolumePlus;
            if (fadeVolume <= 0.0f) {
                fadeVolume = 0.0f;
                reachedMax = true;
            }
            if (fadeVolume >= 1.0f) {
                fadeVolume = 1.0f;
                reachedMax = true;
            }
            updateVolume();
            return reachedMax;
        }

        public void setStartFadeVolume(float startFadeVolume) {
            this.startFadeVolume = startFadeVolume;
        }

        public void setVolumeStereoBalance(float balance) {
            volumeStereoBalance = balance;
            updateVolume();
        }

        void updateVolume() {
            float left = Math.min(1.0f - volumeStereoBalance, 1.0f);
            float right = Math.min(1.0f + volumeStereoBalance, 1.0f);
            if (player != null)
                player.setVolume(fadeVolume * volume);

        }
    }
}

