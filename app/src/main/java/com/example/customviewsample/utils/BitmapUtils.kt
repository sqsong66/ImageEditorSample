package com.example.customviewsample.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import com.bumptech.glide.Glide
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
    val bWidth = bitmap.width
    val bHeight = bitmap.height
    if (bWidth <= maxSize && bHeight <= maxSize) {
        return bitmap
    }
    val scale = if (bWidth > bHeight) {
        maxSize.toFloat() / bWidth
    } else {
        maxSize.toFloat() / bHeight
    }
    val matrix = Matrix().apply { postScale(scale, scale) }
    return Bitmap.createBitmap(bitmap, 0, 0, bWidth, bHeight, matrix, true)
}

/**
 * 使用[Glide]进行图片加载。比[decodeBitmapFromUri]更加高效。
 * @param context 上下文
 * @param uri 图片Uri
 * @param maxSize 图片最大尺寸
 * @return 返回解码后的Bitmap
 */
fun decodeBitmapByGlide(context: Context, uri: Uri?, maxSize: Int = 4096): Bitmap? {
    uri ?: return null
    val inputStream = context.contentResolver.openInputStream(uri)
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeStream(inputStream, null, options)
    inputStream?.close()
    options.inJustDecodeBounds = false

    val destMax = min(max(options.outWidth, options.outHeight), maxSize)
    val destWidth: Int
    val destHeight: Int
    if (options.outWidth > options.outHeight) {
        destWidth = destMax
        destHeight = options.outHeight * destWidth / options.outWidth
    } else {
        destHeight = destMax
        destWidth = options.outWidth * destHeight / options.outHeight
    }
    return Glide.with(context).asBitmap().load(uri).submit(destWidth, destHeight).get()
}

fun decodeBitmapFromUri(context: Context, uri: Uri?, maxSize: Int): Bitmap? {
    uri ?: return null
    return try {
        // 第一步: 解码图片尺寸
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }

        // 第二步: 计算缩放比例
        options.apply {
            inSampleSize = calculateInSampleSize(this, maxSize)
            inJustDecodeBounds = false
        }

        // 第三步: 解码Bitmap
        val decodedBitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }

        // 第四步: 调整Bitmap尺寸
        decodedBitmap?.let { resizeBitmap(it, maxSize) }
    } catch (ex: Exception) {
        ex.printStackTrace()
        null
    }
}

fun decodeBitmapFromResource(context: Context, resId: Int, maxSize: Int): Bitmap? {
    return try {
        // 第一步: 解码图片尺寸
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeResource(context.resources, resId, options)

        // 第二步: 计算缩放比例
        options.apply {
            inSampleSize = calculateInSampleSize(this, maxSize)
            inJustDecodeBounds = false
        }

        // 第三步: 解码Bitmap
        val decodedBitmap = BitmapFactory.decodeResource(context.resources, resId, options)

        // 第四步: 调整Bitmap尺寸
        decodedBitmap?.let { resizeBitmap(it, maxSize) }
    } catch (ex: Exception) {
        ex.printStackTrace()
        null
    }
}

fun decodeBitmapFromAssets(context: Context, assetPath: String, maxSize: Int): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(context.assets.open(assetPath), null, options)
        val destWidth: Int
        val destHeight: Int
        val destMax = min(max(options.outWidth, options.outHeight), maxSize)
        if (options.outWidth > options.outHeight) {
            destWidth = destMax
            destHeight = options.outHeight * destWidth / options.outWidth
        } else {
            destHeight = destMax
            destWidth = options.outWidth * destHeight / options.outHeight
        }
        Glide.with(context).asBitmap().load("file:///android_asset/$assetPath").submit(destWidth, destHeight).get()?.let {
            // Log.i("songmao", "decodeBitmapFromAssets, submit bitmap size: ${it.width} * ${it.height}")
            resizeBitmap(it, maxSize)
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
        null
    }
}

fun calculateInSampleSize(options: BitmapFactory.Options, maxSize: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > maxSize || width > maxSize) {
        val halfHeight = height / 2
        val halfWidth = width / 2

        while ((halfHeight / inSampleSize) >= maxSize && (halfWidth / inSampleSize) >= maxSize) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}


fun decodeBitmapToRawBytes(context: Context, uri: Uri?, maxSize: Int): ByteArray? {
    val bitmap = decodeBitmapFromUri(context, uri, maxSize) ?: return null
    // Log.d("songmao", "bitmap: ${bitmap.width} * ${bitmap.height}, size: ${convertBytesToReadable(bitmap.byteCount.toLong())}")
    return ByteArrayOutputStream().use { stream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        stream.toByteArray()
    }
}