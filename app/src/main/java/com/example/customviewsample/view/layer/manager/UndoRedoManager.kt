package com.example.customviewsample.view.layer.manager

import com.example.customviewsample.view.layer.data.ImageLayerSnapshot
import java.util.ArrayDeque

class UndoRedoManager {

    private val undoStack = ArrayDeque<ImageLayerSnapshot>()
    private val redoStack = ArrayDeque<ImageLayerSnapshot>()

    fun saveSnapshot(snapshot: ImageLayerSnapshot) {
        undoStack.push(snapshot)
        redoStack.clear()
    }

    fun canUndo(): Boolean = undoStack.size > 1

    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo(): ImageLayerSnapshot? {
        if (!canUndo()) return null
        val currentSnapshot = undoStack.pop()
        redoStack.push(currentSnapshot)
        return undoStack.peek()
    }

    fun redo(): ImageLayerSnapshot? {
        if (!canRedo()) return null
        val currentSnapshot = redoStack.pop()
        undoStack.push(currentSnapshot)
        return currentSnapshot
    }

}