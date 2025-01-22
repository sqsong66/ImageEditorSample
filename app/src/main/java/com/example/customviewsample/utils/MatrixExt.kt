package com.example.customviewsample.utils

import android.graphics.Matrix
import kotlin.math.atan2
import kotlin.math.sqrt

fun Matrix.matrixRotateDegree(): Float {
    return FloatArray(9).run {
        getValues(this)
        (atan2(this[Matrix.MSKEW_X], this[Matrix.MSCALE_X]) * (180f / Math.PI)).toFloat()
    }
}

fun Matrix.matrixScale(): Float {
    return FloatArray(9).run {
        getValues(this)
        val scaleX = this[Matrix.MSCALE_X]
        val skewY = this[Matrix.MSKEW_Y]
        sqrt(scaleX * scaleX + skewY * skewY)
    }
}

fun Matrix.matrixTranslate(): FloatArray {
    return FloatArray(9).run {
        getValues(this)
        floatArrayOf(this[Matrix.MTRANS_X], this[Matrix.MTRANS_Y])
    }
}

fun Matrix.matrixTranslatePair(): Pair<Float, Float> {
    return FloatArray(9).run {
        getValues(this)
        Pair(this[Matrix.MTRANS_X], this[Matrix.MTRANS_Y])
    }
}

fun Matrix.scaleValue(): FloatArray {
    val values = FloatArray(9)
    getValues(values)
    return floatArrayOf(values[Matrix.MSCALE_X], values[Matrix.MSCALE_Y])
}