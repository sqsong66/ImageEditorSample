package com.example.customviewsample.view.layer.anno

import androidx.annotation.IntDef

@IntDef(LayerChangedMode.ADD, LayerChangedMode.REMOVE, LayerChangedMode.UPDATE)
@Retention(AnnotationRetention.SOURCE)
annotation class LayerChangedMode {
    companion object {
        const val ADD = 0
        const val REMOVE = 1
        const val UPDATE = 2
    }
}