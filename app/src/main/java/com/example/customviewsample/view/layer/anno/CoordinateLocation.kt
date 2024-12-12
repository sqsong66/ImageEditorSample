package com.example.customviewsample.view.layer.anno

import androidx.annotation.IntDef

@IntDef(
    CoordinateLocation.COORDINATE_NONE,
    CoordinateLocation.COORDINATE_CENTER,
    CoordinateLocation.COORDINATE_CENTER_X,
    CoordinateLocation.COORDINATE_CENTER_Y
)
@Retention(AnnotationRetention.SOURCE)
annotation class CoordinateLocation {
    companion object {
        const val COORDINATE_NONE = 0
        const val COORDINATE_CENTER = 1
        const val COORDINATE_CENTER_X = 2
        const val COORDINATE_CENTER_Y = 3

    }
}
