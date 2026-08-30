

package com.aylis.comp.visual.core.Elements;

import android.graphics.PointF;
import android.graphics.RectF;
import com.aylis.Common.Vec2f;
import com.aylis.Common.Vec2i;
import com.aylis.Common.tlog;
import com.aylis.comp.visual.core.Elements.bars.AudioBars.ISegmentDataProvider;
import com.aylis.comp.visual.core.Graphic.RenderState;
import com.aylis.comp.visual.core.modifiers.ShakeModifier;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.aylis.Common.Tuple2;
import com.aylis.Common.MultiList;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.FrameBuffer;

public abstract class Element {

    protected ElementGroup parent;
    protected long highlightEndTime = 0;
    private int blendMode = 0;

    protected java.util.HashMap<String, String> customTags = new java.util.HashMap<>();

    private String descText = "";

    private boolean glResourcesCreated = false;

    protected float posX = 0.5f, posY = 0.5f;
    protected float localPosX = 0.5f, localPosY = 0.5f;
    protected int anchorX = 0, anchorY = 0;
    private boolean posXIsUniform, posYIsUniform = false;
    private boolean localPosXIsUniform, localPosYIsUniform = false;
    private com.aylis.comp.visual.core.Elements.Base.MVariableFloat scaleXMVar = com.aylis.comp.visual.core.Elements.Base.MVariableFloat.Companion.createConstantFloat(1.0f);
    private com.aylis.comp.visual.core.Elements.Base.MVariableFloat scaleYMVar = com.aylis.comp.visual.core.Elements.Base.MVariableFloat.Companion.createConstantFloat(1.0f);
    private boolean scaleXIsUniform = false, scaleYIsUniform = false;
    protected com.aylis.comp.visual.core.Elements.Base.MVariableFloat rotMVar = com.aylis.comp.visual.core.Elements.Base.MVariableFloat.Companion.createConstantFloat(0.0f);
    protected boolean lockScaleRatio = false;
    protected com.aylis.comp.visual.core.modifiers.ElementAnimator animator = new com.aylis.comp.visual.core.modifiers.ElementAnimator();
    protected boolean useAnimatorMeasures = true;
    protected boolean visible = true;

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Element() {
        glResourcesCreated = false;
    }

    public void setBlendMode(int mode) {
        blendMode = mode;
    }

    public int getBlendMode() {
        return blendMode;
    }

    public void setPosition(float x, float y) {
        posX = x;
        posY = y;
    }

    public void setLocalPosition(float x, float y) {
        localPosX = x;
        localPosY = y;
    }

    public void setPositionUniform(boolean x, boolean y) {
        posXIsUniform = x;
        posYIsUniform = y;
    }

    public void setLocalPositionUniform(boolean x, boolean y) {
        localPosXIsUniform = x;
        localPosYIsUniform = y;
    }

    public void setScale(float x, float y) {
        scaleXMVar = com.aylis.comp.visual.core.Elements.Base.MVariableFloat.Companion.createConstantFloat(x);
        scaleYMVar = com.aylis.comp.visual.core.Elements.Base.MVariableFloat.Companion.createConstantFloat(y);
    }

    public void setScaleUniform(boolean x, boolean y) {
        scaleXIsUniform = x;
        scaleYIsUniform = y;
    }

    public void setScaleUniform(boolean z) {
        scaleXIsUniform = z;
        scaleYIsUniform = z;
    }

    protected float measureDrawRot(Meter meter) {
        return (rotMVar.getValueAsFloat(meter, 0.0f) + animator.getShakeOffsetRotation()) % 1.0f;
    }

    public RectF measureDrawRect(Meter meter) {
        float elementX = meter.measureScreenSpaceAnchorX(anchorX) + meter.measureScreenSpaceX(posX, anchorX != 0 || posXIsUniform);
        float elementY = meter.measureScreenSpaceAnchorY(anchorY) + meter.measureScreenSpaceY(posY, anchorY != 0 || posYIsUniform);

        elementX += animator.getShakeOffset().x;
        elementY += animator.getShakeOffset().y;

        float elementW = meter.measureScreenScaleX(scaleXMeasured(meter), scaleXIsUniform);
        float elementH = meter.measureScreenScaleY(scaleYMeasured(meter), lockScaleRatio ? scaleXIsUniform : scaleYIsUniform);

        elementX -= meter.measureLocalSpaceX(localPosX, localPosXIsUniform, elementW, elementH);
        elementY -= meter.measureLocalSpaceY(localPosY, localPosYIsUniform, elementW, elementH);

        return new RectF(elementX, elementY, elementX + elementW, elementY + elementH);
    }

