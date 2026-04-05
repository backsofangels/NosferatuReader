package com.nosferatu.launcher.reader

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
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
import com.nosferatu.launcher.ui.LocalAppColors
import com.nosferatu.launcher.ui.appColorsFor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.nosferatu.launcher.ui.components.fontsettings.ReaderTextSettings
import androidx.core.content.res.ResourcesCompat
import android.graphics.Typeface
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.r2.navigator.epub.EpubDefaults
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.ColumnCount
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.locateProgression
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.epub.EpubParser
import java.io.File
import com.nosferatu.launcher.reader.TestReaderInjector
import com.nosferatu.launcher.reader.TestNavigatorFragment
import android.app.AlertDialog
import android.widget.Toast
import java.text.DateFormat
import java.net.URLDecoder
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.ViewGroup
import android.graphics.Color as AndroidColor

@OptIn(ExperimentalReadiumApi::class)
class ReaderActivity : AppCompatActivity(), EpubNavigatorFragment.Listener {
    private val _tag = "ReaderActivity"
    private var publication: Publication? = null
    private val viewModel: LibraryViewModel by viewModels {
        LibraryViewModelFactory((application as NosferatuApp).repository)
    }
    private var _lastKnownLocator: Locator? = null
    private var bookId: Long = -1
    private var bookKey: String = ""
    private val bookmarkStore by lazy { BookmarkStore(this) }
    // TOC bottom-sheet state
    private var tocDialog: BottomSheetDialog? = null
    private var tocAdapter: TocAdapter? = null
    private var tocRecycler: RecyclerView? = null
    private var tocFlatLabels: Array<String>? = null
    private val tocFlatLocators: MutableList<Locator?> = mutableListOf()
    private var tocSelectedIndex: Int = -1

    private val assetRetriever by lazy { AssetRetriever(contentResolver, DefaultHttpClient()) }
    private val publicationOpener by lazy {
        PublicationOpener(
            publicationParser = EpubParser(),
            contentProtections = emptyList()
        )
    }

