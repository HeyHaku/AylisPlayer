

package com.aylis.comp.EqualizerUI;

public class EqualizerUIDesc {

    public static final EqualizerUIDesc empty = new EqualizerUIDesc(0);

    public String name;
    public EQPreset currentBands;
    public boolean enabled;
    public int currentPreset;
    public EQPreset[] presets;
    public float bassBoostValue;
    public EQPreset bassBoost;
    public float trebleBoostValue;
    public EQPreset trebleBoost;
    public float virtualizerStrength;

    private EqualizerUIDesc(int presetsCount)
    {
        name = "";
        currentBands = EQPreset.empty;
        currentPreset = -1;
        presets = new EQPreset[presetsCount];
        bassBoostValue = 0.0f;
        bassBoost = EQPreset.clone(EQPreset.empty);
        trebleBoostValue = 0.0f;
        trebleBoost = EQPreset.clone(EQPreset.empty);
        virtualizerStrength = 0.0f;
    }

    public EqualizerUIDesc() {
    }
}
