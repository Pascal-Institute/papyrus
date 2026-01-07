package papyrus.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import papyrus.core.model.BookmarkedTicker
import papyrus.core.model.FilingItem
import papyrus.core.model.TickerEntry
import papyrus.core.state.AppState
import papyrus.ui.*
import papyrus.util.data.BookmarkManager

/**
 * Left Panel Component
 *
 * Main navigation panel containing:
 * - App header
 * - Search box
 * - Bookmark list (when search is empty)
 * - Search results or company filings panel
 *
 * Single Responsibility: Left sidebar navigation and content display
 */
@Composable
fun LeftPanel(
        appState: AppState,
        bookmarks: List<BookmarkedTicker>,
        onSearchTextChange: (String) -> Unit,
        onTickerSelected: (TickerEntry) -> Unit,
        onBookmarkClick: (TickerEntry) -> Unit,
        onBookmarkedTickerClick: (Int) -> Unit,
        onRemoveBookmark: (Int) -> Unit,
        onBackToSearch: () -> Unit,
        onAnalyze: (FilingItem, FileFormatType) -> Unit,
        onOpenInBrowser: (FilingItem) -> Unit
) {
        Column(
                modifier =
                        Modifier.width(AppDimens.SidebarWidth)
                                .fillMaxHeight()
                                .background(AppColors.Surface)
        ) {
                // App Header
                AppHeader(
                        title = "Papyrus",
                        subtitle = "SEC Financial Analyzer"
                )

                // Search Box
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .background(AppColors.Background)
                                        .padding(AppDimens.PaddingMedium)
                ) {
                        SearchBox(
                                value = appState.searchText,
                                onValueChange = onSearchTextChange,
                                placeholder = "Search company or ticker...",
                                isLoading = appState.isLoading && appState.selectedTicker == null
                        )
                }

                // Loading indicator
                AnimatedVisibility(visible = appState.isLoading) {
                        LinearProgressIndicator(
                                modifier =
                                        Modifier.fillMaxWidth().height(AppDimens.ProgressBarHeight),
                                color = AppColors.Primary,
                                backgroundColor = AppColors.PrimaryLight
                        )
                }

                // Content
                if (appState.selectedTicker == null) {
                        // Bookmarks and Search Results
                        Column(modifier = Modifier.fillMaxSize()) {
                                // Bookmark section (only shown when search is empty)
                                if (appState.searchText.isEmpty() && bookmarks.isNotEmpty()) {
                                        BookmarkHorizontalList(
                                                bookmarks = bookmarks,
                                                onTickerClick = onBookmarkedTickerClick,
                                                onRemove = onRemoveBookmark
                                        )
                                }

                                // Search Results
                                SearchResultsList(
                                        results = appState.searchResults,
                                        onTickerSelected = onTickerSelected,
                                        showEmptyState =
                                                appState.searchText.isEmpty() && bookmarks.isEmpty()
                                )
                        }
                } else {
                        // Company Detail & Filings
                        CompanyFilingsPanel(
                                ticker = appState.selectedTicker,
                                filings = appState.submissions,
                                currentAnalyzingFiling = appState.currentAnalyzingFiling,
                                isBookmarked =
                                        BookmarkManager.isBookmarked(appState.selectedTicker.cik),
                                onBackClick = onBackToSearch,
                                onBookmarkClick = { onBookmarkClick(appState.selectedTicker) },
                                onAnalyze = onAnalyze,
                                onOpenInBrowser = onOpenInBrowser
                        )
                }
        }
}
