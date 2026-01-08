package com.nosferatu.launcher

import com.nosferatu.launcher.library.LibraryScanner
import com.nosferatu.launcher.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LibraryManager(
    private val scanner: LibraryScanner,
    private val repository: LibraryRepository
) {
    suspend fun syncLibrary() = withContext(Dispatchers.IO) {
        val filesOnDisk = scanner.findSupportedFiles()

        filesOnDisk.forEach { file ->
            val lastMod = file.lastModified()

            if (repository.needsUpdate(file.absolutePath, lastMod)) {
                val ebook = scanner.parser.parseMetadata(file)
                repository.upsertBook(ebook.toEntity(lastMod))
            }
        }

        val pathsOnDisk = filesOnDisk.map { it.absolutePath }.toSet()
        val pathsInDb = repository.getAllFilePaths()

        val orphans = pathsInDb.filterNot { it in pathsOnDisk }
        if (orphans.isNotEmpty()) {
            repository.deleteByFilePaths(orphans)
        }
    }
}