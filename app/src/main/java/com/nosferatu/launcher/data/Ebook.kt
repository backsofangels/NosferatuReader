package com.nosferatu.launcher.data

data class Ebook(
    val id: String,
    val title: String,
    val author: String?,
    val filePath: String,
    val coverPath: String?,
    val format: EbookFormat,
    val lastModified: Long,
    val lastReadPosition: Int = 0
) {
    fun toEntity(lastModified: Long): EbookEntity {
        return EbookEntity(
            id = this.id,
            title = this.title,
            author = this.author,
            filePath = this.filePath,
            coverPath = this.coverPath,
            lastModified = lastModified,
            format = this.format.name,
            lastReadPosition = this.lastReadPosition
        )
    }
}


enum class EbookFormat {
    EPUB, PDF;
    companion object {
        fun fromExtension(extension: String): EbookFormat? {
            return when (extension.lowercase()) {
                "epub" -> EPUB
                "pdf" -> PDF
                else -> null
            }
        }
    }
}
