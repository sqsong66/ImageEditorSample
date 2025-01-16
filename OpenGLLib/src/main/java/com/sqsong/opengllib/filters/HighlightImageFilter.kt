package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture

class HighlightImageFilter(
    context: Context,
    private var highlight: Float = 0f,
    initOutputBuffer: Boolean = true
) : BaseImageFilter(context, fragmentAssets = "shader/highlight_filter_frag.frag", initOutputBuffer = initOutputBuffer) {

    override fun onPreDraw(program: Program, texture: Texture) {
        program.getUniformLocation("highlight").let {
            // Log.d("songmao", "ContrastImageFilter onPreDraw: highlight location: $it")
            GLES30.glUniform1f(it, highlight)
        }

        program.getUniformLocation("threshold").let {
            // Log.d("songmao", "ContrastImageFilter onPreDraw: threshold location: $it")
            GLES30.glUniform1f(it, 0.2f)
        }
    }

    override fun setProgress(progress: Float, extraType: Int) {
        highlight = range(progress, -0.25f, 0.25f)
        // Log.d("songmao", "BrightnessImageFilter setProgress: $progress, highlight: $highlight")
    }

}