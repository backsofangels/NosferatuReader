package com.nosferatu.launcher.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastModified DESC")
    fun getAllBooksFlow(): Flow<List<EbookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Long): EbookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: EbookEntity)

    @Query("UPDATE books SET lastLocationJson = :location, progression = :progression WHERE id = :id")
    suspend fun updateReadingProgress(id: Long, location: String, progression: Double)

    @Delete
    suspend fun deleteBook(book: EbookEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM books WHERE filePath = :path)")
    suspend fun isBookImported(path: String): Boolean

    @Query("SELECT * FROM books WHERE filePath = :absolutePath")
    suspend fun getBookByPath(absolutePath: String): EbookEntity?

    @Query("SELECT filePath FROM books")
    suspend fun getAllFilePaths(): List<String>

    @Query("DELETE FROM books WHERE filePath IN (:orphans)")
    fun deleteByPaths(orphans: List<String>)

    @Upsert
    fun upsertBook(book: EbookEntity)

    @Update
    fun updateBook(book: EbookEntity)

    @Query("UPDATE books SET coverPath = :coverPath WHERE id = :id")
    fun updateCoverPath(id: Long, coverPath: String?)

    @Query("DELETE FROM books")
    suspend fun deleteBooks()

}