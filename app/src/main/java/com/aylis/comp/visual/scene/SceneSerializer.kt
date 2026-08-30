package com.aylis.comp.visual.scene

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object SceneSerializer {
    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(VisualizerScene::class.java).indent("  ")

    fun toJson(scene: VisualizerScene): String {
        return adapter.toJson(scene)
    }

    fun fromJson(json: String): VisualizerScene? {
        return try {
            adapter.fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
