package com.example.customviewsample.view.layer

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
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
import androidx.core.animation.addListener
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.doOnLayout
import com.example.customviewsample.R
import com.example.customviewsample.common.ext.isSameRect
import com.example.customviewsample.common.helper.VibratorHelper
import com.example.customviewsample.data.CanvasSize
import com.example.customviewsample.utils.dp2Px
import com.example.customviewsample.utils.getThemeColor
import com.example.customviewsample.view.AlphaGridDrawHelper
import com.example.customviewsample.view.layer.anno.CoordinateLocation
import com.example.customviewsample.view.layer.anno.GestureMode
import com.example.customviewsample.view.layer.anno.LayerChangedMode
import com.example.customviewsample.view.layer.anno.LayerRotation
import com.example.customviewsample.view.layer.anno.LayerType
import com.example.customviewsample.view.layer.data.ImageEditorSnapshot
import com.example.customviewsample.view.layer.data.LayerPreviewData
import com.example.customviewsample.view.layer.listener.ImageEditorActionListener
import com.example.customviewsample.view.layer.manager.UndoRedoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

const val SCALE_FACTOR = 0.8f
const val ROTATION_THRESHOLD = 10.0f
const val COORDINATE_DETECT_OFFSET = 5f
const val COORDINATE_MOVE_THRESHOLD = 6f
private const val MIN_MOVE_DISTANCE = 10f
private const val MAX_CLICK_DURATION = 400L
private const val TOUCH_MOVE_OFFSET = 6f

open class ImageEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr), CoroutineScope by MainScope() {

    private var lastX = 0f
    private var lastY = 0f
    private var borderColor = 0
    private var borderWidth = 0f
    private var cornerRadius = 0f
    private val clipPath = Path()
    private val tempRect = RectF()
    private val clipRect = RectF()
    private var isSaveMode = false
    private var touchDownMillis = 0L
    private val preClipRect = RectF()
    private val tempMatrix = Matrix()
    private val touchDownPoint = PointF()
    private val currentLayerCenter = PointF()

    // 尺寸变换时临时存储矩形框
    private val resizeRect = RectF()
    private var currentLayerView: AbsLayerView? = null
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

    private val undoRedoManager by lazy { UndoRedoManager() }

    private var actionListener: ImageEditorActionListener? = null

    @CoordinateLocation
    private var coordinateLoc: Int = CoordinateLocation.COORDINATE_NONE

    @LayerRotation
    private var layerRotation: Int = LayerRotation.ROTATION_NONE

    private val dashPathEffect by lazy {
        DashPathEffect(floatArrayOf(dp2Px(3), dp2Px(3)), 0f)
    }

    private val vibratorHelper by lazy { VibratorHelper(context) }
    var canvasSize = CanvasSize(width = 1512, height = 1512, iconRes = R.drawable.ic_picture, title = "Square", isTint = true)
        private set

