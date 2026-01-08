package com.nosferatu.launcher.utils

import java.io.InputStream
import java.util.zip.ZipInputStream

object EpubExtractor {
    fun getFirstChapter(filePath: String): String {
        val zipIn = ZipInputStream(java.io.File(filePath).inputStream())
        var entry = zipIn.nextEntry

        while (entry != null) {
            if (entry.name.endsWith(".html") || entry.name.endsWith(".xhtml")) {
                return zipIn.bufferedReader().readText()
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }
        zipIn.close()
        return "<html><body>Errore: Nessun contenuto trovato</body></html>"
    }

    fun getAllChapters(filePath: String): List<String> {
        val chapters = mutableListOf<String>()
        val zipIn = ZipInputStream(java.io.File(filePath).inputStream())
        var entry = zipIn.nextEntry

        while (entry != null) {
            if (entry.name.endsWith(".html") || entry.name.endsWith(".xhtml")) {
                // Leggiamo il contenuto e lo aggiungiamo alla lista
                chapters.add(zipIn.bufferedReader().readText())
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }
        zipIn.close()
        return chapters
    }
}