package com.nosferatu.launcher.reader

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

/**
 * EpubCacheManager: Filesystem-based extracted EPUB cache for reader optimization.
 *
 * Purpose:
 * - Extract EPUBs to a deterministic cache location on first open
 * - Reuse extracted content on subsequent opens (cache hit)
 * - Avoid repeated ZIP/container work on old devices
 * - Minimize ReaderActivity critical path startup work
 *
 * Cache Model:
 * - Location: cacheDir/readium_publications/<cacheKey>/
 * - Cache key: SHA256(sourcePath + lastModified + fileLength)
 * - Validation: directory exists + source metadata still matches
 * - No Room involvement: filesystem cache only
 *
 * Safety:
 * - Cache is reconstructible (not source of truth)
 * - Graceful fallback if cache unavailable
 * - Per-entry error handling (one EPUB failure doesn't block others)
 */
class EpubCacheManager(private val context: Context) {
    companion object {
        private const val TAG = "EpubCacheManager"
        private const val CACHE_DIR = "readium_publications"
    }

    /**
     * Get or create extracted EPUB path.
     *
     * @param sourceEpubFile Original EPUB file (must exist and be readable)
     * @return Extracted directory path (valid for Readium), or null if failed
     *
     * On success: returns path to extracted EPUB directory ready for Readium
     * On cache hit: returns path immediately (very fast)
     * On cache miss: extracts EPUB and returns path
     * On error: returns null (caller should fall back to direct open)
     */
    suspend fun getExtractedPath(sourceEpubFile: File): File? = withContext(Dispatchers.IO) {
        try {
            if (!sourceEpubFile.exists() || !sourceEpubFile.isFile) {
                Log.w(TAG, "getExtractedPath: source file doesn't exist or isn't a file: ${sourceEpubFile.absolutePath}")
                return@withContext null
            }

            // Phase 1: Derive cache key from source metadata
            val cacheKey = deriveCacheKey(sourceEpubFile)
            Log.d(TAG, "getExtractedPath: source=${sourceEpubFile.name} key=$cacheKey")

            // Phase 2: Check if valid cache exists
            val cacheDir = getCacheDirForKey(cacheKey)
            if (isValidCache(cacheDir, sourceEpubFile)) {
                Log.d(TAG, "getExtractedPath: CACHE HIT - $cacheKey")
                return@withContext cacheDir
            }

            Log.d(TAG, "getExtractedPath: CACHE MISS - extracting to $cacheKey")

            // Phase 3: Extract EPUB to cache
            val extractedPath = extractEpubToCache(sourceEpubFile, cacheKey)
            if (extractedPath == null) {
                Log.w(TAG, "getExtractedPath: extraction FAILED for ${sourceEpubFile.name}")
                return@withContext null
            }

            Log.d(TAG, "getExtractedPath: extraction SUCCESS - $cacheKey")
            extractedPath
        } catch (e: Exception) {
            Log.e(TAG, "getExtractedPath: FATAL error - ${e.message}", e)
            null
        }
    }

    /**
     * Derive deterministic cache key from source file metadata.
     *
     * Key components:
     * - sourcePath: ensures different copy locations have different caches
     * - lastModified: detects file changes
     * - fileLength: additional change detection
     *
     * Example: sourcePath=/storage/book.epub, lastModified=1712350000000, length=500000
     * Result: SHA256 hash of concatenated string
     */
    private fun deriveCacheKey(file: File): String {
        val keyInput = "${file.absolutePath}|${file.lastModified()}|${file.length()}"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(keyInput.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Get cache directory for a given key.
     */
    private fun getCacheDirForKey(cacheKey: String): File {
        return File(context.cacheDir, "$CACHE_DIR/$cacheKey")
    }

    /**
     * Check if cached directory is valid.
     *
     * Validity rules:
     * 1. Directory exists
     * 2. Source file still exists
     * 3. Source metadata (path + lastModified + length) matches the key
     */
    private fun isValidCache(cacheDir: File, sourceFile: File): Boolean {
        try {
            if (!cacheDir.exists() || !cacheDir.isDirectory) {
                Log.d(TAG, "isValidCache: cache dir doesn't exist: ${cacheDir.absolutePath}")
                return false
            }

            if (!sourceFile.exists()) {
                Log.d(TAG, "isValidCache: source file no longer exists")
                return false
            }

            // Re-derive key from current source metadata
            val currentKey = deriveCacheKey(sourceFile)
            val keyFromPath = cacheDir.name

            if (currentKey != keyFromPath) {
                Log.d(TAG, "isValidCache: key mismatch (current=$currentKey, cached=$keyFromPath)")
                return false
            }

            Log.d(TAG, "isValidCache: cache is VALID")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "isValidCache: error during validation - ${e.message}")
            return false
        }
    }

    /**
     * Extract EPUB to cache directory.
     *
     * Strategy:
     * 1. Ensure cache directory exists
     * 2. Open source EPUB as ZIP
     * 3. Extract all entries to cache directory
     * 4. Verify extraction succeeded
     *
     * @return Cache directory if successful, null if failed
     */
    private fun extractEpubToCache(sourceFile: File, cacheKey: String): File? {
        return try {
            val cacheDir = getCacheDirForKey(cacheKey)

            // Ensure clean extraction (delete partial cache if exists)
            if (cacheDir.exists()) {
                Log.d(TAG, "extractEpubToCache: removing existing cache dir (partial extraction?)")
                cacheDir.deleteRecursively()
            }
            cacheDir.mkdirs()

            Log.d(TAG, "extractEpubToCache: opening ZIP ${sourceFile.name}")

            // Extract all ZIP entries
            var extractedCount = 0
            ZipFile(sourceFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    val targetFile = File(cacheDir, entry.name)

                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            targetFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        extractedCount++
                    }
                }
            }

            Log.d(TAG, "extractEpubToCache: extracted $extractedCount files to ${cacheDir.absolutePath}")

            // Verify extraction
            if (!cacheDir.exists() || cacheDir.listFiles().isNullOrEmpty()) {
                Log.e(TAG, "extractEpubToCache: cache dir is empty after extraction!")
                return null
            }

            cacheDir
        } catch (e: Exception) {
            Log.e(TAG, "extractEpubToCache: extraction FAILED - ${e.message}", e)
            null
        }
    }

    /**
     * Optional: Clean up orphaned cache entries (not called automatically).
     *
     * Call this periodically or on app startup to remove old caches.
     */
    suspend fun cleanupOrphanedCaches() = withContext(Dispatchers.IO) {
        try {
            val cacheRootDir = File(context.cacheDir, CACHE_DIR)
            if (!cacheRootDir.exists()) return@withContext

            val cleanedCount = cacheRootDir.listFiles()?.count { 
                it.deleteRecursively()
            } ?: 0

            Log.d(TAG, "cleanupOrphanedCaches: removed $cleanedCount orphaned directories")
        } catch (e: Exception) {
            Log.w(TAG, "cleanupOrphanedCaches: cleanup failed - ${e.message}")
        }
    }
}
