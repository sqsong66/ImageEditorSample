package com.example.customviewsample.view.layer.anno

import androidx.annotation.IntDef

@IntDef(
    LayerType.LAYER_IMAGE,
    LayerType.LAYER_BACKGROUND,
    LayerType.LAYER_TEXT,
    LayerType.LAYER_WATERMARK,
    LayerType.LAYER_LOGO
)
@Retention(AnnotationRetention.SOURCE)
annotation class LayerType {
    companion object {
        const val LAYER_IMAGE = 0
        const val LAYER_BACKGROUND = 1
        const val LAYER_TEXT = 2
        const val LAYER_WATERMARK = 3
        const val LAYER_LOGO = 4
    }
}
