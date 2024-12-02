package com.example.customviewsample.view.layer

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.view.View

interface AbsLayer {

    /**
     * 更新子View的布局信息
     * @param layoutInfo 布局信息
     * @param clipRect 画布区域
     * @param child 子View
     */
    fun updateChildLayoutInfo(layoutInfo: LayoutInfo, clipRect: RectF, child: View) {
        // 中心点坐标需要加上控件的translationX和translationY
        val cx = (child.left + child.right) / 2f + child.translationX
        val cy = (child.top + child.bottom) / 2f + child.translationY
        layoutInfo.centerXRatio = (cx - clipRect.left) / clipRect.width()
        layoutInfo.centerYRatio = (cy - clipRect.top) / clipRect.height()
        layoutInfo.widthRatio = (child.right - child.left) / clipRect.width()
        layoutInfo.heightRatio = (child.bottom - child.top) / clipRect.height()
    }

    fun mapCoordinateToLocal(view: View, x: Float, y: Float): FloatArray {
        val localPoint = floatArrayOf(x - view.left, y - view.top)
        if (view.matrix.isIdentity) return localPoint
        val invertMatrix = Matrix()
        view.matrix.invert(invertMatrix)
        invertMatrix.mapPoints(localPoint)
        return localPoint
    }

    var isSelectedLayer: Boolean

    val absCenterPoint: PointF

    val absLayoutInfo: LayoutInfo

    @LayerType
    val absLayerType: Int

    fun translate(dx: Float, dy: Float)

    fun updateLayoutInfo(clipRect: RectF)

    fun stagingResizeInfo(clipRect: RectF, updateLayoutInfo: Boolean = true)

    fun transformLayerByResize(clipRect: RectF, destScale: Float)

    fun onUpdateLayout(clipRect: RectF)

    fun invalidateView()

    fun isTouchedInLayer(x: Float, y: Float): Boolean

    fun onScale(scaleFactor: Float, focusX: Float, focusY: Float)
}