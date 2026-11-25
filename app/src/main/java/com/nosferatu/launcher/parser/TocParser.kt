package com.nosferatu.launcher.parser

import android.util.Log
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

data class TocItem(
    val title: String?,
    val href: String?,
    val position: Int,
    val locatorJson: String? = null
)

class TocParser {
    private val TAG = "TocParser"

    suspend fun parse(file: File): List<TocItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<TocItem>()
        try {
            ZipFile(file).use { zip ->
                val opfPath = getOpfPath(zip) ?: return@withContext results
                val opfEntry = zip.getEntry(opfPath) ?: return@withContext results

                val opfContent = zip.getInputStream(opfEntry).use { it.readBytes() }

                val manifest = parseManifest(ByteArrayInputStream(opfContent))

                // Prefer item with properties containing 'nav'
                val navHref = manifest.values.firstOrNull { it.third.contains("nav") }?.first
                    ?: manifest.values.firstOrNull { it.second == "application/x-dtbncx+xml" }?.first
                    ?: manifest.values.firstOrNull { it.first.endsWith(".ncx", true) }?.first
                    ?: manifest.values.firstOrNull { it.first.contains("nav", true) }?.first

                if (navHref != null) {
                    val fullPath = resolvePath(opfPath, navHref)
                    val navEntry = zip.getEntry(fullPath)
                    if (navEntry != null) {
                        // Decide NCX vs XHTML by extension or media-type
                        val isNcx = navHref.endsWith(".ncx", true) ||
                                manifest.values.firstOrNull { it.first == navHref }?.second == "application/x-dtbncx+xml"

                        val items = if (isNcx) {
                            parseNcx(zip.getInputStream(navEntry))
                        } else {
                            parseNavHtml(zip.getInputStream(navEntry))
                        }

                        results.addAll(items.mapIndexed { idx, pair -> TocItem(pair.first, pair.second, idx) })
                    } else {
                        Log.w(TAG, "Nav entry not found in zip: $fullPath")
                    }
                } else {
                    Log.w(TAG, "No nav or NCX found in OPF manifest for: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing TOC for file: ${file.name}", e)
        }
        results
    }

    private fun getOpfPath(zip: ZipFile): String? {
        val entry = zip.getEntry("META-INF/container.xml") ?: return null
        val parser = Xml.newPullParser()
        parser.setInput(zip.getInputStream(entry), "UTF-8")

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "rootfile") {
                val path = parser.getAttributeValue(null, "full-path")
                return path
            }
            eventType = parser.next()
        }
        return null
    }

    private fun parseManifest(input: InputStream): Map<String, Triple<String, String, String>> {
        val manifest = mutableMapOf<String, Triple<String, String, String>>()
        val content = input.bufferedReader().use { it.readText() }
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(ByteArrayInputStream(content.toByteArray(Charsets.UTF_8)), "UTF-8")
        }

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                val id = parser.getAttributeValue(null, "id")
                val href = parser.getAttributeValue(null, "href")
                val type = parser.getAttributeValue(null, "media-type") ?: ""
                val props = parser.getAttributeValue(null, "properties") ?: ""
                if (id != null && href != null) {
                    manifest[id] = Triple(href, type, props)
                }
            }
            eventType = parser.next()
        }
        return manifest
    }

    private fun resolvePath(basePath: String, relativePath: String): String {
        val parent = File(basePath).parent ?: ""
        return if (parent.isEmpty()) relativePath else "$parent/$relativePath"
    }

    private fun parseNcx(input: InputStream): List<Pair<String?, String?>> {
        val result = mutableListOf<Pair<String?, String?>>()
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(input, "UTF-8")
        }

        var eventType = parser.eventType
        var currentText: String? = null
        var currentSrc: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "text" -> currentText = parser.nextText()
                    "content" -> currentSrc = parser.getAttributeValue(null, "src")
                }
            } else if (eventType == XmlPullParser.END_TAG && parser.name == "navPoint") {
                result.add(Pair(currentText, currentSrc))
                currentText = null
                currentSrc = null
            }
            eventType = parser.next()
        }

        return result
    }

    private fun parseNavHtml(input: InputStream): List<Pair<String?, String?>> {
        val result = mutableListOf<Pair<String?, String?>>()
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(input, "UTF-8")
        }

        var eventType = parser.eventType
        var insideA = false
        var href: String? = null
        var text: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.name.equals("a", true)) {
                    href = parser.getAttributeValue(null, "href")
                    insideA = true
                }
            } else if (eventType == XmlPullParser.TEXT) {
                if (insideA) {
                    text = (parser.text ?: text)?.trim()
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.name.equals("a", true) && insideA) {
                    result.add(Pair(text, href))
                    insideA = false
                    href = null
                    text = null
                }
            }
            eventType = parser.next()
        }

        return result
    }
}
