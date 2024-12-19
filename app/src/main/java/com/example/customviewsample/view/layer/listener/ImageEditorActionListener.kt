package com.example.customviewsample.view.layer.listener

import com.example.customviewsample.data.CanvasSize
import com.example.customviewsample.view.layer.anno.LayerChangedMode
import com.example.customviewsample.view.layer.data.LayerPreviewData

interface ImageEditorActionListener {

    fun onShowLayerEditMenu(x: Float, y: Float)

    fun hideLayerEditMenu()

    /**
     * 撤销重做状态变化.
     * @param canUndo 是否可以撤销
     * @param canRedo 是否可以重做
     * @param isReset 是否重置(撤销恢复即会重置所有图层)
     */
    fun onUndoRedoStateChanged(canUndo: Boolean, canRedo: Boolean, isReset: Boolean)

    fun onCanvasSizeChanged(newCanvasSize: CanvasSize)

    fun onAddOrUpdateLayer(@LayerChangedMode changedMode: Int, layerPreviewData: LayerPreviewData)

}