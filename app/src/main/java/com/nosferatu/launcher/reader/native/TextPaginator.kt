package com.nosferatu.launcher.reader.native

import android.annotation.SuppressLint
import android.graphics.text.LineBreaker
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.text.HtmlCompat
import androidx.core.text.parseAsHtml

object TextPaginator {

    @SuppressLint("WrongConstant")
    fun paginate(
        htmlContent: String,
        width: Int,
        height: Int,
        paint: TextPaint,
        spacingMult: Float = 1.4f
    ): List<CharSequence> {

        val cleanHtml = htmlContent
            .replace(Regex("<br\\s*/?>"), "\n")
            .replace("&nbsp;", " ")
            .replace("\r", "")

        val tagHandler = EpubTagHandler()
        val spannedText = cleanHtml.parseAsHtml(
            flags = HtmlCompat.FROM_HTML_MODE_LEGACY,
            tagHandler = tagHandler
        )

        val pages = mutableListOf<CharSequence>()

        val layout = StaticLayout.Builder.obtain(spannedText, 0, spannedText.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, spacingMult)
            .setIncludePad(false)
            .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_FULL)
            .setTextDirection(TextDirectionHeuristics.LTR)
            .build()

        var startLine = 0
        val totalLines = layout.lineCount

        while (startLine < totalLines) {
            val startTop = layout.getLineTop(startLine)
            val textPaddingVertical = 130
            val availableHeight = height - textPaddingVertical

            var endLine = layout.getLineForVertical(startTop + availableHeight)

            if (endLine > startLine) {
                if (layout.getLineBottom(endLine - 1) > (startTop + availableHeight)) {
                    endLine--
                }
            }

            if (endLine < totalLines) {
                val startOffset = layout.getLineStart(startLine)
                val candidateEndOffset = layout.getLineEnd(endLine - 1)
                val lastNewline = spannedText.subSequence(startOffset, candidateEndOffset).lastIndexOf('\n')

                if (lastNewline > 0 && (candidateEndOffset - (startOffset + lastNewline)) < 150) {
                    val newlineLine = layout.getLineForOffset(startOffset + lastNewline)
                    if (newlineLine > startLine) {
                        endLine = newlineLine + 1
                    }
                }
            }

            val safeEndLine = if (endLine <= startLine) startLine + 1 else endLine
            val startOffset = layout.getLineStart(startLine)
            val endOffset = layout.getLineEnd(minOf(safeEndLine - 1, totalLines - 1))

            val pageContent = spannedText.subSequence(startOffset, endOffset)
            if (pageContent.isNotBlank()) {
                pages.add(pageContent)
            }

            startLine = safeEndLine
        }
        return pages
    }
}