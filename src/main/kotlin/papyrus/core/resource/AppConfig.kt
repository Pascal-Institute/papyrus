package papyrus.core.resource

import java.math.RoundingMode

/**
 * Centralized Application Configuration
 *
 * All configuration constants in one place for easy maintenance.
 * Following financial precision principle: explicit, documented settings.
 *
 * Categories:
 * - Network timeouts and retry settings
 * - Cache settings
 * - Financial calculation precision
 * - UI limits and defaults
 * - File processing limits
 */
object AppConfig {
    // =========================
    // Network Configuration
    // =========================
    object Network {
        const val REQUEST_TIMEOUT_MS = 30_000L
        const val CONNECT_TIMEOUT_MS = 15_000L
        const val MAX_RETRY_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 1_000L

        // SEC API rate limiting (as per SEC guidelines)
        const val SEC_RATE_LIMIT_REQUESTS_PER_SECOND = 10
        const val SEC_REQUEST_DELAY_MS = 100L
    }

    // =========================
    // Cache Configuration
    // =========================
    object Cache {
        const val ANALYSIS_CACHE_ENABLED = true
        const val CACHE_EXPIRY_DAYS = 7
        const val MAX_CACHE_SIZE_MB = 100
    }

    // =========================
    // Financial Precision
    // =========================
    object Financial {
        /** Precision for financial calculations (6 decimal places) */
        const val CALCULATION_PRECISION = 6

        /** Rounding mode for all financial calculations */
        val ROUNDING_MODE = RoundingMode.HALF_UP

        /** Display precision for currency (2 decimal places) */
        const val CURRENCY_DISPLAY_PRECISION = 2

        /** Display precision for ratios (4 decimal places) */
        const val RATIO_DISPLAY_PRECISION = 4

        /** Display precision for percentages (2 decimal places) */
        const val PERCENTAGE_DISPLAY_PRECISION = 2

        /** Threshold for detecting parsing errors in ratios */
        const val RATIO_ANOMALY_THRESHOLD = 100.0 // 100x or 10000%

        /** Maximum plausible margin percentage */
        const val MAX_PLAUSIBLE_MARGIN_PERCENT = 150.0
    }

    // =========================
    // File Processing
    // =========================
    object FileProcessing {
        /** Supported file extensions */
        val SUPPORTED_EXTENSIONS = setOf("pdf", "html", "htm", "txt")

        /** Maximum file size in MB */
        const val MAX_FILE_SIZE_MB = 50

        /** Maximum content length for text extraction */
        const val MAX_CONTENT_LENGTH = 10_000_000 // 10 million characters
    }

    // =========================
    // UI Configuration
    // =========================
    object UI {
        /** Default number of search results to display */
        const val DEFAULT_SEARCH_RESULTS_LIMIT = 50

        /** Default report type filter (10-Q for quarterly insights) */
        val DEFAULT_REPORT_TYPES = setOf("10-Q")

        /** Maximum bookmarks allowed */
        const val MAX_BOOKMARKS = 50

        /** Recent viewed tickers to remember */
        const val RECENT_TICKERS_LIMIT = 10

        /** Debounce delay for search input (ms) */
        const val SEARCH_DEBOUNCE_MS = 300L
    }

    // =========================
    // Analysis Configuration
    // =========================
    object Analysis {
        /** Enable beginner-friendly analysis mode */
        const val BEGINNER_MODE_ENABLED = true

        /** Maximum metrics to extract per filing */
        const val MAX_METRICS_PER_FILING = 100

        /** Enable ratio calculations */
        const val CALCULATE_RATIOS = true

        /** Enable term explanations */
        const val INCLUDE_TERM_EXPLANATIONS = true

        /** Minimum confidence threshold for iXBRL extraction */
        const val IXBRL_MIN_CONFIDENCE = 0.7
    }

    // =========================
    // Logging & Debug
    // =========================
    object Debug {
        /** Enable debug logging */
        const val DEBUG_ENABLED = false

        /** Enable performance timing */
        const val TIMING_ENABLED = true

        /** Log cache operations */
        const val LOG_CACHE_OPERATIONS = true

        /** Log API requests */
        const val LOG_API_REQUESTS = false
    }

    // =========================
    // Validation Thresholds
    // =========================
    object Validation {
        /** Minimum CIK length */
        const val MIN_CIK_LENGTH = 1

        /** Maximum CIK length */
        const val MAX_CIK_LENGTH = 10

        /** Minimum ticker length */
        const val MIN_TICKER_LENGTH = 1

        /** Maximum ticker length */
        const val MAX_TICKER_LENGTH = 5
    }
}
