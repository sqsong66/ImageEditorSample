package com.example.customviewsample.common.ext

import android.content.Context
import android.view.LayoutInflater

fun Context.layoutInflater(): LayoutInflater = LayoutInflater.from(this)