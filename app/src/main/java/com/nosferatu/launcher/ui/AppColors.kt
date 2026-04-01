package com.nosferatu.launcher.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(val bg: Color, val onBg: Color)

/** Accessed anywhere in the Compose tree without passing LibraryConfig down every call-site. */
val LocalAppColors = compositionLocalOf { AppColors(Color.White, Color.Black) }

fun bgColorFor(mode: Float): Color = when (mode.toInt()) {
    1 -> Color(0xFFFFF8E1)  // cream
    2 -> Color(0xFF222222)  // dark charcoal — easier on the eyes than pure black
    else -> Color.White
}

fun contentColorFor(mode: Float): Color =
    if (mode.toInt() == 2) Color(0xFFEEEEEE) else Color.Black

fun appColorsFor(mode: Float) = AppColors(
    bg = bgColorFor(mode),
    onBg = contentColorFor(mode)
)
