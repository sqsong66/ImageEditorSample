package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import com.sqsong.cryptlib.CryptLib
import com.sqsong.cryptlib.EncryptKeys
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture

class ToneImageFilter(
    context: Context,
    private var tone: Float = 0f,
    initOutputBuffer: Boolean = true
) : BaseImageFilter(
    context,
    fragmentAssets = CryptLib.getDecryptedShader(EncryptKeys.KEY_SHADER_FRAG_TONE), // "shader/shader_frag_tone.glsl",
    initOutputBuffer = initOutputBuffer
) {

    override fun onPreDraw(program: Program, texture: Texture) {
        program.getUniformLocation("tone").let {
            // Log.d("songmao", "ContrastImageFilter onPreDraw: tone location: $it")
            GLES30.glUniform1f(it, tone)
        }
    }

    override fun setProgress(progress: Float, extraType: Int) {
        tone = -range(progress, -20f, 20f)
        // Log.d("songmao", "BrightnessImageFilter setProgress: $progress, tone: $tone")
    }
}