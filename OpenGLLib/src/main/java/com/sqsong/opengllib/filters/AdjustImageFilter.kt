package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import android.util.Log
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture
import com.sqsong.opengllib.data.AdjustImageInfo

class AdjustImageFilter(
    context: Context,
    initOutputBuffer: Boolean = true,
    private var imageInfo: AdjustImageInfo = AdjustImageInfo()
) : BaseImageFilter(context, fragmentAssets = "shader/filter_adjust_image.glsl", initOutputBuffer = initOutputBuffer) {

    override fun onPreDraw(program: Program, texture: Texture) {
        program.getUniformLocation("width").let {
            GLES30.glUniform1i(it, texture.textureWidth)
        }
        program.getUniformLocation("height").let {
            GLES30.glUniform1i(it, texture.textureHeight)
        }
        program.getUniformLocation("brightness").let {
            GLES30.glUniform1f(it, imageInfo.brightness)
        }
        program.getUniformLocation("contrast").let {
            GLES30.glUniform1f(it, imageInfo.contrast)
        }
        program.getUniformLocation("saturation").let {
            GLES30.glUniform1f(it, imageInfo.saturation)
        }
        program.getUniformLocation("exposure").let {
            GLES30.glUniform1f(it, imageInfo.exposure)
        }
        program.getUniformLocation("shadows").let {
            GLES30.glUniform1f(it, imageInfo.shadows)
        }
        program.getUniformLocation("highlights").let {
            GLES30.glUniform1f(it, imageInfo.highlights)
        }
        program.getUniformLocation("temperature").let {
            GLES30.glUniform1f(it, imageInfo.temperature)
        }
        program.getUniformLocation("hue").let {
            GLES30.glUniform1f(it, imageInfo.hue)
        }
        program.getUniformLocation("sharpen").let {
            GLES30.glUniform1f(it, imageInfo.sharpen)
        }
        program.getUniformLocation("vignette").let {
            GLES30.glUniform1f(it, imageInfo.vignette)
        }
    }

    override fun setProgress(progress: Float, extraType: Int) {
        Log.d("AdjustImageFilter", "setProgress: progress: $progress, extraType: $extraType")
        when (extraType) {
            FilterMode.FILTER_BRIGHTNESS -> {
                imageInfo = imageInfo.copy(brightness = range(progress, -0.15f, 0.15f))
            }

            FilterMode.FILTER_CONTRAST -> {
                imageInfo = imageInfo.copy(contrast = range(progress, 0.75f, 1.25f))
            }

            FilterMode.FILTER_SATURATION -> {
                imageInfo = imageInfo.copy(saturation = range(progress, 0.0f, 2.0f))
            }

            FilterMode.FILTER_EXPOSURE -> {
                imageInfo = imageInfo.copy(exposure = range(progress, -1.0f, 1.0f))
            }

            FilterMode.FILTER_SHADOW -> {
                imageInfo = imageInfo.copy(shadows = range(progress, 0.6f, 1.4f))
            }

            FilterMode.FILTER_HIGHLIGHT -> {
                imageInfo = imageInfo.copy(highlights = range(progress, 0.2f, 1.8f))
            }

            FilterMode.FILTER_TEMPERATURE -> {
                imageInfo = imageInfo.copy(temperature = range(progress, -1f, 1f))
            }

            FilterMode.FILTER_HUE -> {
                imageInfo = imageInfo.copy(hue = range(progress, -3.14159f, 3.14159f))
            }

            FilterMode.FILTER_SHARPNESS -> {
                imageInfo = imageInfo.copy(sharpen = range(progress, 0f, 1f))
            }

            FilterMode.FILTER_VIGNETTE -> {
                imageInfo = imageInfo.copy(vignette = range(progress, 0f, 1f))
            }
        }
    }

}