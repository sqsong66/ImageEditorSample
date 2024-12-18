package com.example.customviewsample.data

import com.example.customviewsample.data.anno.MenuType

data class MainMenuData(
    @MenuType val menuType: Int,
    val menuName: String,
    val menuIcon: Int
)
