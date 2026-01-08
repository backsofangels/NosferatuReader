package com.nosferatu.launcher

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.nosferatu.launcher.utils.EpubExtractor

class ReaderActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var chapters: List<String> = emptyList()
    private var currentChapterIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Usiamo un FrameLayout per sovrapporre aree di tocco alla WebView
        val root = android.widget.FrameLayout(this)
        webView = WebView(this)
        root.addView(webView)

        // Aggiungiamo le zone di "Tap" (Destra per avanti, Sinistra per indietro)
        val viewNext = android.view.View(this)
        val viewPrev = android.view.View(this)

        // Configurazione zone (circa 20% dello schermo ai lati)
        val paramsNext = android.widget.FrameLayout.LayoutParams(200, android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.Gravity.END)
        val paramsPrev = android.widget.FrameLayout.LayoutParams(200, android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.Gravity.START)

        root.addView(viewNext, paramsNext)
        root.addView(viewPrev, paramsPrev)

        setContentView(root)

        setupWebView()

        val filePath = intent.getStringExtra("FILE_PATH") ?: return

        // Per ora carichiamo tutto, ma con un occhio ai logcat per memoria
        chapters = EpubExtractor.getAllChapters(filePath)

        if (chapters.isNotEmpty()) {
            displayCurrentChapter()
        }

        viewNext.setOnClickListener {
            if (currentChapterIndex < chapters.size - 1) {
                currentChapterIndex++
                displayCurrentChapter()
            }
        }

        viewPrev.setOnClickListener {
            if (currentChapterIndex > 0) {
                currentChapterIndex--
                displayCurrentChapter()
            }
        }
    }

    private fun setupWebView() {
        with(webView.settings) {
            javaScriptEnabled = false // Fondamentale per la stabilità sul Tilapia
            loadWithOverviewMode = true
            loadsImagesAutomatically = false
            cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            useWideViewPort = false
            setSupportZoom(true)
            builtInZoomControls = false // Evitiamo i controlli brutti di Android
        }
    }

    private fun injectKoboStyles(html: String): String {
        val css = """
            <style>
                body {
                    background-color: white !important;
                    color: #1a1a1a !important;
                    font-family: 'serif' !important;
                    line-height: 1.6 !important;
                    padding: 30px !important;
                    font-size: 18px !important;
                    text-align: justify !important;
                }
                img { max-width: 100%; height: auto; }
            </style>
        """.trimIndent()

        return "<html><head>$css</head><body>$html</body></html>"
    }

    private fun displayCurrentChapter() {
        val content = chapters[currentChapterIndex]
        val styledHtml = injectKoboStyles(content)
        webView.loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null)
        // Opzionale: scrolla in cima quando cambi capitolo
        webView.scrollTo(0, 0)
    }
}