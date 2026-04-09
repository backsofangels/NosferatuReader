package com.nosferatu.launcher.reader

import android.util.Log

object ThemeAwareCssInjector {
    private const val TAG = "ThemeAwareCssInjector"

    data class ThemeColors(
        val background: String,
        val foreground: String,
        val accent: String,
        val fontFamily: String
    )

    fun getThemeColors(backgroundMode: Int, fontChoice: Int): ThemeColors {
        val (bg, fg, accent) = when (backgroundMode) {
            1 -> Triple("#FFF8E1", "#333333", "#D4A574")  // Cream
            2 -> Triple("#222222", "#E8E8E8", "#BB86FC")  // Dark
            else -> Triple("#FFFFFF", "#000000", "#6200EE")  // Light
        }

        val font = when (fontChoice) {
            1 -> "Georgia, serif"  // Literata fallback
            else -> "system-ui, -apple-system, sans-serif"
        }

        return ThemeColors(bg, fg, accent, font)
    }

    fun generateLeanCss(backgroundMode: Int, fontChoice: Int): String {
        val colors = getThemeColors(backgroundMode, fontChoice)

        Log.d(TAG, "Generating CSS: bg=${colors.background}, fg=${colors.foreground}, font=${colors.fontFamily}")

        return """
            /* === Nosferatu Theme-Aware Lean CSS === */
            
            body {
                background-color: ${colors.background} !important;
                color: ${colors.foreground} !important;
                font-family: ${colors.fontFamily} !important;
                line-height: 1.6 !important;
                margin: 1em !important;
                padding: 0.5em !important;
            }

            p {
                margin: 0.5em 0 !important;
                orphans: 3;
                widows: 3;
            }

            h1, h2, h3, h4, h5, h6 {
                color: ${colors.accent} !important;
                font-weight: 700 !important;
                line-height: 1.3 !important;
                margin-top: 1.5em !important;
                margin-bottom: 0.5em !important;
            }

            h1 { font-size: 1.8em; }
            h2 { font-size: 1.5em; }
            h3 { font-size: 1.3em; }

            a {
                color: ${colors.accent} !important;
                text-decoration: underline !important;
            }

            strong, b {
                font-weight: 700 !important;
            }

            em, i {
                font-style: italic !important;
            }

            blockquote {
                border-left: 3px solid ${colors.accent} !important;
                margin-left: 1em !important;
                padding-left: 1em !important;
                opacity: 0.9 !important;
            }

            code, pre {
                background-color: ${if (backgroundMode == 2) "#333333" else "#f0f0f0"} !important;
                font-family: 'Courier New', monospace !important;
                padding: 0.2em 0.4em !important;
                border-radius: 2px !important;
            }

            img {
                max-width: 100% !important;
                height: auto !important;
                display: block !important;
            }

            hr {
                border: none !important;
                border-top: 1px solid ${colors.accent} !important;
            }

            table {
                width: 100% !important;
                border-collapse: collapse !important;
            }

            th, td {
                border: 1px solid ${colors.accent} !important;
                padding: 0.5em !important;
                text-align: left !important;
            }

            /* Disable publisher styles selectively */
            .publisher-style { display: none !important; }

            /* Support common EPUB classes */
            .toc, .ncx { display: none !important; }
        """.trimIndent()
    }
}
