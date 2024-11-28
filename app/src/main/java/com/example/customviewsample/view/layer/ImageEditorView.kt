package com.example.customviewsample.view.layer

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
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
    private var currentLayerView: View? = null
    private var resizeAnimator: ValueAnimator? = null
    private val layoutInfos = mutableListOf<LayoutInfo>()
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
                val newCx = clipRect.left + layoutInfo.centerXRatio * clipRect.width()
                val newCy = clipRect.top + layoutInfo.centerYRatio * clipRect.height()
                val left = (newCx - newWidth / 2f).toInt()
                val top = (newCy - newHeight / 2f).toInt()
                val right = (newCx + newWidth / 2f).toInt()
                val bottom = (newCy + newHeight / 2f).toInt()
                child.layout(left, top, right, bottom)
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
        val imageLayerView = ImageLayerView(context, cornerRadius = cornerRadius)
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

        // 让其摆放在右下角
        val tx = (clipRect.width() - imageWidth) / 2
        val ty = (clipRect.height() - imageHeight) / 2
        val cx = clipRect.centerX() + tx
        val cy = clipRect.centerY() + ty
        // imageLayerView.rotation = 30f
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
        val curRect = RectF(clipRect)
        val newRect = calculateClipRect(width, height)
        val diffLeft = newRect.left - curRect.left
        val diffTop = newRect.top - curRect.top
        val diffRight = newRect.right - curRect.right
        val diffBottom = newRect.bottom - curRect.bottom
        resizeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = LinearInterpolator()
            addUpdateListener {
                val factor = it.animatedValue as Float
                clipRect.set(
                    curRect.left + factor * diffLeft, curRect.top + factor * diffTop,
                    curRect.right + factor * diffRight, curRect.bottom + factor * diffBottom
                )

                // TODO 重新计算子控件的LayoutInfo
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
    }

    fun clearLayers() {
        removeAllViews()
        layoutInfos.clear()
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

    }


}