package com.example.customviewsample.ui.svg.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.example.customviewsample.base.BaseFragment
import com.example.customviewsample.databinding.FragmentEmojiBinding
import com.example.customviewsample.ui.svg.EmojiPreviewActivity
import com.example.customviewsample.ui.svg.adapter.EmojiItemAdapter
import com.example.customviewsample.utils.getEmojisPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File

class EmojiFragment : BaseFragment<FragmentEmojiBinding>(FragmentEmojiBinding::inflate) {

    private var emojiDirName: String? = null
    private val emojiItemAdapter by lazy {
        EmojiItemAdapter {
            startActivity(Intent(requireContext(), EmojiPreviewActivity::class.java).apply{
                putExtra("emoji_path", it)
            })
        }
    }

    override fun initData(savedInstanceState: Bundle?) {
        binding.recycler.adapter = emojiItemAdapter
        emojiDirName = arguments?.getString(EMOJI_DIR_NAME)
        emojiDirName?.let { loadEmojis(it) }
    }

    private fun loadEmojis(emojiDir: String) {
        flow<List<String>> {
            val rootPath = getEmojisPath(requireContext())
            val dir = File(rootPath, emojiDir)
            dir.list()?.map { name ->
                File(getEmojisPath(requireContext()), "$emojiDir/$name").absolutePath
            }?.let { emit(it.sorted()) }
        }.flowOn(Dispatchers.IO)
            .onEach {
                emojiItemAdapter.submitList(it)
                Log.d("songmao", "loadEmojis: $emojiDirName, size: ${it.size}")
            }.launchIn(lifecycleScope)
    }

    companion object {
        private const val EMOJI_DIR_NAME = "emoji_dir_name"
        fun newInstance(emojiDirName: String): EmojiFragment {
            return EmojiFragment().apply {
                arguments = Bundle().apply {
                    putString(EMOJI_DIR_NAME, emojiDirName)
                }
            }
        }
    }

}