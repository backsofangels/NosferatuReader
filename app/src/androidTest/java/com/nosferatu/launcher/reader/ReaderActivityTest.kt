package com.nosferatu.launcher.reader

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderActivityTest {

    @Test
    fun readerActivity_testMode_showsProvidedMetadata() {
        // Enable test mode to avoid Readium initialization
        TestReaderInjector.useFakeNavigator = true
        TestReaderInjector.fakeTitle = "Sample Test Book"
        TestReaderInjector.fakeAuthor = "Test Author"

        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, ReaderActivity::class.java).apply {
            putExtra("BOOK_PATH", "/sdcard/does-not-exist.epub")
            putExtra("BOOK_ID", 42L)
            putExtra("BOOK_TITLE", "Sample Test Book")
            putExtra("BOOK_AUTHOR", "Test Author")
        }

        ActivityScenario.launch<ReaderActivity>(intent).use { scenario ->
            // Verify the activity was created and the UI shows the provided metadata
            scenario.onActivity { activity ->
                val titleView = activity.findViewById<android.widget.TextView>(com.nosferatu.launcher.R.id.menu_book_title)
                val authorView = activity.findViewById<android.widget.TextView>(com.nosferatu.launcher.R.id.menu_book_author)
                assert(titleView.text.toString().contains("SAMPLE TEST BOOK"))
                assert(authorView.text.toString().contains("Test Author"))
            }
        }

        // Reset injector
        TestReaderInjector.useFakeNavigator = false
        TestReaderInjector.fakeTitle = null
        TestReaderInjector.fakeAuthor = null
    }
}
