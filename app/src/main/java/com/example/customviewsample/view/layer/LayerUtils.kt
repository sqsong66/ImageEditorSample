package com.example.customviewsample.view.layer

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.example.customviewsample.common.ext.keepTwoDecimal
import kotlin.math.atan2
import kotlin.math.sqrt

fun getNewScale(resizeRect: RectF, newRect: RectF): Float {
    return when {
        resizeRect.width() > resizeRect.height() -> {
            when {
                newRect.width() > newRect.height() -> {
                    if (newRect.height() < resizeRect.height()) {
                        newRect.height() / resizeRect.height()
                    } else 1.0f
                }

                else -> {
                    newRect.width() / resizeRect.width()
                }
            }
        }

        resizeRect.width() < resizeRect.height() -> {
            when {
                newRect.width() < newRect.height() -> {
                    if (newRect.width() < resizeRect.width()) {
                        newRect.width() / resizeRect.width()
                    } else 1.0f
                }

                else -> {
                    newRect.height() / resizeRect.height()
                }
            }
        }

        else -> {
            when {
                newRect.width() < newRect.height() -> {
                    newRect.width() / resizeRect.width()
                }

                newRect.width() > newRect.height() -> {
                    newRect.height() / resizeRect.height()
                }

                else -> 1.0f
            }
        }
    }
}

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
    layoutInfo.centerXRatio = ((cx - clipRect.left) / clipRect.width()).keepTwoDecimal()
    layoutInfo.centerYRatio = ((cy - clipRect.top) / clipRect.height()).keepTwoDecimal()
    layoutInfo.widthRatio = ((child.right - child.left) / clipRect.width()).keepTwoDecimal()
    layoutInfo.heightRatio = ((child.bottom - child.top) / clipRect.height()).keepTwoDecimal()
}

/**
 * 将父控件的触摸坐标转换为View内部坐标
 * @param view View
 * @param x 父控件x轴坐标
 * @param y 父控件y轴坐标
 * @return View内部坐标
 */
fun mapCoordinateToLocal(view: View, x: Float, y: Float): FloatArray {
    val localPoint = floatArrayOf(x - view.left, y - view.top)
    if (view.matrix.isIdentity) return localPoint
    val invertMatrix = Matrix()
    view.matrix.invert(invertMatrix)
    invertMatrix.mapPoints(localPoint)
    return localPoint
}

/**
 * 计算两点之间的距离
 * @param x1 第一个点的x轴坐标
 * @param y1 第一个点的y轴坐标
 * @param x2 第二个点的x轴坐标
 * @param y2 第二个点的y轴坐标
 * @return 两点之间的距离
 */
fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val x = x2 - x1
    val y = y2 - y1
    return sqrt(x * x + y * y)
}

fun calculateDistance(event: MotionEvent): Float {
    val x = event.getX(0) - event.getX(1)
    val y = event.getY(0) - event.getY(1)
    return sqrt(x * x + y * y)
}

/**
 * 计算两点之间的中心点
 * @param point 中心点
 * @param event MotionEvent
 */
fun calculateCenterPoint(point: PointF, event: MotionEvent) {
    val x = event.getX(0) + event.getX(1)
    val y = event.getY(0) + event.getY(1)
    point[x / 2] = y / 2
}

/**
 * 计算两点之间的旋转角度
 * @param event MotionEvent
 * @return 旋转角度
 */
fun calculateRotation(event: MotionEvent): Float {
    val deltaX = (event.getX(0) - event.getX(1)).toDouble()
    val deltaY = (event.getY(0) - event.getY(1)).toDouble()
    val radius = atan2(deltaY, deltaX)
    return Math.toDegrees(radius).toFloat()
}

/**
 * 将View的旋转缩放锚点设置到新的位置
 * @param newPivotX 新的缩放锚点x轴坐标
 * @param newPivotY 新的缩放锚点y轴坐标
 * @param matrix 辅助Matrix
 */
fun View.resetViewPivotTo(newPivotX: Float, newPivotY: Float, matrix: Matrix) {
    val prePivotX = pivotX
    val prePivotY = pivotY

    matrix.reset()
    matrix.postTranslate(translationX, translationY)
    matrix.postScale(scaleX, scaleY, prePivotX, prePivotY)
    matrix.postRotate(rotation, prePivotX, prePivotY)
    val preOrigin = floatArrayOf(0f, 0f)
    matrix.mapPoints(preOrigin)

    pivotX = newPivotX
    pivotY = newPivotY
    matrix.reset()
    matrix.postTranslate(translationX, translationY)
    matrix.postScale(scaleX, scaleY, newPivotX, newPivotY)
    matrix.postRotate(rotation, newPivotX, newPivotY)
    val newOrigin = floatArrayOf(0f, 0f)
    matrix.mapPoints(newOrigin)

    val deltaX = preOrigin[0] - newOrigin[0]
    val deltaY = preOrigin[1] - newOrigin[1]
    translationX += deltaX
    translationY += deltaY
}