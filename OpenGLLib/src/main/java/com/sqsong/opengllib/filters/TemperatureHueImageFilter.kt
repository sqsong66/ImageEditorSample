package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import com.sqsong.cryptlib.CryptLib
import com.sqsong.cryptlib.EncryptKeys
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture

class TemperatureHueImageFilter(
    context: Context,
    private var temperature: Float = 0f,
    private var hue: Float = 0f,
    initOutputBuffer: Boolean = true
) : BaseImageFilter(
    context,
    fragmentAssets = CryptLib.getDecryptedShader(EncryptKeys.KEY_SHADER_FRAG_TEMPERATURE_HUE), // "shader/shader_frag_temperature_hue.glsl",
    initOutputBuffer = initOutputBuffer
) {

    override fun onPreDraw(program: Program, texture: Texture) {
        program.getUniformLocation("temperature").let {
            GLES30.glUniform1f(it, temperature)
        }

        program.getUniformLocation("hue").let {
            GLES30.glUniform1f(it, hue)
        }
    }

    override fun setProgress(progress: Float, extraType: Int) {
        when (extraType) {
            FilterMode.FILTER_TEMPERATURE -> {
                temperature = range(progress, -1f, 1f)
            }

            FilterMode.FILTER_HUE -> {
                hue = range(progress, -3.14159f, 3.14159f)
            }
        }
    }

}