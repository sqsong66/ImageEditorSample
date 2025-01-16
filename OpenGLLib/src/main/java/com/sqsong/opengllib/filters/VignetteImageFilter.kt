package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture

class VignetteImageFilter(
    context: Context,
    private var vignette: Float = 0f,
    initOutputBuffer: Boolean = true
) : BaseImageFilter(context, fragmentAssets = "shader/vignette_filter_frag.frag", initOutputBuffer = initOutputBuffer) {

    override fun onPreDraw(program: Program, texture: Texture) {
        program.getUniformLocation("vignetteSize").let {
            // Log.d("songmao", "VignetteImageFilter onPreDraw: vignette location: $it")
            GLES30.glUniform1f(it, vignette)
        }

        fboTexture()?.let { texture ->
            val width = texture.textureWidth.toFloat()
            val height = texture.textureHeight.toFloat()
            program.getUniformLocation("resolution").let {
                GLES30.glUniform2f(it, width, height)
            }
        }
    }

    override fun setProgress(progress: Float, extraType: Int) {
        vignette = range(progress, 0f, 1f)
        // Log.d("songmao", "VignetteImageFilter setProgress: $progress, vignette: $vignette")
    }
}