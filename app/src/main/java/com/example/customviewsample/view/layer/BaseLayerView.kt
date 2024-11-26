package com.example.customviewsample.view.layer

import android.content.Context
import android.util.AttributeSet
import android.view.View

class BaseLayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    fun translate(dx: Float, dy: Float) {
        translationX += dx
        translationY += dy
    }

}