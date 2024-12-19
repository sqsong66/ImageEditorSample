package com.example.customviewsample.view.layer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import com.example.customviewsample.common.helper.BitmapCacheHelper
import com.example.customviewsample.view.layer.anno.LayerType
import com.example.customviewsample.view.layer.data.BackgroundLayerInfo
import com.example.customviewsample.view.layer.data.LayerPreviewData
import com.example.customviewsample.view.layer.data.LayerSnapShot
import com.sqsong.nativelib.NativeLib

class BackgroundLayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    override val layoutInfo: LayoutInfo = LayoutInfo(),
) : ImageLayerView(context, attrs, defStyleAttr) {

    private var bgColor: IntArray? = null

    private val bgPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bgColor?.let { color ->
            bgPaint.shader = LinearGradient(0f, height / 2f, width.toFloat(), height / 2f, color, null, Shader.TileMode.CLAMP)
        }
    }

    override fun getViewLayerType(): Int = LayerType.LAYER_BACKGROUND

    override fun toLayerSnapshot(clipRect: RectF): LayerSnapShot {
        val cachePath = imageBitmap?.let {
            BitmapCacheHelper.get().cacheBitmap(context, it, NativeLib.hasAlpha(it))
        }
        val layoutInfo = LayoutInfo()
        updateChildLayoutInfo(layoutInfo, clipRect, this)
        // Log.w("songmao", "BackgroundLayerView toLayerSnapshot: $layoutInfo")
        val layerInfo = BackgroundLayerInfo(bgCachePath = cachePath, bgColor = bgColor?.clone(), layerWidth = width, layerHeight = height, scaleX = scaleX, scaleY = scaleY, rotation = rotation)
        return LayerSnapShot(getViewLayerType(), layoutInfo, backgroundLayerInfo = layerInfo)
    }

    override fun toLayerPreview(): LayerPreviewData {
        return LayerPreviewData(id, getViewLayerType(), "Background", layerColor = bgColor?.clone(), layerBitmap = imageBitmap)
    }

    override fun restoreLayerFromSnapshot(viewGroup: ViewGroup, snapshot: LayerSnapShot, clipRect: RectF) {
        val layerInfo = snapshot.backgroundLayerInfo ?: return
        val bitmap = BitmapCacheHelper.get().getCachedBitmap(context, layerInfo.bgCachePath)
        val bgColor = layerInfo.bgColor
        if (bitmap == null && bgColor == null) return
        if (bitmap != null) {
            this.bgColor = null
            this.imageBitmap = bitmap
        } else {
            this.bgColor = bgColor
            this.imageBitmap = null
            bgPaint.shader = LinearGradient(0f, height / 2f, width.toFloat(), height / 2f, bgColor!!, null, Shader.TileMode.CLAMP)
        }
        val layoutParams = LayoutParams(layerInfo.layerWidth, layerInfo.layerHeight)
        viewGroup.addView(this, layoutParams)
        onUpdateLayout(clipRect)
        scaleX = layerInfo.scaleX
        scaleY = layerInfo.scaleY
        rotation = layerInfo.rotation
    }

    override fun isEditMenuAvailable(): Boolean = false

    override fun isTouchedInLayer(x: Float, y: Float): Boolean {
        val localPoint = mapCoordinateToLocal(this, x, y)
        return localPoint[0] >= 0 && localPoint[0] <= width && localPoint[1] >= 0 && localPoint[1] <= height
    }

    override fun transformLayerByResize(clipRect: RectF, destScale: Float, destBgScale: Float, factor: Float) {
        // 计算变换大小
        val diffWidth = resizeSize.width * destBgScale - tempSize.width
        val diffHeight = resizeSize.height * destBgScale - tempSize.height
        val newWidth = tempSize.width + diffWidth * factor
        val newHeight = tempSize.height + diffHeight * factor
        val widthRatio = newWidth / clipRect.width()
        val heightRatio = newHeight / clipRect.height()
        layoutInfo.widthRatio = widthRatio
        layoutInfo.heightRatio = heightRatio

        // 计算变换位置
        val dx = stagingCenterPoint.x - clipRect.centerX()
        val dy = stagingCenterPoint.y - clipRect.centerY()
        val tx = dx * destBgScale - dx
        val ty = dy * destBgScale - dy
        val deltaTx = tx + (clipRect.centerX() - resizeRect.centerX())
        val deltaTy = ty + (clipRect.centerY() - resizeRect.centerY())
        // 终点坐标
        val cx = stagingCenterPoint.x + deltaTx
        val cy = stagingCenterPoint.y + deltaTy
        // 从起始坐标tempCenterPoint根据factor变换到终点坐标(cx, cy)
        val tcx = tempCenterPoint.x + (cx - tempCenterPoint.x) * factor
        val tcy = tempCenterPoint.y + (cy - tempCenterPoint.y) * factor
        layoutInfo.centerXRatio = (tcx - clipRect.left) / clipRect.width()
        layoutInfo.centerYRatio = (tcy - clipRect.top) / clipRect.height()
    }

    override fun onInitialLayout(parentView: ViewGroup, bitmap: Bitmap, clipRect: RectF) {
        this.bgColor = null
        this.imageBitmap = bitmap
        this.isSelectedLayer = true
        val layerWidth = clipRect.width().toInt()
        val layerHeight = clipRect.height().toInt()
        val layerRatio = layerWidth.toFloat() / layerHeight
        val bitmapRatio = bitmap.width.toFloat() / bitmap.height

        val scale = if (layerRatio > bitmapRatio) {
            layerWidth.toFloat() / bitmap.width
        } else {
            layerHeight.toFloat() / bitmap.height
        }
        imageMatrix.setScale(scale, scale)
        val imageWidth = (bitmap.width * scale).toInt()
        val imageHeight = (bitmap.height * scale).toInt()
        val layoutParams = ViewGroup.LayoutParams(imageWidth, imageHeight)
        // 移除之前的背景图层
        (parentView.getChildAt(0) as? AbsLayerView)?.let { bgLayer ->
            if (bgLayer.getViewLayerType() == LayerType.LAYER_BACKGROUND) {
                parentView.removeViewAt(0)
            }
        }
        parentView.addView(this, 0, layoutParams)
        val left = clipRect.centerX() - imageWidth / 2f
        val top = clipRect.centerY() - imageHeight / 2f
        val right = clipRect.centerX() + imageWidth / 2f
        val bottom = clipRect.centerY() + imageHeight / 2f
        layout(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
        stagingResizeInfo(clipRect, true)
    }

    fun onInitialLayout(parentView: ViewGroup, bgColor: IntArray, clipRect: RectF) {
        this.bgColor = bgColor
        this.imageBitmap = null
        this.isSelectedLayer = true
        val layerWidth = clipRect.width().toInt()
        val layerHeight = clipRect.height().toInt()
        val layoutParams = ViewGroup.LayoutParams(layerWidth, layerHeight)
        bgPaint.shader = LinearGradient(0f, layerHeight / 2f, layerWidth.toFloat(), layerHeight / 2f, bgColor, null, Shader.TileMode.CLAMP)
        // 移除之前的背景图层
        (parentView.getChildAt(0) as? AbsLayerView)?.let { bgLayer ->
            if (bgLayer.getViewLayerType() == LayerType.LAYER_BACKGROUND) {
                parentView.removeViewAt(0)
            }
        }
        parentView.addView(this, 0, layoutParams)
        layout(clipRect.left.toInt(), clipRect.top.toInt(), clipRect.right.toInt(), clipRect.bottom.toInt())
        stagingResizeInfo(clipRect, true)
    }

    fun updateBackgroundColor(bgColor: IntArray) {
        this.bgColor = bgColor
        this.imageBitmap = null
        bgPaint.shader = LinearGradient(0f, height / 2f, width.toFloat(), height / 2f, bgColor, null, Shader.TileMode.CLAMP)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        imageBitmap?.let {
            drawImageBitmap(canvas, it)
        } ?: run {
            drawBackgroundColor(canvas)
        }
    }

    private fun drawBackgroundColor(canvas: Canvas) {
        bgColor ?: return
        pathRect.set(0f, 0f, width.toFloat(), height.toFloat())
        if (isSelectedLayer && !isTouched && !isSaveMode) {
            borderPath.reset()
            val radius = cornerRadius / scaleX
            borderPath.addRoundRect(pathRect, radius, radius, Path.Direction.CW)
            canvas.save()
            canvas.clipPath(borderPath)
            canvas.drawRect(pathRect, bgPaint)
            canvas.restore()

            borderPaint.strokeWidth = borderWidth / scaleX
            canvas.drawPath(borderPath, borderPaint)
        } else {
            canvas.drawRect(pathRect, bgPaint)
        }
    }
}