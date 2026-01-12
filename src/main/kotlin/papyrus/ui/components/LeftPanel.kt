package papyrus.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import papyrus.core.model.BookmarkedTicker
import papyrus.core.model.FilingItem
import papyrus.core.model.TickerEntry
import papyrus.core.resource.AppStrings
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
                AppHeader(title = AppStrings.APP_TITLE, subtitle = AppStrings.APP_SUBTITLE)

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
                                placeholder = AppStrings.SEARCH_PLACEHOLDER,
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
                // Content
                Box(modifier = Modifier.fillMaxSize()) {
                        if (appState.searchText.isNotEmpty()) {
                                // Search Results (Active Search) - Overrides Detail View
                                SearchResultsList(
                                        results = appState.searchResults,
                                        onTickerSelected = onTickerSelected,
                                        showEmptyState = false
                                )
                        } else if (appState.selectedTicker != null) {
                                // Company Detail & Filings
                                CompanyFilingsPanel(
                                        ticker = appState.selectedTicker,
                                        filings = appState.submissions,
                                        currentAnalyzingFiling = appState.currentAnalyzingFiling,
                                        isBookmarked =
                                                BookmarkManager.isBookmarked(
                                                        appState.selectedTicker.cik
                                                ),
                                        onBackClick = onBackToSearch,
                                        onBookmarkClick = {
                                                onBookmarkClick(appState.selectedTicker)
                                        },
                                        onAnalyze = onAnalyze,
                                        onOpenInBrowser = onOpenInBrowser
                                )
                        } else {
                                // Home / Idle State
                                Column(modifier = Modifier.fillMaxSize()) {
                                        // Bookmark section
                                        if (bookmarks.isNotEmpty()) {
                                                BookmarkHorizontalList(
                                                        bookmarks = bookmarks,
                                                        onTickerClick = onBookmarkedTickerClick,
                                                        onRemove = onRemoveBookmark
                                                )
                                        }

                                        // Empty Search State
                                        SearchResultsList(
                                                results = emptyList(),
                                                onTickerSelected = onTickerSelected,
                                                showEmptyState = true
                                        )
                                }
                        }
                }
        }
}
