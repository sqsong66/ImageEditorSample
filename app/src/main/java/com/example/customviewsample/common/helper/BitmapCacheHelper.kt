package com.example.customviewsample.common.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.example.customviewsample.utils.getTempImageCachePath
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class BitmapCacheHelper private constructor() {

    private var memoryCache: LruCache<String, Bitmap>

    init {
        // 设置LruCache的最大缓存值为系统分配给应用内存的1/8
        val cacheSize: Int = (Runtime.getRuntime().maxMemory() / 8).toInt()
        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int = bitmap.byteCount
        }
    }

    private fun generateBitmapKey(bitmap: Bitmap): String {
        val originKey = "${bitmap.width}_${bitmap.height}_${bitmap.config}_${bitmap.hashCode()}"
        return try {
            val bytes = MessageDigest.getInstance("MD5").digest(originKey.toByteArray())
            val sb = StringBuilder()
            for (b in bytes) {
                sb.append(String.format("%02x", b))
            }
            sb.toString()
        } catch (ex: NoSuchAlgorithmException) {
            originKey.hashCode().toString()
        }
    }

    fun cacheBitmap(context: Context, bitmap: Bitmap?, isPNG: Boolean): String? {
        if (bitmap == null) return null
        val bitmapKey = generateBitmapKey(bitmap)
        memoryCache.get(bitmapKey)?.let { return bitmapKey }
        try {
            val file = File(getTempImageCachePath(context), bitmapKey)
            if (file.parentFile?.exists() == false) file.parentFile?.mkdirs()
            val format = if (isPNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            FileOutputStream(file).use { stream -> bitmap.compress(format, 100, stream) }
            memoryCache.put(bitmapKey, bitmap)
            return bitmapKey
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    fun getCachedBitmap(context: Context, cacheKey: String?): Bitmap? {
        if (cacheKey.isNullOrEmpty()) return null
        val bitmap = memoryCache.get(cacheKey)
        if (bitmap != null) return bitmap
        try {
            val cachePath = File(getTempImageCachePath(context), cacheKey)
            val decodeBitmap = BitmapFactory.decodeFile(cachePath.absolutePath)
            memoryCache.put(cacheKey, decodeBitmap)
            return decodeBitmap
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }


    companion object {
        @Volatile
        private var instance: BitmapCacheHelper? = null

        fun get(): BitmapCacheHelper = instance ?: synchronized(this) {
            instance ?: BitmapCacheHelper().also { instance = it }
        }
    }

}