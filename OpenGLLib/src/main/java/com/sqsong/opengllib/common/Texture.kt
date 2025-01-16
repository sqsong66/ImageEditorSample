package com.sqsong.opengllib.common

import android.opengl.GLES30
import com.sqsong.opengllib.utils.checkGLError

abstract class Texture(
    var textureWidth: Int = 0,
    var textureHeight: Int = 0
) {

    var textureId: Int = NO_TEXTURE
        private set

    protected fun create() {
        val textureIds = IntArray(1)
        GLES30.glGenTextures(1, textureIds, 0)
        textureId = textureIds[0]
        checkGLError("glGenTextures")
        if (textureId != NO_TEXTURE) {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
            onTextureCreated()
        }
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
    }

    abstract fun onTextureCreated()

    fun bindTexture(location: Int, slot: Int = 0) {
        if (textureId == NO_TEXTURE) create()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + slot)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glUniform1i(location, slot)
    }

    fun unbindTexture() {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
    }

    fun delete() {
        if (textureId != NO_TEXTURE) {
            GLES30.glDeleteTextures(1, intArrayOf(textureId), 0)
            textureId = NO_TEXTURE
        }
    }

    companion object {
        const val NO_TEXTURE = -1
    }
}