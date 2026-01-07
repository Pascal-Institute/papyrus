package papyrus.core.resource

/**
 * Centralized Application Strings
 *
 * All user-facing strings organized by category.
 * Following financial precision principle: clear, accurate, traceable messaging.
 *
 * Categories:
 * - App metadata (title, window title)
 * - UI labels and placeholders
 * - Status messages (loading, success, error)
 * - Analysis messages
 * - Error messages
 * - Financial terms and tooltips
 */
object AppStrings {
    // =========================
    // Application Metadata
    // =========================
    const val APP_TITLE = "Papyrus"
    const val APP_SUBTITLE = "SEC Financial Analyzer"
    const val WINDOW_TITLE = "Papyrus - SEC Financial Analyzer"

    // =========================
    // Search & Navigation
    // =========================
    const val SEARCH_PLACEHOLDER = "Search company or ticker..."
    const val SEARCH_EMPTY_TITLE = "Search for companies"
    const val SEARCH_EMPTY_DESCRIPTION = "Enter a ticker symbol or company name"

    const val NO_FILINGS_TITLE = "No filings found"
    const val NO_FILINGS_DESCRIPTION = "This company has no recent SEC filings"

    const val FILTER_NO_RESULTS_TITLE = "필터 결과 없음"
    const val FILTER_NO_RESULTS_DESCRIPTION = "선택한 보고서 유형에 해당하는 파일이 없습니다"

    // =========================
    // File Processing Messages
    // =========================
    object FileProcessing {
        fun readingFile(fileName: String) = "Reading file... $fileName"
        const val EXTRACTING_CONTENT = "문서 내용을 추출하는 중.."
        const val ANALYZING_DATA = "재무 데이터를 분석하는 중.."

        fun unsupportedFileType(extension: String) =
            "Unsupported file type: $extension\nSupported: PDF, HTML, HTM, TXT"

        fun errorReadingFile(message: String?) = "Error reading file: $message"
    }

    // =========================
    // Analysis Messages
    // =========================
    object Analysis {
        fun analyzingDocument(formatName: String) = "${formatName} 문서를 분석하고 있습니다..."
        const val ANALYZING_ERROR = "분석 중 오류 발생"
        fun analysisFailed(message: String?) = "분석 중 오류 발생: $message"

        const val ANALYSIS_NOT_AVAILABLE = "분석 불가"
        const val ANALYSIS_NEEDED = "분석 필요"

        // Summary template
        const val SUMMARY_TITLE = "Quick Analysis Summary"
        const val SUMMARY_SEPARATOR = "------------------------"
        fun documentSize(chars: String) = "Document Size: $chars characters"
        const val KEY_TERMS_HEADER = "Key Financial Terms Found:"
        const val SUMMARY_TIP = "Tip: For detailed analysis, use the Full Content tab"

        // Financial terms
        const val TERM_REVENUE = "Revenue"
        const val TERM_NET_INCOME = "Net Income"
        const val TERM_TOTAL_ASSETS = "Total Assets"
        const val TERM_LIABILITIES = "Liabilities"
        const val TERM_RISK_FACTORS = "Risk Factors"

        const val MARKER_FOUND = "[O]"
        const val MARKER_NOT_FOUND = "[ ]"
    }

    // =========================
    // Network & API Messages
    // =========================
    object Network {
        const val FETCHING_SUBMISSIONS = "Fetching company submissions..."
        fun submissionsFetchError(message: String?) = "Failed to fetch submissions: $message"

        const val FETCHING_DOCUMENT = "Fetching document..."
        fun documentFetchError(message: String?) = "Failed to fetch document: $message"
    }

    // =========================
    // Bookmark Messages
    // =========================
    object Bookmarks {
        fun bookmarkAdded(ticker: String) = "Bookmarked: $ticker"
        fun bookmarkRemoved(ticker: String) = "Removed bookmark: $ticker"
        const val BOOKMARK_ERROR = "Failed to manage bookmark"
    }

    // =========================
    // Validation Messages
    // =========================
    object Validation {
        const val INVALID_CIK = "Invalid CIK number"
        const val INVALID_TICKER = "Invalid ticker symbol"
        const val MISSING_DATA = "Required data is missing"
    }

    // =========================
    // Cache Messages
    // =========================
    object Cache {
        const val LOADING_FROM_CACHE = "Loaded analysis from cache"
        const val SAVING_TO_CACHE = "Analysis cached successfully"
        fun cacheFailed(message: String?) = "Failed to load cached analysis: $message"
        const val PERFORMING_FRESH_ANALYSIS = "Performing fresh analysis..."
    }

    // =========================
    // Financial Analysis Labels
    // =========================
    object Financial {
        // Metric categories
        const val CATEGORY_PROFITABILITY = "Profitability"
        const val CATEGORY_LIQUIDITY = "Liquidity"
        const val CATEGORY_LEVERAGE = "Leverage"
        const val CATEGORY_EFFICIENCY = "Efficiency"

        // Common labels
        const val LABEL_AMOUNT = "Amount"
        const val LABEL_RATIO = "Ratio"
        const val LABEL_PERCENTAGE = "Percentage"
        const val LABEL_CURRENCY = "USD"

        // Insights
        const val INSIGHTS_TITLE = "Key Insights"
        const val RATIOS_TITLE = "Financial Ratios"
        const val METRICS_TITLE = "Financial Metrics"
    }
}
