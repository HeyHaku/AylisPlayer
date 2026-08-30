

package com.aylis.comp.playback.NativeMediaPlayer;

import com.aylis.comp.playback.BaseEqualizerEffect;

public class NativeEqualizerEffect extends BaseEqualizerEffect {

    public static String name = "Native";

    NativeEqualizerEffect(IEqualizerEffectListener equalizerEffectListener) {
        super(equalizerEffectListener, name);
    }
}

