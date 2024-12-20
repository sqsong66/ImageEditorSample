package com.sqsong.nativelib

import android.graphics.Bitmap
import android.graphics.Path

object NativeLib {

    // Used to load the 'nativelib' library on application startup.
    init {
        System.loadLibrary("nativelib")
    }

    external fun getBitmapOutlinePath(bitmap: Bitmap): Path

    external fun cutoutBitmapBySource(cutoutBitmap: Bitmap, srcBitmap: Bitmap): Bitmap?

    external fun hasAlpha(bitmap: Bitmap): Boolean
}