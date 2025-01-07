package com.example.customviewsample.ui.svg

import android.graphics.drawable.PictureDrawable
import android.os.Bundle
import com.bumptech.glide.Glide
import com.example.customviewsample.base.BaseActivity
import com.example.customviewsample.databinding.ActivityEmojiPreviewBinding
import java.io.File

class EmojiPreviewActivity : BaseActivity<ActivityEmojiPreviewBinding>(ActivityEmojiPreviewBinding::inflate) {

    override fun initActivity(savedInstanceState: Bundle?) {
        intent.getStringExtra("emoji_path")?.let { emojiPath ->
            Glide.with(binding.emojiIv)
                .`as`(PictureDrawable::class.java)
                .load(File(emojiPath))
                .into(binding.emojiIv)
        } ?: run { finish() }
    }

    override fun initListeners() {

    }

}