package com.nosferatu.launcher.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Bitmap.CompressFormat
import org.robolectric.RuntimeEnvironment
import com.nosferatu.launcher.data.CoverImage
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
class CoverManagerTest {
    private lateinit var context: Context
    private lateinit var manager: CoverManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        manager = CoverManager(context)
    }

    @After
    fun tearDown() {
        val dir = context.filesDir.resolve("covers")
        dir.listFiles()?.forEach { it.delete() }
    }

    @Test
    fun saveCover_and_deleteCover() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val baos = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.JPEG, 85, baos)
        val bytes = baos.toByteArray()
        val coverImage = CoverImage(bytes)
        val path = manager.saveCover("testbook", coverImage)
        assertNotNull(path)
        val file = java.io.File(path!!)
        assertTrue(file.exists())
        manager.deleteCover(path)
        assertFalse(file.exists())
    }

    @Test
    fun saveCover_null_returnsNull() {
        val path = manager.saveCover("badbook", null)
        assertNull(path)
    }

    @Test
    fun saveCover_scalesDownIfNeeded() {
        val bitmap = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888)
        val baos = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.JPEG, 85, baos)
        val bytes = baos.toByteArray()
        val coverImage = CoverImage(bytes)
        val path = manager.saveCover("bigbook", coverImage)
        assertNotNull(path)
        val decoded = BitmapFactory.decodeFile(path!!)
        assertNotNull(decoded)
        assertTrue(decoded.width <= 600 && decoded.height <= 600)
    }
}
