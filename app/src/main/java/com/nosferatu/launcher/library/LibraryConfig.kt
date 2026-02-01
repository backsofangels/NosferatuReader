package com.nosferatu.launcher.library

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
        // This should be default, for dev on emulator keeping downloads
        // private val DEFAULT_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
        private val DEFAULT_PATH = Environment.getExternalStorageDirectory().absolutePath
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
}
