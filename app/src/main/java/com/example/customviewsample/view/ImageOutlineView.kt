package com.example.customviewsample.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.view.doOnLayout
import com.example.customviewsample.utils.dp2Px
import com.example.customviewsample.utils.matrixScale
import com.example.customviewsample.utils.matrixTranslatePair
import com.google.android.renderscript.Toolkit
import com.sqsong.nativelib.NativeLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ImageOutlineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr), CoroutineScope by MainScope() {

    private val maxBlurRadius = 25f
    private val maxOutlineSize = 500
    private val maxStrokeWidth = dp2Px<Float>(30)

    private var outlineStokeWidth = 0f
    private var outlineBlurRadius = 0
    private var imageBitmap: Bitmap? = null
    private var outlineBitmap: Bitmap? = null
    private val outlineBitmapPadding = (maxStrokeWidth + maxBlurRadius).toInt()

    private var outlinePath: Path? = null
    private var srcOutlineBitmap: Bitmap? = null
    private var outlineCanvas: Canvas? = null

    private val pathMatrix: Matrix = Matrix()
    private val imageMatrix: Matrix = Matrix()
    private val outlineMatrix: Matrix = Matrix()

    private val imagePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            isFilterBitmap = true
        }
    }

    private val outlinePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            isFilterBitmap = true
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
            strokeWidth = outlineStokeWidth
            color = Color.WHITE
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        outlineBitmap?.let {
            canvas.drawBitmap(it, outlineMatrix, imagePaint)
        }
        imageBitmap?.let {
            canvas.drawBitmap(it, imageMatrix, imagePaint)
        }
    }

    fun setImageBitmap(bitmap: Bitmap) {
        imageBitmap = bitmap
        outlinePath = null
        srcOutlineBitmap = null
        if (width == 0 || height == 0) {
            doOnLayout {
                calculateMatrix(bitmap)
                prepareOutline(bitmap)
                invalidate()
            }
        } else {
            calculateMatrix(bitmap)
            prepareOutline(bitmap)
            invalidate()
        }
    }

    private fun prepareOutline(bitmap: Bitmap) {
        if (outlineStokeWidth <= 0) {
            outlineBitmap = null
            return
        }
        flow {
            val start = System.currentTimeMillis()
            val b = prepareOutlineAsync(bitmap)
            Log.w("songmao", "prepareOutline cost: ${System.currentTimeMillis() - start}ms, bitmap size: ${b.width}x${b.height}")
            emit(b)
        }.onEach {
            outlineBitmap = it
            calculateOutlineMatrix(bitmap, it)
            invalidate()
        }.launchIn(this)
    }

    /**
     * 根据原图进行一定比例的缩放，然后再加上一定的描边宽度和模糊半径来生成一张描边扩展图
     */
    private fun buildOutlineBitmap(srcBitmap: Bitmap): Bitmap {
        val (outlineWidth, outlineHeight) = if (srcBitmap.width > srcBitmap.height) {
            if (srcBitmap.width > maxOutlineSize) {
                maxOutlineSize to (srcBitmap.height * maxOutlineSize / srcBitmap.width)
            } else {
                srcBitmap.width to srcBitmap.height
            }
        } else {
            if (srcBitmap.height > maxOutlineSize) {
                (srcBitmap.width * maxOutlineSize / srcBitmap.height) to maxOutlineSize
            } else {
                srcBitmap.width to srcBitmap.height
            }
        }
        val finalOutlineW = outlineWidth + 2 * outlineBitmapPadding
        val finalOutlineH = outlineHeight + 2 * outlineBitmapPadding
        return Bitmap.createBitmap(finalOutlineW, finalOutlineH, Bitmap.Config.ARGB_8888)
    }

    private fun prepareOutlineAsync(bitmap: Bitmap): Bitmap {
        // 根据原图生成一张进行了扩展的描边图
        val b = srcOutlineBitmap ?: buildOutlineBitmap(bitmap).apply { srcOutlineBitmap = this }
        val (outlineWidth, outlineHeight) = (b.width - 2 * outlineBitmapPadding) to (b.height - 2 * outlineBitmapPadding)
        val path = outlinePath ?: NativeLib.getBitmapOutlinePath(bitmap).apply { outlinePath = this }
        val sx = outlineWidth.toFloat() / bitmap.width
        val sy = outlineHeight.toFloat() / bitmap.height
        // 根据原图及扩展图的扩展大小和比例来将描边绘制到扩展图上
        pathMatrix.setScale(sx, sy)
        pathMatrix.postTranslate(outlineBitmapPadding.toFloat(), outlineBitmapPadding.toFloat())
        val scaledOutlinePath = Path(path).apply { transform(pathMatrix) }
        outlineCanvas = Canvas(b).apply {
            drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            outlinePaint.strokeWidth = outlineStokeWidth
            drawPath(scaledOutlinePath, outlinePaint)
        }
        // 对描边图进行高斯模糊处理(如必要)
        return if (outlineBlurRadius > 0) {
            Toolkit.blur(b, outlineBlurRadius)
        } else {
            b
        }
    }

    private fun calculateMatrix(bitmap: Bitmap) {
        imageMatrix.reset()
        val scaleFactor = 0.6f
        val viewRatio = width.toFloat() / height
        val bitmapRatio = bitmap.width.toFloat() / bitmap.height
        val scale = if (viewRatio > bitmapRatio) {
            (width.toFloat() * scaleFactor) / bitmap.width
        } else {
            (height.toFloat() * scaleFactor) / bitmap.height
        }
        val translateX = (width - bitmap.width * scale) / 2
        val translateY = (height - bitmap.height * scale) / 2
        imageMatrix.postScale(scale, scale)
        imageMatrix.postTranslate(translateX, translateY)
    }

    fun setOutlineStrokeWidth(percent: Float) {
        outlineStokeWidth = maxStrokeWidth * percent
        imageBitmap?.let {
            prepareOutline(it)
        }
    }

    fun setOutlineBlurRadius(percent: Float) {
        outlineBlurRadius = (maxBlurRadius * percent).toInt()
        imageBitmap?.let {
            prepareOutline(it)
        }
    }

    private fun calculateOutlineMatrix(bitmap: Bitmap, outlineBitmap: Bitmap) {
        // 获取原始图片展示的缩放比
        val scale = imageMatrix.matrixScale()
        val finalImageW = bitmap.width * scale
        val finalImageH = bitmap.height * scale
        // 扩展前的图片大小与原始图片大小来计算描边图的缩放比
        val (outlineWidth, outlineHeight) = (outlineBitmap.width - 2 * outlineBitmapPadding) to (outlineBitmap.height - 2 * outlineBitmapPadding)
        val outlineScaleX = finalImageW / outlineWidth
        val outlineScaleY = finalImageH / outlineHeight
        outlineMatrix.reset()
        outlineMatrix.postScale(outlineScaleX, outlineScaleY)
        val (tx, ty) = imageMatrix.matrixTranslatePair()
        // 先根据原始图片的平移量来对描边图进行平移
        outlineMatrix.postTranslate(tx, ty)
        // 再根据描边图的扩展大小和比例来对描边图进行平移,让其与原图进行贴合
        outlineMatrix.postTranslate(-outlineBitmapPadding * outlineScaleX, -outlineBitmapPadding * outlineScaleY)
    }
}