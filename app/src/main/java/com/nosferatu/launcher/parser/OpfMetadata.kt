package com.nosferatu.launcher.parser

import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream

data class OpfMetadata (
    val title: String? = null,
    val author: String? = null,
    val coverHref: String? = null
)

object OpfParser {
    private const val _tag = "OpfParser"

    fun parse(inputStream: InputStream): OpfMetadata {
        // 1. Leggiamo tutto lo stream in memoria per poterlo loggare E parsare
        val content = inputStream.bufferedReader().use { it.readText() }

        Log.d(_tag, "== DUMP XML OPF START ==")
        Log.v(_tag, content) // Log verbose per non intasare il logcat principale
        Log.d(_tag, "== DUMP XML OPF END (Size: ${content.length} chars) ==")

        // 2. Creiamo un nuovo stream dalla stringa per il parser
        val parserStream = ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))

        val parser = Xml.newPullParser()
        parser.setInput(parserStream, "UTF-8")

        var title: String? = null
        var author: String? = null
        var coverId: String? = null
        val manifest = mutableMapOf<String, String>()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name
                    val prefix = parser.prefix

                    Log.v(_tag, "Processing Tag: <$name> [Prefix: $prefix]")

                    when (name) {
                        "title", "dc:title" -> {
                            title = parser.nextText()
                            Log.d(_tag, "[MATCH] Titolo: $title")
                        }
                        "creator", "dc:creator" -> {
                            author = parser.nextText()
                            Log.d(_tag, "[MATCH] Autore: $author")
                        }
                        "meta" -> {
                            val metaName = parser.getAttributeValue(null, "name")
                            val contentAttr = parser.getAttributeValue(null, "content")
                            if (metaName == "cover") {
                                coverId = contentAttr
                                Log.d(_tag, "[MATCH] Found cover ID in <meta>: $coverId")
                            }
                        }
                        "item" -> {
                            val id = parser.getAttributeValue(null, "id")
                            val href = parser.getAttributeValue(null, "href")
                            if (id != null && href != null) {
                                manifest[id] = href
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        val finalCoverHref = manifest[coverId]
        Log.d(_tag, "== OPF PARSING FINISHED ==")
        Log.d(_tag, "Final Data -> Title: $title, Author: $author, CoverHref: $finalCoverHref")

        return OpfMetadata(
            title = title,
            author = author,
            coverHref = finalCoverHref
        )
    }
}