package com.example.customviewsample.ui.editor

import android.animation.ValueAnimator
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewPropertyAnimator
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.Insets
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.customviewsample.R
import com.example.customviewsample.base.BaseActivity
import com.example.customviewsample.common.behavior.EditMenuBottomSheetBehavior
import com.example.customviewsample.common.ext.measuredSize
import com.example.customviewsample.common.ext.setEnableState
import com.example.customviewsample.common.ext.setMaterialShapeBackgroundDrawable
import com.example.customviewsample.common.ext.setRippleBackgroundColor
import com.example.customviewsample.data.GradientData
import com.example.customviewsample.data.MainMenuData
import com.example.customviewsample.data.anno.MenuType
import com.example.customviewsample.databinding.ActivityImageEditorBinding
import com.example.customviewsample.ui.editor.adapter.EditorMainMenuAdapter
import com.example.customviewsample.ui.editor.menus.CanvasSizeMenuLayout
import com.example.customviewsample.utils.decodeBitmapByGlide
import com.example.customviewsample.utils.dp2Px
import com.example.customviewsample.view.layer.listener.ImageEditorActionListener
import com.google.android.material.shape.CornerFamily
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class ImageEditorActivity : BaseActivity<ActivityImageEditorBinding>(ActivityImageEditorBinding::inflate), ImageEditorActionListener {

    private var addImageType = 0 // 0-添加图片 1-添加背景
    private var bottomInsets = 0
    private var hideAnimator: ViewPropertyAnimator? = null
    private var backgroundMenuLayout: CanvasSizeMenuLayout? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(::loadImageBitmap)
    }

    private val mainMenuAdapter by lazy {
        EditorMainMenuAdapter(::onMainMenuClick)
    }

    override fun initActivity(savedInstanceState: Bundle?) {
        initLayout()
    }

    private fun initLayout() {
        initMenuLayout()
        loadGradientBackgrounds()
        binding.root.post { initRedoUndoLayout() }
    }

    private fun initRedoUndoLayout() {
        binding.undoIv.setEnableState(false)
        binding.undoIv.setRippleBackgroundColor(allCornerRadius = dp2Px(32))
        binding.redoIv.setEnableState(false)
        binding.redoIv.setRippleBackgroundColor(allCornerRadius = dp2Px(32))
    }

    private fun loadGradientBackgrounds() {
        flow {
            val json = assets.open("json/gradients.json").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<GradientData>>() {}.type
            val gradients = Gson().fromJson<List<GradientData>>(json, listType)
            val colorArrays = gradients.map { it.toGradientColor() }
            val gradientColor = colorArrays[Random.nextInt(0, colorArrays.size)]
            Log.d("sqsong", "loadGradientBackgrounds: ${gradientColor.name}")
            emit(gradientColor.colors)
        }.flowOn(Dispatchers.IO)
            .onEach {
                binding.imageEditorView.updateBackgroundLayerColor(it)
            }
            .launchIn(lifecycleScope)
    }

    private fun initMenuLayout() {
        binding.mainMenuLayout.setMaterialShapeBackgroundDrawable(
            cornerFamily = CornerFamily.ROUNDED,
            topLeftCornerSize = dp2Px(16),
            topRightCornerSize = dp2Px(16),
            backgroundColorResId = com.google.android.material.R.attr.colorSurfaceContainerLow
        )
        binding.menuRecycler.adapter = mainMenuAdapter
    }

    private fun showBackgroundMenu() {
        backgroundMenuLayout = CanvasSizeMenuLayout(
            binding.main, binding.imageEditorView.canvasSize, bottomInsets,
            onCanvasSizeChanged = {
                binding.imageEditorView.updateCanvasSize(it)
            },
            onMenuSlide = ::onBottomSheetMenuSlide,
            onMenuSlideDone = ::onBottomSheetMenuSlideDone,
            removeCallback = {
                backgroundMenuLayout = null
            }
        )
        binding.root.doOnLayout {
            backgroundMenuLayout?.setBehaviorState(EditMenuBottomSheetBehavior.STATE_EXPANDED)
        }
    }

    private fun onBottomSheetMenuSlide(bottomSheet: View, expandHeight: Int, isExpand: Boolean) {
        // Log.w("sqsong", "onBottomSheetMenuSlide: $expandHeight, isExpand: $isExpand")
        if (isExpand) {
            val margin = max(dp2Px<Int>(156), expandHeight + dp2Px<Int>(16))
            (binding.imageEditorView.layoutParams as MarginLayoutParams).apply {
                bottomMargin = margin
                binding.imageEditorView.layoutParams = this
            }
        }

        val menuLayoutHeight = binding.mainMenuLayout.height
        val ty = min(menuLayoutHeight, expandHeight)
        // Log.d("sqsong", "expandHeight: $expandHeight, ty: $ty, menuLayoutHeight: $menuLayoutHeight, isExpand: $isExpand")
        binding.mainMenuLayout.translationY = ty.toFloat()
    }

    private fun onBottomSheetMenuSlideDone(bottomSheet: View, isExpand: Boolean) {
        // Log.w("sqsong", "onBottomSheetMenuSlideDone: isExpand: $isExpand")
        if (!isExpand) animateEditorViewMargin()
    }

    private fun animateEditorViewMargin() {
        val currentMargin = (binding.imageEditorView.layoutParams as MarginLayoutParams).bottomMargin
        val destMargin = dp2Px<Int>(156)
        if (currentMargin == destMargin) return
        ValueAnimator.ofInt(currentMargin, destMargin).apply {
            addUpdateListener {
                (binding.imageEditorView.layoutParams as MarginLayoutParams).apply {
                    bottomMargin = it.animatedValue as Int
                    binding.imageEditorView.layoutParams = this
                }
            }
            duration = 300
            start()
        }
    }

    override fun initListeners() {
        binding.imageEditorView.setImageEditorActionListener(this)
        binding.previewIv.setOnClickListener { binding.previewIv.visibility = View.GONE }
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_add_background -> {
                    addImageType = 1
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    true
                }

                R.id.menu_add_image -> {
                    addImageType = 0
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    true
                }

                R.id.clear_image -> {
                    binding.imageEditorView.clearLayers()
                    true
                }

                R.id.save_image -> {
                    val bitmap = binding.imageEditorView.getEditorBitmap()
                    Log.d("sqsong", "Bitmap size: ${bitmap.width}x${bitmap.height}")
                    binding.previewIv.setImageBitmap(bitmap)
                    binding.previewIv.visibility = View.VISIBLE
                    true
                }

                R.id.menu_color_background -> {
                    loadGradientBackgrounds()
                    true
                }

                else -> false
            }
        }
        binding.deleteIv.setOnClickListener { binding.imageEditorView.removeCurrentLayer() }
        binding.moreIv.setOnClickListener { }
        binding.undoIv.setOnClickListener { }
        binding.redoIv.setOnClickListener { }
    }

    override fun onWindowInsetsApplied(insets: Insets) {
        // Log.d("sqsong", "onWindowInsetsApplied: $insets, bottomInsets: $bottomInsets")
        // 不可以设置binding.root(CoordinatorLayout).updatePadding top, 否则Behavior布局中刷新Layout时会导致Behavior异常收缩
        binding.contentLayout.updatePadding(top = insets.top)
        binding.mainMenuLayout.updatePadding(bottom = insets.bottom)
        bottomInsets = insets.bottom
    }

    private fun onMainMenuClick(menuData: MainMenuData) {
        when (menuData.menuType) {
            MenuType.MENU_MAIN_LAYERS -> {

            }

            MenuType.MENU_MAIN_RESIZE -> {
                showBackgroundMenu()
            }
        }
    }

    private fun loadImageBitmap(uri: Uri) {
        flow {
            decodeBitmapByGlide(this@ImageEditorActivity, uri, 2048)?.let { emit(it) }
        }.flowOn(Dispatchers.IO)
            .catch { Log.e("sqsong", "loadImageBitmap error: $it") }
            .onEach { bitmap ->
                if (addImageType == 0) {
                    binding.imageEditorView.addImageLayer(bitmap)
                } else {
                    binding.imageEditorView.addBackgroundLayer(bitmap)
                }
            }
            .launchIn(lifecycleScope)
    }

    override fun onHandleBackPress() {
        if (backgroundMenuLayout?.hideMenu() == true) {
            return
        }
        super.onHandleBackPress()
    }

    override fun onShowLayerEditMenu(x: Float, y: Float) {
        val shouldAnim = binding.editLayerLayout.visibility == View.GONE
        binding.editLayerLayout.alpha = 0f
        binding.editLayerLayout.scaleX = 0.8f
        binding.editLayerLayout.scaleY = 0.8f
        binding.editLayerLayout.visibility = View.VISIBLE
        val layoutSize = binding.editLayerLayout.measuredSize()
        Log.d("sqsong", "onShowLayerEditMenu: $x, $y, layoutSize: $layoutSize")
        var top = y.toInt() - layoutSize.height
        if (top < 0) top = 0
        val bottom = binding.imageEditorView.height - layoutSize.height
        if (top > bottom) top = bottom
        var left = x.toInt() - layoutSize.width / 2
        if (left < 0) left = 0
        val right = binding.imageEditorView.width - layoutSize.width
        if (left > right) left = right
        (binding.editLayerLayout.layoutParams as MarginLayoutParams).apply {
            this.topMargin = top
            this.leftMargin = left
            binding.editLayerLayout.layoutParams = this
        }
        hideAnimator?.cancel()
        if (shouldAnim) {
            binding.editLayerLayout.animate()
                .alpha(1f)
                .setDuration(150)
                .scaleX(1f)
                .scaleY(1f)
                .start()
        } else {
            binding.editLayerLayout.alpha = 1f
            binding.editLayerLayout.scaleX = 1f
            binding.editLayerLayout.scaleY = 1f
        }
    }

    override fun hideLayerEditMenu() {
        Log.w("sqsong", "hideLayerEditMenu")
        hideAnimator = binding.editLayerLayout.animate()
            .alpha(0f)
            .setDuration(150)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .withEndAction {
                binding.editLayerLayout.visibility = View.GONE
            }.apply { start() }
    }

    override fun onUndoRedoStateChanged(canUndo: Boolean, canRedo: Boolean) {
        binding.undoIv.setEnableState(canUndo)
        binding.redoIv.setEnableState(canRedo)
    }

}