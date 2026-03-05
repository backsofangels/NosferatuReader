package com.nosferatu.launcher.ui.screens.settings

import kotlin.math.abs

enum class FontSizeOption(val label: String, val value: Float) {
    SMALL("Piccola", 0.8f),
    MEDIUM("Media", 1.1f),
    BIG("Grande", 1.5f);

    companion object {
        fun fromValue(value: Float): FontSizeOption =
            entries.find { it.value == value } ?: MEDIUM
    }
}

enum class LineHeightOption(val label: String, val value: Float) {
    SMALL("Stretta", 1.1f),
    MEDIUM("Normale", 1.5f),
    WIDE("Ampia", 2.0f);

    companion object {
        fun fromValue(value: Float): LineHeightOption =
            entries.find { abs(it.value - value) < 0.01f } ?: MEDIUM
    }
}