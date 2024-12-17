package com.example.customviewsample.view.layer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.ViewGroup
import com.example.customviewsample.view.layer.anno.LayerType

class BackgroundLayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ImageLayerView(context, attrs, defStyleAttr, cornerRadius = 0f) {

    private var bgColor: IntArray? = null

    private val bgPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        bgColor?.let { color ->
            val shader = LinearGradient(0f, height / 2f, width.toFloat(), height / 2f, color, null, Shader.TileMode.CLAMP)
            bgPaint.shader = shader
        }
    }

    override fun getViewLayerType(): Int = LayerType.LAYER_BACKGROUND

    override fun isTouchedInLayer(x: Float, y: Float): Boolean {
        val localPoint = mapCoordinateToLocal(this, x, y)
        return localPoint[0] >= 0 && localPoint[0] <= width && localPoint[1] >= 0 && localPoint[1] <= height
    }

    override fun transformLayerByResize(clipRect: RectF, destScale: Float, factor: Float) {
        super.transformLayerByResize(clipRect, destScale, factor)
    }

    override fun onInitialLayout(parentView: ViewGroup, bitmap: Bitmap, clipRect: RectF) {
        this.bgColor = null
        this.imageBitmap = bitmap
        this.isSelectedLayer = true
        val layerWidth = clipRect.width().toInt()
        val layerHeight = clipRect.height().toInt()
        val layerRatio = layerWidth.toFloat() / layerHeight
        val bitmapRatio = bitmap.width.toFloat() / bitmap.height

        val scale = if (layerRatio > bitmapRatio) {
            layerWidth.toFloat() / bitmap.width
        } else {
            layerHeight.toFloat() / bitmap.height
        }
        imageMatrix.setScale(scale, scale)
        val imageWidth = (bitmap.width * scale).toInt()
        val imageHeight = (bitmap.height * scale).toInt()
        val layoutParams = ViewGroup.LayoutParams(imageWidth, imageHeight)
        (parentView.getChildAt(0) as? AbsLayerView)?.let { bgLayer ->
            if (bgLayer.getViewLayerType() == LayerType.LAYER_BACKGROUND) {
                parentView.removeViewAt(0)
            }
        }
        parentView.addView(this, 0, layoutParams)

        val left = clipRect.centerX() - imageWidth / 2f
        val top = clipRect.centerY() - imageHeight / 2f
        val right = clipRect.centerX() + imageWidth / 2f
        val bottom = clipRect.centerY() + imageHeight / 2f
        layout(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
        stagingResizeInfo(clipRect, true)
    }

    fun onInitialLayout(parentView: ViewGroup, bgColor: IntArray, clipRect: RectF) {
        this.bgColor = bgColor
        this.isSelectedLayer = true
        val layerWidth = clipRect.width().toInt()
        val layerHeight = clipRect.height().toInt()
        val layoutParams = ViewGroup.LayoutParams(layerWidth, layerHeight)

        val shader = LinearGradient(0f, layerHeight / 2f, layerWidth.toFloat(), layerHeight / 2f, bgColor, null, Shader.TileMode.CLAMP)
        bgPaint.shader = shader

        parentView.addView(this, 0, layoutParams)
        layout(clipRect.left.toInt(), clipRect.top.toInt(), clipRect.right.toInt(), clipRect.bottom.toInt())
        stagingResizeInfo(clipRect, true)
    }

    override fun onDraw(canvas: Canvas) {
        imageBitmap?.let {
            drawImageBitmap(canvas, it)
        } ?: run {
            drawBackgroundColor(canvas)
        }
    }

    private fun drawBackgroundColor(canvas: Canvas) {
        bgColor ?: return
        pathRect.set(0f, 0f, width.toFloat(), height.toFloat())
        if (isSelectedLayer && !isSaveMode) {
            borderPath.reset()
            val radius = cornerRadius / scaleX
            borderPath.addRoundRect(pathRect, radius, radius, Path.Direction.CW)
            canvas.save()
            canvas.clipPath(borderPath)
            canvas.drawRect(pathRect, bgPaint)
            canvas.restore()

            borderPaint.strokeWidth = borderWidth / scaleX
            canvas.drawPath(borderPath, borderPaint)
        } else {
            canvas.drawRect(pathRect, bgPaint)
        }
    }
}