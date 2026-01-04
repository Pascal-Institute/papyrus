package papyrus.core.model

import kotlinx.serialization.Serializable

/** 북마크된 티커 정보 */
@Serializable
data class BookmarkedTicker(
        val cik: Int,
        val ticker: String,
        val companyName: String,
        val addedAt: String, // ISO 8601 format
        val notes: String = "",
        val tags: List<String> = emptyList(),
        val lastViewed: String? = null
)

/** 북마크 컬렉션 (그룹) */
@Serializable
data class BookmarkCollection(
        val name: String,
        val description: String = "",
        val tickers: List<Int> = emptyList(), // CIK list
        val createdAt: String,
        val color: String = "#1A73E8" // Default blue
)

/** 전체 북마크 데이터 */
@Serializable
data class BookmarkData(
        val version: Int = 1,
        val bookmarks: List<BookmarkedTicker> = emptyList(),
        val collections: List<BookmarkCollection> = emptyList(),
        val recentlyViewed: List<Int> = emptyList() // CIK list, max 10
)
