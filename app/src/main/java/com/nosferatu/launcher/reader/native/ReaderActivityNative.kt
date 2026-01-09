package com.nosferatu.launcher.reader.native

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextPaint
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.nosferatu.launcher.R
import com.nosferatu.launcher.utils.EpubExtractor

class ReaderActivityNative : AppCompatActivity() {

    // UI Elements
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: BookPageAdapter
    private lateinit var measureContainer: FrameLayout
    private lateinit var topControls: LinearLayout
    private lateinit var readerControls: LinearLayout
    private lateinit var readerHeader: TextView
    private lateinit var readerFooter: TextView
    private lateinit var rootLayout: View

    // State
    private var chapters: List<String> = emptyList()
    private var currentChapterIndex = 0
    private var fontSizeSp = 18f
    private var isDarkMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // Carica la preferenza prima di creare la view per evitare il flash bianco
        loadThemePreference()
        setTheme(if (isDarkMode) R.style.Theme_NosferatuLauncher_Dark else R.style.Theme_NosferatuLauncher)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader_native)

        initViews()
        setupListeners()
        loadInitialData()
    }

    private fun initViews() {
        rootLayout = findViewById(R.id.mainRoot)
        viewPager = findViewById(R.id.viewPager)
        measureContainer = findViewById(R.id.measureContainer)

        // Barre di controllo (Top e Bottom)
        topControls = findViewById(R.id.topControls)
        readerControls = findViewById(R.id.readerControls)

        readerHeader = findViewById(R.id.readerHeader)
        readerFooter = findViewById(R.id.readerFooter)

        adapter = BookPageAdapter(emptyList())
        viewPager.adapter = adapter

        // Sincronizza i colori iniziali dell'interfaccia statica
        syncInterfaceColors()
    }

    private fun setupListeners() {
        // Navigazione tramite Tap laterali
        findViewById<View>(R.id.touchNext).setOnClickListener { navigateNext() }
        findViewById<View>(R.id.touchPrev).setOnClickListener { navigatePrevious() }

        // Menu Opzioni tramite zona centrale
        findViewById<View>(R.id.touchCenter).setOnClickListener { toggleControls() }

        // Tasto cambio tema nella barra superiore (Kobo style)
        findViewById<Button>(R.id.btnChangeThemeTop).setOnClickListener { toggleTheme() }

        // Listener per aggiornare il numero di pagina nel footer
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateFooter(position)
            }
        })
    }

    private fun loadInitialData() {
        val filePath = intent.getStringExtra("FILE_PATH") ?: return

        // Imposta il titolo reale del libro nell'header
        val bookTitle = EpubExtractor.getBookTitle(filePath)
        readerHeader.text = bookTitle ?: "Documento senza titolo"

        // Carica i capitoli usando la logica ZIP ripristinata
        chapters = EpubExtractor.getAllChapters(filePath)

        if (chapters.isNotEmpty()) {
            measureContainer.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    measureContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    loadChapter(currentChapterIndex, goToLastPage = false)
                }
            })
        }
    }

    private fun loadChapter(index: Int, goToLastPage: Boolean) {
        if (index !in chapters.indices) return

        val chapterHtml = chapters[index]
        val paint = TextPaint().apply {
            textSize = fontSizeSp * resources.displayMetrics.scaledDensity
            typeface = Typeface.SERIF
            isAntiAlias = true
        }

        val pages = TextPaginator.paginate(
            chapterHtml,
            measureContainer.width,
            measureContainer.height,
            paint,
            1.4f
        )

        adapter.updateData(pages)
        viewPager.setCurrentItem(if (goToLastPage) pages.size - 1 else 0, false)
        updateFooter(viewPager.currentItem)
    }

    // --- LOGICA DI NAVIGAZIONE ---

    private fun navigateNext() {
        val currentPos = viewPager.currentItem
        if (currentPos < adapter.itemCount - 1) {
            viewPager.setCurrentItem(currentPos + 1, true)
        } else if (currentChapterIndex < chapters.size - 1) {
            currentChapterIndex++
            loadChapter(currentChapterIndex, goToLastPage = false)
        }
    }

    private fun navigatePrevious() {
        val currentPos = viewPager.currentItem
        if (currentPos > 0) {
            viewPager.setCurrentItem(currentPos - 1, true)
        } else if (currentChapterIndex > 0) {
            currentChapterIndex--
            loadChapter(currentChapterIndex, goToLastPage = true)
        }
    }

    // --- UI & THEME MANAGEMENT ---

    private fun toggleControls() {
        val isVisible = topControls.visibility == View.VISIBLE
        if (isVisible) {
            hideControlView(topControls)
            hideControlView(readerControls)
        } else {
            showControlView(topControls)
            showControlView(readerControls)
        }
    }

    private fun showControlView(view: View) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.animate().alpha(1f).setDuration(250).start()
    }

    private fun hideControlView(view: View) {
        view.animate().alpha(0f).setDuration(250).withEndAction {
            view.visibility = View.GONE
        }.start()
    }

    private fun toggleTheme() {
        isDarkMode = !isDarkMode
        saveThemePreference(isDarkMode)

        // Applica il tema all'activity
        setTheme(if (isDarkMode) R.style.Theme_NosferatuLauncher_Dark else R.style.Theme_NosferatuLauncher)

        // Sincronizza colori UI statica
        syncInterfaceColors()

        // Refresh adapter per ricolorare le pagine caricate nel ViewPager
        viewPager.adapter = adapter
        val currentPos = viewPager.currentItem
        viewPager.setCurrentItem(currentPos, false)

        // Chiudi le barre dopo il cambio
        toggleControls()
    }

    private fun syncInterfaceColors() {
        val bgColor = ContextCompat.getColor(this, if (isDarkMode) R.color.reader_bg_dark else R.color.reader_bg_light)
        val textColor = ContextCompat.getColor(this, if (isDarkMode) R.color.reader_text_dark else R.color.reader_text_light)

        rootLayout.setBackgroundColor(bgColor)
        readerHeader.setTextColor(textColor)
        readerFooter.setTextColor(textColor)
    }

    private fun updateFooter(position: Int) {
        val totalPages = adapter.itemCount
        readerFooter.text = "Pagina ${position + 1} di $totalPages"
    }

    // --- PERSISTENZA ---

    private fun saveThemePreference(dark: Boolean) {
        val sharedPref = getSharedPreferences("ReaderSettings", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean("DARK_MODE", dark).apply()
    }

    private fun loadThemePreference() {
        val sharedPref = getSharedPreferences("ReaderSettings", Context.MODE_PRIVATE)
        isDarkMode = sharedPref.getBoolean("DARK_MODE", true)
    }

    fun changeFontSize(delta: Float) {
        fontSizeSp += delta
        loadChapter(currentChapterIndex, goToLastPage = false)
    }
}