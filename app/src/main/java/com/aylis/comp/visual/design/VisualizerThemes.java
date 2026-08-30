package com.aylis.comp.visual.design;

import android.content.Context;
import com.aylis.Common.Tuple2;
import com.aylis.Common.tlog;
import com.aylis.PlayerCore;
import com.aylis.comp.AppPreferences.AppPreferences;
import com.aylis.comp.visual.ui.VisualizerThemeInfo;
import com.aylis.comp.visual.core.Elements.RootElement;
import com.aylis.comp.visual.core.Elements.Element;
import com.aylis.comp.visual.core.Elements.BackgroundElement;
import com.aylis.comp.visual.scene.SceneBuilder;
import com.aylis.comp.visual.scene.VisualizerScene;
import com.aylis.comp.visual.scene.SceneSerializer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class VisualizerThemes {

    public interface IVisualizerFactory {
        RootElement create(int themeId);
    }

    private final List<Tuple2<VisualizerThemeInfo, IVisualizerFactory>> themesList = new ArrayList<>();
    private int baseThemesCount;
    private static VisualizerThemes instance = null;

    public static VisualizerThemes s() {
        if (instance == null)
            new VisualizerThemes();
        return instance;
    }

    VisualizerThemes() {
        instance = this;
        for (int i = 0; i < 9; i++) {
            final int presetIndex = i;
            themesList.add(new Tuple2<>(new VisualizerThemeInfo(presetIndex, 0), new IVisualizerFactory() {
                @Override
                public RootElement create(int themeId) {
                    return createPresetFromAssets(themeId, "presets/preset_" + presetIndex + ".ayp");
                }
            }));
        }

        baseThemesCount = themesList.size();
    }

    private RootElement createPresetFromAssets(int themeId, String assetPath) {
        Context context = PlayerCore.s().getAppContext();
        if (context == null) return createEmptyFallback(themeId);

        try (InputStream is = context.getAssets().open(assetPath);
             ZipInputStream zis = new ZipInputStream(is)) {

            String sceneJson = null;
            String customizationJson = null;
            ZipEntry entry;
            byte[] buffer = new byte[4096];

            // Распаковываем ZIP-поток .ayp файла
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int count;
                while ((count = zis.read(buffer)) != -1) {
                    baos.write(buffer, 0, count);
                }
                byte[] dataBytes = baos.toByteArray();

                if ("scene.json".equals(name) || name.endsWith("/scene.json")) {
                    sceneJson = new String(dataBytes, StandardCharsets.UTF_8);
                } else if ("customization.json".equals(name) || name.endsWith("/customization.json")) {
                    customizationJson = new String(dataBytes, StandardCharsets.UTF_8);
                }
                zis.closeEntry();
            }

            RootElement root = null;
            if (sceneJson != null && !sceneJson.isEmpty()) {
                VisualizerScene scene = SceneSerializer.INSTANCE.fromJson(sceneJson);
                if (scene != null) {
                    root = SceneBuilder.INSTANCE.buildFromScene(themeId, scene);
                }
            }

            if (root == null) {
                BackgroundElement bg = new BackgroundElement();
                bg.setBackgroundColor(0.05f, 0.05f, 0.05f, 1.0f);
                root = new RootElement(themeId, bg);
            }

            if (customizationJson != null && !customizationJson.isEmpty()) {
                Element.CustomizationList customizationList = Element.CustomizationList.deserialize(customizationJson);
                if (customizationList != null) {
                    root.setCustomization(customizationList);
                }
            }

            return root;

        } catch (Exception e) {
            tlog.w("Failed to load visual preset from assets: " + assetPath + ", error: " + e.getMessage());
            return createEmptyFallback(themeId);
        }
    }

    private RootElement createEmptyFallback(int themeId) {
        BackgroundElement fallbackBg = new BackgroundElement();
        fallbackBg.setBackgroundColor(0.1f, 0.1f, 0.1f, 1.0f);
        return new RootElement(themeId, fallbackBg);
    }

    public void loadCustomThemes() {
        while (themesList.size() > baseThemesCount) {
            themesList.remove(themesList.size() - 1);
        }

        Context context = PlayerCore.s().getAppContext();
        if (context == null) return;

        List<AppPreferences.CustomThemeInfo> customInfo = AppPreferences.createOrGetInstance().getCustomThemes(context);
        for (final AppPreferences.CustomThemeInfo info : customInfo) {
            IVisualizerFactory baseFactory = (info.baseId >= 0 && info.baseId < baseThemesCount)
                    ? themesList.get(info.baseId).obj2
                    : themesList.get(0).obj2;

            int baseIconRes = themesList.get(info.baseId >= 0 && info.baseId < baseThemesCount ? info.baseId : 0).obj1.iconResId;

            themesList.add(new Tuple2<>(
                    new VisualizerThemeInfo(info.id, baseIconRes),
                    new IVisualizerFactory() {
                        @Override
                        public RootElement create(int themeId) {
                            return baseFactory.create(themeId);
                        }
                    }
            ));
        }
    }

    public List<Tuple2<VisualizerThemeInfo, IVisualizerFactory>> getThemesList() {
        return themesList;
    }

    public RootElement getThemeObject(int themeId) {
        for (Tuple2<VisualizerThemeInfo, IVisualizerFactory> t : themesList) {
            if (t.obj1.id == themeId) {
                return t.obj2.create(themeId);
            }
        }
        return null;
    }
}