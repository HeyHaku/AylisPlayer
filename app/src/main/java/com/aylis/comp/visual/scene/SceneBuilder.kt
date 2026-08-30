package com.aylis.comp.visual.scene

import com.aylis.comp.visual.core.Elements.Element
import com.aylis.comp.visual.core.Elements.RootElement
import com.aylis.comp.visual.core.Elements.ElementGroup
import com.aylis.comp.visual.core.Elements.ElementsFactory
import org.json.JSONObject

object SceneBuilder {

    fun buildFromScene(themeId: Int, scene: VisualizerScene): RootElement {
        val root = RootElement(themeId)

        for (sceneElement in scene.elements) {
            val element = createElement(sceneElement)
            if (element != null) {
                root.addChildAtEnd(element)
            }
        }

        return root
    }

    private fun createElement(sceneElement: SceneElement): Element? {
        try {
            val element = ElementsFactory.create(sceneElement.type) ?: return null

            val jsonObj = reconstructProperties(sceneElement.properties)

            val customizationData = Element.CustomizationData(jsonObj)

            val jsonRoot = JSONObject()
            val jsonArray = org.json.JSONArray()
            jsonArray.put(jsonObj)
            jsonRoot.put("list", jsonArray)

            val list = Element.CustomizationList(jsonRoot.toString())
            val counter = arrayOf(0)
            element.setCustomization(list, counter)

            if (element is ElementGroup && sceneElement.children != null) {
                for (childScene in sceneElement.children) {
                    val childEl = createElement(childScene)
                    if (childEl != null) {
                        element.addChildAtEnd(childEl)
                    }
                }
            }

            return element
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun reconstructProperties(properties: Map<String, SceneProperty>): JSONObject {
        val jsonObj = JSONObject()
        for ((key, prop) in properties) {
            val propObj = JSONObject()
            val typeParts = prop.type.split(" ")
            val mainType = if (typeParts.isNotEmpty()) typeParts[0] else ""

            when (mainType) {
                "i" -> {
                    val doubleVal = prop.value.toDoubleOrNull() ?: 0.0
                    propObj.put("v", doubleVal.toInt())
                }
                "crgb", "crgba" -> {
                    val longVal = prop.value.toLongOrNull() ?: 0L
                    propObj.put("v", longVal.toInt())
                }
                "f" -> {
                    propObj.put("v", prop.value.toDoubleOrNull() ?: 0.0)
                }
                "b" -> {
                    propObj.put("v", if (prop.value == "true" || prop.value == "1") 1 else 0)
                }
                else -> {
                    propObj.put("v", prop.value)
                }
            }
            propObj.put("t", prop.type)
            propObj.put("tag", prop.group)

            if (prop.properties != null && prop.properties.isNotEmpty()) {
                val nestedJson = reconstructProperties(prop.properties)
                val keys = nestedJson.keys()
                while (keys.hasNext()) {
                    val k = keys.next() as String
                    propObj.put(k, nestedJson.get(k))
                }
            }
            jsonObj.put(key, propObj)
        }
        return jsonObj
    }

    fun collectLocalImages(scene: VisualizerScene): List<String> {
        val list = mutableListOf<String>()
        scene.elements.forEach { collectElementLocalImages(it, list) }
        return list
    }

    private fun collectElementLocalImages(element: SceneElement, list: MutableList<String>) {
        element.properties.values.forEach { collectPropertyLocalImages(it, list) }
        element.children?.forEach { collectElementLocalImages(it, list) }
    }

    private fun collectPropertyLocalImages(prop: SceneProperty, list: MutableList<String>) {
        val typeParts = prop.type.split(" ")
        val mainType = if (typeParts.isNotEmpty()) typeParts[0] else ""
        if (mainType == "img" && prop.value.isNotEmpty()) {
            if (!prop.value.startsWith("http://") && !prop.value.startsWith("https://") && !prop.value.startsWith("zip://")) {
                list.add(prop.value)
            }
        }
        if (mainType == "font" && prop.value.isNotEmpty()) {
            val fontPath = com.aylis.comp.visual.core.CustomFontManager.getFontFilePath(prop.value)
            if (fontPath != null) {
                list.add(android.net.Uri.fromFile(java.io.File(fontPath)).toString())
            }
        }
        prop.properties?.values?.forEach { collectPropertyLocalImages(it, list) }
    }

    fun updateImagePaths(scene: VisualizerScene, pathMap: Map<String, String>): VisualizerScene {
        return scene.copy(elements = scene.elements.map { updateElementImagePaths(it, pathMap) })
    }

    private fun updateElementImagePaths(element: SceneElement, pathMap: Map<String, String>): SceneElement {
        val newProperties = element.properties.mapValues { (_, prop) ->
            val typeParts = prop.type.split(" ")
            val mainType = if (typeParts.isNotEmpty()) typeParts[0] else ""
            val newValue = if (mainType == "img") pathMap[prop.value] ?: prop.value else prop.value
            val newNested = prop.properties?.let { updatePropertiesImagePaths(it, pathMap) }
            prop.copy(value = newValue, properties = newNested)
        }
        val newChildren = element.children?.map { updateElementImagePaths(it, pathMap) }
        return element.copy(properties = newProperties, children = newChildren)
    }

    private fun updatePropertiesImagePaths(properties: Map<String, SceneProperty>, pathMap: Map<String, String>): Map<String, SceneProperty> {
        return properties.mapValues { (_, prop) ->
            val typeParts = prop.type.split(" ")
            val mainType = if (typeParts.isNotEmpty()) typeParts[0] else ""
            val newValue = if (mainType == "img") pathMap[prop.value] ?: prop.value else prop.value
            val newNested = prop.properties?.let { updatePropertiesImagePaths(it, pathMap) }
            prop.copy(value = newValue, properties = newNested)
        }
    }

    fun exportToScene(root: RootElement): VisualizerScene {
        val elements = mutableListOf<SceneElement>()
        for (child in root.childList) {
            elements.add(exportElement(child))
        }
        return VisualizerScene(1, elements)
    }

    fun exportElement(element: Element): SceneElement {
        val jsonObj = JSONObject()
        val data = Element.CustomizationData(jsonObj)
        element.onReadCustomization(data)

        val properties = exportProperties(jsonObj)

        var children: List<SceneElement>? = null
        if (element is ElementGroup) {
            val childList = mutableListOf<SceneElement>()
            for (child in element.childList) {
                childList.add(exportElement(child))
            }
            if (childList.isNotEmpty()) {
                children = childList
            }
        }

        return SceneElement(
            id = element.hashCode().toString(),
            type = ElementsFactory.getTypeName(element),
            properties = properties,
            children = children
        )
    }

    private fun exportProperties(jsonObj: JSONObject): Map<String, SceneProperty> {
        val properties = mutableMapOf<String, SceneProperty>()
        val it = jsonObj.keys()
        while (it.hasNext()) {
            val key = it.next() as String
            if (key == "_name") continue
            val propObj = jsonObj.optJSONObject(key) ?: continue
            val value = propObj.optString("v", "")
            val type = propObj.optString("t", "")
            val tag = propObj.optString("tag", "General")

            var nestedMap: Map<String, SceneProperty>? = null
            val typeParts = Element.CustomizationData.getPropertyTypeParts(type)
            if (typeParts.isNotEmpty() && typeParts[0] == "_child") {
                val nestedJson = JSONObject()
                val keys = propObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next() as String
                    if (k == "v" || k == "t" || k == "tag") continue
                    val subObj = propObj.optJSONObject(k)
                    if (subObj != null) {
                        nestedJson.put(k, subObj)
                    }
                }
                if (nestedJson.length() > 0) {
                    nestedMap = exportProperties(nestedJson)
                }
            }

            properties[key] = SceneProperty(value, type, tag, nestedMap)
        }
        return properties
    }
}
