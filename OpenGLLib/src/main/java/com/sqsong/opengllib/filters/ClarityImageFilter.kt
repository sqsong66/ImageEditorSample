package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import com.sqsong.cryptlib.CryptLib
import com.sqsong.cryptlib.EncryptKeys
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture

class ClarityImageFilter(
    context: Context,
    private var tone: Float = 0f,
    initOutputBuffer: Boolean = true
) : BaseImageFilter(
    context,
    fragmentAssets = CryptLib.getDecryptedShader(EncryptKeys.KEY_SHADER_FRAG_CLARITY), // "shader/shader_frag_clarity.frag",
    initOutputBuffer = initOutputBuffer
) {

    override fun onPreDraw(program: Program, texture: Texture) {
        program.getUniformLocation("sharpness").let {
            // Log.d("songmao", "ClarityImageFilter onPreDraw: sharpness location: $it")
            GLES30.glUniform1f(it, tone)
        }

        fboTexture()?.let { texture ->
            val width = texture.textureWidth.toFloat()
            val height = texture.textureHeight.toFloat()
            program.getUniformLocation("texOffset").let {
                GLES30.glUniform2f(it, 1f / width, 1f / height)
            }
        }
    }

    override fun setProgress(progress: Float, extraType: Int) {
        tone = range(progress, 0f, 0.6f)
        // Log.d("songmao", "ClarityImageFilter setProgress: $progress, sharpness: $tone")
    }
}