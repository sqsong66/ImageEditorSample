package com.sqsong.opengllib.common

import android.graphics.Bitmap
import com.sqsong.opengllib.filters.BaseImageFilter
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface

class EglBuffer {

    private fun createDisplay(eglConfigs: Array<EGLConfig?>): EGLDisplay {
        val eglDisplay: EGLDisplay = EGL.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        EGL.eglInitialize(eglDisplay, IntArray(2))
        EGL.eglChooseConfig(eglDisplay, CONFIG_ATTRIB_LIST, eglConfigs, 1, IntArray(1))
        return eglDisplay
    }

    private fun createSurface(width: Int, height: Int, eglDisplay: EGLDisplay, eglConfigs: Array<EGLConfig?>): EGLSurface? {
        val surfaceAttrs = intArrayOf(
            EGL10.EGL_WIDTH, width,
            EGL10.EGL_HEIGHT, height,
            EGL10.EGL_NONE
        )
        return EGL.eglCreatePbufferSurface(eglDisplay, eglConfigs[0], surfaceAttrs)
    }

    private fun destroyEglSurface(eglDisplay: EGLDisplay, eglSurface: EGLSurface?) {
        EGL.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
        if (eglSurface != null) {
            EGL.eglDestroySurface(eglDisplay, eglSurface)
        }
    }

    private fun createEGLContext(eglDisplay: EGLDisplay, eglConfigs: Array<EGLConfig?>): EGLContext? {
        return EGL.eglCreateContext(eglDisplay, eglConfigs[0], EGL10.EGL_NO_CONTEXT, CONTEXT_ATTRIB_LIST)
    }

    private fun destroyEglContext(eglDisplay: EGLDisplay, eglContext: EGLContext?) {
        EGL.eglDestroyContext(eglDisplay, eglContext)
    }

    fun getRenderedBitmap(bitmap: Bitmap, imageFilter: BaseImageFilter): Bitmap? {
        val w = bitmap.width
        val h = bitmap.height
        var eglDisplay = EGL10.EGL_NO_DISPLAY
        var eglSurface: EGLSurface? = null
        var eglContext: EGLContext? = null
        val eglConfigs = arrayOfNulls<EGLConfig>(1)
        try {
            eglDisplay = createDisplay(eglConfigs)
            eglSurface = createSurface(w, h, eglDisplay, eglConfigs)
            eglContext = createEGLContext(eglDisplay, eglConfigs)
            EGL.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
            imageFilter.ifNeedInit()
            val inputTexture = BitmapTexture(bitmap)
            imageFilter.onInputTextureLoaded(inputTexture.textureWidth, inputTexture.textureHeight)
            imageFilter.onViewSizeChanged(w, h)
            imageFilter.onDrawFrame(inputTexture)
            EGL.eglSwapBuffers(eglDisplay, eglSurface)
            return imageFilter.getRenderedBitmap()
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            imageFilter.onDestroy()
            destroyEglSurface(eglDisplay, eglSurface)
            destroyEglContext(eglDisplay, eglContext)
        }
        return null
    }

    companion object {
        private val EGL = EGLContext.getEGL() as EGL10
        private const val EGL_CONTEXT_CLIENT_VERSION = 0x3098
        private const val EGL_OPENGL_ES2_BIT = 4
        private val CONFIG_ATTRIB_LIST = intArrayOf(
            EGL10.EGL_BUFFER_SIZE, 32,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
            EGL10.EGL_NONE
        )
        private val CONTEXT_ATTRIB_LIST = intArrayOf(
            EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE
        )
    }
}