   public RectF measureDrawRect(Meter meter, Vec2i elementDim) {
        float elementX = meter.measureScreenSpaceAnchorX(anchorX) + meter.measureScreenSpaceX(posX, anchorX != 0 || posXIsUniform);
        float elementY = meter.measureScreenSpaceAnchorY(anchorY) + meter.measureScreenSpaceY(posY, anchorY != 0 || posYIsUniform);

        elementX += animator.getShakeOffset().x;
        elementY += animator.getShakeOffset().y;

        float elementW = elementDim.x;
        float elementH = elementDim.y;

        elementX -= meter.measureLocalSpaceX(localPosX, localPosXIsUniform, elementW, elementH);
        elementY -= meter.measureLocalSpaceY(localPosY, localPosYIsUniform, elementW, elementH);

        return new RectF(elementX, elementY, elementX + elementW, elementY + elementH);
    }

    private float scaleXMeasured(Meter meter) {
        return scaleXMVar.getValueAsFloat(meter, 1.0f) * animator.getShakeOffsetScale();
    }

    private float scaleYMeasured(Meter meter) {
        if (lockScaleRatio) {
            return scaleXMVar.getValueAsFloat(meter, 1.0f) * animator.getShakeOffsetScale();
        }
        return scaleYMVar.getValueAsFloat(meter, 1.0f) * animator.getShakeOffsetScale();
    }

    PointF measureDrawScaleRect(Meter meter) {
        float elementW = meter.measureScaleX(scaleXMeasured(meter), scaleXIsUniform);
        float elementH = meter.measureScaleY(scaleYMeasured(meter), lockScaleRatio ? scaleXIsUniform : scaleYIsUniform);

        return new PointF(elementW, elementH);
    }

    public void removeFromParent() {
        if (parent != null)
            parent.removeChild(this);
    }

    protected void markNeedReCreateGLResources() {
        glResourcesCreated = false;
    }

    public void reCreateGLResources(RenderState renderData) {
        glResourcesCreated = false;
    }

    protected void onRenderCheckResources(RenderState renderData) {
        if (!glResourcesCreated)
            onCreateGLResources(renderData);
        glResourcesCreated = true;
    }

    public void onEarlyUpdate(RenderState renderData, FrameBuffer resultFB) {
        if (!visible) return;
    }

    public void onRender(RenderState renderData, FrameBuffer resultFB) {
        if (!visible) return;
        onRenderCheckResources(renderData);

        renderData.bindFrameBuffer(resultFB);
        renderData.setBlendMode(blendMode);

        animator.onRender(renderData);
    }

    public void drawHighlightRecursive(RenderState renderData) {
        long now = System.currentTimeMillis();
        if (now < highlightEndTime) {
            long remaining = highlightEndTime - now;
            int alpha = (int) (180 * remaining / 1000);
            if (alpha > 0) {
                int color = (alpha << 24) | 0x00FFFFFF;
                android.graphics.RectF rect = measureDrawRect(renderData.res.meter);
                float thickness = 4f;
                com.aylis.comp.visual.core.Graphic.IAtlasTexture whiteTex = renderData.res.getAtlasTexWhite();
                if (whiteTex != null) {
                    renderData.res.getBufferRenderer().drawRectangleRightBottom(renderData, rect.left, rect.top, 0f, rect.right, rect.top + thickness, color, Vec2f.zero, Vec2f.one, whiteTex);
                    renderData.res.getBufferRenderer().drawRectangleRightBottom(renderData, rect.left, rect.bottom - thickness, 0f, rect.right, rect.bottom, color, Vec2f.zero, Vec2f.one, whiteTex);
                    renderData.res.getBufferRenderer().drawRectangleRightBottom(renderData, rect.left, rect.top, 0f, rect.left + thickness, rect.bottom, color, Vec2f.zero, Vec2f.one, whiteTex);
                    renderData.res.getBufferRenderer().drawRectangleRightBottom(renderData, rect.right - thickness, rect.top, 0f, rect.right, rect.bottom, color, Vec2f.zero, Vec2f.one, whiteTex);
                }
            }
        }
    }

