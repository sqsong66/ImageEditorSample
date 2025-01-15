package com.example.customviewsample.view

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnRepeat
import androidx.core.content.ContextCompat
import kotlin.math.min

class IndeterminateCircleProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 12f
        color = ContextCompat.getColor(context, android.R.color.holo_blue_light)
    }

    private val arcBounds = RectF()

    // 动画时长可以调整，看个人喜好，官方大约在 1~2秒范围内
    private val animationDuration = 1500L

    private var animator: ValueAnimator? = null

    // 每轮动画结束时，累积一点角度偏移，避免从同一位置开始
    private var ringRotationOffset = 0f

    // 记录当前这一小段弧的起止(0..1)，以及本轮的旋转值
    private var startTrim = 0f
    private var endTrim = 0f
    private var rotation = 0f

    init {
        setupAnimator()
    }

    /**
     * 配置无限循环的ValueAnimator
     */
    private fun setupAnimator() {
        val va = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = animationDuration
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART

            addListener(doOnRepeat {
                ringRotationOffset = (ringRotationOffset + 0f) % 360f
            })

            addUpdateListener { anim ->
                val fraction = anim.animatedValue as Float
                updateArc(fraction)
            }
        }
        animator = va
    }

    /**
     * 在 0..1 范围内，拆分多阶段来计算弧段 (startTrim, endTrim) 以及 rotation
     */
    private fun updateArc(fraction: Float) {
        // 一轮动画里，可以让圆转 1~2圈：这里示例1圈(360°)
        rotation = fraction * 360f

        // 我们把 [0..1] 划分成 4 段, 每段各 0.25
        // A(0..0.25)   B(0.25..0.50)   C(0.50..0.75)   D(0.75..1.0)
        // 你也可以改成别的比例(官方并非平均分，且更多阶段)
        when {
            fraction < 0.25f -> {
                // 阶段A：弧段从 0 -> 0.5(180°)
                val localFrac = (fraction - 0f) / 0.25f // map到 [0..1]
                startTrim = 0f
                endTrim = 0.5f * localFrac // 0 -> 0.5
            }
            fraction < 0.50f -> {
                // 阶段B：让“头尾”都往前运动 —— 让startTrim略微增长, endTrim继续增长
                val localFrac = (fraction - 0.25f) / 0.25f // map到 [0..1]
                // endTrim 从 0.5 -> 0.75
                val endBase = 0.5f
                val endMax = 0.75f
                endTrim = endBase + (endMax - endBase) * localFrac

                // startTrim 也从 0 -> 0.25
                val startMax = 0.25f
                startTrim = startMax * localFrac
            }
            fraction < 0.75f -> {
                // 阶段C：主要是“收缩”，让 endTrim 保持或收一点, startTrim 快速追赶
                val localFrac = (fraction - 0.50f) / 0.25f // [0..1]
                // endTrim 从 0.75 -> 0.9 (比如再增加一点点)
                val endBase = 0.75f
                val endMax = 0.9f
                endTrim = endBase + (endMax - endBase) * localFrac

                // startTrim 从 0.25 -> 0.8
                val startBase = 0.25f
                val startMax = 0.8f
                startTrim = startBase + (startMax - startBase) * localFrac
            }
            else -> {
                // 阶段D：把弧段收回到 0，以无缝进入下一轮
                val localFrac = (fraction - 0.75f) / 0.25f // [0..1]
                // endTrim 从 0.9 -> 1.0
                val endBase = 0.9f
                val endMax = 1f
                endTrim = endBase + (endMax - endBase) * localFrac

                // startTrim 从 0.8 -> 1.0 (相当于弧度缩到0)
                val startBase = 0.8f
                val startMax = 1f
                startTrim = startBase + (startMax - startBase) * localFrac
            }
        }

        // 对 startTrim 和 endTrim 做一次处理，避免 startTrim > endTrim 时画不出弧
        // 有些情况下(环绕)可能要加1
        if (startTrim > endTrim) {
            endTrim += 1f
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val widthF = width.toFloat()
        val heightF = height.toFloat()
        val diameter = min(widthF, heightF)
        val radius = diameter / 2f - paint.strokeWidth

        arcBounds.set(
            (widthF / 2f) - radius,
            (heightF / 2f) - radius,
            (widthF / 2f) + radius,
            (heightF / 2f) + radius
        )

        // 整体旋转(动画内旋转 + 偏移)
        canvas.save()
        val totalRotation = rotation + ringRotationOffset
        canvas.rotate(totalRotation, widthF / 2f, heightF / 2f)

        // 计算 sweepAngle
        val sweepAngle = (endTrim - startTrim) * 360f

        // 让 0度在 12点方向 => 减90度
        canvas.drawArc(arcBounds, (startTrim * 360f) - 90f, sweepAngle, false, paint)
        canvas.restore()
    }

    fun start() {
        animator?.takeIf { !it.isRunning }?.start()
    }

    fun stop() {
        animator?.cancel()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }

}