package com.sviridova.nexus.util

import android.content.res.Resources
import android.view.View

fun withAlpha(color: Int, alpha: Int): Int =
    (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

fun Resources.density(): Float = displayMetrics.density

fun View.density(value: Float): Float = value * resources.displayMetrics.density

fun String.cyrUpper(): String = uppercase(java.util.Locale("ru", "RU"))

fun formatSigned(temp: Int): String = if (temp > 0) "+$temp°" else "$temp°"
