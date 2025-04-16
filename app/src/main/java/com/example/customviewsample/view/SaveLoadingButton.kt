package com.example.customviewsample.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.doOnLayout
import com.example.customviewsample.R
import com.example.customviewsample.common.ext.setRippleForeground
import com.example.customviewsample.utils.dp2Px
import com.example.customviewsample.utils.getThemeColor
import com.example.customviewsample.utils.getThemeColorWithAlpha

annotation class SaveLoadingState {
    companion object {
        const val STATE_SAVING = 0
        const val STATE_DONE = 1
    }
}

class SaveLoadingButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private var borderWidth = 0f
    private var borderColor = 0
    private var bgColor = 0
    private var cornerRadius = 0f
    private var startIcon = 0
    private var startIconTint = 0
    private var iconSize = 0f
    private var iconMargin = 0f
    private var saveText = ""
    private var textSize = 0f
    private var textColor = 0
    private var shimmerColor = 0
    private var shimmerDuration = 0L
    private var shimmerWidth = 0f
    private var saveState = SaveLoadingState.STATE_SAVING
    private var hPadding = 0f
    private var vPadding = 0f

    private val tempRect = RectF()
    private val tempPath = Path()
    private val shimmerRect = RectF()
    private var valueAnimator: ValueAnimator? = null

    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
            color = borderColor
        }
    }

    private val shimmerPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    }

    init {
        orientation = HORIZONTAL
        handleAttributes(context, attrs)
        setPadding(hPadding.toInt(), vPadding.toInt(), hPadding.toInt(), vPadding.toInt())
        setRippleForeground(allCornerRadius = cornerRadius)
        initLayout()
    }

    private fun handleAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.SaveLoadingButton).apply {
            borderWidth = getDimension(R.styleable.SaveLoadingButton_slb_borderWidth, 0f)
            borderColor = getColor(R.styleable.SaveLoadingButton_slb_borderColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorOnSurface, 100))
            bgColor = getColor(R.styleable.SaveLoadingButton_slb_bgColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorSurfaceContainer, 255))
            cornerRadius = getDimension(R.styleable.SaveLoadingButton_slb_cornerRadius, dp2Px(8))
            startIcon = getResourceId(R.styleable.SaveLoadingButton_slb_startIcon, R.drawable.ic_download)
            startIconTint = getColor(R.styleable.SaveLoadingButton_slb_startIconTint, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorOnSurface, 255))
            iconSize = getDimension(R.styleable.SaveLoadingButton_slb_iconSize, dp2Px(20))
            iconMargin = getDimension(R.styleable.SaveLoadingButton_slb_iconMargin, dp2Px(8))
            saveText = getString(R.styleable.SaveLoadingButton_slb_saveText) ?: "Save to Gallery"
            textSize = getDimension(R.styleable.SaveLoadingButton_slb_textSize, dp2Px(16))
            textColor = getColor(R.styleable.SaveLoadingButton_slb_textColor, getThemeColor(context, com.google.android.material.R.attr.colorOnSurface))
            shimmerColor = getColor(R.styleable.SaveLoadingButton_slb_shimmerColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorOnSurface, 50))
            shimmerDuration = getInt(R.styleable.SaveLoadingButton_slb_shimmerDuration, 1000).toLong()
            shimmerWidth = getDimension(R.styleable.SaveLoadingButton_slb_shimmerWidth, dp2Px(120))
            saveState = getInt(R.styleable.SaveLoadingButton_slb_saveState, SaveLoadingState.STATE_SAVING)
            hPadding = getDimension(R.styleable.SaveLoadingButton_slb_hPadding, dp2Px(16))
            vPadding = getDimension(R.styleable.SaveLoadingButton_slb_vPadding, dp2Px(12))
            recycle()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        valueAnimator?.cancel()
    }

    private fun initLayout() {
        gravity = Gravity.CENTER_VERTICAL
        if (startIcon != 0) {
            val iconView = AppCompatImageView(context).apply {
                setImageResource(startIcon)
                setColorFilter(startIconTint, android.graphics.PorterDuff.Mode.SRC_IN)
                layoutParams = LayoutParams(iconSize.toInt(), iconSize.toInt())
            }
            addView(iconView, 0)
        }
        val textView = AppCompatTextView(context).apply {
            text = saveText
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = if (startIcon != 0) iconMargin.toInt() else 0
            }
            typeface = Typeface.DEFAULT_BOLD
        }
        addView(textView)
        isEnabled = saveState != SaveLoadingState.STATE_SAVING
        setChildEnableState()
        if (saveState == SaveLoadingState.STATE_SAVING) {
            doOnLayout {
                startShimmerAnimation()
            }
        }
    }

    private fun setChildEnableState() {
        for (i in 0 until childCount) {
            val alpha = if (isEnabled) 1f else 0.8f
            getChildAt(i).alpha = alpha
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        tempRect.set(0f, 0f, width.toFloat(), height.toFloat())
        tempRect.inset(borderWidth / 2, borderWidth / 2)
        tempPath.reset()
        tempPath.addRoundRect(tempRect, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.clipPath(tempPath)
        paint.style = Paint.Style.FILL
        paint.color = bgColor
        canvas.drawRoundRect(tempRect, cornerRadius, cornerRadius, paint)

        if (borderWidth > 0) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = borderWidth
            paint.color = borderColor
            canvas.drawRoundRect(tempRect, cornerRadius, cornerRadius, paint)
        }
        super.dispatchDraw(canvas)

        if (!shimmerRect.isEmpty) {
            canvas.drawRect(shimmerRect, shimmerPaint)
        }
    }

    private fun startShimmerAnimation() {
        valueAnimator?.cancel()
        valueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = shimmerDuration
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val animatedValue = it.animatedValue as Float
                shimmerRect.set(-shimmerWidth + (width + shimmerWidth) * animatedValue, 0f, 
                    shimmerWidth + (width + shimmerWidth) * animatedValue, height.toFloat())
                shimmerPaint.shader = LinearGradient(
                    shimmerRect.left, 0f, shimmerRect.right, 0f,
                    intArrayOf(Color.TRANSPARENT, shimmerColor, Color.TRANSPARENT),
                    floatArrayOf(0f, 0.5f, 1f),
                    android.graphics.Shader.TileMode.CLAMP
                )
                invalidate()
            }
            start()
        }
    }

}