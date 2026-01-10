package com.nosferatu.launcher.reader.native

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextPaint
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageButton // Cambiato da Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.nosferatu.launcher.R
import com.nosferatu.launcher.data.database.AppDatabase
import com.nosferatu.launcher.utils.EpubExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    private lateinit var btnChangeTheme: ImageButton // Cambiato da Button

    // State
    private var chapters: List<String> = emptyList()
    private var currentChapterIndex = 0
    private var fontSizeSp = 18f
    private var isDarkMode = true
    private var savedPageIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        loadThemePreference()
        setTheme(if (isDarkMode) R.style.Theme_NosferatuLauncher_Dark else R.style.Theme_NosferatuLauncher)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader_native)

        initViews()
        setupListeners()
        loadInitialData()
    }

    override fun onPause() {
        super.onPause()
        saveProgress()
    }

    private fun saveProgress() {
        val filePath = intent.getStringExtra("FILE_PATH") ?: return
        val currentPos = viewPager.currentItem

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@ReaderActivityNative)
            db.bookDao().updateChapterAndPage(filePath, currentChapterIndex, currentPos)
        }
    }

    private fun initViews() {
        rootLayout = findViewById(R.id.mainRoot)
        viewPager = findViewById(R.id.viewPager)
        measureContainer = findViewById(R.id.measureContainer)
        topControls = findViewById(R.id.topControls)
        readerControls = findViewById(R.id.readerControls)
        readerHeader = findViewById(R.id.readerHeader)
        readerFooter = findViewById(R.id.readerFooter)
        btnChangeTheme = findViewById(R.id.btnChangeThemeTop) // Inizializzato come ImageButton

        adapter = BookPageAdapter(emptyList())
        viewPager.adapter = adapter

        syncInterfaceColors()
    }

    private fun setupListeners() {
        findViewById<View>(R.id.touchNext).setOnClickListener { navigateNext() }
        findViewById<View>(R.id.touchPrev).setOnClickListener { navigatePrevious() }
        findViewById<View>(R.id.touchCenter).setOnClickListener { toggleControls() }

        // Listener per l'ImageButton
        btnChangeTheme.setOnClickListener { toggleTheme() }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateFooter(position)
            }
        })
    }

    private fun loadInitialData() {
        val filePath = intent.getStringExtra("FILE_PATH") ?: return

        currentChapterIndex = intent.getIntExtra("LAST_CHAPTER", 0)
        savedPageIndex = intent.getIntExtra("LAST_POSITION", 0)

        val bookTitle = EpubExtractor.getBookTitle(filePath)
        readerHeader.text = bookTitle ?: "Documento senza titolo"

        chapters = EpubExtractor.getAllChapters(filePath)

        if (chapters.isNotEmpty()) {
            measureContainer.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    measureContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    loadChapter(currentChapterIndex, goToSavedPage = true)
                }
            })
        }
    }

    private fun loadChapter(index: Int, goToLastPage: Boolean = false, goToSavedPage: Boolean = false) {
        if (index !in chapters.indices) return

        currentChapterIndex = index
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

        val targetPage = when {
            goToSavedPage -> savedPageIndex.coerceIn(0, pages.size - 1)
            goToLastPage -> pages.size - 1
            else -> 0
        }

        viewPager.setCurrentItem(targetPage, false)
        updateFooter(viewPager.currentItem)
    }

    private fun navigateNext() {
        val currentPos = viewPager.currentItem
        if (currentPos < adapter.itemCount - 1) {
            viewPager.setCurrentItem(currentPos + 1, true)
        } else if (currentChapterIndex < chapters.size - 1) {
            loadChapter(currentChapterIndex + 1, goToLastPage = false)
        }
    }

    private fun navigatePrevious() {
        val currentPos = viewPager.currentItem
        if (currentPos > 0) {
            viewPager.setCurrentItem(currentPos - 1, true)
        } else if (currentChapterIndex > 0) {
            loadChapter(currentChapterIndex - 1, goToLastPage = true)
        }
    }

    private fun toggleControls() {
        val isVisible = topControls.visibility == View.VISIBLE
        if (isVisible) hideControlView(topControls).also { hideControlView(readerControls) }
        else showControlView(topControls).also { showControlView(readerControls) }
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

        // Sincronizza i colori e l'icona
        syncInterfaceColors()

        // Refresh adapter per applicare i colori al testo delle pagine
        viewPager.adapter = adapter
        viewPager.setCurrentItem(viewPager.currentItem, false)

        // Nasconde i controlli dopo il cambio
        toggleControls()
    }

    private fun syncInterfaceColors() {
        val bgColor = ContextCompat.getColor(this, if (isDarkMode) R.color.reader_bg_dark else R.color.reader_bg_light)
        val textColor = ContextCompat.getColor(this, if (isDarkMode) R.color.reader_text_dark else R.color.reader_text_light)

        rootLayout.setBackgroundColor(bgColor)
        readerHeader.setTextColor(textColor)
        readerFooter.setTextColor(textColor)

        // Cambio dinamico dell'icona:
        // Se siamo in Dark Mode, mostriamo il SOLE per passare al Light
        // Se siamo in Light Mode, mostriamo la LUNA per passare al Dark
        if (isDarkMode) {
            btnChangeTheme.setImageResource(R.drawable.ic_theme_light)
        } else {
            btnChangeTheme.setImageResource(R.drawable.ic_theme_dark)
        }
    }

    private fun updateFooter(position: Int) {
        val totalPages = adapter.itemCount
        readerFooter.text = "Pagina ${position + 1} di $totalPages"
    }

    private fun saveThemePreference(dark: Boolean) {
        getSharedPreferences("ReaderSettings", Context.MODE_PRIVATE).edit().putBoolean("DARK_MODE", dark).apply()
    }

    private fun loadThemePreference() {
        isDarkMode = getSharedPreferences("ReaderSettings", Context.MODE_PRIVATE).getBoolean("DARK_MODE", true)
    }

    fun changeFontSize(delta: Float) {
        fontSizeSp += delta
        loadChapter(currentChapterIndex)
    }
}