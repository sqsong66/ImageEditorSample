package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import com.sqsong.cryptlib.CryptLib
import com.sqsong.cryptlib.EncryptKeys
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture

class ColorTemperatureImageFilter(
    context: Context,
    private var temperature: Float = 0f,
    initOutputBuffer: Boolean = true
) : BaseImageFilter(
    context,
    fragmentAssets = CryptLib.getDecryptedShader(EncryptKeys.KEY_SHADER_FRAG_COLOR_TEMPERATURE), // "shader/shader_frag_color_temperature.frag",
    initOutputBuffer = initOutputBuffer
) {

    override fun onPreDraw(program: Program, texture: Texture) {
        program.getUniformLocation("temperature").let {
            // Log.d("songmao", "ContrastImageFilter onPreDraw: temperature location: $it")
            GLES30.glUniform1f(it, temperature)
        }
    }

    override fun setProgress(progress: Float, extraType: Int) {
        temperature = -range(progress, -1.2f, 1.2f)
        // Log.d("songmao", "BrightnessImageFilter setProgress: $progress, temperature: $temperature")
    }
}