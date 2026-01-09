package com.nosferatu.launcher.utils

import nl.siegmann.epublib.epub.EpubReader
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream
import android.graphics.Bitmap
import android.graphics.BitmapFactory

object EpubExtractor {

    // --- LOGICA DI LETTURA ORIGINALE (FUNZIONANTE) ---

    fun getAllChapters(filePath: String): List<String> {
        val chapters = mutableListOf<String>()
        try {
            val zipIn = ZipInputStream(FileInputStream(File(filePath)))
            var entry = zipIn.nextEntry

            while (entry != null) {
                // Filtriamo solo i contenuti testuali
                if (entry.name.endsWith(".html") || entry.name.endsWith(".xhtml")) {
                    val content = zipIn.bufferedReader().use { it.readText() }
                    chapters.add(content)
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
            zipIn.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Ordiniamo i capitoli per nome file per evitare disordine (es. cap1, cap2...)
        return chapters
    }

    // --- LOGICA METADATI (ISOLATA) ---

    fun getBookTitle(filePath: String): String? {
        return try {
            val book = EpubReader().readEpub(FileInputStream(File(filePath)))
            book.metadata.firstTitle
        } catch (e: Exception) {
            null
        }
    }

    fun getBookAuthor(filePath: String): String {
        return try {
            val book = EpubReader().readEpub(FileInputStream(File(filePath)))
            val authors = book.metadata.authors
            if (authors.isNullOrEmpty()) "Autore Sconosciuto"
            else authors.joinToString(", ") { "${it.firstname} ${it.lastname}".trim() }
        } catch (e: Exception) {
            "Autore Sconosciuto"
        }
    }

    // Aggiungi all'interno dell'oggetto EpubExtractor
    fun getBookCover(filePath: String, maxWidth: Int = 300, maxHeight: Int = 450): Bitmap? {
        return try {
            val file = File(filePath)
            val book = EpubReader().readEpub(FileInputStream(file))
            val coverImage = book.coverImage ?: return null

            val imageData = coverImage.data

            // Configurazione per ridimensionare l'immagine durante la lettura
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true // Legge solo le dimensioni senza caricare in memoria
            }
            BitmapFactory.decodeByteArray(imageData, 0, imageData.size, options)

            // Calcola il fattore di campionamento
            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
            options.inJustDecodeBounds = false

            BitmapFactory.decodeByteArray(imageData, 0, imageData.size, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Funzione di utilità per il risparmio memoria
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