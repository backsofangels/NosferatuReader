package com.nosferatu.launcher.data

import org.junit.Assert.*
import org.junit.Test

class EbookTest {
    @Test
    fun toEntity_mapsCorrectly() {
        val cover = CoverImage(byteArrayOf(1, 2, 3))
        val ebook = Ebook("Title", "Author", "/tmp/book.epub", EbookFormat.EPUB, cover)
        val entity = ebook.toEntity(lastModified = 12345L, coverPath = "/tmp/cover.jpg")

        assertEquals("Title", entity.title)
        assertEquals("Author", entity.author)
        assertEquals("/tmp/book.epub", entity.filePath)
        assertEquals("EPUB", entity.format)
        assertEquals("/tmp/cover.jpg", entity.coverPath)
        assertEquals(12345L, entity.lastModified)
    }
}
