package com.nosferatu.launcher.library

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import com.nosferatu.launcher.parser.ParserStrategy
import com.nosferatu.launcher.parser.RawMetadata
import com.nosferatu.launcher.data.EbookFormat
import com.nosferatu.launcher.parser.BookParser
import kotlinx.coroutines.runBlocking

class LibraryScannerTest {
    @Test
    fun scanDirectory_filtersAndRespectsDepth() {
        val temp = Files.createTempDirectory("libscan").toFile()
        try {
            val f1 = File(temp, "a.epub").apply { writeText("x") }
            val sub = File(temp, "sub").apply { mkdirs() }
            val f2 = File(sub, "b.epub").apply { writeText("x") }
            val deep = File(sub, "deep").apply { mkdirs() }
            val f3 = File(deep, "c.epub").apply { writeText("x") }

            val strategy = object : ParserStrategy {
                override suspend fun parse(file: File) = RawMetadata(file.nameWithoutExtension, null, null)
            }
            val parser = BookParser(mapOf(EbookFormat.EPUB to strategy))
            val scanner = LibraryScanner(parser)

            val result = scanner.scanDirectory(temp)
            assertTrue(result.any { it.name == "a.epub" })
            assertTrue(result.any { it.name == "b.epub" })
            assertFalse(result.any { it.name == "c.epub" })
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test
    fun extractMetadata_returnsEbookOrNull() {
        runBlocking {
        val strategy = object : ParserStrategy { override suspend fun parse(file: File) = RawMetadata("t","a", null) }
        val parser = BookParser(mapOf(EbookFormat.EPUB to strategy))
        val scanner = LibraryScanner(parser)

        val tmp = Files.createTempDirectory("extract").toFile()
        val f = File(tmp, "book.epub").apply { writeText("x") }
        try {
            val ebook = scanner.extractMetadata(f)
            assertNotNull(ebook)
            assertEquals("t", ebook?.title)
        } finally {
            tmp.deleteRecursively()
        }
    }
}
}
