#include <jni.h>
#include <string>
#include <android/log.h>
#include <opencv2/core.hpp>
#include <android/bitmap.h>
#include "opencv2/imgproc.hpp"
#include "LibAlpha.h"

#define LOG_TAG "NativeCutout"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static jobject createBitmap(JNIEnv *env, int width, int height) {
    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    jmethodID createBitmapMethod = env->GetStaticMethodID(bitmapClass, "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    jclass configClass = env->FindClass("android/graphics/Bitmap$Config");
    jfieldID argb8888Field = env->GetStaticFieldID(configClass, "ARGB_8888", "Landroid/graphics/Bitmap$Config;");
    jobject argb8888Config = env->GetStaticObjectField(configClass, argb8888Field);
    jobject outputBitmap = env->CallStaticObjectMethod(bitmapClass, createBitmapMethod, width, height, argb8888Config);
    return outputBitmap;
}

static bool bitmapToRGBA(JNIEnv *env, jobject bitmap, cv::Mat &mat) {
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        LOGE("Failed to get bitmap info");
        return false;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap is not RGBA_8888 format");
        return false;
    }

    void *pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        LOGE("Failed to lock pixels");
        return false;
    }

    cv::Mat tmp(info.height, info.width, CV_8UC4, pixels);
    tmp.copyTo(mat); // 深拷贝
    AndroidBitmap_unlockPixels(env, bitmap);
    return true;
}

// 将Bitmap转换为内存中的图像数据(RGBA)
static bool bitmapToRGBA(JNIEnv *env, jobject bitmap, uint8_t **outData, int &width, int &height, int &stride) {
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        LOGE("Failed to get bitmap info");
        return false;
    }
    width = info.width;
    height = info.height;
    // 仅支持ARGB_8888格式
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format not RGBA_8888");
        return false;
    }

    stride = info.stride;

    void *pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        LOGE("Failed to lock bitmap pixels");
        return false;
    }

    // 不拷贝的话，需要在使用完后unlockPixels
    // 为了简单，这里拷贝一份数据，以便后续处理
    int dataSize = stride * height;
    *outData = new uint8_t[dataSize];
    memcpy(*outData, pixels, dataSize);

    AndroidBitmap_unlockPixels(env, bitmap);
    return true;
}

// 将内存中的RGBA数据写回Bitmap
static void rgbaToBitmap(JNIEnv *env, jobject bitmap, uint8_t *data, int stride, int height) {
    void *pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        LOGE("Failed to lock bitmap pixels");
        return;
    }
    memcpy(pixels, data, stride * height);
    AndroidBitmap_unlockPixels(env, bitmap);
}

static cv::Mat bitmapToMat(JNIEnv *env, jobject bitmap) {
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        LOGE("Failed to get bitmap info");
        return {};
    }

    void *pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        LOGE("Failed to lock bitmap pixels");
        return {};
    }

    cv::Mat mat;
    // Assuming ARGB_8888
    if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        mat = cv::Mat(info.height, info.width, CV_8UC4, pixels);
    } else {
        LOGE("Unsupported bitmap format");
    }

    // 创建一个深拷贝，以便unlock后仍然能使用
    cv::Mat matCopy = mat.clone();
    AndroidBitmap_unlockPixels(env, bitmap);
    return matCopy;
}

/**
 * 将 Android Bitmap (ARGB_8888) 锁定并封装为 cv::Mat
 * 注意：需要在用完后 unlock
 *
 * @param env       JNI 环境
 * @param bitmapObj Java 层传进来的 Bitmap
 * @param mat       输出的 cv::Mat (CV_8UC4)
 * @param needCopy  若为 true，则会将像素拷贝到 mat；否则 mat 直接映射到 bitmap 内存 (仅适合只读场景)
 * @return 0 表示成功，非0表示失败
 */
static int bitmapToMat(JNIEnv *env, jobject bitmapObj, cv::Mat &mat, bool needCopy) {
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmapObj, &info) < 0) {
        return -1;
    }
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        // 需要 ARGB_8888 格式
        return -2;
    }

    void *bitmapPixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmapObj, &bitmapPixels) < 0) {
        return -3;
    }
    if (!bitmapPixels) {
        AndroidBitmap_unlockPixels(env, bitmapObj);
        return -4;
    }

    // 构造一个 Mat 并使用 bitmapPixels 作为数据指针
    mat = cv::Mat(info.height, info.width, CV_8UC4, bitmapPixels);

    // 如果需要做只读操作且处理后还要写回图像，可在处理后再拷贝。
    // 这里给出一个选项 needCopy:
    if (needCopy) {
        // 拷贝一份到新的 mat (不会改动原图内容)
        mat.copyTo(mat.clone());
    }

    // 注意：还没有 unlockPixels，调用者如果只是想只读访问，可处理完后再 unlock
    // 如果要立刻 unlock 可在这里 mat.clone() 后 unlock
    return 0;
}

