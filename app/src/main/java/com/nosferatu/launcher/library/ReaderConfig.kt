package com.nosferatu.launcher.library

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class ReaderConfig(private val context: Context) {
    private val prefs = context.getSharedPreferences("reader_prefs", Context.MODE_PRIVATE)
    private val TAG = "ReaderConfig"

    init {
        val loaded = prefs.getFloat(KEY_READING_BACKGROUND_MODE, -1f)
        Log.d(TAG, "init: loaded readingBackgroundMode=$loaded from reader_prefs")
    }

    companion object {
        private const val KEY_READING_BACKGROUND_MODE = "reading_background_mode"
    }

    // -1f means "use global app background mode"
    var readingBackgroundMode by mutableFloatStateOf(prefs.getFloat(KEY_READING_BACKGROUND_MODE, -1f))
        private set

    fun updateReadingBackgroundMode(newValue: Float) {
        val committed = prefs.edit().putFloat(KEY_READING_BACKGROUND_MODE, newValue).commit()
        if (committed) {
            readingBackgroundMode = newValue
            Log.d(TAG, "updateReadingBackgroundMode: saved $newValue to reader_prefs (committed)")
        } else {
            readingBackgroundMode = newValue
            Log.w(TAG, "updateReadingBackgroundMode: failed to commit $newValue to reader_prefs, but updated in-memory value")
        }
    }

    fun clearReadingBackgroundMode() {
        readingBackgroundMode = -1f
        prefs.edit().remove(KEY_READING_BACKGROUND_MODE).apply()
    }
}