    public void updateRenderStates(RenderState renderData, FrameBuffer resultFB) {
        renderData.bindFrameBuffer(resultFB);
        renderData.setBlendMode(blendMode);
    }

    protected com.aylis.comp.visual.core.Elements.Base.Transform2D elementTransform = new com.aylis.comp.visual.core.Elements.Base.Transform2D();
    private float[] transformResult = new float[8];

    protected void drawRotatedTexture(RenderState renderData, android.graphics.RectF drawRect, float z, int intcolor, Vec2f tex0, Vec2f tex1, com.aylis.comp.visual.core.Graphic.IAtlasTexture tex) {
        float rotation = measureDrawRot(renderData.res.meter);
        if (rotation == 0.0f) {
            renderData.res.getBufferRenderer().drawRectangleRightBottom(renderData, drawRect.left, drawRect.top, z, drawRect.right, drawRect.bottom, intcolor, tex0, tex1, tex);
            return;
        }

        elementTransform.reset();
        elementTransform.rotate(rotation * 360.0f, drawRect.centerX(), drawRect.centerY());
        elementTransform.mapRectToVertices(drawRect, transformResult);

        renderData.res.getBufferRenderer().drawRectangle(renderData,
                transformResult[0], transformResult[1],
                transformResult[2], transformResult[3],
                transformResult[4], transformResult[5],
                transformResult[6], transformResult[7],
                z, intcolor, tex0, tex1, tex);
    }

    protected void onCreateGLResources(RenderState renderData) {
    }

    public boolean getCustomization(Element.CustomizationList customization, int customizationIndex) {

        Element.CustomizationData customizationData = new CustomizationData(customization.getNewDataJSONObject());
        onReadCustomization(customizationData);

        if (customTags != null) {
            Iterator<String> keys = customizationData.GetAllProperties();
            while(keys.hasNext()) {
                String key = keys.next();
                if (customTags.containsKey(key)) {
                    customizationData.setPropertyGroupTag(key, customTags.get(key));
                }
            }
        }

        return true;
    }

    public boolean setCustomization(Element.CustomizationList customization, Integer[] dataCounter) {
        return setCustomization(customization, dataCounter, -1);
    }

    public boolean setCustomization(Element.CustomizationList customization, Integer[] dataCounter, int selectedIndex) {
        int currentIndex = dataCounter[0];

        Element.CustomizationData customizationData = customization.getData(dataCounter[0]);
        dataCounter[0]++;
        if (customizationData == null) return false;

        Iterator<String> keys = customizationData.GetAllProperties();
        while(keys.hasNext()) {
            String key = keys.next();
            String rawTag = customizationData.getRawPropertyGroupTag(key);
            if (rawTag != null && !rawTag.isEmpty()) {
                customTags.put(key, rawTag);
            }
        }

        onApplyCustomization(customizationData);

        if (currentIndex == selectedIndex) {
            this.highlightEndTime = System.currentTimeMillis() + 1000;
        }

        return true;
    }

    public void onApplyCustomization(Element.CustomizationData customizationData) {
        if (!(this instanceof RootElement)) {
            visible = customizationData.getPropertyBool("visible", visible);
        }

        Vec2f pos = customizationData.getPropertyVec2f("position", new Vec2f(posX, posY));
        posX = pos.x;
        posY = pos.y;

        Vec2f scl = customizationData.getPropertyVec2f("scale", new Vec2f(-999f, -999f));
        if (scl.x != -999f) {
            scaleXMVar = com.aylis.comp.visual.core.Elements.Base.MVariableFloat.Companion.createConstantFloat(scl.x);
            scaleYMVar = com.aylis.comp.visual.core.Elements.Base.MVariableFloat.Companion.createConstantFloat(scl.y);
            customizationData.jsonObj.remove("scale");
        }
        scaleXMVar = customizationData.getPropertyMVariableFloat("scaleX", scaleXMVar);
        scaleYMVar = customizationData.getPropertyMVariableFloat("scaleY", scaleYMVar);

        Vec2f anc = customizationData.getPropertyVec2f("anchor", new Vec2f(localPosX, localPosY));
        localPosX = anc.x;
        localPosY = anc.y;

        anchorX = AnchorMode.create(customizationData.getPropertyString("anchorX", AnchorMode.getTypeName(anchorX, 0)), anchorX);
        anchorY = AnchorMode.create(customizationData.getPropertyString("anchorY", AnchorMode.getTypeName(anchorY, 0)), anchorY);

        String bm = customizationData.getPropertyString("blendMode", AppBlendMode.getTypeName(blendMode));
        blendMode = AppBlendMode.getGlMode(bm);
        
        descText = customizationData.getPropertyString("description", descText);
        
        float oldRot = customizationData.getPropertyFloat("rotation", -999f);
        if (oldRot != -999f) {
            rotMVar = com.aylis.comp.visual.core.Elements.Base.MVariableFloat.Companion.createConstantFloat(oldRot);
            customizationData.jsonObj.remove("rotation");
        }
        rotMVar = customizationData.getPropertyMVariableFloat("rotation", rotMVar);
        
        lockScaleRatio = customizationData.getPropertyBool("lockScaleRatio", lockScaleRatio);
        if (!(this instanceof RootElement) && !(this instanceof PreCompElement) && useAnimatorMeasures) {
            animator.onApplyCustomization(customizationData);
        }
    }

