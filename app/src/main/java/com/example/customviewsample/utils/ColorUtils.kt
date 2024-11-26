package com.example.customviewsample.utils

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.annotation.StyleableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.MaterialColors

fun getThemeColorWithAlpha(context: Context?, resId: Int, alpha: Int = 255): Int {
    if (context == null) return Color.TRANSPARENT
    val color = MaterialColors.getColor(context, resId, Color.TRANSPARENT)
    return ColorUtils.setAlphaComponent(color, alpha)
}

fun getThemeColor(context: Context?, resId: Int): Int {
    if (context == null) return Color.TRANSPARENT
    return MaterialColors.getColor(context, resId, Color.TRANSPARENT)
}

fun getSurfaceGradientDrawable(context: Context): GradientDrawable {
    val startColor = getThemeColor(context, com.google.android.material.R.attr.colorSurface)
    val middleColor = getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorSurface, (255 * 0.8f).toInt())
    val endColor = Color.TRANSPARENT
    return GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(startColor, middleColor, endColor))
}

fun getGradientDrawable(startColor: Int, endColor: Int): GradientDrawable {
    return GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(startColor, endColor))
}

fun intColorToHexString(color: Int): String {
    return String.format("#%08X", color)
}

fun getColorStateList(context: Context, attributes: TypedArray, @StyleableRes index: Int): ColorStateList? {
    if (attributes.hasValue(index)) {
        val resourceId = attributes.getResourceId(index, 0)
        if (resourceId != 0) {
            val value = AppCompatResources.getColorStateList(context, resourceId)
            if (value != null) {
                return value
            }
        }
    }
    return attributes.getColorStateList(index)
}