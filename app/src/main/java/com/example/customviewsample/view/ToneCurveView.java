package com.example.customviewsample.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.view.ViewCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ToneCurveView 是一个曲线调色控件，可以对 RGB、Red、Green、Blue 四个通道的曲线进行
 * 添加/删除/拖拽节点操作，并实时绘制到界面上。可用于图像的色调校正、曲线调色等。
 */
public class ToneCurveView extends View {

    // 一些颜色常量
    public static final int COLOR_GRID = Color.parseColor("#80ffffff");    // 网格线颜色（半透明白）
    public static final int COLOR_GRID_DIAG = Color.parseColor("#80BAC8DC"); // 对角参考线颜色
    public static final int COLOR_SELECTED_POINT_STROKE = Color.parseColor("#FF2645"); // 选中点的描边颜色
    public static final int COLOR_RGB = Color.parseColor("#ffffff");       // RGB 通道的主颜色（白）
    public static final int COLOR_RED = Color.parseColor("#FF3938");       // R 通道颜色
    public static final int COLOR_GREEN = Color.parseColor("#67E332");     // G 通道颜色
    public static final int COLOR_BLUE = Color.parseColor("#3781FC");      // B 通道颜色

    // 当前是否是拖拽状态 / 是否移动过等
    public boolean hasMoveStarted;
    public boolean isTouchSlopExceeded;

    // 宽高、以及曲线绘制时的一些坐标参数
    public int mWidth;    // View 的宽
    public int mHeight;   // View 的高
    public float mLeftBound;   // 可绘制区域左边界 (一般是 dp2px(20) )
    public float mRightBound;  // 可绘制区域右边界 (mWidth - dp2px(20))

    // 当前的曲线类型 (0=RGB, 1=Red, 2=Green, 3=Blue)
    public int mCurrentCurveType;

    // 内部使用的一些 Path
    public Path mCurrentCurvePath;
    public Path mTempPath;

    // 几个用于绘制的 Paint
    public Paint mCurvePaint;     // 主曲线的画笔
    public Paint mGridPaint;      // 网格的画笔
    public Paint mGrayPaint;      // 画那些额外参考线的 Paint
    public Paint mPointPaint;     // 画节点圆圈的 Paint

    // 用于触摸记录的坐标
    public float mLastTouchX;
    public float mLastTouchY;

    // 记录被选中的节点坐标（down 时存储）
    public PointF mDownPoint = new PointF();

    // 维护的曲线节点列表（这里存的是当前激活的那一条曲线）
    public ArrayList<PointF> mCurrentPoints;

    // 维护所有通道曲线的节点
    private ArrayList<PointF> mRgbPoints;   // 0 = RGB
    private ArrayList<PointF> mRedPoints;   // 1 = R
    private ArrayList<PointF> mGreenPoints; // 2 = G
    private ArrayList<PointF> mBluePoints;  // 3 = B

    // 当前选中的节点索引
    public int mSelectedIndex = -1;
    // 之前 touch 操作临时插入的节点索引（如果有的话）
    public int mTempIndex = -1;

    // 是否需要回调
    public boolean mIsUserEditing = false;
    // 是否第一次初始化后
    public boolean mIsFirstInit = true;

    // 网格/节点半径、曲线粗细等
    public float mPointRadius;     // 节点可点击半径
    public float mGridLineWidth;   // 网格线宽
    public float mCurveLineWidth;  // 主曲线线宽
    public float mSelectStrokeWidth; // 选中节点时描边的线宽

    // View 的监听器（回调接口）
    public OnToneCurveChangeListener mListener;

    /**
     * 用于回调通知：新增/删除/拖拽/完成 等事件的接口
     */
    public interface OnToneCurveChangeListener {
        // 当想添加节点，但节点总数已达上限时
        void onCurvePointsReachMax();

        // 当用户按下某个点或新建一个点时回调 (isNew = true 表示是新插入的点)
        void onPointTouched(PointF normalizedPoint, int curveType, boolean isNew);

