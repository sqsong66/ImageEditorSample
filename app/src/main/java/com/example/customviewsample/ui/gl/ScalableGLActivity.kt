package com.example.customviewsample.ui.gl

import android.os.Bundle
import com.example.customviewsample.base.BaseActivity
import com.example.customviewsample.databinding.ActivityScalableGlactivityBinding

class ScalableGLActivity : BaseActivity<ActivityScalableGlactivityBinding>(ActivityScalableGlactivityBinding::inflate) {

    override fun initActivity(savedInstanceState: Bundle?) {

    }

    override fun initListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

}