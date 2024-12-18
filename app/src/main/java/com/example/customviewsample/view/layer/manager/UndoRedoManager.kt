package com.example.customviewsample.view.layer.manager

import android.util.Log
import com.example.customviewsample.view.layer.data.ImageLayerSnapshot

class UndoRedoManager {

    private val undoList = mutableListOf<ImageLayerSnapshot>()
    private val redoList = mutableListOf<ImageLayerSnapshot>()

    fun saveSnapshot(snapshot: ImageLayerSnapshot) {
        undoList.add(snapshot)
        redoList.clear()
        Log.d("songmao", "saveLayerSnapshot: undoList size = ${undoList.size}, $undoList")
    }

    fun canUndo(): Boolean = undoList.size > 1

    fun canRedo(): Boolean = redoList.isNotEmpty()

    fun undo(): ImageLayerSnapshot? {
        if (!canUndo()) return null
        val currentSnapshot = undoList.removeAt(undoList.lastIndex)
        redoList.add(currentSnapshot)
        Log.i("songmao", "undoList size = ${undoList.size}, $undoList")
        return undoList.lastOrNull()?.copy()
    }

    fun redo(): ImageLayerSnapshot? {
        if (!canRedo()) return null
        val currentSnapshot = redoList.removeAt(redoList.lastIndex)
        undoList.add(currentSnapshot.copy())
        Log.w("songmao", "redoList size = ${redoList.size}")
        return currentSnapshot
    }

}