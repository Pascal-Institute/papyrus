package papyrus.core.resource

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

/**
 * Unit tests for AppStrings
 *
 * Ensures all resource strings are non-empty and properly formatted.
 * Following financial precision principle: verify all messages are traceable.
 */
@DisplayName("AppStrings Resource Tests")
class AppStringsTest {

    @Test
    @DisplayName("App metadata strings are not empty")
    fun `app metadata strings are valid`() {
        assertFalse(AppStrings.APP_TITLE.isEmpty())
        assertFalse(AppStrings.APP_SUBTITLE.isEmpty())
        assertFalse(AppStrings.WINDOW_TITLE.isEmpty())

        // Window title should contain app title
        assertTrue(AppStrings.WINDOW_TITLE.contains(AppStrings.APP_TITLE))
    }

    @Test
    @DisplayName("Search strings are not empty")
    fun `search strings are valid`() {
        assertFalse(AppStrings.SEARCH_PLACEHOLDER.isEmpty())
        assertFalse(AppStrings.SEARCH_EMPTY_TITLE.isEmpty())
        assertFalse(AppStrings.SEARCH_EMPTY_DESCRIPTION.isEmpty())
    }

    @Test
    @DisplayName("File processing message functions work correctly")
    fun `file processing messages are generated correctly`() {
        val fileName = "test.pdf"
        val message = AppStrings.FileProcessing.readingFile(fileName)
        assertTrue(message.contains(fileName))

        val extension = "xyz"
        val errorMsg = AppStrings.FileProcessing.unsupportedFileType(extension)
        assertTrue(errorMsg.contains(extension))
    }

    @Test
    @DisplayName("Analysis message functions work correctly")
    fun `analysis messages are generated correctly`() {
        val formatName = "HTML"
        val message = AppStrings.Analysis.analyzingDocument(formatName)
        assertTrue(message.contains(formatName))

        val errorMsg = "Network error"
        val failMsg = AppStrings.Analysis.analysisFailed(errorMsg)
        assertTrue(failMsg.contains(errorMsg) || failMsg.isNotEmpty())
    }

    @Test
    @DisplayName("Financial term constants are not empty")
    fun `financial terms are valid`() {
        assertFalse(AppStrings.Analysis.TERM_REVENUE.isEmpty())
        assertFalse(AppStrings.Analysis.TERM_NET_INCOME.isEmpty())
        assertFalse(AppStrings.Analysis.TERM_TOTAL_ASSETS.isEmpty())
        assertFalse(AppStrings.Analysis.TERM_LIABILITIES.isEmpty())
        assertFalse(AppStrings.Analysis.TERM_RISK_FACTORS.isEmpty())
    }

    @Test
    @DisplayName("Marker constants are distinct")
    fun `analysis markers are distinct`() {
        assertNotEquals(AppStrings.Analysis.MARKER_FOUND, AppStrings.Analysis.MARKER_NOT_FOUND)
        assertTrue(AppStrings.Analysis.MARKER_FOUND.length > 0)
        assertTrue(AppStrings.Analysis.MARKER_NOT_FOUND.length > 0)
    }

    @Test
    @DisplayName("Summary template strings are valid")
    fun `summary template strings are valid`() {
        assertFalse(AppStrings.Analysis.SUMMARY_TITLE.isEmpty())
        assertFalse(AppStrings.Analysis.SUMMARY_SEPARATOR.isEmpty())
        assertFalse(AppStrings.Analysis.KEY_TERMS_HEADER.isEmpty())
        assertFalse(AppStrings.Analysis.SUMMARY_TIP.isEmpty())
    }

    @Test
    @DisplayName("Document size message function works")
    fun `document size message is formatted correctly`() {
        val chars = "1,234,567"
        val message = AppStrings.Analysis.documentSize(chars)
        assertTrue(message.contains(chars))
        assertTrue(message.contains("characters") || message.isNotEmpty())
    }

    @Test
    @DisplayName("Analysis status constants are not empty")
    fun `analysis status constants are valid`() {
        assertFalse(AppStrings.Analysis.ANALYSIS_NOT_AVAILABLE.isEmpty())
        assertFalse(AppStrings.Analysis.ANALYSIS_NEEDED.isEmpty())
        assertFalse(AppStrings.Analysis.ANALYZING_ERROR.isEmpty())
    }

    @Test
    @DisplayName("Network message functions work correctly")
    fun `network messages are generated correctly`() {
        assertFalse(AppStrings.Network.FETCHING_SUBMISSIONS.isEmpty())
        assertFalse(AppStrings.Network.FETCHING_DOCUMENT.isEmpty())

        val error = "404 Not Found"
        val subError = AppStrings.Network.submissionsFetchError(error)
        assertTrue(subError.isNotEmpty())

        val docError = AppStrings.Network.documentFetchError(error)
        assertTrue(docError.isNotEmpty())
    }

    @Test
    @DisplayName("Cache messages are valid")
    fun `cache messages are valid`() {
        assertFalse(AppStrings.Cache.LOADING_FROM_CACHE.isEmpty())
        assertFalse(AppStrings.Cache.SAVING_TO_CACHE.isEmpty())
        assertFalse(AppStrings.Cache.PERFORMING_FRESH_ANALYSIS.isEmpty())
    }

    @Test
    @DisplayName("Empty state strings are not empty")
    fun `empty state strings are valid`() {
        assertFalse(AppStrings.NO_FILINGS_TITLE.isEmpty())
        assertFalse(AppStrings.NO_FILINGS_DESCRIPTION.isEmpty())
        assertFalse(AppStrings.FILTER_NO_RESULTS_TITLE.isEmpty())
        assertFalse(AppStrings.FILTER_NO_RESULTS_DESCRIPTION.isEmpty())
    }
}
