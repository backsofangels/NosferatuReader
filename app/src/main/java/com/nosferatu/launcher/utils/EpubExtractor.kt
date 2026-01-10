package com.nosferatu.launcher.utils

import nl.siegmann.epublib.epub.EpubReader
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.ByteArrayOutputStream

object EpubExtractor {
    fun getAllChapters(filePath: String): List<String> {
        val chapters = mutableListOf<String>()
        try {
            val zipIn = ZipInputStream(FileInputStream(File(filePath)))
            var entry = zipIn.nextEntry

            while (entry != null) {
                // Filtriamo solo i contenuti testuali
                if (entry.name.endsWith(".html") || entry.name.endsWith(".xhtml")) {
                    val content = zipIn.bufferedReader().readText()
                    chapters.add(content)
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
            zipIn.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return chapters
    }

    fun getBookTitle(filePath: String): String? {
        return try {
            val book = EpubReader().readEpub(FileInputStream(File(filePath)))
            book.metadata.firstTitle
        } catch (e: Exception) {
            null
        }
    }

    fun getBookCoverBytes(filePath: String): ByteArray? {
        return try {
            val file = File(filePath)
            val book = EpubReader().readEpub(FileInputStream(file))

            // LOG 1: Il libro viene letto?
            Log.d("DEBUG_COVER", "Lettura libro: ${file.name}")

            val coverImage = book.coverImage
            if (coverImage == null) {
                Log.e("DEBUG_COVER", "Cover non trovata nell'EPUB: ${file.name}")
                return null
            }

            val rawData = coverImage.data
            Log.d("DEBUG_COVER", "Dati grezzi trovati: ${rawData.size} bytes")

            val bitmap = getBookCover(filePath, 300, 450)
            if (bitmap == null) {
                Log.e("DEBUG_COVER", "Bitmap non creata per: ${file.name}")
                return null
            }

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val finalBytes = stream.toByteArray()

            Log.d("DEBUG_COVER", "Compressione completata: ${finalBytes.size} bytes")
            finalBytes
        } catch (e: Exception) {
            Log.e("DEBUG_COVER", "Errore estrazione: ${e.message}")
            null
        }
    }

    fun getBookCover(filePath: String, maxWidth: Int = 300, maxHeight: Int = 450): Bitmap? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null

            val book = EpubReader().readEpub(FileInputStream(file))
            val coverImage = book.coverImage
            if (coverImage == null) Log.e("EPUB", "Cover non trovata nel file: $filePath")
            val imageData = coverImage.data

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(imageData, 0, imageData.size, options)

            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
            options.inJustDecodeBounds = false

            BitmapFactory.decodeByteArray(imageData, 0, imageData.size, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}