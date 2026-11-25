package com.nosferatu.launcher.reader

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.artifex.mupdf.mini.DocumentActivity
import com.nosferatu.launcher.NosferatuApp
import com.nosferatu.launcher.R
import com.nosferatu.launcher.library.LibraryConfig
import com.nosferatu.launcher.library.LibraryViewModel
import com.nosferatu.launcher.library.LibraryViewModelFactory
import com.nosferatu.launcher.ui.LocalAppColors
import com.nosferatu.launcher.ui.appColorsFor
import com.nosferatu.launcher.ui.components.common.SingleChoiceOptionRow
import com.nosferatu.launcher.ui.components.fontsettings.ReaderTextSettings
import java.text.DateFormat
import java.util.Date

/**
 * Host-integration subclass of the MuPDF [DocumentActivity].
 *
 * [DocumentActivity] exposes `onReaderSettingsRequested()`, `onBookmarkToggleRequested()`
 * and `onBookmarksRequested()` as no-op hooks meant to be overridden by the host app.
 * Previously the app launched [DocumentActivity] directly, so the gear/bookmark/bookmarks
 * chrome buttons rendered correctly but did nothing when tapped. This subclass wires them
 * to real behavior:
 *  - page bookmarks, reusing the same [BookmarkStore]/[Bookmark] storage used by the
 *    Readium [ReaderActivity];
 *  - an in-reader text/background settings panel that mirrors [ReaderSettingsBottomSheet]'s
 *    content (font size slider + background choice), hosted in the `font_controls_container`
 *    `ComposeView` bridge added to `document_activity.xml`.
 *
 * [DocumentActivity] extends the plain `android.app.Activity` (not `ComponentActivity`), so it
 * does not provide the `ViewTreeLifecycleOwner`/`ViewTreeViewModelStoreOwner`/
 * `ViewTreeSavedStateRegistryOwner` that Compose's `ComposeView` requires. This class supplies a
 * minimal, self-contained implementation of those owners instead of changing `DocumentActivity`'s
 * base class (keeping the `:lib` module's MuPDF internals untouched).
 */
