package com.example.customviewsample.ui.editor.adapter

import android.view.ViewGroup.MarginLayoutParams
import com.example.customviewsample.common.ext.setRippleBackgroundColor
import com.example.customviewsample.common.recycler.AbstractItemAdapter
import com.example.customviewsample.databinding.ItemEditorMainMenuBinding
import com.example.customviewsample.utils.dp2Px

class EditorMainMenuAdapter(
    private val onMenuClick: () -> Unit
) : AbstractItemAdapter<Any, ItemEditorMainMenuBinding>(ItemEditorMainMenuBinding::inflate) {

    init {
        repeat(10) {
            appendData(Any())
        }
    }

    override fun inflateData(binding: ItemEditorMainMenuBinding, data: Any, position: Int) {
        (binding.root.layoutParams as MarginLayoutParams).apply {
            marginStart = if (position == 0) dp2Px<Int>(16) else dp2Px<Int>(4)
            marginEnd = if (position == itemCount - 1) dp2Px<Int>(16) else dp2Px<Int>(4)
            binding.root.layoutParams = this
        }
        binding.root.setRippleBackgroundColor(allCornerRadius = dp2Px(8))
        binding.root.setOnClickListener {
            onMenuClick()
        }
    }

}