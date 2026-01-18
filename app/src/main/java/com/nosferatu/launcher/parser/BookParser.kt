package com.nosferatu.launcher.parser

import android.util.Log
import com.nosferatu.launcher.data.CoverImage
import com.nosferatu.launcher.data.Ebook
import com.nosferatu.launcher.data.EbookFormat
import java.io.File

class BookParser(
    private val strategies: Map<EbookFormat, ParserStrategy>
) {
    private val _tag = "BookParser"
    val supportedFormats: Set<EbookFormat> = strategies.keys

    suspend fun parseMetadata(file: File): Ebook {
        Log.d(_tag, "Parsing metadata for file: ${file.absolutePath}")
        val extension = file.extension
        val format = EbookFormat.fromExtension(extension)
        Log.d(_tag, "Detected format: $format for extension: $extension")

        val strategy = strategies[format] ?: run {
            Log.w(_tag, "No strategy found for format: $format. Returning default ebook.")
            return Ebook(
                title = file.nameWithoutExtension,
                author = "Formato non supportato",
                filePath = file.absolutePath,
                format = format, // Defaulting to EPUB if extension is unknown
                coverImage = null
            )
        }

        val raw = try {
            Log.d(_tag, "Using strategy: ${strategy.javaClass.simpleName}")
            strategy.parse(file)
        } catch (e: Exception) {
            Log.e(_tag, "Error during parsing with strategy ${strategy.javaClass.simpleName}: ${file.absolutePath}", e)
            null
        }

        val ebook = Ebook(
            title = raw?.title ?: file.nameWithoutExtension,
            author = raw?.author ?: "Autore Sconosciuto",
            filePath = file.absolutePath,
            format = format,
            coverImage = raw?.coverData?.let { CoverImage(it) }
        )
        Log.d(_tag, "Successfully created Ebook object: ${ebook.title} by ${ebook.author}")
        return ebook
    }
}