    public void onReadCustomization(Element.CustomizationData outCustomizationData) {
        if (!(this instanceof RootElement)) {
            outCustomizationData.putPropertyBool("visible", visible, "0_general");
        }
        outCustomizationData.putPropertyString("description", descText, "txt", "0_general");

        outCustomizationData.putPropertyVec2f("position", new Vec2f(posX, posY), "f2 -0.5 1.5", "0_general");

        outCustomizationData.putPropertyBool("lockScaleRatio", lockScaleRatio, "0_general");
        outCustomizationData.putPropertyMVariableFloat("scaleX", scaleXMVar, "0_general", 0.0f, 2.0f);
        outCustomizationData.putPropertyMVariableFloat("scaleY", scaleYMVar, "0_general", 0.0f, 2.0f);
        outCustomizationData.putPropertyMVariableFloat("rotation", rotMVar, "0_general", -1.0f, 1.0f);

        outCustomizationData.putPropertyString("blendMode", AppBlendMode.getTypeName(blendMode), AppBlendMode.getSelectorString(), "0_general");

        if (!(this instanceof RootElement) && !(this instanceof PreCompElement) && useAnimatorMeasures) {
            animator.onReadCustomization(outCustomizationData);
        }
    }

    public static class CustomizationData {

        JSONObject jsonObj;
        boolean badFormat = false;

        public static String[] getPropertyTypeParts(String type) {
            String splitChar = type.contains("|") ? "\\|" : " ";
            String[] parts = type.split(splitChar);
            if(parts.length < 1) {
                parts = new String[1];
                parts[0] = type;
            }
            return parts;
        }

        public CustomizationData(JSONObject jsonObj) {
            this.jsonObj = jsonObj;
        }

        public void setCustomizationName(String value) {
            try {
                jsonObj.put("_name", value);
            } catch (JSONException ignored) {
            }
        }

        public String getCustomizationName() {
            try {
                return jsonObj.getString("_name");
            } catch (JSONException e) {
                badFormat = true;
            }
            return "";
        }

        public String getChildTypeValue() {

            try {
                return jsonObj.getString("v");
            } catch (JSONException e) {
                return "";
            }
        }

        public void putChildTypeValue(String typeValue) {

            try {
                jsonObj.put("v", typeValue);
            } catch (JSONException e) {
            }
        }

        public CustomizationData putChild(String name, String childType , String[] validTypes)
        {
            return putChild(name, childType, validTypes, "General");
        }

        public CustomizationData putChild(String name, String childType , String[] validTypes, String groupTag)
        {
            JSONObject jsonObjChild = new JSONObject();
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("_child");
                for(String type : validTypes) {
                    sb.append(" ");
                    sb.append(type);
                }

                jsonObjChild.put("v", childType);
                jsonObjChild.put("t", sb.toString());
                jsonObjChild.put("tag", groupTag);
                jsonObj.put(name, jsonObjChild);
            } catch (JSONException e) {
            }
            return new CustomizationData(jsonObjChild);
        }

        public CustomizationData getChild(String name) {
            try {
                JSONObject jsonObjChild = jsonObj.getJSONObject(name);

                return new CustomizationData(jsonObjChild);
            } catch (JSONException e) {
                return new CustomizationData(new JSONObject());
            }
        }

