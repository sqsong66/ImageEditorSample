package com.example.customviewsample.ui.segment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.customviewsample.R
import com.example.customviewsample.base.BaseActivity
import com.example.customviewsample.databinding.ActivityImageSegmentationBinding
import com.example.customviewsample.utils.decodeBitmapByGlide
import com.sqsong.nativelib.NativeLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ImageSegmentationActivity : BaseActivity<ActivityImageSegmentationBinding>(ActivityImageSegmentationBinding::inflate) {

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
            decodeBitmapByGlide(this@ImageSegmentationActivity, uri, 2048)?.let {
                val path = NativeLib.getBitmapOutlinePath(it)
                emit(path)
            }
        }.flowOn(Dispatchers.IO)
            .catch { Log.e("sqsong", "loadImageBitmap error: $it") }
            .onEach { path ->
                Log.e("sqsong", "Load bitmap path: $path")
            }
            .launchIn(lifecycleScope)
    }

}