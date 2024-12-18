package com.example.customviewsample.view.layer.listener

interface ImageEditorActionListener {

    fun onShowLayerEditMenu(x: Float, y: Float)

    fun hideLayerEditMenu()

    fun onUndoRedoStateChanged(canUndo: Boolean, canRedo: Boolean)

}