package com.gallery.matting

import android.graphics.Bitmap
import java.nio.ByteBuffer

data class ImageData(
    val width: Int = 0,
    val height: Int = 0,
    val stride: Int = 0,
    val data: ByteArray? = null
) {
    fun copyData(
        width: Int = this.width,
        height: Int = this.height,
        stride: Int = this.stride,
        data: ByteArray? = this.data
    ): ImageData {
        return ImageData(width, height, stride, data)
    }

    // 将 ImageData 转换为 Bitmap
    fun convertToBitmap(): Bitmap {
        val createBitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ALPHA_8)
        return try {
            if (data != null) {
                createBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(data))
            }
            createBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            createBitmap
        }
    }

    // 重写 equals 方法，比较两个 ImageData 对象是否相同
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImageData) return false
        return width == other.width && height == other.height && stride == other.stride && data.contentEquals(other.data)
    }

    // 重写 hashCode
    override fun hashCode(): Int {
        return 31 * (31 * (31 * width + height) + stride) + (data?.contentHashCode() ?: 0)
    }

    // 用于打印 ImageData 对象的内容
    override fun toString(): String {
        return "ImageData(width=$width, height=$height, stride=$stride, data=${data?.joinToString()})"
    }
}