package com.example.customviewsample.ui.svg.adapter

import android.graphics.drawable.PictureDrawable
import android.view.ViewGroup.MarginLayoutParams
import com.bumptech.glide.Glide
import com.example.customviewsample.common.recycler.AbstractItemAdapter
import com.example.customviewsample.databinding.ItemEmojisBinding
import com.example.customviewsample.utils.dp2Px
import com.example.customviewsample.utils.screenWidth
import java.io.File

class EmojiItemAdapter(
    private val onEmojiClick: (String) -> Unit
) : AbstractItemAdapter<String, ItemEmojisBinding>(ItemEmojisBinding::inflate) {

    private val itemSize = (screenWidth - dp2Px<Int>(16) * 7) / 6

    override fun inflateData(binding: ItemEmojisBinding, data: String, position: Int) {
        (binding.root.layoutParams as MarginLayoutParams).apply {
            val isFirstRow = position < 6
            topMargin = if (isFirstRow) dp2Px(16) else 0
            width = itemSize
            height = itemSize
            binding.root.layoutParams = this
        }
        Glide.with(binding.root)
            .`as`(PictureDrawable::class.java)
            .load(File(data))
            .into(binding.root)
        binding.root.setOnClickListener {
            onEmojiClick(data)
        }
    }

}