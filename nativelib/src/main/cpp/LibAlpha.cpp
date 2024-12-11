//
//  LibAlpha.c
//  BGEraser
//
//  Created by 张慧 on 2019/12/31.
//  Copyright © 2019 Apowersoft. All rights reserved.
//
#include <opencv2/opencv.hpp>
#include "LibAlpha.h"
#include "enhance_foreground.h"

int gauss_smooth(uint8_t *alpha, int width, int height, int alpha_type) {
    int max_ = MAX(width, height);
    int blur_size = 3;
    if (max_ > 1000)
        blur_size = 5;
    int data_type = CV_8UC1;
    switch (alpha_type) {
        case 1:
            data_type = CV_8UC1;
            break;
        case 3:
            data_type = CV_8UC3;
            break;
        case 4:
            data_type = CV_8UC4;
            break;

        default:
            return -1;
            break;
    }
    cv::Mat src_img(height, width, data_type, alpha);
    cv::GaussianBlur(src_img, src_img, cv::Size(blur_size, blur_size), 0);

    return 0;
}

uint8_t *smooth_step_table(int a, int b) {
    uint8_t *smooth_table = new uint8_t[256];
    double fa = a / 255.0;
    double fb = b / 255.0;
    double r, y;
    for (int i = 0; i < 256; i++) {
        if (i <= a)
            smooth_table[i] = 0;
        else if (i < b) {
            r = i / 255.0;
            y = (r - fa) / (fb - fa);
            y = y * y * (3.0 - 2.0 * y);
            y = y * 255;
            y = int(y);;
            if (y > 255)
                y = 255;
            else if (y < 0)
                y = 0;
            smooth_table[i] = (uint8_t) y;
        } else
            smooth_table[i] = 255;
    }

    return smooth_table;
}

static uint8_t *alpha_table = smooth_step_table(60, 220);

int WXAdjustAlpha(uint8_t *dst, uint8_t *src, int width, int height, int nb_channel, int stride, int *rect) {
    if (nb_channel != 1 && nb_channel != 3 && nb_channel != 4)
        return -1;
    if (src == NULL || width <= 0 || height <= 0)
        return -2;
    if (stride < width * nb_channel)
        return -3;
    if (NULL == dst && NULL == rect)
        return -4;

    bool brect = false;
    if (NULL != rect)
        brect = true;

    bool badjust = false;
    if (NULL != dst)
        badjust = true;

    if (badjust) {
        if (src != dst) {
            memcpy(dst, src, stride * height);
        }
        gauss_smooth(dst, width, height, nb_channel);
    }

    int minx = width, miny = height, maxx = 0, maxy = 0;
    int src_row_index = 0;// , dst_row_index = 0;
    int src_index = 0;
    uint8_t alpha = 0;
    for (int i = 0; i < height; i++) {
        for (int j = 0; j < width; j++) {
            alpha = src[src_index] & 0xff;
            if (badjust) {
                alpha = alpha_table[alpha];
                for (int c = 0; c < nb_channel; c++) {
                    if (c == 3)
                        dst[src_index + 3] = src[src_index + 3];
                    else
                        dst[src_index + c] = alpha;
                }
            }
            src_index += nb_channel;
            //求外接矩形
            if (brect && alpha > 0) {
                if (i < miny)
                    miny = i;
                if (j < minx)
                    minx = j;
                if (i > maxy)
                    maxy = i;
                if (j > maxx)
                    maxx = j;
            }
        }
        src_row_index += stride;
        src_index = src_row_index;
    }
    //外接矩形：x,y,w,h
    if (brect) {
        //外接矩形：x,y,w,h
        if (maxx <= minx || maxy <= miny) {
            rect[0] = 0;
            rect[1] = 0;
            rect[2] = 0;
            rect[3] = 0;
            return -5;
        }
        rect[0] = minx;
        rect[1] = miny;
        rect[2] = maxx - minx + 1;
        rect[3] = maxy - miny + 1;
    }
    return 0;
}

