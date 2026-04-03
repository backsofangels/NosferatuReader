package com.nosferatu.launcher.ui

import android.content.Context
import android.Manifest
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import org.junit.ClassRule
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import com.nosferatu.launcher.data.EbookEntity
import com.nosferatu.launcher.ui.MainActivity
import com.nosferatu.launcher.data.database.AppDatabase
import com.nosferatu.launcher.reader.ReaderActivity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    companion object {
        @JvmField
        @ClassRule
        val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private val ctx: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        val db = AppDatabase.getDatabase(ctx)
        runBlocking { db.bookDao().deleteBooks() }
    }

    @After
    fun tearDown() {
        val db = AppDatabase.getDatabase(ctx)
        runBlocking { db.bookDao().deleteBooks() }
    }

    @Test
    fun bottomBar_showsTabs() {
        // Ensure the activity is launched and Compose is set up
        composeTestRule.activityRule.scenario.onActivity { }
        composeTestRule.onNodeWithText("Miei Libri").assertExists()
    }

    @Test
    fun libraryShowsInsertedBook() {
        val db = AppDatabase.getDatabase(ctx)
        runBlocking {
            db.bookDao().insertBook(
                EbookEntity(
                    title = "Test Book",
                    author = "Author",
                    filePath = "/sdcard/test.epub",
                    lastModified = System.currentTimeMillis(),
                    format = "EPUB"
                )
            )
        }

        composeTestRule.activityRule.scenario.onActivity { }
        composeTestRule.onNodeWithText("Miei Libri").performClick()
        composeTestRule.onNodeWithText("Test Book").assertExists()
    }

    @Test
    fun clickingBook_launchesReaderIntent() {
        val db = AppDatabase.getDatabase(ctx)
        runBlocking {
            db.bookDao().insertBook(
                EbookEntity(
                    title = "Intent Book",
                    author = "Author",
                    filePath = "/sdcard/intent.epub",
                    lastModified = System.currentTimeMillis(),
                    format = "EPUB"
                )
            )
        }

        Intents.init()
        try {
            composeTestRule.activityRule.scenario.onActivity { }
            composeTestRule.onNodeWithText("Miei Libri").performClick()
            composeTestRule.onNodeWithText("Intent Book").performClick()
            Intents.intended(hasComponent(ReaderActivity::class.java.name))
        } finally {
            Intents.release()
        }
    }
}
