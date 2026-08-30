

package com.aylis.comp.playback.ExoMediaPlayer;

import com.aylis.comp.playback.BaseEqualizerEffect;

public class ExoEqualizerEffect extends BaseEqualizerEffect {

    public static String name = "Exo";

    public ExoEqualizerEffect(BaseEqualizerEffect.IEqualizerEffectListener equalizerEffectListener) {
        super(equalizerEffectListener, name);
    }

}

