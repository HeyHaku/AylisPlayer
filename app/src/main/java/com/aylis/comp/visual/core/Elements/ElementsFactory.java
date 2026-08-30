package com.aylis.comp.visual.core.Elements;

import com.aylis.Common.tlog;
import com.aylis.comp.visual.core.Elements.Images.ImageElement;
import com.aylis.comp.visual.core.Elements.VideoElement;
import com.aylis.comp.visual.core.Elements.Shaders.BlurElement;
import com.aylis.comp.visual.core.Elements.Shaders.Box3DElement;
import com.aylis.comp.visual.core.Elements.Shaders.BulgePinchEffectElement;
import com.aylis.comp.visual.core.Elements.Shaders.ColorCorrectionElement;
import com.aylis.comp.visual.core.Elements.Shaders.CurveEffectElement;
import com.aylis.comp.visual.core.Elements.Shaders.EdgeEffectElement;
import com.aylis.comp.visual.core.Elements.Shaders.FOVElement;
import com.aylis.comp.visual.core.Elements.Shaders.GlitchEffectElement;
import com.aylis.comp.visual.core.Elements.Shaders.GodraysEffectElement;
import com.aylis.comp.visual.core.Elements.Shaders.Kaleidoscope;
import com.aylis.comp.visual.core.Elements.Shaders.LiquifyEffectElement;
import com.aylis.comp.visual.core.Elements.Shaders.PixelEffectElement;
import com.aylis.comp.visual.core.Elements.Shaders.SpherifyEffectElement;
import com.aylis.comp.visual.core.Elements.Shaders.TwirlEffectElement;
import com.aylis.comp.visual.core.Elements.Shaders.VignetteElement;
import com.aylis.comp.visual.core.Elements.Shaders.ZoomBlurEffectElement;
import com.aylis.comp.visual.core.Elements.Shaders.MirrorEffectElement;
import com.aylis.comp.visual.core.Elements.MotionBlurEffectElement;
import com.aylis.comp.visual.core.Elements.Shaders.RainDropsEffectElement;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ElementsFactory {
    private static final Map<String, Class<? extends Element>> elementTypes = new LinkedHashMap<>();
    private static final Map<String, String> displayNames = new LinkedHashMap<>();
    public static final String typeNameNone = "None";

    static {
        elementTypes.put(typeNameNone, null);
        elementTypes.put("RootElement", RootElement.class);

        elementTypes.put("TextElement", TextElement.class);
        elementTypes.put("AudioDataProviderElement", AudioDataProviderElement.class);
        elementTypes.put("BlurElement", BlurElement.class);
        elementTypes.put("BlurGroupElement", BlurElement.class);
        elementTypes.put("ParticlesElement", ParticlesElement.class);
        elementTypes.put("AlbumArtPictureElement", ImageElement.class);
        elementTypes.put("SegmentElement", SegmentElement.class);
        elementTypes.put("AlbumArtBlurredPictureElement", ImageElement.class);
        elementTypes.put("BackgroundElement", BackgroundElement.class);
        elementTypes.put("FxaaGroupElement", FxaaGroupElement.class);
        elementTypes.put("ImageElement", ImageElement.class);
        elementTypes.put("VideoElement", VideoElement.class);
        elementTypes.put("PreCompElement", PreCompElement.class);
        elementTypes.put("DummyElement", DummyElement.class);
        elementTypes.put("FOV", FOVElement.class);
        elementTypes.put("Kaleidoscope", Kaleidoscope.class);
        elementTypes.put("LiquifyEffect", LiquifyEffectElement.class);
        elementTypes.put("3DBox", Box3DElement.class);
        elementTypes.put("CurveEffect", CurveEffectElement.class);
        elementTypes.put("GodraysEffect", GodraysEffectElement.class);
        elementTypes.put("TwirlEffect", TwirlEffectElement.class);
        elementTypes.put("PixelEffect", PixelEffectElement.class);
        elementTypes.put("ZoomBlurEffect", ZoomBlurEffectElement.class);
        elementTypes.put("ColorCorrection", ColorCorrectionElement.class);
        elementTypes.put("BulgePinchEffect", BulgePinchEffectElement.class);
        elementTypes.put("Spherify", SpherifyEffectElement.class);
        elementTypes.put("EdgeEffect", EdgeEffectElement.class);
        elementTypes.put("GlitchEffect", GlitchEffectElement.class);
        elementTypes.put("Vignette", VignetteElement.class);
        elementTypes.put("MirrorEffect", MirrorEffectElement.class);
        elementTypes.put("MotionBlurEffect", MotionBlurEffectElement.class);
        elementTypes.put("RgbSplitEffect", RgbSplitEffectElement.class);
        elementTypes.put("RainDropsEffect", RainDropsEffectElement.class);
        elementTypes.put("RedLandscape", RedLandscapeElement.class);

        displayNames.put("SolidCircleElement", "Solid Circle");
        displayNames.put("TextElement", "Text");
        displayNames.put("AudioDataProviderElement", "AudioCore");
        displayNames.put("BlurElement", "Blur");
        displayNames.put("BlurGroupElement", "Blur");
        displayNames.put("ParticlesElement", "Particles");
        displayNames.put("SegmentElement", "Bars/Waves");
        displayNames.put("BackgroundElement", "Master Scene");
        displayNames.put("FxaaGroupElement", "FXAA Antialiasing");
        displayNames.put("ImageElement", "Media");
        displayNames.put("VideoElement", "Video");
        displayNames.put("PreCompElement", "Scene layer");
        displayNames.put("DummyElement", "Dummy element");
        displayNames.put("FOV", "FOV Effect");
        displayNames.put("Kaleidoscope", "Kaleidoscope");
        displayNames.put("LiquifyEffect", "Liquify Effect");
        displayNames.put("3DBox", "3D Box");
        displayNames.put("CurveEffect", "Curve Effect");
        displayNames.put("GodraysEffect", "Godrays Effect");
        displayNames.put("TwirlEffect", "Twirl Effect");
        displayNames.put("PixelEffect", "Pixel Effect");
        displayNames.put("ZoomBlurEffect", "Zoom Blur Effect");
        displayNames.put("ColorCorrection", "Color Correction");
        displayNames.put("BulgePinchEffect", "Bulge Pinch Effect");
        displayNames.put("Spherify", "Spherify");
        displayNames.put("EdgeEffect", "Edge Effect");
        displayNames.put("GlitchEffect", "Glitch Effect");
        displayNames.put("Vignette", "Vignette Border");
        displayNames.put("MirrorEffect", "Mirror Effect");
        displayNames.put("MotionBlurEffect", "Motion Blur Effect");
        displayNames.put("RgbSplitEffect", "RGB Split Effect");
        displayNames.put("RainDropsEffect", "Rain Drops");
        displayNames.put("RedLandscape", "Red Landscape");
    }

    public static Element create(String name) {
        Class<? extends Element> cls = elementTypes.get(name);
        if (cls != null) {
            try {
                return cls.newInstance();
            } catch (Exception e) {
                tlog.w("Failed to create element for typeName: " + name + ", error: " + e.getMessage());
            }
        }

        try {
            String className;
            if (name.contains(".")) {
                className = name;
            } else {
                className = "com.aylis.comp.visual.core.Elements." + name;
            }
            Class<?> clazz = Class.forName(className);
            if (Element.class.isAssignableFrom(clazz)) {
                return (Element) clazz.newInstance();
            }
        } catch (Exception e) {
            tlog.w("Failed to find or instantiate class for name: " + name + ", error: " + e.getMessage());
        }

        return null;
    }

    public static String getTypeName(Element element) {
        if (element == null) {
            return typeNameNone;
        }

        for (Map.Entry<String, Class<? extends Element>> entry : elementTypes.entrySet()) {
            Class<? extends Element> value = entry.getValue();
            if (value != null && value.equals(element.getClass())) {
                return entry.getKey();
            }
        }
        for (Map.Entry<String, Class<? extends Element>> entry : elementTypes.entrySet()) {
            Class<? extends Element> value = entry.getValue();
            if (value != null && value.isInstance(element)) {
                return entry.getKey();
            }
        }
        return element.getClass().getSimpleName();
    }

    public static Set<String> getAddableTypeNames() {
        Set<String> set = new LinkedHashSet<>();
        for (String str : elementTypes.keySet()) {
            if (!str.equals(typeNameNone) && !str.equals("RootElement") && !str.equals("BackgroundElement")
                    && !str.equals("BlurGroupElement") && !str.equals("AlbumArtPictureElement")
                    && !str.equals("AlbumArtBlurredPictureElement") && !str.equals("FxaaGroupElement")) {
                set.add(str);
            }
        }
        return set;
    }

    public static String getElementDisplayName(String typeName) {
        String displayName = displayNames.get(typeName);
        if (displayName != null) {
            return displayName;
        }
        String cleanName = typeName.endsWith("Element") ? typeName.substring(0, typeName.length() - 7) : typeName;
        StringBuilder sb = new StringBuilder();
        boolean lastLower = false;
        for (int i = 0; i < cleanName.length(); i++) {
            char c = cleanName.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && lastLower) {
                sb.append(' ');
            }
            sb.append(c);
            lastLower = Character.isLowerCase(c);
        }
        return sb.toString();
    }
}
