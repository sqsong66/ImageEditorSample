package com.example.customviewsample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.example.customviewsample.base.BaseActivity
import com.example.customviewsample.databinding.ActivityMainBinding
import com.example.customviewsample.ui.curve.ColorCurveActivity
import com.example.customviewsample.ui.cutout.ImageCutoutActivity
import com.example.customviewsample.ui.editor.ImageEditorActivity
import com.example.customviewsample.ui.feather.PathFeatherActivity
import com.example.customviewsample.ui.gl.ScalableGLActivity
import com.example.customviewsample.ui.jni.JNITestActivity
import com.example.customviewsample.ui.other.SaveAnimationActivity
import com.example.customviewsample.ui.outline.ImageOutlineActivity
import com.example.customviewsample.ui.segment.ImageSegmentationActivity
import com.example.customviewsample.ui.svg.LoadSvgActivity

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate), View.OnClickListener {

    private val zipLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { uri ->
        uri?.data?.data?.let {
            Log.d("sqsong", "zip uri: $it")
        }
    }

    override fun initActivity(savedInstanceState: Bundle?) {

    }

    override fun initListeners() {
        binding.imageEditor.setOnClickListener(this)
        binding.imageSegmentationBtn.setOnClickListener(this)
        binding.imageCutoutBtn.setOnClickListener(this)
        binding.loadSvgBtn.setOnClickListener(this)
        binding.curveViewBtn.setOnClickListener(this)
        binding.scalableOpenGLBtn.setOnClickListener(this)
        binding.outlineBtn.setOnClickListener(this)
        binding.jniBtn.setOnClickListener(this)
        binding.pathFeatherBtn.setOnClickListener(this)
        binding.saveAnimationBtn.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.imageEditor -> {
                startActivity(Intent(this, ImageEditorActivity::class.java))
            }

            R.id.imageSegmentationBtn -> {
                startActivity(Intent(this, ImageSegmentationActivity::class.java))
                /*val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/zip"
                    putExtra(Intent.EXTRA_TITLE, "photos.zip")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val initialUri = DocumentsContract.buildDocumentUri(
                            "com.android.providers.downloads.documents", // 指定文件提供者
                            "downloads"  // 目录的 ID（或根据需要修改）
                        )
                        putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
                    }
                }
                zipLauncher.launch(intent)*/
            }

            R.id.imageCutoutBtn -> {
                startActivity(Intent(this, ImageCutoutActivity::class.java))
            }

            R.id.loadSvgBtn -> {
                startActivity(Intent(this, LoadSvgActivity::class.java))
            }

            R.id.curveViewBtn -> {
                startActivity(Intent(this, ColorCurveActivity::class.java))
            }

            R.id.scalableOpenGLBtn -> {
                startActivity(Intent(this, ScalableGLActivity::class.java))
            }

            R.id.outlineBtn -> {
                startActivity(Intent(this, ImageOutlineActivity::class.java))
            }

            R.id.jniBtn -> {
                startActivity(Intent(this, JNITestActivity::class.java))
            }

            R.id.pathFeatherBtn -> {
                startActivity(Intent(this, PathFeatherActivity::class.java))
            }

            R.id.saveAnimationBtn -> {
                startActivity(Intent(this, SaveAnimationActivity::class.java))
            }
        }
    }

}