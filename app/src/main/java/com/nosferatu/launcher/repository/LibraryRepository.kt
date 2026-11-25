package com.nosferatu.launcher.repository

import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.nosferatu.launcher.data.BookDao
import com.nosferatu.launcher.data.TocDao
import com.nosferatu.launcher.data.TocEntryEntity
import com.nosferatu.launcher.parser.TocParser
import com.nosferatu.launcher.library.CoverManager
import com.nosferatu.launcher.library.LibraryConfig
import com.nosferatu.launcher.library.LibraryScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LibraryRepository(
    private val bookDao: BookDao,
    private val scanner: LibraryScanner,
    private val coverManager: CoverManager,
    private val libraryConfig: LibraryConfig,
    private val tocDao: TocDao? = null
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

            // Load existing book record (if any)
            val existingBook = try {
                bookDao.getBookByPath(file.absolutePath)
            } catch (e: Exception) {
                Log.w(_tag, "Failed to read existing book for path ${file.absolutePath}: ${e.message}")
                null
            }

            // If book already imported and TOC was previously imported and file unchanged, skip work
            val wasTocImported = try { bookDao.isTocImported(file.absolutePath) } catch (e: Exception) { null }
            if (existingBook != null && wasTocImported == true && existingBook.lastModified == file.lastModified()) {
                Log.d(_tag, "Skipping file (TOC already imported and unchanged): ${file.absolutePath} (bookId=${existingBook.id})")
                return@forEach
            }

            val metadata = scanner.extractMetadata(file)
            if (metadata == null) {
                Log.w(_tag, "Failed to extract metadata for file: ${file.absolutePath}, skipping")
                return@forEach
            }

            val coverPath = coverManager.saveCover(metadata.title, metadata.coverImage)

            val entity = metadata.toEntity(
                lastModified = file.lastModified(),
                coverPath = coverPath
            )

            // Insert or replace the book entry
            try {
                bookDao.insertBook(entity)
                Log.d(_tag, "Inserted/updated book: ${entity.title} path=${entity.filePath}")
            } catch (e: Exception) {
                Log.w(_tag, "Insert/update failed for ${file.absolutePath}: ${e.message}")
            }

            // If a TocDao is available, parse TOC and persist entries for this book (unless flagged)
            if (tocDao != null) {
                try {
                    val stored = bookDao.getBookByPath(file.absolutePath)
                    if (stored == null) {
                        Log.w(_tag, "Unable to determine book id after insert for ${file.absolutePath}, skipping TOC persistence")
                    } else if (stored.tocImported && stored.lastModified == file.lastModified()) {
                        Log.d(_tag, "TOC already imported for bookId=${stored.id}, skipping TOC parsing")
                    } else {
                        Log.d(_tag, "Parsing TOC for file: ${file.absolutePath}")
                        val tocItems = TocParser().parse(file)
                        Log.d(_tag, "TocParser returned ${tocItems.size} items for file: ${file.absolutePath}")

                        val entries = tocItems.map { item ->
                            TocEntryEntity(
                                bookId = stored.id,
                                title = item.title,
                                href = item.href,
                                locatorJson = item.locatorJson,
                                position = item.position
                            )
                        }

                        Log.d(_tag, "Persisting ${entries.size} TOC entries for bookId=${stored.id}")
                        tocDao.replaceForBook(stored.id, entries)
                        Log.d(_tag, "Persisted ${entries.size} TOC entries for bookId=${stored.id}")

                        // Mark the book as having had its TOC imported to avoid reprocessing
                        try {
                            val updated = stored.copy(tocImported = true)
                            bookDao.updateBook(updated)
                            Log.d(_tag, "Marked book id=${stored.id} tocImported=true")
                        } catch (e: Exception) {
                            Log.w(_tag, "Unable to update tocImported flag for bookId=${stored.id}: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(_tag, "Error parsing/persisting TOC for file: ${file.absolutePath}", e)
                }
            }
        }

        val pathsInDb = bookDao.getAllFilePaths()
        val pathsOnDisk = filesOnDisk.map { it.absolutePath }.toSet()
        val orphans = pathsInDb.filterNot { it in pathsOnDisk }

        if (orphans.isNotEmpty()) {
            bookDao.deleteByPaths(orphans)
        }
    }

    suspend fun updateBookPosition(bookId: Long, location: String, progression: Double) {
        bookDao.updateReadingProgress(bookId, location, progression)
    }

    suspend fun deleteBooks() {
        Log.d(_tag, "Deleting all books")
        bookDao.deleteBooks()
    }

    suspend fun getTocEntriesForBook(bookId: Long): List<TocEntryEntity> = withContext(Dispatchers.IO) {
        try {
            val entries = tocDao?.getForBook(bookId) ?: emptyList()
            Log.d(_tag, "Loaded ${entries.size} TOC entries from DB for bookId=$bookId")
            entries
        } catch (e: Exception) {
            Log.w(_tag, "Error loading TOC entries for bookId=$bookId: ${e.message}")
            emptyList()
        }
    }
}