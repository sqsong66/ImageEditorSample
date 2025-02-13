package com.sqsong.cryptlib

object CryptLib {

    init {
        System.loadLibrary("cryptlib")
    }

    fun getDecryptedShader(key: Int): String {
        return getDecryptedString(key) ?: ""
    }

    external fun getDecryptedString(key: Int): String?

    external fun decryptedString(text: String): String?

    external fun encryptString(text: String): String?
}