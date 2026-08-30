package com.aylis.comp.visual.core.gl.mdesl.graphics

import android.opengl.GLES11Ext
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder

class OESTexture : Texture {

    constructor(width: Int, height: Int) : super() {
        GLES20.glEnable(target)
        val idContainer = IntArray(1)
        GLES20.glGenTextures(1, idContainer, 0)
        id = idContainer[0]
        this.width = width
        this.height = height
        bind()

        setFilter(Texture.DEFAULT_FILTER)
        setWrap(Texture.DEFAULT_WRAP)
    }

    override fun getTarget(): Int {
        return GLES11Ext.GL_TEXTURE_EXTERNAL_OES
    }

    override fun upload(dataFormat: Int, data: ByteBuffer?) {
        // Not used for OES Texture
    }

    override fun upload(x: Int, y: Int, width: Int, height: Int, dataFormat: Int, data: ByteBuffer?) {
        // Not used for OES Texture
    }
}
