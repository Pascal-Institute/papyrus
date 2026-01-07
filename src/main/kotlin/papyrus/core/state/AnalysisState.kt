package papyrus.core.state

import papyrus.core.model.FinancialAnalysis

/**
 * Analysis State - sealed class representing different states of document analysis
 *
 * This follows the State pattern to make analysis states type-safe and exhaustive.
 * UI can pattern match on these states to render appropriate components.
 */
sealed class AnalysisState {
    /** Initial idle state - no analysis in progress */
    object Idle : AnalysisState()

    /** Loading state with optional progress message */
    data class Loading(val message: String = "Loading...") : AnalysisState()

    /** Simple document analysis result */
    data class AnalyzeResult(
            val documentTitle: String,
            val documentUrl: String?,
            val content: String,
            val summary: String
    ) : AnalysisState()

    /** Financial analysis result with comprehensive metrics */
    data class FinancialAnalysisResult(val analysis: FinancialAnalysis) : AnalysisState()

    /** Error state with optional retry action */
    data class Error(val message: String, val retryAction: (() -> Unit)?) : AnalysisState()
}