        public void putPropertyBool(String name, boolean value) {
            String tag = "General";
            try {
                JSONObject existing = jsonObj.optJSONObject(name);
                if (existing != null) {
                    tag = existing.optString("tag", "General");
                }
            } catch (Exception ignored) {}
            putPropertyBool(name, value, tag);
        }

        public void putPropertyBool(String name, boolean value, String groupTag) {
            try {
                JSONObject jsonObjProp = jsonObj.optJSONObject(name);
                if (jsonObjProp == null) jsonObjProp = new JSONObject();
                jsonObjProp.put("v", value ? 1 : 0);
                jsonObjProp.put("t", "b");
                jsonObjProp.put("tag", groupTag);
                jsonObj.put(name, jsonObjProp);
            } catch (JSONException e) {
            }
        }

        public void putPropertyBool(String name, boolean value, String groupTag, String hint) {
            try {
                JSONObject jsonObjProp = jsonObj.optJSONObject(name);
                if (jsonObjProp == null) jsonObjProp = new JSONObject();
                jsonObjProp.put("v", value ? 1 : 0);
                jsonObjProp.put("t", "b");
                jsonObjProp.put("tag", groupTag);
                if (hint != null) {
                    jsonObjProp.put("hint", hint);
                }
                jsonObj.put(name, jsonObjProp);
            } catch (JSONException e) {
            }
        }

        public void putPropertyInt(String name, int value) {
            String type = "i";
            String tag = "General";
            try {
                JSONObject existing = jsonObj.optJSONObject(name);
                if (existing != null) {
                    type = existing.optString("t", "i");
                    tag = existing.optString("tag", "General");
                }
            } catch (Exception ignored) {}
            putPropertyInt(name, value, type, tag);
        }

        public void putPropertyInt(String name, int value, String type) {
            String tag = "General";
            try {
                JSONObject existing = jsonObj.optJSONObject(name);
                if (existing != null) {
                    tag = existing.optString("tag", "General");
                }
            } catch (Exception ignored) {}
            putPropertyInt(name, value, type, tag);
        }

        public void putPropertyInt(String name, int value, String type, String groupTag) {
            putPropertyInt(name, value, type, groupTag, null);
        }

        public void putPropertyInt(String name, int value, String type, String groupTag, String hint) {
            try {
                JSONObject jsonObjProp = jsonObj.optJSONObject(name);
                if (jsonObjProp == null) jsonObjProp = new JSONObject();
                jsonObjProp.put("v", value);
                jsonObjProp.put("t", type);
                jsonObjProp.put("tag", groupTag);
                if (hint != null) {
                    jsonObjProp.put("hint", hint);
                }
                jsonObj.put(name, jsonObjProp);
            } catch (JSONException e) {
            }
        }

        public void putPropertyFloat(String name, float value) {
            String type = "f";
            String tag = "General";
            try {
                JSONObject existing = jsonObj.optJSONObject(name);
                if (existing != null) {
                    type = existing.optString("t", "f");
                    tag = existing.optString("tag", "General");
                }
            } catch (Exception ignored) {}
            putPropertyFloat(name, value, type, tag);
        }

        public void putPropertyFloat(String name, float value, String type) {
            String tag = "General";
            try {
                JSONObject existing = jsonObj.optJSONObject(name);
                if (existing != null) {
                    tag = existing.optString("tag", "General");
                }
            } catch (Exception ignored) {}
            putPropertyFloat(name, value, type, tag);
        }

        public void putPropertyFloat(String name, float value, String type, String groupTag) {
            putPropertyFloat(name, value, type, groupTag, null);
        }

        public void putPropertyAction(String name, int triggerCount, String groupTag) {
            try {
                JSONObject jsonObjProp = jsonObj.optJSONObject(name);
                if (jsonObjProp == null) jsonObjProp = new JSONObject();
                jsonObjProp.put("v", triggerCount);
                jsonObjProp.put("t", "action");
                jsonObjProp.put("tag", groupTag);
                jsonObj.put(name, jsonObjProp);
            } catch (JSONException e) {
            }
        }
        
