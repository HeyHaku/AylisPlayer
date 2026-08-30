

package com.aylis.comp.EqualizerUI;

public class EqualizerUISettings {

    public boolean enabled;
    public int presetIndex = -1;
    public EQPreset currentBands;
    public float bassValue;
    public float trebleValue;
    public EQPreset bandsFinal;
    public float virtualizerStrength;

    public EqualizerUISettings()
    {
        enabled = false;
        presetIndex = -1;
        currentBands = EQPreset.clone(EQPreset.empty);
        bassValue = 0.0f;
        trebleValue = 0.0f;
        bandsFinal = EQPreset.clone(EQPreset.empty);
        virtualizerStrength = 0.0f;
    }
}
