package papyrus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import papyrus.core.model.TickerEntry
import papyrus.core.resource.AppStrings
import papyrus.ui.AppDimens
import papyrus.ui.EmptyState
import papyrus.ui.TickerCard

/**
 * Search Results List Component
 *
 * Displays ticker search results in a scrollable list.
 * Shows empty state when no results are available.
 *
 * Single Responsibility: Display ticker search results
 */
@Composable
fun SearchResultsList(
        results: List<TickerEntry>,
        onTickerSelected: (TickerEntry) -> Unit,
        showEmptyState: Boolean = true
) {
        if (results.isEmpty()) {
                if (showEmptyState) {
                        EmptyState(
                                icon = Icons.Outlined.Search,
                                title = AppStrings.SEARCH_EMPTY_TITLE,
                                description = AppStrings.SEARCH_EMPTY_DESCRIPTION
                        )
                }
        } else {
                LazyColumn(
                        modifier =
                                Modifier.fillMaxSize().padding(horizontal = AppDimens.PaddingSmall),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall)
                ) {
                        items(results) { ticker ->
                                TickerCard(ticker = ticker, onClick = { onTickerSelected(ticker) })
                        }
                }
        }
}