    private var isMenuVisible = false
    private var isFontBarVisible = false
    private var isSeeking = false
    private val libraryConfig by lazy { LibraryConfig(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply theme before inflation so all XML attr references resolve correctly
        val bgMode = getSharedPreferences("library_prefs", MODE_PRIVATE).getFloat("background_mode", 0f)
        when (bgMode.toInt()) {
            1 -> setTheme(R.style.Theme_NosferatuReader_Cream)
            2 -> setTheme(R.style.Theme_NosferatuReader_Dark)
            else -> setTheme(R.style.Theme_NosferatuReader)
        }
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

        // bookmark toggle (top-right)
        findViewById<ImageView>(R.id.btn_bookmark_toggle).setOnClickListener {
            toggleBookmark()
        }

        // tooltips for accessibility and desktop hover
        ViewCompat.setTooltipText(findViewById(R.id.btn_back), getString(R.string.cd_back))
        ViewCompat.setTooltipText(findViewById(R.id.btn_bookmark_toggle), getString(R.string.cd_bookmark_toggle))

        setupSettingsPanel()
        // Apply initial typeface according to stored preference
        applyTypefaceToReaderUI()

        // bookmarks panel trigger (bottom bar)
        findViewById<View>(R.id.btn_bookmarks).setOnClickListener {
            openBookmarksPanel()
        }
        // TOC panel trigger (bottom-left)
        findViewById<View>(R.id.btn_toc).setOnClickListener {
            openTocPanel()
        }
        ViewCompat.setTooltipText(findViewById(R.id.btn_toc), getString(R.string.cd_open_toc))
        ViewCompat.setTooltipText(findViewById(R.id.btn_bookmarks), getString(R.string.bookmarks_title))

        // Book Loading Logic
        val bookPath = intent.getStringExtra("BOOK_PATH") ?: return finish()
        val lastLocationJson = intent.getStringExtra("LAST_LOCATION_JSON")
        bookId = intent.getLongExtra("BOOK_ID", -1)
        // Derive a stable string identifier for this book (use filename). This scopes bookmarks per-file.
        bookKey = try { File(bookPath).name } catch (_: Exception) { bookPath }

        // If tests enabled fake navigator, avoid initializing Readium and populate minimal UI
        if (TestReaderInjector.useFakeNavigator) {
            // Insert a minimal test fragment so the layout has content
            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.reader_container, TestNavigatorFragment(), "EpubNavigator")
                    .commitNow()
            }

            // Populate header/footer with supplied test metadata
            val testTitle = intent.getStringExtra("BOOK_TITLE") ?: TestReaderInjector.fakeTitle ?: ""
            val testAuthor = intent.getStringExtra("BOOK_AUTHOR") ?: TestReaderInjector.fakeAuthor ?: ""
            findViewById<TextView>(R.id.menu_book_title).text = testTitle.uppercase()
            findViewById<TextView>(R.id.menu_book_author).text = testAuthor

            // Observe TestNavigatorFragment locator updates and update UI accordingly
            val testNavigator = supportFragmentManager.findFragmentByTag("EpubNavigator") as? TestNavigatorFragment
            if (testNavigator != null) {
                lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        testNavigator.currentLocator.collect { locator ->
                            if (locator != null) {
                                _lastKnownLocator = locator
                                updateUiWithLocator(locator)
                            }
                        }
                    }
                }
            }

            // Skip Readium initialization in test mode
            return
        }

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
                        defaults = EpubDefaults()
                    )
                ).createFragmentFactory(
                    initialPreferences = EpubPreferences(
                        fontSize = libraryConfig.fontSizeScale.toDouble(),
                        fontWeight = if (libraryConfig.forceBold) 2.0 else null,
                        columnCount = ColumnCount.ONE,
                        publisherStyles = true,
                        theme = when (libraryConfig.backgroundMode.toInt()) {
                            1 -> Theme.SEPIA
                            2 -> Theme.DARK
                            else -> Theme.LIGHT
                        }
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
                    setupProgressBarSeek(navigator)
                    launch {
                        repeatOnLifecycle(Lifecycle.State.STARTED) {
                            navigator.currentLocator.collect { locator ->
                                _lastKnownLocator = locator
                                updateUiWithLocator(locator)
                                updateBookmarkIcon()
                                updateTocSelection()
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

        settingsContainer.setContent {
            val appColors = appColorsFor(libraryConfig.backgroundMode)
            val isDark = libraryConfig.backgroundMode.toInt() == 2
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
            // Build typography for overlay using the same runtime font detection
            val baseTypography = Typography()
            val ctx = this@ReaderActivity
            val resId = ctx.resources.getIdentifier("literata_regular", "font", ctx.packageName)
            val chosenFamily = if (resId != 0) FontFamily(Font(resId)) else FontFamily.Default
            val typography = if (libraryConfig.fontChoice.toInt() == 1) baseTypography.copy(
                displayLarge = baseTypography.displayLarge.copy(fontFamily = chosenFamily),
                displayMedium = baseTypography.displayMedium.copy(fontFamily = chosenFamily),
                displaySmall = baseTypography.displaySmall.copy(fontFamily = chosenFamily),
                headlineLarge = baseTypography.headlineLarge.copy(fontFamily = chosenFamily),
                headlineMedium = baseTypography.headlineMedium.copy(fontFamily = chosenFamily),
                headlineSmall = baseTypography.headlineSmall.copy(fontFamily = chosenFamily),
                titleLarge = baseTypography.titleLarge.copy(fontFamily = chosenFamily),
                titleMedium = baseTypography.titleMedium.copy(fontFamily = chosenFamily),
                titleSmall = baseTypography.titleSmall.copy(fontFamily = chosenFamily),
                bodyLarge = baseTypography.bodyLarge.copy(fontFamily = chosenFamily),
                bodyMedium = baseTypography.bodyMedium.copy(fontFamily = chosenFamily),
                bodySmall = baseTypography.bodySmall.copy(fontFamily = chosenFamily),
                labelLarge = baseTypography.labelLarge.copy(fontFamily = chosenFamily),
                labelMedium = baseTypography.labelMedium.copy(fontFamily = chosenFamily),
                labelSmall = baseTypography.labelSmall.copy(fontFamily = chosenFamily)
            ) else baseTypography

            MaterialTheme(colorScheme = colorScheme, typography = typography) {
                CompositionLocalProvider(LocalAppColors provides appColors) {
                    ReaderTextSettings(
                        libraryConfig = libraryConfig,
                        onPreferenceChanged = { applyReaderPreferences() }
                    )
                }
            }
        }

        // The in-reader font controls remain in the layout but are not toggled from a reader button.
        // Font selection is persisted in `LibraryConfig` and applied via `applyReaderPreferences()`/`applyTypefaceToReaderUI()`.
    }

    private fun applyReaderPreferences() {
        val fragment = supportFragmentManager.findFragmentByTag("EpubNavigator")
        val navigator = fragment as? EpubNavigatorFragment
        val testNavigator = fragment as? TestNavigatorFragment

        val newPreferences = EpubPreferences(
            fontSize = libraryConfig.fontSizeScale.toDouble(),
            fontWeight = if (libraryConfig.forceBold) 2.0 else null,
            columnCount = ColumnCount.ONE,
            publisherStyles = true,
            theme = when (libraryConfig.backgroundMode.toInt()) {
                1 -> Theme.SEPIA
                2 -> Theme.DARK
                else -> Theme.LIGHT
            }
        )

        Log.d(_tag, "Applicazione nuove preferenze: Font=${newPreferences.fontSize}, Bold=${libraryConfig.forceBold}, PublisherStyles=${newPreferences.publisherStyles}")
        navigator?.submitPreferences(newPreferences)
        testNavigator?.submitPreferences(newPreferences)
        // Apply the selected font to the reader UI elements (header/footer/menu)
        applyTypefaceToReaderUI()
    }

    private fun isCurrentLocatorBookmarked(): Boolean {
        val locator = _lastKnownLocator ?: return false
        val locatorJson = locator.toJSON().toString()
        return bookmarkStore.isBookmarkedForBookKey(bookKey, locatorJson)
    }

    private fun updateBookmarkIcon() {
        try {
            val btn = findViewById<ImageView>(R.id.btn_bookmark_toggle)
            val isBookmarked = isCurrentLocatorBookmarked()
            btn.setImageResource(if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline)
        } catch (t: Throwable) {
            // ignore
        }
    }

    private fun updateTocSelection() {
        try {
            val adapter = tocAdapter ?: return
            val recycler = tocRecycler ?: return
            val current = _lastKnownLocator ?: return
            val currentHref = current.href?.toString() ?: return

            // try to find by href prefix first
            var sel = tocFlatLocators.indexOfFirst { it?.href?.toString()?.startsWith(currentHref) == true }

            if (sel < 0) {
                val currProg = current.locations.totalProgression ?: 0.0
                var bestIdx = -1
                var bestDiff = Double.MAX_VALUE
                for (i in tocFlatLocators.indices) {
                    val loc = tocFlatLocators[i] ?: continue
                    val prog = loc.locations.totalProgression ?: continue
                    val diff = kotlin.math.abs(currProg - prog)
                    if (diff < bestDiff) { bestDiff = diff; bestIdx = i }
                }
                sel = bestIdx
            }

            if (sel >= 0 && sel < adapter.itemCount) {
                adapter.select(sel)
                recycler.scrollToPosition(sel)
                tocSelectedIndex = sel
            }
        } catch (t: Throwable) {
            // ignore
        }
    }

    private fun toggleBookmark() {
        val locator = _lastKnownLocator
        if (locator == null) {
            Toast.makeText(this, getString(R.string.no_bookmarks), Toast.LENGTH_SHORT).show()
            return
        }
        val locatorJson = locator.toJSON().toString()
        val href = locator.href.toString()
        val prog = locator.locations.totalProgression ?: 0.0
        val title = locator.title
        val now = System.currentTimeMillis()
        if (bookmarkStore.isBookmarkedForBookKey(bookKey, locatorJson)) {
            bookmarkStore.removeBookmarkForBookKey(bookKey, locatorJson)
            Toast.makeText(this, getString(R.string.bookmark_removed), Toast.LENGTH_SHORT).show()
        } else {
            val bm = Bookmark(locatorJson, href, prog, title, now)
            bookmarkStore.addBookmarkForBookKey(bookKey, bm)
            Toast.makeText(this, getString(R.string.bookmark_added), Toast.LENGTH_SHORT).show()
        }
        updateBookmarkIcon()
    }

    private fun openBookmarksPanel() {
        val list = bookmarkStore.listBookmarksForBookKey(bookKey)
        if (list.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_bookmarks), Toast.LENGTH_SHORT).show()
            return
        }
        val labels = list.mapIndexed { idx, bm ->
            "Segnalibro ${idx + 1}"
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.bookmarks_title))
            .setItems(labels) { dialog, which ->
                val chosen = list[which]
                // Show action dialog: Apri or Rimuovi
                val detailTitle = (chosen.chapterTitle ?: chosen.href) ?: labels[which]
                AlertDialog.Builder(this)
                    .setTitle("${labels[which]} — $detailTitle")
                    .setItems(arrayOf(getString(R.string.open_bookmark), getString(R.string.remove_bookmark))) { d, index ->
                        when (index) {
                            0 -> {
                                // Open
                                try {
                                    val locatorObj = try { Locator.fromJSON(JSONObject(chosen.locatorJson)) } catch (e: Exception) { null }
                                    val navigator = supportFragmentManager.findFragmentByTag("EpubNavigator") as? EpubNavigatorFragment
                                    if (navigator != null && locatorObj != null) {
                                        lifecycleScope.launch { navigator.go(locatorObj, animated = false) }
                                    }
                                } catch (t: Throwable) { /* ignore */ }
                            }
                            1 -> {
                                // Remove
                                bookmarkStore.removeBookmarkForBookKey(bookKey, chosen.locatorJson)
                                Toast.makeText(this, getString(R.string.bookmark_removed), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun applyTypefaceToReaderUI() {
        try {
            val resId = resources.getIdentifier("literata_regular", "font", packageName)
            val tf: Typeface? = if (libraryConfig.fontChoice.toInt() == 1 && resId != 0) {
                ResourcesCompat.getFont(this, resId)
            } else Typeface.DEFAULT

            val header = findViewById<TextView>(R.id.immersive_header_title)
            val footer = findViewById<TextView>(R.id.immersive_footer_text)
            val page = findViewById<TextView>(R.id.menu_page_text)
            val title = findViewById<TextView>(R.id.menu_book_title)
            val author = findViewById<TextView>(R.id.menu_book_author)

            header.typeface = tf
            footer.typeface = tf
            page.typeface = tf
            title.typeface = tf
            author.typeface = tf
        } catch (t: Throwable) {
            Log.w(_tag, "Impossibile applicare il font personalizzato: ${t.message}")
        }
    }

    // --- TOC extraction & panel -------------------------------------------------
    private data class TocNode(val title: String?, val href: String?, val children: List<TocNode> = emptyList())

    private fun extractToc(pub: Publication?): List<TocNode> {
        if (pub == null) return emptyList()
        try {
            val cls = pub.javaClass
            val candidateNames = arrayOf("getTableOfContents", "getToc", "getTableOfContent", "tableOfContents")
            var tocObj: Any? = null
            for (name in candidateNames) {
                try {
                    val m = cls.getMethod(name)
                    tocObj = m.invoke(pub)
                    if (tocObj != null) break
                } catch (nm: NoSuchMethodException) {
                    // try next
                }
            }
            if (tocObj == null) return emptyList()
            if (tocObj !is List<*>) return emptyList()

            fun mapLink(linkObj: Any?): TocNode? {
                if (linkObj == null) return null
                try {
                    val lcls = linkObj.javaClass
                    val getTitle = try { lcls.getMethod("getTitle") } catch (_: Exception) { null }
                    val getHref = try { lcls.getMethod("getHref") } catch (_: Exception) { null }
                    val getChildren = try { lcls.getMethod("getChildren") } catch (_: Exception) { null }
                    val title = getTitle?.invoke(linkObj) as? String
                    val hrefObj = getHref?.invoke(linkObj)
                    val hrefStr = hrefObj?.toString()
                    val children = mutableListOf<TocNode>()
                    if (getChildren != null) {
                        val ch = getChildren.invoke(linkObj)
                        if (ch is List<*>) {
                            for (c in ch) {
                                val mapped = mapLink(c)
                                if (mapped != null) children.add(mapped)
                            }
                        }
                    }
                    return TocNode(title, hrefStr, children)
                } catch (t: Throwable) {
                    return null
                }
            }

            val out = mutableListOf<TocNode>()
            for (item in tocObj as List<*>) {
                val node = mapLink(item)
                if (node != null) out.add(node)
            }
            return out
        } catch (t: Throwable) {
            Log.w(_tag, "TOC extract failed: ${t.message}")
            return emptyList()
        }
    }

    private fun flattenToc(nodes: List<TocNode>, depth: Int = 0, out: MutableList<Pair<TocNode, Int>>) {
        for (n in nodes) {
            out.add(n to depth)
            if (n.children.isNotEmpty()) flattenToc(n.children, depth + 1, out)
        }
    }

    // Normalize hrefs for matching: strip fragment, leading ./ or /, and trim
    private fun normalizeHrefForMatch(href: String?): String? {
        if (href == null) return null
        var h = href
        // remove fragment
        val hashIdx = h.indexOf('#')
        if (hashIdx >= 0) h = h.substring(0, hashIdx)
        // remove query
        val qIdx = h.indexOf('?')
        if (qIdx >= 0) h = h.substring(0, qIdx)
        h = h.trim()
        h = h.removePrefix("./").removePrefix("/")
        // decode percent-encoding if present
        try {
            h = URLDecoder.decode(h, "UTF-8")
        } catch (_: Exception) { }
        // strip common extensions used in EPUBs
        h = h.replace(Regex("\\.(x?html?|xml|ncx|opf|htm|xht)", RegexOption.IGNORE_CASE), "")
        return if (h.isEmpty()) null else h
    }

    // Heuristic matcher: try exact, suffix, contains, last-segment match
    private fun findBestPositionForHref(href: String?, positions: List<Locator>): Locator? {
        if (href == null) return null
        val target = normalizeHrefForMatch(href) ?: return null

        val normalized = positions.map { pos ->
            val ph = pos.href?.toString()
            Pair(pos, normalizeHrefForMatch(ph))
        }

        // exact
        normalized.firstOrNull { it.second == target }?.let { return it.first }

        // endsWith (covers paths like "OPS/ch1.xhtml" vs "ch1.xhtml")
        normalized.firstOrNull { it.second != null && it.second!!.endsWith(target) }?.let { return it.first }

        // contains
        normalized.firstOrNull { it.second != null && it.second!!.contains(target) }?.let { return it.first }

        // last segment match
        val targetLast = target.substringAfterLast('/')
        normalized.firstOrNull { it.second != null && it.second!!.substringAfterLast('/') == targetLast }?.let { return it.first }

        return null
    }

    private fun getRuntimeTypeface(): Typeface? {
        val resId = resources.getIdentifier("literata_regular", "font", packageName)
        return if (libraryConfig.fontChoice.toInt() == 1 && resId != 0) {
            try { ResourcesCompat.getFont(this, resId) } catch (_: Exception) { null }
        } else null
    }

    // RecyclerView adapter for the TOC bottom sheet (supports headers and items)
    private inner class TocAdapter(
        private val items: List<Pair<TocNode, Int>>,
        private val onClick: (Int) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var selectedIndex: Int = -1

        private val VIEW_TYPE_HEADER = 0
        private val VIEW_TYPE_ITEM = 1

        // Precompute theme colors and runtime typeface
        private val appColors = appColorsFor(libraryConfig.backgroundMode)
        private val selectedBgColor = appColors.selectedRowBackground.toArgb()
        private val textColorInt = appColors.onBg.toArgb()
        private val iconColorInt = appColors.onBg.toArgb()
        private val runtimeTf = getRuntimeTypeface()

        inner class HeaderVH(val view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.toc_item_title)
            init {
                view.setOnClickListener { val pos = bindingAdapterPosition; if (pos != RecyclerView.NO_POSITION) onClick(pos) }
            }
        }

        inner class ItemVH(val view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.toc_item_icon)
            val title: TextView = view.findViewById(R.id.toc_item_title)
            init {
                view.setOnClickListener { val pos = bindingAdapterPosition; if (pos != RecyclerView.NO_POSITION) onClick(pos) }
            }
        }

        override fun getItemViewType(position: Int): Int {
            val (node, depth) = items[position]
            return if (depth == 0 && node.children.isNotEmpty()) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == VIEW_TYPE_HEADER) {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_toc_header, parent, false)
                HeaderVH(v)
            } else {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_toc, parent, false)
                ItemVH(v)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val (node, depth) = items[position]
            val titleStr = node.title ?: node.href ?: "-"

            val base = resources.getDimensionPixelSize(R.dimen.spacing_16)
            val padStart = base + depth * (base / 2)

            if (holder is HeaderVH) {
                holder.title.text = titleStr
                holder.title.setPadding(padStart, holder.title.paddingTop, holder.title.paddingRight, holder.title.paddingBottom)
                holder.title.contentDescription = titleStr
                holder.title.setTextColor(textColorInt)
                runtimeTf?.let { holder.title.typeface = it }
                // header styling
                if (position == selectedIndex) {
                    holder.view.setBackgroundColor(selectedBgColor)
                } else {
                    holder.view.setBackgroundResource(0)
                }
            } else if (holder is ItemVH) {
                holder.title.text = titleStr
                holder.title.setPadding(padStart, holder.title.paddingTop, holder.title.paddingRight, holder.title.paddingBottom)
                holder.title.contentDescription = titleStr
                holder.title.setTextColor(textColorInt)
                runtimeTf?.let { holder.title.typeface = it }

                // icon: parents show chevron, leaves show toc/book icon
                if (node.children.isNotEmpty()) {
                    holder.icon.setImageResource(R.drawable.ic_expand_more)
                } else {
                    holder.icon.setImageResource(R.drawable.ic_toc)
                }
                holder.icon.setColorFilter(iconColorInt)

                if (position == selectedIndex) {
                    holder.view.setBackgroundColor(selectedBgColor)
                    holder.title.setTypeface(null, Typeface.BOLD)
                } else {
                    holder.view.setBackgroundResource(0)
                    holder.title.setTypeface(null, Typeface.NORMAL)
                }
            }
        }

        override fun getItemCount(): Int = items.size

        fun select(idx: Int) {
            val prev = selectedIndex
            selectedIndex = idx
            if (prev >= 0) notifyItemChanged(prev)
            if (idx >= 0) notifyItemChanged(idx)
        }
    }

    private fun openTocPanel() {
        // Try loading TOC from DB first, fall back to runtime extraction
        lifecycleScope.launch {
            val repo = (application as NosferatuApp).repository

            val dbEntries = try {
                withContext(Dispatchers.IO) { repo.getTocEntriesForBook(bookId) }
            } catch (t: Throwable) {
                Log.w(_tag, "Failed to load TOC from DB: ${t.message}")
                emptyList()
            }

            Log.d(_tag, "openTocPanel: loaded ${dbEntries.size} entries from DB for bookId=$bookId")

            val tocNodes: List<TocNode> = if (dbEntries.isNotEmpty()) {
                dbEntries.sortedBy { it.position }.map { TocNode(it.title, it.href, emptyList()) }
            } else {
                Log.d(_tag, "openTocPanel: DB empty, extracting runtime TOC from publication")
                val runtime = extractToc(publication)
                Log.d(_tag, "openTocPanel: runtime extract returned ${runtime.size} TOC nodes")
                runtime
            }

            if (tocNodes.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ReaderActivity, "Indice non disponibile", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val flat = mutableListOf<Pair<TocNode, Int>>()
            flattenToc(tocNodes, 0, flat)
            val labels = flat.map { (node, depth) ->
                val title = node.title ?: node.href ?: "-"
                "${"\u00A0".repeat(depth * 2)}$title"
            }.toTypedArray()

            // store labels so other code can reference them
            tocFlatLabels = labels
            tocFlatLocators.clear()
            for (i in labels.indices) tocFlatLocators.add(null)

            val pub = publication
            if (pub == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ReaderActivity, "Impossibile navigare al capitolo selezionato", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            // Compute mapping between TOC hrefs and publication positions in background
            val hrefs = flat.map { it.first.href }
            val positions = try { pub.positions() } catch (_: Throwable) { emptyList<Locator>() }

            val matched = hrefs.map { href ->
                findBestPositionForHref(href, positions)
            }

            val matchedCount = matched.count { it != null }
            val unmatchedCount = matched.count { it == null }
            Log.d(_tag, "openTocPanel: matched positions=$matchedCount unmatched=$unmatchedCount for ${hrefs.size} hrefs")

            tocFlatLocators.clear()
            tocFlatLocators.addAll(matched)

            // determine selected index based on current locator
            val current = _lastKnownLocator
            var selected = -1
            if (current != null) {
                val currentHref = current.href?.toString()
                val currentProg = current.locations.totalProgression ?: 0.0
                if (currentHref != null) {
                    selected = matched.indexOfFirst { it?.href?.toString()?.startsWith(currentHref) == true }
                }
                if (selected < 0) {
                    // fallback to nearest progression
                    var bestIdx = -1
                    var bestDiff = Double.MAX_VALUE
                    for (i in matched.indices) {
                        val m = matched[i] ?: continue
                        val prog = m.locations.totalProgression ?: continue
                        val diff = kotlin.math.abs(currentProg - prog)
                        if (diff < bestDiff) { bestDiff = diff; bestIdx = i }
                    }
                    selected = bestIdx
                }
            }

            tocSelectedIndex = selected

            // Show bottom-sheet on main thread with a RecyclerView for better UX
            withContext(Dispatchers.Main) {
                val dialog = BottomSheetDialog(this@ReaderActivity)
                val content = layoutInflater.inflate(R.layout.bottom_sheet_toc, null)
                val recycler = content.findViewById<RecyclerView>(R.id.toc_recycler)

                val adapter = TocAdapter(flat) { which ->
                    Log.d(_tag, "openTocPanel: user selected TOC index=$which")
                    val chosenLocator = tocFlatLocators.getOrNull(which)
                    val navigator = supportFragmentManager.findFragmentByTag("EpubNavigator") as? EpubNavigatorFragment
                    if (navigator != null && chosenLocator != null) {
                        lifecycleScope.launch {
                            Log.d(_tag, "openTocPanel: navigating to locator href=${chosenLocator.href} progression=${chosenLocator.locations.totalProgression}")
                            navigator.go(chosenLocator, animated = false)
                        }
                        dialog.dismiss()
                    } else {
                        lifecycleScope.launch {
                            try {
                                Log.d(_tag, "openTocPanel: fallback navigation for index=$which, href=${hrefs.getOrNull(which)}")
                                val pos = try { pub.positions() } catch (_: Throwable) { emptyList<Locator>() }
                                val target = findBestPositionForHref(hrefs[which], pos)
                                if (target != null) {
                                    val nav = supportFragmentManager.findFragmentByTag("EpubNavigator") as? EpubNavigatorFragment
                                    if (nav != null) {
                                        Log.d(_tag, "openTocPanel: fallback resolved locator href=${target.href} progression=${target.locations.totalProgression}")
                                        nav.go(target, animated = false)
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(this@ReaderActivity, getString(R.string.toc_navigation_failed), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (t: Throwable) {
                                Log.w(_tag, "TOC navigation error: ${t.message}")
                            }
                        }
                        dialog.dismiss()
                    }
                }

                recycler.layoutManager = LinearLayoutManager(this@ReaderActivity)
                recycler.adapter = adapter

                // set initial selection and expose adapter/recycler for updates
                adapter.select(tocSelectedIndex)
                if (tocSelectedIndex >= 0) recycler.scrollToPosition(tocSelectedIndex)

                dialog.setContentView(content)
                dialog.setOnDismissListener {
                    tocDialog = null
                    tocAdapter = null
                    tocRecycler = null
                }

                tocDialog = dialog
                tocAdapter = adapter
                tocRecycler = recycler
                dialog.show()

                // expand sheet and animate entrance
                val bottomSheet = dialog.findViewById<android.view.View>(com.google.android.material.R.id.design_bottom_sheet)
                if (bottomSheet != null) {
                    try {
                        // Apply reader theme background to the bottom sheet so it matches the reader theme
                        try {
                            val sheetColor = appColorsFor(libraryConfig.backgroundMode).bg.toArgb()
                            bottomSheet.setBackgroundColor(sheetColor)
                        } catch (_: Exception) { }
                        val behavior = BottomSheetBehavior.from(bottomSheet)
                        behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    } catch (_: Exception) { }
                }
            }
        }
    }

    private fun updateUiWithLocator(locator: Locator) {
        val chapterTitle = locator.title ?: ""
        val progression = locator.locations.totalProgression ?: 0.0
        val percent = (progression * 100).toInt().coerceIn(0, 100)
        val bookTitle = publication?.metadata?.title?.uppercase() ?: (TestReaderInjector.fakeTitle?.uppercase() ?: "")
        val bookAuthor = publication?.metadata?.authors?.firstOrNull()?.name ?: TestReaderInjector.fakeAuthor

        findViewById<TextView>(R.id.immersive_header_title).text = chapterTitle.uppercase()
        findViewById<TextView>(R.id.immersive_footer_text).text = getString(com.nosferatu.launcher.R.string.reader_footer_format, bookTitle, percent)
        findViewById<ProgressBar>(R.id.immersive_progress_bar).progress = percent

        findViewById<TextView>(R.id.menu_page_text).text = getString(com.nosferatu.launcher.R.string.percent_format, percent)
        if (!isSeeking) {
            findViewById<SeekBar>(R.id.menu_seek_bar).progress = percent
        }
        findViewById<TextView>(R.id.menu_book_title).text = bookTitle
        findViewById<TextView>(R.id.menu_book_author).text = bookAuthor
    }

    private fun setupProgressBarSeek(navigator: EpubNavigatorFragment) {
        val menuSeekBar = findViewById<SeekBar>(R.id.menu_seek_bar)
        menuSeekBar.max = 100
        menuSeekBar.setOnSeekBarChangeListener(createSeekBarListener(navigator))
    }

    private fun createSeekBarListener(navigator: EpubNavigatorFragment): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isSeeking = true
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    findViewById<TextView>(R.id.menu_page_text).text = "$progress%"
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val targetProgression = (seekBar.progress / 100.0).coerceIn(0.0, 1.0)
                val pub = publication
                if (pub == null) {
                    isSeeking = false
                    return
                }

                lifecycleScope.launch {
                    try {
                        val locator = pub.locateProgression(targetProgression) ?: return@launch
                        _lastKnownLocator = locator
                        saveLocation(bookId, locator)
                        navigator.go(locator, animated = false)
                    } finally {
                        isSeeking = false
                    }
                }
            }
        }
    }

    private fun toggleMenu() {
        isMenuVisible = !isMenuVisible
        val topBar = findViewById<View>(R.id.menu_top_bar)
        val bottomBar = findViewById<View>(R.id.menu_bottom_bar)
        val immersiveFooter = findViewById<View>(R.id.immersive_footer)
        val fontContainer = findViewById<View>(R.id.font_controls_container)

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
            // font toggle removed from UI; nothing to reset
        }
    }

    private suspend fun openPublication(file: File): Publication = withContext(Dispatchers.IO) {
        val asset = assetRetriever.retrieve(file).getOrElse { throw Exception("Asset fallito") }
        publicationOpener.open(asset, allowUserInteraction = true).getOrElse { throw Exception("Opener fallito") }
    }

    private fun saveLocation(bookId: Long, locator: Locator?) {
        if (bookId == -1L) return
        if (locator == null) return
        val jsonLocation = locator.toJSON().toString()
        val progression = locator.locations.totalProgression ?: 0.0
        viewModel.saveBookPosition(bookId, jsonLocation, progression)
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

            // Side tap navigation — left/right zones swappable via invertTouches
            val tapRight = point.x > screenWidth * 0.5f
            val goForward = if (libraryConfig.invertTouches) !tapRight else tapRight
            val navigator = supportFragmentManager.findFragmentByTag("EpubNavigator") as? EpubNavigatorFragment
                ?: return false
            lifecycleScope.launch { navigateByOnePage(navigator, goForward) }
            return true
        }
    }

    override fun onJumpToLocator(locator: Locator) { _lastKnownLocator = locator }
    @ExperimentalReadiumApi override fun onExternalLinkActivated(url: AbsoluteUrl) { /* do nothing */ }

    private suspend fun navigateByOnePage(navigator: EpubNavigatorFragment, forward: Boolean) {
        val pub = publication ?: return
        val allPositions = pub.positions()
        if (allPositions.isEmpty()) {
            if (forward) navigator.goForward(animated = true) else navigator.goBackward(animated = true)
            return
        }
        val currentProgression = _lastKnownLocator?.locations?.totalProgression ?: 0.0
        val currentIndex = allPositions.indexOfLast { (it.locations.totalProgression ?: 0.0) <= currentProgression + 0.0001 }
        val safeIndex = if (currentIndex < 0) 0 else currentIndex
        val targetIndex = if (forward) safeIndex + 1 else safeIndex - 1
        val target = allPositions.getOrNull(targetIndex.coerceIn(0, allPositions.lastIndex)) ?: return
        navigator.go(target, animated = false)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (libraryConfig.volumeKeys && event.action == KeyEvent.ACTION_DOWN) {
            val navigator = supportFragmentManager.findFragmentByTag("EpubNavigator") as? EpubNavigatorFragment
            if (navigator != null) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        val forward = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP
                        lifecycleScope.launch { navigateByOnePage(navigator, forward) }
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}