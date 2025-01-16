package com.sqsong.opengllib.filters

import androidx.annotation.IntDef

@IntDef(
    FilterMode.FILTER_BRIGHTNESS,
    FilterMode.FILTER_CONTRAST,
    FilterMode.FILTER_SATURATION,
    FilterMode.FILTER_EXPOSURE,
    FilterMode.FILTER_HIGHLIGHT,
    FilterMode.FILTER_SHADOW,
    FilterMode.FILTER_TEMPERATURE,
    FilterMode.FILTER_SHARPNESS,
    FilterMode.FILTER_VIGNETTE,
    FilterMode.FILTER_HUE
)
@Retention(AnnotationRetention.SOURCE)
annotation class FilterMode {
    companion object {
        const val FILTER_BRIGHTNESS = 0
        const val FILTER_CONTRAST = 1
        const val FILTER_SATURATION = 2
        const val FILTER_EXPOSURE = 3
        const val FILTER_HIGHLIGHT = 4
        const val FILTER_SHADOW = 5
        const val FILTER_TEMPERATURE = 6
        const val FILTER_SHARPNESS = 7
        const val FILTER_VIGNETTE = 8
        const val FILTER_HUE = 9
    }
}