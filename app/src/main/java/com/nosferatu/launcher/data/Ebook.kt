package com.nosferatu.launcher.data

data class Ebook(
    val title: String,
    val author: String?,
    val filePath: String,
    val format: EbookFormat,
    val coverImage: CoverImage? = null
) {
    fun toEntity(lastModified: Long, coverPath: String?): EbookEntity {
        return EbookEntity(
            title = this.title,
            author = this.author,
            filePath = this.filePath,
            lastModified = lastModified,
            format = this.format.name,
            coverPath = coverPath,
            lastLocationJson = null,
            progression = 0.0
        )
    }
}

class CoverImage(val data: ByteArray)

enum class EbookFormat {
    EPUB, PDF, CBZ;

    companion object {
        fun fromExtension(extension: String): EbookFormat = when (extension.lowercase()) {
            "epub" -> EPUB
            "pdf" -> PDF
            "cbz" -> CBZ
            else -> throw IllegalArgumentException("Extension not supported: $extension")
        }
    }
}