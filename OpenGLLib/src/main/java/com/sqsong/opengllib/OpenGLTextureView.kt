package com.sqsong.opengllib

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import com.sqsong.opengllib.filters.BaseImageFilter
import com.sqsong.opengllib.utils.isOpenGL30Supported

class OpenGLTextureView(
    context: Context,
    attrs: AttributeSet? = null
) : GLTextureView(context, attrs) {

    private val render by lazy {
        OpenGLRender(BaseImageFilter(context))
    }

    init {
        val glVersion = if (isOpenGL30Supported(context)) 3 else 2
        setEGLContextClientVersion(glVersion)
        isOpaque = false
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setRenderer(render)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun setImageBitmap(bitmap: Bitmap) {
        render.setImageBitmap(bitmap)
        requestRender()
    }

    fun setFilter(filter: BaseImageFilter, progress: Float = Float.MIN_VALUE) {
        render.setFilter(filter, progress)
        requestRender()
    }

    fun setProgress(progress: Float, extraType: Int = 0) {
        render.setProgress(progress, extraType)
        requestRender()
    }

    override fun onPause() {
        super.onPause()
        render.onDestroy()
    }

    fun getRenderedBitmap(): Bitmap? {
        return render.getRenderedBitmap()
    }

    fun onDestroy() {
        render.onDestroy()
    }

    fun setGlBackgroundColor(color: Int) {
        render.setGlBackgroundColor(color)
        requestRender()
    }

}