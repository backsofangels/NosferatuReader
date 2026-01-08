package com.nosferatu.launcher.library

import android.os.Environment
import com.nosferatu.launcher.data.Ebook
import com.nosferatu.launcher.parser.BookParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.File

class LibraryScanner(val parser: BookParser) {
    fun findSupportedFiles(): List<File> {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        return downloadsDir.listFiles()?.filter { file ->
            file.isFile && isSupported(file.extension)
        } ?: emptyList()
    }

    private fun isSupported(extension: String): Boolean {
        return listOf("epub", "pdf", "cbz").contains(extension.lowercase())
    }

    suspend fun quickScan(): List<Ebook> = withContext(Dispatchers.IO) {
        val root = File(LibraryConfig.rootPath)

        val files = root.listFiles()
            ?.filter { it.isFile }
            ?: return@withContext emptyList()

        files.map {
            async {
                try {
                    parser.parseMetadata(it)
                } catch (e: Exception) {
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }
}
