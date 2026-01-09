package papyrus.core.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import papyrus.core.model.BookmarkedTicker
import papyrus.core.model.FilingItem
import papyrus.core.model.TickerEntry
import papyrus.core.secApiClient
import papyrus.core.state.AnalysisState
import papyrus.core.state.AppState
import papyrus.util.data.BookmarkManager

/**
 * MainViewModel - handles business logic for the main application
 *
 * Responsibilities:
 * - Ticker search and selection
 * - Bookmark management
 * - Recent view history
 * - Filing submissions fetching
 *
 * Follows MVVM pattern to separate UI logic from business logic. All network calls and state
 * updates are managed here.
 */
class MainViewModel(private val scope: CoroutineScope) {
    /** Main application state - exposed as mutable for Compose */
    var appState by mutableStateOf(AppState())
        private set

    /** Bookmark list - separate from app state for independent updates */
    var bookmarks by mutableStateOf<List<BookmarkedTicker>>(emptyList())
        private set

    /** Recently viewed CIKs - tracks user's browsing history */
    var recentlyViewedCiks by mutableStateOf<List<Int>>(emptyList())
        private set

    init {
        loadInitialData()
    }

    /** Load tickers and bookmarks on startup */
    private fun loadInitialData() {
        scope.launch {
            appState = appState.copy(isLoading = true)
            secApiClient.loadTickers()
            bookmarks = BookmarkManager.getAllBookmarks()
            recentlyViewedCiks = BookmarkManager.getRecentlyViewed()
            appState = appState.copy(isLoading = false)
        }
    }

    /** Update search text and perform search */
    fun onSearchTextChange(query: String) {
        appState =
                appState.copy(searchText = query, searchResults = secApiClient.searchTicker(query))
    }

    /** Handle ticker selection from search results */
    fun onTickerSelected(ticker: TickerEntry) {
        scope.launch {
            // Add to recent view history
            BookmarkManager.addToRecentlyViewed(ticker.cik)
            recentlyViewedCiks = BookmarkManager.getRecentlyViewed()

            appState =
                    appState.copy(
                            selectedTicker = ticker,
                            searchText = "",
                            searchResults = emptyList(),
                            isLoading = true,
                            analysisState = AnalysisState.Idle
                    )

            // Fetch submissions
            val filings = fetchSubmissions(ticker.cik)

            appState = appState.copy(submissions = filings, isLoading = false)
        }
    }

    /** Handle bookmarked ticker selection (by CIK) */
    fun onBookmarkedTickerClick(cik: Int) {
        scope.launch {
            // Find ticker info by CIK
            val ticker =
                    secApiClient.searchTicker("").find { it.cik == cik }
                            ?: bookmarks.find { it.cik == cik }?.let {
                                TickerEntry(it.cik, it.ticker, it.companyName)
                            }

            if (ticker != null) {
                BookmarkManager.addToRecentlyViewed(cik)
                recentlyViewedCiks = BookmarkManager.getRecentlyViewed()

                appState =
                        appState.copy(
                                selectedTicker = ticker,
                                searchText = "",
                                searchResults = emptyList(),
                                isLoading = true,
                                analysisState = AnalysisState.Idle
                        )

                // Fetch submissions
                val filings = fetchSubmissions(cik)

                appState = appState.copy(submissions = filings, isLoading = false)
            }
        }
    }

    /** Toggle bookmark for a ticker */
    fun onBookmarkClick(ticker: TickerEntry) {
        if (BookmarkManager.isBookmarked(ticker.cik)) {
            BookmarkManager.removeBookmark(ticker.cik)
        } else {
            BookmarkManager.addBookmark(ticker)
        }
        bookmarks = BookmarkManager.getAllBookmarks()
    }

    /** Remove bookmark by CIK */
    fun onRemoveBookmark(cik: Int) {
        BookmarkManager.removeBookmark(cik)
        bookmarks = BookmarkManager.getAllBookmarks()
    }

    /** Navigate back to search view */
    fun onBackToSearch() {
        appState =
                appState.copy(
                        selectedTicker = null,
                        submissions = emptyList(),
                        analysisState = AnalysisState.Idle
                )
    }

    /** Update drag state for file drop */
    fun setDragging(isDragging: Boolean) {
        appState = appState.copy(isDragging = isDragging)
    }

    /** Fetch filings for a CIK */
    private suspend fun fetchSubmissions(cik: Int): List<FilingItem> {
        val submissions = secApiClient.getSubmissions(cik)
        return submissions?.filings?.recent?.let { secApiClient.transformFilings(it) }
                ?: emptyList()
    }

    /** Refresh bookmarks from storage */
    fun refreshBookmarks() {
        bookmarks = BookmarkManager.getAllBookmarks()
        recentlyViewedCiks = BookmarkManager.getRecentlyViewed()
    }
}
