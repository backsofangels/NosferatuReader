package com.nosferatu.launcher.reader

data class Bookmark(
    val locatorJson: String,
    val href: String,
    val progression: Double,
    val chapterTitle: String?,
    val createdAt: Long
)
