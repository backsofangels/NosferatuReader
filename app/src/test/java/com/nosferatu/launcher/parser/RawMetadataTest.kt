package com.nosferatu.launcher.parser

import org.junit.Assert.*
import org.junit.Test

class RawMetadataTest {
    @Test
    fun equals_sameCoverBytes() {
        val a = RawMetadata("T", "A", byteArrayOf(1, 2, 3))
        val b = RawMetadata("T", "A", byteArrayOf(1, 2, 3))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentCoverBytes() {
        val a = RawMetadata("T", "A", byteArrayOf(1))
        val b = RawMetadata("T", "A", byteArrayOf(2))
        assertNotEquals(a, b)
    }

    @Test
    fun equals_nullCover() {
        val a = RawMetadata("T", "A", null)
        val b = RawMetadata("T", "A", null)
        assertEquals(a, b)
    }
}
