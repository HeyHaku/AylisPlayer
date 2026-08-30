package com.aylis.comp.visual.core.Elements;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreCompManager {
    private static final Map<String, WeakReference<PreCompElement>> preComps = new HashMap<>();
    private static final List<String> tempPreCompNames = new ArrayList<>();

    public static synchronized void register(String name, PreCompElement element) {
        if (name != null && !name.isEmpty()) {
            if (element != null) {
                preComps.put(name, new WeakReference<>(element));
            } else {
                if (!tempPreCompNames.contains(name)) {
                    tempPreCompNames.add(name);
                }
            }
        }
    }

    public static synchronized void unregister(String name) {
        preComps.remove(name);
        tempPreCompNames.remove(name);
    }

    public static synchronized PreCompElement get(String name) {
        WeakReference<PreCompElement> ref = preComps.get(name);
        return ref != null ? ref.get() : null;
    }

    public static synchronized void clear() {
        tempPreCompNames.clear();
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, WeakReference<PreCompElement>> entry : preComps.entrySet()) {
            if (entry.getValue().get() == null) {
                toRemove.add(entry.getKey());
            }
        }
        for (String k : toRemove) {
            preComps.remove(k);
        }
    }

    public static synchronized String[] getPreCompNames() {
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, WeakReference<PreCompElement>> entry : preComps.entrySet()) {
            PreCompElement pe = entry.getValue().get();
            if (pe != null) {
                names.add(entry.getKey());
            }
        }
        for (String name : tempPreCompNames) {
            if (!names.contains(name)) {
                names.add(name);
            }
        }
        Collections.sort(names);
        names.add(0, "None");
        return names.toArray(new String[0]);
    }

    public static synchronized void scanAndRegister(ElementGroup group) {
        if (group == null) return;
        for (Element e : group.getChildList()) {
            if (e instanceof PreCompElement) {
                PreCompElement pe = (PreCompElement) e;
                register(pe.getPreCompName(), pe);
            }
            if (e instanceof ElementGroup) {
                scanAndRegister((ElementGroup) e);
            }
        }
    }
}
