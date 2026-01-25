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
        Log.d(_tag, "== START PARSING: ${file.name} ==")
        try {
            ZipFile(file).use { zip ->
                // 1. Ricerca del file OPF
                val opfPath = getOpfPath(zip) ?: run {
                    Log.e(_tag, "[!] ABORT: container.xml non trovato o 'full-path' mancante in ${file.name}")
                    return@withContext fallback(file)
                }
                Log.d(_tag, "[1] OPF Path individuato: $opfPath")

                val opfEntry = zip.getEntry(opfPath) ?: run {
                    Log.e(_tag, "[!] ABORT: L'entry zip $opfPath è dichiarata nel container ma non esiste fisicamente")
                    return@withContext fallback(file)
                }

                Log.v(_tag, "Entry: ${opfEntry.name}")

                // 2. Parsing dei metadati tramite OpfParser
                Log.d(_tag, "[2] Avvio OpfParser.parse per: $opfPath")

                val opfMetadata = zip.getInputStream(opfEntry).use { OpfParser.parse(it) }
                Log.d(_tag, "[2.1] OpfMetadata content: $opfMetadata")

                Log.d(_tag, "[3] Risultato OpfParser: Title='${opfMetadata.title}', Author='${opfMetadata.author}', CoverHref='${opfMetadata.coverHref}'")

                // 3. Estrazione della Cover
                var coverBytes: ByteArray? = null
                opfMetadata.coverHref?.let { href ->
                    val fullPath = resolvePath(opfPath, href)
                    Log.d(_tag, "[4] Tentativo estrazione cover. Href originale: '$href' -> Path risolto: '$fullPath'")

                    val entry = zip.getEntry(fullPath)
                    if (entry != null) {
                        coverBytes = zip.getInputStream(entry).readBytes()
                        Log.d(_tag, "[5] SUCCESS: Cover estratta correttamente (${coverBytes?.size} bytes)")
                    } else {
                        Log.e(_tag, "[!] FAILURE: L'entry cover '$fullPath' non esiste nello zip. Verifica resolvePath!")
                        // Qui cerchiamo di capire cosa c'è nello zip per debuggare
                        // zip.entries().asSequence().forEach { Log.v(_tag, "Zip Entry: ${it.name}") }
                    }
                } ?: Log.w(_tag, "[4] WARNING: Nessun Href cover trovato nei metadati OPF")

                RawMetadata(
                    title = opfMetadata.title ?: file.nameWithoutExtension,
                    author = opfMetadata.author,
                    coverData = coverBytes
                )
            }
        } catch (e: Exception) {
            Log.e(_tag, "[!!!] ERRORE CRITICO durante il parsing di ${file.name}", e)
            fallback(file)
        } finally {
            Log.d(_tag, "== END PARSING: ${file.name} ==")
        }
    }

    private fun getOpfPath(zip: ZipFile): String? {
        val entry = zip.getEntry("META-INF/container.xml") ?: return null
        val parser = Xml.newPullParser()
        parser.setInput(zip.getInputStream(entry), "UTF-8")

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "rootfile") {
                val path = parser.getAttributeValue(null, "full-path")
                Log.v(_tag, "container.xml: trovato rootfile con path '$path'")
                return path
            }
            eventType = parser.next()
        }
        return null
    }

    private fun resolvePath(basePath: String, relativePath: String): String {
        val parent = File(basePath).parent ?: ""
        val resolved = if (parent.isEmpty()) relativePath else "$parent/$relativePath"
        // Log fondamentale per vedere se stiamo generando path tipo "OEBPS/\cover.jpg" o "OEBPS/cover.jpg"
        Log.v(_tag, "resolvePath: basePath='$basePath', relativePath='$relativePath' -> result='$resolved'")
        return resolved
    }

    private fun fallback(file: File): RawMetadata {
        Log.w(_tag, "FALLBACK attivato per: ${file.name}. Titolo impostato al nome file.")
        return RawMetadata(file.nameWithoutExtension, null)
    }
}