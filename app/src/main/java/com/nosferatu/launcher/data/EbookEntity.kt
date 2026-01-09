package com.nosferatu.launcher.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class EbookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String?,
    val filePath: String,
    val lastModified: Long,
    val format: String,
    val coverData: ByteArray? = null,
    val lastReadPosition: Int = 0
)