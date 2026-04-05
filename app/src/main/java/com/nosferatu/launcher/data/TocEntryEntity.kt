package com.nosferatu.launcher.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "toc_entries",
    indices = [Index(value = ["bookId"])])
data class TocEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val title: String?,
    val href: String?,
    val locatorJson: String?,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)
