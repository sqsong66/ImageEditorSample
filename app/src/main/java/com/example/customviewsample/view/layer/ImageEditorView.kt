package com.example.customviewsample.view.layer

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.example.customviewsample.R
import com.example.customviewsample.common.ext.isSameRect
import com.example.customviewsample.common.helper.VibratorHelper
import com.example.customviewsample.data.CanvasSize
import com.example.customviewsample.utils.dp2Px
import com.example.customviewsample.view.AlphaGridDrawHelper
import com.example.customviewsample.view.layer.anno.AbsLayer
import com.example.customviewsample.view.layer.anno.GestureMode

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
    private var isSaveMode = false
    private val preClipRect = RectF()
    private var touchDownMillis = 0L
    private val touchDownPoint = PointF()

    // 尺寸变换时临时存储矩形框
    private val resizeRect = RectF()
    private var currentLayerView: AbsLayer? = null
    private var resizeAnimator: ValueAnimator? = null

    @GestureMode
    private var gestureMode: Int = GestureMode.GESTURE_NONE

    // 双指时两指之间的距离
    private var fingerDistance = 0f

    // 双指时两指之间的中心点
    private val touchDownFingerCenter = PointF()

    // 双指按下时的角度
    private var fingerDownAngle = 0f

    // 双指移动时中心点
    private val moveFingerCenter = PointF()

    private val vibratorHelper by lazy { VibratorHelper(context) }
    var canvasSize = CanvasSize(width = 1512, height = 1512, iconRes = R.drawable.ic_picture, title = "Square", isTint = true)
        private set

    private val testPaint by lazy {
        Paint().apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
    }

    private val alphaGridDrawHelper by lazy {
        AlphaGridDrawHelper(
            gridSize = dp2Px(20),
            lightColor = ContextCompat.getColor(context, R.color.grid_light),
            darkColor = ContextCompat.getColor(context, R.color.grid_dark)
        )
    }

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
        Log.i("sqsong", "ImageEditorView dispatchDraw")
        if (isSaveMode) {
            super.dispatchDraw(canvas)
        } else {
            canvas.save()
            clipPath.reset()
            clipPath.addRoundRect(clipRect, cornerRadius, cornerRadius, Path.Direction.CW)
            canvas.clipPath(clipPath)
            alphaGridDrawHelper.drawAlphaGrid(canvas, clipRect)
            super.dispatchDraw(canvas)
            canvas.restore()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        clipRect.set(calculateClipRect(w, h))
        stagingChildResizeInfo(updateLayoutInfo = false)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
        // 测量所有子控件
        children.forEach { child -> measureChild(child, widthMeasureSpec, heightMeasureSpec) }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val sizeChanged = !clipRect.isSameRect(preClipRect)
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
        ImageLayerView(context, cornerRadius = dp2Px(6), isSelectedLayer = true).apply {
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
        val destScale = getNewScale(resizeRect, newRect)
        children.forEach { child -> (child as AbsLayer).tempStagingSize() }
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
                    (child as AbsLayer).transformLayerByResize(clipRect, destScale, factor)
                }
                requestLayout()
            }
            start()
        }
    }

    override fun addView(child: View?, index: Int, params: LayoutParams?) {
        if (child !is AbsLayer) {
            throw IllegalArgumentException("Child $child must implement AbsLayer")
        }
        super.addView(child, index, params)
    }

    private fun stagingChildResizeInfo(updateLayoutInfo: Boolean) {
        resizeRect.set(clipRect)
        children.forEach { child ->
            (child as AbsLayer).apply {
                stagingResizeInfo(clipRect, updateLayoutInfo = updateLayoutInfo)
            }
        }
    }

    fun clearLayers() {
        removeAllViews()
    }

    private fun clearCurrentLayer() {
        currentLayerView?.isSelectedLayer = false
        currentLayerView?.invalidateView()
        currentLayerView = null
    }

    /******************** 触摸事件 ********************/
    // 拦截所有子控件的触摸事件，交由当前父控件来统一处理
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                if (!clipRect.contains(event.x, event.y)) {
                    clearCurrentLayer()
                    return false
                }
                onDown(event)
            }

            MotionEvent.ACTION_POINTER_DOWN -> onPointerDown(event)

            MotionEvent.ACTION_MOVE -> onMove(event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> onUp(event)
        }
        return true
    }

    private fun onDown(event: MotionEvent) {
        gestureMode = GestureMode.GESTURE_DRAG
        touchDownPoint.set(event.x, event.y)
        touchDownMillis = System.currentTimeMillis()
        lastX = event.x
        lastY = event.y
    }

    private fun onPointerDown(event: MotionEvent) {
        if (event.pointerCount != 2) return
        gestureMode = GestureMode.GESTURE_SCALE_ROTATE
        fingerDistance = calculateDistance(event)
        calculateCenterPoint(touchDownFingerCenter, event)
        fingerDownAngle = calculateRotation(event)
        currentLayerView?.stagingLayerTempCacheInfo(touchDownFingerCenter.x, touchDownFingerCenter.y)
    }

    private fun onMove(event: MotionEvent) {
        currentLayerView?.let { processMoveEvent(it, event) }
        lastX = event.x
        lastY = event.y
    }

    private fun onUp(event: MotionEvent) {
        val distance = calculateDistance(touchDownPoint.x, touchDownPoint.y, event.x, event.y)
        val isMoved = distance > MIN_MOVE_DISTANCE
        val isClickTime = System.currentTimeMillis() - touchDownMillis < MAX_CLICK_DURATION
        if (isClickTime && !isMoved) {
            processClickEvent(event.x, event.y)
        }
        stagingChildResizeInfo(updateLayoutInfo = true)
        currentLayerView?.resetLayerPivot()
    }

    private fun processMoveEvent(absLayer: AbsLayer, event: MotionEvent) {
        when (gestureMode) {
            GestureMode.GESTURE_DRAG -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                absLayer.translateLayer(dx, dy, clipRect.centerX(), clipRect.centerY())
            }

            GestureMode.GESTURE_SCALE_ROTATE -> {
                scaleRotateLayer(absLayer, event)
            }
        }
    }

    private fun scaleRotateLayer(absLayer: AbsLayer, event: MotionEvent) {
        if (event.pointerCount != 2) return
        val newDistance = calculateDistance(event)
        calculateCenterPoint(moveFingerCenter, event)
        if (newDistance > MIN_MOVE_DISTANCE) {
            val scale = newDistance / fingerDistance
            val angle = calculateRotation(event)
            val deltaAngle = angle - fingerDownAngle
            val tx = moveFingerCenter.x - touchDownFingerCenter.x
            val ty = moveFingerCenter.y - touchDownFingerCenter.y
            absLayer.onScaleRotate(scale, deltaAngle, tx, ty, moveFingerCenter.x, moveFingerCenter.y)
        }
    }

    private fun processClickEvent(x: Float, y: Float) {
        findTouchedLayer(x, y)?.apply {
            if (currentLayerView == this) return
            clearCurrentLayer()
            isSelectedLayer = true
            invalidateView()
            vibratorHelper.vibrate()
            startTouchAnim()
            currentLayerView = this
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

    // 无动画效果切换尺寸
    /*fun updateCanvasSize(canvasSize: CanvasSize) {
        this.canvasSize = canvasSize
        val currentRect = RectF(clipRect)
        val newRect = calculateClipRect(width, height)
        if (currentRect.isSameRect(newRect)) return
        clipRect.set(newRect)
        val destScale = getNewScale(clipRect)
        children.forEach { child ->
            (child as AbsLayer).transformLayerByResize(clipRect, destScale, 1f)
        }
        requestLayout()
    }*/

    fun getEditorBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(canvasSize.width, canvasSize.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // 计算缩放比例
        val scaleX = canvasSize.width.toFloat() / clipRect.width()
        val scaleY = canvasSize.height.toFloat() / clipRect.height()
        // 创建矩阵，将 clipRect 区域映射到 Bitmap
        val matrix = Matrix()
        matrix.postTranslate(-clipRect.left, -clipRect.top)
        matrix.postScale(scaleX, scaleY)
        // 保存 Canvas 状态
        canvas.save()
        // 应用矩阵变换
        canvas.concat(matrix)
        isSaveMode = true
        children.forEach { child -> (child as AbsLayer).changeSaveState(true) }
        // 调用 dispatchDraw 绘制子控件
        dispatchDraw(canvas)
        isSaveMode = false
        children.forEach { child -> (child as AbsLayer).changeSaveState(false) }
        // 恢复 Canvas 状态
        canvas.restore()
        return bitmap
    }
}