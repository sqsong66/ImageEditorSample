package com.example.customviewsample.view.layer

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Size
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import com.example.customviewsample.utils.dp2Px
import com.example.customviewsample.view.layer.anno.CoordinateLocation
import com.example.customviewsample.view.layer.anno.LayerType

abstract class AbsLayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var isSelectedLayer = false
    protected val pathRect = RectF()
    protected var isSaveMode = false
    protected val borderPath = Path()
    protected val cornerRadius = dp2Px<Float>(6)
    protected val borderWidth = dp2Px<Float>(1.5f)

    private val resizeRect = RectF()
    private val tempMatrix = Matrix()
    private val centerPoint = PointF()
    private val layoutInfo = LayoutInfo()
    // 临时保存的中心点，在进行尺寸变换动画前临时存储，在动画过程中中心点的计算基于该中心点
    private var tempCenterPoint = PointF()
    // 临时保存的尺寸大小，在进行尺寸变换动画前临时存储，在动画过程中尺寸的计算基于改尺寸
    private var tempSize = Size(0, 0)
    // 父控件尺寸发生变化时或触摸过后记录子控件的尺寸，在进行尺寸切换时进行动画过渡使用
    private var resizeSize = Size(0, 0)
    // 缓存子控件的缩放、旋转信息，方便在移动时进行各种变换操作，变换操作基于缓存的数值
    private var layerCacheInfo = LayerTempCacheInfo()

    @LayerType
    abstract fun getViewLayerType(): Int

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
     * @return 判断子控件坐标与父控件坐标的关系(是否重合)
     */
    @CoordinateLocation
    fun translateLayer(dx: Float, dy: Float, pcx: Float, pcy: Float): Int {
        translate(dx, dy)
        return CoordinateLocation.COORDINATE_NONE
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
        centerPoint.set(cx, cy)
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
    fun transformLayerByResize(clipRect: RectF, destScale: Float, factor: Float) {
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
     * 子控件在触摸移动时进行缩放/旋转/平移操作。
     * @param scaleFactor 缩放因子
     * @param deltaAngle 旋转角度
     * @param tx x轴偏移量
     * @param ty y轴偏移量
     * @param focusX 双指触摸时中心点x轴坐标
     * @param focusY 双指触摸时中心点y轴坐标
     */
    open fun onLayerTranslation(scaleFactor: Float, deltaAngle: Float, tx: Float, ty: Float, focusX: Float, focusY: Float) {
        scaleX = scaleFactor * layerCacheInfo.scaleX
        scaleY = scaleFactor * layerCacheInfo.scaleY
        rotation = deltaAngle + layerCacheInfo.rotation
        translationX = tx + layerCacheInfo.translationX
        translationY = ty + layerCacheInfo.translationY
        invalidate() // 需要重绘边框
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
    fun resetLayerPivot() = resetViewPivotTo(width / 2f, height / 2f, tempMatrix)

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

}
