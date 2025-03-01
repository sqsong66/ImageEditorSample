package com.example.customviewsample.view.layer

import android.graphics.RectF
import android.widget.ImageView
import com.example.customviewsample.view.layer.anno.CoordinateLocation
import com.example.customviewsample.view.layer.anno.LayerType

@Deprecated("Use AbsLayerView instead")
interface AbsLayer {

    var isSelectedLayer: Boolean

    val absLayoutInfo: LayoutInfo

    @LayerType
    val absLayerType: Int

    /**
     * 子控件进行平移操作
     * @param dx x轴偏移量
     * @param dy y轴偏移量
     * @param pcx 父控件中心点x轴坐标
     * @param pcy 父控件中心点y轴坐标
     * @return 判断子控件坐标与父控件坐标的关系(是否重合)
     */
    @CoordinateLocation
    fun translateLayer(dx: Float, dy: Float, pcx: Float, pcy: Float): Int

    /**
     * 更新子View的布局信息
     * @param clipRect 父控件画布区域
     */
    fun updateLayoutInfo(clipRect: RectF)

    /**
     * 父控件尺寸发生变化时或触摸过后，保存子控件的尺寸大小以及中心点以及位置信息，方便在进行尺寸切换时进行动画过渡。
     * @param clipRect 父控件画布区域
     * @param updateLayoutInfo 是否更新布局信息
     */
    fun stagingResizeInfo(clipRect: RectF, updateLayoutInfo: Boolean = true)

    /**
     * 在进行修改画布尺寸前，临时保存子控件的尺寸大小以及中心点位置信息，
     * 方便在进行尺寸吸怪动画时进行流畅变换过渡。
     */
    fun tempStagingSize()

    /**
     * 父控件切换画布尺寸时，子控件根据画布局域以及目标缩放比例进行变换
     * @param clipRect 父控件画布区域
     * @param destScale 目标缩放比例(最终缩放比例)
     * @param factor 变换因子(0f ~ 1f)
     */
    fun transformLayerByResize(clipRect: RectF, destScale: Float, factor: Float)

    /**
     * 根据布局信息更新子控件的布局位置
     * @param clipRect 父控件画布区域
     */
    fun onUpdateLayout(clipRect: RectF)

    fun invalidateView()

    /**
     * 判断触摸点是否在子控件内，如果是图片控件[ImageView]则判断触摸点是否在图片的非透明区域。
     * @param x 触摸点x轴坐标
     * @param y 触摸点y轴坐标
     * @return 是否在子控件内
     */
    fun isTouchedInLayer(x: Float, y: Float): Boolean

    /**
     * 子控件在触摸时先缓存当前的缩放、旋转信息，方便在移动时进行缩放、旋转操作。缩放旋转时的初始值会使用当前缓存的数值。
     * @param focusX 双指触摸时中心点x轴坐标
     * @param focusY 双指触摸时中心点y轴坐标
     */
    fun stagingLayerTempCacheInfo(focusX: Float, focusY: Float)

    /**
     * 子控件在触摸移动时进行缩放、旋转操作。
     * @param scaleFactor 缩放因子
     * @param deltaAngle 旋转角度
     */
    fun onScaleRotate(scaleFactor: Float, deltaAngle: Float, tx: Float, ty: Float, focusX: Float, focusY: Float)

    /**
     * 在保存操作时，需要修改子控件中的绘制方法，防止不必要的信息绘制到最终结果图。
     * @param isSave 是否保存状态
     */
    fun changeSaveState(isSave: Boolean)

    /**
     * 重置子控件的缩放、旋转锚点(为自身中心点)
     */
    fun resetLayerPivot()

    fun startTouchAnim()

    /**
     * 是否检测图层中心点与父容器中心点重合以及旋转角度是否是90度的整数倍
     */
    fun detectCenterCoordinateAndRotation(): Boolean
}