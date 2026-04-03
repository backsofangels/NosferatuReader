package com.nosferatu.launcher.data

import org.junit.Assert.*
import org.junit.Test

class EbookFormatTest {
    @Test
    fun fromExtension_lowercase() {
        assertEquals(EbookFormat.EPUB, EbookFormat.fromExtension("epub"))
    }

    @Test
    fun fromExtension_uppercase() {
        assertEquals(EbookFormat.EPUB, EbookFormat.fromExtension("EPUB"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromExtension_unsupported() {
        EbookFormat.fromExtension("txt")
    }
}
