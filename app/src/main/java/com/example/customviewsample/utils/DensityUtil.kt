package com.example.customviewsample.utils

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue

val screenWidth: Int
    get() = Resources.getSystem().displayMetrics.widthPixels

val screenHeight: Int
    get() = Resources.getSystem().displayMetrics.heightPixels

fun <T> dp2px(dpValue: Float): Int {
    val scale = Resources.getSystem().displayMetrics.density
    return (dpValue * scale + 0.5f).toInt()
}

inline fun <reified T> dp2Px(dpValue: Int): T {
    val result = Resources.getSystem().displayMetrics.density * dpValue + 0.5f
    return when (T::class) {
        Int::class -> result.toInt() as T
        Float::class -> result as T
        else -> throw IllegalStateException("Type not support.")
    }
}

inline fun <reified T> dp2Px(dpValue: Float): T {
    val result = Resources.getSystem().displayMetrics.density * dpValue + 0.5f
    return when (T::class) {
        Int::class -> result.toInt() as T
        Float::class -> result as T
        else -> throw IllegalStateException("Type not support.")
    }
}

/**
 * Value of px to value of dp.
 *
 * @param pxValue The value of px.
 * @return value of dp
 */
fun px2dp(pxValue: Float): Int {
    val scale = Resources.getSystem().displayMetrics.density
    return (pxValue / scale + 0.5f).toInt()
}

/**
 * Value of sp to value of px.
 *
 * @param spValue The value of sp.
 * @return value of px
 */
fun sp2px(spValue: Float): Int {
    val fontScale = Resources.getSystem().displayMetrics.scaledDensity
    return (spValue * fontScale + 0.5f).toInt()
}

/**
 * Value of px to value of sp.
 *
 * @param pxValue The value of px.
 * @return value of sp
 */
fun px2sp(pxValue: Float): Int {
    val fontScale = Resources.getSystem().displayMetrics.scaledDensity
    return (pxValue / fontScale + 0.5f).toInt()
}

fun getActionBarHeight(context: Context): Int {
    val typedValue = TypedValue()
    return if (context.theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
        return TypedValue.complexToDimensionPixelSize(typedValue.data, context.resources.displayMetrics)
    } else {
        0
    }
}

fun getStatusBarHeight(context: Context): Int {
    val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        context.resources.getDimensionPixelSize(resourceId)
    } else {
        0
    }
}