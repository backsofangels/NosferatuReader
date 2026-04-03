package com.nosferatu.launcher.parser

import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.runBlocking
import com.nosferatu.launcher.data.EbookFormat
import java.io.File

class BookParserTest {
    @Test
    fun parseMetadata_withStrategy_createsEbook() {
        runBlocking {
        val strategy = object : ParserStrategy {
            override suspend fun parse(file: File) = RawMetadata("Title", "Author", byteArrayOf(1, 2))
        }
        val parser = BookParser(mapOf(EbookFormat.EPUB to strategy))
        val file = File("sample.epub")

        val ebook = parser.parseMetadata(file)
        assertEquals("Title", ebook.title)
        assertEquals("Author", ebook.author)
        assertEquals(EbookFormat.EPUB, ebook.format)
        assertNotNull(ebook.coverImage)
        }
    }

    @Test
    fun parseMetadata_strategyThrows_returnsFallback() {
        runBlocking {
        val strategy = object : ParserStrategy {
            override suspend fun parse(file: File): RawMetadata { throw RuntimeException("boom") }
        }
        val parser = BookParser(mapOf(EbookFormat.EPUB to strategy))
        val file = File("broken.epub")

        val ebook = parser.parseMetadata(file)
        assertEquals("broken", ebook.title)
        assertEquals("Autore Sconosciuto", ebook.author)
        assertNull(ebook.coverImage)
        }
    }

    @Test
    fun parseMetadata_noStrategy_returnsUnsupportedBook() {
        runBlocking {
            val parser = BookParser(emptyMap())
            val file = File("legacy.epub")

            val ebook = parser.parseMetadata(file)
            assertEquals("legacy", ebook.title)
            assertEquals("Formato non supportato", ebook.author)
        }
    }
}
