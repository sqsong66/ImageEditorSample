package com.example.customviewsample.data.anno

import androidx.annotation.IntDef


@IntDef(
    MenuType.MENU_MAIN_LAYERS,
    MenuType.MENU_MAIN_RESIZE
)
@Retention(AnnotationRetention.SOURCE)
annotation class MenuType() {

    companion object {
        const val MENU_MAIN_LAYERS = 0
        const val MENU_MAIN_RESIZE = 1
    }

}
