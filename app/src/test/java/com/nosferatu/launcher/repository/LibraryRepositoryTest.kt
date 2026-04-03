package com.nosferatu.launcher.repository

import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.*
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import com.nosferatu.launcher.data.BookDao
import com.nosferatu.launcher.library.CoverManager
import com.nosferatu.launcher.library.LibraryConfig
import com.nosferatu.launcher.library.LibraryScanner
import com.nosferatu.launcher.parser.BookParser
import com.nosferatu.launcher.parser.ParserStrategy
import com.nosferatu.launcher.parser.RawMetadata
import com.nosferatu.launcher.data.EbookFormat

class LibraryRepositoryTest {
    @Test
    fun syncLibrary_rootNotExists_doesNothing() {
        runBlocking {
        val bookDao: BookDao = mock()
        val coverManager: CoverManager = mock()
        val libraryConfig: LibraryConfig = mock()
        val parser = BookParser(mapOf(EbookFormat.EPUB to object : ParserStrategy {
            override suspend fun parse(file: File) = RawMetadata("t","a", null)
        }))
        val scanner = LibraryScanner(parser)
        val repo = LibraryRepository(bookDao, scanner, coverManager, libraryConfig)

        val nonExistent = File("no_such_dir_${System.currentTimeMillis()}")
        whenever(libraryConfig.getRootDirectory()).thenReturn(nonExistent)

        repo.syncLibrary()

        verify(bookDao, never()).insertBook(any())
        }
    }

    @Test
    fun syncLibrary_insertsNewBook() {
        runBlocking {
            val bookDao: BookDao = mock()
            val coverManager: CoverManager = mock()
            val libraryConfig: LibraryConfig = mock()
            val tmp = Files.createTempDirectory("repoTest").toFile()
            val f = File(tmp, "book.epub").apply { writeText("x") }

            val parser = BookParser(mapOf(EbookFormat.EPUB to object : ParserStrategy { override suspend fun parse(file: File) = RawMetadata("t","a", null) }))
            val scanner = LibraryScanner(parser)
            val repo = LibraryRepository(bookDao, scanner, coverManager, libraryConfig)

            whenever(libraryConfig.getRootDirectory()).thenReturn(tmp)
            whenever(bookDao.getBookByPath(f.absolutePath)).thenReturn(null)
            whenever(bookDao.getAllFilePaths()).thenReturn(listOf())
            whenever(coverManager.saveCover(any(), any())).thenReturn("/tmp/cover.jpg")

            repo.syncLibrary()

            verify(bookDao).insertBook(any())
            tmp.deleteRecursively()
        }
    }
}
