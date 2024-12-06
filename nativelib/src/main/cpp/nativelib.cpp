#include <jni.h>
#include <string>
#include <opencv2/core.hpp>
#include <android/bitmap.h>
#include "opencv2/imgproc.hpp"

extern "C"
JNIEXPORT jobject JNICALL
Java_com_sqsong_nativelib_NativeLib_getBitmapOutlinePath(JNIEnv *env, jobject thiz, jobject bitmap) {
// 将 Android Bitmap 转换为 OpenCV Mat
    AndroidBitmapInfo info;
    void *pixels = nullptr;
    if (AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS) {
        // 错误处理
        return nullptr;
    }

    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
        // 错误处理
        return nullptr;
    }

    cv::Mat src(info.height, info.width, CV_8UC4, pixels);

    // 创建一个比原始图像稍大的新图像，周围填充 1 个像素的透明边框
    int borderSize = 1;  // 可以根据需要调整边框大小
    cv::Mat paddedSrc;
    cv::copyMakeBorder(src, paddedSrc, borderSize, borderSize, borderSize, borderSize, cv::BORDER_CONSTANT, cv::Scalar(0, 0, 0, 0));

    // 提取 Alpha 通道
    cv::Mat alphaChannel;
    cv::extractChannel(paddedSrc, alphaChannel, 3); // 提取 Alpha 通道

    // 将 Alpha 通道转换为二值图像
    cv::Mat binary;
    cv::threshold(alphaChannel, binary, 127, 255, cv::THRESH_BINARY);

    // 查找轮廓
    std::vector<std::vector<cv::Point>> contours;
    cv::findContours(binary, contours, cv::RETR_CCOMP, cv::CHAIN_APPROX_SIMPLE);

    // 创建 Java 中的 Path 对象
    jclass pathClass = env->FindClass("android/graphics/Path");
    jmethodID pathConstructor = env->GetMethodID(pathClass, "<init>", "()V");
    jobject path = env->NewObject(pathClass, pathConstructor);

    jmethodID moveToMethod = env->GetMethodID(pathClass, "moveTo", "(FF)V");
    jmethodID lineToMethod = env->GetMethodID(pathClass, "lineTo", "(FF)V");
    jmethodID closeMethod = env->GetMethodID(pathClass, "close", "()V");

    // 将轮廓转换为 Path
    for (const auto &contour: contours) {
        if (contour.empty()) continue;
        // 移动到第一个点（注意坐标需要减去边框大小）
        env->CallVoidMethod(path, moveToMethod, (jfloat) (contour[0].x - borderSize), (jfloat) (contour[0].y - borderSize));
        for (size_t i = 1; i < contour.size(); ++i) {
            env->CallVoidMethod(path, lineToMethod, (jfloat) (contour[i].x - borderSize), (jfloat) (contour[i].y - borderSize));
        }
        env->CallVoidMethod(path, closeMethod);
    }

    AndroidBitmap_unlockPixels(env, bitmap);
    return path;
}