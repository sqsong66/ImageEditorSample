package com.example.customviewsample.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.example.customviewsample.R
import com.example.customviewsample.utils.dp2Px
import com.example.customviewsample.utils.getThemeColor

class LayerPreviewImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var borderWidth: Float = 0f
    private var borderColor: Int = 0
    private var cornerRadius: Float = 0f
    private var text: String? = null
    private var textSize: Float = 0f
    private var textColor: Int = 0
    private var bgColor: IntArray? = null

    // 0-image, 1-background, 2-text
    private var layerType: Int = 0

    private val tempPath = Path()

    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
            color = borderColor
            typeface = Typeface.DEFAULT_BOLD
        }
    }

    private val bgPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            style = Paint.Style.FILL
        }
    }

    init {
        scaleType = ScaleType.CENTER_CROP
        handleAttributes(context, attrs)
    }

    private fun handleAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.LayerPreviewImageView).apply {
            borderWidth = getDimension(R.styleable.LayerPreviewImageView_lpiv_borderWidth, dp2Px(2))
            borderColor = getColor(R.styleable.LayerPreviewImageView_lpiv_borderColor, getThemeColor(context, com.google.android.material.R.attr.colorOnSurface))
            cornerRadius = getDimension(R.styleable.LayerPreviewImageView_lpiv_cornerRadius, dp2Px(8))
            text = getString(R.styleable.LayerPreviewImageView_lpiv_text) ?: "Aa"
            textSize = getDimension(R.styleable.LayerPreviewImageView_lpiv_textSize, dp2Px(16))
            textColor = getColor(R.styleable.LayerPreviewImageView_lpiv_textColor, borderColor)
            layerType = getInt(R.styleable.LayerPreviewImageView_lpiv_layerType, 0)
            recycle()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        tempPath.reset()
        tempPath.addRoundRect(0f, 0f, w.toFloat(), h.toFloat(), cornerRadius, cornerRadius, Path.Direction.CW)
        bgColor?.let { colorArray ->
            // 横向渐变
            bgPaint.shader = LinearGradient(0f, 0f, w.toFloat(), 0f, colorArray, null, Shader.TileMode.CLAMP)
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.clipPath(tempPath)
        when (layerType) {
            1 -> { // background
                bgColor?.let {
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
                } ?: run {
                    super.onDraw(canvas)
                }
            }

            2 -> { // text
                text?.let { text ->
                    paint.color = textColor
                    paint.style = Paint.Style.FILL
                    paint.textSize = textSize
                    val textWidth = paint.measureText(text)
                    val x = (width - textWidth) / 2
                    val y = height / 2f - (paint.descent() + paint.ascent()) / 2
                    canvas.drawText(text, x, y, paint)
                }
            }

            else -> { // image
                super.onDraw(canvas)
            }
        }

        if (borderWidth > 0) {
            paint.strokeWidth = borderWidth
            paint.color = borderColor
            paint.style = Paint.Style.STROKE
            canvas.drawPath(tempPath, paint)
        }
    }

    fun setLayerParams(layerType: Int, layerBitmap: Bitmap?, layerColor: IntArray?) {
        this.layerType = layerType
        this.bgColor = layerColor
        layerColor?.let {
            bgPaint.shader = LinearGradient(0f, 0f, width.toFloat(), 0f, it, null, Shader.TileMode.CLAMP)
        }
        setImageBitmap(layerBitmap)
        invalidate()
    }

}