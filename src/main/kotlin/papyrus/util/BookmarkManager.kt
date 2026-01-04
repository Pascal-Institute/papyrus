package papyrus.util

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import papyrus.core.model.BookmarkCollection
import papyrus.core.model.BookmarkData
import papyrus.core.model.BookmarkedTicker
import papyrus.core.model.TickerEntry

/** 북마크 관리자 - 티커 즐겨찾기 저장/로드 */
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

    /** 북마크 데이터 로드 */
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

    /** 북마크 데이터 저장 */
    private fun saveBookmarks(data: BookmarkData) {
        try {
            cachedData = data
            bookmarkFile.writeText(json.encodeToString(data))
        } catch (e: Exception) {
            System.err.println("Failed to save bookmarks: ${e.message}")
        }
    }

    /** 티커 북마크 추가 */
    fun addBookmark(
            ticker: TickerEntry,
            notes: String = "",
            tags: List<String> = emptyList()
    ): Boolean {
        val data = loadBookmarks()

        // 이미 북마크된 경우 스킵
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

    /** 티커 북마크 제거 */
    fun removeBookmark(cik: Int): Boolean {
        val data = loadBookmarks()
        val filtered = data.bookmarks.filter { it.cik != cik }

        if (filtered.size == data.bookmarks.size) return false

        val updatedData = data.copy(bookmarks = filtered)
        saveBookmarks(updatedData)
        return true
    }

    /** 북마크 여부 확인 */
    fun isBookmarked(cik: Int): Boolean {
        return loadBookmarks().bookmarks.any { it.cik == cik }
    }

    /** 모든 북마크 가져오기 */
    fun getAllBookmarks(): List<BookmarkedTicker> {
        return loadBookmarks().bookmarks
    }

    /** 북마크 노트 업데이트 */
    fun updateBookmarkNotes(cik: Int, notes: String): Boolean {
        val data = loadBookmarks()
        val updated = data.bookmarks.map { if (it.cik == cik) it.copy(notes = notes) else it }

        if (updated == data.bookmarks) return false

        saveBookmarks(data.copy(bookmarks = updated))
        return true
    }

    /** 북마크 태그 업데이트 */
    fun updateBookmarkTags(cik: Int, tags: List<String>): Boolean {
        val data = loadBookmarks()
        val updated = data.bookmarks.map { if (it.cik == cik) it.copy(tags = tags) else it }

        if (updated == data.bookmarks) return false

        saveBookmarks(data.copy(bookmarks = updated))
        return true
    }

    /** 최근 조회 기록 추가 */
    fun addToRecentlyViewed(cik: Int) {
        val data = loadBookmarks()
        val recent = (listOf(cik) + data.recentlyViewed.filter { it != cik }).take(10)

        // 북마크된 티커의 lastViewed 업데이트
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

    /** 최근 조회 기록 가져오기 */
    fun getRecentlyViewed(): List<Int> {
        return loadBookmarks().recentlyViewed
    }

    /** 컬렉션 생성 */
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

    /** 컬렉션에 티커 추가 */
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

    /** 컬렉션에서 티커 제거 */
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

    /** 컬렉션 삭제 */
    fun deleteCollection(name: String): Boolean {
        val data = loadBookmarks()
        val filtered = data.collections.filter { it.name != name }

        if (filtered.size == data.collections.size) return false

        saveBookmarks(data.copy(collections = filtered))
        return true
    }

    /** 모든 컬렉션 가져오기 */
    fun getAllCollections(): List<BookmarkCollection> {
        return loadBookmarks().collections
    }

    /** 태그별 북마크 검색 */
    fun getBookmarksByTag(tag: String): List<BookmarkedTicker> {
        return loadBookmarks().bookmarks.filter { tag in it.tags }
    }

    /** 북마크 검색 */
    fun searchBookmarks(query: String): List<BookmarkedTicker> {
        val q = query.lowercase()
        return loadBookmarks().bookmarks.filter {
            it.ticker.lowercase().contains(q) ||
                    it.companyName.lowercase().contains(q) ||
                    it.notes.lowercase().contains(q) ||
                    it.tags.any { tag -> tag.lowercase().contains(q) }
        }
    }

    /** 캐시 초기화 */
    fun clearCache() {
        cachedData = null
    }
}
