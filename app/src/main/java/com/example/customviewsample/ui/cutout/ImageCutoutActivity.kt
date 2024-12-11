package com.example.customviewsample.ui.cutout

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.example.customviewsample.R
import com.example.customviewsample.base.BaseActivity
import com.example.customviewsample.databinding.ActivityImageCutoutBinding
import com.example.customviewsample.utils.saveBitmapToGallery
import com.sqsong.nativelib.NativeLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ImageCutoutActivity : BaseActivity<ActivityImageCutoutBinding>(ActivityImageCutoutBinding::inflate) {

    override fun initActivity(savedInstanceState: Bundle?) {
        loadCutoutBitmap()
    }

    override fun initListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.save_image -> {
                    nativeCutoutImage()
                    true
                }

                else -> false
            }
        }
    }

    private fun nativeCutoutImage() {
        flow {
            val start = System.currentTimeMillis()
            val cutoutBitmap = BitmapFactory.decodeStream(assets.open("image/cutout.png"))
            val resultBitmap = BitmapFactory.decodeStream(assets.open("image/result.jpg"))
            val end = System.currentTimeMillis()
            Log.d("songmao", "decode bitmap cost: ${end - start}ms")
            val bitmap = NativeLib.cutoutBitmapBySource(cutoutBitmap, resultBitmap)
            Log.w("songmao", "Cutout bitmap cost: ${System.currentTimeMillis() - end}ms")
            /*bitmap?.let {
                saveBitmapToGallery(this@ImageCutoutActivity, it, true)
            }*/
            emit(bitmap)
        }.flowOn(Dispatchers.IO)
            .catch { ex -> ex.printStackTrace() }
            .onEach {
                binding.imageView.setImageBitmap(it)
            }.launchIn(lifecycleScope)
    }

    private fun loadCutoutBitmap() {
        flow {
            val cutoutBitmap = BitmapFactory.decodeStream(assets.open("image/cutout.png"))
            emit(cutoutBitmap)
        }.flowOn(Dispatchers.IO)
            .catch { ex -> ex.printStackTrace() }
            .onEach {
                binding.imageView.setImageBitmap(it)
            }.launchIn(lifecycleScope)
    }

}