        public int getPropertyAction(String name, int def) {
            try {
                JSONObject jsonObjProp = jsonObj.optJSONObject(name);
                if (jsonObjProp != null && "action".equals(jsonObjProp.optString("t"))) {
                    return jsonObjProp.getInt("v");
                }
            } catch (Exception e) {}
            return def;
        }

        public void putPropertyMVariableFloat(String name, com.aylis.comp.visual.core.Elements.Base.MVariableFloat mVar, String groupTag, float min, float max) {
            try {
                JSONObject existing = jsonObj.optJSONObject(name);
                if (groupTag == null || groupTag.isEmpty()) {
                    if (existing != null && existing.has("tag")) {
                        groupTag = existing.optString("tag", "");
                    }
                }
                JSONObject jsonObjProp = jsonObj.optJSONObject(name);
                if (jsonObjProp == null) jsonObjProp = new JSONObject();
                jsonObjProp.put("v", mVar.toString());
                jsonObjProp.put("t", "mvarf " + min + " " + max);
                jsonObjProp.put("tag", groupTag);
                jsonObj.put(name, jsonObjProp);
            } catch (JSONException e) {
            }
        }

        public void putPropertyMeasuredVar(String name, com.aylis.comp.visual.core.Elements.Base.MeasuredVar mVar, String groupTag, float min, float max) {
            try {
                JSONObject existing = jsonObj.optJSONObject(name);
                if (groupTag == null || groupTag.isEmpty()) {
                    if (existing != null && existing.has("tag")) {
                        groupTag = existing.optString("tag", "");
                    }
                }
                JSONObject jsonObjProp = jsonObj.optJSONObject(name);
                if (jsonObjProp == null) jsonObjProp = new JSONObject();
                jsonObjProp.put("v", mVar.toString());
                jsonObjProp.put("t", "mvar " + min + " " + max);
                jsonObjProp.put("tag", groupTag);
                jsonObj.put(name, jsonObjProp);
            } catch (JSONException e) {
            }
        }

        public com.aylis.comp.visual.core.Elements.Base.MVariableFloat getPropertyMVariableFloat(String name, com.aylis.comp.visual.core.Elements.Base.MVariableFloat def) {
            try {
                JSONObject jsonObjProp = jsonObj.optJSONObject(name);
                if (jsonObjProp != null && jsonObjProp.has("v")) {
                    return com.aylis.comp.visual.core.Elements.Base.MVariableFloat.Companion.fromString(jsonObjProp.getString("v"), def);
                }
            } catch (Exception e) {}
            return def;
        }

        public com.aylis.comp.visual.core.Elements.Base.MeasuredVar getPropertyMeasuredVar(String name, com.aylis.comp.visual.core.Elements.Base.MeasuredVar def) {
            try {
                JSONObject jsonObjProp = jsonObj.optJSONObject(name);
                if (jsonObjProp != null && jsonObjProp.optString("t").startsWith("mvar ")) {
                    return com.aylis.comp.visual.core.Elements.Base.MeasuredVar.Companion.fromString(jsonObjProp.getString("v"), def);
                }
            } catch (Exception e) {}
            return def;
        }

        public void putPropertyFloat(String name, float value, String type, String groupTag, String hint) {
            try {
                JSONObject jsonObjProp = jsonObj.optJSONObject(name);
                if (jsonObjProp == null) jsonObjProp = new JSONObject();
                jsonObjProp.put("v", value);
                jsonObjProp.put("t", type);
                jsonObjProp.put("tag", groupTag);
                if (hint != null) {
                    jsonObjProp.put("hint", hint);
                }
                jsonObj.put(name, jsonObjProp);
            } catch (JSONException e) {
            }
        }

        public void putPropertyVec2f(String name, Vec2f value) {
            String type = "f2";
            String tag = "General";
            try {
                JSONObject existing = jsonObj.optJSONObject(name);
                if (existing != null) {
                    type = existing.optString("t", "f2");
                    tag = existing.optString("tag", "General");
                }
            } catch (Exception ignored) {}
            putPropertyVec2f(name, value, type, tag);
        }

        public void putPropertyVec2f(String name, Vec2f value, String type) {
            String tag = "General";
            try {
                JSONObject existing = jsonObj.optJSONObject(name);
                if (existing != null) {
                    tag = existing.optString("tag", "General");
                }
            } catch (Exception ignored) {}
            putPropertyVec2f(name, value, type, tag);
        }

