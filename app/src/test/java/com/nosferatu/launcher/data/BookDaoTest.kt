package com.nosferatu.launcher.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import org.robolectric.RuntimeEnvironment
import com.nosferatu.launcher.data.database.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: BookDao

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .build()
        dao = db.bookDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndGetBook() = runBlocking {
        val book = EbookEntity(title = "Test", author = "Author", filePath = "/tmp/book.epub", lastModified = 1L, format = "EPUB")
        dao.insertBook(book)
        val retrieved = dao.getBookByPath("/tmp/book.epub")
        assertNotNull(retrieved)
        assertEquals("Test", retrieved?.title)
    }

    @Test
    fun updateReadingProgress() = runBlocking {
        val book = EbookEntity(title = "Prog", author = null, filePath = "/tmp/prog.epub", lastModified = 2L, format = "EPUB")
        dao.insertBook(book)
        val retrieved = dao.getBookByPath("/tmp/prog.epub")!!
        dao.updateReadingProgress(retrieved.id, "{\"loc\":1}", 0.5)
        val updated = dao.getBookById(retrieved.id)
        assertEquals("{\"loc\":1}", updated?.lastLocationJson)
        assertEquals(0.5, updated?.progression ?: 0.0, 0.0)
    }

    @Test
    fun deleteByPathAndIsImported() = runBlocking {
        val p1 = "/tmp/orphan.epub"
        val book = EbookEntity(title = "O", author = null, filePath = p1, lastModified = 3L, format = "EPUB")
        dao.insertBook(book)
        assertTrue(dao.isBookImported(p1))
        dao.deleteByPaths(listOf(p1))
        val after = dao.getBookByPath(p1)
        assertNull(after)
    }

    @Test
    fun getAllFilePaths() = runBlocking {
        dao.deleteBooks()
        val p1 = "/tmp/a.epub"
        val p2 = "/tmp/b.epub"
        dao.insertBook(EbookEntity(title = "A", author = null, filePath = p1, lastModified = 10L, format = "EPUB"))
        dao.insertBook(EbookEntity(title = "B", author = null, filePath = p2, lastModified = 20L, format = "EPUB"))
        val paths = dao.getAllFilePaths()
        assertTrue(paths.contains(p1))
        assertTrue(paths.contains(p2))
    }
}
