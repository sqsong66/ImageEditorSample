package com.example.customviewsample

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.example.customviewsample.base.BaseActivity
import com.example.customviewsample.databinding.ActivityMainBinding
import com.example.customviewsample.ui.editor.ImageEditorActivity

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate), View.OnClickListener {

    override fun initActivity(savedInstanceState: Bundle?) {

    }

    override fun initListeners() {
        binding.imageEditor.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.imageEditor -> {
                startActivity(Intent(this, ImageEditorActivity::class.java))
            }
        }
    }

}