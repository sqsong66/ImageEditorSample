package com.sqsong.opengllib

import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import com.sqsong.opengllib.common.BitmapTexture
import com.sqsong.opengllib.common.Texture
import com.sqsong.opengllib.filters.BaseImageFilter
import java.util.LinkedList
import java.util.Queue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class OpenGLRender(
    private var imageFilter: BaseImageFilter,
) : GLSurfaceView.Renderer, GLTextureView.Renderer {

    private var bgRed = 0f
    private var bgGreen = 0f
    private var bgBlue = 0f
    private var bgAlpha = 1f
    private var viewWidth = 0
    private var viewHeight = 0
    private var imageBitmap: Bitmap? = null
    private var inputTexture: Texture? = null
    private val runOnDraw = LinkedList<Runnable>()

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig?) {
        GLES30.glClearColor(0f, 0f, 0f, 0f)
        /*GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)*/
        GLES30.glDisable(GLES30.GL_BLEND)

        imageFilter.ifNeedInit()
        imageBitmap?.let {
            if (inputTexture == null) {
                inputTexture = BitmapTexture(it).also { inputTexture ->
                    imageFilter.onInputTextureLoaded(inputTexture.textureWidth, inputTexture.textureHeight)
                }
            }
        }
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        GLES30.glViewport(0, 0, width, height)
        imageFilter.onViewSizeChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        runAll(runOnDraw)
        inputTexture?.let { texture ->
            imageFilter.onDrawFrame(texture)
        }
    }

    fun setImageBitmap(bitmap: Bitmap) {
        if (this.imageBitmap == bitmap) return
        this.imageBitmap = bitmap
        runOnDraw {
            inputTexture?.delete()
            inputTexture = null
            inputTexture = BitmapTexture(bitmap).also {
                imageFilter.onInputTextureLoaded(it.textureWidth, it.textureHeight)
            }
        }
    }

    fun setFilter(filter: BaseImageFilter, progress: Float) {
        runOnDraw {
            val oldFilter = imageFilter
            this.imageFilter = filter
            oldFilter.onDestroy()
            imageFilter.ifNeedInit()
            imageFilter.onViewSizeChanged(viewWidth, viewHeight)
            inputTexture?.let {
                imageFilter.onInputTextureLoaded(it.textureWidth, it.textureHeight)
            }
            if (progress != Float.MIN_VALUE) {
                this.imageFilter.setProgress(progress, 0)
            }
        }
    }

    fun onDestroy() {
        inputTexture?.delete()
        inputTexture = null
        imageFilter.onDestroy()
    }

    private fun runOnDraw(runnable: Runnable) {
        synchronized(runOnDraw) {
            runOnDraw.add(runnable)
        }
    }

    private fun runAll(queue: Queue<Runnable>) {
        synchronized(queue) {
            while (queue.isNotEmpty()) {
                queue.poll()?.run()
            }
        }
    }

    fun setProgress(progress: Float, extraType: Int = 0) {
        runOnDraw {
            imageFilter.setProgress(progress, extraType)
        }
    }

    fun getRenderedBitmap(): Bitmap? {
        return imageFilter.getRenderedBitmap()
    }

    fun setGlBackgroundColor(color: Int) {
        bgRed = ((color shr 16) and 0xff) / 255f
        bgGreen = ((color shr 8) and 0xff) / 255f
        bgBlue = (color and 0xff) / 255f
        bgAlpha = ((color shr 24) and 0xff) / 255f
    }

    fun setUserScale(scaleFactor: Float, focusX: Float, focusY: Float, tx: Float, ty: Float) {
        runOnDraw {
            imageFilter.setUserScale(scaleFactor, focusX, focusY, tx, ty)
        }
    }

}