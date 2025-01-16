package com.sqsong.opengllib.filters

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES30
import com.sqsong.opengllib.common.BitmapTexture
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture

class LUTImageFilter(
    context: Context,
    private val lutBitmaps: List<Bitmap>,
    private var intensity: Float = 1.0f,
    fragmentAsset: String = "shader/filter_lut.glsl",
    initOutputBuffer: Boolean = true
) : BaseImageFilter(context, fragmentAssets = fragmentAsset, initOutputBuffer = initOutputBuffer) {

    private var lutTextures = mutableListOf<Texture>()

    override fun onInitialized(program: Program) {
        lutTextures.clear()
        lutTextures.addAll(lutBitmaps.map { BitmapTexture(it) })
    }

    override fun onPreDraw(program: Program, texture: Texture) {
        lutTextures.forEachIndexed { index, lutTexture ->
            val location = program.getUniformLocation("inputImageTexture${index + 1}")
            if (location != -1) {
                lutTexture.bindTexture(location, 1 + index)
            }
        }

        program.getUniformLocation("intensity").let {
            GLES30.glUniform1f(it, intensity)
        }
    }

    override fun onAfterDraw() {
        lutTextures.forEach { it.unbindTexture() }
    }

    override fun setProgress(progress: Float, extraType: Int) {
        intensity = progress
    }

    override fun onDestroy() {
        super.onDestroy()
        lutTextures.forEach { it.delete() }
        lutTextures.clear()
    }
}