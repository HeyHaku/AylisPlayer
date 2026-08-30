

package com.aylis.comp.AppPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import com.aylis.PlayerCore;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.aylis.Common.Events.WeakEvent1;
import com.aylis.Common.Events.WeakEvent2;
import com.aylis.Common.Events.WeakEvent3;
import com.aylis.Common.MultiList;
import com.aylis.Common.Tuple2;
import com.aylis.Common.Utils;
import com.aylis.Common.UtilsFileSys;
import com.aylis.Common.UtilsSerialize;
import com.aylis.Design.SortDesign;
import com.aylis.Common.tlog;
import com.aylis.comp.visual.core.Elements.Element;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class AppPreferences implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static WeakEvent3<Integer  , Integer  , Boolean  > onIntPreferenceChanged = new WeakEvent3<>();
    public static WeakEvent2<Integer  , Boolean  > onBoolPreferenceChanged = new WeakEvent2<>();
    public static WeakEvent2<Integer  , String  > onStringPreferenceChanged = new WeakEvent2<>();
    public static WeakEvent1<Integer  > onThemeSceneChanged = new WeakEvent1<>();

    private static final Object createInstanceLock = new Object();
    private static volatile WeakReference<AppPreferences> instanceWeak = new WeakReference<>(null);

    private static int offset_Bool = 1000;
    public static int PREF_Bool_pref_visControlsTimeout = offset_Bool;
    public static int PREF_Bool_visualPreferShowVideoContent = offset_Bool + 1;
    public static int PREF_Bool_fixAssumeMonoOutputFromMonoInput = offset_Bool + 2;
    public static int PREF_Bool_followCurrentState = offset_Bool + 3;
    public static int PREF_Bool_audioMuteState = offset_Bool + 4;
    public static int PREF_Bool_showAlbumArtInstead = offset_Bool + 5;
    public static int PREF_Bool_tipShow_reorder = offset_Bool + 6;
    public static int PREF_Bool_firstLaunch = offset_Bool + 7;
    public static int PREF_Bool_uiSectionOpened0 = offset_Bool + 8;
    public static int PREF_Bool_uiSectionOpened1 = offset_Bool + 9;
    public static int PREF_Bool_uiSectionOpened00 = offset_Bool + 10;
    public static int PREF_Bool_uiSectionOpened01 = offset_Bool + 11;
    public static int PREF_Bool_uiSectionOpened2 = offset_Bool + 12;
    public static int PREF_Bool_visualizerUseGlobalSession = offset_Bool + 13;
    public static int PREF_Bool_equalizerEnabled = offset_Bool + 14;
    public static int PREF_Bool_mediaControlsHidden = offset_Bool + 15;
    public static int PREF_Bool_uiLayoutCCS = offset_Bool + 16;
    private static int PREF_Bool_COUNT = offset_Bool + 17;

    private static int offset_Int = 2000;
    public static int PREF_Int_mainPageIndex = offset_Int;
    public static int PREF_Int_recentlyAddedWeeks = offset_Int + 1;
    public static int PREF_Int_visualizerThemeId = offset_Int + 2;
    public static int PREF_Int_lockOrient = offset_Int + 3;
    public static int PREF_Int_playbackEngine = offset_Int + 4;
    public static int PREF_Int_videoScalingMode = offset_Int + 5;
    public static int PREF_Int_SortSelectedRadioOption = offset_Int + 6;
    public static int PREF_Int_SortMaskCheckOptions = offset_Int + 7;
    public static int PREF_Int_volumeStereoBalance = offset_Int + 8;
    public static int PREF_Int_crossfadeValue = offset_Int + 9;
    public static int PREF_Int_equalizerPreset = offset_Int + 10;
    public static int PREF_Int_equalizerBassValue = offset_Int + 11;
    public static int PREF_Int_equalizerTrebleValue = offset_Int + 12;
    public static int PREF_Int_virtualizerStrength = offset_Int + 13;
    public static int PREF_Int_reverbPreset = offset_Int + 14;
    public static int PREF_Int_visualizerAspectRatio = offset_Int + 15;
    public static int PREF_Int_exoVisualizerOffset = offset_Int + 16;
    public static int PREF_Int_visualizerFrameRateLimit = offset_Int + 17;
    public static int PREF_Int_themeMode = offset_Int + 18;
    private static int PREF_Int_COUNT = offset_Int + 19;

    private static int offset_String = 3000;
    public static int PREF_String_currentAbsoluteLibraryAddress = offset_String;
    public static int PREF_String_vThemeCustomization0 = offset_String + 1;
    public static int PREF_String_vThemeCustomization1 = offset_String + 2;
    public static int PREF_String_vThemeCustomization2 = offset_String + 3;
    public static int PREF_String_vThemeCustomization3 = offset_String + 4;
    public static int PREF_String_vThemeCustomization4 = offset_String + 5;
    public static int PREF_String_vThemeCustomization5 = offset_String + 6;
    public static int PREF_String_vThemeCustomization6 = offset_String + 7;
    public static int PREF_String_vThemeCustomization7 = offset_String + 8;
    public static int PREF_String_vThemeCustomization8 = offset_String + 9;
    public static int PREF_String_vThemeCustomization9 = offset_String + 10;
    public static int PREF_String_vThemeCustomization10 = offset_String + 11;
    public static int PREF_String_equalizerBarsValues = offset_String + 12;
    public static int PREF_String_visualizerResolutionScale = offset_String + 13;
    private static int PREF_String_COUNT = offset_String + 14;

    private AtomicIntegerArray prefBool = new AtomicIntegerArray(PREF_Bool_COUNT - offset_Bool);
    private AtomicIntegerArray prefsInt = new AtomicIntegerArray(PREF_Int_COUNT - offset_Int);
    private AtomicReferenceArray<String> prefsString = new AtomicReferenceArray<>(PREF_String_COUNT - offset_String);
    private String defaultFolderString = null;

    private AppPreferences() {
        setBoolDefault(PREF_Bool_pref_visControlsTimeout, false);
        setBoolDefault(PREF_Bool_visualPreferShowVideoContent, false);
        setBoolDefault(PREF_Bool_fixAssumeMonoOutputFromMonoInput, true);
        setBoolDefault(PREF_Bool_followCurrentState, true);
        setBoolDefault(PREF_Bool_audioMuteState, false);
        setBoolDefault(PREF_Bool_showAlbumArtInstead, true);
        setBoolDefault(PREF_Bool_tipShow_reorder, true);
        setBoolDefault(PREF_Bool_firstLaunch, true);
        setBoolDefault(PREF_Bool_uiSectionOpened0, true);
        setBoolDefault(PREF_Bool_uiSectionOpened1, true);
        setBoolDefault(PREF_Bool_uiSectionOpened00, true);
        setBoolDefault(PREF_Bool_uiSectionOpened01, true);
        setBoolDefault(PREF_Bool_visualizerUseGlobalSession, true);
        setBoolDefault(PREF_Bool_equalizerEnabled, false);
        setBoolDefault(PREF_Bool_mediaControlsHidden, false);
        setBoolDefault(PREF_Bool_uiLayoutCCS, true);

        setIntDefault(PREF_Int_mainPageIndex, 1);
        setIntDefault(PREF_Int_recentlyAddedWeeks, 2);
        setIntDefault(PREF_Int_visualizerThemeId, 8);
        setIntDefault(PREF_Int_lockOrient, 0);
        setIntDefault(PREF_Int_playbackEngine, 1);
        setIntDefault(PREF_Int_videoScalingMode, 1);
        setIntDefault(PREF_Int_SortSelectedRadioOption, SortDesign.Sort_Mode_Title);
        setIntDefault(PREF_Int_SortMaskCheckOptions, 0);
        setIntDefault(PREF_Int_volumeStereoBalance, 0);
        setIntDefault(PREF_Int_crossfadeValue, -1000);
        setIntDefault(PREF_Int_equalizerPreset, -1);
        setIntDefault(PREF_Int_equalizerBassValue, 0);
        setIntDefault(PREF_Int_equalizerTrebleValue, 0);
        setIntDefault(PREF_Int_virtualizerStrength, 0);
        setIntDefault(PREF_Int_reverbPreset, 0);
        setIntDefault(PREF_Int_visualizerAspectRatio, 0);
        setIntDefault(PREF_Int_exoVisualizerOffset, -500);
        setIntDefault(PREF_Int_visualizerFrameRateLimit, 60);
        setIntDefault(PREF_Int_themeMode, 1);

        setStringDefault(PREF_String_currentAbsoluteLibraryAddress, "");
        setStringDefault(PREF_String_vThemeCustomization10, "");
        setStringDefault(PREF_String_equalizerBarsValues, "");
        setStringDefault(PREF_String_visualizerResolutionScale, "1.5");
    }

    public static AppPreferences createOrGetInstance() {
        AppPreferences inst0 = instanceWeak.get();
        if (inst0 != null) return inst0;

        synchronized (createInstanceLock) {
            AppPreferences inst = instanceWeak.get();
            if (inst == null) {
                inst = new AppPreferences();
                instanceWeak = new WeakReference<>(inst);
            }

            return inst;
        }
    }

    public static boolean preferencesGetBoolSafe(SharedPreferences settings, String key, boolean defValue) {
        try {
            return settings.getBoolean(key, defValue);
        } catch (Exception e) {
            return defValue;
        }
    }

    public static int preferencesGetIntSafe(SharedPreferences settings, String key, int defValue) {
        try {
            return settings.getInt(key, defValue);
        } catch (Exception e) {
            return defValue;
        }
    }

    public static String preferencesGetStringSafe(SharedPreferences settings, String key, String defValue) {
        try {
            return settings.getString(key, defValue);
        } catch (Exception e) {
            return defValue;
        }
    }

    public boolean preferencesGetBoolSafe(Context context, String key, boolean defValue) {
        return preferencesGetBoolSafe(getPreferences(context), key, defValue);
    }

    public int preferencesGetIntSafe(Context context, String key, int defValue) {
        return preferencesGetIntSafe(getPreferences(context), key, defValue);
    }

    public String preferencesGetStringSafe(Context context, String key, String defValue) {
        return preferencesGetStringSafe(getPreferences(context), key, defValue);
    }

    public boolean getBool(int pref) {
        return prefBool.get(pref - offset_Bool) != 0;
    }

    public int getInt(int pref) {
        return prefsInt.get(pref - offset_Int);
    }

    public String getString(int pref) {
        return prefsString.get(pref - offset_String);
    }

    public void toggleBool(final int preference) {
        setBool(preference, prefBool.get(preference - offset_Bool) == 0);
    }

    public void setBool(final int preference, final boolean value) {
        int oldValue = prefBool.getAndSet(preference - offset_Bool, value ? 1 : 0);

        if (value == (oldValue == 0))
            onBoolPreferenceChanged.invoke(preference, value);
    }

    public void setInt(final int preference, final int value) {
        setInt(preference, value, false);
    }

    public void setInt(final int preference, final int value, boolean userForce) {
        int oldValue = prefsInt.getAndSet(preference - offset_Int, value);

        if (userForce || value != oldValue)
            onIntPreferenceChanged.invoke(preference, value, userForce);
    }

    public void setString(final int preference, final String value) {
        String oldValue = prefsString.getAndSet(preference - offset_String, value);

        if (Utils.compareNullEqual(oldValue, value))
            onStringPreferenceChanged.invoke(preference, value);
    }

    public void setBoolDefault(final int preference, final boolean value) {
        prefBool.set(preference - offset_Bool, value ? 1 : 0);
    }

    public void setIntDefault(final int preference, final int value) {
        prefsInt.set(preference - offset_Int, value);
    }

    public void setStringDefault(final int preference, final String value) {
        prefsString.set(preference - offset_String, value);
    }

    private void onContext(Context appContext) {
        SharedPreferences settings = getPreferences(appContext);
        settings.registerOnSharedPreferenceChangeListener(this);

        load_pref_playbackEngine(settings);
        load_pref_visControlsTimeout(settings);
        load_pref_visualizerGlobalSession(settings);
        load_pref_exoVisualizerOffset(settings);
        load_pref_visualizerFrameRateLimit(settings);
        load_pref_visualizerResolutionScale(settings);
        load_pref_themeMode(settings);
    }

    public void load(Context context) {
        onContext(context);

        SharedPreferences preferences = getPreferences(context);

        for (int i = 0; i < prefBool.length(); i++) {
            try {
                final boolean value = preferencesGetBoolSafe(preferences, "bool" + i, prefBool.get(i) != 0);
                setBool(i + offset_Bool, value);
            } catch (Exception ignored) {
            }
        }

        for (int i = 0; i < prefsInt.length(); i++) {
            try {
                final int value = preferencesGetIntSafe(preferences, "int" + i, prefsInt.get(i));
                setInt(i + offset_Int, value);
            } catch (Exception ignored) {
            }
        }

        for (int i = 0; i < prefsString.length(); i++) {
            try {
                final String value = preferencesGetStringSafe(preferences, "string" + i, prefsString.get(i));
                setString(i + offset_String, value);
            } catch (Exception ignored) {
            }
        }
    }

    public void save(Context context) {
        SharedPreferences preferences = getPreferences(context);

        SharedPreferences.Editor ed = preferences.edit();

        for (int i = 0; i < prefBool.length(); i++) {
            boolean value = prefBool.get(i) != 0;
            ed.putBoolean("bool" + i, value);
        }

        for (int i = 0; i < prefsInt.length(); i++) {
            int value = prefsInt.get(i);
            ed.putInt("int" + i, value);
        }

        for (int i = 0; i < prefsString.length(); i++) {
            String value = prefsString.get(i);
            ed.putString("string" + i, value);
        }

        save_pref_playbackEngine(ed);
        save_pref_visControlsTimeout(ed);
        save_pref_visualizerGlobalSession(ed);
        save_pref_exoVisualizerOffset(ed);
        save_pref_visualizerFrameRateLimit(ed);
        save_pref_themeMode(ed);

        ed.apply();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences settings, String key) {
        switch (key) {
            case "pref_playbackEngine":
                load_pref_playbackEngine(settings);
                break;
            case "pref_visControlsTimeout":
                load_pref_visControlsTimeout(settings);
                break;
            case "pref_visualizerGlobalSession":
                load_pref_visualizerGlobalSession(settings);
                break;
            case "pref_exoVisualizerOffset":
                load_pref_exoVisualizerOffset(settings);
                break;
            case "pref_visualizerFrameRateLimit":
                load_pref_visualizerFrameRateLimit(settings);
                break;
            case "pref_visualizerResolutionScale":
                load_pref_visualizerResolutionScale(settings);
                break;
            case "key_theme_mode":
                load_pref_themeMode(settings);
                break;
        }
    }

    void load_pref_playbackEngine(SharedPreferences settings) {
        String valueStr = preferencesGetStringSafe(settings, "pref_playbackEngine2", "1");
        int valueInt = Utils.strToIntSafe(valueStr);
        this.setInt(PREF_Int_playbackEngine, valueInt);
    }

    void save_pref_playbackEngine(SharedPreferences.Editor ed) {
        ed.putString("pref_playbackEngine2", "" + this.getInt(PREF_Int_playbackEngine));
    }

    void load_pref_exoVisualizerOffset(SharedPreferences settings) {
        String valueStr = preferencesGetStringSafe(settings, "pref_exoVisualizerOffset", "-500");
        int valueInt = Utils.strToIntSafe(valueStr);
        this.setInt(PREF_Int_exoVisualizerOffset, valueInt);
    }

    void save_pref_exoVisualizerOffset(SharedPreferences.Editor ed) {
        ed.putString("pref_exoVisualizerOffset", "" + this.getInt(PREF_Int_exoVisualizerOffset));
    }

    void load_pref_visControlsTimeout(SharedPreferences settings) {
        boolean val = preferencesGetBoolSafe(settings, "pref_visControlsTimeout", false);
        this.setBool(PREF_Bool_pref_visControlsTimeout, val);
    }

    void save_pref_visControlsTimeout(SharedPreferences.Editor ed) {
        ed.putBoolean("pref_visControlsTimeout", this.getBool(PREF_Bool_pref_visControlsTimeout));
    }

    void load_pref_visualizerGlobalSession(SharedPreferences settings) {
        boolean val = preferencesGetBoolSafe(settings, "pref_visualizerGlobalSession", true);
        this.setBool(PREF_Bool_visualizerUseGlobalSession, val);
    }

    void load_pref_themeMode(SharedPreferences settings) {
        String valueStr = preferencesGetStringSafe(settings, "key_theme_mode", "1");
        int valueInt = Utils.strToIntSafe(valueStr);
        this.setInt(PREF_Int_themeMode, valueInt);
    }

    void save_pref_themeMode(SharedPreferences.Editor ed) {
        ed.putString("key_theme_mode", "" + this.getInt(PREF_Int_themeMode));
    }

    void save_pref_visualizerGlobalSession(SharedPreferences.Editor ed) {
        ed.putBoolean("pref_visualizerGlobalSession", this.getBool(PREF_Bool_visualizerUseGlobalSession));
    }

    void load_pref_visualizerFrameRateLimit(SharedPreferences settings) {
        String valueStr = preferencesGetStringSafe(settings, "pref_visualizerFrameRateLimit", "60");
        int valueInt = Utils.strToIntSafe(valueStr);
        if (valueInt != 60 && valueInt != 120) {
            valueInt = 60;
        }
        this.setInt(PREF_Int_visualizerFrameRateLimit, valueInt);
    }

    void load_pref_visualizerResolutionScale(SharedPreferences settings) {
        String scaleStr = preferencesGetStringSafe(settings, "pref_visualizerResolutionScale", "1.5");
        this.setString(PREF_String_visualizerResolutionScale, scaleStr);
    }

    void save_pref_visualizerFrameRateLimit(SharedPreferences.Editor ed) {
        ed.putString("pref_visualizerFrameRateLimit", "" + this.getInt(PREF_Int_visualizerFrameRateLimit));
        ed.putString("pref_visualizerResolutionScale", this.getString(PREF_String_visualizerResolutionScale));
    }

    public SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }

    public void saveAddTokenList(List<String> entrys, Context context, String key) {

        SharedPreferences preferences = getPreferences(context);
        String strdata = preferencesGetStringSafe(preferences, key, "");
        List<String> listedFolders = UtilsSerialize.deserializeIterableAsList(";", strdata);

        SharedPreferences.Editor ed = preferences.edit();
        ed.putString(key, UtilsSerialize.serializeIterableSkipInvalidWithAdd(";", listedFolders, entrys, true));
        ed.apply();
    }

    public void prefAddLibraryFolderGenerateHash(String folderPath, Context context) {

        MultiList<String, String> entrys = prefGetLibraryFolders(context);

        Random rnd = new Random();

        String newIdHash;
        int maxCount = 1000000;
        int counter = 0;
        do {
            counter++;
            newIdHash = "" + rnd.nextInt(maxCount);
        } while (entrys.contains1(newIdHash) && counter < maxCount);

        prefAddLibraryFolder(newIdHash, folderPath, context);
    }

    public void prefAddLibraryFolder(String idhash, String folderPath, Context context) {
        if (idhash.contains(";")) return;
        if (idhash.contains(":")) return;
        if (folderPath.contains(";")) return;
        if (folderPath.contains(":")) return;

        SharedPreferences preferences = getPreferences(context);
        String strdata = getLibFoldersString(preferences);
        List<String> listedFolders = UtilsSerialize.deserializeIterableAsList(";", strdata);

        String entryStr = idhash + ":" + folderPath;

        SharedPreferences.Editor ed = preferences.edit();
        ed.putString("libFolders", UtilsSerialize.serializeIterableSkipInvalidWithAdd(";", listedFolders, entryStr, true));
        ed.apply();
    }

    public void prefRemoveLibraryFolder(String idHash, String folderPath, Context context) {

        SharedPreferences preferences = getPreferences(context);
        String strdata = getLibFoldersString(preferences);
        List<String> listedFolders = UtilsSerialize.deserializeIterableAsList(";", strdata);

        String entryStr = idHash + ":" + folderPath;

        SharedPreferences.Editor ed = preferences.edit();
        ed.putString("libFolders", UtilsSerialize.serializeIterableSkipInvalidWithExclude(";", listedFolders, entryStr, true));
        ed.apply();
    }

    public MultiList<String, String> prefGetLibraryFolders(Context context) {
        SharedPreferences preferences = getPreferences(context);

        String strdata = getLibFoldersString(preferences);
        List<String> entryStr = UtilsSerialize.deserializeIterableAsList(";", strdata);

        MultiList<String, String> result = new MultiList<>(entryStr.size());
        for (String s : entryStr) {
            int index = s.indexOf(":");
            if (index < 0) continue;
            String s1 = s.substring(0, index);
            String s2 = s.substring(index + 1);
            result.add(new Tuple2<>(s1, s2));
        }

        return result;
    }

    String getLibFoldersString(SharedPreferences preferences) {
        if (defaultFolderString == null) {
            StringBuilder strbld = new StringBuilder();

            strbld.append("001:");
            strbld.append("/storage");
            strbld.append(";");

            try {
                strbld.append("002:");
                strbld.append(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DCIM).getCanonicalPath());
                strbld.append(";");
            } catch (IOException ignored) {
            }

            try {
                strbld.append("003:");
                strbld.append(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_MOVIES).getCanonicalPath());
                strbld.append(";");
            } catch (IOException ignored) {
            }

            try {
                strbld.append("004:");
                strbld.append(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_MUSIC).getCanonicalPath());

            } catch (IOException ignored) {
            }

            defaultFolderString = strbld.toString();
        }

        return preferencesGetStringSafe(preferences, "libFolders", defaultFolderString);
    }

    public void prefAddStandalonePlaylistGenerateHash(Context context, String path, boolean preventDuplicates) {

        List<String> pathList = new ArrayList<>(1);
        pathList.add(path);
        prefAddStandalonePlaylistGenerateHash(context, pathList, preventDuplicates);
    }

    public void prefAddStandalonePlaylistGenerateHash(Context context, List<String> path, boolean preventDuplicates) {

        MultiList<String, String> entrys = prefGetStandalonePlaylists(context);

        List<String> finalEntrys = new ArrayList<>();

        Random rnd = new Random();

        for (String pth : path) {

            if (preventDuplicates)
                if (entrys.contains2(pth)) continue;

            String newidhash;
            do {
                newidhash = "" + rnd.nextInt(1000000);
            } while (entrys.contains1(newidhash));

            if (newidhash.contains(";")) continue;
            if (newidhash.contains(":")) continue;
            if (pth.contains(";")) continue;
            if (pth.contains(":")) continue;

            finalEntrys.add(newidhash + ":" + pth);
        }

        saveAddTokenList(finalEntrys, context, "libStandalonePlaylists");

    }

    public void prefRemoveStandalonePlaylist(String idHash, String folderPath, Context context) {

        SharedPreferences preferences = getPreferences(context);
        String strdata = preferencesGetStringSafe(preferences, "libStandalonePlaylists", "");
        List<String> listedFolders = UtilsSerialize.deserializeIterableAsList(";", strdata);

        String entryStr = idHash + ":" + folderPath;

        SharedPreferences.Editor ed = preferences.edit();
        ed.putString("libStandalonePlaylists", UtilsSerialize.serializeIterableSkipInvalidWithExclude(";", listedFolders, entryStr, true));
        ed.apply();
    }

    public MultiList<String, String> prefGetStandalonePlaylists(Context context) {
        SharedPreferences preferences = getPreferences(context);

        String strData = preferencesGetStringSafe(preferences, "libStandalonePlaylists", "");
        List<String> entryStr = UtilsSerialize.deserializeIterableAsList(";", strData);

        MultiList<String, String> result = new MultiList<>(entryStr.size());
        for (String s : entryStr) {

            int index = s.indexOf(":");
            if (index < 0) continue;
            String s1 = s.substring(0, index);
            String filepath = s.substring(index + 1);

            if (UtilsFileSys.fileExists(filepath))
                result.add(new Tuple2<>(s1, filepath));
        }

        return result;
    }

    public Element.CustomizationList getPrefThemeCustomizationData(int identifier) {
        int pref = themeCustomizationIdentifierToPref(identifier);
        if (pref >= 0) {
            return Element.CustomizationList.deserialize(getString(pref));
        } else {

            SharedPreferences preferences = getPreferences(PlayerCore.s().getAppContext());
            String str = preferencesGetStringSafe(preferences, "vThemeCust" + identifier, "");
            return Element.CustomizationList.deserialize(str);
        }
    }

    public void savePrefThemeCustomizationData(int identifier, Element.CustomizationList customizationList) {
        if (customizationList == null) return;
        String str = customizationList.serialize();
        if (str == null) return;

        int pref = themeCustomizationIdentifierToPref(identifier);
        if (pref >= 0) {
            setString(pref, str);
            SharedPreferences preferences = getPreferences(PlayerCore.s().getAppContext());
            SharedPreferences.Editor ed = preferences.edit();
            ed.putString("string" + (pref - offset_String), str);
            ed.apply();
        } else {

            SharedPreferences preferences = getPreferences(PlayerCore.s().getAppContext());
            SharedPreferences.Editor ed = preferences.edit();
            ed.putString("vThemeCust" + identifier, str);
            ed.apply();
        }
    }

    int themeCustomizationIdentifierToPref(int identifier) {
        if (identifier >= 0 && identifier <= 10)
            return PREF_String_vThemeCustomization0 + identifier;

        return -1;
    }

    public com.aylis.comp.visual.scene.VisualizerScene getPrefThemeScene(int identifier) {
        android.content.Context context = PlayerCore.s().getAppContext();
        java.io.File file = new java.io.File(context.getFilesDir(), "vThemeScene_" + identifier + ".json");
        String str = "";

        if (file.exists()) {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(file), "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append("\n");
                str = sb.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Migration fallback
            SharedPreferences preferences = getPreferences(context);
            str = preferencesGetStringSafe(preferences, "vThemeScene" + identifier, "");
            if (!str.isEmpty()) {
                // Save to file for next time
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                    fos.write(str.getBytes("UTF-8"));
                } catch (Exception e) {}
                preferences.edit().remove("vThemeScene" + identifier).apply(); // Clear RAM
            }
        }

        if (str.isEmpty()) return null;
        return com.aylis.comp.visual.scene.SceneSerializer.INSTANCE.fromJson(str);
    }

    public void savePrefThemeScene(int identifier, com.aylis.comp.visual.scene.VisualizerScene scene) {
        android.content.Context context = PlayerCore.s().getAppContext();
        java.io.File file = new java.io.File(context.getFilesDir(), "vThemeScene_" + identifier + ".json");
        SharedPreferences preferences = getPreferences(context);

        if (scene == null) {
            if (file.exists()) file.delete();
            preferences.edit().remove("vThemeScene" + identifier).apply();
            return;
        }

        String str = com.aylis.comp.visual.scene.SceneSerializer.INSTANCE.toJson(scene);
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
            fos.write(str.getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Ensure SharedPreferences is clean
        if (preferences.contains("vThemeScene" + identifier)) {
            preferences.edit().remove("vThemeScene" + identifier).apply();
        }
    }

    public static class CustomThemeInfo {
        public int id;
        public int baseId;
        public String name;

        public CustomThemeInfo(int id, int baseId, String name) {
            this.id = id;
            this.baseId = baseId;
            this.name = name;
        }
    }

    public List<CustomThemeInfo> getCustomThemes(Context context) {
        SharedPreferences preferences = getPreferences(context);
        String data = preferencesGetStringSafe(preferences, "pref_custom_themes_info", "[]");
        List<CustomThemeInfo> list = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(data);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                list.add(new CustomThemeInfo(
                        obj.getInt("id"),
                        obj.getInt("baseId"),
                        obj.optString("name", "Custom " + obj.getInt("id"))
                ));
            }
        } catch (JSONException e) {
            tlog.w("Failed to parse custom themes: " + e.getMessage());
        }
        return list;
    }

    public void saveCustomThemes(Context context, List<CustomThemeInfo> themes) {
        SharedPreferences preferences = getPreferences(context);
        SharedPreferences.Editor ed = preferences.edit();
        JSONArray jsonArray = new JSONArray();
        try {
            for (CustomThemeInfo t : themes) {
                JSONObject obj = new JSONObject();
                obj.put("id", t.id);
                obj.put("baseId", t.baseId);
                obj.put("name", t.name);
                jsonArray.put(obj);
            }
        } catch (JSONException ignored) {}
        ed.putString("pref_custom_themes_info", jsonArray.toString());
        ed.apply();
    }

    public int addCustomTheme(Context context, int baseId, String name) {
        List<CustomThemeInfo> themes = getCustomThemes(context);
        int nextId = 10;
        while (true) {
            boolean found = false;
            for (CustomThemeInfo t : themes) {
                if (t.id == nextId) {
                    found = true;
                    break;
                }
            }
            if (!found) break;
            nextId++;
        }
        CustomThemeInfo newTheme = new CustomThemeInfo(nextId, baseId, name);
        themes.add(newTheme);
        saveCustomThemes(context, themes);
        return nextId;
    }

    public void deleteCustomTheme(Context context, int id) {
        List<CustomThemeInfo> themes = getCustomThemes(context);
        for (int i = 0; i < themes.size(); i++) {
            if (themes.get(i).id == id) {
                themes.remove(i);

                SharedPreferences preferences = getPreferences(context);
                preferences.edit().remove("vThemeCust" + id).apply();
                break;
            }
        }
        saveCustomThemes(context, themes);
    }

    public List<String> getCustomPresets(Context context) {
        SharedPreferences preferences = getPreferences(context);
        String data = preferencesGetStringSafe(preferences, "pref_custom_presets_json", "[]");
        List<String> list = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(data);
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.getString(i));
            }
        } catch (JSONException e) {
            tlog.w("Failed to parse custom presets: " + e.getMessage());
        }
        return list;
    }

    public void saveCustomPresets(Context context, List<String> presets) {
        SharedPreferences preferences = getPreferences(context);
        SharedPreferences.Editor ed = preferences.edit();
        JSONArray jsonArray = new JSONArray();
        for (String s : presets) {
            jsonArray.put(s);
        }
        ed.putString("pref_custom_presets_json", jsonArray.toString());
        ed.apply();
    }

    public void addCustomPreset(Context context, String json) {
        List<String> presets = getCustomPresets(context);
        presets.add(json);
        saveCustomPresets(context, presets);
    }

    public void deleteCustomPreset(Context context, int index) {
        List<String> presets = getCustomPresets(context);
        if (index >= 0 && index < presets.size()) {
            presets.remove(index);
            saveCustomPresets(context, presets);
        }
    }

    public void resetTips() {
        this.setBool(PREF_Bool_tipShow_reorder, true);
    }

}

