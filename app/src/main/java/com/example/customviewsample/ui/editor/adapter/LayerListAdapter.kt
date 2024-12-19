package com.example.customviewsample.ui.editor.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import com.example.customviewsample.common.ext.setMaterialShapeBackgroundDrawable
import com.example.customviewsample.common.recycler.AbstractItemAdapter
import com.example.customviewsample.databinding.ItemLayerListBinding
import com.example.customviewsample.utils.dp2Px
import com.example.customviewsample.view.layer.anno.LayerChangedMode
import com.example.customviewsample.view.layer.anno.LayerType
import com.example.customviewsample.view.layer.data.LayerPreviewData
import java.util.Collections

class LayerListAdapter : AbstractItemAdapter<LayerPreviewData, ItemLayerListBinding>(ItemLayerListBinding::inflate) {

    override fun submitList(list: List<LayerPreviewData>) {
        super.submitList(list)
        dataList.joinToString { "ID: ${it.id}, name: ${it.layerName}, " }.let {
            Log.d("sqsong", "submitList: $it")
        }
    }

    override fun onViewHolderInit(binding: ItemLayerListBinding) {
        binding.root.setMaterialShapeBackgroundDrawable(
            allCornerSize = dp2Px(8),
            backgroundColorResId = com.google.android.material.R.attr.colorSurfaceContainerHighest,
            alpha = 100
        )
    }

    override fun inflateData(binding: ItemLayerListBinding, data: LayerPreviewData, position: Int) {
        binding.layerPreviewIv.setLayerParams(data.layerType, data.layerBitmap, data.layerColor)
        binding.layerNameTv.text = data.layerName
        binding.layerOrderIv.visibility = if (data.layerType == LayerType.LAYER_BACKGROUND) View.INVISIBLE else View.VISIBLE
    }

    fun isBackgroundLayer(position: Int): Boolean {
        return dataList[position].layerType == LayerType.LAYER_BACKGROUND
    }

    fun printDataList() {
        dataList.joinToString { "ID: ${it.id}, name: ${it.layerName}, " }.let {
            Log.d("sqsong", "printDataList: $it")
        }
    }

    fun swapItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition == toPosition) return
        Collections.swap(dataList, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    fun addOrUpdateLayer(@LayerChangedMode changedMode: Int, layerPreviewData: LayerPreviewData) {
        when (changedMode) {
            LayerChangedMode.ADD -> {
                if (layerPreviewData.layerType == LayerType.LAYER_BACKGROUND) {
                    dataList.add(layerPreviewData)
                    notifyItemInserted(dataList.lastIndex)
                } else {
                    dataList.add(0, layerPreviewData)
                    notifyItemInserted(0)
                }
            }

            LayerChangedMode.UPDATE -> {
                val index = dataList.indexOfFirst { it.id == layerPreviewData.id }
                if (index != -1) {
                    dataList[index] = layerPreviewData
                    notifyItemChanged(index)
                }
            }

            LayerChangedMode.REMOVE -> {
                val index = dataList.indexOfFirst { it.id == layerPreviewData.id }
                if (index != -1) {
                    dataList.removeAt(index)
                    notifyItemRemoved(index)
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateLayerList(layerList: List<LayerPreviewData>) {
        dataList.clear()
        dataList.addAll(layerList)
        notifyDataSetChanged()
    }

}