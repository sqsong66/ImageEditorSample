package com.example.customviewsample.ui.other

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.customviewsample.R
import com.example.customviewsample.base.BaseActivity
import com.example.customviewsample.databinding.ActivitySaveAnimationBinding

class SaveAnimationActivity : BaseActivity<ActivitySaveAnimationBinding>(ActivitySaveAnimationBinding::inflate) {

    override fun initActivity(savedInstanceState: Bundle?) {
        binding.editorSaveView.startAnimation()
    }

    override fun initListeners() {
        binding.editorSaveView.setOnClickListener {  }
        binding.saveLoadingButton.setOnClickListener {  }
        binding.changeModeButton.setOnClickListener { binding.editorSaveView.toggleSaveMode() }
    }

}