    private val borderPaint by lazy {
        Paint().apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = borderWidth
        }
    }

    private val testPaint by lazy {
        Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = borderWidth
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
        doOnLayout { // 等待ViewGroup布局完成后需要在undoRedoManager中保存一次初始快照
            undoRedoManager.saveSnapshot(ImageEditorSnapshot(canvasSize.copy(), RectF(clipRect), emptyList()))
        }
    }

    private fun handleAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.ImageEditorView).apply {
            cornerRadius = getDimension(R.styleable.ImageEditorView_iev_cornerRadius, dp2Px(6))
            borderWidth = getDimension(R.styleable.ImageEditorView_iev_borderWidth, dp2Px(1.5f))
            borderColor = getColor(R.styleable.ImageEditorView_iev_borderColor, getThemeColor(context, com.google.android.material.R.attr.colorPrimary))
            recycle()
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        // Log.i("songmao", "ImageEditorView dispatchDraw")
        if (isSaveMode) {
            super.dispatchDraw(canvas)
        } else {
            canvas.save()
            clipPath.reset()
            clipPath.addRoundRect(clipRect, cornerRadius, cornerRadius, Path.Direction.CW)
            canvas.clipPath(clipPath)
            alphaGridDrawHelper.drawAlphaGrid(canvas, clipRect)
            super.dispatchDraw(canvas)

            currentLayerView?.let { layerView ->
                drawRotationAuxiliaryLine(canvas, layerView)
                drawCoordinateAuxiliaryLine(canvas)
                /*tempMatrix.set(layerView.matrix)
                tempMatrix.postTranslate(layerView.left.toFloat(), layerView.top.toFloat())
                tempRect.set(0f, 0f, layerView.width.toFloat(), layerView.height.toFloat())
                tempMatrix.mapRect(tempRect)
                canvas.drawRect(tempRect, testPaint)*/
            }
            canvas.restore()
        }
    }

    private fun drawRotationAuxiliaryLine(canvas: Canvas, layerView: AbsLayerView) {
        if (layerRotation == LayerRotation.ROTATION_NONE) return
        borderPaint.pathEffect = dashPathEffect
        val point = layerView.getLayerCenterPoint()
        val radian = Math.toRadians((layerView.rotation).toDouble())
        val dx = cos(radian)
        val dy = sin(radian)
        val length = hypot(clipRect.width(), clipRect.height()) * 1.5f
        val halfLength = length / 2f
        val x1 = point.x - dx * halfLength
        val y1 = point.y - dy * halfLength
        val x2 = point.x + dx * halfLength
        val y2 = point.y + dy * halfLength
        canvas.drawLine(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), borderPaint)
    }

    private fun drawCoordinateAuxiliaryLine(canvas: Canvas) {
        if (coordinateLoc == CoordinateLocation.COORDINATE_NONE) return
        borderPaint.pathEffect = null
        when (coordinateLoc) {
            CoordinateLocation.COORDINATE_CENTER -> {
                canvas.drawLine(clipRect.centerX(), clipRect.top, clipRect.centerX(), clipRect.bottom, borderPaint)
                canvas.drawLine(clipRect.left, clipRect.centerY(), clipRect.right, clipRect.centerY(), borderPaint)
            }

            CoordinateLocation.COORDINATE_CENTER_X -> {
                canvas.drawLine(clipRect.centerX(), clipRect.top, clipRect.centerX(), clipRect.bottom, borderPaint)
            }

            CoordinateLocation.COORDINATE_CENTER_Y -> {
                canvas.drawLine(clipRect.left, clipRect.centerY(), clipRect.right, clipRect.centerY(), borderPaint)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        clipRect.set(calculateClipRect(w, h))
        stagingChildResizeInfo(updateLayoutInfo = false)
        currentLayerView?.let { showLayerEditMenu(it) }
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
            val layer = child as AbsLayerView
            if (sizeChanged) { // 画布区域如果发生变化则需要根据布局信息重新计算子View的位置
                layer.onUpdateLayout(clipRect)
            } else { // 画布区域未发生变化则根据控件的布局更新布局信息
                layer.updateLayoutInfo(clipRect)
            }
        }
        preClipRect.set(clipRect)
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

    override fun addView(child: View?, index: Int, params: LayoutParams?) {
        if (child !is AbsLayerView) {
            throw IllegalArgumentException("Child $child must implement AbsLayerView")
        }
        super.addView(child, index, params)
    }

    private fun stagingChildResizeInfo(updateLayoutInfo: Boolean) {
        resizeRect.set(clipRect)
        children.forEach { child ->
            (child as AbsLayerView).apply {
                stagingResizeInfo(clipRect, updateLayoutInfo = updateLayoutInfo)
            }
        }
    }

    private fun clearCurrentLayer() {
        currentLayerView?.isSelectedLayer = false
        currentLayerView?.invalidate()
        currentLayerView = null
        actionListener?.hideLayerEditMenu()
    }

    private fun showLayerEditMenu(layerView: AbsLayerView) {
        if (!layerView.isEditMenuAvailable()) return
        tempRect.set(0f, 0f, layerView.width.toFloat(), layerView.height.toFloat())
        tempMatrix.set(layerView.matrix)
        tempMatrix.postTranslate(layerView.left.toFloat(), layerView.top.toFloat())
        tempMatrix.mapRect(tempRect)
        actionListener?.onShowLayerEditMenu(tempRect.centerX(), tempRect.top - dp2Px<Int>(10))
    }

    private fun saveLayerSnapshot() {
        flow {
            val snapshots = children.mapNotNull { (it as AbsLayerView).toLayerSnapshot(clipRect) }.toList()
            val layerSnapshot = ImageEditorSnapshot(canvasSize.copy(), RectF(clipRect), snapshots)
            emit(layerSnapshot)
        }.flowOn(Dispatchers.IO)
            .catch { e -> e.printStackTrace() }
            .onEach { snapshot ->
                undoRedoManager.saveSnapshot(snapshot)
                actionListener?.onUndoRedoStateChanged(undoRedoManager.canUndo(), undoRedoManager.canRedo(), false)
            }
            .launchIn(this)
    }

    /**
     * 从快照中恢复画布及图层数据。
     * @param snapshot 快照信息
     */
    private fun restoreSnapshot(snapshot: ImageEditorSnapshot) {
        clearLayers()
        actionListener?.hideLayerEditMenu()
        currentLayerView = null
        if (canvasSize != snapshot.canvasSize) {
            actionListener?.onCanvasSizeChanged(snapshot.canvasSize)
        }
        canvasSize = snapshot.canvasSize
        clipRect.set(calculateClipRect(width, height))
        snapshot.layerList.forEach { layerSnapshot ->
            when (layerSnapshot.viewLayerType) {
                LayerType.LAYER_IMAGE -> {
                    // 此处的layoutInfo需要复制快照中的layoutInfo，否则会导致多个LayerView共用一个layoutInfo，导致[UndoRedoManager]中的undoList中的layoutInfo被修改
                    ImageLayerView(context, layoutInfo = layerSnapshot.layoutInfo.copy()).apply {
                        restoreLayerFromSnapshot(this@ImageEditorView, layerSnapshot, clipRect)
                    }
                }

                LayerType.LAYER_BACKGROUND -> {
                    BackgroundLayerView(context, layoutInfo = layerSnapshot.layoutInfo.copy()).apply {
                        restoreLayerFromSnapshot(this@ImageEditorView, layerSnapshot, clipRect)
                    }
                }

                else -> {

                }
            }
        }
        stagingChildResizeInfo(updateLayoutInfo = false)
        requestLayout()
        actionListener?.onUndoRedoStateChanged(undoRedoManager.canUndo(), undoRedoManager.canRedo(), true)
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
        currentLayerView?.apply {
            updateTouchState(true)
            if (isEditMenuAvailable()) {
                actionListener?.hideLayerEditMenu()
            }
            currentLayerCenter.set(getLayerCenterPoint())
        }
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
        currentLayerView?.let {
            val distance = calculateDistance(touchDownPoint.x, touchDownPoint.y, event.x, event.y)
            if (distance > TOUCH_MOVE_OFFSET) {
                processMoveEvent(it, event)
            }
        }
        lastX = event.x
        lastY = event.y
    }

    private fun onUp(event: MotionEvent) {
        // 判断是否需要保存快照
        currentLayerView?.let { layerView ->
            val newCenter = layerView.getLayerCenterPoint()
            val isLayerMoved = calculateDistance(currentLayerCenter.x, currentLayerCenter.y, newCenter.x, newCenter.y) > MIN_MOVE_DISTANCE
            if (isLayerMoved) {
                saveLayerSnapshot()
            }
        }
        val distance = calculateDistance(touchDownPoint.x, touchDownPoint.y, event.x, event.y)
        val isMoved = distance > MIN_MOVE_DISTANCE
        val isClickTime = System.currentTimeMillis() - touchDownMillis < MAX_CLICK_DURATION
        if (isClickTime && !isMoved) processClickEvent(event.x, event.y)
        stagingChildResizeInfo(updateLayoutInfo = true)
        currentLayerView?.apply {
            resetLayerPivot()
            updateTouchState(false)
            if (isEditMenuAvailable()) showLayerEditMenu(this)
        } ?: run {
            actionListener?.hideLayerEditMenu()
        }
        coordinateLoc = CoordinateLocation.COORDINATE_NONE
        layerRotation = LayerRotation.ROTATION_NONE
        invalidate()
    }

    private fun processMoveEvent(absLayer: AbsLayerView, event: MotionEvent) {
        when (gestureMode) {
            GestureMode.GESTURE_DRAG -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                coordinateLoc = absLayer.translateLayer(dx, dy, clipRect.centerX(), clipRect.centerY()) {
                    vibratorHelper.vibrate()
                }
                invalidate()
            }

            GestureMode.GESTURE_SCALE_ROTATE -> {
                scaleRotateLayer(absLayer, event)
            }
        }
    }

    private fun scaleRotateLayer(absLayer: AbsLayerView, event: MotionEvent) {
        if (event.pointerCount != 2) return
        val newDistance = calculateDistance(event)
        calculateCenterPoint(moveFingerCenter, event)
        if (newDistance > MIN_MOVE_DISTANCE) {
            val scale = newDistance / fingerDistance
            val angle = calculateRotation(event)
            val deltaAngle = angle - fingerDownAngle
            val tx = moveFingerCenter.x - touchDownFingerCenter.x
            val ty = moveFingerCenter.y - touchDownFingerCenter.y
            val (loc, rotation) = absLayer.onLayerTranslation(scale, deltaAngle, tx, ty, clipRect.centerX(), clipRect.centerY()) {
                vibratorHelper.vibrate()
            }
            coordinateLoc = loc
            layerRotation = rotation
            invalidate()
        }
    }

    private fun processClickEvent(x: Float, y: Float) {
        findTouchedLayer(x, y)?.apply {
            if (currentLayerView == this) return
            clearCurrentLayer()
            isSelectedLayer = true
            invalidate()
            vibratorHelper.vibrate()
            startTouchAnim()
            currentLayerView = this
        } ?: run {
            currentLayerView?.isSelectedLayer = false
            currentLayerView?.invalidate()
            currentLayerView = null
        }
    }

    private fun findTouchedLayer(x: Float, y: Float): AbsLayerView? {
        // 反向遍历children
        for (i in childCount - 1 downTo 0) {
            val child = getChildAt(i)
            if (child is AbsLayerView && child.isTouchedInLayer(x, y)) {
                return child
            }
        }
        return null
    }

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
        children.forEach { child -> (child as AbsLayerView).changeSaveState(true) }
        // 调用 dispatchDraw 绘制子控件
        dispatchDraw(canvas)
        isSaveMode = false
        children.forEach { child -> (child as AbsLayerView).changeSaveState(false) }
        // 恢复 Canvas 状态
        canvas.restore()
        return bitmap
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 取消所有协程，防止内存泄漏
        cancel()
    }

    /******************** Public Method ********************/
    fun addImageLayer(bitmap: Bitmap) {
        ImageLayerView(context).apply {
            onInitialLayout(this@ImageEditorView, bitmap, clipRect)
            currentLayerView?.isSelectedLayer = false
            currentLayerView?.invalidate()
            currentLayerView = this
            showLayerEditMenu(this)
            actionListener?.onAddOrUpdateLayer(LayerChangedMode.ADD, toLayerPreview())
        }
        saveLayerSnapshot()
    }

    fun addBackgroundLayer(bitmap: Bitmap) {
        BackgroundLayerView(context).apply {
            onInitialLayout(this@ImageEditorView, bitmap, clipRect)
            currentLayerView?.isSelectedLayer = false
            currentLayerView?.invalidate()
            currentLayerView = this
            actionListener?.onAddOrUpdateLayer(LayerChangedMode.ADD, toLayerPreview())
        }
        saveLayerSnapshot()
    }

    private fun addBackgroundLayer(bgColor: IntArray) {
        BackgroundLayerView(context).apply {
            onInitialLayout(this@ImageEditorView, bgColor, clipRect)
            currentLayerView?.isSelectedLayer = false
            currentLayerView?.invalidate()
            currentLayerView = this
            actionListener?.onAddOrUpdateLayer(LayerChangedMode.ADD, toLayerPreview())
        }
        saveLayerSnapshot()
    }

    fun updateBackgroundLayerColor(bgColor: IntArray) {
        val bgLayer = getChildAt(0) as? BackgroundLayerView
        if (bgLayer != null) {
            bgLayer.updateBackgroundColor(bgColor)
            saveLayerSnapshot()
            actionListener?.onAddOrUpdateLayer(LayerChangedMode.UPDATE, bgLayer.toLayerPreview())
        } else {
            addBackgroundLayer(bgColor)
        }
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
        val destBgScale = getNewBgScale(resizeRect, newRect)
        children.forEach { child -> (child as AbsLayerView).tempStagingSize() }
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
                    (child as AbsLayerView).transformLayerByResize(clipRect, destScale, destBgScale, factor)
                }
                requestLayout()
            }
            addListener(onEnd = {
                Log.w("sqsong", "resizeAnimator end")
                saveLayerSnapshot()
            })
            start()
        }
    }

    fun clearLayers() {
        removeAllViews()
    }

    fun setImageEditorActionListener(actionListener: ImageEditorActionListener) {
        this.actionListener = actionListener
    }

    fun removeCurrentLayer() {
        currentLayerView?.let {
            removeView(it)
            actionListener?.onAddOrUpdateLayer(LayerChangedMode.REMOVE, it.toLayerPreview())
            currentLayerView = null
            actionListener?.hideLayerEditMenu()
            saveLayerSnapshot()
        }
    }

    fun undo() {
        val snapshot = undoRedoManager.undo() ?: return
        restoreSnapshot(snapshot)
    }

    fun redo() {
        val snapshot = undoRedoManager.redo() ?: return
        restoreSnapshot(snapshot)
    }

    fun getLayerPreviewList(): List<LayerPreviewData> {
        return children.map { (it as AbsLayerView).toLayerPreview() }.toList().reversed()
    }

    fun swapLayerOrder(fromPosition: Int, toPosition: Int) {
        val childCount = childCount
        val index1 = childCount - 1 - fromPosition
        val index2 = childCount - 1 - toPosition
        if (index1 < 0 || index1 >= childCount || index2 < 0 || index2 >= childCount) return
        if (index1 == index2) return
        val view1 = getChildAt(index1)
        val view2 = getChildAt(index2)
        // 为了避免移除后索引变化，先移除索引较大的子控件
        if (index1 > index2) {
            removeViewAt(index1)
            removeViewAt(index2)
            // 交换顺序重新添加
            addView(view1, index2)
            addView(view2, index1)
        } else {
            removeViewAt(index2)
            removeViewAt(index1)
            // 交换顺序重新添加
            addView(view2, index1)
            addView(view1, index2)
        }
        saveLayerSnapshot()
    }
}