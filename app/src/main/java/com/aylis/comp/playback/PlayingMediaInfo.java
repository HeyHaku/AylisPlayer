

package com.aylis.comp.playback;

public class PlayingMediaInfo {

    public static final PlayingMediaInfo empty = new PlayingMediaInfo(0, false);

    public final long duration;
    public final boolean containsVideoTrack;

    public PlayingMediaInfo(long duration, boolean containsVideoTrack) {
        this.duration = duration;
        this.containsVideoTrack = containsVideoTrack;
    }

}
