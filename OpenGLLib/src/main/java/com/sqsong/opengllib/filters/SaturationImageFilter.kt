package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture

class SaturationImageFilter(
    context: Context,
    private var saturation: Float = 1f,
    initOutputBuffer: Boolean = true
) : BaseImageFilter(context, fragmentAssets = "shader/saturation_filter_frag.frag", initOutputBuffer = initOutputBuffer) {

    override fun onPreDraw(program: Program, texture: Texture) {
        program.getUniformLocation("saturation").let {
            // Log.d("songmao", "ContrastImageFilter onPreDraw: saturation location: $it")
            GLES30.glUniform1f(it, saturation)
        }
    }

    override fun setProgress(progress: Float, extraType: Int) {
        saturation = range(progress, 0.0f, 2.0f)
        // Log.d("songmao", "BrightnessImageFilter setProgress: $progress, saturation: $saturation")
    }

}