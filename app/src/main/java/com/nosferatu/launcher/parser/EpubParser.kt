package com.nosferatu.launcher.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.siegmann.epublib.epub.EpubReader
import java.io.File
import java.io.FileInputStream

class EpubParser : ParserStrategy {
    override suspend fun parse(file: File): RawMetadata = withContext(Dispatchers.IO) {
        FileInputStream(file).use { fis ->
            val book = EpubReader().readEpub(fis)
            val metadata = book.metadata

            val authorName = metadata.authors
                .joinToString(separator = ", ") { "${it.firstname} ${it.lastname}".trim() }
                .takeIf { it.isNotBlank() }

            RawMetadata(
                title = book.title,
                author = authorName
            )
        }
    }
}