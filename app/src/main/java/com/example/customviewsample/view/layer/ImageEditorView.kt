package com.example.customviewsample.view.layer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.doOnLayout
import com.example.customviewsample.R
import com.example.customviewsample.utils.dp2Px
import com.example.customviewsample.view.AlphaGridDrawHelper

data class LayoutInfo(
    var widthRatio: Float,
    var heightRatio: Float,
    var centerXRatio: Float,
    var centerYRatio: Float
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
    private var canvasSize = Size(1080, 1920)
    private val layoutInfos = mutableListOf<LayoutInfo>()

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
        updateClipRect(r - l, b - t)
        val sizeChanged = preClipRect.width() != clipRect.width() || preClipRect.height() != clipRect.height()
        Log.e("sqsong", "onLayout sizeChanged: $sizeChanged")
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
                val cx = (child.left + child.right) / 2f + child.translationX
                val cy = (child.top + child.bottom) / 2f + child.translationY
                layoutInfo.centerXRatio = (cx - clipRect.left) / clipRect.width()
                layoutInfo.centerYRatio = (cy - clipRect.top) / clipRect.height()
                layoutInfo.widthRatio = (child.right - child.left) / clipRect.width()
                layoutInfo.heightRatio = (child.bottom - child.top) / clipRect.height()
                Log.w("sqsong", "layoutInfo: $layoutInfo")
            }
        }
        preClipRect.set(clipRect)
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

        val tx = (clipRect.width() - imageWidth) / 2
        val ty = (clipRect.height() - imageHeight) / 2
        imageLayerView.translationX = tx
        imageLayerView.translationY = ty

        val cx = clipRect.centerX() + imageLayerView.translationX
        val cy = clipRect.centerY() + imageLayerView.translationY
        val left = (cx - imageWidth / 2f).toInt()
        val top = (cy - imageHeight / 2f).toInt()
        val right = (cx + imageWidth / 2f).toInt()
        val bottom = (cy + imageHeight / 2f).toInt()
        imageLayerView.layout(left, top, right, bottom)

        // 计算并保存 LayoutInfo
        val widthRatio = imageWidth / clipRect.width()
        val heightRatio = imageHeight / clipRect.height()
        val centerXRatio = (cx - clipRect.left) / clipRect.width()
        val centerYRatio = (cy - clipRect.top) / clipRect.height()
        Log.d("sqsong", "widthRatio: $widthRatio, heightRatio: $heightRatio, centerXRatio: $centerXRatio, centerYRatio: $centerYRatio")
        layoutInfos.add(LayoutInfo(widthRatio, heightRatio, centerXRatio, centerYRatio))
    }


    override fun dispatchDraw(canvas: Canvas) {
        canvas.save()
        canvas.clipPath(clipPath)
        alphaGridDrawHelper.drawAlphaGrid(canvas, clipRect)
        super.dispatchDraw(canvas)

        canvas.drawCircle(clipRect.centerX(), clipRect.centerY(), 10f, testPaint)
        canvas.restore()
    }

    private fun updateClipRect(width: Int, height: Int) {
        val canvasRatio = canvasSize.width.toFloat() / canvasSize.height
        val viewRatio = width.toFloat() / height
        if (canvasRatio > viewRatio) {
            val clipWidth = width.toFloat()
            val clipHeight = clipWidth / canvasRatio
            clipRect.set(0f, (height - clipHeight) / 2, clipWidth, (height + clipHeight) / 2)
        } else {
            val clipHeight = height.toFloat()
            val clipWidth = clipHeight * canvasRatio
            clipRect.set((width - clipWidth) / 2, 0f, (width + clipWidth) / 2, clipHeight)
        }
        clipPath.reset()
        clipPath.addRoundRect(clipRect, cornerRadius, cornerRadius, Path.Direction.CW)
//        if (preClipRect.isEmpty) preClipRect.set(clipRect)
    }

    fun clearLayers() {
        removeAllViews()
        layoutInfos.clear()
    }

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
        children.lastOrNull()?.let {
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