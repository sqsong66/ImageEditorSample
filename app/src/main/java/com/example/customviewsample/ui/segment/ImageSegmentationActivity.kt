package com.example.customviewsample.ui.segment

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.lifecycle.lifecycleScope
import com.example.customviewsample.R
import com.example.customviewsample.base.BaseActivity
import com.example.customviewsample.databinding.ActivityImageSegmentationBinding
import com.example.customviewsample.utils.decodeBitmapByGlide
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import dev.eren.removebg.RemoveBg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.nio.ByteBuffer

class ImageSegmentationActivity : BaseActivity<ActivityImageSegmentationBinding>(ActivityImageSegmentationBinding::inflate) {

    private val segmenter by lazy {
        Segmentation.getClient(
            SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .enableRawSizeMask()
                .build()
        )
    }

    private val remover by lazy {
        RemoveBg(this)
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(::loadImageBitmap)
    }

    override fun initActivity(savedInstanceState: Bundle?) {

    }

    override fun initListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_add_image -> {
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    true
                }

                else -> false
            }
        }
    }

    private fun loadImageBitmap(uri: Uri) {
        flow {
            decodeBitmapByGlide(this@ImageSegmentationActivity, uri, 2048)?.let { emit(it) }
        }.flowOn(Dispatchers.IO)
            .catch { Log.e("sqsong", "loadImageBitmap error: $it") }
            .onEach { bitmap ->
                remover.clearBackground(bitmap).collectLatest {
                    binding.imageView.setImageBitmap(it)
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun maskColorsFromByteBuffer(byteBuffer: ByteBuffer, maskWidth: Int, maskHeight: Int): IntArray {
        @ColorInt val colors = IntArray(maskWidth * maskHeight)
        for (i in 0 until maskWidth * maskHeight) {
            val backgroundLikelihood = 1f - byteBuffer.float
            if (backgroundLikelihood > 0.9f) {
                colors[i] = Color.argb(128, 255, 0, 255)
            } else if (backgroundLikelihood > 0.2) {
                // Linear interpolation to make sure when backgroundLikelihood is 0.2, the alpha is 0 and
                // when backgroundLikelihood is 0.9, the alpha is 128.
                // +0.5 to round the float value to the nearest int.
                val alpha = (182.9 * backgroundLikelihood - 36.6 + 0.5).toInt()
                colors[i] = Color.argb(alpha, 255, 0, 255)
            }
        }
        return colors
    }


}