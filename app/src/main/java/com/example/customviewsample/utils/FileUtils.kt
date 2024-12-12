package com.example.customviewsample.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.spec.SecretKeySpec

fun extractRelativePath(absolutePath: String?, fileName: String): String {
    if (absolutePath.isNullOrEmpty()) return ""
    val externalStorageRootPath = Environment.getExternalStorageDirectory().absolutePath
    return if (absolutePath.startsWith(externalStorageRootPath)) {
        absolutePath.removePrefix("$externalStorageRootPath/").removeSuffix(fileName)
    } else {
        absolutePath.removeSuffix(fileName)
    }
}

fun convertBytesToReadable(sizeInBytes: Long): String {
    val kiloBytes = sizeInBytes / 1024.0
    val megaBytes = kiloBytes / 1024.0
    val gigaBytes = megaBytes / 1024.0

    return when {
        gigaBytes > 1 -> String.format(Locale.getDefault(), "%.2f GB", gigaBytes)
        megaBytes > 1 -> String.format(Locale.getDefault(), "%.2f MB", megaBytes)
        kiloBytes > 1 -> String.format(Locale.getDefault(), "%.2f KB", kiloBytes)
        else -> "$sizeInBytes Bytes"
    }
}

fun getTempImageCachePath(context: Context): String {
    val externalDir: String = context.externalCacheDir?.absolutePath ?: context.cacheDir.absolutePath
    return externalDir.plus(File.separator).plus("TempImage")
}

fun getUndoRedoCacheDirPath(context: Context): String {
    val externalDir: String = context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
    return externalDir.plus(File.separator).plus("UndoRedoCache")
}

fun getLogPath(context: Context): String {
    val externalDir: String = context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
    return externalDir.plus(File.separator).plus("Logs")
}

fun encryptZipFile(zipFile: File, encryptedZipFile: File) {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val key = MessageDigest.getInstance("SHA-256").digest("GQMKbBCeJ0EDCmog7tGe".toByteArray())
    val secretKey = SecretKeySpec(key, "AES")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)

    FileOutputStream(encryptedZipFile).use { outputStream ->
        outputStream.write(cipher.iv)
        CipherOutputStream(outputStream, cipher).use { cipherOutputStream ->
            FileInputStream(zipFile).use { inputStream ->
                inputStream.copyTo(cipherOutputStream)
            }
        }
    }
}

fun zipLogFiles(context: Context): Uri? {
    val logDir = getLogPath(context)
    // 使用.foo后缀，Gmail邮箱无法接收.zip后缀文件
    val zipFile = File(logDir, "Logs.foo")
    if (zipFile.exists()) zipFile.delete()
    if (zipFile.parentFile?.exists() == false) {
        zipFile.parentFile?.mkdirs()
    }
    val zipResult = zipFolder(File(logDir), zipFile)
    if (!zipResult) return null
    val zipEncryptedFile = File(logDir, "Logs_encrypted.zip")
    encryptZipFile(zipFile, zipEncryptedFile)
    // 删除原zip文件
    zipFile.delete()
    // 重命名加密后的zip文件
    zipEncryptedFile.renameTo(zipFile)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
}

fun zipFolder(sourceFolder: File, zipFile: File): Boolean {
    val zipSuccess = try {
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOutputStream ->
            if (!flatZipFolder(sourceFolder, sourceFolder, zipOutputStream)) {
                return false
            }
        }
        true
    } catch (e: IOException) {
        e.printStackTrace()
        false
    }
    if (!zipSuccess && zipFile.exists()) {
        zipFile.delete()
    }
    return zipSuccess
}

private fun flatZipFolder(rootFolder: File, sourceFolder: File, zipOutputStream: ZipOutputStream): Boolean {
    val files = sourceFolder.listFiles() ?: return false
    for (file in files) {
        when {
            file.isDirectory -> {
                if (!flatZipFolder(rootFolder, file, zipOutputStream)) {
                    return false
                }
            }

            file.extension == "zip" || file.extension == "foo" -> { // 跳过压缩的Log文件本身
                continue
            }

            else -> {
                FileInputStream(file).use { fileInputStream ->
                    val zipEntryName = rootFolder.toURI().relativize(file.toURI()).path
                    zipOutputStream.putNextEntry(ZipEntry(zipEntryName))
                    val buffer = ByteArray(2048)
                    var length: Int
                    while (fileInputStream.read(buffer).also { length = it } > 0) {
                        zipOutputStream.write(buffer, 0, length)
                    }
                    zipOutputStream.closeEntry()
                }
            }
        }
    }
    return true
}

/**
 * 删除过期的日志文件
 */
fun deleteExpiredLogFiles(context: Context) {
    // 最多保留最近的10条日志记录数据
    val logDir = getLogPath(context)

    // 先删除压缩的zip日志文件
    File(logDir).listFiles()?.forEach { file ->
        if (!file.isDirectory) file.delete()
    }

    val logFiles = File(logDir).listFiles()
    if (logFiles.isNullOrEmpty() || logFiles.size <= 10) return
    logFiles.sortedBy { it.lastModified() }.take(logFiles.size - 10).forEach {
        it.delete()
    }
}

