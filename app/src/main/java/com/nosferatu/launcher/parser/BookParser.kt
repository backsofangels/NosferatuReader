package com.nosferatu.launcher.parser

import com.nosferatu.launcher.data.Ebook
import com.nosferatu.launcher.data.EbookFormat
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

        return Ebook(
            id = UUID.nameUUIDFromBytes(file.absolutePath.toByteArray()).toString(),
            title = raw?.title ?: file.nameWithoutExtension,
            author = raw?.author,
            filePath = file.absolutePath,
            coverPath = null,
            format = format,
            lastModified = file.lastModified(),
            lastReadPosition = 0
        )
    }
}