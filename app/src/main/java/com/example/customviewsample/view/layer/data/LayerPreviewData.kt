package com.example.customviewsample.view.layer.data

import android.graphics.Bitmap
import com.example.customviewsample.view.layer.anno.LayerType

data class LayerPreviewData(
    val id: Int,
    @LayerType val layerType: Int,
    val layerName: String,
    val layerBitmap: Bitmap? = null,
    val layerColor: IntArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LayerPreviewData

        if (id != other.id) return false
        if (layerType != other.layerType) return false
        if (layerName != other.layerName) return false
        if (layerBitmap != other.layerBitmap) return false
        if (layerColor != null) {
            if (other.layerColor == null) return false
            if (!layerColor.contentEquals(other.layerColor)) return false
        } else if (other.layerColor != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + layerType
        result = 31 * result + layerName.hashCode()
        result = 31 * result + (layerBitmap?.hashCode() ?: 0)
        result = 31 * result + (layerColor?.contentHashCode() ?: 0)
        return result
    }
}