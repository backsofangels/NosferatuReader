package com.nosferatu.launcher.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface BookDao {
    @Query("SELECT lastModified FROM books WHERE filePath = :path")
    suspend fun getLastModified(path: String): Long?

    @Upsert
    suspend fun saveOrUpdate(book: EbookEntity)

    @Query("SELECT filePath FROM books")
    suspend fun getAllFilePaths(): List<String>

    @Query("SELECT * FROM books")
    suspend fun getAllBooks(): List<EbookEntity>

    @Query("DELETE FROM books WHERE filePath IN (:paths)")
    suspend fun deleteByFilePaths(paths: List<String>)

    @Query("UPDATE books SET lastReadPosition = :position WHERE id = :bookId")
    suspend fun updateReadPosition(bookId: String, position: Int)

    @Query("UPDATE books SET lastChapterPosition = :chapter, lastReadPosition = :page WHERE filePath = :path")
    suspend fun updateChapterAndPage(path: String, chapter: Int, page: Int)
}