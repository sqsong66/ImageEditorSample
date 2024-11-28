package com.example.customviewsample.common.ext

import android.graphics.RectF

fun RectF.isSameRect(rect: RectF): Boolean {
    return left != rect.left || top != rect.top || right != rect.right || bottom != rect.bottom
}