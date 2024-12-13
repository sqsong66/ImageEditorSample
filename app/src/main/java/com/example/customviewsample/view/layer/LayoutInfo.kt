package com.example.customviewsample.view.layer

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LayoutInfo(
    var widthRatio: Float = 1f,
    var heightRatio: Float = 1f,
    var centerXRatio: Float = 0.5f,
    var centerYRatio: Float = 0.5f
) : Parcelable
