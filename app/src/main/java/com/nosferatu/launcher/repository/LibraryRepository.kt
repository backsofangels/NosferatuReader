package com.nosferatu.launcher.repository

import android.content.Context
import com.nosferatu.launcher.data.AppDatabase
import com.nosferatu.launcher.data.Ebook
import com.nosferatu.launcher.data.EbookEntity


class LibraryRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val bookDao = db.bookDao()

    suspend fun upsertBook(ebook: EbookEntity) {
        bookDao.saveOrUpdate(ebook)
    }

    suspend fun saveOrUpdate(ebook: Ebook) {
        val entity = ebook.toEntity(ebook.lastModified)
        bookDao.saveOrUpdate(entity)
    }

    suspend fun needsUpdate(filePath: String, lastModified: Long): Boolean {
        val lastModification = bookDao.getLastModified(filePath)
        return if (lastModification != null) {
            lastModification < lastModified
        } else true
    }

    suspend fun getAllFilePaths(): List<String> {
        return bookDao.getAllFilePaths()
    }

    suspend fun getAllBooks(): List<EbookEntity> {
        return bookDao.getAllBooks()
    }

    suspend fun deleteByFilePaths(filePaths: List<String>) {
        bookDao.deleteByFilePaths(filePaths)
    }
}
