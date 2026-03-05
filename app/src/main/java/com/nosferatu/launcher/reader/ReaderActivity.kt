package com.nosferatu.launcher.reader

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nosferatu.launcher.NosferatuApp
import com.nosferatu.launcher.R
import com.nosferatu.launcher.library.LibraryConfig
import com.nosferatu.launcher.library.LibraryViewModel
import com.nosferatu.launcher.library.LibraryViewModelFactory
import com.nosferatu.launcher.ui.components.fontsettings.ReaderTextSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.r2.navigator.epub.EpubDefaults
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.epub.EpubParser
import java.io.File

@OptIn(ExperimentalReadiumApi::class)
class ReaderActivity : AppCompatActivity(), EpubNavigatorFragment.Listener {
    private val _tag = "ReaderActivity"
    private var publication: Publication? = null
    private val viewModel: LibraryViewModel by viewModels {
        LibraryViewModelFactory((application as NosferatuApp).repository)
    }
    private var _lastKnownLocator: Locator? = null
    private var bookId: Long = -1

    private val assetRetriever by lazy { AssetRetriever(contentResolver, DefaultHttpClient()) }
    private val publicationOpener by lazy {
        PublicationOpener(
            publicationParser = EpubParser(),
            contentProtections = emptyList()
        )
    }

