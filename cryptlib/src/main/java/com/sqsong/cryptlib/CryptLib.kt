package com.sqsong.cryptlib

object CryptLib {

    init {
        System.loadLibrary("cryptlib")
    }

    external fun getDecryptedString(key: Int): String?

    external fun decryptedString(text: String): String?

    external fun encryptString(text: String): String?
}