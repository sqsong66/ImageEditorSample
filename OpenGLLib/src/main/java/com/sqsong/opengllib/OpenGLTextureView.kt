package com.sqsong.opengllib

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.sqsong.opengllib.filters.BaseImageFilter
import com.sqsong.opengllib.utils.isOpenGL30Supported
import kotlin.math.max
import kotlin.math.min

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


    /****************** onTouch事件 *********************/
    private var lastFocusX = 0f
    private var lastFocusY = 0f
    private var lastSpan = 1f

    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            lastFocusX = detector.focusX
            lastFocusY = detector.focusY
            lastSpan = detector.currentSpan
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // (1) 获取缩放因子(相对于上一次)
            val factor = detector.currentSpan / lastSpan

            // (2) pivot: 在屏幕坐标下 => 转到OpenGL坐标
            val pivotX = detector.focusX // screenXToGL(detector.focusX, width)
            val pivotY = detector.focusY // screenYToGL(detector.focusY, height)

            // (3) 如果还有平移(双指滑动中心点变化)
            val dxScreen = detector.focusX - lastFocusX
            val dyScreen = detector.focusY - lastFocusY
//            val dxGL = screenDeltaToGL(dxScreen, width)  // 自行定义
//            val dyGL = screenDeltaToGL(dyScreen, height) // 自行定义

            // (4) 通过 filter.updateUserMatrix() 累加
            render.setUserScale(factor, pivotX, pivotY, dxScreen, dyScreen)

            // (5) 更新 "last" 记录
            lastFocusX = detector.focusX
            lastFocusY = detector.focusY
            lastSpan = detector.currentSpan

            // 触发重绘
            requestRender()
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {}
    })

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        return true
    }
}