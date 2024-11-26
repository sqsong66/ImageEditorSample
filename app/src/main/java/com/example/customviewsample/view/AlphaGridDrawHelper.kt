package com.example.customviewsample.view

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.customviewsample.utils.dp2Px
import kotlin.math.ceil

class AlphaGridDrawHelper(
    private val gridSize: Int = dp2Px<Int>(12),
    private val lightColor: Int = Color.WHITE,
    private val darkColor: Int = Color.parseColor("#FFE9E9EB"),
) {

    private val gridPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
        }
    }

    fun drawAlphaGrid(canvas: Canvas, rect: RectF) {
        val horizontalCount = ceil(rect.width() / gridSize).toInt()
        val verticalCount = ceil(rect.height() / gridSize).toInt()
        var verticalStartWhite = true
        for (i in 0..verticalCount) {
            var isWhite = verticalStartWhite
            for (j in 0..horizontalCount) {
                gridPaint.color = if (isWhite) lightColor else darkColor
                canvas.drawRect(
                    rect.left + j * gridSize,
                    rect.top + i * gridSize,
                    rect.left + (j + 1) * gridSize,
                    rect.top + (i + 1) * gridSize,
                    gridPaint
                )
                isWhite = !isWhite
            }
            verticalStartWhite = !verticalStartWhite
        }
    }

}