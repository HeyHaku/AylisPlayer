package com.aylis.comp.visual.scene

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VisualizerScene(
    val version: Int = 1,
    val elements: List<SceneElement> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SceneElement(
    val id: String = "",
    val type: String = "",
    val properties: Map<String, SceneProperty> = emptyMap(),

    val children: List<SceneElement>? = null
)

@JsonClass(generateAdapter = true)
data class SceneProperty @JvmOverloads constructor(
    val value: String = "",
    val type: String = "",
    val group: String = "General",
    val properties: Map<String, SceneProperty>? = null
)
