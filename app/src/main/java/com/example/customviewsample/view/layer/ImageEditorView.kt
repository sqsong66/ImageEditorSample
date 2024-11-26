package com.example.customviewsample.view.layer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Size
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.customviewsample.R
import com.example.customviewsample.utils.dp2Px
import com.example.customviewsample.view.AlphaGridDrawHelper

class ImageEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private var cornerRadius = 0f

    private val clipPath = Path()
    private val clipRect = RectF()
    private var canvasSize = Size(1080, 1920)
    private val alphaGridDrawHelper by lazy {
        AlphaGridDrawHelper(
            gridSize = dp2Px(20),
            lightColor = ContextCompat.getColor(context, R.color.grid_light),
            darkColor = ContextCompat.getColor(context, R.color.grid_dark)
        )
    }

    init {
        handleAttributes(context, attrs)
    }

    private fun handleAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.ImageEditorView).apply {
            cornerRadius = getDimension(R.styleable.ImageEditorView_iev_cornerRadius, dp2Px(8))
            recycle()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateClipRect(w, h)
        // Log.w("sqsong", "onSizeChanged: $w, $h")
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // Log.e("sqsong", "onLayout: $l, $t, $r, $b")
    }

    override fun dispatchDraw(canvas: Canvas) {
        // Log.i("sqsong", "dispatchDraw: $clipRect")
        canvas.save()
        canvas.clipPath(clipPath)
        alphaGridDrawHelper.drawAlphaGrid(canvas, clipRect)
        canvas.restore()
        super.dispatchDraw(canvas)
    }

    private fun updateClipRect(width: Int, height: Int) {
        // 根据canvasSize比例来计算clipRect，按中心摆放
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
    }
}