package com.example.customviewsample.common.ext

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.customviewsample.utils.getThemeColor
import com.example.customviewsample.utils.getThemeColorWithAlpha
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

fun EditText.showSoftKeyBoard(window: Window) {
    isFocusable = true
    isFocusableInTouchMode = true
    requestFocus()
    if (isSystemInsetsAnimationSupport(window)) {
        WindowInsetsControllerCompat(window, this).show(WindowInsetsCompat.Type.ime())
    } else {
        postDelayed({
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(this, 0)
        }, 300)
    }
}

internal fun View.isSystemInsetsAnimationSupport(window: Window): Boolean {
    val windowInsetsController = WindowInsetsControllerCompat(window, this)
    return windowInsetsController.systemBarsBehavior != 0
}

fun View.measureViewSize() {
    val wrapContentSpec = View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
    measure(wrapContentSpec, wrapContentSpec)
}

fun View.setMaterialShapeBackgroundDrawable(
    @CornerFamily cornerFamily: Int = CornerFamily.ROUNDED,
    allCornerSize: Float = 0f,
    topLeftCornerSize: Float = 0f,
    topRightCornerSize: Float = 0f,
    bottomLeftCornerSize: Float = 0f,
    bottomRightCornerSize: Float = 0f,
    backgroundColorResId: Int = com.google.android.material.R.attr.colorSurface,
    alpha: Int = 255
) {
    val shapeAppearanceModel = ShapeAppearanceModel.builder()
        .setTopLeftCorner(cornerFamily, if (allCornerSize != 0f) allCornerSize else topLeftCornerSize)
        .setTopRightCorner(cornerFamily, if (allCornerSize != 0f) allCornerSize else topRightCornerSize)
        .setBottomLeftCorner(cornerFamily, if (allCornerSize != 0f) allCornerSize else bottomLeftCornerSize)
        .setBottomRightCorner(cornerFamily, if (allCornerSize != 0f) allCornerSize else bottomRightCornerSize)
        .build()
    val materialShapeDrawable = MaterialShapeDrawable(shapeAppearanceModel).apply {
        setTint(getThemeColorWithAlpha(context, backgroundColorResId, alpha))
    }
    background = materialShapeDrawable
}


fun View.setRippleBackgroundColor(
    allCornerRadius: Float = 0f,
    topLeftCornerRadius: Float = 0f,
    topRightCornerRadius: Float = 0f,
    bottomLeftCornerRadius: Float = 0f,
    bottomRightCornerRadius: Float = 0f,
    backgroundColorResId: Int = Int.MIN_VALUE,
    alpha: Int = 255
) {
    val rippleColor = getThemeColor(context, com.google.android.material.R.attr.colorControlHighlight)
    val radii = if (allCornerRadius != 0f) {
        floatArrayOf(allCornerRadius, allCornerRadius, allCornerRadius, allCornerRadius, allCornerRadius, allCornerRadius, allCornerRadius, allCornerRadius)
    } else {
        floatArrayOf(topLeftCornerRadius, topLeftCornerRadius, topRightCornerRadius, topRightCornerRadius, bottomRightCornerRadius, bottomRightCornerRadius, bottomLeftCornerRadius, bottomLeftCornerRadius)
    }
    val maskDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadii = radii
        setColor(Color.BLACK)
    }
    val contentDrawable = if (backgroundColorResId == Int.MIN_VALUE) {
        null
    } else {
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = radii
            setColor(getThemeColorWithAlpha(context, backgroundColorResId, alpha))
        }
    }
    val rippleDrawable = RippleDrawable(ColorStateList.valueOf(rippleColor), contentDrawable, maskDrawable)
    background = rippleDrawable
}

fun View.setRippleBackgroundColor(
    allCornerRadius: Float = 0f,
    topLeftCornerRadius: Float = 0f,
    topRightCornerRadius: Float = 0f,
    bottomLeftCornerRadius: Float = 0f,
    bottomRightCornerRadius: Float = 0f,
    backgroundColor: Int = Int.MIN_VALUE,
) {
    val rippleColor = getThemeColor(context, com.google.android.material.R.attr.colorControlHighlight)
    val radii = if (allCornerRadius != 0f) {
        floatArrayOf(allCornerRadius, allCornerRadius, allCornerRadius, allCornerRadius, allCornerRadius, allCornerRadius, allCornerRadius, allCornerRadius)
    } else {
        floatArrayOf(topLeftCornerRadius, topLeftCornerRadius, topRightCornerRadius, topRightCornerRadius, bottomRightCornerRadius, bottomRightCornerRadius, bottomLeftCornerRadius, bottomLeftCornerRadius)
    }
    val maskDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadii = radii
        setColor(Color.BLACK)
    }
    val contentDrawable = if (backgroundColor == Int.MIN_VALUE) {
        null
    } else {
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = radii
            setColor(backgroundColor)
        }
    }
    val rippleDrawable = RippleDrawable(ColorStateList.valueOf(rippleColor), contentDrawable, maskDrawable)
    background = rippleDrawable
}

fun View.setRippleForeground(
    allCornerRadius: Float = 0f,
    topLeftCornerRadius: Float = 0f,
    topRightCornerRadius: Float = 0f,
    bottomLeftCornerRadius: Float = 0f,
    bottomRightCornerRadius: Float = 0f,
) {
    val rippleColor = getThemeColor(context, com.google.android.material.R.attr.colorControlHighlight)
    val radii = if (allCornerRadius != 0f) {
        floatArrayOf(allCornerRadius, allCornerRadius, allCornerRadius, allCornerRadius, allCornerRadius, allCornerRadius, allCornerRadius, allCornerRadius)
    } else {
        floatArrayOf(topLeftCornerRadius, topLeftCornerRadius, topRightCornerRadius, topRightCornerRadius, bottomRightCornerRadius, bottomRightCornerRadius, bottomLeftCornerRadius, bottomLeftCornerRadius)
    }
    val maskDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadii = radii
        setColor(Color.BLACK)
    }
    val rippleDrawable = RippleDrawable(ColorStateList.valueOf(rippleColor), null, maskDrawable)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        foreground = rippleDrawable
    }
}

fun TextView.setTopDrawable(drawableRes: Int) {
    setCompoundDrawablesWithIntrinsicBounds(0, drawableRes, 0, 0)
}