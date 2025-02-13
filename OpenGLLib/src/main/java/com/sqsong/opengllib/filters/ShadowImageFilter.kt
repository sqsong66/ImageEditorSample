package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import com.sqsong.cryptlib.CryptLib
import com.sqsong.cryptlib.EncryptKeys
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture

class ShadowImageFilter(
    context: Context,
    private var shadowStrength: Float = 0f,
    initOutputBuffer: Boolean = true
) : BaseImageFilter(
    context,
    fragmentAssets = CryptLib.getDecryptedShader(EncryptKeys.KEY_SHADER_FRAG_SHADOW), // "shader/shader_frag_shadow.glsl",
    initOutputBuffer = initOutputBuffer
) {

    override fun onPreDraw(program: Program, texture: Texture) {
        program.getUniformLocation("shadowStrength").let {
            // Log.d("songmao", "ContrastImageFilter onPreDraw: saturation location: $it")
            GLES30.glUniform1f(it, shadowStrength)
        }
    }

    override fun setProgress(progress: Float, extraType: Int) {
        shadowStrength = -range(progress, -0.5f, 0.5f)
        // Log.d("songmao", "BrightnessImageFilter setProgress: $progress, shadowStrength: $shadowStrength")
    }
}