package com.sqsong.opengllib.common

import android.opengl.GLES30

class SimpleTexture(
    textureWidth: Int,
    textureHeight: Int
) : Texture(textureWidth, textureHeight) {

    init {
        create()
    }

    override fun onTextureCreated() {
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, textureWidth, textureHeight, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null)
    }
}