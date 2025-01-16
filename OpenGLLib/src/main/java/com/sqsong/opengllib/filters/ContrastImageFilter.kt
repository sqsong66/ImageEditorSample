package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture

class ContrastImageFilter(
    context: Context,
    private var contrast: Float = 1f,
    initOutputBuffer: Boolean = true
) : BaseImageFilter(context, fragmentAssets = "shader/contrast_filter_frag.frag", initOutputBuffer = initOutputBuffer) {

    override fun onPreDraw(program: Program, texture: Texture) {
        program.getUniformLocation("contrast").let {
            // Log.d("BaseImageFilter", "ContrastImageFilter onPreDraw: contrast location: $it, value: $contrast")
            GLES30.glUniform1f(it, contrast)
        }
    }

    override fun setProgress(progress: Float, extraType: Int) {
        contrast = range(progress, 0.75f, 1.25f)
        // Log.d("BaseImageFilter", "ContrastImageFilter setProgress: $progress, contrast: $contrast")
    }

}