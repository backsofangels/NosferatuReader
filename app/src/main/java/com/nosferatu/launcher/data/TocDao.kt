package com.nosferatu.launcher.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface TocDao {

    @Query("SELECT * FROM toc_entries WHERE bookId = :bookId ORDER BY position ASC")
    suspend fun getForBook(bookId: Long): List<TocEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<TocEntryEntity>)

    @Query("DELETE FROM toc_entries WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: Long)

    @Transaction
    suspend fun replaceForBook(bookId: Long, entries: List<TocEntryEntity>) {
        deleteForBook(bookId)
        if (entries.isNotEmpty()) {
            insertAll(entries)
        }
    }
}
