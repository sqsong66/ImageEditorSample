package com.example.customviewsample.ui.editor.adapter

import android.view.ViewGroup.MarginLayoutParams
import com.example.customviewsample.common.ext.setRippleBackgroundColor
import com.example.customviewsample.common.recycler.AbstractItemAdapter
import com.example.customviewsample.data.MainMenuData
import com.example.customviewsample.databinding.ItemEditorMainMenuBinding
import com.example.customviewsample.utils.dp2Px
import com.example.customviewsample.utils.getMainMenu

class EditorMainMenuAdapter(
    private val onMenuClick: (MainMenuData) -> Unit
) : AbstractItemAdapter<MainMenuData, ItemEditorMainMenuBinding>(ItemEditorMainMenuBinding::inflate) {

    init {
        submitList(getMainMenu())
    }

    override fun inflateData(binding: ItemEditorMainMenuBinding, data: MainMenuData, position: Int) {
        (binding.root.layoutParams as MarginLayoutParams).apply {
            marginStart = if (position == 0) dp2Px<Int>(8) else dp2Px<Int>(2)
            marginEnd = if (position == itemCount - 1) dp2Px<Int>(8) else dp2Px<Int>(2)
            binding.root.layoutParams = this
        }
        binding.root.setRippleBackgroundColor(allCornerRadius = dp2Px(8))
        binding.menuIconIv.setImageResource(data.menuIcon)
        binding.menuNameTv.text = data.menuName
        binding.root.setOnClickListener {
            onMenuClick(data)
        }
    }

}