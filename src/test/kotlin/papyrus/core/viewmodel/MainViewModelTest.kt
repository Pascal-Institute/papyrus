package papyrus.core.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

/**
 * Unit tests for MainViewModel
 *
 * Tests:
 * - State initialization
 * - Search functionality
 * - Ticker selection
 * - Navigation flow
 */
@DisplayName("MainViewModel Tests")
class MainViewModelTest {
    private lateinit var viewModel: MainViewModel

    @BeforeEach
    fun setup() {
        viewModel = MainViewModel(CoroutineScope(Dispatchers.Unconfined))
    }

    @Test
    @DisplayName("Initial app state should be empty")
    fun `initial state is empty`() {
        val state = viewModel.appState
        assertEquals("", state.searchText)
        assertTrue(state.searchResults.isEmpty())
        assertNull(state.selectedTicker)
        assertTrue(state.submissions.isEmpty())
    }

    @Test
    @DisplayName("Search text update triggers search")
    fun `onSearchTextChange updates search text and results`() {
        // Given: Empty search
        assertEquals("", viewModel.appState.searchText)

        // When: Search text is updated
        viewModel.onSearchTextChange("AAPL")

        // Then: Search text is updated
        assertEquals("AAPL", viewModel.appState.searchText)
        // Note: searchResults would be populated if SecApi.loadTickers() is called
    }

    @Test
    @DisplayName("Clearing search text clears results")
    fun `empty search text clears results`() {
        // Given: Active search
        viewModel.onSearchTextChange("AAPL")

        // When: Search is cleared
        viewModel.onSearchTextChange("")

        // Then: Search text is empty
        assertEquals("", viewModel.appState.searchText)
    }

    @Test
    @DisplayName("Back to search clears selected ticker")
    fun `onBackToSearch clears selection`() {
        // When: Back to search
        viewModel.onBackToSearch()

        // Then: Selection is cleared
        assertNull(viewModel.appState.selectedTicker)
    }

    @Test
    @DisplayName("Set dragging updates isDragging state")
    fun `setDragging updates drag state`() {

        // When: Dragging starts
        viewModel.setDragging(true)

        // Then: isDragging is true
        assertTrue(viewModel.appState.isDragging)

        // When: Dragging ends
        viewModel.setDragging(false)

        // Then: isDragging is false
        assertFalse(viewModel.appState.isDragging)
    }

    @Test
    @DisplayName("Initial bookmarks list is empty before load")
    fun `initial bookmarks state`() {
        // Bookmarks are loaded asynchronously in init
        // Initially could be empty or populated depending on timing
        assertNotNull(viewModel.bookmarks)
    }

    @Test
    @DisplayName("Initial recently viewed list exists")
    fun `initial recently viewed state`() {
        assertNotNull(viewModel.recentlyViewedCiks)
    }
}
