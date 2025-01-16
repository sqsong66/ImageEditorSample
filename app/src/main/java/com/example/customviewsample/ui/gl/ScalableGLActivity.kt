package com.example.customviewsample.ui.gl

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.customviewsample.base.BaseActivity
import com.example.customviewsample.databinding.ActivityScalableGlactivityBinding
import com.example.customviewsample.utils.decodeBitmapByGlide
import com.sqsong.opengllib.filters.GaussianBlurImageFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ScalableGLActivity : BaseActivity<ActivityScalableGlactivityBinding>(ActivityScalableGlactivityBinding::inflate) {

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(::loadImageBitmap)
    }

    override fun initActivity(savedInstanceState: Bundle?) {
        binding.glTextureView.setFilter(GaussianBlurImageFilter(this, maxBlurRadius = 80))
    }

    override fun initListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.addImageBtn.setOnClickListener { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        binding.slider.addOnChangeListener { _, value, _ ->
            binding.glTextureView.setProgress(value)
        }
    }

    private fun loadImageBitmap(uri: Uri) {
        flow {
            decodeBitmapByGlide(this@ScalableGLActivity, uri, 2048)?.let { emit(it) }
        }.flowOn(Dispatchers.IO)
            .catch { Log.e("sqsong", "loadImageBitmap error: $it") }
            .onEach { bitmap ->
                binding.glTextureView.setImageBitmap(bitmap)
            }
            .launchIn(lifecycleScope)
    }

}