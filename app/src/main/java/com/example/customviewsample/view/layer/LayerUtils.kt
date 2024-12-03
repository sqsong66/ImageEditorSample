package com.example.customviewsample.view.layer

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
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
    layoutInfo.centerXRatio = (cx - clipRect.left) / clipRect.width()
    layoutInfo.centerYRatio = (cy - clipRect.top) / clipRect.height()
    layoutInfo.widthRatio = (child.right - child.left) / clipRect.width()
    layoutInfo.heightRatio = (child.bottom - child.top) / clipRect.height()
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
 * 重置锚点到控件中心
 * @param view 控件View
 */
fun resetPivotToCenter(view: View) {
    val oldPivotX = view.pivotX
    val oldPivotY = view.pivotY

    // 获取控件的宽高
    val viewWidth = view.width.toFloat()
    val viewHeight = view.height.toFloat()

    // 计算控件中心点
    val newPivotX = viewWidth / 2
    val newPivotY = viewHeight / 2

    // 保存旧的变换矩阵
    val tempMatrix = Matrix()
    tempMatrix.postTranslate(view.translationX, view.translationY)
    tempMatrix.postScale(view.scaleX, view.scaleY, oldPivotX, oldPivotY)
    tempMatrix.postRotate(view.rotation, oldPivotX, oldPivotY)

    // 获取旧的顶点位置
    val oldPoints = floatArrayOf(0f, 0f)
    tempMatrix.mapPoints(oldPoints)

    // 更新锚点到控件中心
    view.pivotX = newPivotX
    view.pivotY = newPivotY

    // 保存新的变换矩阵
    tempMatrix.reset()
    tempMatrix.postTranslate(view.translationX, view.translationY)
    tempMatrix.postScale(view.scaleX, view.scaleY, newPivotX, newPivotY)
    tempMatrix.postRotate(view.rotation, newPivotX, newPivotY)

    // 获取新的顶点位置
    val newPoints = floatArrayOf(0f, 0f)
    tempMatrix.mapPoints(newPoints)

    // 计算位置差异并调整平移
    val deltaX = oldPoints[0] - newPoints[0]
    val deltaY = oldPoints[1] - newPoints[1]
    view.translationX += deltaX
    view.translationY += deltaY
}