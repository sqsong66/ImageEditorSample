package com.example.customviewsample.ui.editor

import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import com.example.customviewsample.base.BaseActivity
import com.example.customviewsample.common.behavior.EditMenuBottomSheetBehavior
import com.example.customviewsample.common.ext.setMaterialShapeBackgroundDrawable
import com.example.customviewsample.databinding.ActivityImageEditorBinding
import com.example.customviewsample.databinding.LayoutMenuEditorMainBinding
import com.example.customviewsample.ui.editor.adapter.EditorMainMenuAdapter
import com.example.customviewsample.utils.dp2Px
import com.google.android.material.shape.CornerFamily
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ImageEditorActivity : BaseActivity<ActivityImageEditorBinding>(ActivityImageEditorBinding::inflate) {

    private var bottomInsets = 0
    private lateinit var subMenuBehavior: EditMenuBottomSheetBehavior<*>
    private var currentMenuState = EditMenuBottomSheetBehavior.STATE_HIDDEN

    private val mainMenuAdapter by lazy {
        EditorMainMenuAdapter {
            // Handle menu item click
            subMenuBehavior.state = EditMenuBottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun initActivity(savedInstanceState: Bundle?) {
        initLayout()
    }

    private fun initLayout() {
        initMenuLayout()
    }

    private fun initMenuLayout() {
        binding.mainMenuLayout.setMaterialShapeBackgroundDrawable(
            cornerFamily = CornerFamily.ROUNDED,
            topLeftCornerSize = dp2Px(24),
            topRightCornerSize = dp2Px(24),
            backgroundColorResId = com.google.android.material.R.attr.colorSurfaceContainerLow
        )
        binding.menuRecycler.adapter = mainMenuAdapter

        val subMenuBinding = LayoutMenuEditorMainBinding.inflate(LayoutInflater.from(this), binding.main, true)
        subMenuBinding.root.updatePadding(bottom = bottomInsets)
        subMenuBinding.doneIv.setOnClickListener {
            subMenuBehavior.state = EditMenuBottomSheetBehavior.STATE_HIDDEN
        }
        subMenuBehavior = EditMenuBottomSheetBehavior.from(subMenuBinding.root).apply {
            skipCollapsed = true
            isHideable = true
            state = EditMenuBottomSheetBehavior.STATE_HIDDEN
            hideFriction = 0.05f
        }
        subMenuBehavior.addBottomSheetCallback(object : EditMenuBottomSheetBehavior.EditMenuBottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                Log.d("sqsong", "onStateChanged, state: $newState")
                if (newState != EditMenuBottomSheetBehavior.STATE_SETTLING) {
                    currentMenuState = newState
                    if (newState == EditMenuBottomSheetBehavior.STATE_HIDDEN) {
                        animateEditorViewMargin()
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                Log.d("sqsong", "slideOffset: $slideOffset, state: ${subMenuBehavior.state}")
                if (currentMenuState == EditMenuBottomSheetBehavior.STATE_HIDDEN) {
                    val margin = max(dp2Px<Int>(156), (abs(slideOffset + 1) * subMenuBinding.root.height + dp2Px<Int>(16)).toInt())
                    (binding.imageEditorView.layoutParams as MarginLayoutParams).apply {
                        bottomMargin = margin
                        binding.imageEditorView.layoutParams = this
                    }
                }

                val expandHeight = (abs(slideOffset + 1) * subMenuBinding.root.height).toInt()
                val menuLayoutHeight = binding.mainMenuLayout.height
                val ty = min(menuLayoutHeight, expandHeight)
                Log.w("sqsong", "expandHeight: $expandHeight, ty: $ty")
                binding.mainMenuLayout.translationY = ty.toFloat()
            }
        })
    }

    private fun animateEditorViewMargin() {
        val currentMargin = (binding.imageEditorView.layoutParams as MarginLayoutParams).bottomMargin
        val destMargin = dp2Px<Int>(156)
        if (currentMargin == destMargin) return
        ValueAnimator.ofInt(currentMargin, destMargin).apply {
            addUpdateListener {
                (binding.imageEditorView.layoutParams as MarginLayoutParams).apply {
                    bottomMargin = it.animatedValue as Int
                    binding.imageEditorView.layoutParams = this
                }
            }
            duration = 300
            start()
        }
    }

    override fun initListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    override fun onWindowInsetsApplied(insets: Insets) {
        binding.root.updatePadding(left = insets.left, top = insets.top, right = insets.right, bottom = 0)
        binding.mainMenuLayout.updatePadding(bottom = insets.bottom)
        bottomInsets = insets.bottom
    }

}