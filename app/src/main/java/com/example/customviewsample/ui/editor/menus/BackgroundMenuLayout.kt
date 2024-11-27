package com.example.customviewsample.ui.editor.menus

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.example.customviewsample.common.behavior.EditMenuBottomSheetBehavior
import com.example.customviewsample.databinding.LayoutMenuEditorMainBinding

class BackgroundMenuLayout(
    rootLayout: CoordinatorLayout,
    // View: 当前BottomSheet； Int: 当前展开高度； Boolean: 是否展开
    override val onMenuSlide: (View, Int, Boolean) -> Unit = { _, _, _ -> },
    // View: 当前BottomSheet； Boolean: 是否展开
    override val onMenuSlideDone: (View, Boolean) -> Unit = { _, _ -> },
    override val removeCallback: () -> Unit = {}
) : BaseMenuLayout<LayoutMenuEditorMainBinding>(
    rootLayout = rootLayout,
    block = LayoutMenuEditorMainBinding::inflate
) {

    init {
        initLayout()
    }

    private fun initLayout() {
        binding.doneIv.setOnClickListener { menuBehavior.state = EditMenuBottomSheetBehavior.STATE_HIDDEN }
    }

}