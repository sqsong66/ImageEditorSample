package com.sqsong.opengllib.filters

import android.content.Context
import com.sqsong.opengllib.common.FrameBuffer
import com.sqsong.opengllib.common.GLVertexLinker
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture

open class GroupImageFilter(
    context: Context,
    private val filters: MutableList<BaseImageFilter> = mutableListOf()
) : BaseImageFilter(context) {

    private val mergedFilters by lazy { mutableListOf<BaseImageFilter>() }

    init {
        updateMergedFilters()
    }

    override fun onInitialized(program: Program) {
        filters.forEach { filter ->
            filter.ifNeedInit()
        }
    }

    override fun onInputTextureLoaded(textureWidth: Int, textureHeight: Int) {
        super.onInputTextureLoaded(textureWidth, textureHeight)
        filters.forEach { filter ->
            filter.onInputTextureLoaded(textureWidth, textureHeight)
        }
    }

    override fun onBeforeFrameBufferDraw(inputTexture: Texture, fboProgram: Program?, defaultFboGLVertexLinker: GLVertexLinker): Texture? {
        // 最开始输入的纹理为原始纹理，然后经过一系列的滤镜处理，最终返回最终处理后的纹理
        var texture: Texture? = inputTexture
        mergedFilters.forEach { filter ->
            texture?.let { filter.onDrawFrame(it) }
            texture = filter.fboTexture()
        }
        return texture
    }

    fun addFilter(filter: BaseImageFilter) {
        filters.add(filter)
        updateMergedFilters()
    }

    private fun updateMergedFilters() {
        mergedFilters.clear()
        filters.forEach { filter ->
            if (filter is GroupImageFilter) {
                filter.updateMergedFilters()
                val subFilters = filter.mergedFilters
                mergedFilters.addAll(subFilters)
            } else {
                mergedFilters.add(filter)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        filters.forEach { filter ->
            filter.onDestroy()
        }
    }

}