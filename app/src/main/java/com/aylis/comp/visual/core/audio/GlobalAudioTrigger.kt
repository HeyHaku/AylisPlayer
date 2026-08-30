package com.aylis.comp.visual.core.audio

import com.aylis.comp.visual.core.Elements.bars.AudioBars.ISegmentDataProvider

object GlobalAudioTrigger {
    @JvmField
    var currentBass: Float = 0.0f

    @JvmField
    var isPlaying: Boolean = false

    fun update(provider: ISegmentDataProvider?, isPlaying: Boolean) {
        if (!isPlaying || provider == null) {
            currentBass = 0.0f
            this.isPlaying = false
            return
        }

        val rms = provider.getRms()
        currentBass = if (rms.isNaN()) 0.0f else rms
        this.isPlaying = true
    }
}
