package com.nosferatu.launcher.reader

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class BookmarkStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("bookmarks_prefs", Context.MODE_PRIVATE)
    // New implementation: use a string-based book identifier (e.g. filename) to scope bookmarks.
    private fun sanitizeBookKey(raw: String): String = raw.replace(Regex("[^A-Za-z0-9_.-]"), "_")
    private fun keyFor(bookKey: String) = "book_${'$'}{sanitizeBookKey(bookKey)}_bookmarks"

    fun listBookmarksForBookKey(bookKey: String): List<Bookmark> {
        val key = keyFor(bookKey)
        val json = prefs.getString(key, "[]") ?: "[]"
        val arr = JSONArray(json)
        val out = mutableListOf<Bookmark>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val locator = obj.optString("locatorJson")
            val href = obj.optString("href")
            val progression = obj.optDouble("progression", 0.0)
            val title = if (obj.has("chapterTitle")) obj.optString("chapterTitle") else null
            val createdAt = obj.optLong("createdAt", 0L)
            out.add(Bookmark(locator, href, progression, title, createdAt))
        }
        return out
    }

    fun addBookmarkForBookKey(bookKey: String, bookmark: Bookmark) {
        val key = keyFor(bookKey)
        val arr = JSONArray(prefs.getString(key, "[]") ?: "[]")
        // avoid duplicates by locatorJson
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            if (obj.optString("locatorJson") == bookmark.locatorJson) return
        }
        val obj = JSONObject()
        obj.put("locatorJson", bookmark.locatorJson)
        obj.put("href", bookmark.href)
        obj.put("progression", bookmark.progression)
        obj.put("chapterTitle", bookmark.chapterTitle)
        obj.put("createdAt", bookmark.createdAt)
        // store book identifier for future compatibility/migration/debug
        obj.put("bookKey", bookKey)
        arr.put(obj)
        prefs.edit().putString(key, arr.toString()).apply()
    }

    fun removeBookmarkForBookKey(bookKey: String, locatorJson: String) {
        val key = keyFor(bookKey)
        val arr = JSONArray(prefs.getString(key, "[]") ?: "[]")
        val out = JSONArray()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            if (obj.optString("locatorJson") != locatorJson) out.put(obj)
        }
        prefs.edit().putString(key, out.toString()).apply()
    }

    fun isBookmarkedForBookKey(bookKey: String, locatorJson: String): Boolean {
        val key = keyFor(bookKey)
        val arr = JSONArray(prefs.getString(key, "[]") ?: "[]")
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            if (obj.optString("locatorJson") == locatorJson) return true
        }
        return false
    }

    // Backwards-compatible overloads that accept a numeric bookId (keeps existing callers working)
    private fun keyFor(bookId: Long) = keyFor(bookId.toString())
    fun listBookmarks(bookId: Long): List<Bookmark> = listBookmarksForBookKey(bookId.toString())
    fun addBookmark(bookId: Long, bookmark: Bookmark) = addBookmarkForBookKey(bookId.toString(), bookmark)
    fun removeBookmark(bookId: Long, locatorJson: String) = removeBookmarkForBookKey(bookId.toString(), locatorJson)
    fun isBookmarked(bookId: Long, locatorJson: String): Boolean = isBookmarkedForBookKey(bookId.toString(), locatorJson)
}
