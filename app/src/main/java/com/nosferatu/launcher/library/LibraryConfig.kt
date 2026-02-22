package com.nosferatu.launcher.library

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import androidx.core.content.edit

class LibraryConfig(private val context: Context) {
    private val TAG = "LibraryConfig"
    private val prefs = context.getSharedPreferences("library_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ROOT_PATH = "root_path"
        private const val KEY_FONT_SIZE = "font_size_scale"
        private val DEFAULT_PATH = Environment.getExternalStorageDirectory().absolutePath
        private const val DEFAULT_FONT_SIZE = 1.0f
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

    var fontSizeScale: Float
        get() = prefs.getFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE)
        @SuppressLint("UseKtx")
        set(value) {
            prefs.edit().putFloat(KEY_FONT_SIZE, value).apply()
            Log.d(TAG, "Font size scale salvato: $value")
        }
}
