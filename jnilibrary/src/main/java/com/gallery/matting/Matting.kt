package com.gallery.matting

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.Log

object Matting {

    init {
        System.loadLibrary("matting-jni")
    }

    private fun cutoutBitmap(mask: Bitmap, bitmap: Bitmap): Bitmap {
        val newBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_IN))
        canvas.drawBitmap(mask, 0.0f, 0.0f, null)
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, paint)
        return newBitmap
    }

    fun mattingBitmap(segModelPath: String, mattingModelPath: String, bitmap: Bitmap): Bitmap {
        val imageData = ImageData()
        val result = mattingBitmap(segModelPath, mattingModelPath, bitmap, imageData)
        Log.d("sqsong", "mattingBitmap result: $result")
        val mask = imageData.convertToBitmap()
        val matting = cutoutBitmap(mask, bitmap)
        mask.recycle()
        return matting
    }

    private external fun mattingBitmap(path1: String, path2: String, bitmap: Bitmap, imageData: ImageData): Int

}