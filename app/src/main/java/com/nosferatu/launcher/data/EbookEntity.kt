package com.nosferatu.launcher.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class EbookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String?,
    val filePath: String,
    val lastModified: Long,
    val format: String,
    val coverPath: String? = null,
    val lastLocationJson: String? = null,
    val progression: Double = 0.0,
)