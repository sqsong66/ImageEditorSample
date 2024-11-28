package com.example.customviewsample.data

data class CanvasSize(
    val width: Int,
    val height: Int,
    val iconRes: Int,
    val title: String,
    val isTint: Boolean = false
) {
    fun widthHeightRatio(): Float = width.toFloat() / height.toFloat()
}
