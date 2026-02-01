package com.nosferatu.launcher.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.nosferatu.launcher.data.CoverImage
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.scale
import java.util.UUID

class CoverManager(context: Context) {
    private val _tag = "CoverManager"

    private val coverDir = File(context.filesDir, "covers").apply {
        if (!exists()) {
            Log.d(_tag, "Creating covers directory at: ${absolutePath}")
            mkdirs()
        }
    }

    fun saveCover(bookName: String, coverImage: CoverImage?): String? {
        val data = coverImage?.data ?: run {
            Log.d(_tag, "No cover data provided for book $bookName")
            return null
        }

        val coverId = UUID.randomUUID().toString()

        val outputFile = File(coverDir, "cover_$coverId.jpg")

        return try {
            Log.d(_tag, "Decoding and saving cover for book $bookName")
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size) ?: run {
                Log.e(_tag, "Failed to decode cover bitmap for book $bookName")
                return null
            }

            // Scaling down because we're not doing fine art photography
            val scaledBitmap = scaleDownIfNeeded(bitmap, 600)

            FileOutputStream(outputFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            Log.d(_tag, "Cover saved successfully for book $bookName at ${outputFile.absolutePath}")
            outputFile.absolutePath
        } catch (e: Exception) {
            Log.e(_tag, "Error saving cover for book $bookName", e)
            null
        }
    }


    private fun scaleDownIfNeeded(realBitmap: Bitmap, maxResolution: Int): Bitmap {
        val width = realBitmap.width
        val height = realBitmap.height

        if (width <= maxResolution && height <= maxResolution) return realBitmap

        Log.d(_tag, "Scaling down bitmap from ${width}x${height} to max resolution $maxResolution")
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
            Log.d(_tag, "Deleting cover at: $it")
            File(it).delete() 
        }
    }
}
