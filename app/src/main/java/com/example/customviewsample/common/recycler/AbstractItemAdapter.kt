package com.example.customviewsample.common.recycler

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.viewbinding.ViewBinding

abstract class AbstractItemAdapter<T, V : ViewBinding>(
    open val block: (LayoutInflater, ViewGroup?, Boolean) -> V
) : Adapter<AbstractItemAdapter<T, V>.AbstractItemViewHolder>() {

    protected val dataList = mutableListOf<T>()

    @SuppressLint("NotifyDataSetChanged")
    open fun submitList(list: List<T>) {
        dataList.clear()
        dataList.addAll(list)
        notifyDataSetChanged()
    }

    fun appendData(data: T) {
        dataList.add(data)
        notifyItemInserted(dataList.size - 1)
    }

    fun removeData(data: T?): Int {
        val index = dataList.indexOf(data)
        if (index != -1) {
            dataList.removeAt(index)
            notifyItemRemoved(index)
        }
        return dataList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbstractItemViewHolder {
        return AbstractItemViewHolder(block(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: AbstractItemViewHolder, position: Int) {
        holder.bindData(dataList[position], position)
    }

    abstract fun inflateData(binding: V, data: T, position: Int)

    open fun onViewHolderInit(binding: V) {}

    inner class AbstractItemViewHolder(private val binding: V) : RecyclerView.ViewHolder(binding.root) {

        init {
            onViewHolderInit(binding)
        }

        fun bindData(data: T, position: Int) {
            inflateData(binding, data, position)
        }
    }

}