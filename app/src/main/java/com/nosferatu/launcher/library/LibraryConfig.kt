package com.nosferatu.launcher.library

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
        private const val KEY_FONT_CHOICE = "font_choice"
        private const val KEY_FORCE_BOLD = "force_bold"
        private const val KEY_VOLUME_KEYS = "volume_keys"
        private const val KEY_INVERT_TOUCHES = "invert_touches"
        // Avoid accessing Android Environment at class initialization time so JVM unit tests
        // can instantiate/mocking this class without triggering Android API calls.
        private val DEFAULT_PATH: String
            get() = try {
                Environment.getExternalStorageDirectory().absolutePath
            } catch (t: Throwable) {
                // Fallback to user home on non-Android environments (tests)
                System.getProperty("user.home") ?: "/"
            }
        // 🔴 OPTIMIZATION: Reduced from 1.1f to 0.85f for low-memory devices
        // Smaller font = smaller rendering viewport = fewer tiles = less memory
        private const val DEFAULT_FONT_SIZE = 0.85f
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

    var fontChoice by mutableFloatStateOf(
        prefs.getFloat(KEY_FONT_CHOICE, 0.0f)
    )
        private set

    fun updateFontChoice(newValue: Float) {
        fontChoice = newValue
        prefs.edit().putFloat(KEY_FONT_CHOICE, newValue).apply()
    }

    fun updateFontSize(newValue: Float) {
        fontSizeScale = newValue
        prefs.edit().putFloat(KEY_FONT_SIZE, newValue).apply()
        Log.d(TAG, "Font size scale aggiornato e salvato: $newValue")
    }

    var backgroundMode by mutableFloatStateOf(prefs.getFloat(KEY_BACKGROUND_MODE, DEFAULT_BACKGROUND_MODE))
        private set

    fun updateBackgroundMode(newValue: Float) {
        backgroundMode = newValue
        prefs.edit().putFloat(KEY_BACKGROUND_MODE, newValue).apply()
        Log.d(TAG, "Background mode aggiornato e salvato: $newValue")
    }

    var forceBold by mutableStateOf(prefs.getBoolean(KEY_FORCE_BOLD, false))
        private set

    fun updateForceBold(newValue: Boolean) {
        forceBold = newValue
        prefs.edit().putBoolean(KEY_FORCE_BOLD, newValue).apply()
        Log.d(TAG, "Force bold aggiornato e salvato: $newValue")
    }

    var volumeKeys by mutableStateOf(prefs.getBoolean(KEY_VOLUME_KEYS, false))
        private set

    fun updateVolumeKeys(newValue: Boolean) {
        volumeKeys = newValue
        prefs.edit().putBoolean(KEY_VOLUME_KEYS, newValue).apply()
        Log.d(TAG, "Volume keys aggiornato e salvato: $newValue")
    }

    var invertTouches by mutableStateOf(prefs.getBoolean(KEY_INVERT_TOUCHES, false))
        private set

    fun updateInvertTouches(newValue: Boolean) {
        invertTouches = newValue
        prefs.edit().putBoolean(KEY_INVERT_TOUCHES, newValue).apply()
        Log.d(TAG, "Invert touches aggiornato e salvato: $newValue")
    }
}
