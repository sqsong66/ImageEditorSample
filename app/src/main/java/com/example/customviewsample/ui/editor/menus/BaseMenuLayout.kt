package com.example.customviewsample.ui.editor.menus

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.viewbinding.ViewBinding
import com.example.customviewsample.common.behavior.EditMenuBottomSheetBehavior
import com.example.customviewsample.common.ext.layoutInflater
import kotlin.math.abs

abstract class BaseMenuLayout<V : ViewBinding>(
    val rootLayout: CoordinatorLayout,
    val block: (LayoutInflater, ViewGroup, Boolean) -> V,
    // View: 当前BottomSheet； Int: 当前展开高度； Boolean: 是否展开
    open val onMenuSlide: (View, Int, Boolean) -> Unit = { _, _, _ -> },
    // View: 当前BottomSheet； Boolean: 是否展开
    open val onMenuSlideDone: (View, Boolean) -> Unit = { _, _ -> },
    open val removeCallback: () -> Unit = {}
) {

    private var currentMenuState = EditMenuBottomSheetBehavior.STATE_HIDDEN

    protected val binding: V by lazy { block(rootLayout.layoutInflater(), rootLayout, true) }

    private val bottomSheetCallback = object : EditMenuBottomSheetBehavior.EditMenuBottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            Log.w("BaseMenuLayout", "onStateChanged: $newState")
            if (newState != EditMenuBottomSheetBehavior.STATE_SETTLING) {
                currentMenuState = newState
                onMenuSlideDone(bottomSheet, newState != EditMenuBottomSheetBehavior.STATE_HIDDEN)
            }
            if (newState == EditMenuBottomSheetBehavior.STATE_HIDDEN) {
                rootLayout.removeView(binding.root)
                removeCallback()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            Log.d("BaseMenuLayout", "onSlide: $slideOffset")
            val expandHeight = (bottomSheet.height * abs(slideOffset + 1)).toInt()
            onMenuSlide(bottomSheet, expandHeight, currentMenuState == EditMenuBottomSheetBehavior.STATE_HIDDEN)
        }
    }

    protected val menuBehavior = EditMenuBottomSheetBehavior.from(binding.root).apply {
        skipCollapsed = true
        isHideable = true
        state = EditMenuBottomSheetBehavior.STATE_HIDDEN
        addBottomSheetCallback(bottomSheetCallback)
    }

    init {
        // 拦截事件，防止事件穿透到后面
        binding.root.setOnClickListener { }
    }

    fun setBehaviorState(state: Int) {
        menuBehavior.state = state
    }

    fun hideMenu(): Boolean {
        return if (currentMenuState != EditMenuBottomSheetBehavior.STATE_HIDDEN) {
            menuBehavior.state = EditMenuBottomSheetBehavior.STATE_HIDDEN
            true
        } else {
            false
        }
    }

}