        // 当用户拖拽某个点移动过程中，实时回调所有点 (normalizedPoints = 所有在 [0..1] 区间的点)
        void onPointsChanged(PointF[] normalizedPoints, int curveType, boolean isEditing);

        // 当用户松手时 (finish)
        void onEditFinished(boolean changed);

        // 当用户按下时 (准备开始)
        void onEditStart();
    }

    public ToneCurveView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);

        // 初始化各种列表
        mRgbPoints = new ArrayList<>();
        mRedPoints = new ArrayList<>();
        mGreenPoints = new ArrayList<>();
        mBluePoints = new ArrayList<>();

        // 缓存 path
        mCurrentCurvePath = new Path();
        mTempPath = new Path();

        // 初始化画笔
        mCurvePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCurvePaint.setStyle(Paint.Style.STROKE);
        mCurvePaint.setDither(true);
        mCurvePaint.setFilterBitmap(true);
        mCurvePaint.setColor(Color.WHITE);

        mGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGridPaint.setStyle(Paint.Style.STROKE);
        mGridPaint.setDither(true);
        mGridPaint.setFilterBitmap(true);

        mGrayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGrayPaint.setStyle(Paint.Style.STROKE);
        mGrayPaint.setDither(true);
        mGrayPaint.setFilterBitmap(true);

        mPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPointPaint.setStyle(Paint.Style.FILL);

        // 一般从 dp 转 px 的逻辑。这里先简单写死
        // (你可以将 g.c(context, 20.0f) 替换成自定义方法 dp2px(20) )
        mPointRadius = dp2px(10);
        mGridLineWidth = dp2px(1);
        mCurveLineWidth = dp2px(2);
        mSelectStrokeWidth = dp2px(2.4f);
    }

    /**
     * 假装是一个把 dp 转 px 的工具函数
     */
    private float dp2px(float dpValue) {
        float scale = getContext().getResources().getDisplayMetrics().density;
        return dpValue * scale + 0.5f;
    }

    /**
     * 切换当前要操作的曲线类型
     *
     * @param curveType 0=RGB,1=Red,2=Green,3=Blue
     */
    public void setCurCurveType(int curveType) {
        mCurrentCurveType = curveType;
        if (curveType == 0) {
            setCurveColor(COLOR_RGB);
        } else if (curveType == 1) {
            setCurveColor(COLOR_RED);
        } else if (curveType == 2) {
            setCurveColor(COLOR_GREEN);
        } else if (curveType == 3) {
            setCurveColor(COLOR_BLUE);
        }
        // 每次切换都要更新当前曲线引用
        switchCurveList();
        invalidate();
    }

    /**
     * 根据当前曲线类型，切换到对应的 mCurrentPoints
     */
    private void switchCurveList() {
        if (mCurrentCurveType == 0) {
            mCurrentPoints = mRgbPoints;
        } else if (mCurrentCurveType == 1) {
            mCurrentPoints = mRedPoints;
        } else if (mCurrentCurveType == 2) {
            mCurrentPoints = mGreenPoints;
        } else if (mCurrentCurveType == 3) {
            mCurrentPoints = mBluePoints;
        }
        // 如果当前没有点，则默认插入两端点
        if (mCurrentPoints == null) {
            mCurrentPoints = new ArrayList<>();
        }
        if (mCurrentPoints.size() < 2) {
            mCurrentPoints.clear();
            // 左下角
            mCurrentPoints.add(new PointF(mLeftBound, mHeight - mLeftBound));
            // 右上角
            mCurrentPoints.add(new PointF(mWidth - mLeftBound, mLeftBound));
        }
    }

    /**
     * 设置当前曲线画笔颜色
     */
    private void setCurveColor(int color) {
        mCurvePaint.setColor(color);
    }

    /**
     * 在 onMeasure 中记录 View 宽高，以及一些坐标边界
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);

        // 四周预留一些边距
        mLeftBound = dp2px(20);
        mRightBound = mWidth - dp2px(20);
        // ...
        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mWidth == 0 || mHeight == 0) return;

        // 1. 先画网格(纵横各 4 条线) + 对角线
        drawGrid(canvas);

        // 2. 确保当前曲线列表正确
        if (mCurrentPoints == null || mCurrentPoints.size() < 2) {
            switchCurveList();
        }

        // 3. 先画一条对角参考线 (从左下到右上)
        mGridPaint.setColor(COLOR_GRID_DIAG);
        Path diagPath = new Path();
        diagPath.moveTo(mLeftBound, mHeight - mLeftBound);
        diagPath.lineTo(mRightBound, mLeftBound);
        canvas.drawPath(diagPath, mGridPaint);

        // 4. 计算插值后的 Path，并画出当前曲线
        ArrayList<Float> curvePoints = buildInterpolatedCurve(getNormalizedPoints());
        drawCurrentCurve(canvas, curvePoints);

        // 5. 画其它几条曲线(若当前不是 RGB，则把 RGB/Red/Green/Blue 的曲线都半透明画出来)
        drawOtherCurves(canvas);

        // 6. 画所有节点（小圆点）
        drawPoints(canvas);

        // 7. 如果有选中的节点，再多画一个内圈
        drawSelectedPoint(canvas);

        // 8. 如果在用户交互，可能要回调
        if (mListener != null && !mIsFirstInit) {
            if (mIsUserEditing) {
                // 通知坐标更新
                mListener.onPointsChanged(getNormalizedPoints(), mCurrentCurveType, isCurrentPointEdge());
                // 标志位复位
                mIsUserEditing = false;
            }
        }
        if (mIsFirstInit) {
            // 第一次不回调
            mIsFirstInit = false;
        }
    }

    /**
     * 画网格线
     */
    private void drawGrid(Canvas canvas) {
        mGridPaint.setColor(COLOR_GRID);
        mGridPaint.setStrokeWidth(mGridLineWidth);

        RectF rect = new RectF(mLeftBound, mLeftBound, mRightBound, mHeight - mLeftBound);
        canvas.drawRect(rect, mGridPaint);

        // 垂直方向上画 3 根网格线(把矩形分成四份)
        float perWidth = (rect.width() / 4f);
        for (int i = 1; i < 4; i++) {
            float x = mLeftBound + perWidth * i;
            canvas.drawLine(x, mLeftBound, x, mHeight - mLeftBound, mGridPaint);
        }

        // 水平方向上画 3 根网格线
        float perHeight = (rect.height() / 4f);
        for (int i = 1; i < 4; i++) {
            float y = mLeftBound + perHeight * i;
            canvas.drawLine(mLeftBound, y, mRightBound, y, mGridPaint);
        }
    }

    /**
     * 将当前曲线节点(PointF)转为 [0..1] 的相对坐标
     */
    public PointF[] getNormalizedPoints() {
        if (mCurrentPoints == null || mCurrentPoints.isEmpty()) {
            return null;
        }
        PointF[] result = new PointF[mCurrentPoints.size()];
        for (int i = 0; i < mCurrentPoints.size(); i++) {
            PointF p = mCurrentPoints.get(i);
            // X: [mLeftBound .. (mWidth - mLeftBound)]
            float nx = (p.x - mLeftBound) / (mWidth - 2 * mLeftBound);
            // Y: [mLeftBound .. (mHeight - mLeftBound)] 但要翻转 Y 方向
            float ny = ((mHeight - mLeftBound) - p.y) / (mHeight - 2 * mLeftBound);
            // clamp
            nx = Math.max(0, Math.min(nx, 1f));
            ny = Math.max(0, Math.min(ny, 1f));
            result[i] = new PointF(nx, ny);
        }
        return result;
    }

    /**
     * 将一组 [0..1] 坐标的曲线做插值，得到更平滑的曲线点
     * 这里原本代码用了类似 Catmull-Rom 的方式进行平滑，你可以自行改成三次贝塞尔等。
     *
     * @param normalizedPoints 归一化后的点
     * @return 插值结果，存储在一个 ArrayList 中 (x0, y0, x1, y1, x2, y2, ...)
     */
    private ArrayList<Float> buildInterpolatedCurve(PointF[] normalizedPoints) {
        if (normalizedPoints == null || normalizedPoints.length < 2) {
            return new ArrayList<>();
        }
        // 原代码里把首尾延伸了 -0.001f、1.001f 来避免边界影响
        // 这里保留逻辑
        ArrayList<PointF> pts = new ArrayList<>(Arrays.asList(normalizedPoints));
        pts.add(0, new PointF(-0.001f, normalizedPoints[0].y));
        pts.add(new PointF(1.001f, normalizedPoints[normalizedPoints.length - 1].y));

        ArrayList<Float> result = new ArrayList<>();
        // 这里的 50 是插值采样数量，可自行调大或调小
        int STEP_COUNT = 10;

        for (int i = 1; i < pts.size() - 2; i++) {
            PointF p0 = pts.get(i - 1);
            PointF p1 = pts.get(i);
            PointF p2 = pts.get(i + 1);
            PointF p3 = pts.get(i + 2);
            for (int step = 1; step < STEP_COUNT; step++) {
                // t 取值范围 [0..1]
                float t = (float) step / STEP_COUNT;
                // Catmull-Rom 或类似插值
                float t2 = t * t;
                float t3 = t2 * t;

                // 计算 x
                float x = 0.5f * (
                        (2f * p1.x) +
                                (-p0.x + p2.x) * t +
                                (2f * p0.x - 5f * p1.x + 4f * p2.x - p3.x) * t2 +
                                (-p0.x + 3f * p1.x - 3f * p2.x + p3.x) * t3
                );
                // 计算 y
                float y = 0.5f * (
                        (2f * p1.y) +
                                (-p0.y + p2.y) * t +
                                (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * t2 +
                                (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * t3
                );

                // clamp 到 [0..1]
                x = Math.max(0, Math.min(1f, x));
                y = Math.max(0, Math.min(1f, y));

                // 放进 result
                result.add(x);
                result.add(y);
            }
            // 追加 p2.x, p2.y
            result.add(p2.x);
            result.add(p2.y);
        }
        return result;
    }

    /**
     * 将插值后的 [0..1] 数据转为实际画布坐标后画出来
     */
    private void drawCurrentCurve(Canvas canvas, ArrayList<Float> curveData) {
        mCurrentCurvePath.reset();
        // mCurvePaint 已经由 setCurveColor(...) 设置颜色
        mCurvePaint.setStrokeWidth(mCurveLineWidth);

        for (int i = 0; i < curveData.size() / 2; i++) {
            float nx = curveData.get(i * 2);      // 0..1
            float ny = curveData.get(i * 2 + 1);  // 0..1
            float px = mLeftBound + nx * (mWidth - 2 * mLeftBound);
            float py = (mHeight - mLeftBound) - ny * (mHeight - 2 * mLeftBound);
            if (i == 0) {
                mCurrentCurvePath.moveTo(px, py);
            } else {
                mCurrentCurvePath.lineTo(px, py);
            }
        }
        canvas.drawPath(mCurrentCurvePath, mCurvePaint);
    }

    /**
     * 画其他几条曲线 (如果当前是操作 R 通道, 则把 G/B/RGB 的都以半透明方式画上去作参考)
     */
    private void drawOtherCurves(Canvas canvas) {
        PointF[] redPoints = toNormalized(mRedPoints);
        PointF[] greenPoints = toNormalized(mGreenPoints);
        PointF[] bluePoints = toNormalized(mBluePoints);
        PointF[] rgbPoints = toNormalized(mRgbPoints);

        // 这里为了区分不同通道，可能要用不透明度 0.5f ~ 0.7f 等
        int alphaRed = 0x80FF3938; // #80FF3938
        int alphaGreen = 0x8067E332;
        int alphaBlue = 0x803781FC;
        int alphaWhite = 0x80ffffff;

        // 根据当前通道，画剩下三条通道的曲线
        switch (mCurrentCurveType) {
            case 0: // 如果当前是 RGB
                // 画 R/G/B
                drawSubCurve(canvas, redPoints, alphaRed);
                drawSubCurve(canvas, greenPoints, alphaGreen);
                drawSubCurve(canvas, bluePoints, alphaBlue);
                break;
            case 1: // 如果当前是 Red
                // 画 RGB/G/B
                drawSubCurve(canvas, rgbPoints, alphaWhite);
                drawSubCurve(canvas, greenPoints, alphaGreen);
                drawSubCurve(canvas, bluePoints, alphaBlue);
                break;
            case 2: // 如果当前是 Green
                // 画 RGB/R/B
                drawSubCurve(canvas, rgbPoints, alphaWhite);
                drawSubCurve(canvas, redPoints, alphaRed);
                drawSubCurve(canvas, bluePoints, alphaBlue);
                break;
            case 3: // 如果当前是 Blue
                // 画 RGB/R/G
                drawSubCurve(canvas, rgbPoints, alphaWhite);
                drawSubCurve(canvas, redPoints, alphaRed);
                drawSubCurve(canvas, greenPoints, alphaGreen);
                break;
        }
    }

    /**
     * 将 List<PointF> 转为 PointF[] (归一化坐标)
     */
    private PointF[] toNormalized(List<PointF> list) {
        if (list == null || list.isEmpty()) return null;
        PointF[] arr = new PointF[list.size()];
        for (int i = 0; i < list.size(); i++) {
            PointF p = list.get(i);
            float nx = (p.x - mLeftBound) / (mWidth - 2 * mLeftBound);
            float ny = ((mHeight - mLeftBound) - p.y) / (mHeight - 2 * mLeftBound);
            nx = Math.max(0, Math.min(1f, nx));
            ny = Math.max(0, Math.min(1f, ny));
            arr[i] = new PointF(nx, ny);
        }
        return arr;
    }

    /**
     * 对某条曲线进行插值并以指定颜色绘制 (半透明)
     */
    private void drawSubCurve(Canvas canvas, PointF[] points, int color) {
        if (points == null || points.length < 2) return;
        ArrayList<Float> data = buildInterpolatedCurve(points);
        Paint tempPaint = new Paint(mCurvePaint);
        tempPaint.setColor(color);
        tempPaint.setStrokeWidth(mCurveLineWidth);

        mTempPath.reset();
        for (int i = 0; i < data.size() / 2; i++) {
            float nx = data.get(i * 2);
            float ny = data.get(i * 2 + 1);
            float px = mLeftBound + nx * (mWidth - 2 * mLeftBound);
            float py = (mHeight - mLeftBound) - ny * (mHeight - 2 * mLeftBound);
            if (i == 0) {
                mTempPath.moveTo(px, py);
            } else {
                mTempPath.lineTo(px, py);
            }
        }
        canvas.drawPath(mTempPath, tempPaint);
    }

    /**
     * 画当前曲线上的所有节点
     */
    private void drawPoints(Canvas canvas) {
        if (mCurrentPoints == null) return;
        for (int i = 0; i < mCurrentPoints.size(); i++) {
            PointF point = mCurrentPoints.get(i);
            // 外圈白色
            mPointPaint.setColor(Color.WHITE);
            canvas.drawCircle(point.x, point.y, mPointRadius / 2f, mPointPaint);
        }
    }

    /**
     * 为被选中的那个点画一个内圈
     */
    private void drawSelectedPoint(Canvas canvas) {
        if (mSelectedIndex >= 0 && mSelectedIndex < mCurrentPoints.size()) {
            PointF selected = mCurrentPoints.get(mSelectedIndex);
            // 内圈
            mPointPaint.setStyle(Paint.Style.FILL);
            if (mCurrentCurveType == 0) {
                mPointPaint.setColor(COLOR_SELECTED_POINT_STROKE); // #FF2645
            } else {
                mPointPaint.setColor(mCurvePaint.getColor());
            }
            canvas.drawCircle(selected.x, selected.y, (mPointRadius / 2f) - mSelectStrokeWidth, mPointPaint);

            // 画完之后记得恢复一下 Style
            mPointPaint.setStyle(Paint.Style.FILL);
        }
    }

    /**
     * 是否当前选中的是首尾点
     */
    private boolean isCurrentPointEdge() {
        return mSelectedIndex == 0 || mSelectedIndex == mCurrentPoints.size() - 1;
    }

    /**
     * onTouch 事件，处理点击新增节点、拖拽节点、松手回调等逻辑
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                hasMoveStarted = false;
                isTouchSlopExceeded = false;
                mTempIndex = -1;

                mLastTouchX = x;
                mLastTouchY = y;

                // 检查有没有点被选中
                mSelectedIndex = findPointIndex(x, y);
                if (mListener != null) {
                    // 如果点的数量过多(比如 >= 12)，就不能新增
                    if (mCurrentPoints.size() >= 12 && mSelectedIndex == -1) {
                        mListener.onCurvePointsReachMax();
                    }
                    mListener.onEditStart();
                }

                // 如果未选中任何点且数量未达上限，则可能要新增点
                if (mSelectedIndex == -1 && mCurrentPoints.size() < 12) {
                    // 查找插入位置
                    insertPointIfInRange(x, y);
                } else {
                    // 记录下按下时的坐标
                    if (mSelectedIndex != -1) {
                        PointF sel = mCurrentPoints.get(mSelectedIndex);
                        mDownPoint.x = sel.x;
                        mDownPoint.y = sel.y;
                    }
                }
                ViewCompat.postInvalidateOnAnimation(this); // 刷新
                break;

            case MotionEvent.ACTION_MOVE:
                if (!hasMoveStarted) {
                    // 移动距离大于 10px 就认为开始拖动了
                    if (Math.abs(x - mLastTouchX) > 10 || Math.abs(y - mLastTouchY) > 10) {
                        hasMoveStarted = true;
                    }
                }

                if (hasMoveStarted) {
                    // 拖拽选中的点
                    if (mSelectedIndex != -1) {
                        // 边界检查
                        x = clamp(x, mLeftBound, mRightBound);
                        y = clamp(y, mLeftBound, mHeight - mLeftBound);

                        // 首点和尾点强制固定 X
                        if (mSelectedIndex == 0) {
                            x = mLeftBound;
                        } else if (mSelectedIndex == mCurrentPoints.size() - 1) {
                            x = mRightBound;
                        } else {
                            // 保证点不越过相邻点
                            clampBetweenNeighbors(x);
                        }

                        mCurrentPoints.set(mSelectedIndex, new PointF(x, y));
                        mIsUserEditing = true;
                        if (mListener != null) {
                            // 通知 onPointTouched(...) 之类，可根据需要调用
                            mListener.onPointTouched(toNormalizedPoint(x, y), mCurrentCurveType, isCurrentPointEdge());
                        }
                    }
                    ViewCompat.postInvalidateOnAnimation(this);
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mListener != null) {
                    if (mSelectedIndex >= 0 && mSelectedIndex < mCurrentPoints.size()) {
                        PointF sel = mCurrentPoints.get(mSelectedIndex);
                        boolean changed = !(sel.x == mDownPoint.x && sel.y == mDownPoint.y);
                        mListener.onEditFinished(changed);
                    } else {
                        mListener.onEditFinished(false);
                    }
                }
                ViewCompat.postInvalidateOnAnimation(this);
                break;
        }

        return true;
    }

    /**
     * 查找当前点击位置附近，是否存在已添加的节点
     *
     * @return 若找到，返回其索引，否则 -1
     */
    private int findPointIndex(float x, float y) {
        if (mCurrentPoints != null) {
            for (int i = 0; i < mCurrentPoints.size(); i++) {
                PointF pf = mCurrentPoints.get(i);
                if (Math.abs(x - pf.x) <= mPointRadius && Math.abs(y - pf.y) <= mPointRadius) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 如果点击在相邻两点之间，则插入一个新的点
     */
    private void insertPointIfInRange(float x, float y) {
        for (int i = 0; i < mCurrentPoints.size() - 1; i++) {
            PointF p1 = mCurrentPoints.get(i);
            PointF p2 = mCurrentPoints.get(i + 1);
            if (x > (p1.x + mPointRadius) && x < (p2.x - mPointRadius)) {
                // 把新点插进去
                mSelectedIndex = i + 1;
                y = clamp(y, mLeftBound, mHeight - mLeftBound);
                mCurrentPoints.add(mSelectedIndex, new PointF(x, y));
                mIsUserEditing = true;
                if (mListener != null) {
                    mListener.onPointTouched(toNormalizedPoint(x, y), mCurrentCurveType, true);
                }
                return;
            }
        }
    }

    /**
     * 把屏幕坐标转换为 [0..1] 范围的坐标
     */
    private PointF toNormalizedPoint(float x, float y) {
        float nx = (x - mLeftBound) / (mWidth - 2 * mLeftBound);
        float ny = ((mHeight - mLeftBound) - y) / (mHeight - 2 * mLeftBound);
        nx = Math.max(0, Math.min(nx, 1f));
        ny = Math.max(0, Math.min(ny, 1f));
        return new PointF(nx, ny);
    }

    /**
     * 边界夹紧
     */
    private float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(val, max));
    }

    /**
     * 让当前选中的点不能越过前后两个点 (除了首尾点)
     */
    private void clampBetweenNeighbors(float x) {
        // 假设 mSelectedIndex 在 (1 .. size-2) 之间
        if (mSelectedIndex < 1 || mSelectedIndex > mCurrentPoints.size() - 2) {
            return;
        }
        PointF left = mCurrentPoints.get(mSelectedIndex - 1);
        PointF right = mCurrentPoints.get(mSelectedIndex + 1);
        if (x <= left.x + mPointRadius) {
            mCurrentPoints.get(mSelectedIndex).x = left.x + mPointRadius;
        } else if (x >= right.x - mPointRadius) {
            mCurrentPoints.get(mSelectedIndex).x = right.x - mPointRadius;
        }
    }

    // ============ Setter / Getter for external usage ============

    public void setPointsRGB(List<PointF> list) {
        if (list != null) {
            mRgbPoints.clear();
            mRgbPoints.addAll(list);
        }
    }

    public void setPointsRed(List<PointF> list) {
        if (list != null) {
            mRedPoints.clear();
            mRedPoints.addAll(list);
        }
    }

    public void setPointsGreen(List<PointF> list) {
        if (list != null) {
            mGreenPoints.clear();
            mGreenPoints.addAll(list);
        }
    }

    public void setPointsBlue(List<PointF> list) {
        if (list != null) {
            mBluePoints.clear();
            mBluePoints.addAll(list);
        }
    }

    public ArrayList<PointF> getPointsRGB() {
        return mRgbPoints;
    }

    public ArrayList<PointF> getPointsRed() {
        return mRedPoints;
    }

    public ArrayList<PointF> getPointsGreen() {
        return mGreenPoints;
    }

    public ArrayList<PointF> getPointsBlue() {
        return mBluePoints;
    }

    public int getCurrentCurveType() {
        return mCurrentCurveType;
    }

    /**
     * 设置监听器
     */
    public void setOnToneCurveChangeListener(OnToneCurveChangeListener listener) {
        mListener = listener;
    }
}
