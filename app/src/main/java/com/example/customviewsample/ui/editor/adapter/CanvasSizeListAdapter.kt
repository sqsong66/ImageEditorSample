package com.example.customviewsample.ui.editor.adapter

import android.content.res.ColorStateList
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import com.example.customviewsample.common.ext.setRippleBackgroundColor
import com.example.customviewsample.common.recycler.AbstractItemAdapter
import com.example.customviewsample.data.CanvasSize
import com.example.customviewsample.databinding.ItemCanvasSizeBinding
import com.example.customviewsample.utils.dp2Px
import com.example.customviewsample.utils.getCanvasSizeList
import com.example.customviewsample.utils.getThemeColorWithAlpha
import kotlin.math.max
import kotlin.math.min

class CanvasSizeListAdapter(
    private val onApplySize: (CanvasSize) -> Unit
) : AbstractItemAdapter<CanvasSize, ItemCanvasSizeBinding>(ItemCanvasSizeBinding::inflate) {

    private var checkIndex = 0
    private val canvasWidth = dp2Px<Int>(100)
    private val maxCanvasHeight = dp2Px<Int>(160)
    private val minCanvasHeight = dp2Px<Int>(42)

    init {
        val itemList = getCanvasSizeList()
        submitList(itemList)
    }

    fun updateCheckIndex(canvasSize: CanvasSize) {
        val index = dataList.indexOfFirst { it.isSameSize(canvasSize) }
        if (index != -1) {
            val preIndex = checkIndex
            checkIndex = index
            notifyItemChanged(checkIndex)
            if (preIndex != -1) {
                notifyItemChanged(preIndex)
            }
        }
    }

    override fun inflateData(binding: ItemCanvasSizeBinding, data: CanvasSize, position: Int) {
        val calculateHeight = (canvasWidth / data.widthHeightRatio()).toInt()
        val frameHeight = max(minCanvasHeight, min(maxCanvasHeight, calculateHeight))
        binding.sizeFrame.layoutParams.apply {
            width = canvasWidth
            height = frameHeight
            binding.sizeFrame.layoutParams = this
        }

        (binding.root.layoutParams as MarginLayoutParams).apply {
            marginStart = if (position == 0) dp2Px(16) else dp2Px(4)
            marginEnd = if (position == dataList.size - 1) dp2Px(16) else dp2Px(4)
            width = canvasWidth
            height = ViewGroup.LayoutParams.MATCH_PARENT
            binding.root.layoutParams = this
        }

        if (checkIndex == position) {
            binding.sizeFrame.setRippleBackgroundColor(
                allCornerRadius = dp2Px(8),
                backgroundColorResId = com.google.android.material.R.attr.colorSurface,
                alpha = 0,
                borderWidth = dp2Px(2),
                borderColorResId = com.google.android.material.R.attr.colorPrimary,
                borderAlpha = 255
            )
        } else {
            binding.sizeFrame.setRippleBackgroundColor(
                allCornerRadius = dp2Px(8),
                backgroundColorResId = com.google.android.material.R.attr.colorSurface,
                alpha = 0,
                borderWidth = dp2Px(1),
                borderColorResId = com.google.android.material.R.attr.colorOnSurface,
                borderAlpha = 120
            )
        }
        binding.sizeNameTv.text = data.title
        binding.logoIv.setImageResource(data.iconRes)
        binding.logoIv.imageTintList = if (data.isTint) {
            val color = getThemeColorWithAlpha(binding.logoIv.context, com.google.android.material.R.attr.colorOnSurface, 180)
            ColorStateList.valueOf(color)
        } else {
            null
        }
        binding.sizeFrame.setOnClickListener {
            if (checkIndex == position) return@setOnClickListener
            val lastCheckIndex = checkIndex
            checkIndex = position
            onApplySize(data)
            notifyItemChanged(lastCheckIndex)
            notifyItemChanged(checkIndex)
        }
    }

}