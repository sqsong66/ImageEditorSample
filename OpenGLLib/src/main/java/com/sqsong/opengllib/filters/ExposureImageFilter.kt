package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture

class ExposureImageFilter(
    context: Context,
    private var exposure: Float = 0f,
    initOutputBuffer: Boolean = true
) : BaseImageFilter(context, fragmentAssets = "shader/exposure_filter_frag.frag", initOutputBuffer = initOutputBuffer) {

    override fun onPreDraw(program: Program, texture: Texture) {
        program.getUniformLocation("exposure").let {
            // Log.w("songmao", "ContrastImageFilter onPreDraw: exposure location: $it, exposure: $exposure")
            GLES30.glUniform1f(it, exposure)
        }
    }

    override fun setProgress(progress: Float, extraType: Int) {
        exposure = range(progress, -1f, 0.6f)
        // Log.d("songmao", "ExposureImageFilter setProgress: $progress, exposure: $exposure")
    }

}