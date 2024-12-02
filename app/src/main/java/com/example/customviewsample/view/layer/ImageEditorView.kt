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
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.customviewsample.R
import com.example.customviewsample.common.ext.isSameRect
import com.example.customviewsample.data.CanvasSize
import com.example.customviewsample.utils.dp2Px
import com.example.customviewsample.view.AlphaGridDrawHelper

data class LayoutInfo(
    var widthRatio: Float = 1f,
    var heightRatio: Float = 1f,
    var centerXRatio: Float = 0.5f,
    var centerYRatio: Float = 0.5f
)

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
    private val stageClipRect = RectF()
    private val centerPoints = mutableListOf<PointF>()

    // 尺寸变换时临时存储矩形框
    private val resizeRect = RectF()
    private var currentLayerView: View? = null
    private var resizeAnimator: ValueAnimator? = null
    private val layoutInfos = mutableListOf<LayoutInfo>()
    private val resizeList = mutableListOf<Size>()
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
        canvas.drawCircle(clipRect.centerX(), clipRect.centerY(), 10f, testPaint)
        canvas.restore()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        clipRect.set(calculateClipRect(w, h))
        if (resizeRect.isEmpty) {
            resizeRect.set(clipRect)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
        // 测量所有子控件
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val sizeChanged = clipRect.isSameRect(preClipRect)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val layoutInfo = layoutInfos.getOrNull(i) ?: continue
            if (sizeChanged) {
                // 尺寸发生变化，根据比例重新计算位置和尺寸
                val newWidth = layoutInfo.widthRatio * clipRect.width()
                val newHeight = layoutInfo.heightRatio * clipRect.height()
//                Log.w("sqsong", "onLayout newWidth: $newWidth, newHeight: $newHeight")
                val newCx = clipRect.left + layoutInfo.centerXRatio * clipRect.width()
                val newCy = clipRect.top + layoutInfo.centerYRatio * clipRect.height()
                val left = (newCx - newWidth / 2f).toInt()
                val top = (newCy - newHeight / 2f).toInt()
                val right = (newCx + newWidth / 2f).toInt()
                val bottom = (newCy + newHeight / 2f).toInt()
                child.layout(left, top, right, bottom)
//                Log.d("sqsong", "onLayout: left: $left, top: $top, right: $right, bottom: $bottom, clipRect: ${clipRect}, centerX: ${clipRect.centerX()}, centerY: ${clipRect.centerY()}")
                // 重置位移
                child.translationX = 0f
                child.translationY = 0f
            } else {
                // 尺寸未变化，更新 LayoutInfo
                updateChildLayoutInfo(layoutInfo, child)
            }
        }
        preClipRect.set(clipRect)
    }

    private fun updateChildLayoutInfo(layoutInfo: LayoutInfo, child: View) {
        val cx = (child.left + child.right) / 2f + child.translationX
        val cy = (child.top + child.bottom) / 2f + child.translationY
        layoutInfo.centerXRatio = (cx - clipRect.left) / clipRect.width()
        layoutInfo.centerYRatio = (cy - clipRect.top) / clipRect.height()
        layoutInfo.widthRatio = (child.right - child.left) / clipRect.width()
        layoutInfo.heightRatio = (child.bottom - child.top) / clipRect.height()
    }

    fun addImageLayer(bitmap: Bitmap) {
        val imageLayerView = ImageLayerView(context, cornerRadius = cornerRadius / 2f)
        imageLayerView.setImageBitmap(bitmap)

        val scaleFactor = 0.8f
        // 计算初始宽高，确保图片按比例缩放并居中
        var imageWidth = clipRect.width() * scaleFactor
        var imageHeight = imageWidth * bitmap.height / bitmap.width
        if (imageHeight > clipRect.height() * scaleFactor) {
            imageHeight = clipRect.height() * scaleFactor
            imageWidth = imageHeight * bitmap.width / bitmap.height
        }
        val layoutParams = LayoutParams(imageWidth.toInt(), imageHeight.toInt())
        addView(imageLayerView, layoutParams)
        // Log.d("sqsong", "addImageLayer: imageWidth: $imageWidth, imageHeight: $imageHeight")

        // 初始化让其摆放在右下角
        val tx = (clipRect.width() - imageWidth) / 2
        val ty = (clipRect.height() - imageHeight) / 2
        imageLayerView.translationX = tx
        imageLayerView.translationY = ty

        val cx = clipRect.centerX() // + tx
        val cy = clipRect.centerY() // + ty
        val left = (cx - imageWidth / 2f).toInt()
        val top = (cy - imageHeight / 2f).toInt()
        val right = (cx + imageWidth / 2f).toInt()
        val bottom = (cy + imageHeight / 2f).toInt()
        imageLayerView.layout(left, top, right, bottom)
        currentLayerView = imageLayerView

        // 计算并保存 LayoutInfo
        LayoutInfo().apply {
            updateChildLayoutInfo(this, imageLayerView)
            layoutInfos.add(this)
        }
        centerPoints.add(PointF((left + right) / 2f + tx, (top + bottom) / 2f + ty))
        resizeList.add(Size(imageWidth.toInt(), imageHeight.toInt()))
        stageClipRect.set(clipRect)
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
        val destRect = calculateClipRect(width, height)
        clipRect.set(destRect)

        // 最终要缩放的比例
        val destScale = getNewScale(clipRect)

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val layoutInfo = layoutInfos.getOrNull(i) ?: continue
            val size = resizeList.getOrNull(i) ?: continue
            val centerPoint = centerPoints.getOrNull(i) ?: continue
            val newWidth = size.width * destScale
            val newHeight = size.height * destScale
            layoutInfo.widthRatio = newWidth / clipRect.width()
            layoutInfo.heightRatio = newHeight / clipRect.height()

            // 计算平移量
            val cx = centerPoint.x
            val cy = centerPoint.y
            val dx = cx - clipRect.centerX()
            val dy = cy - clipRect.centerY()
            val tx = dx * destScale - dx
            val ty = dy * destScale - dy
            val newCx = cx + tx + (clipRect.centerX() - stageClipRect.centerX())
            val newCy = cy + ty + (clipRect.centerY() - stageClipRect.centerY())
            layoutInfo.centerXRatio = (newCx - clipRect.left) / clipRect.width()
            layoutInfo.centerYRatio = (newCy - clipRect.top) / clipRect.height()
//            Log.d("sqsong", "updateCanvasSize, destScale: $destScale, layoutInfo: $layoutInfo")
        }
        requestLayout()
    }

    /*fun updateCanvasSize(canvasSize: CanvasSize) {
        this.canvasSize = canvasSize
        resizeAnimator?.cancel()
        val curRect = RectF(clipRect)
        val newRect = calculateClipRect(width, height)
        val diffLeft = newRect.left - curRect.left
        val diffTop = newRect.top - curRect.top
        val diffRight = newRect.right - curRect.right
        val diffBottom = newRect.bottom - curRect.bottom

        // 最终要缩放的比例
        val destScale = getNewScale(newRect)

        val childrenSize = mutableListOf<Size>()
        repeat(childCount) {
            childrenSize.add(Size(getChildAt(it).width, getChildAt(it).height))
        }

        resizeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = LinearInterpolator()
            addUpdateListener {
                val factor = it.animatedValue as Float
                clipRect.set(
                    curRect.left + factor * diffLeft, curRect.top + factor * diffTop,
                    curRect.right + factor * diffRight, curRect.bottom + factor * diffBottom
                )

                // 重新计算子控件的 LayoutInfo
                for (i in 0 until childCount) {
                    val child = getChildAt(i)
                    val layoutInfo = layoutInfos.getOrNull(i) ?: continue

                    // 计算子控件在父视图坐标系中的中心位置，考虑到 translation
                    val cx = (child.left + child.right) / 2f + child.translationX
                    val cy = (child.top + child.bottom) / 2f + child.translationY

                    // 更新 LayoutInfo，相对于新的 clipRect 的比例
                    layoutInfo.centerXRatio = (cx - clipRect.left) / clipRect.width()
                    layoutInfo.centerYRatio = (cy - clipRect.top) / clipRect.height()
                    // 如果需要调整子控件的尺寸比例，可以在此处更新 widthRatio 和 heightRatio
                    layoutInfo.widthRatio = (child.width) / clipRect.width()
                    layoutInfo.heightRatio = (child.height) / clipRect.height()
                }
                requestLayout()
            }
            start()
        }
    }*/

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

    fun clearLayers() {
        removeAllViews()
        layoutInfos.clear()
        centerPoints.clear()
    }


    /******************** 触摸事件 ********************/
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> onDown(event)
            MotionEvent.ACTION_MOVE -> onMove(event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> onUp(event)
        }
        return true
    }

    private fun onDown(event: MotionEvent) {
        lastX = event.x
        lastY = event.y
    }

    private fun onMove(event: MotionEvent) {
        currentLayerView?.let {
            val dx = event.x - lastX
            val dy = event.y - lastY
            it.translationX += dx
            it.translationY += dy
        }

        lastX = event.x
        lastY = event.y
    }

    private fun onUp(event: MotionEvent) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val layoutInfo = layoutInfos.getOrNull(i) ?: continue
            updateChildLayoutInfo(layoutInfo, child)
            // 更新中心点
            val cx = (child.left + child.right) / 2f + child.translationX
            val cy = (child.top + child.bottom) / 2f + child.translationY
            centerPoints[i].set(cx, cy)
        }
        stageClipRect.set(clipRect)
        resizeRect.set(clipRect)
    }

}