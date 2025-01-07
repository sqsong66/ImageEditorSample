package com.example.customviewsample.ui.svg

import android.graphics.drawable.PictureDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.customviewsample.base.BaseActivity
import com.example.customviewsample.databinding.ActivityLoadSvgBinding
import com.example.customviewsample.ui.svg.adapter.EmojisFragmentAdapter
import com.example.customviewsample.utils.getEmojisPath
import com.example.customviewsample.utils.unzipEmojisFile
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File

class LoadSvgActivity : BaseActivity<ActivityLoadSvgBinding>(ActivityLoadSvgBinding::inflate) {

    private val emojisFragmentAdapter by lazy {
        EmojisFragmentAdapter(this)
    }

    override fun initActivity(savedInstanceState: Bundle?) {
        loadEmojisDir()
    }

    override fun initListeners() {
        binding.unzipEmojiButton.setOnClickListener { unzipEmojis() }
        binding.loadSvgButton.setOnClickListener { loadSvgImage() }
    }

    private fun loadEmojisDir() {
        File(getEmojisPath(this@LoadSvgActivity)).list()?.let {
            emojisFragmentAdapter.setEmojiDirs(it.toList())
            binding.viewPager.adapter = emojisFragmentAdapter

            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                tab.text = it[position]
            }.attach()
        }
    }

    private fun loadSvgImage() {
        /*flow {
            val dirName = File(getEmojisPath(this@LoadSvgActivity)).list()?.random()
            val fileName = File(getEmojisPath(this@LoadSvgActivity), dirName!!).list()?.random()
            Log.d("songmao", "loadSvgImage dirName: $dirName, fileName: $fileName")
            val svgFile = File(getEmojisPath(this@LoadSvgActivity), "$dirName/$fileName")
            val start = System.currentTimeMillis()
            val svg = SVG.getFromInputStream(svgFile.inputStream())
            svg.setDocumentWidth(1024f)
            svg.setDocumentHeight(1024f)
            val picture = svg.renderToPicture(1024, 1024)
            val bitmap = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val drawRect = RectF(0f, 0f, 1024f, 1024f)
            canvas.drawPicture(picture, drawRect)
            val end = System.currentTimeMillis()
            Log.d("songmao", "loadSvgImage cost: ${end - start}ms.")
            emit(bitmap)
        }.flowOn(Dispatchers.IO)
            .onEach {
                binding.imageView.setImageBitmap(it)
            }
            .launchIn(lifecycleScope)*/
        val dirName = File(getEmojisPath(this@LoadSvgActivity)).list()?.random()
        val fileName = File(getEmojisPath(this@LoadSvgActivity), dirName!!).list()?.random()
        Log.d("songmao", "loadSvgImage dirName: $dirName, fileName: $fileName")
        val svgFile = File(getEmojisPath(this@LoadSvgActivity), "$dirName/$fileName")
        Glide.with(this)
            .`as`(PictureDrawable::class.java)
            .load(svgFile)
            .into(binding.imageView)
    }

    private fun unzipEmojis() {
        flow {
            val start = System.currentTimeMillis()
            val files = unzipEmojisFile(this@LoadSvgActivity, "emojis.zip")
            Log.d("songmao", "unzipEmojis cost: ${System.currentTimeMillis() - start}ms.")
            val dirName = File(getEmojisPath(this@LoadSvgActivity)).list()?.random()
            Log.d("songmao", "unzipEmojis dirName: $dirName")
            emit(files)
        }.flowOn(Dispatchers.IO)
            .onEach {
                Log.d("songmao", "unzipEmojis file size: ${it.size}")
                Toast.makeText(this, "Unzip success, file size: ${it.size}", Toast.LENGTH_SHORT).show()
            }.launchIn(lifecycleScope)
    }

}