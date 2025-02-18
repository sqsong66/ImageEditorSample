package com.sqsong.opengllib

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.IntDef
import androidx.core.animation.addListener
import androidx.core.view.children
import com.sqsong.opengllib.filters.BaseImageFilter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@IntDef(
    GestureMode.GESTURE_NONE, GestureMode.GESTURE_DRAG, GestureMode.GESTURE_SCALE
)
@Retention(AnnotationRetention.SOURCE)
annotation class GestureMode {

    companion object {
        const val GESTURE_NONE = 0
        const val GESTURE_DRAG = 1
        const val GESTURE_SCALE = 2
    }

}

data class LayoutInfo(
    var scaleX: Float = 1f,
    var scaleY: Float = 1f,
    var rotation: Float = 0f,
    var translationX: Float = 0f,
    var translationY: Float = 0f
)

open class ScalableGLTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ViewGroup(context, attrs, defStyleAttr) {

    companion object {
        private const val DOUBLE_CLICK_INTERVAL = 220L
    }

    private var minScaleFactor = 0.5f
    private var maxScaleFactor = 10f
    private var enableScalable = true
    private var glTextureView: OpenGLTextureView? = null

    private var lastX = 0f
    private var lastY = 0f

    // 手指按下的位置
    private val touchDownPoint = PointF()

    // 手指按下时的时间
    private var touchDownMillis = 0L

    // 是否是双击事件
    private var isDoubleClick = false

    @GestureMode
    private var gestureMode = GestureMode.GESTURE_NONE

    // 双指时两指之间的距离
    private var fingerDistance = 0f

    // 双指时两指之间的中心点
    private val touchDownFingerCenter = PointF()

    // 双指移动时中心点
    private val moveFingerCenter = PointF()

    // 双指时保存当前 TextureView 的缩放、旋转、平移信息
    private var textureLayoutInfo = LayoutInfo()

    // 临时矩阵
    private val tempMatrix by lazy { Matrix() }

    // 父控件的矩形区域
    private val viewRect = RectF()

    // 临时矩形区域
    private val tempRect = RectF()

    // 手指抬起时动画
    private var valueAnimator: ValueAnimator? = null

    private var isAnimating = false