class NosferatuDocumentActivity :
    DocumentActivity(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val bookmarkStore by lazy { BookmarkStore(this) }
    private val overlayLibraryConfig by lazy { LibraryConfig(this) }
    private var fontControlsVisible = false

    private var statusBarClockTimer: java.util.Timer? = null
    private var statusBarBatteryReceiver: BroadcastReceiver? = null

    private var bookId: Long = -1
    private val viewModel: LibraryViewModel by lazy {
        LibraryViewModelFactory((application as NosferatuApp).repository).create(LibraryViewModel::class.java)
    }

    companion object {
        // Keeps the default library_prefs font size scale (0.85) mapped to MuPDF's default
        // reflow em-size (8pt), so existing users see an unchanged default layout.
        private const val BASE_LAYOUT_EM_PER_SCALE = 8f / 0.85f
        private const val MIN_LAYOUT_EM = 4f
        private const val TAG = "NosferatuDocumentActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        savedStateRegistryController.performRestore(savedInstanceState)
        super.onCreate(savedInstanceState)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        bookId = intent.getLongExtra("BOOK_ID", -1)
        Log.d(TAG, "onCreate: bookId=$bookId")

        window.decorView.let { root ->
            root.setViewTreeLifecycleOwner(this)
            root.setViewTreeViewModelStoreOwner(this)
            root.setViewTreeSavedStateRegistryOwner(this)
        }

        setupFontControlsOverlay()
        setupCustomStatusBar()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        savedStateRegistryController.performSave(outState)
    }

    override fun onStart() {
        super.onStart()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onResume() {
        super.onResume()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onPause() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        super.onPause()
    }

    override fun onStop() {
        savePagePosition()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onStop()
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        statusBarClockTimer?.cancel()
        statusBarBatteryReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering battery receiver: ${e.message}")
            }
        }
        super.onDestroy()
    }

    // --- Status bar (clock + battery), mirrors ReaderActivity.setupCustomStatusBar() ----

    private fun Int.statusBarDpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun setupCustomStatusBar() {
        try {
            val container = findViewById<FrameLayout>(R.id.reader_status_bar_container) ?: return
            val statusBarView = layoutInflater.inflate(R.layout.reader_status_bar, container, false)
            container.addView(statusBarView)

            val clockTextView = statusBarView.findViewById<TextView>(R.id.reader_status_clock)
            val batteryPercentageView = statusBarView.findViewById<TextView>(R.id.reader_battery_percentage)
            val batteryFillView = statusBarView.findViewById<View>(R.id.reader_battery_fill)

            // Clock update timer (every second)
            statusBarClockTimer = kotlin.concurrent.timer(initialDelay = 0, period = 1000) {
                try {
                    val now = System.currentTimeMillis()
                    val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(now))
                    this@NosferatuDocumentActivity.runOnUiThread {
                        try { clockTextView.text = timeStr } catch (_: Exception) {}
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Clock update failed: ${e.message}")
                }
            }

            fun applyBatteryUi(percentage: Int) {
                try {
                    batteryPercentageView.text = "$percentage%"
                    val totalPx = 18.statusBarDpToPx()
                    val fillWidth = (totalPx * percentage) / 100
                    val p = batteryFillView.layoutParams
                    p.width = fillWidth
                    batteryFillView.layoutParams = p
                } catch (_: Exception) {}
            }

            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

            // Populate initial battery state from sticky intent
            try {
                val sticky = registerReceiver(null, intentFilter)
                if (sticky != null) {
                    val level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val percentage = if (level >= 0 && scale > 0) (level * 100) / scale else 0
                    runOnUiThread { applyBatteryUi(percentage) }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Initial battery sticky intent failed: ${t.message}")
            }

            statusBarBatteryReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                    val percentage = if (level >= 0 && scale > 0) (level * 100) / scale else 0
                    runOnUiThread { applyBatteryUi(percentage) }
                }
            }

            try {
                registerReceiver(statusBarBatteryReceiver, intentFilter)
            } catch (t: Throwable) {
                Log.w(TAG, "registerReceiver failed: ${t.message}")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "setupCustomStatusBar failed: ${t.message}")
        }
    }

    // --- Position saving -----------------------------------------------------------------

    private fun savePagePosition() {
        if (bookId == -1L) return
        try {
            // Create a JSON-like string to represent the page position, similar to Readium's locator format
            val locationJson = """{"page":$currentPage,"totalPages":$pageCount}"""
            val progression = if (pageCount > 0) currentPage.toDouble() / pageCount.toDouble() else 0.0
            Log.d(TAG, "Saving position for bookId=$bookId: page=$currentPage/$pageCount, progression=$progression")
            viewModel.saveBookPosition(bookId, locationJson, progression)
        } catch (e: Exception) {
            Log.w(TAG, "Error saving page position: ${e.message}")
        }
    }

    // --- Page bookmarks -----------------------------------------------------------------

    private fun pageBookmarkKey(page: Int) = "page:$page"

    // Called by DocumentActivity on every page load; piggy-back to refresh the bookmark icon.
    override fun showPageNumber(pageNumber: Int) {
        super.showPageNumber(pageNumber)
        updateBookmarkIcon()
    }

    private fun updateBookmarkIcon() {
        val button = bookmarkToggleButton as? ImageView ?: return
        val bookmarked = bookmarkStore.isBookmarkedForBookKey(key, pageBookmarkKey(currentPage))
        button.setImageResource(if (bookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline)
    }

    override fun onBookmarkToggleRequested() {
        val pageKey = pageBookmarkKey(currentPage)
        if (bookmarkStore.isBookmarkedForBookKey(key, pageKey)) {
            bookmarkStore.removeBookmarkForBookKey(key, pageKey)
            Toast.makeText(this, getString(R.string.bookmark_removed), Toast.LENGTH_SHORT).show()
        } else {
            val progression = if (pageCount > 0) currentPage.toDouble() / pageCount.toDouble() else 0.0
            val label = getString(R.string.reader_bookmark_page_label, currentPage + 1)
            bookmarkStore.addBookmarkForBookKey(
                key,
                Bookmark(pageKey, "", progression, label, System.currentTimeMillis())
            )
            Toast.makeText(this, getString(R.string.bookmark_added), Toast.LENGTH_SHORT).show()
        }
        updateBookmarkIcon()
    }

    override fun onBookmarksRequested() {
        val bookmarks = bookmarkStore.listBookmarksForBookKey(key)
        if (bookmarks.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_bookmarks), Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        val labels = bookmarks.map { bookmark ->
            "${bookmark.chapterTitle} — ${dateFormat.format(Date(bookmark.createdAt))}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.bookmarks_title))
            .setItems(labels) { _, index -> showBookmarkActionDialog(bookmarks[index]) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showBookmarkActionDialog(bookmark: Bookmark) {
        AlertDialog.Builder(this)
            .setTitle(bookmark.chapterTitle)
            .setItems(arrayOf(getString(R.string.open_bookmark), getString(R.string.remove_bookmark))) { _, which ->
                if (which == 0) {
                    bookmark.locatorJson.removePrefix("page:").toIntOrNull()?.let { gotoPage(it) }
                } else {
                    bookmarkStore.removeBookmarkForBookKey(key, bookmark.locatorJson)
                    Toast.makeText(this, getString(R.string.bookmark_removed), Toast.LENGTH_SHORT).show()
                    updateBookmarkIcon()
                }
            }
            .show()
    }

    // --- In-reader settings overlay (text size + background) ----------------------------

    private fun setupFontControlsOverlay() {
        val container = findViewById<ComposeView>(R.id.font_controls_container) ?: return
        container.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        container.setContent {
            val appColors = appColorsFor(overlayLibraryConfig.backgroundMode)
            val isDark = overlayLibraryConfig.backgroundMode.toInt() == 2
            val colorScheme = if (isDark) {
                darkColorScheme().copy(
                    background = appColors.bg,
                    surface = appColors.surface,
                    onBackground = appColors.onBg,
                    onSurface = appColors.onBg
                )
            } else {
                lightColorScheme(
                    background = appColors.bg,
                    surface = appColors.surface,
                    onBackground = appColors.onBg,
                    onSurface = appColors.onBg
                )
            }

            MaterialTheme(colorScheme = colorScheme) {
                CompositionLocalProvider(LocalAppColors provides appColors) {
                    Column(modifier = Modifier.fillMaxWidth().background(appColors.bg)) {
                        ReaderTextSettings(
                            libraryConfig = overlayLibraryConfig,
                            onPreferenceChanged = { applyFontSizeToDocument() }
                        )

                        val currentBgMode = overlayLibraryConfig.backgroundMode.toInt()
                        SingleChoiceOptionRow(
                            label = stringResource(id = R.string.color_white),
                            selected = currentBgMode == 0,
                            onClick = { changeBackgroundMode(0f) }
                        )
                        SingleChoiceOptionRow(
                            label = stringResource(id = R.string.color_cream),
                            selected = currentBgMode == 1,
                            onClick = { changeBackgroundMode(1f) }
                        )
                        SingleChoiceOptionRow(
                            label = stringResource(id = R.string.color_black),
                            selected = currentBgMode == 2,
                            onClick = { changeBackgroundMode(2f) }
                        )
                    }
                }
            }
        }
    }

    private fun changeBackgroundMode(mode: Float) {
        if (overlayLibraryConfig.backgroundMode.toInt() == mode.toInt()) return
        overlayLibraryConfig.updateBackgroundMode(mode)
        // Background mode drives setTheme()/applyReaderTheme(), which must run before
        // setContentView(); recreate() is the simplest safe way to re-apply it everywhere.
        recreate()
    }

    private fun applyFontSizeToDocument() {
        // Font size only affects reflowable documents (EPUB/FB2/MOBI opened via MuPDF); a
        // fixed-layout PDF page is a rasterized bitmap and cannot reflow to a new text size.
        if (!isReflowable) return
        layoutEm = (overlayLibraryConfig.fontSizeScale * BASE_LAYOUT_EM_PER_SCALE).coerceAtLeast(MIN_LAYOUT_EM)
        relayoutDocument()
    }

    override fun onReaderSettingsRequested() {
        toggleFontControls()
    }

    private fun toggleFontControls() {
        val container = findViewById<ComposeView>(R.id.font_controls_container) ?: return
        fontControlsVisible = !fontControlsVisible
        container.visibility = if (fontControlsVisible) View.VISIBLE else View.GONE
    }

    // Hide the settings overlay whenever the reader chrome itself is hidden (page tap),
    // mirroring ReaderActivity.toggleMenu()'s behavior for font_controls_container.
    override fun onReaderUiVisibilityChanged(visible: Boolean) {
        if (!visible && fontControlsVisible) {
            fontControlsVisible = false
            findViewById<ComposeView>(R.id.font_controls_container)?.visibility = View.GONE
        }
    }
}
