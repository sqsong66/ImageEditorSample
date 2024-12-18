package com.example.customviewsample.view.layer

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import com.example.customviewsample.common.helper.BitmapCacheHelper
import com.example.customviewsample.utils.dp2Px
import com.example.customviewsample.utils.getThemeColor
import com.example.customviewsample.view.layer.anno.CoordinateLocation
import com.example.customviewsample.view.layer.anno.LayerRotation
import com.example.customviewsample.view.layer.anno.LayerType
import com.example.customviewsample.view.layer.data.LayerSnapShot
import kotlin.math.abs

abstract class AbsLayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    protected open val layoutInfo: LayoutInfo = LayoutInfo(),
    protected open val cornerRadius: Float = dp2Px(6f),
    protected open val borderWidth: Float = dp2Px(1.5f),
    protected open val borderColor: Int = getThemeColor(context, com.google.android.material.R.attr.colorPrimary),
) : View(context, attrs, defStyleAttr) {

    var isTouched = false
        private set
    var isSelectedLayer = false
    protected val pathRect = RectF()
    protected var isSaveMode = false
    protected val borderPath = Path()

    protected val resizeRect = RectF()
    protected val tempMatrix = Matrix()

    // protected val layoutInfo = LayoutInfo()
    protected val stagingCenterPoint = PointF()
    protected val tempArray = FloatArray(2)

    // 临时保存的中心点，在进行尺寸变换动画前临时存储，在动画过程中中心点的计算基于该中心点
    protected var tempCenterPoint = PointF()

    // 临时保存的尺寸大小，在进行尺寸变换动画前临时存储，在动画过程中尺寸的计算基于改尺寸
    protected var tempSize = Size(0, 0)

    // 父控件尺寸发生变化时或触摸过后记录子控件的尺寸，在进行尺寸切换时进行动画过渡使用
    protected var resizeSize = Size(0, 0)

    // 缓存子控件的缩放、旋转信息，方便在移动时进行各种变换操作，变换操作基于缓存的数值
    private var layerCacheInfo = LayerTempCacheInfo()

    private val pivotPoint = PointF()

    @CoordinateLocation
    private var coordinateLoc: Int = CoordinateLocation.COORDINATE_NONE

    @LayerRotation
    var layerRotation: Int = LayerRotation.ROTATION_0

    protected val borderPaint by lazy {
        Paint().apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = borderWidth
        }
    }

    @LayerType
    abstract fun getViewLayerType(): Int

    abstract fun toLayerSnapshot(): LayerSnapShot?

    abstract fun restoreLayerFromSnapshot(viewGroup: ViewGroup, snapshot: LayerSnapShot)

    protected open fun detectCenterCoordinateAndRotation(): Boolean = true

    /**
     * 判断触摸点是否在子控件内，如果是图片控件[ImageView]则判断触摸点是否在图片的非透明区域。
     * @param x 触摸点x轴坐标
     * @param y 触摸点y轴坐标
     * @return 是否在子控件内
     */
    open fun isTouchedInLayer(x: Float, y: Float): Boolean = false

    /**
     * 子控件进行平移操作
     * @param dx x轴偏移量
     * @param dy y轴偏移量
     * @param pcx 父控件中心点x轴坐标
     * @param pcy 父控件中心点y轴坐标
     * @param onVibrate 震动回调
     * @return 判断子控件坐标与父控件坐标的关系(是否重合)
     */
    @CoordinateLocation
    fun translateLayer(dx: Float, dy: Float, pcx: Float, pcy: Float, onVibrate: () -> Unit): Int {
        if (detectCenterCoordinateAndRotation()) {
            val preCoordinateLoc = coordinateLoc
            val point = getLayerCenterPoint()
            val cx = point.x
            val cy = point.y
            coordinateLoc = detectCoordinateLoc(cx, cy, pcx, pcy)
            var tx = dx
            var ty = dy
            when (coordinateLoc) {
                CoordinateLocation.COORDINATE_CENTER -> {
                    if (abs(dx) < COORDINATE_MOVE_THRESHOLD) tx = pcx - cx
                    if (abs(dy) < COORDINATE_MOVE_THRESHOLD) ty = pcy - cy
                    if (preCoordinateLoc != coordinateLoc) onVibrate()
                }

                CoordinateLocation.COORDINATE_CENTER_X -> {
                    if (abs(dx) < COORDINATE_MOVE_THRESHOLD) tx = pcx - cx
                    if (preCoordinateLoc != coordinateLoc && preCoordinateLoc != CoordinateLocation.COORDINATE_CENTER) onVibrate()
                }

                CoordinateLocation.COORDINATE_CENTER_Y -> {
                    if (abs(dy) < COORDINATE_MOVE_THRESHOLD) ty = pcy - cy
                    if (preCoordinateLoc != coordinateLoc && preCoordinateLoc != CoordinateLocation.COORDINATE_CENTER) onVibrate()
                }
            }
            translate(tx, ty)
        } else {
            translate(dx, dy)
        }
        return coordinateLoc
    }

    /**
     * 子控件在触摸移动时进行缩放/旋转/平移操作。
     * @param scaleFactor 缩放因子
     * @param deltaAngle 旋转角度
     * @param tx x轴偏移量
     * @param ty y轴偏移量
     * @param pcx 父控件中心点x轴坐标
     * @param pcy 父控件中心点y轴坐标
     * @param onVibrate 震动回调
     */
    open fun onLayerTranslation(scaleFactor: Float, deltaAngle: Float, tx: Float, ty: Float, pcx: Float, pcy: Float, onVibrate: () -> Unit): Pair<Int, Int> {
        scaleX = scaleFactor * layerCacheInfo.scaleX
        scaleY = scaleFactor * layerCacheInfo.scaleY
        val dr = deltaAngle + layerCacheInfo.rotation - rotation
        val layerRotation = rotateLayer(dr, onVibrate)

        val dx = tx + layerCacheInfo.translationX - translationX
        val dy = ty + layerCacheInfo.translationY - translationY
        // Log.w("sqsong", "onLayerTranslation, rotation: $rotation")
        val translateLoc = translateLayer(dx, dy, pcx, pcy, onVibrate)
        invalidate() // 需要重绘边框
        return translateLoc to layerRotation
    }

    @LayerRotation
    private fun rotateLayer(angle: Float, onVibrate: () -> Unit): Int {
        if (detectCenterCoordinateAndRotation()) {
            val preRotation = layerRotation
            layerRotation = detectLayerRotation()
            var destAngle = angle
            when (layerRotation) {
                LayerRotation.ROTATION_0 -> {
                    if (abs(angle) < ROTATION_THRESHOLD) destAngle = 0f - rotation
                    if (preRotation != layerRotation) onVibrate()
                }

                LayerRotation.ROTATION_45 -> {
                    if (abs(angle) < ROTATION_THRESHOLD) destAngle = (if (rotation > 0f) 45f else -45f) - rotation
                    if (preRotation != layerRotation) onVibrate()
                }

                LayerRotation.ROTATION_90 -> {
                    if (abs(angle) < ROTATION_THRESHOLD) destAngle = (if (rotation > 0f) 90f else -90f) - rotation
                    if (preRotation != layerRotation) onVibrate()
                }

                LayerRotation.ROTATION_135 -> {
                    if (abs(angle) < ROTATION_THRESHOLD) destAngle = (if (rotation > 0f) 135f else -135f) - rotation
                    if (preRotation != layerRotation) onVibrate()
                }

                LayerRotation.ROTATION_180 -> {
                    if (abs(angle) < ROTATION_THRESHOLD) destAngle = (if (rotation > 0f) 180f else -180f) - rotation
                    if (preRotation != layerRotation) onVibrate()
                }

                LayerRotation.ROTATION_225 -> {
                    if (abs(angle) < ROTATION_THRESHOLD) destAngle = (if (rotation > 0f) 225f else -225f) - rotation
                    if (preRotation != layerRotation) onVibrate()
                }

                LayerRotation.ROTATION_270 -> {
                    if (abs(angle) < ROTATION_THRESHOLD) destAngle = (if (rotation > 0f) 270f else -270f) - rotation
                    if (preRotation != layerRotation) onVibrate()
                }

                LayerRotation.ROTATION_315 -> {
                    if (abs(angle) < ROTATION_THRESHOLD) destAngle = (if (rotation > 0f) 315f else -315f) - rotation
                    if (preRotation != layerRotation) onVibrate()
                }
            }
            rotation += destAngle
        } else {
            rotation += angle
        }
        return layerRotation
    }

    @LayerRotation
    private fun detectLayerRotation(): Int {
        val angle = abs(rotation % 360)
        return when (angle) {
            in 0f..1f -> LayerRotation.ROTATION_0
            in 359f..360f -> LayerRotation.ROTATION_0
            in 44f..46f -> LayerRotation.ROTATION_45
            in 89f..91f -> LayerRotation.ROTATION_90
            in 134f..136f -> LayerRotation.ROTATION_135
            in 179f..181f -> LayerRotation.ROTATION_180
            in 224f..226f -> LayerRotation.ROTATION_225
            in 269f..271f -> LayerRotation.ROTATION_270
            in 314f..316f -> LayerRotation.ROTATION_315
            else -> LayerRotation.ROTATION_NONE
        }
    }

    /**
     * 检测图层中心点是否与父容器中心点重合
     * @param cx 子控件中心点x坐标(相对于父容器)
     * @param cy 子控件中心点y坐标(相对于父容器)
     * @param px 父容器中心点x坐标
     * @param py 父容器中心点y坐标
     * @return [CoordinateLocation]
     */
    @CoordinateLocation
    private fun detectCoordinateLoc(cx: Float, cy: Float, px: Float, py: Float): Int {
        val inCenterX = abs(px - cx) <= COORDINATE_DETECT_OFFSET
        val inCenterY = abs(py - cy) <= COORDINATE_DETECT_OFFSET
        return when {
            inCenterX && inCenterY -> CoordinateLocation.COORDINATE_CENTER
            inCenterX -> CoordinateLocation.COORDINATE_CENTER_X
            inCenterY -> CoordinateLocation.COORDINATE_CENTER_Y
            else -> CoordinateLocation.COORDINATE_NONE
        }
    }

    private fun translate(dx: Float, dy: Float) {
        translationX += dx
        translationY += dy
    }

    /**
     * 更新子View的布局信息
     * @param clipRect 父控件画布区域
     */
    fun updateLayoutInfo(clipRect: RectF) {
        updateChildLayoutInfo(layoutInfo, clipRect, this)
    }

    /**
     * 父控件尺寸发生变化时或触摸过后，保存子控件的尺寸大小以及中心点以及位置信息，方便在进行尺寸切换时进行动画过渡。
     * @param clipRect 父控件画布区域
     * @param updateLayoutInfo 是否更新布局信息
     */
    fun stagingResizeInfo(clipRect: RectF, updateLayoutInfo: Boolean) {
        if (updateLayoutInfo) updateLayoutInfo(clipRect)
        resizeRect.set(clipRect)
        // update center point
        val cx = (left + right) / 2f + translationX
        val cy = (top + bottom) / 2f + translationY
        stagingCenterPoint.set(cx, cy)
        resizeSize = Size(right - left, bottom - top)
    }

    /**
     * 在进行修改画布尺寸前，临时保存子控件的尺寸大小以及中心点位置信息，
     * 方便在进行尺寸吸怪动画时进行流畅变换过渡。
     */
    fun tempStagingSize() {
        tempSize = Size(right - left, bottom - top)
        tempCenterPoint.set((left + right) / 2f + translationX, (top + bottom) / 2f + translationY)
    }

    /**
     * 父控件切换画布尺寸时，子控件根据画布局域以及目标缩放比例进行变换
     * @param clipRect 父控件画布区域
     * @param destScale 目标缩放比例(最终缩放比例)
     * @param factor 变换因子(0f ~ 1f)
     */
    open fun transformLayerByResize(clipRect: RectF, destScale: Float, destBgScale: Float, factor: Float) {
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
        val dx = stagingCenterPoint.x - clipRect.centerX()
        val dy = stagingCenterPoint.y - clipRect.centerY()
        val tx = dx * destScale - dx
        val ty = dy * destScale - dy
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

    /**
     * 根据布局信息更新子控件的布局位置
     * @param clipRect 父控件画布区域
     */
    fun onUpdateLayout(clipRect: RectF) {
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

    /**
     * 子控件在触摸时先缓存当前的缩放、旋转信息，方便在移动时进行缩放、旋转操作。缩放旋转时的初始值会使用当前缓存的数值。
     * @param focusX 双指触摸时中心点x轴坐标
     * @param focusY 双指触摸时中心点y轴坐标
     */
    fun stagingLayerTempCacheInfo(focusX: Float, focusY: Float) {
        // StackOverflow: https://stackoverflow.com/questions/14415035/setpivotx-works-strange-on-scaled-view
        // 在进行缩放、旋转操作时，先将缩放锚点设置到双指中心点
        val focusPoint = mapCoordinateToLocal(this, focusX, focusY)
        pivotPoint.set(focusPoint[0] - width / 2f, focusPoint[1] - height / 2f)
        resetViewPivotTo(focusPoint[0], focusPoint[1], tempMatrix)
        layerCacheInfo = LayerTempCacheInfo(
            scaleX = scaleX,
            scaleY = scaleY,
            rotation = rotation,
            translationX = translationX,
            translationY = translationY
        )
    }

    /**
     * 在保存操作时，需要修改子控件中的绘制方法，防止不必要的信息绘制到最终结果图。
     * @param isSave 是否保存状态
     */
    fun changeSaveState(isSave: Boolean) {
        isSaveMode = isSave
    }

    /**
     * 重置子控件的缩放、旋转锚点(为自身中心点)
     */
    fun resetLayerPivot() {
        pivotPoint.set(0f, 0f)
        resetViewPivotTo(width / 2f, height / 2f, tempMatrix)
    }

    fun getLayerCenterPoint(): PointF {
        tempArray[0] = width / 2f
        tempArray[1] = height / 2f
        matrix.mapPoints(tempArray)
        tempCenterPoint.set(tempArray[0] + left, tempArray[1] + top)
        return tempCenterPoint
    }

    /**
     * 点击到控件时进行的缩放动画
     */
    fun startTouchAnim() {
        // 图层选中时的缩放动画，宽度最多放大25dp，高度按比例计算。这样能保证无论图层放大多少，缩放在视觉上保持一致。
        // 否则可能出现图层放大也大，缩放的幅度也会越大
        val currentWidth = width * scaleX
        val currentHeight = height * scaleY
        val maxXSize = dp2Px<Float>(20)
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

    fun updateTouchState(isTouched: Boolean) {
        this.isTouched = isTouched
        invalidate()
    }

    open fun isEditMenuAvailable(): Boolean = false

    companion object {

        fun crateLayerViewBySnapshot(context: Context, snapshot: LayerSnapShot): AbsLayerView? {
            return when (snapshot.viewLayerType) {
                LayerType.LAYER_IMAGE -> {
                    val layerInfo = snapshot.imageLayerInfo ?: return null
                    val bitmap = BitmapCacheHelper.get().getCachedBitmap(context, layerInfo.imageCachePath) ?: return null

                    null
                }

                LayerType.LAYER_BACKGROUND -> {
                    null
                }

                else -> null
            }
        }

    }
}
