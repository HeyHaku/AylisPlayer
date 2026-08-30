package com.aylis.comp.visual.core;

import android.graphics.Typeface;
import android.os.Environment;

import com.aylis.Common.tlog;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomFontManager {

    private static final String DEFAULT_FONT = "Default";

    public static String getFontsFolder() {
        android.content.Context ctx = com.aylis.MainActivity.getInstance();
        if (ctx != null) {
            java.io.File externalFilesDir = ctx.getExternalFilesDir(null);
            if (externalFilesDir != null) {
                return externalFilesDir.getAbsolutePath() + "/OpenPlayer/Fonts";
            }
        }
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/OpenPlayer/Fonts";
    }

    private static List<String> availableFontNames = new ArrayList<>();
    private static Map<String, File> fontFilesMap = new HashMap<>();
    private static Map<String, Typeface> typefaceCache = new HashMap<>();

    public static void createFolders() {
        File folder = new File(getFontsFolder());
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public static void scanFonts() {
        availableFontNames.clear();
        fontFilesMap.clear();
        
        availableFontNames.add(DEFAULT_FONT);

        File folder = new File(getFontsFolder());
        if (!folder.exists()) {
            folder.mkdirs();
        }

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        String name = file.getName();
                        if (name.toLowerCase().endsWith(".ttf") || name.toLowerCase().endsWith(".otf")) {
                            String displayName = name.substring(0, name.lastIndexOf('.'));
                            // Remove spaces and special characters for the UI string if needed, or just keep it
                            displayName = displayName.replace(" ", "_");
                            availableFontNames.add(displayName);
                            fontFilesMap.put(displayName, file);
                        }
                    }
                }
            }
        }
    }

    public static List<String> getAvailableFontNames() {
        scanFonts();
        return availableFontNames;
    }

    public static String getUIString() {
        List<String> fonts = getAvailableFontNames();
        if (fonts.isEmpty()) {
            return "font Default";
        }
        
        StringBuilder sb = new StringBuilder("font");
        for (String font : fonts) {
            sb.append(" ").append(font);
        }
        return sb.toString();
    }

    public static String getFontFilePath(String fontName) {
        if (fontName == null || fontName.isEmpty() || fontName.equals(DEFAULT_FONT)) {
            return null;
        }
        if (fontFilesMap.isEmpty()) {
            scanFonts();
        }
        File file = fontFilesMap.get(fontName);
        if (file != null && file.exists()) {
            return file.getAbsolutePath();
        }
        return null;
    }

    public static Typeface getTypeface(String fontName) {
        if (fontName == null || fontName.isEmpty() || fontName.equals(DEFAULT_FONT)) {
            return Typeface.DEFAULT;
        }

        if (fontFilesMap.isEmpty()) {
            scanFonts();
        }

        if (typefaceCache.containsKey(fontName)) {
            return typefaceCache.get(fontName);
        }

        File fontFile = fontFilesMap.get(fontName);
        if (fontFile != null && fontFile.exists()) {
            try {
                Typeface tf = Typeface.createFromFile(fontFile);
                typefaceCache.put(fontName, tf);
                return tf;
            } catch (Exception e) {
                tlog.w("Failed to load custom font: " + fontName);
            }
        }

        return Typeface.DEFAULT;
    }
}