        public void putPropertyVec2f(String name, Vec2f value, String type, String groupTag) {
            try {
                JSONObject jsonObjProp = jsonObj.optJSONObject(name);
                if (jsonObjProp == null) jsonObjProp = new JSONObject();
                jsonObjProp.put("v", value.toString());
                jsonObjProp.put("t", type);
                jsonObjProp.put("tag", groupTag);
                jsonObj.put(name, jsonObjProp);
            } catch (JSONException e) {
            }
        }

        public void putPropertyString(String name, String value) {
            String type = "txt";
            String tag = "General";
            try {
                JSONObject existing = jsonObj.optJSONObject(name);
                if (existing != null) {
                    type = existing.optString("t", "txt");
                    tag = existing.optString("tag", "General");
                }
            } catch (Exception ignored) {}
            putPropertyString(name, value, type, tag);
        }

        public void putPropertyString(String name, String value, String type) {
            String tag = "General";
            try {
                JSONObject existing = jsonObj.optJSONObject(name);
                if (existing != null) {
                    tag = existing.optString("tag", "General");
                }
            } catch (Exception ignored) {}
            putPropertyString(name, value, type, tag);
        }

        public void putPropertyString(String name, String value, String type, String groupTag) {
            putPropertyString(name, value, type, groupTag, null);
        }

        public void putPropertyString(String name, String value, String type, String groupTag, String hint) {
            try {
                JSONObject jsonObjProp = jsonObj.optJSONObject(name);
                if (jsonObjProp == null) jsonObjProp = new JSONObject();
                jsonObjProp.put("v", value);
                jsonObjProp.put("t", type);
                jsonObjProp.put("tag", groupTag);
                if (hint != null) {
                    jsonObjProp.put("hint", hint);
                }
                jsonObj.put(name, jsonObjProp);
            } catch (JSONException e) {
            }
        }

        public boolean getPropertyBool(String name, boolean defaultValue) {
            try {
                return jsonObj.getJSONObject(name).getInt("v") != 0;
            } catch (JSONException e) {
                return defaultValue;
            }
        }

        public int getPropertyInt(String name, int defaultValue) {
            try {
                return jsonObj.getJSONObject(name).getInt("v");
            } catch (JSONException e) {
                return defaultValue;
            }
        }

        public float getPropertyFloat(String name, float defaultValue) {
            try {
                return (float)jsonObj.getJSONObject(name).getDouble("v");
            } catch (JSONException e) {
                return defaultValue;
            }
        }

        public String getPropertyString(String name, String defaultValue) {
            try {
                return  jsonObj.getJSONObject(name).getString("v");
            } catch (JSONException e) {
                return defaultValue;
            }
        }

        public Vec2f getPropertyVec2f(String name, Vec2f defaultValue) {
            try {
                return Vec2f.FromString(jsonObj.getJSONObject(name).getString("v"), defaultValue);
            } catch (JSONException e) {
                return defaultValue;
            }
        }

        public String getPropertyType(String name) {

            try {
                return  jsonObj.getJSONObject(name).getString("t");
            } catch (JSONException e) {
                return "";
            }
        }

        public String getPropertyHint(String name) {
            try {
                return jsonObj.getJSONObject(name).optString("hint", "");
            } catch (JSONException e) {
                return "";
            }
        }

        public String getPropertyGroupTag(String name) {
            String rawTag = null;
            try {
                rawTag = jsonObj.getJSONObject(name).optString("tag", null);
            } catch (JSONException e) {

            }
            return PropertySorter.resolveUiTag(name, rawTag);
        }

        public String getRawPropertyGroupTag(String name) {
            try {
                return jsonObj.getJSONObject(name).optString("tag", null);
            } catch (JSONException e) {
                return null;
            }
        }

        public void setPropertyGroupTag(String name, String tag) {
            try {
                JSONObject obj = jsonObj.getJSONObject(name);
                obj.put("tag", tag);
            } catch (JSONException e) {
            }
        }

        public int getPropertyOrderIndex(String name) {
            int rawOrder = -1;
            String rawTag = null;
            try {
                JSONObject propObj = jsonObj.optJSONObject(name);
                if (propObj != null) {
                    rawOrder = propObj.optInt("o", -1);
                    rawTag = propObj.optString("tag", null);
                }
            } catch (Exception e) {

            }

            String actualTag = PropertySorter.resolveUiTag(name, rawTag);
            return PropertySorter.resolveUiOrder(name, actualTag, rawOrder);
        }

