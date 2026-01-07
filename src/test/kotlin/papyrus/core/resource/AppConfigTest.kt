package papyrus.core.resource

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.math.RoundingMode

/**
 * Unit tests for AppConfig
 *
 * Validates configuration values are within acceptable ranges.
 * Following financial precision principle: verify all settings are explicit and documented.
 */
@DisplayName("AppConfig Resource Tests")
class AppConfigTest {

    @Test
    @DisplayName("Network timeouts are positive")
    fun `network timeouts are valid`() {
        assertTrue(AppConfig.Network.REQUEST_TIMEOUT_MS > 0)
        assertTrue(AppConfig.Network.CONNECT_TIMEOUT_MS > 0)
        assertTrue(AppConfig.Network.RETRY_DELAY_MS > 0)
        assertTrue(AppConfig.Network.MAX_RETRY_ATTEMPTS > 0)
    }

    @Test
    @DisplayName("SEC API rate limit settings are reasonable")
    fun `sec rate limits are reasonable`() {
        assertTrue(AppConfig.Network.SEC_RATE_LIMIT_REQUESTS_PER_SECOND > 0)
        assertTrue(AppConfig.Network.SEC_RATE_LIMIT_REQUESTS_PER_SECOND <= 100)
        assertTrue(AppConfig.Network.SEC_REQUEST_DELAY_MS > 0)
    }

    @Test
    @DisplayName("Cache settings are valid")
    fun `cache settings are valid`() {
        assertTrue(AppConfig.Cache.CACHE_EXPIRY_DAYS > 0)
        assertTrue(AppConfig.Cache.MAX_CACHE_SIZE_MB > 0)
    }

    @Test
    @DisplayName("Financial precision settings are appropriate")
    fun `financial precision is properly configured`() {
        // Calculation precision should be >= 6 for financial accuracy
        assertTrue(AppConfig.Financial.CALCULATION_PRECISION >= 6)

        // Rounding mode should be HALF_UP for financial calculations
        assertEquals(RoundingMode.HALF_UP, AppConfig.Financial.ROUNDING_MODE)

        // Display precisions should be reasonable
        assertTrue(AppConfig.Financial.CURRENCY_DISPLAY_PRECISION >= 0)
        assertTrue(AppConfig.Financial.RATIO_DISPLAY_PRECISION >= 0)
        assertTrue(AppConfig.Financial.PERCENTAGE_DISPLAY_PRECISION >= 0)

        // Display precisions should not be excessive
        assertTrue(AppConfig.Financial.CURRENCY_DISPLAY_PRECISION <= 10)
        assertTrue(AppConfig.Financial.RATIO_DISPLAY_PRECISION <= 10)
        assertTrue(AppConfig.Financial.PERCENTAGE_DISPLAY_PRECISION <= 10)
    }

    @Test
    @DisplayName("Anomaly detection thresholds are reasonable")
    fun `anomaly thresholds are reasonable`() {
        assertTrue(AppConfig.Financial.RATIO_ANOMALY_THRESHOLD > 1.0)
        assertTrue(AppConfig.Financial.MAX_PLAUSIBLE_MARGIN_PERCENT > 0)
        assertTrue(AppConfig.Financial.MAX_PLAUSIBLE_MARGIN_PERCENT <= 200.0)
    }

    @Test
    @DisplayName("File processing limits are appropriate")
    fun `file processing limits are appropriate`() {
        assertTrue(AppConfig.FileProcessing.MAX_FILE_SIZE_MB > 0)
        assertTrue(AppConfig.FileProcessing.MAX_CONTENT_LENGTH > 0)
        assertTrue(AppConfig.FileProcessing.SUPPORTED_EXTENSIONS.isNotEmpty())

        // Should support common financial document formats
        assertTrue(AppConfig.FileProcessing.SUPPORTED_EXTENSIONS.contains("pdf"))
        assertTrue(AppConfig.FileProcessing.SUPPORTED_EXTENSIONS.contains("html"))
    }

    @Test
    @DisplayName("UI configuration values are sensible")
    fun `ui config values are sensible`() {
        assertTrue(AppConfig.UI.DEFAULT_SEARCH_RESULTS_LIMIT > 0)
        assertTrue(AppConfig.UI.MAX_BOOKMARKS > 0)
        assertTrue(AppConfig.UI.RECENT_TICKERS_LIMIT > 0)
        assertTrue(AppConfig.UI.SEARCH_DEBOUNCE_MS >= 0)

        // Default report types should not be empty
        assertTrue(AppConfig.UI.DEFAULT_REPORT_TYPES.isNotEmpty())
    }

    @Test
    @DisplayName("Analysis configuration is valid")
    fun `analysis config is valid`() {
        assertTrue(AppConfig.Analysis.MAX_METRICS_PER_FILING > 0)
        assertTrue(AppConfig.Analysis.IXBRL_MIN_CONFIDENCE >= 0.0)
        assertTrue(AppConfig.Analysis.IXBRL_MIN_CONFIDENCE <= 1.0)
    }

    @Test
    @DisplayName("Validation thresholds are logical")
    fun `validation thresholds are logical`() {
        // CIK validation
        assertTrue(AppConfig.Validation.MIN_CIK_LENGTH > 0)
        assertTrue(AppConfig.Validation.MAX_CIK_LENGTH >= AppConfig.Validation.MIN_CIK_LENGTH)

        // Ticker validation
        assertTrue(AppConfig.Validation.MIN_TICKER_LENGTH > 0)
        assertTrue(AppConfig.Validation.MAX_TICKER_LENGTH >= AppConfig.Validation.MIN_TICKER_LENGTH)
    }

    @Test
    @DisplayName("Financial precision constants align with AGENTS.md principles")
    fun `financial precision follows agents principles`() {
        // From AGENTS.md: "Precision for financial calculations (6 decimal places)"
        assertEquals(6, AppConfig.Financial.CALCULATION_PRECISION)

        // From AGENTS.md: "Use BigDecimal with RoundingMode.HALF_UP"
        assertEquals(RoundingMode.HALF_UP, AppConfig.Financial.ROUNDING_MODE)

        // Currency should show 2 decimals (standard practice)
        assertEquals(2, AppConfig.Financial.CURRENCY_DISPLAY_PRECISION)
    }
}
