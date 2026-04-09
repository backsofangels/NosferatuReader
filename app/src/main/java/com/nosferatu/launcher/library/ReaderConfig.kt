package com.nosferatu.launcher.library

import android.content.Context
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class ReaderConfig(private val context: Context) {
    private val prefs = context.getSharedPreferences("reader_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_READING_BACKGROUND_MODE = "reading_background_mode"
    }

    // -1f means "use global app background mode"
    var readingBackgroundMode by mutableFloatStateOf(prefs.getFloat(KEY_READING_BACKGROUND_MODE, -1f))
        private set

    fun updateReadingBackgroundMode(newValue: Float) {
        readingBackgroundMode = newValue
        prefs.edit().putFloat(KEY_READING_BACKGROUND_MODE, newValue).apply()
    }

    fun clearReadingBackgroundMode() {
        readingBackgroundMode = -1f
        prefs.edit().remove(KEY_READING_BACKGROUND_MODE).apply()
    }
}
