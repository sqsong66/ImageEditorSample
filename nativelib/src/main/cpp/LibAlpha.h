//
//  LibAlpha.h
//  BGEraser
//
//  Created by 张慧 on 2019/12/31.
//  Copyright © 2019 Apowersoft. All rights reserved.
//

#ifndef LibAlpha_h
#define LibAlpha_h
//#include <stdint.h>
#include <opencv2/core.hpp>

#ifdef __cplusplus
#define EXTERN_C extern "C"
#else
#define EXTERN_C
#endif

#define WXBGERASER_CAPI EXTERN_C
#ifdef __cplusplus
extern "C" {
#endif
//黑白图优化鬼影及边缘，获取黑白图的外接矩形框，用于裁剪到边缘
//dst与src可以是同一地址，也可以不同，必须同分辨率同格式
//dst：处理后的buf,如果不需要优化黑白图，只需要获取外接矩形，可以设置为空
//src：原图buf
//width:图像宽
//height:图像高
//nb_channel:色彩空间类型，4：rgba，3：rgb，1：gray
//stride:数据扫描宽度Stride bitmapdata stride  数据行宽：一般是图片宽度的整数倍，例如rgba一般是宽度的4倍，但是考虑到内存对齐，也不一定是宽度的整数倍
//rect:裁剪到边缘的矩形框：x,y,w,h
WXBGERASER_CAPI int WXAdjustAlpha(uint8_t *dst, uint8_t *src, int width, int height, int nb_channel, int stride, int *rect);

/**
黑白图与原图合并，形成透明图
img与alpha必须同分辨率
dst：合并后的rgba
src：原图
alpha:黑白图
width:图像宽
height:图像高
src_type：原图的色彩空间类型，4：rgba，3：rgb，1：gray  bitmap空间色彩可以判定
src_stride：原图birmapdata数据扫描宽度Stride，数据行宽，一般是图片宽度的整数倍，例如rgba一般是宽度的4倍，但是考虑到内存对齐，也不一定是宽度的整数倍
dst_stride：rgba数据行宽
alpha_type:黑白图色彩空间类型，4：rgba，3：rgb，1：gray
alpha_stride:数据行宽*/
WXBGERASER_CAPI int WXMergeRGBA(uint8_t *dst, uint8_t *src, uint8_t *alpha, int width, int height, int src_type, int src_stride, int dst_stride, int alpha_type, int alpha_stride);

/**
优化前景边缘
img_dst、img_src、alpha三者必须同分辨率  img_dst、img_src可以是同一个地址
img_dst：优化后的图片
dst_type：优化后的图片类型，最好和src_type同样的颜色空间，4：rgba，3：rgb，1：gray
dst_stride：birmapdata数据扫描宽度Stride
img_src：原图
img_width：图片宽
img_height：图片高
src_type：原图色彩空间，4：rgba，3：rgb，1：gray
src_stride：原图Stride
alpha：黑白图mask
alpha_type：黑白图色彩空间，4：rgba，3：rgb，1：gray
alpha_stride：黑白图Stride
rect:裁剪到边缘的矩形框：x,y,w,h
返回值小于0 出错或者没必要进行前景优化：-1至-6参数错误  -7：mask图基本全黑  -8：抠出图片占比太小  -9：接近全白
 */
WXBGERASER_CAPI int WXEnhanceForeground(uint8_t *img_dst, int dst_type, int dst_stride, uint8_t *img_src, int img_width, int img_height, int src_type, int src_stride, uint8_t *alpha, int alpha_type, int alpha_stride, int *rect, bool is_mask_alpha_channel);

WXBGERASER_CAPI int WXShadowView(uint8_t *rgba_view, int view_stride, int view_width, int view_height, int x, int y, uint8_t *rgba_fg, int fg_stride, int fg_width, int fg_height, uint8_t r, uint8_t g, uint8_t b);

WXBGERASER_CAPI void CalculateMaskRect(const uint8_t *alpha, int mask_width, int mask_height, int *rect);

#ifdef __cplusplus
};
#endif

#endif /* LibAlpha_h */
