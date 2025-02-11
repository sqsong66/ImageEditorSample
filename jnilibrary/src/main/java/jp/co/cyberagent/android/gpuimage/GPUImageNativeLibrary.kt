package jp.co.cyberagent.android.gpuimage

import android.content.Context
import android.content.res.AssetManager


object GPUImageNativeLibrary {

    init {
        System.loadLibrary("gpuimage-library")
    }

    fun getShader(key: Int): String {
        return getShader(null, key - 1)
    }

    private external fun getShader(context: Context?, key: Int): String

    external fun aesDecrypt(context: Context, assetManager: AssetManager, str: String): ByteArray
}