package com.example.customviewsample

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.example.customviewsample.base.BaseActivity
import com.example.customviewsample.databinding.ActivityMainBinding
import com.example.customviewsample.ui.cutout.ImageCutoutActivity
import com.example.customviewsample.ui.editor.ImageEditorActivity
import com.example.customviewsample.ui.segment.ImageSegmentationActivity
import com.example.customviewsample.ui.svg.LoadSvgActivity

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate), View.OnClickListener {

    override fun initActivity(savedInstanceState: Bundle?) {

    }

    override fun initListeners() {
        binding.imageEditor.setOnClickListener(this)
        binding.imageSegmentationBtn.setOnClickListener(this)
        binding.imageCutoutBtn.setOnClickListener(this)
        binding.loadSvgBtn.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.imageEditor -> {
                startActivity(Intent(this, ImageEditorActivity::class.java))
            }

            R.id.imageSegmentationBtn -> {
                startActivity(Intent(this, ImageSegmentationActivity::class.java))
            }

            R.id.imageCutoutBtn -> {
                startActivity(Intent(this, ImageCutoutActivity::class.java))
            }

            R.id.loadSvgBtn -> {
                startActivity(Intent(this, LoadSvgActivity::class.java))
            }
        }
    }

}