static void matToBitmap(JNIEnv *env, const cv::Mat &mat, jobject bitmap) {
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        LOGE("Failed to get bitmap info");
        return;
    }

    void *pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        LOGE("Failed to lock bitmap pixels");
        return;
    }

    cv::Mat dst(info.height, info.width, CV_8UC4, pixels);
    mat.copyTo(dst);

    AndroidBitmap_unlockPixels(env, bitmap);
}

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

//extern "C"
//JNIEXPORT jobject JNICALL
//Java_com_sqsong_nativelib_NativeLib_cutoutBitmapBySource(JNIEnv *env, jobject thiz, jobject cutout_bitmap, jobject src_bitmap) {
//    // 将Bitmap转换为Mat
//    cv::Mat cutoutMat = bitmapToMat(env, cutout_bitmap);
//    cv::Mat srcMat = bitmapToMat(env, src_bitmap);
//
//    if (cutoutMat.empty() || srcMat.empty()) {
//        LOGE("Either cutoutMat or srcMat is empty.");
//        return nullptr;
//    }
//
//    // 确保两个Mat尺寸、通道数一致
//    if (cutoutMat.size() != srcMat.size()) {
//        LOGE("cutoutMat and srcMat size mismatch.");
//        return nullptr;
//    }
//
//    // 创建输出Mat
//    cv::Mat outputMat = srcMat.clone();
//    // 遍历每个像素，用cutoutMat的alpha替换outputMat的alpha
//    for (int y = 0; y < cutoutMat.rows; y++) {
//        for (int x = 0; x < cutoutMat.cols; x++) {
//            auto &cutoutPixel = cutoutMat.at<cv::Vec4b>(y, x);
//            auto &srcPixel = srcMat.at<cv::Vec4b>(y, x);
//            auto &outPixel = outputMat.at<cv::Vec4b>(y, x);
//            // RGB来自srcBitmap
//            outPixel[0] = srcPixel[0]; // B
//            outPixel[1] = srcPixel[1]; // G
//            outPixel[2] = srcPixel[2]; // R
//            // Alpha来自cutoutBitmap
//            outPixel[3] = cutoutPixel[3];
//        }
//    }
//
//    // 创建输出Bitmap
//    jobject outputBitmap = createBitmap(env, outputMat.cols, outputMat.rows);
//    matToBitmap(env, outputMat, outputBitmap);
//    return outputBitmap;
//}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_sqsong_nativelib_NativeLib_cutoutBitmapBySource(JNIEnv *env, jobject thiz, jobject cutoutBitmap, jobject srcBitmap) {
    if (srcBitmap == nullptr || cutoutBitmap == nullptr) {
        LOGE("Null input bitmaps");
        return nullptr;
    }

    int srcWidth, srcHeight, srcStride;
    uint8_t *srcData = nullptr;
    if (!bitmapToRGBA(env, srcBitmap, &srcData, srcWidth, srcHeight, srcStride)) {
        LOGE("Failed to load srcBitmap");
        return nullptr;
    }

    int cutWidth, cutHeight, cutStride;
    uint8_t *cutData = nullptr;
    if (!bitmapToRGBA(env, cutoutBitmap, &cutData, cutWidth, cutHeight, cutStride)) {
        LOGE("Failed to load cutoutBitmap");
        delete[] srcData;
        return nullptr;
    }

    if (srcWidth != cutWidth || srcHeight != cutHeight) {
        LOGE("Size mismatch");
        delete[] srcData;
        delete[] cutData;
        return nullptr;
    }
    // 输出数据缓冲区
    auto *outData = new uint8_t[srcStride * srcHeight];
    float maxRatio = 5.0f; // 限制最大放大倍数，防止噪点过度放大
    // 单次遍历所有像素
    // srcData和cutData均为 RGBA 格式：B,G,R,A
    // outData最终要输出为经过"反预乘"恢复后的前景图。
    int pixelCount = srcWidth * srcHeight;
    for (int i = 0; i < pixelCount; i++) {
        // 通道顺序为：BGRA
        uint8_t B_s = srcData[i * 4 + 0];
        uint8_t G_s = srcData[i * 4 + 1];
        uint8_t R_s = srcData[i * 4 + 2];
        // A_s无关，因为srcBitmap是JPG转的，无有效透明信息
        // 我们真正关心的Alpha来自cutoutBitmap
        uint8_t A_c = cutData[i * 4 + 3];
        float A = A_c / 255.0f;
        float Bf = B_s / 255.0f;
        float Gf = G_s / 255.0f;
        float Rf = R_s / 255.0f;
        float FR, FG, FB;
        if (A > 0.001f) {
            float ratio = 1.0f / A;
            if (ratio > maxRatio) ratio = maxRatio;
            // F ≈ I / A
            FB = Bf * ratio;
            FG = Gf * ratio;
            FR = Rf * ratio;
        } else {
            // Alpha近乎0，直接保留原色或设为黑色
            FB = Bf;
            FG = Gf;
            FR = Rf;
        }
        // 最终输出 = F * A
        FB *= A;
        FG *= A;
        FR *= A;
        outData[i * 4 + 0] = (uint8_t) (FB * 255.0f);
        outData[i * 4 + 1] = (uint8_t) (FG * 255.0f);
        outData[i * 4 + 2] = (uint8_t) (FR * 255.0f);
        outData[i * 4 + 3] = A_c;
    }
    jobject outputBitmap = createBitmap(env, srcWidth, srcHeight);
    rgbaToBitmap(env, outputBitmap, outData, srcStride, srcHeight);
    delete[] srcData;
    delete[] cutData;
    delete[] outData;
    return outputBitmap;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_sqsong_nativelib_NativeLib_hasAlpha(JNIEnv *env, jobject thiz, jobject bitmap) {
    // 获取Bitmap基本信息
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        return JNI_FALSE; // 无法获取信息则直接返回
    }

    // 这里要求bitmap为ARGB_8888格式，这样必然有Alpha通道数据
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        return JNI_FALSE;
    }

    // 锁定bitmap的像素进行操作
    void *bitmapPixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &bitmapPixels) < 0) {
        return JNI_FALSE;
    }

    // 使用bitmap的数据创建OpenCV的Mat对象，不拷贝数据，直接指向同一块内存
    cv::Mat mat(info.height, info.width, CV_8UC4, bitmapPixels);

    // 分离通道，以获得Alpha通道
    std::vector<cv::Mat> channels;
    cv::split(mat, channels); // channels[3]为Alpha通道

    // 解锁像素，后面不再需要直接访问bitmapPixels
    AndroidBitmap_unlockPixels(env, bitmap);

    // 利用OpenCV的minMaxLoc快速找到Alpha通道的最小值和最大值
    double minVal = 0, maxVal = 0;
    cv::minMaxLoc(channels[3], &minVal, &maxVal);

    // 若最小Alpha值小于255，说明存在至少一个像素不是完全不透明(有透明度)
    if (minVal < 255.0) {
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_sqsong_nativelib_NativeLib_nativeOutlineBitmap(JNIEnv *env, jobject thiz, jobject srcBitmap, jobject destBitmap, jint strokeWidth, jfloat blurRadius, jint strokeColor) {
    // 1. 读取 srcBitmap 到 Mat
    AndroidBitmapInfo srcInfo;
    if (AndroidBitmap_getInfo(env, srcBitmap, &srcInfo) != ANDROID_BITMAP_RESULT_SUCCESS) {
        return;
    }
    if (srcInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        return; // 必须是 RGBA_8888
    }

    void* srcPixels = nullptr;
    if (AndroidBitmap_lockPixels(env, srcBitmap, &srcPixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
        return;
    }
    cv::Mat srcMat(srcInfo.height, srcInfo.width, CV_8UC4, srcPixels);

    // 2. 为了避免贴边无法描边，我们加留白
    int padding = strokeWidth + (int)ceil(blurRadius);
    if (padding < 1) padding = 1; // 至少留白1像素
    cv::Mat padded;
    // copyMakeBorder(源, 目标, top, bottom, left, right, borderType, 填充值)
    // 对 RGBA，可以用 Scalar(0,0,0,0) 表示全透明
    copyMakeBorder(srcMat, padded, padding, padding, padding, padding,
                   cv::BORDER_CONSTANT, cv::Scalar(0,0,0,0));

    // 解锁 srcBitmap (我们已经复制到 padded)
    AndroidBitmap_unlockPixels(env, srcBitmap);

    // 3. 分离 alpha 通道 & 二值化
    std::vector<cv::Mat> channels;
    split(padded, channels);  // [0]=B, [1]=G, [2]=R, [3]=A
    cv::Mat alpha = channels[3];

    cv::Mat mask;
    threshold(alpha, mask, 128, 255, cv::THRESH_BINARY);

    // 4. 形态学膨胀 (dilate)
    if (strokeWidth > 0) {
        int kernelSize = strokeWidth * 2 + 1;
        cv::Mat kernel = getStructuringElement(cv::MORPH_ELLIPSE, cv::Size(kernelSize, kernelSize));
        dilate(mask, mask, kernel);
    }

    // 5. 高斯模糊
    if (blurRadius > 0.f) {
        float sigma = blurRadius;
        int kSize = std::max(1, (int)(sigma * 4)) | 1; // 通常取4*sigma并保证奇数
        GaussianBlur(mask, mask, cv::Size(kSize, kSize), sigma);
    }

    // 6. 用 mask 构造"描边层"
    // strokeColorRGBA 是 0xAARRGGBB
    uchar A = (strokeColor >> 24) & 0xFF;
    uchar R = (strokeColor >> 16) & 0xFF;
    uchar G = (strokeColor >>  8) & 0xFF;
    uchar B = (strokeColor      ) & 0xFF;
    // 创建 strokeMat (padded同大小) 填充 strokeColor
    cv::Mat strokeMat(padded.rows, padded.cols, CV_8UC4, cv::Scalar(B, G, R, A));

    // 替换 alpha 通道 = mask
    std::vector<cv::Mat> strokeChans;
    split(strokeMat, strokeChans);  // strokeChans[0]=B,1=G,2=R,3=A
    strokeChans[3] = mask;
    merge(strokeChans, strokeMat);

    // 7. 在 padded 上把 strokeMat 叠加到原图 padded
    //    先把 padded 复制一份 outMat
    cv::Mat outMat = padded.clone(); // CV_8UC4

    for (int y = 0; y < outMat.rows; y++) {
        cv::Vec4b* dstPix = outMat.ptr<cv::Vec4b>(y);
        const cv::Vec4b* fgPix = strokeMat.ptr<cv::Vec4b>(y);
        const cv::Vec4b* bgPix = padded.ptr<cv::Vec4b>(y);

        for (int x = 0; x < outMat.cols; x++) {
            float fgA = fgPix[x][3] / 255.f; // stroke alpha
            float bgA = bgPix[x][3] / 255.f;
            float outA = fgA + bgA * (1.f - fgA);
            if (outA < 1e-5f) {
                dstPix[x] = cv::Vec4b(0,0,0,0);
                continue;
            }
            float fgB = fgPix[x][0]/255.f, fgG = fgPix[x][1]/255.f, fgR = fgPix[x][2]/255.f;
            float bgB = bgPix[x][0]/255.f, bgG = bgPix[x][1]/255.f, bgR = bgPix[x][2]/255.f;

            float outB = (fgB * fgA + bgB * bgA * (1.f - fgA)) / outA;
            float outG = (fgG * fgA + bgG * bgA * (1.f - fgA)) / outA;
            float outR = (fgR * fgA + bgR * bgA * (1.f - fgA)) / outA;

            dstPix[x][0] = (uchar)cvRound(outB*255.f);
            dstPix[x][1] = (uchar)cvRound(outG*255.f);
            dstPix[x][2] = (uchar)cvRound(outR*255.f);
            dstPix[x][3] = (uchar)cvRound(outA*255.f);
        }
    }

    // outMat 现在是 (padded宽, padded高) 的图，四周有留白

    // 8. 如果你想要的结果与 srcBitmap 尺寸相同，需要再裁切 (去掉 padding)。
    //    否则就把整个 outMat 拷贝到 destBitmap，看需求。
    //    这里示例把最终尺寸缩回原来的 w/h (不含留白)。
    int finalW = srcInfo.width;
    int finalH = srcInfo.height;
    // 裁切 ROI, 如果 padding 超过图尺寸，需要检查
    cv::Rect roi(padding, padding, finalW, finalH);
    // 注意：要确保roi在图像范围内
    if (roi.x + roi.width > outMat.cols)  roi.width = outMat.cols - roi.x;
    if (roi.y + roi.height > outMat.rows) roi.height = outMat.rows - roi.y;
    cv::Mat cropped = outMat(roi);

    // 9. 写回 destBitmap
    AndroidBitmapInfo destInfo;
    if (AndroidBitmap_getInfo(env, destBitmap, &destInfo) == ANDROID_BITMAP_RESULT_SUCCESS
        && destInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888
        && destInfo.width == finalW
        && destInfo.height == finalH) {
        void* destPixels = nullptr;
        if (AndroidBitmap_lockPixels(env, destBitmap, &destPixels) == ANDROID_BITMAP_RESULT_SUCCESS) {
            // 拷贝 cropped 数据
            for (int y = 0; y < cropped.rows; y++) {
                memcpy(
                        (uint8_t*)destPixels + y * destInfo.stride,
                        cropped.ptr(y),
                        cropped.cols * 4
                );
            }
            AndroidBitmap_unlockPixels(env, destBitmap);
        }
    }
}