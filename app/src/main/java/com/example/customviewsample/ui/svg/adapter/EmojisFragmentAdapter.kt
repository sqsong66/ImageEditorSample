package com.example.customviewsample.ui.svg.adapter

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.customviewsample.ui.svg.fragment.EmojiFragment

class EmojisFragmentAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    private val emojiDirs = mutableListOf<String>()

    override fun getItemCount(): Int = emojiDirs.size

    override fun createFragment(position: Int): Fragment {
        return EmojiFragment.newInstance(emojiDirs[position])
    }

    fun setEmojiDirs(emojiDirs: List<String>) {
        this.emojiDirs.clear()
        this.emojiDirs.addAll(emojiDirs)
        notifyDataSetChanged()
    }
}