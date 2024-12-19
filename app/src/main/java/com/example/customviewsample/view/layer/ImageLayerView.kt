package com.example.customviewsample.view.layer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import com.example.customviewsample.common.helper.BitmapCacheHelper
import com.example.customviewsample.utils.dp2Px
import com.example.customviewsample.utils.getThemeColor
import com.example.customviewsample.view.layer.anno.LayerType
import com.example.customviewsample.view.layer.data.ImageLayerInfo
import com.example.customviewsample.view.layer.data.LayerPreviewData
import com.example.customviewsample.view.layer.data.LayerSnapShot
import com.sqsong.nativelib.NativeLib

open class ImageLayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    override val layoutInfo: LayoutInfo = LayoutInfo(),
    override val cornerRadius: Float = dp2Px(6f),
    override val borderWidth: Float = dp2Px(1.5f),
    override val borderColor: Int = getThemeColor(context, com.google.android.material.R.attr.colorPrimary),
) : AbsLayerView(context, attrs, defStyleAttr, layoutInfo, cornerRadius, borderWidth, borderColor) {

    protected val imageMatrix = Matrix()
    protected var imageBitmap: Bitmap? = null

    private val imagePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            isFilterBitmap = true
        }
    }

    private val testPaint by lazy {
        Paint().apply {
            color = Color.GREEN
            style = Paint.Style.FILL
        }
    }

    override fun getViewLayerType(): Int = LayerType.LAYER_IMAGE

    override fun toLayerSnapshot(clipRect: RectF): LayerSnapShot? {
        val cachePath = imageBitmap?.let { BitmapCacheHelper.get().cacheBitmap(context, it, NativeLib.hasAlpha(it)) }
        val layoutInfo = LayoutInfo()
        updateChildLayoutInfo(layoutInfo, clipRect, this)
        val layerInfo = ImageLayerInfo(imageCachePath = cachePath, layerWidth = width, layerHeight = height, scaleX = scaleX, scaleY = scaleY, rotation = rotation)
        return LayerSnapShot(getViewLayerType(), layoutInfo, layerInfo)
    }

    override fun restoreLayerFromSnapshot(viewGroup: ViewGroup, snapshot: LayerSnapShot, clipRect: RectF) {
        val layerInfo = snapshot.imageLayerInfo ?: return
        val bitmap = BitmapCacheHelper.get().getCachedBitmap(context, layerInfo.imageCachePath) ?: return
        this.imageBitmap = bitmap
        val layoutParams = LayoutParams(layerInfo.layerWidth, layerInfo.layerHeight)
        viewGroup.addView(this, layoutParams)
        onUpdateLayout(clipRect)
        scaleX = layerInfo.scaleX
        scaleY = layerInfo.scaleY
        rotation = layerInfo.rotation
    }

    override fun toLayerPreview(): LayerPreviewData {
        return LayerPreviewData(id, getViewLayerType(), "Image", imageBitmap)
    }

    override fun isEditMenuAvailable(): Boolean = true

    override fun isTouchedInLayer(x: Float, y: Float): Boolean {
        val bitmap = imageBitmap ?: return false
        // 首先判断触摸点是否在图片范围内，需要将父布局中的坐标转换为图片控件的本地坐标
        val localPoint = mapCoordinateToLocal(this, x, y)
        if (localPoint[0] < 0 || localPoint[0] > width || localPoint[1] < 0 || localPoint[1] > height) {
            return false
        }
        // 将坐标映射到图片Bitmap上，如果透明度不为0，则认为是在图片上，否则认为是在图片外
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

    private fun updateImageMatrix() {
        val bitmap = imageBitmap ?: return
        if (width == 0 || height == 0) return

        val bmpWidth = bitmap.width
        val bmpHeight = bitmap.height
        if (bmpWidth <= 0 || bmpHeight <= 0) return
        // FIT_CENTER逻辑：等比缩放并居中
        imageMatrix.reset()
        val scaleX = width.toFloat() / bmpWidth
        val scaleY = height.toFloat() / bmpHeight
        val scale = minOf(scaleX, scaleY)
        imageMatrix.postScale(scale, scale)
        val scaledWidth = bmpWidth * scale
        val scaledHeight = bmpHeight * scale
        val dx = (width - scaledWidth) / 2f
        val dy = (height - scaledHeight) / 2f
        imageMatrix.postTranslate(dx, dy)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        resetLayerPivot()
        updateImageMatrix()
        super.onLayout(changed, left, top, right, bottom)
    }

    open fun onInitialLayout(parentView: ViewGroup, bitmap: Bitmap, clipRect: RectF) {
        this.imageBitmap = bitmap
        this.isSelectedLayer = true
        // 根据图片最长边相对控件最短边的[SCALE_FACTOR]倍数进行中心缩放展示
        var imageWidth = clipRect.width() * SCALE_FACTOR
        var imageHeight = imageWidth * bitmap.height / bitmap.width
        if (imageHeight > clipRect.height() * SCALE_FACTOR) {
            imageHeight = clipRect.height() * SCALE_FACTOR
            imageWidth = imageHeight * bitmap.width / bitmap.height
        }
        // calculate image matrix
        imageMatrix.reset()
        imageMatrix.postScale(imageWidth / bitmap.width, imageHeight / bitmap.height)
        Log.w("sqsong", "onInitialLayout: bitmap size: ${bitmap.width}x${bitmap.height}, image size: $imageWidth x $imageHeight")
        val layoutParams = LayoutParams(imageWidth.toInt(), imageHeight.toInt())
        // 添加控件到父布局中
        parentView.addView(this, layoutParams)

        val cx = clipRect.centerX()
        val cy = clipRect.centerY()
        val left = (cx - imageWidth / 2f).toInt()
        val top = (cy - imageHeight / 2f).toInt()
        val right = (cx + imageWidth / 2f).toInt()
        val bottom = (cy + imageHeight / 2f).toInt()
        // 初始化计算控件的摆放位置
        layout(left, top, right, bottom)
        stagingResizeInfo(clipRect, true)
    }

    override fun onDraw(canvas: Canvas) {
        imageBitmap?.let { drawImageBitmap(canvas, it) }
    }

    protected fun drawImageBitmap(canvas: Canvas, bitmap: Bitmap) {
        if (isSelectedLayer && !isTouched && !isSaveMode) {
            pathRect.set(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            imageMatrix.mapRect(pathRect)
            borderPath.reset()
            val radius = cornerRadius / scaleX
            borderPath.addRoundRect(pathRect, radius, radius, Path.Direction.CW)
            canvas.save()
            canvas.clipPath(borderPath)
            canvas.drawBitmap(bitmap, imageMatrix, imagePaint)
            canvas.restore()

            borderPaint.strokeWidth = borderWidth / scaleX
            canvas.drawPath(borderPath, borderPaint)
        } else {
            canvas.drawBitmap(bitmap, imageMatrix, imagePaint)
        }
    }
}