package com.example.customviewsample.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import androidx.annotation.IntDef
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.animation.addListener
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withSave
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import com.example.customviewsample.R
import com.example.customviewsample.common.ext.measureViewSize
import com.example.customviewsample.common.ext.setRippleForeground
import com.example.customviewsample.utils.dp2Px
import com.example.customviewsample.utils.getThemeColorWithAlpha

@IntDef(
    EditorSaveState.SAVE_NONE,
    EditorSaveState.SAVE_START,
    EditorSaveState.SAVE_DONE,
    EditorSaveState.SAVE_COLLAPSE
)
@Retention(AnnotationRetention.SOURCE)
annotation class EditorSaveState {
    companion object {
        const val SAVE_NONE = 0
        const val SAVE_START = 1
        const val SAVE_DONE = 2
        const val SAVE_COLLAPSE = 3
    }
}

class EditorSaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private var borderWidth = 0f
    private var borderColor = 0
    private var cornerRadius = 0f
    private var saveText = ""
    private var textSize = 0f
    private var textColor = 0
    private var saveIcon = 0
    private var galleryText = ""
    private var settingText = ""
    private var settingIcon = 0
    private var arrowIcon = 0
    private var iconTint = 0
    private var hPadding = 0f
    private var vPadding = 0f
    private var isAutoSaveMode = false
    private var progressMaskColor = 0
    private var progressMaskDuration = 0L
    private var iconSize = 0f
    private var doneSize = 0f
    private var doneIconDrawable: Drawable? = null
    private var progressDoneColor = 0
    private var progressDoneDuration = 0L

    private val iconMargin = dp2Px<Int>(4)
    private val tempRect = RectF()
    private val tempPath = Path()
    private val maskProgressRect = RectF()
    private val doneProgressRect = RectF()
    private var valueAnimator: ValueAnimator? = null
    private var initialWidth = 0
    private var destWidth = 0

    @EditorSaveState
    private var saveState = EditorSaveState.SAVE_NONE

    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
            color = borderColor
        }
    }

    init {
        orientation = HORIZONTAL
        handleAttribute(context, attrs)
        setPadding(hPadding.toInt(), vPadding.toInt(), 0, vPadding.toInt())
        gravity = if (isAutoSaveMode) Gravity.CENTER else Gravity.CENTER_VERTICAL
        setRippleForeground(allCornerRadius = cornerRadius)
        initLayout()
    }

    private fun handleAttribute(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.EditorSaveView).apply {
            borderWidth = getDimension(R.styleable.EditorSaveView_esv_borderWidth, dp2Px(1))
            borderColor = getColor(R.styleable.EditorSaveView_esv_borderColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorOnSurface, 100))
            cornerRadius = getDimension(R.styleable.EditorSaveView_esv_cornerRadius, dp2Px(8))
            saveText = getString(R.styleable.EditorSaveView_esv_saveText) ?: "Saving to your device..."
            textSize = getDimension(R.styleable.EditorSaveView_esv_textSize, dp2Px(14))
            textColor = getColor(R.styleable.EditorSaveView_esv_textColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorOnSurface, 255))
            saveIcon = getResourceId(R.styleable.EditorSaveView_esv_saveIcon, R.drawable.ic_download)
            galleryText = getString(R.styleable.EditorSaveView_esv_galleryText) ?: "Gallery"
            settingText = getString(R.styleable.EditorSaveView_esv_settingText) ?: "Settings"
            settingIcon = getResourceId(R.styleable.EditorSaveView_esv_settingIcon, R.drawable.ic_settings)
            arrowIcon = getResourceId(R.styleable.EditorSaveView_esv_arrowIcon, R.drawable.ic_right_arrow1)
            iconTint = getColor(R.styleable.EditorSaveView_esv_iconTint, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorOnSurface, 255))
            hPadding = getDimension(R.styleable.EditorSaveView_esv_hPadding, dp2Px(12))
            vPadding = getDimension(R.styleable.EditorSaveView_esv_vPadding, dp2Px(8))
            isAutoSaveMode = getBoolean(R.styleable.EditorSaveView_esv_isAutoSaveMode, true)
            progressMaskColor = getColor(R.styleable.EditorSaveView_esv_progressMaskColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorPrimary, 100))
            progressMaskDuration = getInteger(R.styleable.EditorSaveView_esv_progressMaskDuration, 1000).toLong()
            iconSize = getDimension(R.styleable.EditorSaveView_esv_iconSize, dp2Px(18))
            doneSize = getDimension(R.styleable.EditorSaveView_esv_doneSize, dp2Px(24))
            val doneIcon = getResourceId(R.styleable.EditorSaveView_esv_doneIcon, R.drawable.ic_check_circle)
            if (doneIcon != 0) {
                doneIconDrawable = AppCompatResources.getDrawable(context, doneIcon)?.apply {
                    setTint(iconTint)
                }
            }
            progressDoneColor = getColor(R.styleable.EditorSaveView_esv_progressDoneColor, "#5cc98e".toColorInt())
            progressDoneDuration = getInteger(R.styleable.EditorSaveView_esv_progressDoneDuration, 400).toLong()
            recycle()
        }
    }

    private fun addSaveLayout(saveText: String) {
        val saveIcon = AppCompatImageView(context).apply {
            setImageResource(saveIcon)
            setColorFilter(iconTint)
            layoutParams = LayoutParams(iconSize.toInt(), iconSize.toInt()).apply {
                topMargin = dp2Px(2)
                bottomMargin = dp2Px(2)
            }
        }
        addView(saveIcon)
        val saveTextView = AppCompatTextView(context).apply {
            text = saveText
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = iconMargin
            }
            tag = "save_text"
        }
        addView(saveTextView)
    }

    private fun addSettingLayout() {
        val settingIcon = AppCompatImageView(context).apply {
            setImageResource(settingIcon)
            setColorFilter(iconTint)
            layoutParams = LayoutParams(iconSize.toInt(), iconSize.toInt()).apply {
                topMargin = dp2Px(2)
                bottomMargin = dp2Px(2)
            }
        }
        addView(settingIcon)
        val settingTextView = AppCompatTextView(context).apply {
            text = settingText
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = iconMargin
                bottomMargin = dp2Px(1)
            }
            tag = "setting_text"
        }
        addView(settingTextView)
        val arrowIcon = AppCompatImageView(context).apply {
            setImageResource(arrowIcon)
            setColorFilter(iconTint)
            layoutParams = LayoutParams(iconSize.toInt(), iconSize.toInt()).apply {
                marginStart = iconMargin
            }
        }
        addView(arrowIcon)
    }

    private fun addGapLine() {
        val gapLine = View(context).apply {
            setBackgroundColor(borderColor)
            layoutParams = LayoutParams((borderWidth / 2).toInt(), LayoutParams.MATCH_PARENT).apply {
                marginStart = dp2Px(8)
                marginEnd = dp2Px(8)
            }
        }
        addView(gapLine)
    }

    private fun calculateDestWidth(): Int {
        var width = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.isVisible) {
                child.measureViewSize()
                width += (child.measuredWidth + child.marginStart + child.marginEnd)
            }
        }
        return width + paddingStart + paddingEnd
    }

    private fun initLayout() {
        removeAllViews()
        if (isAutoSaveMode) {
            addSaveLayout(saveText)
        } else {
            addSettingLayout()
            destWidth = calculateDestWidth()
            Log.d("sqsong", "initLayout destWidth: $destWidth")
        }
        doOnLayout {
            initialWidth = width
        }
    }

    private fun initFullLayout() {
        removeAllViews()
        addSaveLayout(galleryText)
        addGapLine()
        addSettingLayout()
    }

    private fun initSettingLayout() {
        removeAllViews()
        addSettingLayout()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        valueAnimator?.cancel()
    }

    override fun dispatchDraw(canvas: Canvas) {
        tempRect.set(0f, 0f, width.toFloat(), height.toFloat())
        tempRect.inset(borderWidth / 2, borderWidth / 2)
        tempPath.reset()
        tempPath.addRoundRect(tempRect, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.clipPath(tempPath)
        // draw border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = borderWidth
        paint.color = borderColor
        canvas.drawRoundRect(tempRect, cornerRadius, cornerRadius, paint)
        super.dispatchDraw(canvas)

        // draw progress mask
        canvas.withSave {
            when (saveState) {
                EditorSaveState.SAVE_START -> {
                    paint.style = Paint.Style.FILL
                    paint.color = progressMaskColor
                    drawRect(maskProgressRect, paint)
                }

                EditorSaveState.SAVE_DONE -> {
                    // 先绘制mask progress
                    paint.style = Paint.Style.FILL
                    paint.color = progressMaskColor
                    drawRect(maskProgressRect, paint)
                    // 再绘制done progress

                    canvas.withSave {
                        canvas.clipRect(doneProgressRect)
                        paint.style = Paint.Style.FILL
                        paint.color = progressDoneColor
                        drawRect(doneProgressRect, paint)

                        doneIconDrawable?.let { drawable ->
                            drawable.setBounds(
                                (width - doneSize.toInt()) / 2,
                                (height - doneSize.toInt()) / 2,
                                (width + doneSize.toInt()) / 2,
                                (height + doneSize.toInt()) / 2
                            )
                            drawable.draw(canvas)
                        }
                    }
                }

                EditorSaveState.SAVE_COLLAPSE -> {
                    canvas.withSave {
                        canvas.clipRect(doneProgressRect)
                        paint.style = Paint.Style.FILL
                        paint.color = progressDoneColor
                        drawRect(doneProgressRect, paint)

                        doneIconDrawable?.let { drawable ->
                            drawable.setBounds(
                                (width - doneSize.toInt()) / 2,
                                (height - doneSize.toInt()) / 2,
                                (width + doneSize.toInt()) / 2,
                                (height + doneSize.toInt()) / 2
                            )
                            drawable.draw(canvas)
                        }
                    }
                }
            }
        }
    }

    fun startAnimation() {
        doOnLayout {
            if (isAutoSaveMode) {
                startAutoSaveAnimation()
            } else {
                startSettingAnimation()
            }
        }
    }

    private fun startSettingAnimation() {
        valueAnimator?.cancel()
        saveState = EditorSaveState.SAVE_NONE
        valueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 800
            interpolator = AccelerateInterpolator()
            addUpdateListener {
                val animatedValue = it.animatedValue as Float
                val newWidth = destWidth + ((initialWidth - destWidth) * (1 - animatedValue)).toInt()
                layoutParams.width = newWidth
                requestLayout()
            }
            addListener(onStart = {
                initSettingLayout()
                destWidth = calculateDestWidth()
                Log.d("sqsong", "startSettingAnimation destWidth: $destWidth")
            })
            start()
        }
    }

    private fun startAutoSaveAnimation() {
        valueAnimator?.cancel()
        saveState = EditorSaveState.SAVE_START
        valueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = progressMaskDuration
            interpolator = LinearInterpolator()
            addUpdateListener {
                val animatedValue = it.animatedValue as Float
                maskProgressRect.set(0f, 0f, width * animatedValue, height.toFloat())
                invalidate()
            }
            addListener(onEnd = {
                startDoneAnimation()
            })
            start()
        }
    }

    private fun startDoneAnimation() {
        valueAnimator?.cancel()
        saveState = EditorSaveState.SAVE_DONE
        valueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = progressDoneDuration
            interpolator = AccelerateInterpolator()
            addUpdateListener {
                val animatedValue = it.animatedValue as Float
                doneProgressRect.set(0f, 0f, width * animatedValue, height.toFloat())
                invalidate()
            }
            addListener(onEnd = {
                initFullLayout()
                destWidth = calculateDestWidth()
                Log.d("sqsong", "startDoneAnimation destWidth: $destWidth")
                startCollapseAnimation(destWidth)
            })
            start()
        }
    }

    private fun startCollapseAnimation(destWidth: Int) {
        valueAnimator?.cancel()
        saveState = EditorSaveState.SAVE_COLLAPSE
        valueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = progressDoneDuration
            interpolator = LinearInterpolator()
            addUpdateListener {
                val animatedValue = it.animatedValue as Float
                val newWidth = destWidth + ((initialWidth - destWidth) * (1 - animatedValue)).toInt()
                layoutParams.width = newWidth
                requestLayout()
                doneProgressRect.set(width * animatedValue, 0f, width.toFloat(), height.toFloat())
                invalidate()
            }
            addListener(onEnd = {
                saveState = EditorSaveState.SAVE_NONE

            })
            start()
        }
    }

    fun resetViewSize() {
        layoutParams = layoutParams.apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
        }
        requestLayout()
    }

    fun toggleSaveMode() {
        this.isAutoSaveMode = !this.isAutoSaveMode
        resetViewSize()
        startAnimation()
    }
}