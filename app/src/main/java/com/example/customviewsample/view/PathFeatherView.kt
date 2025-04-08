package com.example.customviewsample.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class PathFeatherView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    
    companion object {
        const val MAX_FEATHER_RADIUS = 35f // 最大羽化半径
        const val MODE_BRUSH = 0 // 画刷模式
        const val MODE_SHAPE = 1 // 形状模式
        const val MODE_STICKER = 2 // 贴纸模式
        
        // 预览区域大小和边距(DP)
        const val PREVIEW_SIZE_DP = 100f
        const val PREVIEW_PADDING_DP = 10f
        
        // 预览显示区域半径（以触摸点为中心，显示多大区域的图片）
        const val PREVIEW_RADIUS_FACTOR = 2.5f // 笔刷宽度的倍数
    }

    // 预览区域像素大小
    private var previewSizePx = 0
    private var previewPaddingPx = 0
    
    // 主要参数
    private var currentMode = MODE_BRUSH // 当前模式
    private var sourceBitmap: Bitmap? = null // 原始图片
    private var workingBitmap: Bitmap? = null // 工作图片
    private var canvasBitmap: Canvas? = null // 画布
    private var tempBitmap: Bitmap? = null // 临时图片，用于羽化过程
    private var tempCanvas: Canvas? = null // 临时画布
    
    // 预览区域相关
    private var previewBitmap: Bitmap? = null // 预览区域Bitmap
    private var previewCanvas: Canvas? = null // 预览区域Canvas
    private val previewRect = Rect() // 预览区域矩形
    private val previewPaint = Paint(Paint.ANTI_ALIAS_FLAG) // 预览区域边框画笔
    private val previewMatrix = Matrix() // 用于缩放到预览尺寸
    private var previewAtLeftTop = true // 预览区域位置：true为左上角，false为右上角
    private var showPreview = false // 是否显示预览区域

    // 画笔设置
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG) // 清除画刷
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) // 用于绘制位图
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG) // 指示圆圈

    // 存储区域
    private val bitmapRect = RectF() // 位图区域
    private val viewRect = RectF() // 视图区域
    private val displayMatrix = Matrix() // 用于控制位图显示
    private val previewSrcRect = Rect() // 预览源区域
    private val previewDstRect = Rect() // 预览目标区域

    // 羽化参数
    private var featherRadius = MAX_FEATHER_RADIUS / 2f // 羽化半径
    private var touchStrokeWidth = 50f // 触摸轨迹宽度
    private var bitmapScale = 1f // 位图缩放比例

    // 触摸状态
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isTouchingInsideBitmap = false // 是否在位图内触摸
    private var shouldDrawCircle = false // 是否绘制指示圆圈

    // 用于绘制羽化效果的参数
    private var currentPath = Path()
    private var inEraserMode = true // 是否在擦除模式
    private var hasPathModified = false // 是否有路径修改

    // 用于绘制背景
    private val alphaGridDrawHelper = AlphaGridDrawHelper()

    // Xfermode常量
    private val clearXfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) // 用于清除像素

    init {
        // 初始化dp值转换为像素
        previewSizePx = dpToPx(PREVIEW_SIZE_DP).toInt()
        previewPaddingPx = dpToPx(PREVIEW_PADDING_DP).toInt()
        
        // 配置用于清除和羽化的画笔
        clearPaint.apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = touchStrokeWidth
            // 核心：设置Xfermode为CLEAR，用于擦除像素
            xfermode = clearXfermode
            // 核心：设置MaskFilter为BlurMaskFilter，实现边缘羽化效果
            maskFilter = BlurMaskFilter(featherRadius, BlurMaskFilter.Blur.NORMAL)
        }

        // 配置用于绘制指示圆圈的画笔
        circlePaint.apply {
            style = Paint.Style.STROKE
            color = Color.BLUE // 蓝色圆圈
            strokeWidth = dpToPx(1f) // 1dp的线宽
        }
        
        // 配置预览区域边框画笔
        previewPaint.apply {
            style = Paint.Style.STROKE
            color = Color.BLACK
            strokeWidth = dpToPx(1f)
        }

        // 设置图层类型以支持Xfermode
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    // dp转换为px
    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }

    /**
     * 设置需要显示的Bitmap
     */
    fun setImageBitmap(bitmap: Bitmap?) {
        sourceBitmap?.recycle() // 回收旧的Bitmap
        sourceBitmap = bitmap
        setupWorkingCanvas()
        setupPreviewCanvas()
        invalidate()
    }

    /**
     * 设置模式（画刷、形状、贴纸）
     */
    fun setMode(mode: Int) {
        currentMode = mode
        invalidate()
    }

    /**
     * 重置或创建工作用的Bitmap和Canvas
     */
    private fun setupWorkingCanvas() {
        if (sourceBitmap == null || width <= 0 || height <= 0) {
            workingBitmap?.recycle()
            workingBitmap = null
            canvasBitmap = null
            tempBitmap?.recycle()
            tempBitmap = null
            displayMatrix.reset()
            return
        }

        // 回收旧的工作Bitmap
        workingBitmap?.recycle()
        tempBitmap?.recycle()

        // 创建新的工作Bitmap，尺寸与源图片相同
        workingBitmap = Bitmap.createBitmap(
            sourceBitmap!!.width,
            sourceBitmap!!.height,
            Bitmap.Config.ARGB_8888
        )
        canvasBitmap = Canvas(workingBitmap!!)

        // 创建临时Bitmap，用于羽化效果
        tempBitmap = Bitmap.createBitmap(
            sourceBitmap!!.width,
            sourceBitmap!!.height,
            Bitmap.Config.ARGB_8888
        )
        tempCanvas = Canvas(tempBitmap!!)

        // 将源图片绘制到工作Bitmap上
        canvasBitmap?.drawBitmap(sourceBitmap!!, 0f, 0f, bitmapPaint)
        
        // 设置bitmap区域
        bitmapRect.set(0f, 0f, workingBitmap!!.width.toFloat(), workingBitmap!!.height.toFloat())

        // 更新显示矩阵
        updateDisplayMatrix()
    }
    
    /**
     * 设置预览区域的Canvas
     */
    private fun setupPreviewCanvas() {
        previewBitmap?.recycle()
        
        // 创建用于预览区域的Bitmap
        previewBitmap = Bitmap.createBitmap(
            previewSizePx,
            previewSizePx,
            Bitmap.Config.ARGB_8888
        )
        previewCanvas = Canvas(previewBitmap!!)
        
        // 初始化预览目标区域
        previewDstRect.set(0, 0, previewSizePx, previewSizePx)
        
        // 更新预览区域位置
        updatePreviewRect()
    }
    
    /**
     * 更新预览区域的位置
     */
    private fun updatePreviewRect() {
        if (width <= 0 || height <= 0) return
        
        if (previewAtLeftTop) {
            // 左上角位置
            previewRect.set(
                previewPaddingPx,
                previewPaddingPx,
                previewPaddingPx + previewSizePx,
                previewPaddingPx + previewSizePx
            )
        } else {
            // 右上角位置
            previewRect.set(
                width - previewPaddingPx - previewSizePx,
                previewPaddingPx,
                width - previewPaddingPx,
                previewPaddingPx + previewSizePx
            )
        }
    }

    /**
     * 更新用于显示workingBitmap的Matrix
     */
    private fun updateDisplayMatrix() {
        if (workingBitmap == null || width <= 0 || height <= 0) return

        displayMatrix.reset()

        val bitmapWidth = workingBitmap!!.width.toFloat()
        val bitmapHeight = workingBitmap!!.height.toFloat()
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        // 计算合适的缩放比例，使bitmap完全适应view
        val scale: Float
        var dx = 0f
        var dy = 0f

        if (bitmapWidth * viewHeight > viewWidth * bitmapHeight) {
            // Bitmap较宽，以宽度为准缩放
            scale = viewWidth / bitmapWidth
            dy = (viewHeight - bitmapHeight * scale) * 0.5f
        } else {
            // Bitmap较高或等比，以高度为准缩放
            scale = viewHeight / bitmapHeight
            dx = (viewWidth - bitmapWidth * scale) * 0.5f
        }

        // 设置缩放和平移到Matrix
        displayMatrix.setScale(scale, scale)
        displayMatrix.postTranslate(dx, dy)
        bitmapScale = scale

        // 更新视图区域
        viewRect.set(dx, dy, dx + bitmapWidth * scale, dy + bitmapHeight * scale)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // View尺寸变化时，重新计算居中位置
        updateDisplayMatrix()
        updatePreviewRect()
    }
    
    /**
     * 检查并调整预览区域位置
     */
    private fun checkAndUpdatePreviewPosition(x: Float, y: Float): Boolean {
        // 检查触摸点是否在预览区域内
        if (previewRect.contains(x.toInt(), y.toInt())) {
            // 触摸点在预览区域内，切换预览区域位置
            previewAtLeftTop = !previewAtLeftTop
            updatePreviewRect()
            invalidate()
            return true
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (workingBitmap == null || canvasBitmap == null) return false
        if (currentMode != MODE_BRUSH) return false  // 仅在画刷模式下处理

        val x = event.x
        val y = event.y
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 开始触摸时显示预览
                showPreview = true
                
                // 检查是否触摸在预览区域
                if (checkAndUpdatePreviewPosition(x, y)) {
                    // 如果触摸在预览区域内，处理完预览位置变化后直接返回
                    return true
                }
                
                // 检查触摸起点是否在Bitmap显示区域内
                if (viewRect.contains(x, y)) {
                    isTouchingInsideBitmap = true

                    // 将触摸坐标从屏幕坐标转换为Bitmap坐标
                    val bitmapPointArray = mapTouchPointToBitmap(x, y)
                    val bitmapX = bitmapPointArray[0]
                    val bitmapY = bitmapPointArray[1]

                    // 记录起始点坐标（屏幕坐标，用于绘制指示圆圈）
                    lastTouchX = x
                    lastTouchY = y

                    // 重置当前路径
                    currentPath = Path()
                    currentPath.moveTo(bitmapX, bitmapY)
                    
                    // 立即在按下的点绘制一个点，避免只点击不滑动时无效果
                    canvasBitmap?.drawPoint(bitmapX, bitmapY, clearPaint)
                    shouldDrawCircle = true // 开始绘制指示圆圈
                    hasPathModified = true
                    
                    // 更新预览内容
                    updatePreviewContent(bitmapX, bitmapY)
                    
                    invalidate()
                    return true // 消费事件
                } else {
                    isTouchingInsideBitmap = false
                    return false // 不在Bitmap内开始的触摸不处理
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // 在移动时也检查是否触摸到预览区域
                if (showPreview && checkAndUpdatePreviewPosition(x, y)) {
                    // 如果触摸到预览区域，只处理预览位置变化并继续处理擦除
                    if (!isTouchingInsideBitmap) {
                        return true
                    }
                }
                
                if (!isTouchingInsideBitmap) return false // 如果触摸不是从内部开始的，移动也不处理

                // 将触摸坐标从屏幕坐标转换为Bitmap坐标
                val currentBitmapPointArray = mapTouchPointToBitmap(x, y)
                val currentBitmapX = currentBitmapPointArray[0]
                val currentBitmapY = currentBitmapPointArray[1]

                // 将上一个触摸点也转换为Bitmap坐标
                val lastBitmapPointArray = mapTouchPointToBitmap(lastTouchX, lastTouchY)
                val lastBitmapX = lastBitmapPointArray[0]
                val lastBitmapY = lastBitmapPointArray[1]

                // 添加到路径中
                currentPath.quadTo(lastBitmapX, lastBitmapY, (currentBitmapX + lastBitmapX) / 2, (currentBitmapY + lastBitmapY) / 2)

                // 在Bitmap上绘制线段，采用擦除模式
                canvasBitmap?.drawLine(lastBitmapX, lastBitmapY, currentBitmapX, currentBitmapY, clearPaint)

                // 更新上一个触摸点（屏幕坐标）
                lastTouchX = x
                lastTouchY = y

                hasPathModified = true
                
                // 更新预览内容
                updatePreviewContent(currentBitmapX, currentBitmapY)
                
                invalidate() // 请求重绘
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 触摸结束时隐藏预览
                showPreview = false
                
                if (!isTouchingInsideBitmap) return false

                // 结束路径并应用擦除效果
                val bitmapPointArray = mapTouchPointToBitmap(x, y)
                currentPath.lineTo(bitmapPointArray[0], bitmapPointArray[1])
                
                // 将路径进行羽化擦除
                applyFeatherErase()
                
                // 重置状态
                isTouchingInsideBitmap = false
                shouldDrawCircle = false
                currentPath.reset()
                
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * 将触摸点的屏幕坐标转换为bitmap内部坐标
     */
    private fun mapTouchPointToBitmap(touchX: Float, touchY: Float): FloatArray {
        val inverseMatrix = Matrix()
        if (!displayMatrix.invert(inverseMatrix)) {
            // 如果Matrix不能求逆，返回原值
            return floatArrayOf(touchX, touchY)
        }

        val point = floatArrayOf(touchX, touchY)
        inverseMatrix.mapPoints(point)
        return point
    }

    /**
     * 应用羽化擦除效果
     */
    private fun applyFeatherErase() {
        if (!hasPathModified) return
        
        tempCanvas?.drawColor(0, PorterDuff.Mode.CLEAR)
        
        // 设置Path绘制时的羽化效果
        clearPaint.style = Paint.Style.STROKE
        clearPaint.strokeWidth = touchStrokeWidth
        clearPaint.maskFilter = BlurMaskFilter(featherRadius, BlurMaskFilter.Blur.NORMAL)
        clearPaint.xfermode = clearXfermode
        
        // 在临时画布上绘制路径
        tempCanvas?.drawPath(currentPath, clearPaint)
        
        // 将临时画布的内容应用到主画布
        canvasBitmap?.drawBitmap(tempBitmap!!, 0f, 0f, bitmapPaint)
        
        hasPathModified = false
    }
    
    /**
     * 更新预览区域的内容，以触摸点为中心
     */
    private fun updatePreviewContent(bitmapX: Float, bitmapY: Float) {
        if (previewCanvas == null || workingBitmap == null) return
        
        // 计算实际擦除半径
        val actualEraseRadius = touchStrokeWidth / 2 + featherRadius
        
        // 计算预览区域的半径 (固定为预览区域大小的一半)
        val previewRadius = previewSizePx / 2f
        
        // 计算预览区域的边界，以触摸点为中心
        var left = (bitmapX - previewRadius / bitmapScale).toInt()
        var top = (bitmapY - previewRadius / bitmapScale).toInt()
        var right = (bitmapX + previewRadius / bitmapScale).toInt()
        var bottom = (bitmapY + previewRadius / bitmapScale).toInt()
        
        // 调整边界，防止超出bitmap范围
        if (left < 0) {
            right -= left
            left = 0
        }
        if (top < 0) {
            bottom -= top
            top = 0
        }
        if (right > workingBitmap!!.width) {
            left -= (right - workingBitmap!!.width)
            right = workingBitmap!!.width
        }
        if (bottom > workingBitmap!!.height) {
            top -= (bottom - workingBitmap!!.height)
            bottom = workingBitmap!!.height
        }
        
        // 再次检查边界
        left = left.coerceAtLeast(0)
        top = top.coerceAtLeast(0)
        right = right.coerceAtMost(workingBitmap!!.width)
        bottom = bottom.coerceAtMost(workingBitmap!!.height)
        
        // 确保预览区域是正方形
        val width = right - left
        val height = bottom - top
        if (width > height) {
            val diff = (width - height) / 2
            left += diff
            right -= diff
        } else if (height > width) {
            val diff = (height - width) / 2
            top += diff
            bottom -= diff
        }
        
        // 设置源矩形
        previewSrcRect.set(left, top, right, bottom)
        
        // 清除预览画布
        previewCanvas?.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR)
        
        // 绘制背景网格
        alphaGridDrawHelper.drawAlphaGrid(previewCanvas!!, RectF(0f, 0f, previewSizePx.toFloat(), previewSizePx.toFloat()))
        
        // 将源区域绘制到预览画布
        previewCanvas?.drawBitmap(
            workingBitmap!!,
            previewSrcRect,
            previewDstRect,
            bitmapPaint
        )
        
        // 绘制指示圆圈
        if (shouldDrawCircle) {
            // 预览区域和源图像的比例
            val previewToSourceRatio = previewSizePx.toFloat() / (right - left)
            
            // 计算触摸点在预览区域中的位置
            val previewX = ((bitmapX - left) * previewToSourceRatio)
            val previewY = ((bitmapY - top) * previewToSourceRatio)
            
            // 使用原图的一致计算方法 - 轨迹宽度/2 + 羽化半径
            val circleRadiusInPreview = actualEraseRadius * previewToSourceRatio
            
            // 在预览中绘制圆圈
            previewCanvas?.drawCircle(previewX, previewY, circleRadiusInPreview, circlePaint)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // 绘制Alpha网格背景（透明部分的棋盘格效果）
        canvas.save()
        canvas.clipRect(viewRect)
        alphaGridDrawHelper.drawAlphaGrid(canvas, viewRect)
        canvas.restore()
        
        // 绘制处理后的Bitmap
        workingBitmap?.let {
            canvas.drawBitmap(it, displayMatrix, null)
        }
        
        // 在触摸位置绘制指示圆圈，半径需要包含羽化区域
        if (shouldDrawCircle && isTouchingInsideBitmap) {
            // 计算正确的半径：轨迹宽度的一半 + 羽化半径
            val actualEraseRadius = touchStrokeWidth / 2 + featherRadius
            
            // 应用与显示相同的缩放比例
            val circleRadius = actualEraseRadius * bitmapScale
            
            canvas.drawCircle(lastTouchX, lastTouchY, circleRadius, circlePaint)
        }
        
        // 只在触摸状态下绘制预览区域
        if (showPreview && previewBitmap != null) {
            canvas.drawBitmap(previewBitmap!!, null, previewRect, null)
            canvas.drawRect(previewRect, previewPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // View销毁时回收Bitmap资源
        sourceBitmap?.recycle()
        sourceBitmap = null
        workingBitmap?.recycle()
        workingBitmap = null
        tempBitmap?.recycle()
        tempBitmap = null
        previewBitmap?.recycle()
        previewBitmap = null
    }

    /**
     * 设置羽化半径（像素）
     */
    fun setFeatherRadius(radius: Float) {
        featherRadius = radius.coerceIn(0f, MAX_FEATHER_RADIUS)
        // 更新画笔的MaskFilter
        clearPaint.maskFilter = if (featherRadius > 0) {
            BlurMaskFilter(featherRadius, BlurMaskFilter.Blur.NORMAL)
        } else {
            null // 设置为null以禁用羽化效果
        }
        invalidate()
    }
    
    /**
     * 设置触摸轨迹的宽度
     */
    fun setTouchStrokeWidth(width: Float) {
        touchStrokeWidth = width
        clearPaint.strokeWidth = touchStrokeWidth
        invalidate()
    }
    
    /**
     * 设置擦除模式
     */
    fun setEraserMode(eraser: Boolean) {
        this.inEraserMode = eraser
        if (eraser) {
            clearPaint.xfermode = clearXfermode
        } else {
            // 可以在这里设置其他绘制模式
        }
    }
}
