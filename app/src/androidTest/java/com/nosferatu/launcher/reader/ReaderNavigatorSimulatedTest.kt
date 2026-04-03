package com.nosferatu.launcher.reader

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.nosferatu.launcher.R
import org.readium.r2.shared.publication.Locator
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderNavigatorSimulatedTest {

    @Before
    fun setup() {
        TestReaderInjector.useFakeNavigator = true
        TestReaderInjector.fakeTitle = "Fake Book"
        TestReaderInjector.fakeAuthor = "Fake Author"
    }

    @After
    fun teardown() {
        TestReaderInjector.useFakeNavigator = false
        TestReaderInjector.fakeTitle = null
        TestReaderInjector.fakeAuthor = null
    }

    @Test
    fun fragmentLocator_updatesActivityUI() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(ctx, ReaderActivity::class.java).apply {
            putExtra("BOOK_PATH", "/sdcard/does-not-exist.epub")
            putExtra("BOOK_ID", 1L)
            putExtra("BOOK_TITLE", "Fake Book")
            putExtra("BOOK_AUTHOR", "Fake Author")
        }

        ActivityScenario.launch<ReaderActivity>(intent).use { scenario ->
            // Emit first locator
            scenario.onActivity { activity ->
                val frag = activity.supportFragmentManager.findFragmentByTag("EpubNavigator") as? TestNavigatorFragment
                frag?.setCurrentLocatorFromJson(
                    """
                    { "href":"chap1.xhtml", "type":"application/xhtml+xml", "title":"Capitolo Uno", "locations": { "totalProgression": 0.12 } }
                    """.trimIndent()
                )
            }

            // Verify UI updated (header and percent)
            onView(withId(R.id.immersive_header_title)).check(matches(withText("CAPITOLO UNO")))
            onView(withId(R.id.menu_page_text)).check(matches(withText("12%")))
            onView(withId(R.id.menu_book_title)).check(matches(withText("FAKE BOOK")))
            onView(withId(R.id.menu_book_author)).check(matches(withText("Fake Author")))

            // Emit second locator
            scenario.onActivity { activity ->
                val frag = activity.supportFragmentManager.findFragmentByTag("EpubNavigator") as? TestNavigatorFragment
                frag?.setCurrentLocatorFromJson(
                    """
                    { "href":"chap2.xhtml", "type":"application/xhtml+xml", "title":"Secondo Capitolo", "locations": { "totalProgression": 0.5 } }
                    """.trimIndent()
                )
            }

            onView(withId(R.id.immersive_header_title)).check(matches(withText("SECONDO CAPITOLO")))
            onView(withId(R.id.menu_page_text)).check(matches(withText("50%")))
        }
    }

    @Test
    fun submitPreferences_isRecordedOnFragment() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(ctx, ReaderActivity::class.java).apply {
            putExtra("BOOK_PATH", "/sdcard/does-not-exist.epub")
            putExtra("BOOK_ID", 2L)
        }

        ActivityScenario.launch<ReaderActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val frag = activity.supportFragmentManager.findFragmentByTag("EpubNavigator") as? TestNavigatorFragment
                // Create a preferences object and submit directly to fragment
                val prefs = org.readium.r2.navigator.epub.EpubPreferences(
                    fontSize = 1.4,
                    fontWeight = 2.0,
                    columnCount = org.readium.r2.navigator.preferences.ColumnCount.ONE,
                    publisherStyles = true,
                    theme = org.readium.r2.navigator.preferences.Theme.DARK
                )
                frag?.submitPreferences(prefs)
                // Assert stored
                val stored = frag?.lastSubmittedPreferences
                assert(stored != null)
                assert(stored?.fontSize == prefs.fontSize)
                assert(stored?.fontWeight == prefs.fontWeight)
                assert(stored?.theme == prefs.theme)
            }
        }
    }

    @Test
    fun goForwardAndBackward_updatesPositionsAndUI() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(ctx, ReaderActivity::class.java).apply {
            putExtra("BOOK_PATH", "/sdcard/does-not-exist.epub")
            putExtra("BOOK_ID", 3L)
        }

        ActivityScenario.launch<ReaderActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val frag = activity.supportFragmentManager.findFragmentByTag("EpubNavigator") as? TestNavigatorFragment
                val jsons = listOf(
                    """
                    { "href":"chap1.xhtml", "type":"application/xhtml+xml", "title":"Capitolo Uno", "locations": { "totalProgression": 0.1 } }
                    """.trimIndent(),
                    """
                    { "href":"chap2.xhtml", "type":"application/xhtml+xml", "title":"Capitolo Due", "locations": { "totalProgression": 0.5 } }
                    """.trimIndent(),
                    """
                    { "href":"chap3.xhtml", "type":"application/xhtml+xml", "title":"Capitolo Tre", "locations": { "totalProgression": 0.9 } }
                    """.trimIndent()
                )
                val locs = jsons.mapNotNull { Locator.fromJSON(JSONObject(it)) }
                frag?.setPositions(locs)
            }

            // initial
            onView(withId(R.id.immersive_header_title)).check(matches(withText("CAPITOLO UNO")))
            onView(withId(R.id.menu_page_text)).check(matches(withText("10%")))

            // forward -> Capitolo Due
            scenario.onActivity { activity ->
                val frag = activity.supportFragmentManager.findFragmentByTag("EpubNavigator") as? TestNavigatorFragment
                frag?.goForward(false)
            }
            onView(withId(R.id.immersive_header_title)).check(matches(withText("CAPITOLO DUE")))
            onView(withId(R.id.menu_page_text)).check(matches(withText("50%")))

            // forward -> Capitolo Tre
            scenario.onActivity { activity ->
                val frag = activity.supportFragmentManager.findFragmentByTag("EpubNavigator") as? TestNavigatorFragment
                frag?.goForward(false)
            }
            onView(withId(R.id.immersive_header_title)).check(matches(withText("CAPITOLO TRE")))
            onView(withId(R.id.menu_page_text)).check(matches(withText("90%")))

            // backward -> Capitolo Due
            scenario.onActivity { activity ->
                val frag = activity.supportFragmentManager.findFragmentByTag("EpubNavigator") as? TestNavigatorFragment
                frag?.goBackward(false)
            }
            onView(withId(R.id.immersive_header_title)).check(matches(withText("CAPITOLO DUE")))
            onView(withId(R.id.menu_page_text)).check(matches(withText("50%")))
        }
    }
}