int WXMergeRGBA(uint8_t *dst, uint8_t *src, uint8_t *alpha, int width, int height, int src_nb_channel, int src_stride, int dst_stride, int alpha_nb_channel, int alpha_stride) {
    if (src_nb_channel != 1 && src_nb_channel != 3 && src_nb_channel != 4)
        return -2;
    if (alpha_nb_channel != 1 && alpha_nb_channel != 3 && alpha_nb_channel != 4)
        return -3;

    int src_row_index = 0, dst_row_index = 0, alpha_row_index = 0;
    int src_index = 0, dst_index = 0, alpha_index = 0;
    if (src_nb_channel == 1) {
        uint8_t temp = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                temp = src[src_index];
                dst[dst_index] = temp;
                dst[dst_index + 1] = temp;
                dst[dst_index + 2] = temp;

                dst[dst_index + 3] = alpha[alpha_index];
                dst_index += 4;
                src_index += src_nb_channel;
                alpha_index += alpha_nb_channel;
            }
            src_row_index += src_stride;
            dst_row_index += dst_stride;
            alpha_row_index += alpha_stride;
            src_index = src_row_index;
            dst_index = dst_row_index;
            alpha_index = alpha_row_index;
        }
    } else {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                dst[dst_index] = src[src_index];
                dst[dst_index + 1] = src[src_index + 1];
                dst[dst_index + 2] = src[src_index + 2];

                dst[dst_index + 3] = alpha[alpha_index];
                dst_index += 4;
                src_index += src_nb_channel;
                alpha_index += alpha_nb_channel;
            }
            src_row_index += src_stride;
            dst_row_index += dst_stride;
            alpha_row_index += alpha_stride;
            src_index = src_row_index;
            dst_index = dst_row_index;
            alpha_index = alpha_row_index;
        }
    }
    return 0;
}

int WXEnhanceForeground(uint8_t *img_dst, int dst_type, int dst_stride, uint8_t *img_src, int img_width, int img_height, int src_type,
                        int src_stride, uint8_t *alpha, int alpha_type, int alpha_stride, int *rect, bool is_mask_alpha_channel) {
    if (0 >= img_width || 0 >= img_height)
        return -2;

    if (nullptr == img_dst || (1 != dst_type && 3 != dst_type && 4 != dst_type) ||
        dst_stride < img_width * dst_type)
        return -3;

    if (NULL == img_src || (1 != src_type && 3 != src_type && 4 != src_type) ||
        src_stride < img_width * src_type)
        return -4;

    if (nullptr == alpha || (1 != alpha_type && 3 != alpha_type && 4 != alpha_type) ||
        alpha_stride < img_width * alpha_type)
        return -5;

    return enhance_foreground::enhance(img_dst, dst_type, dst_stride, img_src, img_width,
                                       img_height, src_type, src_stride, alpha, alpha_type,
                                       alpha_stride, rect, is_mask_alpha_channel);
}

int WXShadowView(uint8_t *rgba_view, int view_stride, int view_width, int view_height, int x, int y, uint8_t *rgba_fg, int fg_stride, int fg_width, int fg_height, uint8_t r, uint8_t g, uint8_t b) {
    if (nullptr == rgba_view || 0 >= view_width || 0 >= view_height || view_width * 4 > view_stride)
        return -1;

    if (nullptr == rgba_fg || 0 >= fg_width || 0 >= fg_height || fg_width * 4 > fg_stride)
        return -2;

    if (0 > x || 0 > y || x + fg_width > view_width || y + fg_height > view_height)
        return -3;

    memset(rgba_view, (uint8_t) 0, sizeof(uint8_t) * view_stride * view_height);

    int view_index = y * view_stride + x * 4, view_row_index = view_index;
    int fg_index = 0, fg_row_index = 0;
    for (int i = y; i < y + fg_height; i++) {
        for (int j = x; j < x + fg_width; j++) {
            rgba_view[view_index] = b;
            rgba_view[view_index + 1] = g;
            rgba_view[view_index + 2] = r;
            rgba_view[view_index + 3] = rgba_fg[fg_index + 3];

            view_index += 4;
            fg_index += 4;
        }
        view_row_index += view_stride;
        view_index = view_row_index;
        fg_row_index += fg_stride;
        fg_index = fg_row_index;
    }
    return 0;
}

void CalculateMaskRect(const uint8_t *alpha, int mask_width, int mask_height, int *rect) {
    int min_x = mask_width;
    int min_y = mask_height;
    int max_x = -1;
    int max_y = -1;
    int srcIndex = 0, rowIndex = 0;
    for (int i = 0; i < mask_height; ++i) {
        for (int j = 0; j < mask_width; ++j) {
            if (alpha[srcIndex] > 0) {
                if (i < min_y)
                    min_y = i;
                if (i > max_y)
                    max_y = i;
                if (j < min_x)
                    min_x = j;
                if (j > max_x)
                    max_x = j;
            }
            srcIndex += 4;
        }
        rowIndex += mask_width * 4;
        srcIndex = rowIndex;
    }

    if (rect != nullptr) {
        if (max_x <= min_x || max_y <= min_y) {
            rect[0] = 0;
            rect[1] = 0;
            rect[2] = 0;
            rect[3] = 0;
        } else {
            rect[0] = min_x;
            rect[1] = min_y;
            rect[2] = max_x - min_x + 1;
            rect[3] = max_y - min_y + 1;
        }
    }
}

