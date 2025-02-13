package com.sqsong.opengllib

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView

open class ScalableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatImageView(context, attrs) {
    private var lastX = 0f
    private var lastY = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> onTouchDown(event)

            MotionEvent.ACTION_POINTER_DOWN -> onPointerDown(event)

            MotionEvent.ACTION_MOVE -> onTouchMove(event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> onTouchUp(event)
        }
        return true
    }

    private fun onTouchDown(event: MotionEvent) {
        lastX = event.rawX
        lastY = event.rawY
    }

    private fun onPointerDown(event: MotionEvent) {

    }

    private fun onTouchMove(event: MotionEvent) {
        val dx = event.rawX - lastX
        val dy = event.rawY - lastY
        translationX += dx
        translationY += dy
        lastX = event.rawX
        lastY = event.rawY
    }

    private fun onTouchUp(event: MotionEvent) {

    }
}