package com.nosferatu.launcher.parser

import java.io.File

interface ParserStrategy {
    suspend fun parse(file: File): RawMetadata
}

data class RawMetadata(
    val title: String?,
    val author: String?,
    val coverData: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RawMetadata

        if (title != other.title) return false
        if (author != other.author) return false
        if (!coverData.contentEquals(other.coverData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title?.hashCode() ?: 0
        result = 31 * result + (author?.hashCode() ?: 0)
        result = 31 * result + (coverData?.contentHashCode() ?: 0)
        return result
    }
}