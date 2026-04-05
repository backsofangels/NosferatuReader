package com.nosferatu.launcher.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nosferatu.launcher.data.database.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TocDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: TocDao

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .build()
        dao = db.tocDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndGetForBook() = runBlocking {
        val entries = listOf(
            TocEntryEntity(bookId = 1L, title = "Chapter 1", href = "chap1.xhtml", locatorJson = "{\"href\":\"chap1.xhtml\"}", position = 0),
            TocEntryEntity(bookId = 1L, title = "Chapter 2", href = "chap2.xhtml", locatorJson = "{\"href\":\"chap2.xhtml\"}", position = 1),
        )
        dao.insertAll(entries)
        val got = dao.getForBook(1L)
        assertEquals(2, got.size)
        assertEquals("Chapter 1", got[0].title)
        assertEquals("chap2.xhtml", got[1].href)
    }

    @Test
    fun replaceForBookDeletesAndInserts() = runBlocking {
        val initial = listOf(
            TocEntryEntity(bookId = 2L, title = "Old", href = "old.xhtml", locatorJson = null, position = 0)
        )
        dao.insertAll(initial)
        assertEquals(1, dao.getForBook(2L).size)
        val newEntries = listOf(
            TocEntryEntity(bookId = 2L, title = "New A", href = "a.xhtml", locatorJson = null, position = 0),
            TocEntryEntity(bookId = 2L, title = "New B", href = "b.xhtml", locatorJson = null, position = 1),
        )
        dao.replaceForBook(2L, newEntries)
        val got = dao.getForBook(2L)
        assertEquals(2, got.size)
        assertEquals("New A", got[0].title)
    }

    @Test
    fun deleteForBookRemovesAll() = runBlocking {
        dao.insertAll(listOf(
            TocEntryEntity(bookId = 3L, title = "One", href = null, locatorJson = null, position = 0)
        ))
        assertEquals(1, dao.getForBook(3L).size)
        dao.deleteForBook(3L)
        assertTrue(dao.getForBook(3L).isEmpty())
    }
}
