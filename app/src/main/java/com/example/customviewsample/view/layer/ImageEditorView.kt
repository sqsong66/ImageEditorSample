package com.example.customviewsample.view.layer

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.example.customviewsample.R
import com.example.customviewsample.common.ext.isSameRect
import com.example.customviewsample.common.helper.VibratorHelper
import com.example.customviewsample.data.CanvasSize
import com.example.customviewsample.utils.dp2Px
import com.example.customviewsample.view.AlphaGridDrawHelper
import kotlin.math.sqrt

private const val MIN_MOVE_DISTANCE = 10f
private const val MAX_CLICK_DURATION = 400L

class ImageEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private var lastX = 0f
    private var lastY = 0f
    private var cornerRadius = 0f
    private val clipPath = Path()
    private val clipRect = RectF()
    private val preClipRect = RectF()
    private var touchDownMillis = 0L
    private val touchDownPoint = PointF()

    // 尺寸变换时临时存储矩形框
    private val resizeRect = RectF()
    private var currentLayerView: AbsLayer? = null
    private var resizeAnimator: ValueAnimator? = null
    private val vibratorHelper by lazy { VibratorHelper(context) }
    private var canvasSize: CanvasSize = CanvasSize(width = 2016, height = 1512, iconRes = R.drawable.ic_picture_landscape, title = "Landscape")

    private val testPaint by lazy {
        Paint().apply {
            color = Color.GREEN
            style = Paint.Style.FILL
        }
    }

    private val alphaGridDrawHelper by lazy {
        AlphaGridDrawHelper(
            gridSize = dp2Px(20),
            lightColor = ContextCompat.getColor(context, R.color.grid_light),
            darkColor = ContextCompat.getColor(context, R.color.grid_dark)
        )
    }

    private val scaleGestureListener = ScaleGestureDetector(context, object : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            currentLayerView?.onScale(detector.scaleFactor, detector.focusX, detector.focusY)
            return true
        }
    })


    init {
        handleAttributes(context, attrs)
        clipChildren = false
        clipToPadding = false
    }

    private fun handleAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.ImageEditorView).apply {
            cornerRadius = getDimension(R.styleable.ImageEditorView_iev_cornerRadius, dp2Px(8))
            recycle()
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.save()
        clipPath.reset()
        clipPath.addRoundRect(clipRect, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.clipPath(clipPath)
        alphaGridDrawHelper.drawAlphaGrid(canvas, clipRect)
        super.dispatchDraw(canvas)
        canvas.restore()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        clipRect.set(calculateClipRect(w, h))
        children.forEach { child ->
            (child as AbsLayer).stagingResizeInfo(clipRect, updateLayoutInfo = false)
        }
        if (resizeRect.isEmpty) {
            resizeRect.set(clipRect)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
        // 测量所有子控件
        children.forEach { child -> measureChild(child, widthMeasureSpec, heightMeasureSpec) }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val sizeChanged = clipRect.isSameRect(preClipRect)
        children.forEach { child ->
            val layer = child as AbsLayer
            if (sizeChanged) { // 画布区域如果发生变化则需要根据布局信息重新计算子View的位置
                layer.onUpdateLayout(clipRect)
            } else { // 画布区域未发生变化则根据控件的布局更新布局信息
                layer.updateLayoutInfo(clipRect)
            }
        }
        preClipRect.set(clipRect)
    }

    fun addImageLayer(bitmap: Bitmap) {
        ImageLayerView(context, cornerRadius = cornerRadius / 2f, isSelectedLayer = true).apply {
            onInitialLayout(this@ImageEditorView, bitmap, clipRect)
            currentLayerView?.isSelectedLayer = false
            currentLayerView?.invalidateView()
            currentLayerView = this
        }
    }

    private fun calculateClipRect(width: Int, height: Int): RectF {
        val canvasRatio = canvasSize.width.toFloat() / canvasSize.height
        val viewRatio = width.toFloat() / height
        val rectF = if (canvasRatio > viewRatio) {
            val clipWidth = width.toFloat()
            val clipHeight = clipWidth / canvasRatio
            RectF(0f, (height - clipHeight) / 2, clipWidth, (height + clipHeight) / 2)
        } else {
            val clipHeight = height.toFloat()
            val clipWidth = clipHeight * canvasRatio
            RectF((width - clipWidth) / 2, 0f, (width + clipWidth) / 2, clipHeight)
        }
        return rectF
    }


    fun updateCanvasSize(canvasSize: CanvasSize) {
        this.canvasSize = canvasSize
        resizeAnimator?.cancel()
        val currentRect = RectF(clipRect)
        val newRect = calculateClipRect(width, height)
        val diffLeft = newRect.left - currentRect.left
        val diffTop = newRect.top - currentRect.top
        val diffRight = newRect.right - currentRect.right
        val diffBottom = newRect.bottom - currentRect.bottom
        resizeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val factor = it.animatedValue as Float
                clipRect.set(
                    currentRect.left + factor * diffLeft, currentRect.top + factor * diffTop,
                    currentRect.right + factor * diffRight, currentRect.bottom + factor * diffBottom
                )
                children.forEach { child ->
                    // 根据factor得到当前的scale
                    val destScale = getNewScale(clipRect)
                    (child as AbsLayer).transformLayerByResize(clipRect, destScale)
                }
                requestLayout()
            }
            start()
        }
    }

    private fun getNewScale(newRect: RectF): Float {
        return when {
            resizeRect.width() > resizeRect.height() -> {
                when {
                    newRect.width() > newRect.height() -> {
                        if (newRect.height() < resizeRect.height()) {
                            newRect.height() / resizeRect.height()
                        } else 1.0f
                    }

                    else -> {
                        newRect.width() / resizeRect.width()
                    }
                }
            }

            resizeRect.width() < resizeRect.height() -> {
                when {
                    newRect.width() < newRect.height() -> {
                        if (newRect.width() < resizeRect.width()) {
                            newRect.width() / resizeRect.width()
                        } else 1.0f
                    }

                    else -> {
                        newRect.height() / resizeRect.height()
                    }
                }
            }

            else -> {
                when {
                    newRect.width() < newRect.height() -> {
                        newRect.width() / resizeRect.width()
                    }

                    newRect.width() > newRect.height() -> {
                        newRect.height() / resizeRect.height()
                    }

                    else -> 1.0f
                }
            }
        }
    }

    override fun addView(child: View?, index: Int, params: LayoutParams?) {
        child?.scaleX
        if (child !is AbsLayer) {
            throw IllegalArgumentException("Child $child must implement AbsLayer")
        }
        super.addView(child, index, params)
    }

    fun clearLayers() {
        removeAllViews()
    }


    /******************** 触摸事件 ********************/
    // 拦截所有子控件的触摸事件，交由当前父控件来统一处理
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // if (!clipRect.contains(event.x, event.y)) return false
        // scaleGestureListener.onTouchEvent(event)
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                if (!clipRect.contains(event.x, event.y)) return false
                onDown(event)
            }

            MotionEvent.ACTION_MOVE -> onMove(event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> onUp(event)
        }
        return true
    }

    private fun onDown(event: MotionEvent) {
        touchDownPoint.set(event.x, event.y)
        touchDownMillis = System.currentTimeMillis()
        lastX = event.x
        lastY = event.y
    }

    private fun onMove(event: MotionEvent) {
        currentLayerView?.let {
            val dx = event.x - lastX
            val dy = event.y - lastY
            it.translate(dx, dy)
        }
        lastX = event.x
        lastY = event.y
    }

    private fun onUp(event: MotionEvent) {
        resizeRect.set(clipRect)
        children.forEach { child ->
            (child as AbsLayer).stagingResizeInfo(clipRect)
        }

        val distance = calculateDistance(touchDownPoint.x, touchDownPoint.y, event.x, event.y)
        val isMoved = distance > MIN_MOVE_DISTANCE
        val isClickTime = System.currentTimeMillis() - touchDownMillis < MAX_CLICK_DURATION
        if (isClickTime && !isMoved) {
            processClickEvent(event.x, event.y)
        }
    }

    private fun processClickEvent(x: Float, y: Float) {
        findTouchedLayer(x, y)?.let { layerView ->
            if (currentLayerView == layerView) return
            currentLayerView?.isSelectedLayer = false
            currentLayerView?.invalidateView()

            currentLayerView = layerView
            currentLayerView?.isSelectedLayer = true
            currentLayerView?.invalidateView()
            ValueAnimator.ofFloat(1f, 0.95f, 1f).apply {
                duration = 200
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener {
                    val scaleFactor = it.animatedValue as Float
                    val view = (layerView as View)
                    view.scaleX = scaleFactor
                    view.scaleY = scaleFactor
                }
                addListener(doOnStart { vibratorHelper.vibrate() })
                start()
            }
        } ?: run {
            currentLayerView?.isSelectedLayer = false
            currentLayerView?.invalidateView()
            currentLayerView = null
        }
    }

    private fun findTouchedLayer(x: Float, y: Float): AbsLayer? {
        // 反向遍历children
        for (i in childCount - 1 downTo 0) {
            val child = getChildAt(i)
            if (child is AbsLayer && child.isTouchedInLayer(x, y)) {
                return child
            }
        }
        return null
    }

    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val x = x2 - x1
        val y = y2 - y1
        return sqrt(x * x + y * y)
    }
}