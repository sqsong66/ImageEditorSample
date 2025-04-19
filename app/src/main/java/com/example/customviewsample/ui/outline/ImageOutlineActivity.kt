package com.example.customviewsample.ui.outline

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.customviewsample.base.BaseActivity
import com.example.customviewsample.databinding.ActivityImageOutlineBinding
import com.example.customviewsample.utils.decodeBitmapByGlide
import com.sqsong.nativelib.NativeLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ImageOutlineActivity : BaseActivity<ActivityImageOutlineBinding>(ActivityImageOutlineBinding::inflate) {

    private var srcBitmap: Bitmap? = null
    private var destBitmap: Bitmap? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(::loadImageBitmap)
    }


    private val selectMediasLaunch = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d("sqsong", "result: $result")
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val clipData = data?.clipData
            if (clipData != null) {
                val uris = mutableListOf<Uri>()
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i).uri?.let { uris.add(it) }
                }
                Log.d("sqsong", "uris: ${uris.size}")
                uris.getOrNull(0)?.let { uri ->
                    loadImageBitmap(uri)
                }
            } else {
                data?.data?.let(::loadImageBitmap)
            }
        }
    }


    private fun selectImages() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        selectMediasLaunch.launch(intent)
    }


    override fun initActivity(savedInstanceState: Bundle?) {

    }

    override fun initListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.chooseImageBtn.setOnClickListener {
            // pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            // getContent.launch("image/*")
            selectImages()
        }
        binding.strokeSlider.addOnChangeListener { _, value, _ ->
            binding.imageView.setOutlineStrokeWidth(value)
        }
        binding.blurSlider.addOnChangeListener { _, value, _ ->
            binding.imageView.setOutlineBlurRadius(value)
        }
    }

    private fun processOutline(src: Bitmap, dest: Bitmap, strokeWidth: Int) {
        flow {
            // process outline
            val start = System.currentTimeMillis()
            dest.eraseColor(Color.TRANSPARENT)
            NativeLib.nativeOutlineBitmap(src, dest, strokeWidth, 20f, Color.RED)
            Log.d("sqsong", "processOutline cost: ${System.currentTimeMillis() - start}ms")
            emit(Unit)
        }.flowOn(Dispatchers.Default)
            .catch { Log.e("sqsong", "processOutline error: $it") }
            .onEach {
                binding.imageView.setImageBitmap(dest)
            }
            .launchIn(lifecycleScope)
    }

    private fun loadImageBitmap(uri: Uri) {
        flow {
            decodeBitmapByGlide(this@ImageOutlineActivity, uri, 2048)?.let { emit(it) }
        }.flowOn(Dispatchers.IO)
            .catch { Log.e("sqsong", "loadImageBitmap error: $it") }
            .onEach { bitmap ->
                binding.imageView.setImageBitmap(bitmap)
            }
            .launchIn(lifecycleScope)
    }
}