    private val testPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
    }

    init {
        handleAttributes(context, attrs)
        handleTextureView(context)
    }

    private fun handleAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.ScalableGLTextureView).apply {
            enableScalable = getBoolean(R.styleable.ScalableGLTextureView_sgltv_enableScalable, true)
            minScaleFactor = getFloat(R.styleable.ScalableGLTextureView_sgltv_minScaleFactor, 0.5f)
            maxScaleFactor = getFloat(R.styleable.ScalableGLTextureView_sgltv_maxScaleFactor, 10f)
            recycle()
        }
    }

    private fun handleTextureView(context: Context) {
        glTextureView = OpenGLTextureView(context)
        removeAllViews()
        addView(glTextureView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun setImageBitmap(bitmap: Bitmap) {
        val vRatio = width.toFloat() / height
        val bRatio = bitmap.width.toFloat() / bitmap.height
        val dw: Int
        val dh: Int
        if (vRatio < bRatio) {
            dw = width
            dh = dw * bitmap.height / bitmap.width
        } else {
            dh = height
            dw = dh * bitmap.width / bitmap.height
        }
        glTextureView?.let { view ->
            view.layoutParams.apply {
                width = dw
                height = dh
                view.layoutParams = this
            }
            val cx = width / 2
            val cy = height / 2
            val left = cx - dw / 2
            val top = cy - dh / 2
            val right = cx + dw / 2
            val bottom = cy + dh / 2
            view.scaleX = 1f
            view.scaleY = 1f
            view.translationX = 0f
            view.translationY = 0f
            view.layout(left, top, right, bottom)
            view.setImageBitmap(bitmap)
            viewRect.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        }
    }

    fun setFilter(filter: BaseImageFilter, progress: Float = Float.MIN_VALUE) {
        glTextureView?.setFilter(filter, progress)
    }

    fun setProgress(progress: Float, extraType: Int = 0) {
        glTextureView?.setProgress(progress, extraType)
    }

    fun onPause() {
        glTextureView?.onPause()
    }

    fun getRenderedBitmap(): Bitmap? {
        return glTextureView?.getRenderedBitmap()
    }

    fun onDestroy() {
        glTextureView?.onDestroy()
    }

    fun setGlBackgroundColor(color: Int) {
        glTextureView?.setGlBackgroundColor(color)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        glTextureView?.onDestroy()
        valueAnimator?.cancel()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
        // 测量所有子控件
        children.forEach { child -> measureChild(child, widthMeasureSpec, heightMeasureSpec) }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        children.forEach { child ->
            val left = (width - child.measuredWidth) / 2
            val top = (height - child.measuredHeight) / 2
            child.translationX = 0f
            child.translationY = 0f
            child.layout(left, top, left + child.measuredWidth, top + child.measuredHeight)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewRect.set(0f, 0f, w.toFloat(), h.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawLine(0f, height / 2f, width.toFloat(), height / 2f, testPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!enableScalable) return false
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> onTouchDown(event)

            MotionEvent.ACTION_POINTER_DOWN -> onPointerDown(event)

            MotionEvent.ACTION_MOVE -> onTouchMove(event)

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> onTouchUp(event)
        }
        return true
    }

    private fun onTouchDown(event: MotionEvent) {
        val timeMillis = System.currentTimeMillis()
        if (timeMillis - touchDownMillis < DOUBLE_CLICK_INTERVAL) {
            isDoubleClick = true
        } else {
            touchDownMillis = timeMillis
        }
        touchDownPoint.set(event.x, event.y)
        gestureMode = GestureMode.GESTURE_DRAG
        lastX = event.x
        lastY = event.y
    }

    private fun onPointerDown(event: MotionEvent) {
        if (event.pointerCount != 2) return
        isDoubleClick = false
        gestureMode = GestureMode.GESTURE_SCALE
        fingerDistance = calculateDistance(event)
        calculateCenterPoint(touchDownFingerCenter, event)
        glTextureView?.let { stagingTextureLayoutInfo(it, touchDownFingerCenter.x, touchDownFingerCenter.y) }
    }

    private fun stagingTextureLayoutInfo(view: View, focusX: Float, focusY: Float) {
        val focusPoint = mapCoordinateToLocal(view, focusX, focusY)
        resetViewPivotTo(view, focusPoint[0], focusPoint[1], tempMatrix)
        textureLayoutInfo = LayoutInfo(
            scaleX = view.scaleX,
            scaleY = view.scaleY,
            rotation = view.rotation,
            translationX = view.translationX,
            translationY = view.translationY
        )
    }

    private fun onTouchMove(event: MotionEvent) {
        glTextureView?.let {
            val distance = calculateDistance(touchDownPoint.x, touchDownPoint.y, event.x, event.y)
            if (distance > 6f) {
                processMoveEvent(it, event)
            }
        }
        lastX = event.x
        lastY = event.y
    }

    private fun processMoveEvent(view: OpenGLTextureView, event: MotionEvent) {
        when (gestureMode) {
            GestureMode.GESTURE_DRAG -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                view.translationX += dx
                view.translationY += dy
            }

            GestureMode.GESTURE_SCALE -> {
                scaleTextureView(view, event)
            }
        }
    }

    private fun scaleTextureView(view: OpenGLTextureView, event: MotionEvent) {
        if (event.pointerCount != 2) return
        val newDistance = calculateDistance(event)
        calculateCenterPoint(moveFingerCenter, event)
        if (newDistance > 10f) {
            val scale = newDistance / fingerDistance
            val tx = moveFingerCenter.x - touchDownFingerCenter.x
            val ty = moveFingerCenter.y - touchDownFingerCenter.y
            onTextureViewTransition(view, scale, tx, ty)
        }
    }

    private fun onTextureViewTransition(view: View, scale: Float, tx: Float, ty: Float) {
        view.scaleX = min(textureLayoutInfo.scaleX * scale, maxScaleFactor)
        view.scaleY = min(textureLayoutInfo.scaleY * scale, maxScaleFactor)
        val dx = tx + textureLayoutInfo.translationX - view.translationX
        val dy = ty + textureLayoutInfo.translationY - view.translationY
        view.translationX += dx
        view.translationY += dy
    }

    private fun onTouchUp(event: MotionEvent) {
        Log.w("songmao", "onTouchUp: $isDoubleClick")
        glTextureView?.let { view ->
            if (isDoubleClick) {
                val transformedRect = getTransformedRect(view)
                animateScaleTextureView(view, event, transformedRect)
            } else {
                resetViewPivotTo(view, view.width / 2f, view.height / 2f, tempMatrix)
                val transformedRect = getTransformedRect(view)
                animateTextureViewToFitParent(view, transformedRect)
            }
        }
        gestureMode = GestureMode.GESTURE_NONE
    }

    private fun animateScaleTextureView(view: OpenGLTextureView, event: MotionEvent, rect: RectF) {
        val sx = view.scaleX
        val sy = view.scaleY
        val halfMaxScale = maxScaleFactor / 2f
        when {
            sx < 1f || sy < 1f -> {
                scaleTranslateAnimate(view, 1f, rect)
            }

            sx < halfMaxScale || sy < halfMaxScale -> {
                stagingTextureLayoutInfo(view, event.x, event.y)
                scaleTranslateAnimate(view, halfMaxScale, rect, needTranslate = false)
            }

            else -> {
                scaleTranslateAnimate(view, 1f, rect)
            }
        }
    }

    private fun animateTextureViewToFitParent(view: View, rect: RectF) {
        if (rect.contains(0f, 0f, width.toFloat(), height.toFloat())) return
        val sx = view.scaleX
        val sy = view.scaleY
        when {
            sx < 1f || sy < 1f -> {
                val scaleFactor = max(minScaleFactor, sx)
                scaleTranslateAnimate(view, scaleFactor, rect, animateDuration = 80)
            }

            else -> {
                translateAnimate(view, rect)
            }
        }
    }

    private fun translateAnimate(view: View, rect: RectF) {
        if (isAnimating) return
        valueAnimator?.cancel()
        val startTx = view.translationX
        val startTy = view.translationY
        if (rect.width() <= width && rect.height() <= height) {
            tempRect.set(viewRect)
        } else {
            tempRect.set(0f, 0f, width.toFloat(), height.toFloat())
        }

        // 计算水平偏移量
        val dx = if (rect.width() <= width) {
            // 小于父控件，始终居中对齐
            tempRect.centerX() - rect.centerX()
        } else {
            when {
                rect.left > tempRect.left -> tempRect.left - rect.left
                rect.right < tempRect.right -> tempRect.right - rect.right
                else -> 0f
            }
        }

        // 计算垂直偏移量
        val dy = if (rect.height() <= height) {
            tempRect.centerY() - rect.centerY()
        } else {
            when {
                rect.top > tempRect.top -> tempRect.top - rect.top
                rect.bottom < tempRect.bottom -> tempRect.bottom - rect.bottom
                else -> 0f
            }
        }
        valueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 80
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val factor = it.animatedValue as Float
                view.translationX = startTx + dx * factor
                view.translationY = startTy + dy * factor
            }
            addListener(onStart = {
                isAnimating = true
            }, onEnd = {
                isAnimating = false
            })
            start()
        }
    }

    private fun scaleTranslateAnimate(
        view: View, minScaleFactor: Float, rect: RectF,
        needTranslate: Boolean = true, animateDuration: Long = 200
    ) {
        if (isAnimating) return
        valueAnimator?.cancel()
        val viewScale = view.scaleX
        val diffScale = minScaleFactor - viewScale
        val tx = view.translationX
        val ty = view.translationY
        val dx = if (needTranslate) width / 2 - rect.centerX() else 0f
        val dy = if (needTranslate) height / 2 - rect.centerY() else 0f
        valueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = animateDuration
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val factor = it.animatedValue as Float
                val scale = viewScale + diffScale * factor
                view.scaleX = scale
                view.scaleY = scale
                view.translationX = tx + dx * factor
                view.translationY = ty + dy * factor
            }
            addListener(onStart = {
                isAnimating = true
            }, onEnd = {
                isAnimating = false
                resetViewPivotTo(view, view.width / 2f, view.height / 2f, tempMatrix)
                if (isDoubleClick) {
                    isDoubleClick = false
                }
            })
            start()
        }
    }

    // 获取变换后的矩形区域
    private fun getTransformedRect(view: View): RectF {
        // 构造一个在父布局坐标系下的初始矩形
        val rect = RectF(view.left.toFloat(), view.top.toFloat(), view.right.toFloat(), view.bottom.toFloat())
        tempMatrix.reset()
        // 注意：缩放和旋转的 pivot 也需要加上 view.left 和 view.top
        tempMatrix.postScale(view.scaleX, view.scaleY, view.pivotX + view.left, view.pivotY + view.top)
        tempMatrix.postRotate(view.rotation, view.pivotX + view.left, view.pivotY + view.top)
        // 平移这里只考虑 view.translationX/Y，因为布局位置已经体现在了 rect 中
        tempMatrix.postTranslate(view.translationX, view.translationY)
        tempMatrix.mapRect(rect)
        return rect
    }

    private fun calculateDistance(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt(x * x + y * y)
    }

    /**
     * 计算两点之间的中心点
     * @param point 中心点
     * @param event MotionEvent
     */
    private fun calculateCenterPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point[x / 2] = y / 2
    }

    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val x = x2 - x1
        val y = y2 - y1
        return sqrt(x * x + y * y)
    }

    private fun mapCoordinateToLocal(view: View, x: Float, y: Float): FloatArray {
        val localPoint = floatArrayOf(x - view.left, y - view.top)
        if (view.matrix.isIdentity) return localPoint
        val invertMatrix = Matrix()
        view.matrix.invert(invertMatrix)
        invertMatrix.mapPoints(localPoint)
        return localPoint
    }

    private fun resetViewPivotTo(view: View, newPivotX: Float, newPivotY: Float, matrix: Matrix) {
        val prePivotX = view.pivotX
        val prePivotY = view.pivotY

        matrix.reset()
        matrix.postTranslate(view.translationX, view.translationY)
        matrix.postScale(view.scaleX, view.scaleY, prePivotX, prePivotY)
        matrix.postRotate(view.rotation, prePivotX, prePivotY)
        val preOrigin = floatArrayOf(0f, 0f)
        matrix.mapPoints(preOrigin)

        view.pivotX = newPivotX
        view.pivotY = newPivotY
        matrix.reset()
        matrix.postTranslate(view.translationX, view.translationY)
        matrix.postScale(view.scaleX, view.scaleY, newPivotX, newPivotY)
        matrix.postRotate(view.rotation, newPivotX, newPivotY)
        val newOrigin = floatArrayOf(0f, 0f)
        matrix.mapPoints(newOrigin)

        val deltaX = preOrigin[0] - newOrigin[0]
        val deltaY = preOrigin[1] - newOrigin[1]
        view.translationX += deltaX
        view.translationY += deltaY
    }
}