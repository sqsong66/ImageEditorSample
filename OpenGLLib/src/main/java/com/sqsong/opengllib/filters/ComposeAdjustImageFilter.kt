package com.sqsong.opengllib.filters

import android.content.Context

/**
 * 组合滤镜效果。需要注意📢：由于在Render中禁用GL_BLEND，所以需要我们自己来处理预乘alpha的问题。
 * 由于该组合滤镜是按照顺序类进行滤镜效果的，所以我们只需要在最后一个滤镜中处理预乘alpha的问题即可。
 * 即在[VignetteImageFilter]的shader中进行预乘alpha。像[LUTImageFilter]这些filter则需要单独
 * 在其shader中进行预乘alpha。
 */
class ComposeAdjustImageFilter(
    context: Context,
) : GroupImageFilter(context) {

    private val brightnessImageFilter by lazy {
        BrightnessImageFilter(context, initOutputBuffer = false)
    }

    private val contrastImageFilter by lazy {
        ContrastImageFilter(context, initOutputBuffer = false)
    }

    private val saturationImageFilter by lazy {
        SaturationImageFilter(context, initOutputBuffer = false)
    }

    private val exposureImageFilter by lazy {
        ExposureImageFilter(context, initOutputBuffer = false)
    }

    private val highlightShadowImageFilter by lazy {
        HighlightShadowImageFilter(context, initOutputBuffer = false)
    }

    private val temperatureHueImageFilter by lazy {
        TemperatureHueImageFilter(context, initOutputBuffer = false)
    }

    private val sharpenImageFilter by lazy {
        SharpenImageFilter(context, initOutputBuffer = false)
    }

    private val vignetteImageFilter by lazy {
        VignetteImageFilter(context, initOutputBuffer = false)
    }

    init {
        addFilter(brightnessImageFilter)
        addFilter(contrastImageFilter)
        addFilter(saturationImageFilter)
        addFilter(exposureImageFilter)
        addFilter(highlightShadowImageFilter)
        addFilter(temperatureHueImageFilter)
        addFilter(sharpenImageFilter)
        addFilter(vignetteImageFilter)
    }

    override fun setProgress(progress: Float, extraType: Int) {
        when (extraType) {
            FilterMode.FILTER_BRIGHTNESS -> {
                brightnessImageFilter.setProgress(progress, extraType)
            }

            FilterMode.FILTER_CONTRAST -> {
                contrastImageFilter.setProgress(progress, extraType)
            }

            FilterMode.FILTER_SATURATION -> {
                saturationImageFilter.setProgress(progress, extraType)
            }

            FilterMode.FILTER_EXPOSURE -> {
                exposureImageFilter.setProgress(progress, extraType)
            }

            FilterMode.FILTER_HIGHLIGHT -> {
                highlightShadowImageFilter.setProgress(progress, extraType)
            }

            FilterMode.FILTER_SHADOW -> {
                highlightShadowImageFilter.setProgress(progress, extraType)
            }

            FilterMode.FILTER_TEMPERATURE -> {
                temperatureHueImageFilter.setProgress(progress, extraType)
            }

            FilterMode.FILTER_HUE -> {
                temperatureHueImageFilter.setProgress(progress, extraType)
            }

            FilterMode.FILTER_SHARPNESS -> {
                sharpenImageFilter.setProgress(progress, extraType)
            }

            FilterMode.FILTER_VIGNETTE -> {
                vignetteImageFilter.setProgress(progress, extraType)
            }
        }
    }
}