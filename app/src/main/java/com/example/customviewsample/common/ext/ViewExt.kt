package com.example.customviewsample.common.ext

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.util.Size
import android.view.LayoutInflater
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

fun createRippleDrawable(
    context: Context,
    backgroundColorResId: Int? = null, // 背景颜色资源 ID（可选）
    backgroundColorAlpha: Int = 0, // 背景颜色透明度
    allCornerRadius: Float = 0f, // 四个角的圆角半径
    topLeftCornerRadius: Float = 0f, // 左上角圆角
    topRightCornerRadius: Float = 0f, // 右上角圆角
    bottomLeftCornerRadius: Float = 0f, // 左下角圆角
    bottomRightCornerRadius: Float = 0f, // 右下角圆角
    borderWidth: Int = 0, // 边框宽度（可选）
    borderColorResId: Int? = null, // 边框颜色资源 ID（可选）
    borderColorAlpha: Int = 255, // 边框颜色透明度
): RippleDrawable {
    val rippleColor = getThemeColor(context, com.google.android.material.R.attr.colorControlHighlight)
    val radii = if (allCornerRadius > 0) {
        floatArrayOf(allCornerRadius, allCornerRadius, allCornerRadius, allCornerRadius, allCornerRadius, allCornerRadius, allCornerRadius, allCornerRadius)
    } else {
        floatArrayOf(topLeftCornerRadius, topLeftCornerRadius, topRightCornerRadius, topRightCornerRadius, bottomRightCornerRadius, bottomRightCornerRadius, bottomLeftCornerRadius, bottomLeftCornerRadius)
    }

    // Mask drawable: Defines the ripple bounds
    val maskDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadii = radii
        setColor(Color.BLACK) // 必须设置 Mask 的颜色，但具体颜色不会显示
    }

    // Content drawable: The visible background with optional border
    val contentDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadii = radii
        backgroundColorResId?.let {
            setColor(getThemeColorWithAlpha(context, backgroundColorResId, backgroundColorAlpha))
        }
        if (borderWidth > 0 && borderColorResId != null) {
            setStroke(borderWidth, getThemeColorWithAlpha(context, borderColorResId, borderColorAlpha))
        }
    }
    // Create RippleDrawable
    return RippleDrawable(
        ColorStateList.valueOf(rippleColor),
        contentDrawable, // Set as the content drawable
        maskDrawable // Set as the mask drawable
    )
}

fun View.setRippleBackgroundColor(
    allCornerRadius: Float = 0f,
    topLeftCornerRadius: Float = 0f,
    topRightCornerRadius: Float = 0f,
    bottomLeftCornerRadius: Float = 0f,
    bottomRightCornerRadius: Float = 0f,
    backgroundColorResId: Int = Int.MIN_VALUE,
    alpha: Int = 255,
    borderWidth: Int = 0, // 边框宽度
    borderColorResId: Int = Int.MIN_VALUE, // 边框颜色资源 ID
    borderAlpha: Int = 255, // 边框透明度
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
            if (borderWidth > 0 && borderColorResId != Int.MIN_VALUE) {
                setStroke(borderWidth, getThemeColorWithAlpha(context, borderColorResId, borderAlpha))
            }
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

fun View.layoutInflater(): LayoutInflater = LayoutInflater.from(context)

fun View.measuredSize(): Size {
    val width = width
    val height = height
    if (width != 0 && height != 0) {
        return Size(width, height)
    }
    val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.UNSPECIFIED)
    val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.UNSPECIFIED)
    measure(widthSpec, heightSpec)
    return Size(measuredWidth, measuredHeight)
}