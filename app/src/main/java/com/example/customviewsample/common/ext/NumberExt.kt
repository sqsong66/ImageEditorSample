package com.example.customviewsample.common.ext

import java.util.Locale

fun Float.keepTwoDecimal(): Float {
    return String.format(Locale.getDefault(), "%.2f", this).toFloat()
}