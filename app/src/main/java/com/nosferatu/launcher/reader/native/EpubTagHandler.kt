package com.nosferatu.launcher.reader.native

import android.text.Editable
import android.text.Html
import android.text.style.AlignmentSpan
import android.text.style.LeadingMarginSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.text.Layout
import org.xml.sax.XMLReader

class EpubTagHandler : Html.TagHandler {

    // Marcatori per identificare l'inizio dei tag
    private class Header
    private class Blockquote

    override fun handleTag(opening: Boolean, tag: String, output: Editable, xmlReader: XMLReader) {
        when {
            tag.equals("p", ignoreCase = true) -> handleParagraph(opening, output)
            tag.equals("blockquote", ignoreCase = true) -> handleBlockquote(opening, output)
            tag.matches(Regex("h[1-6]", RegexOption.IGNORE_CASE)) -> handleHeader(opening, output)
        }
    }

    private fun handleParagraph(opening: Boolean, output: Editable) {
        if (opening) {
            output.setSpan(this, output.length, output.length, Editable.SPAN_MARK_MARK)
        } else {
            val start = output.getSpanStart(this)
            val end = output.length
            output.removeSpan(this)

            if (start != end) {
                // LeadingMarginSpan.Standard(indentFirstLine, indentRest)
                // 40px di rientro sulla prima riga, tipico dei libri
                output.setSpan(LeadingMarginSpan.Standard(40, 0), start, end, 0)
                output.append("\n\n") // Doppio spazio per separazione naturale
            }
        }
    }

    private fun handleHeader(opening: Boolean, output: Editable) {
        if (opening) {
            output.setSpan(Header(), output.length, output.length, Editable.SPAN_MARK_MARK)
        } else {
            val start = output.getSpanStart(Header::class.java)
            val end = output.length
            output.removeSpan(Header::class.java)

            if (start != end) {
                // Titoli in grassetto, più grandi e centrati
                output.setSpan(RelativeSizeSpan(1.5f), start, end, 0)
                output.setSpan(StyleSpan(Typeface.BOLD), start, end, 0)
                output.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), start, end, 0)
                output.append("\n\n")
            }
        }
    }

    private fun handleBlockquote(opening: Boolean, output: Editable) {
        if (opening) {
            output.setSpan(Blockquote(), output.length, output.length, Editable.SPAN_MARK_MARK)
        } else {
            val start = output.getSpanStart(Blockquote::class.java)
            val end = output.length
            output.removeSpan(Blockquote::class.java)

            if (start != end) {
                // Rientro totale per le citazioni (sia prima riga che successive)
                output.setSpan(LeadingMarginSpan.Standard(60, 60), start, end, 0)
                output.setSpan(StyleSpan(Typeface.ITALIC), start, end, 0)
                output.append("\n")
            }
        }
    }
}