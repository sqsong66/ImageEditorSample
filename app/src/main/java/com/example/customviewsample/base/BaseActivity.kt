package com.example.customviewsample.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.viewbinding.ViewBinding

abstract class BaseActivity<V : ViewBinding>(open val block: (LayoutInflater) -> V) : AppCompatActivity() {

    protected val binding: V by lazy { block(layoutInflater) }

    abstract fun initActivity(savedInstanceState: Bundle?)

    abstract fun initListeners()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        applyWindowInsets()
        initActivity(savedInstanceState)
        initListeners()
        handleBackPress()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            onWindowInsetsApplied(systemBars)
            insets
        }
    }

    protected open fun onWindowInsetsApplied(insets: Insets) {
        binding.root.updatePadding(left = insets.left, top = insets.top, right = insets.right, bottom = insets.bottom)
    }

    private fun handleBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onHandleBackPress()
            }
        })
    }

    protected open fun onHandleBackPress() {
        supportFinishAfterTransition()
    }

}