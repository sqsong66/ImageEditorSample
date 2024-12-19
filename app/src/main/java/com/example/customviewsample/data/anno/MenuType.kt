package com.example.customviewsample.data.anno

import androidx.annotation.IntDef


@IntDef(
    MenuType.MENU_MAIN_LAYERS,
    MenuType.MENU_MAIN_RESIZE,
    MenuType.MENU_MAIN_TEXT,
    MenuType.MENU_MAIN_BACKGROUND
)
@Retention(AnnotationRetention.SOURCE)
annotation class MenuType() {

    companion object {
        const val MENU_MAIN_LAYERS = 0
        const val MENU_MAIN_RESIZE = 1
        const val MENU_MAIN_TEXT = 2
        const val MENU_MAIN_BACKGROUND = 3
    }

}
