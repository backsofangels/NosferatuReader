package com.nosferatu.launcher.ui.screens.settings

enum class FontSizeOption(val labelRes: Int, val value: Float) {
    SMALL(com.nosferatu.launcher.R.string.font_size_small, 0.8f),
    MEDIUM(com.nosferatu.launcher.R.string.font_size_medium, 1.1f),
    BIG(com.nosferatu.launcher.R.string.font_size_large, 1.5f);

    companion object {
        fun fromValue(value: Float): FontSizeOption =
            entries.find { it.value == value } ?: MEDIUM
    }
}

enum class BackgroundColorOption(val labelRes: Int, val value: Float) {
    WHITE(com.nosferatu.launcher.R.string.color_white, 0.0f),
    CREAM(com.nosferatu.launcher.R.string.color_cream, 1.0f),
    BLACK(com.nosferatu.launcher.R.string.color_black, 2.0f);

    companion object {
        fun fromValue(value: Float): BackgroundColorOption =
            entries.find { kotlin.math.abs(it.value - value) < 0.01f } ?: WHITE
    }
}
