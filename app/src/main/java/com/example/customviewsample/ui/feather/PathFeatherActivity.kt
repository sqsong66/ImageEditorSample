package com.example.customviewsample.ui.feather

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.customviewsample.base.BaseActivity
import com.example.customviewsample.databinding.ActivityPathFeatherBinding
import com.example.customviewsample.utils.decodeBitmapByGlide
import com.example.customviewsample.view.PathFeatherView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class PathFeatherActivity : BaseActivity<ActivityPathFeatherBinding>(ActivityPathFeatherBinding::inflate) {

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(::loadImageBitmap)
    }

    override fun initActivity(savedInstanceState: Bundle?) {

    }

    override fun initListeners() {
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.chooseImageBtn.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.slider.addOnChangeListener { _, value, _ ->
            binding.pathFeatherView.setFeatherRadius(PathFeatherView.MAX_FEATHER_RADIUS * value)
        }
    }

    private fun loadImageBitmap(uri: Uri) {
        flow {
            val srcBitmap = decodeBitmapByGlide(this@PathFeatherActivity, uri, 2048) ?: throw IllegalArgumentException("decodeBitmapByGlide failed")
            emit(srcBitmap)
        }.flowOn(Dispatchers.IO)
            .catch {
                Log.e("sqsong", "loadImageBitmap error: $it")
                it.printStackTrace()
            }
            .onEach { bitmap ->
                binding.pathFeatherView.setImageBitmap(bitmap)
            }
            .launchIn(lifecycleScope)
    }

}