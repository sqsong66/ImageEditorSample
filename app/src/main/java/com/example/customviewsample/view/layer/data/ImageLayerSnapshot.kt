package com.example.customviewsample.view.layer.data

import android.graphics.RectF
import android.os.Parcelable
import com.example.customviewsample.data.CanvasSize
import com.example.customviewsample.view.layer.LayoutInfo
import com.example.customviewsample.view.layer.anno.LayerType
import kotlinx.parcelize.Parcelize

@Parcelize
data class ImageLayerSnapshot(
    val canvasSize: CanvasSize,
    val clipRect: RectF,
    val layerList: List<LayerSnapShot>,
) : Parcelable

@Parcelize
data class LayerSnapShot(
    @LayerType
    val viewLayerType: Int,
    val layoutInfo: LayoutInfo,
    val imageLayerInfo: ImageLayerInfo? = null,
    val backgroundLayerInfo: BackgroundLayerInfo? = null,
) : Parcelable

@Parcelize
data class BackgroundLayerInfo(
    val bgCachePath: String?,
    val bgColor: IntArray?,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotation: Float = 0f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BackgroundLayerInfo

        if (bgCachePath != other.bgCachePath) return false
        if (bgColor != null) {
            if (other.bgColor == null) return false
            if (!bgColor.contentEquals(other.bgColor)) return false
        } else if (other.bgColor != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bgCachePath?.hashCode() ?: 0
        result = 31 * result + (bgColor?.contentHashCode() ?: 0)
        return result
    }
}

@Parcelize
data class ImageLayerInfo(
    val imageCachePath: String?,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotation: Float = 0f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val isLightOn: Boolean = false,
) : Parcelable
