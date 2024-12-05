package com.example.customviewsample.ui.editor.menus

import android.annotation.SuppressLint
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updatePadding
import com.example.customviewsample.common.behavior.EditMenuBottomSheetBehavior
import com.example.customviewsample.data.CanvasSize
import com.example.customviewsample.databinding.LayoutMenuCanvasSizeBinding
import com.example.customviewsample.ui.editor.adapter.CanvasSizeListAdapter

@SuppressLint("SetTextI18n")
class CanvasSizeMenuLayout(
    rootLayout: CoordinatorLayout,
    private val canvasSize: CanvasSize,
    private val bottomPadding: Int = 0,
    private val onCanvasSizeChanged: (CanvasSize) -> Unit,
    // View: 当前BottomSheet； Int: 当前展开高度； Boolean: 是否展开
    override val onMenuSlide: (View, Int, Boolean) -> Unit = { _, _, _ -> },
    // View: 当前BottomSheet； Boolean: 是否展开
    override val onMenuSlideDone: (View, Boolean) -> Unit = { _, _ -> },
    override val removeCallback: () -> Unit = {}
) : BaseMenuLayout<LayoutMenuCanvasSizeBinding>(
    rootLayout = rootLayout,
    block = LayoutMenuCanvasSizeBinding::inflate
) {

    private val canvasSizeListAdapter by lazy {
        CanvasSizeListAdapter { canvasSize ->
            binding.dimensionTv.text = "${canvasSize.width}x${canvasSize.height}"
            onCanvasSizeChanged(canvasSize)
        }
    }


    init {
        initLayout()
    }

    private fun initLayout() {
        binding.root.updatePadding(bottom = bottomPadding)
        binding.doneIv.setOnClickListener { menuBehavior.state = EditMenuBottomSheetBehavior.STATE_HIDDEN }
        binding.canvasSizeRecycler.adapter = canvasSizeListAdapter
        canvasSizeListAdapter.updateCheckIndex(canvasSize)
        binding.dimensionTv.text = "${canvasSize.width}x${canvasSize.height}"
    }

}