package com.nosferatu.launcher.library

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import java.io.File
import androidx.core.content.edit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class LibraryConfig(private val context: Context) {
    private val TAG = "LibraryConfig"
    private val prefs = context.getSharedPreferences("library_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ROOT_PATH = "root_path"
        private const val KEY_BACKGROUND_MODE = "background_mode"
        private const val KEY_FONT_SIZE = "font_size_scale"
        private const val KEY_LINE_HEIGHT = "line_height_factor"
        private const val KEY_PAGE_MARGINS = "page_margins"
        private val DEFAULT_PATH = Environment.getExternalStorageDirectory().absolutePath
        private const val DEFAULT_FONT_SIZE = 1.1f
        private const val DEFAULT_LINE_HEIGHT = 1.5f
        private const val DEFAULT_PAGE_MARGINS = 1.0f
        private const val DEFAULT_BACKGROUND_MODE = 0.0f // 0 = bianco, 1 = panna, 2 = nero
    }

    var rootPath: String
        get() {
            val path = prefs.getString(KEY_ROOT_PATH, DEFAULT_PATH) ?: DEFAULT_PATH
            Log.d(TAG, "Getting root path: $path")
            return path
        }
        set(value) {
            Log.d(TAG, "Setting root path to: $value")
            prefs.edit { putString(KEY_ROOT_PATH, value) }
        }

    fun getRootDirectory(): File {
        val dir = File(rootPath)
        Log.d(TAG, "Root directory: ${dir.absolutePath} (exists: ${dir.exists()})")
        return dir
    }

    var fontSizeScale by mutableFloatStateOf(
        prefs.getFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE)
    )
        private set

    var lineHeightFactor by mutableFloatStateOf(prefs.getFloat(KEY_LINE_HEIGHT, DEFAULT_LINE_HEIGHT))
        private set

    fun updateFontSize(newValue: Float) {
        fontSizeScale = newValue
        prefs.edit().putFloat(KEY_FONT_SIZE, newValue).apply()
        Log.d(TAG, "Font size scale aggiornato e salvato: $newValue")
    }

    fun updateLineHeight(newValue: Float) {
        lineHeightFactor = newValue
        prefs.edit().putFloat(KEY_LINE_HEIGHT, newValue).apply()
        Log.d(TAG, "Line height scale aggiornato e salvato: $newValue")
    }

    var pageMargins by mutableFloatStateOf(prefs.getFloat(KEY_PAGE_MARGINS, DEFAULT_PAGE_MARGINS))
        private set

    fun updatePageMargins(newValue: Float) {
        pageMargins = newValue
        prefs.edit().putFloat(KEY_PAGE_MARGINS, newValue).apply()
    }

    var backgroundMode by mutableFloatStateOf(prefs.getFloat(KEY_BACKGROUND_MODE, DEFAULT_BACKGROUND_MODE))
        private set

    fun updateBackgroundMode(newValue: Float) {
        backgroundMode = newValue
        prefs.edit().putFloat(KEY_BACKGROUND_MODE, newValue).apply()
        Log.d(TAG, "Background mode aggiornato e salvato: $newValue")
    }
}
