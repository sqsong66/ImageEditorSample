package com.sqsong.opengllib.common

import android.graphics.Bitmap
import android.opengl.GLES30
import android.util.Log
import java.nio.ByteBuffer

class FrameBuffer(val width: Int, val height: Int) {

    // 离屏渲染的纹理
    var texture: Texture? = null
        private set
    private var frameBufferId: Int = GLES30.GL_NONE

    init {
        createFrameBuffer()
    }

    private fun createFrameBuffer() {
        // 创建FBO
        val frameBuffer = IntArray(1)
        GLES30.glGenFramebuffers(1, frameBuffer, 0)
        frameBufferId = frameBuffer[0]
        // Log.i("BaseImageFilter", "createFrameBuffer: $frameBufferId")

        // 创建纹理
        texture = SimpleTexture(width, height)
        // 绑定纹理到FBO
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBufferId)
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, texture!!.textureId, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
    }

    fun bindFrameBuffer() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBufferId)
    }

    fun unbindFrameBuffer() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    fun getRenderedBitmap(): Bitmap {
        // 绑定离谱渲染缓冲区读取渲染结果
        bindFrameBuffer()
        val buffer = ByteBuffer.allocateDirect(width * height * 4)
        GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        return bitmap
    }

    fun delete() {
        if (frameBufferId != GLES30.GL_NONE) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(frameBufferId), 0)
            frameBufferId = GLES30.GL_NONE
        }
        texture?.delete()
        texture = null
    }
}