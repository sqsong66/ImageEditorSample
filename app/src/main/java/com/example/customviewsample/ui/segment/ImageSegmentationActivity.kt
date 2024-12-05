package com.example.customviewsample.ui.segment

import android.os.Bundle
import com.example.customviewsample.base.BaseActivity
import com.example.customviewsample.databinding.ActivityImageSegmentationBinding

class ImageSegmentationActivity : BaseActivity<ActivityImageSegmentationBinding>(ActivityImageSegmentationBinding::inflate) {

    override fun initActivity(savedInstanceState: Bundle?) {

    }

    override fun initListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

}