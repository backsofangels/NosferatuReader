package com.nosferatu.launcher.parser

import android.util.Log
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.zip.ZipFile

class EpubParser : ParserStrategy {
    private val _tag = "EpubParser"

    override suspend fun parse(file: File): RawMetadata = withContext(Dispatchers.IO) {
        Log.d(_tag, "Starting EPUB parsing for: ${file.name}")
        try {
            ZipFile(file).use { zip ->
                // Find OPF file reading container
                val opfPath = getOpfPath(zip) ?: run {
                    Log.w(_tag, "OPF path not found in ${file.name}")
                    return@withContext fallback(file)
                }
                Log.d(_tag, "Found OPF path: $opfPath")

                val opfEntry = zip.getEntry(opfPath) ?: run {
                    Log.w(_tag, "OPF entry not found at $opfPath in ${file.name}")
                    return@withContext fallback(file)
                }

                // Parsing metadata
                Log.d(_tag, "Parsing OPF metadata for: ${file.name}")
                val opfMetadata = zip.getInputStream(opfEntry).use { OpfParser.parse(it) }
                Log.d(_tag, "OPF metadata parsed: Title='${opfMetadata.title}', Author='${opfMetadata.author}'")

                // Cover extraction
                var coverBytes: ByteArray? = null
                opfMetadata.coverHref?.let { href ->
                    val fullPath = resolvePath(opfPath, href)
                    Log.d(_tag, "Attempting to extract cover from: $fullPath")
                    zip.getEntry(fullPath)?.let { entry ->
                        coverBytes = zip.getInputStream(entry).readBytes()
                        Log.d(_tag, "Cover extracted successfully (${coverBytes?.size} bytes)")
                    } ?: Log.w(_tag, "Cover entry not found at $fullPath")
                } ?: Log.d(_tag, "No cover found in OPF metadata")

                RawMetadata(
                    title = opfMetadata.title ?: file.nameWithoutExtension,
                    author = opfMetadata.author,
                    coverData = coverBytes
                )
            }
        } catch (e: Exception) {
            Log.e(_tag, "Critical error parsing EPUB: ${file.name}", e)
            fallback(file)
        }
    }

    private fun getOpfPath(zip: ZipFile): String? {
        val entry = zip.getEntry("META-INF/container.xml") ?: return null
        val parser = Xml.newPullParser()
        parser.setInput(zip.getInputStream(entry), "UTF-8")

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "rootfile") {
                return parser.getAttributeValue(null, "full-path")
            }
            eventType = parser.next()
        }
        return null
    }

    private fun resolvePath(basePath: String, relativePath: String): String {
        val parent = File(basePath).parent ?: ""
        return if (parent.isEmpty()) relativePath else "$parent/$relativePath"
    }

    private fun fallback(file: File): RawMetadata {
        Log.d(_tag, "Using fallback metadata for: ${file.name}")
        return RawMetadata(file.nameWithoutExtension, null)
    }
}
