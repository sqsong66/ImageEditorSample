package com.example.customviewsample.ui.editor.menus

import android.annotation.SuppressLint
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.customviewsample.common.behavior.EditMenuBottomSheetBehavior
import com.example.customviewsample.common.ext.setMaterialShapeBackgroundDrawable
import com.example.customviewsample.databinding.LayoutMenuLayersBinding
import com.example.customviewsample.ui.editor.adapter.LayerListAdapter
import com.example.customviewsample.utils.dp2Px
import com.example.customviewsample.view.layer.anno.LayerChangedMode
import com.example.customviewsample.view.layer.data.LayerPreviewData

@SuppressLint("SetTextI18n")
class LayerListMenuLayout(
    rootLayout: CoordinatorLayout,
    private val bottomPadding: Int = 0,
    private val layerPreviewList: List<LayerPreviewData>,
    private val onSwapLayer: (Int, Int) -> Unit,
    // View: 当前BottomSheet； Int: 当前展开高度； Boolean: 是否展开
    override val onMenuSlide: (View, Int, Boolean) -> Unit = { _, _, _ -> },
    // View: 当前BottomSheet； Boolean: 是否展开
    override val onMenuSlideDone: (View, Boolean) -> Unit = { _, _ -> },
    override val removeCallback: () -> Unit = {}
) : BaseMenuLayout<LayoutMenuLayersBinding>(
    rootLayout = rootLayout,
    block = LayoutMenuLayersBinding::inflate
) {

    private val layerListAdapter by lazy {
        LayerListAdapter()
    }

    init {
        initLayout()
        initLayerList()
    }

    private fun initLayout() {
        binding.root.updatePadding(bottom = bottomPadding)
        binding.resizeCanvasTv.setMaterialShapeBackgroundDrawable(allCornerSize = dp2Px(4), backgroundColorResId = com.google.android.material.R.attr.colorSurfaceContainerHighest)
        binding.doneIv.setOnClickListener { menuBehavior.state = EditMenuBottomSheetBehavior.STATE_HIDDEN }
        binding.layerListRecycler.adapter = layerListAdapter
        layerListAdapter.submitList(layerPreviewList)
    }

    private fun initLayerList() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(recyclerView: androidx.recyclerview.widget.RecyclerView, viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, target: androidx.recyclerview.widget.RecyclerView.ViewHolder): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                // 禁止移动背景图层
                if (layerListAdapter.isBackgroundLayer(toPosition)) return false
                layerListAdapter.swapItem(fromPosition, toPosition)
                onSwapLayer(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {}

            override fun onSelectedChanged(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                viewHolder ?: return
                animatedItemView(true, viewHolder.itemView)
            }

            override fun clearView(recyclerView: androidx.recyclerview.widget.RecyclerView, viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                animatedItemView(false, viewHolder.itemView)
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.layerListRecycler)
    }

    private fun animatedItemView(isSelected: Boolean, itemView: View) {
        val sx = if (isSelected) 0.98f else 1f
        val sy = if (isSelected) 0.98f else 1f
        val alpha = if (isSelected) 0.95f else 1f
        itemView.animate().scaleX(sx).scaleY(sy).alpha(alpha).setDuration(200)
            .withEndAction {
                if (!isSelected) layerListAdapter.printDataList()
            }
            .start()
    }

    fun onAddOrUpdateLayer(@LayerChangedMode changedMode: Int, layerPreviewData: LayerPreviewData) {
        layerListAdapter.addOrUpdateLayer(changedMode, layerPreviewData)
        if (changedMode == LayerChangedMode.ADD) {
            binding.layerListRecycler.smoothScrollToPosition(0)
        }
    }

    fun updateLayerList(layerPreviewList: List<LayerPreviewData>) {
        layerListAdapter.submitList(layerPreviewList)
    }

}