    private var isMenuVisible = false
    private var isFontBarVisible = false
    private val libraryConfig by lazy { LibraryConfig(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        // Setup UI Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.reader_container).setPadding(0, 48.dpToPx(), 0, 48.dpToPx())

        // UI Listeners
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupSettingsPanel()

        // Book Loading Logic
        val bookPath = intent.getStringExtra("BOOK_PATH") ?: return finish()
        val lastLocationJson = intent.getStringExtra("LAST_LOCATION_JSON")
        bookId = intent.getLongExtra("BOOK_ID", -1)

        lifecycleScope.launch {
            try {
                publication = openPublication(File(bookPath))

                val initialLocator = lastLocationJson?.let {
                    try { Locator.fromJSON(JSONObject(it)) } catch (e: Exception) { null }
                }

                // Inizializziamo il navigatore con le preferenze attuali da LibraryConfig
                val navigatorFactory = EpubNavigatorFactory(
                    publication = publication!!,
                    configuration = EpubNavigatorFactory.Configuration(
                        defaults = EpubDefaults(
                            pageMargins = 1.0,
                            lineHeight = libraryConfig.lineHeightFactor.toDouble()
                        )
                    )
                ).createFragmentFactory(
                    initialPreferences = EpubPreferences(
                        fontSize = libraryConfig.fontSizeScale.toDouble(),
                        lineHeight = libraryConfig.lineHeightFactor.toDouble()
                    ),
                    initialLocator = initialLocator,
                    listener = this@ReaderActivity
                )

                supportFragmentManager.fragmentFactory = navigatorFactory

                if (savedInstanceState == null) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.reader_container, EpubNavigatorFragment::class.java, null, "EpubNavigator")
                        .commitNow()
                }

                val navigator = supportFragmentManager.findFragmentByTag("EpubNavigator") as? EpubNavigatorFragment
                navigator?.addInputListener(readerInputListener)

                if (navigator != null) {
                    launch {
                        repeatOnLifecycle(Lifecycle.State.STARTED) {
                            navigator.currentLocator.collect { locator ->
                                _lastKnownLocator = locator
                                updateUiWithLocator(locator)
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(_tag, "Errore fatale apertura libro: ${e.message}")
                finish()
            }
        }
    }

    private fun setupSettingsPanel() {
        val settingsContainer = findViewById<ComposeView>(R.id.font_controls_container)
        val btnToggle = findViewById<View>(R.id.btn_font_settings)

        settingsContainer.setContent {
            // Applica il tema della tua app se necessario
            ReaderTextSettings(
                libraryConfig = libraryConfig,
                onPreferenceChanged = { applyReaderPreferences() }
            )
        }

        btnToggle.setOnClickListener {
            isFontBarVisible = !isFontBarVisible
            settingsContainer.visibility = if (isFontBarVisible) View.VISIBLE else View.GONE
            btnToggle.rotation = if (isFontBarVisible) 45f else 0f
        }
    }

    private fun applyReaderPreferences() {
        val navigator = supportFragmentManager.findFragmentByTag("EpubNavigator") as? EpubNavigatorFragment

        val newPreferences = EpubPreferences(
            fontSize = libraryConfig.fontSizeScale.toDouble(),
            lineHeight = libraryConfig.lineHeightFactor.toDouble()
        )

        Log.d(_tag, "Applicazione nuove preferenze: Font=${newPreferences.fontSize}, LH=${newPreferences.lineHeight}")
        navigator?.submitPreferences(newPreferences)
    }

    private fun updateUiWithLocator(locator: Locator) {
        val chapterTitle = locator.title ?: ""
        val progression = locator.locations.totalProgression ?: 0.0
        val percent = (progression * 100).toInt()
        val bookTitle = publication?.metadata?.title?.uppercase() ?: ""
        val bookAuthor = publication?.metadata?.authors?.firstOrNull()?.name

        findViewById<TextView>(R.id.immersive_header_title).text = chapterTitle.uppercase()
        findViewById<TextView>(R.id.immersive_footer_text).text = "$bookTitle - $percent%"
        findViewById<ProgressBar>(R.id.immersive_progress_bar).progress = percent

        findViewById<TextView>(R.id.menu_page_text).text = "$percent%"
        findViewById<TextView>(R.id.menu_book_title).text = bookTitle
        findViewById<TextView>(R.id.menu_book_author).text = bookAuthor
    }

    private fun toggleMenu() {
        isMenuVisible = !isMenuVisible
        val topBar = findViewById<View>(R.id.menu_top_bar)
        val bottomBar = findViewById<View>(R.id.menu_bottom_bar)
        val immersiveFooter = findViewById<View>(R.id.immersive_footer)
        val fontContainer = findViewById<View>(R.id.font_controls_container)
        val btnFontSettings = findViewById<View>(R.id.btn_font_settings)

        if (isMenuVisible) {
            topBar.visibility = View.VISIBLE
            bottomBar.visibility = View.VISIBLE
            immersiveFooter.visibility = View.INVISIBLE
        } else {
            topBar.visibility = View.GONE
            bottomBar.visibility = View.GONE
            immersiveFooter.visibility = View.VISIBLE
            isFontBarVisible = false
            fontContainer.visibility = View.GONE
            btnFontSettings.rotation = 0f
        }
    }

    private suspend fun openPublication(file: File): Publication = withContext(Dispatchers.IO) {
        val asset = assetRetriever.retrieve(file).getOrElse { throw Exception("Asset fallito") }
        publicationOpener.open(asset, allowUserInteraction = true).getOrElse { throw Exception("Opener fallito") }
    }

    private fun saveLocation(bookId: Long, locator: Locator?) {
        if (bookId == -1L) return
        val jsonLocation = locator?.toJSON().toString()
        viewModel.saveBookPosition(bookId, jsonLocation)
    }

    override fun onStop() {
        saveLocation(bookId, _lastKnownLocator)
        super.onStop()
    }

    override fun onDestroy() {
        publication?.close()
        super.onDestroy()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private val readerInputListener = object : InputListener {
        override fun onTap(event: TapEvent): Boolean {
            val point = event.point
            val screenWidth = resources.displayMetrics.widthPixels.toFloat()
            val screenHeight = resources.displayMetrics.heightPixels.toFloat()
            val isCenter = point.x in (screenWidth * 0.3f)..(screenWidth * 0.7f) &&
                    point.y in (screenHeight * 0.3f)..(screenHeight * 0.7f)

            if (isCenter) { toggleMenu(); return true }
            if (isMenuVisible) { toggleMenu(); return true }
            return false
        }
    }

    override fun onJumpToLocator(locator: Locator) { _lastKnownLocator = locator }
    @ExperimentalReadiumApi override fun onExternalLinkActivated(url: AbsoluteUrl) { /* do nothing */ }
}