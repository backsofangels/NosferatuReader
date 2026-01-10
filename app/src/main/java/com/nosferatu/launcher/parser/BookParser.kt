package com.nosferatu.launcher.parser

import android.util.Log
import com.nosferatu.launcher.data.Ebook
import com.nosferatu.launcher.data.EbookFormat
import com.nosferatu.launcher.utils.EpubExtractor
import java.io.File
import java.util.UUID

class BookParser {
    private val strategies: Map<EbookFormat, ParserStrategy> = mapOf(
        EbookFormat.EPUB to EpubParser()
        // TODO: EbookFormat.PDF to PdfParser()
    )

    suspend fun parseMetadata(file: File): Ebook {
        val format = EbookFormat.fromExtension(file.extension) ?: EbookFormat.EPUB
        val strategy = strategies[format]

        val raw = if (strategy != null) {
            try {
                strategy.parse(file)
            } catch (e: Exception) {
                null
            }
        } else null

        val coverBytes = if (format == EbookFormat.EPUB) {
            val bytes = EpubExtractor.getBookCoverBytes(file.absolutePath)
            Log.d("DEBUG_PARSER", "Cover per ${file.name}: ${bytes?.size ?: 0} bytes")
            bytes
        } else null

        return Ebook(
            id = UUID.nameUUIDFromBytes(file.absolutePath.toByteArray()).toString(),
            title = raw?.title ?: file.nameWithoutExtension,
            author = raw?.author,
            filePath = file.absolutePath,
            coverData = coverBytes,
            format = format,
            lastModified = file.lastModified(),
            lastReadPosition = 0
        )
    }
}