        public Iterator<String> GetAllPropertiesSortedByOrder() {
            Iterator<String> itKeys = jsonObj.keys();
            MultiList<String, Integer> multiList = new MultiList<>();
            while (itKeys.hasNext()) {
                String next = itKeys.next();
                multiList.add(new Tuple2<>(next, getPropertyOrderIndex(next)));
            }
            Collections.sort(multiList, new Comparator<Tuple2<String, Integer>>() {
                @Override
                public int compare(Tuple2<String, Integer> t1, Tuple2<String, Integer> t2) {
                    return Integer.compare(t1.obj2, t2.obj2);
                }
            });
            List<String> result = new ArrayList<>();
            for (Tuple2<String, Integer> item : multiList) {
                result.add(item.obj1);
            }
            return result.iterator();
        }

        public Map<String, MultiList<String, Integer>> GetAllPropertiesGroupedSortedByOrder() {
            Iterator<String> itKeys = jsonObj.keys();
            Map<String, MultiList<String, Integer>> map = new TreeMap<>(new Comparator<String>() {
                @Override
                public int compare(String s1, String s2) {
                    return s1.compareToIgnoreCase(s2);
                }
            });
            while (itKeys.hasNext()) {
                String next = itKeys.next();
                String propertyGroupTag = getPropertyGroupTag(next);
                if (propertyGroupTag.length() > 0) {
                    MultiList<String, Integer> multiList = map.get(propertyGroupTag);
                    if (multiList == null) {
                        multiList = new MultiList<>();
                        map.put(propertyGroupTag, multiList);
                    }
                    multiList.add(new Tuple2<>(next, getPropertyOrderIndex(next)));
                }
            }
            for (MultiList<String, Integer> list : map.values()) {
                Collections.sort(list, new Comparator<Tuple2<String, Integer>>() {
                    @Override
                    public int compare(Tuple2<String, Integer> t1, Tuple2<String, Integer> t2) {
                        return Integer.compare(t1.obj2, t2.obj2);
                    }
                });
            }
            return map;
        }

        public Iterator<String> GetAllPropertiesSorted()
        {
            Iterator<String> it = jsonObj.keys();
            List<String> list = new ArrayList<String>();
            while (it.hasNext()) {
                list.add(it.next());
            }

            Collections.sort(list);
            return list.iterator();
        }

        public Iterator<String> GetAllProperties()
        {
            return jsonObj.keys();
        }
    }

    public static class CustomizationList {

        private JSONObject jsonRoot;
        private JSONArray jsonArray;

        public CustomizationList(String serialized) {

            try {
                jsonRoot = new JSONObject(serialized);
                jsonArray = jsonRoot.getJSONArray("list");

            } catch (JSONException e) {
                tlog.w("Failed to create from saved string: "+e.getMessage());
                createEmpty();
            }
        }

        public CustomizationList() {

            createEmpty();
        }

        private void createEmpty()
        {
            try {
                jsonRoot = new JSONObject();
                jsonArray = new JSONArray();
                jsonRoot.put("list", jsonArray);
            } catch (JSONException e) {

                tlog.w(e.getMessage());
                jsonRoot = null;
                jsonArray = null;
            }
        }

        public static CustomizationList deserialize(String serialized) {
            if (serialized == null) return null;

            return new CustomizationList(serialized);

        }

        public String serialize() {
            if(jsonRoot == null) return "";
            return jsonRoot.toString();

        }

        public JSONObject getNewDataJSONObject() {
            JSONObject jsonObj = new JSONObject();
            jsonArray.put(jsonObj);
            return jsonObj;
        }

        public int dataCount() {
            return jsonArray.length();
        }

        public CustomizationData getData(int customizationIndex) {

            if(customizationIndex < 0 || customizationIndex >= jsonArray.length())
                return null;

            try {
                JSONObject obj = jsonArray.getJSONObject(customizationIndex);
                return new CustomizationData(obj);
            }catch (JSONException e) {
                tlog.w(e.getMessage());
                return null;
            }

        }

        public CustomizationList createClone() {
            return new CustomizationList(jsonRoot != null ? jsonRoot.toString() : "");

        }
    }
}

