package com.nosferatu.launcher.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val bg: Color,
    val surface: Color,
    val onBg: Color,
    val onBgMuted: Color,
    val onBgFaint: Color,
    val divider: Color,
    val accent: Color,
    val progressTrack: Color
    ,
    // subtle background used to indicate a selected row in lists
    val selectedRowBackground: Color
)

/** Default light palette for composition-local fallback. */
val LocalAppColors = compositionLocalOf {
    AppColors(
        bg = Color.White,
        surface = Color(0xFFF5F5F5),
        onBg = Color(0xFF333333),
        onBgMuted = Color(0xFF666666),
        onBgFaint = Color(0xFFAAAAAA),
        divider = Color(0xFFE0E0E0),
        accent = Color(0xFFe94560),
        progressTrack = Color(0xFFEEEEEE)
        ,
        selectedRowBackground = Color.Black.copy(alpha = 0.04f)
    )
}

fun appColorsFor(mode: Float): AppColors = when (mode.toInt()) {
    1 -> AppColors(
        bg = Color(0xFFFFF8E1),
        surface = Color(0xFFF0E6D0),
        onBg = Color(0xFF333333),
        onBgMuted = Color(0xFF7A6A50),
        onBgFaint = Color(0xFFB0A080),
        divider = Color(0xFFD8C9A8),
        accent = Color(0xFFe94560),
        progressTrack = Color(0xFFEDE0C8)
        ,
        selectedRowBackground = Color.Black.copy(alpha = 0.04f)
    )
    2 -> AppColors(
        bg = Color(0xFF222222),
        surface = Color(0xFF2C2C2C),
        onBg = Color(0xFFEEEEEE),
        onBgMuted = Color(0xFF999999),
        onBgFaint = Color(0xFF666666),
        divider = Color(0xFF3A3A3A),
        accent = Color(0xFFe94560),
        progressTrack = Color(0xFF444444)
        ,
        selectedRowBackground = Color.White.copy(alpha = 0.06f)
    )
    else -> AppColors(
        bg = Color.White,
        surface = Color(0xFFF5F5F5),
        onBg = Color(0xFF333333),
        onBgMuted = Color(0xFF666666),
        onBgFaint = Color(0xFFAAAAAA),
        divider = Color(0xFFE0E0E0),
        accent = Color(0xFFe94560),
        progressTrack = Color(0xFFEEEEEE)
        ,
        selectedRowBackground = Color.Black.copy(alpha = 0.04f)
    )
}

fun bgColorFor(mode: Float): Color = appColorsFor(mode).bg

fun contentColorFor(mode: Float): Color = appColorsFor(mode).onBg
