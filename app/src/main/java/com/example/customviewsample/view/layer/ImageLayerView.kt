package com.example.customviewsample.view.layer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.example.customviewsample.utils.dp2Px

class ImageLayerView @JvmOverloads constructor(
    context: Context,
    private val cornerRadius: Float = 0f,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val paint by lazy {
        Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = dp2Px(2)
        }
    }
    private val path = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        path.reset()
        path.addRoundRect(0f, 0f, w.toFloat(), h.toFloat(), cornerRadius, cornerRadius, Path.Direction.CW)
    }

    override fun layout(l: Int, t: Int, r: Int, b: Int) {
        super.layout(l, t, r, b)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.clipPath(path)
        super.onDraw(canvas)
        canvas.restore()
        canvas.drawPath(path, paint)
    }

}