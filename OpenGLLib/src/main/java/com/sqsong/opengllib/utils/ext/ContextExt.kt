package com.sqsong.opengllib.utils.ext

import android.content.Context

fun Context.readAssetsText(fileName: String): String {
    return assets.open(fileName).bufferedReader().use { it.readText() }
}