package com.example.customviewsample.common.ext

import android.content.Intent

inline fun <T1 : Any, T2 : Any, R> letIfNotNull(a: T1?, b: T2?, block: (T1, T2) -> R) {
    if (a != null && b != null) block(a, b)
}

inline fun <T1 : Any, T2 : Any, T3 : Any, R> letIfNotNull(a: T1?, b: T2?, c: T3?, block: (T1, T2, T3) -> R) {
    if (a != null && b != null && c != null) block(a, b, c)
}

inline fun <reified T : Enum<T>> Intent.putEnumExtra(victim: T): Intent =
    putExtra(T::class.java.name, victim.ordinal)

inline fun <reified T: Enum<T>> Intent.getEnumExtra(): T? =
    getIntExtra(T::class.java.name, -1)
        .takeUnless { it == -1 }
        ?.let { T::class.java.enumConstants?.get(it) }