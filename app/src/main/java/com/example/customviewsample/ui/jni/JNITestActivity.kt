package com.example.customviewsample.ui.jni

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.customviewsample.base.BaseActivity
import com.example.customviewsample.databinding.ActivityJniTestBinding
import com.example.customviewsample.utils.decodeBitmapByGlide
import com.gallery.matting.Matting
import jp.co.cyberagent.android.gpuimage.GPUImageNativeLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File

class JNITestActivity : BaseActivity<ActivityJniTestBinding>(ActivityJniTestBinding::inflate) {

    private var modelPaths: Pair<String, String>? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(::loadImageBitmap)
    }

    override fun initActivity(savedInstanceState: Bundle?) {
        val keyArray = intArrayOf(101, 50, 90, 33, 111, 2, 3, 12, 5)
        keyArray.forEach { key ->
            val shader = GPUImageNativeLibrary.getShader(key)
            Log.d("sqsong", "Shader$key:\n$shader")
        }
        decryptModels()
    }

    private fun decryptModels() {
        flow {
            val startTime = System.currentTimeMillis()
            val mattingModelFile = createTempFile("portrait_matting_v5.1.model")
            val segModelFile = createTempFile("portrait_seg_v5.1.model")
            val mattingModelBytes = GPUImageNativeLibrary.aesDecrypt(applicationContext, assets, "encrypt_portrait_matting_v5.1.model")
            val segModelBytes = GPUImageNativeLibrary.aesDecrypt(applicationContext, assets, "encrypt_portrait_seg_v5.1.model")
            mattingModelFile.outputStream().use { it.write(mattingModelBytes) }
            segModelFile.outputStream().use { it.write(segModelBytes) }
            Log.d("sqsong", "decryptModels cost: ${System.currentTimeMillis() - startTime}ms")
            emit(Pair(mattingModelFile.absolutePath, segModelFile.absolutePath))
        }.flowOn(Dispatchers.IO)
            .catch { Log.e("sqsong", "decryptModels error: $it") }
            .onEach { (mattingModelFile, segModelFile) ->
                Log.d("sqsong", "mattingModelFile: ${mattingModelFile}")
                Log.d("sqsong", "segModelFile: ${segModelFile}")
                modelPaths = Pair(mattingModelFile, segModelFile)
            }
            .launchIn(lifecycleScope)
    }

    private fun createTempFile(name: String): File {
        val dir = externalCacheDir ?: cacheDir
        val file = File(dir, name)
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()
        return file
    }

    override fun initListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.chooseImageBtn.setOnClickListener { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    }

    private fun loadImageBitmap(uri: Uri) {
        flow {
            val srcBitmap = decodeBitmapByGlide(this@JNITestActivity, uri, 1024) ?: throw IllegalArgumentException("decodeBitmapByGlide failed")
            modelPaths?.let { (mattingModel, segModel) ->
                val mattingBitmap = Matting.mattingBitmap(segModel, mattingModel, srcBitmap)
                emit(Pair(srcBitmap, mattingBitmap))
            }
        }.flowOn(Dispatchers.IO)
            .catch {
                Log.e("sqsong", "loadImageBitmap error: $it")
                it.printStackTrace()
            }
            .onEach { (src, matting) ->
                binding.srcImage.setImageBitmap(src)
                binding.mattingImage.setImageBitmap(matting)
            }
            .launchIn(lifecycleScope)
    }
}