package com.nosferatu.launcher.repository

import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.nosferatu.launcher.data.BookDao
import com.nosferatu.launcher.library.CoverManager
import com.nosferatu.launcher.library.LibraryConfig
import com.nosferatu.launcher.library.LibraryScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LibraryRepository(
    private val bookDao: BookDao,
    private val scanner: LibraryScanner,
    private val coverManager: CoverManager,
    private val libraryConfig: LibraryConfig
) {
    val allBooks = bookDao.getAllBooksFlow()
    private val _tag = "LibraryRepository"

    suspend fun syncLibrary() = withContext(Dispatchers.IO) {
        val rootDirectory = libraryConfig.getRootDirectory()

        if (!rootDirectory.exists() || !rootDirectory.isDirectory) {
            Log.e(_tag, "Root directory does not exist or is not a directory")
            return@withContext
        }

        val filesOnDisk = scanner.scanDirectory(rootDirectory)

        Log.d(_tag, "Root: ${rootDirectory.absolutePath} - Esiste: ${rootDirectory.exists()} - Leggibile: ${rootDirectory.canRead()}")

        // Add books/update books
        filesOnDisk.forEach { file ->
            Log.d(_tag, "Processing file: ${file.absolutePath}")
            val existingBook = bookDao.getBookByPath(file.absolutePath)

            if (existingBook == null || existingBook.lastModified != file.lastModified()) {
                val metadata = scanner.extractMetadata(file) ?: return@forEach

                val entity = metadata.toEntity(
                    lastModified = file.lastModified(),
                    coverPath = null
                )

                bookDao.insertBook(entity)

                val coverPath = coverManager.saveCover(entity.id, metadata.coverImage)

                bookDao.updateCoverPath(entity.id, coverPath)
            }
        }

        val pathsInDb = bookDao.getAllFilePaths()
        val pathsOnDisk = filesOnDisk.map { it.absolutePath }.toSet()
        val orphans = pathsInDb.filterNot { it in pathsOnDisk }

        if (orphans.isNotEmpty()) {
            bookDao.deleteByPaths(orphans)
        }
    }
}