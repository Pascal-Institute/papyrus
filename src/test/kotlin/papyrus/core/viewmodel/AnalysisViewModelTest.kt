package papyrus.core.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import papyrus.core.state.AnalysisState

/**
 * Unit tests for AnalysisViewModel
 *
 * Tests:
 * - State initialization
 * - Analysis state transitions
 * - Reset functionality
 * - Error handling
 */
@DisplayName("AnalysisViewModel Tests")
class AnalysisViewModelTest {
    private lateinit var viewModel: AnalysisViewModel

    @BeforeEach
    fun setup() {
        viewModel = AnalysisViewModel(CoroutineScope(Dispatchers.Unconfined))
    }

    @Test
    @DisplayName("Initial state should be Idle")
    fun `initial state is idle`() {
        assertEquals(AnalysisState.Idle, viewModel.analysisState)
        assertNull(viewModel.currentAnalyzingFiling)
    }

    @Test
    @DisplayName("Reset analysis should set state to Idle")
    fun `reset analysis returns to idle state`() {
        // Given: ViewModel in non-idle state
        viewModel.analysisState = AnalysisState.Loading("Testing")

        // When: Reset is called
        viewModel.resetAnalysis()

        // Then: State returns to Idle
        assertEquals(AnalysisState.Idle, viewModel.analysisState)
        assertNull(viewModel.currentAnalyzingFiling)
    }

    @Test
    @DisplayName("Analysis state can be set directly")
    fun `analysis state can be updated`() {
        // Given: Initial idle state
        assertEquals(AnalysisState.Idle, viewModel.analysisState)

        // When: State is updated to Loading
        val loadingMessage = "Analyzing document..."
        viewModel.analysisState = AnalysisState.Loading(loadingMessage)

        // Then: State reflects the update
        assertTrue(viewModel.analysisState is AnalysisState.Loading)
        assertEquals(loadingMessage, (viewModel.analysisState as AnalysisState.Loading).message)
    }

    @Test
    @DisplayName("Error state preserves error message")
    fun `error state contains message`() {
        // Given: Error message
        val errorMessage = "Failed to analyze document"

        // When: Error state is set
        viewModel.analysisState = AnalysisState.Error(errorMessage, null)

        // Then: Error message is preserved
        assertTrue(viewModel.analysisState is AnalysisState.Error)
        assertEquals(errorMessage, (viewModel.analysisState as AnalysisState.Error).message)
    }

    @Test
    @DisplayName("CurrentAnalyzingFiling tracks active filing")
    fun `currentAnalyzingFiling can be accessed`() {

        // When: State is verified (read-only access)
        val currentFiling = viewModel.currentAnalyzingFiling

        // Then: Value is accessible
        assertNull(currentFiling)
    }
}
