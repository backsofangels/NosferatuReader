package com.nosferatu.launcher.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.nosferatu.launcher.data.CoverImage
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.scale

class CoverManager(context: Context) {
    private val TAG = "CoverManager"

    // Saving in internal cache
    private val coverDir = File(context.filesDir, "covers").apply {
        if (!exists()) {
            Log.d(TAG, "Creating covers directory at: ${absolutePath}")
            mkdirs()
        }
    }

    /**
     * Takes bytes, compresses them and saves them on disk
     * Returns absolute path of the saved file
     */
    fun saveCover(bookId: Long, coverImage: CoverImage?): String? {
        val data = coverImage?.data ?: run {
            Log.d(TAG, "No cover data provided for book $bookId")
            return null
        }
        val outputFile = File(coverDir, "cover_$bookId.jpg")

        return try {
            Log.d(TAG, "Decoding and saving cover for book $bookId")
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size) ?: run {
                Log.e(TAG, "Failed to decode cover bitmap for book $bookId")
                return null
            }

            // Scaling down because we're not doing fine art photography
            val scaledBitmap = scaleDownIfNeeded(bitmap, 600)

            FileOutputStream(outputFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            Log.d(TAG, "Cover saved successfully for book $bookId at ${outputFile.absolutePath}")
            outputFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cover for book $bookId", e)
            null
        }
    }


    private fun scaleDownIfNeeded(realBitmap: Bitmap, maxResolution: Int): Bitmap {
        val width = realBitmap.width
        val height = realBitmap.height

        if (width <= maxResolution && height <= maxResolution) return realBitmap

        Log.d(TAG, "Scaling down bitmap from ${width}x${height} to max resolution $maxResolution")
        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxResolution
            newHeight = (maxResolution / ratio).toInt()
        } else {
            newHeight = maxResolution
            newWidth = (maxResolution * ratio).toInt()
        }

        return realBitmap.scale(newWidth, newHeight)
    }

    fun deleteCover(path: String?) {
        path?.let { 
            Log.d(TAG, "Deleting cover at: $it")
            File(it).delete() 
        }
    }
}
