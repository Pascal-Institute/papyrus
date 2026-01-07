package papyrus.util.data

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import papyrus.core.model.BookmarkCollection
import papyrus.core.model.BookmarkData
import papyrus.core.model.BookmarkedTicker
import papyrus.core.model.TickerEntry

/** Bookmark manager - save/load ticker favorites */
object BookmarkManager {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val bookmarkFile: File by lazy {
        val appDir = File(System.getProperty("user.home"), ".papyrus")
        if (!appDir.exists()) appDir.mkdirs()
        File(appDir, "bookmarks.json")
    }

    private var cachedData: BookmarkData? = null

    /** Load bookmark data */
    fun loadBookmarks(): BookmarkData {
        cachedData?.let {
            return it
        }

        return try {
            if (bookmarkFile.exists()) {
                val content = bookmarkFile.readText()
                json.decodeFromString<BookmarkData>(content).also { cachedData = it }
            } else {
                BookmarkData().also { cachedData = it }
            }
        } catch (e: Exception) {
            System.err.println("Failed to load bookmarks: ${e.message}")
            BookmarkData().also { cachedData = it }
        }
    }

    /** Save bookmark data */
    private fun saveBookmarks(data: BookmarkData) {
        try {
            cachedData = data
            bookmarkFile.writeText(json.encodeToString(data))
        } catch (e: Exception) {
            System.err.println("Failed to save bookmarks: ${e.message}")
        }
    }

    /** Add ticker bookmark */
    fun addBookmark(
            ticker: TickerEntry,
            notes: String = "",
            tags: List<String> = emptyList()
    ): Boolean {
        val data = loadBookmarks()

        // Skip if already bookmarked
        if (data.bookmarks.any { it.cik == ticker.cik }) {
            return false
        }

        val newBookmark =
                BookmarkedTicker(
                        cik = ticker.cik,
                        ticker = ticker.ticker,
                        companyName = ticker.title,
                        addedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        notes = notes,
                        tags = tags
                )

        val updatedData = data.copy(bookmarks = data.bookmarks + newBookmark)
        saveBookmarks(updatedData)
        return true
    }

    /** Remove ticker bookmark */
    fun removeBookmark(cik: Int): Boolean {
        val data = loadBookmarks()
        val filtered = data.bookmarks.filter { it.cik != cik }

        if (filtered.size == data.bookmarks.size) return false

        val updatedData = data.copy(bookmarks = filtered)
        saveBookmarks(updatedData)
        return true
    }

    /** Check if bookmarked */
    fun isBookmarked(cik: Int): Boolean {
        return loadBookmarks().bookmarks.any { it.cik == cik }
    }

    /** Get all bookmarks */
    fun getAllBookmarks(): List<BookmarkedTicker> {
        return loadBookmarks().bookmarks
    }

    /** Update bookmark notes */
    fun updateBookmarkNotes(cik: Int, notes: String): Boolean {
        val data = loadBookmarks()
        val updated = data.bookmarks.map { if (it.cik == cik) it.copy(notes = notes) else it }

        if (updated == data.bookmarks) return false

        saveBookmarks(data.copy(bookmarks = updated))
        return true
    }

    /** Update bookmark tags */
    fun updateBookmarkTags(cik: Int, tags: List<String>): Boolean {
        val data = loadBookmarks()
        val updated = data.bookmarks.map { if (it.cik == cik) it.copy(tags = tags) else it }

        if (updated == data.bookmarks) return false

        saveBookmarks(data.copy(bookmarks = updated))
        return true
    }

    /** Add to recently viewed history */
    fun addToRecentlyViewed(cik: Int) {
        val data = loadBookmarks()
        val recent = (listOf(cik) + data.recentlyViewed.filter { it != cik }).take(10)

        // Update lastViewed for bookmarked ticker
        val updatedBookmarks =
                data.bookmarks.map {
                    if (it.cik == cik) {
                        it.copy(
                                lastViewed =
                                        LocalDateTime.now()
                                                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        )
                    } else it
                }

        saveBookmarks(data.copy(recentlyViewed = recent, bookmarks = updatedBookmarks))
    }

    /** Get recently viewed history */
    fun getRecentlyViewed(): List<Int> {
        return loadBookmarks().recentlyViewed
    }

    /** Create collection */
    fun createCollection(
            name: String,
            description: String = "",
            color: String = "#1A73E8"
    ): Boolean {
        val data = loadBookmarks()

        if (data.collections.any { it.name == name }) return false

        val newCollection =
                BookmarkCollection(
                        name = name,
                        description = description,
                        createdAt =
                                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        color = color
                )

        saveBookmarks(data.copy(collections = data.collections + newCollection))
        return true
    }

    /** Add ticker to collection */
    fun addToCollection(collectionName: String, cik: Int): Boolean {
        val data = loadBookmarks()
        val updated =
                data.collections.map { collection ->
                    if (collection.name == collectionName && cik !in collection.tickers) {
                        collection.copy(tickers = collection.tickers + cik)
                    } else collection
                }

        if (updated == data.collections) return false

        saveBookmarks(data.copy(collections = updated))
        return true
    }

    /** Remove ticker from collection */
    fun removeFromCollection(collectionName: String, cik: Int): Boolean {
        val data = loadBookmarks()
        val updated =
                data.collections.map { collection ->
                    if (collection.name == collectionName) {
                        collection.copy(tickers = collection.tickers.filter { it != cik })
                    } else collection
                }

        if (updated == data.collections) return false

        saveBookmarks(data.copy(collections = updated))
        return true
    }

    /** Delete collection */
    fun deleteCollection(name: String): Boolean {
        val data = loadBookmarks()
        val filtered = data.collections.filter { it.name != name }

        if (filtered.size == data.collections.size) return false

        saveBookmarks(data.copy(collections = filtered))
        return true
    }

    /** Get all collections */
    fun getAllCollections(): List<BookmarkCollection> {
        return loadBookmarks().collections
    }

    /** Search bookmarks by tag */
    fun getBookmarksByTag(tag: String): List<BookmarkedTicker> {
        return loadBookmarks().bookmarks.filter { tag in it.tags }
    }

    /** Search bookmarks */
    fun searchBookmarks(query: String): List<BookmarkedTicker> {
        val q = query.lowercase()
        return loadBookmarks().bookmarks.filter {
            it.ticker.lowercase().contains(q) ||
                    it.companyName.lowercase().contains(q) ||
                    it.notes.lowercase().contains(q) ||
                    it.tags.any { tag -> tag.lowercase().contains(q) }
        }
    }

    /** Clear cache */
    fun clearCache() {
        cachedData = null
    }
}
