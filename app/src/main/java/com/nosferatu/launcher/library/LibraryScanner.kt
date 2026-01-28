package com.nosferatu.launcher.library

import android.util.Log
import com.nosferatu.launcher.data.Ebook
import com.nosferatu.launcher.parser.BookParser
import java.io.File

class LibraryScanner(private val parser: BookParser) {
    private val _tag = "LibraryScanner"

    fun scanDirectory(directory: File): List<File> {
        Log.d(_tag, "Scanning directory: ${directory.absolutePath}")
        val formats = parser.supportedFormats.map { it.name.lowercase() }.toSet()
        Log.d(_tag, "Supported formats: $formats")

        val files = directory.walkTopDown()
            .onEnter { folder ->
                !folder.name.startsWith(".") && folder.name != "Android"
            }
            .maxDepth(2)
            .filter { it.isFile && it.extension.lowercase() in formats }
            .toList()
        
        Log.d(_tag, "Found ${files.size} supported files in ${directory.absolutePath}")
        return files
    }

    suspend fun extractMetadata(file: File): Ebook? {
        Log.d(_tag, "Extracting metadata for: ${file.absolutePath}")
        return try {
            val ebook = parser.parseMetadata(file)
            Log.d(_tag, "Successfully parsed metadata for: ${file.name} (Title: ${ebook.title})")
            ebook
        } catch (e: Exception) {
            Log.e(_tag, "Error parsing file: ${file.absolutePath}", e)
            Log.e(_tag, "Exception message: ${e.message}")
            null
        }
    }
}
