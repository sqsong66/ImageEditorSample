package com.sqsong.opengllib

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.sqsong.opengllib.filters.BaseImageFilter

class OpenGLSurfaceView(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private var imageFilter: BaseImageFilter = BaseImageFilter(context)

    private val render by lazy {
        OpenGLRender(imageFilter)
    }

    init {
        setEGLContextClientVersion(3)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(true)
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
        render.onDestroy()
        super.onPause()
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