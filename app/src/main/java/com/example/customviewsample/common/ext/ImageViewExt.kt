package com.example.customviewsample.common.ext

import android.content.res.ColorStateList
import androidx.appcompat.widget.AppCompatImageView
import com.example.customviewsample.utils.getThemeColorWithAlpha

fun AppCompatImageView.setEnableState(enable: Boolean) {
    isEnabled = enable
    val tintColor = getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorOnSurface, if (enable) 255 else 100)
    imageTintList = ColorStateList.valueOf(tintColor)
}