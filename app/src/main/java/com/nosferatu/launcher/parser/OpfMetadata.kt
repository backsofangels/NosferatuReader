package com.nosferatu.launcher.parser

import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream

data class OpfMetadata(
    val title: String? = null,
    val author: String? = null,
    val coverHref: String? = null
)

object OpfParser {
    private const val TAG = "OpfParser"
    private const val NS_DC = "http://purl.org/dc/elements/1.1/"
    private const val NS_OPF = "http://www.idpf.org/2007/opf"

    fun parse(inputStream: InputStream): OpfMetadata {
        val content = inputStream.bufferedReader().use { it.readText() }

        Log.v(TAG, "OPF Content Dump: $content")

        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(ByteArrayInputStream(content.toByteArray(Charsets.UTF_8)), "UTF-8")
        }

        var title: String? = null
        var author: String? = null
        var coverIdFromMeta: String? = null
        var coverHrefFromGuide: String? = null
        val manifest = mutableMapOf<String, Pair<String, String>>()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                val name = parser.name
                val ns = parser.namespace

                Log.v(TAG, "Parsing tag: <$name> in namespace: $ns")

                when {
                    ns == NS_DC && name == "title" -> {
                        title = parser.nextText()
                        Log.d(TAG, "Title matched: $title")
                    }
                    ns == NS_DC && name == "creator" -> {
                        author = parser.nextText()
                        Log.d(TAG, "Author matched: $author")
                    }
                    name == "meta" -> {
                        val nameAttr = parser.getAttributeValue(null, "name")
                        val contentAttr = parser.getAttributeValue(null, "content")
                        if (nameAttr == "cover") {
                            coverIdFromMeta = contentAttr
                            Log.d(TAG, "Cover ID found in metadata: $coverIdFromMeta")
                        }
                    }
                    name == "item" -> {
                        val id = parser.getAttributeValue(null, "id")
                        val href = parser.getAttributeValue(null, "href")
                        val type = parser.getAttributeValue(null, "media-type")
                        if (id != null && href != null) {
                            manifest[id] = href to (type ?: "")
                            Log.v(TAG, "Manifest entry: $id -> $href ($type)")
                        }
                    }
                    name == "reference" -> {
                        val type = parser.getAttributeValue(null, "type")
                        val href = parser.getAttributeValue(null, "href")
                        if (type == "cover" && href != null && !href.endsWith(".html") && !href.endsWith(".xhtml")) {
                            coverHrefFromGuide = href
                            Log.d(TAG, "Cover href found in guide: $coverHrefFromGuide")
                        } else if (type == "cover") {
                            Log.w(TAG, "Ignored HTML cover page in guide: $href")
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        val finalCoverHref = resolveCover(coverIdFromMeta, coverHrefFromGuide, manifest)

        Log.d(TAG, "Parsing complete. Result: Title=$title, Author=$author, Cover=$finalCoverHref")

        return OpfMetadata(title, author, finalCoverHref)
    }

    private fun resolveCover(
        metaId: String?,
        guideHref: String?,
        manifest: Map<String, Pair<String, String>>
    ): String? {
        val fromMeta = manifest[metaId]
        if (fromMeta != null && fromMeta.second.startsWith("image/")) {
            Log.d(TAG, "Resolution: Success using Meta ID ($metaId)")
            return decodeAndLog(fromMeta.first)
        }

        if (guideHref != null) {
            val isText = guideHref.endsWith(".html", true) ||
                    guideHref.endsWith(".xhtml", true) ||
                    guideHref.endsWith(".htm", true)
            if (!isText) {
                Log.d(TAG, "Resolution: Success using Guide href ($guideHref)")
                return decodeAndLog(guideHref)
            } else {
                Log.w(TAG, "Resolution: Guide points to HTML page ($guideHref), ignoring and falling back to heuristics")
            }
        }

        val heuristicMatch = manifest.values.firstOrNull { (href, type) ->
            type.startsWith("image/") && (
                    href.contains("cover", ignoreCase = true) ||
                            href.contains("thumb", ignoreCase = true) ||
                            href.contains("front", ignoreCase = true)
                    )
        }?.first

        if (heuristicMatch != null) {
            Log.d(TAG, "Resolution: Success using heuristic search ($heuristicMatch)")
            return decodeAndLog(heuristicMatch)
        }

        val firstImage = manifest.values.firstOrNull { it.second.startsWith("image/") }?.first
        if (firstImage != null) {
            Log.w(TAG, "Resolution: Extreme fallback, picking first available image: $firstImage")
            return decodeAndLog(firstImage)
        }

        Log.e(TAG, "Resolution: Failed to find any image cover in any section")
        return null
    }

    private fun decodeAndLog(href: String): String {
        return try {
            java.net.URLDecoder.decode(href, "UTF-8")
        } catch (e: Exception) {
            Log.e(TAG, "Decoding failed for href: $href", e)
            href
        }
    }
}