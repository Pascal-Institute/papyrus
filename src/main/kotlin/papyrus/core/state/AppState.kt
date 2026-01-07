package papyrus.core.state

import papyrus.core.model.FilingItem
import papyrus.core.model.TickerEntry

/**
 * Application State - represents the entire UI state of the app
 *
 * Following MVVM principles, this data class consolidates all application state
 * to enable predictable state management and easier testing.
 */
data class AppState(
        val searchText: String = "",
        val searchResults: List<TickerEntry> = emptyList(),
        val selectedTicker: TickerEntry? = null,
        val submissions: List<FilingItem> = emptyList(),
        val isLoading: Boolean = false,
        val isDragging: Boolean = false,
        val analysisState: AnalysisState = AnalysisState.Idle,
        val currentAnalyzingFiling: String? = null
)
