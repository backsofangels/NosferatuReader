package com.nosferatu.launcher.parser

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

data class OpfMetadata (
    val title: String? = null,
    val author: String? = null,
    val coverHref: String? = null
)

object OpfParser {
    fun parse(inputStream: InputStream): OpfMetadata {
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var title: String? = null
        var author: String? = null
        var coverId: String? = null
        val manifest = mutableMapOf<String, String>()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (name) {
                        "dc:title" -> title = parser.nextText()
                        "dc:creator" -> author = parser.nextText()
                        "meta" -> {
                            if (parser.getAttributeValue(null, "name") == "cover") {
                                coverId = parser.getAttributeValue(null, "content")
                            }
                        }
                        "item" -> {
                            val id = parser.getAttributeValue(null, "id")
                            val href = parser.getAttributeValue(null, "href")
                            if (id != null && href != null) manifest[id] = href
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return OpfMetadata(
            title = title,
            author = author,
            coverHref = manifest[coverId]
        )
    }
}