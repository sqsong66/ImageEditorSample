package com.example.customviewsample.view.layer

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.widget.AppCompatImageView
import com.example.customviewsample.utils.dp2Px

class ImageLayerView @JvmOverloads constructor(
    context: Context,
    private val cornerRadius: Float = 0f,
    override var isSelectedLayer: Boolean = false,
    override val absLayerType: Int = LayerType.LAYER_IMAGE,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr), AbsLayer {

    companion object {
        const val SCALE_FACTOR = 0.8f
    }

    private val path = Path()
    private val pathRect = RectF()
    private var isSaveMode = false
    private val resizeRect = RectF()
    private val tempMatrix = Matrix()
    private val centerPoint = PointF()
    private val layoutInfo = LayoutInfo()
    private var tempCenterPoint = PointF()
    private val borderWidth = dp2Px<Float>(2)
    private var tempSize = Size(0, 0)
    private var resizeSize = Size(0, 0)
    private var layerCacheInfo = LayerTempCacheInfo()

    private val paint by lazy {
        Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = borderWidth
        }
    }

    private val testPaint by lazy {
        Paint().apply {
            color = Color.GREEN
            style = Paint.Style.FILL
        }
    }

    override val absCenterPoint: PointF
        get() = centerPoint

    override val absLayoutInfo: LayoutInfo
        get() = layoutInfo

    override fun translate(dx: Float, dy: Float) {
        translationX += dx
        translationY += dy
    }

    override fun updateLayoutInfo(clipRect: RectF) {
        updateChildLayoutInfo(layoutInfo, clipRect, this)
    }

    override fun stagingResizeInfo(clipRect: RectF, updateLayoutInfo: Boolean) {
        if (updateLayoutInfo) updateLayoutInfo(clipRect)
        resizeRect.set(clipRect)
        // update center point
        val cx = (left + right) / 2f + translationX
        val cy = (top + bottom) / 2f + translationY
        centerPoint.set(cx, cy)
        resizeSize = Size(right - left, bottom - top)
    }

    override fun tempStagingSize() {
        tempSize = Size(right - left, bottom - top)
        tempCenterPoint.set((left + right) / 2f + translationX, (top + bottom) / 2f + translationY)
    }

    override fun transformLayerByResize(clipRect: RectF, destScale: Float, factor: Float) {
        // 计算变换大小
        val diffWidth = resizeSize.width * destScale - tempSize.width
        val diffHeight = resizeSize.height * destScale - tempSize.height
        val newWidth = tempSize.width + diffWidth * factor
        val newHeight = tempSize.height + diffHeight * factor
        val widthRatio = newWidth / clipRect.width()
        val heightRatio = newHeight / clipRect.height()
        layoutInfo.widthRatio = widthRatio
        layoutInfo.heightRatio = heightRatio

        // 计算变换位置
        val dx = centerPoint.x - clipRect.centerX()
        val dy = centerPoint.y - clipRect.centerY()
        val tx = dx * destScale - dx
        val ty = dy * destScale - dy
        val deltaTx = tx + (clipRect.centerX() - resizeRect.centerX())
        val deltaTy = ty + (clipRect.centerY() - resizeRect.centerY())
        // 终点坐标
        val cx = centerPoint.x + deltaTx
        val cy = centerPoint.y + deltaTy
        // 从起始坐标tempCenterPoint根据factor变换到终点坐标(cx, cy)
        val tcx = tempCenterPoint.x + (cx - tempCenterPoint.x) * factor
        val tcy = tempCenterPoint.y + (cy - tempCenterPoint.y) * factor
        layoutInfo.centerXRatio = (tcx - clipRect.left) / clipRect.width()
        layoutInfo.centerYRatio = (tcy - clipRect.top) / clipRect.height()
    }

    override fun onUpdateLayout(clipRect: RectF) {
        // Log.w("sqsong", "onUpdateLayout, pivotX: $pivotX, pivotY: $pivotY")
        // 根据缩放比例重新计算控件的宽高
        val newWidth = clipRect.width() * layoutInfo.widthRatio
        val newHeight = clipRect.height() * layoutInfo.heightRatio
        // 根据布局信息中的中心点比例来计算控件的中心点位置，然后进一步计算出控件的摆放位置
        val cx = clipRect.left + clipRect.width() * layoutInfo.centerXRatio
        val cy = clipRect.top + clipRect.height() * layoutInfo.centerYRatio
        val left = (cx - newWidth / 2f).toInt()
        val top = (cy - newHeight / 2f).toInt()
        val right = (cx + newWidth / 2f).toInt()
        val bottom = (cy + newHeight / 2f).toInt()
        layout(left, top, right, bottom)
        // 重置位移
        translationX = 0f
        translationY = 0f
    }

    override fun invalidateView() = invalidate()

    override fun isTouchedInLayer(x: Float, y: Float): Boolean {
        // 首先判断触摸点是否在图片范围内，需要将父布局中的坐标转换为图片控件的本地坐标
        val localPoint = mapCoordinateToLocal(this, x, y)
        if (localPoint[0] < 0 || localPoint[0] > width || localPoint[1] < 0 || localPoint[1] > height) {
            return false
        }

        // 将坐标映射到图片Bitmap上，如果透明度不为0，则认为是在图片上，否则认为是在图片外
        val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return false
        val scale = minOf(width.toFloat() / bitmap.width, height.toFloat() / bitmap.height)
        val dx = (width - bitmap.width * scale) / 2
        val dy = (height - bitmap.height * scale) / 2
        val bitmapPoint = floatArrayOf((localPoint[0] - dx) / scale, (localPoint[1] - dy) / scale)
        val bitmapX = bitmapPoint[0].toInt()
        val bitmapY = bitmapPoint[1].toInt()
        if (bitmapX < 0 || bitmapX >= bitmap.width || bitmapY < 0 || bitmapY >= bitmap.height) {
            return false
        }
        val pixel = bitmap.getPixel(bitmapX, bitmapY)
        return (pixel shr 24 and 0xff) != 0
    }

    override fun stagingLayerTempCacheInfo(focusX: Float, focusY: Float) {
        // StackOverflow: https://stackoverflow.com/questions/14415035/setpivotx-works-strange-on-scaled-view
        // 在进行缩放、旋转操作时，先将缩放锚点设置到双指中心点
        val focusPoint = mapCoordinateToLocal(this, focusX, focusY)
        resetViewPivotTo(focusPoint[0], focusPoint[1], tempMatrix)
        layerCacheInfo = LayerTempCacheInfo(
            scaleX = scaleX,
            scaleY = scaleY,
            rotation = rotation,
        )
    }


    override fun onScaleRotate(scaleFactor: Float, deltaAngle: Float, focusX: Float, focusY: Float) {
        scaleX = scaleFactor * layerCacheInfo.scaleX
        scaleY = scaleFactor * layerCacheInfo.scaleY
        rotation = deltaAngle + layerCacheInfo.rotation
        Log.w("sqsong", "onScaleRotate: scaleX: $scaleX, scaleY: $scaleY, rotation: $rotation")
        // 重绘(边框)
        invalidate()
    }

    override fun changeSaveState(isSave: Boolean) {
        isSaveMode = isSave
    }

    override fun resetLayerPivot() {
        resetViewPivotTo(width / 2f, height / 2f, tempMatrix)
    }

    override fun startTouchAnim() {
        // 图层选中时的缩放动画，宽度最多放大25dp，高度按比例计算。这样能保证无论图层放大多少，缩放在视觉上保持一致。
        // 否则可能出现图层放大也大，缩放的幅度也会越大
        val currentWidth = width * scaleX
        val currentHeight = height * scaleY
        val maxXSize = dp2Px<Float>(25)
        val maxYSize = maxXSize * height / width
        ValueAnimator.ofFloat(0f, 1.0f, 0f).apply {
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val scaleFactor = it.animatedValue as Float
                val newWidth = currentWidth + maxXSize * scaleFactor
                val newHeight = currentHeight + maxYSize * scaleFactor
                val sx = newWidth / width
                val sy = newHeight / height
                scaleX = sx
                scaleY = sy
            }
            start()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // 当控件的尺寸发生变化时，重置缩放锚点(为中心点)
        resetLayerPivot()
    }

    override fun onDraw(canvas: Canvas) {
        if (isSelectedLayer && !isSaveMode) {
            drawSelectedLayer(canvas) {
                super.onDraw(canvas)
            }
        } else {
            super.onDraw(canvas)
        }
    }

    private inline fun drawSelectedLayer(canvas: Canvas, superDraw: () -> Unit) {
        val intrinsicWidth = drawable?.intrinsicWidth ?: 0
        val intrinsicHeight = drawable?.intrinsicHeight ?: 0
        if (intrinsicWidth <= 0 || intrinsicHeight <= 0) {
            superDraw()
            return
        }
        // 使用图片矩阵来计算clip区域，这样可以让边框完全贴合图片；如果直接使用控件的宽高来计算，可能会出现边框与图片之间有间隙
        pathRect.set(0f, 0f, intrinsicWidth.toFloat(), intrinsicHeight.toFloat())
        imageMatrix.mapRect(pathRect)
        path.reset()
        val radius = cornerRadius / scaleX
        path.addRoundRect(pathRect, radius, radius, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(path)
        superDraw()
        canvas.restore()

        paint.strokeWidth = borderWidth / scaleX
        canvas.drawPath(path, paint)
    }

    fun onInitialLayout(parentView: ViewGroup, bitmap: Bitmap, clipRect: RectF) {
        setImageBitmap(bitmap)
        // 根据图片最长边相对控件最短边的[SCALE_FACTOR]倍数进行中心缩放展示
        var imageWidth = clipRect.width() * SCALE_FACTOR
        var imageHeight = imageWidth * bitmap.height / bitmap.width
        if (imageHeight > clipRect.height() * SCALE_FACTOR) {
            imageHeight = clipRect.height() * SCALE_FACTOR
            imageWidth = imageHeight * bitmap.width / bitmap.height
        }
        Log.w("sqsong", "onInitialLayout: bitmap size: ${bitmap.width}x${bitmap.height}, image size: $imageWidth x $imageHeight")
        val layoutParams = LayoutParams(imageWidth.toInt(), imageHeight.toInt())
        // 添加控件到父布局中
        parentView.addView(this, layoutParams)
        // 设置初始偏移量(实验性，这里将图片放到父控件画布的右下角)
        val tx = (clipRect.width() - imageWidth) / 2
        val ty = (clipRect.height() - imageHeight) / 2

        val cx = clipRect.centerX() + tx
        val cy = clipRect.centerY() + ty
        val left = (cx - imageWidth / 2f).toInt()
        val top = (cy - imageHeight / 2f).toInt()
        val right = (cx + imageWidth / 2f).toInt()
        val bottom = (cy + imageHeight / 2f).toInt()
        // 计算控件的摆放位置
        layout(left, top, right, bottom)
        stagingResizeInfo(clipRect, true)
    }
}