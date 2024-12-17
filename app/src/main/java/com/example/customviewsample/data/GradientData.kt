package com.example.customviewsample.data

import androidx.core.graphics.toColorInt

data class GradientData(
    val name: String,
    val colors: List<String>
) {
    fun toGradientColor(): GradientColor {
        return GradientColor(name, colors.map { it.toColorInt() }.toIntArray())
    }
}

data class GradientColor(
    val name: String,
    val colors: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GradientColor

        if (name != other.name) return false
        if (!colors.contentEquals(other.colors)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + colors.contentHashCode()
        return result
    }
}
