package com.example.customviewsample.view.layer.manager

import android.util.Log
import com.example.customviewsample.view.layer.data.ImageEditorSnapshot

class UndoRedoManager {

    private val undoList = mutableListOf<ImageEditorSnapshot>()
    private val redoList = mutableListOf<ImageEditorSnapshot>()

    fun saveSnapshot(snapshot: ImageEditorSnapshot) {
        undoList.add(snapshot)
        redoList.clear()
        Log.d("sqsong", "saveSnapshot: undoList size = ${undoList.size}")
    }

    fun canUndo(): Boolean = undoList.size > 1

    fun canRedo(): Boolean = redoList.isNotEmpty()

    fun undo(): ImageEditorSnapshot? {
        if (!canUndo()) return null
        val currentSnapshot = undoList.removeAt(undoList.lastIndex)
        redoList.add(currentSnapshot)
        return undoList.lastOrNull()
    }

    fun redo(): ImageEditorSnapshot? {
        if (!canRedo()) return null
        val currentSnapshot = redoList.removeAt(redoList.lastIndex)
        undoList.add(currentSnapshot.copy())
        return currentSnapshot
    }

    fun clear() {
        undoList.clear()
        redoList.clear()
    }
}