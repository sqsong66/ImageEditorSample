package com.example.customviewsample.view.layer.anno

import androidx.annotation.IntDef

@IntDef(
    LayerRotation.ROTATION_NONE,
    LayerRotation.ROTATION_0,
    LayerRotation.ROTATION_45,
    LayerRotation.ROTATION_90,
    LayerRotation.ROTATION_135,
    LayerRotation.ROTATION_180,
    LayerRotation.ROTATION_225,
    LayerRotation.ROTATION_270,
    LayerRotation.ROTATION_315,
)
@Retention(AnnotationRetention.SOURCE)
annotation class LayerRotation {
    companion object {
        const val ROTATION_NONE = -1
        const val ROTATION_0 = 0
        const val ROTATION_45 = 45
        const val ROTATION_90 = 90
        const val ROTATION_135 = 135
        const val ROTATION_180 = 180
        const val ROTATION_225 = 225
        const val ROTATION_270 = 270
        const val ROTATION_315 = 315
    }
}
