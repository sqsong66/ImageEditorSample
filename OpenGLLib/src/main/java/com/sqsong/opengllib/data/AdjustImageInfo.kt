package com.sqsong.opengllib.data

/*
* uniform float brightness;   // 亮度参数调整，范围可以是从-1.0（更暗）到1.0（更亮）
        uniform float contrast;     // 对比度参数调整，大于1.0增加对比度，小于1.0减少对比度，等于1.0时无变化
        uniform float saturation;   // 饱和度参数调整，大于1.0增加饱和度，小于1.0减少饱和度，等于1.0时无变化
        uniform float exposure;     // 曝光度参数调整，默认为0
        uniform float shadows;      // 调节阴影， 默认为1
        uniform float highlights;   // 调节高光，默认为1
        uniform float temperature;  // 色温调整, 默认为0
        uniform float hue;          // 色调调整，默认为0
        uniform float sharpen;      // 锐化，默认为0
        uniform float vignette;     // 暗角，默认为0*/
data class AdjustImageInfo(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val exposure: Float = 0f,
    val shadows: Float = 1f,
    val highlights: Float = 1f,
    val temperature: Float = 0f,
    val hue: Float = 0f,
    val sharpen: Float = 0f,
    val vignette: Float = 0f
)
