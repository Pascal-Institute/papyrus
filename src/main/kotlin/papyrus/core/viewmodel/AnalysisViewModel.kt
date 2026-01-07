package papyrus.core.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import papyrus.core.model.FilingItem
import papyrus.ui.FileFormatType
import papyrus.core.network.SecApi
import papyrus.core.resource.AppStrings
import papyrus.core.service.analyzer.FinancialAnalyzer
import papyrus.core.state.AnalysisState

/**
 * AnalysisViewModel - handles document analysis and XBRL data fetching
 *
 * Responsibilities:
 * - Document analysis (HTML, PDF, TXT)
 * - Financial analysis with FinancialAnalyzer
 * - XBRL company facts retrieval
 * - Error handling and retry logic
 *
 * Separates analysis logic from UI, enabling background processing
 * and better testability.
 */
class AnalysisViewModel(private val scope: CoroutineScope) {
    /** Current analysis state - exposed for UI consumption */
    var analysisState by mutableStateOf<AnalysisState>(AnalysisState.Idle)

    /** Currently analyzing filing (accession number) */
    var currentAnalyzingFiling by mutableStateOf<String?>(null)
        private set

    /**
     * Analyze a filing document
     *
     * @param filing The filing item to analyze
     * @param cik Company CIK number
     * @param fileFormat Format type (HTML, PDF, TXT)
     */
    fun analyzeFiling(filing: FilingItem, cik: Int, fileFormat: FileFormatType) {
        scope.launch {
            val url =
                    SecApi.getDocumentUrlWithFormat(
                            cik.toString(),
                            filing.accessionNumber,
                            filing.primaryDocument,
                            fileFormat.extension
                    )

            analysisState =
                    AnalysisState.Loading(AppStrings.Analysis.analyzingDocument(fileFormat.displayName))
            currentAnalyzingFiling = filing.accessionNumber

            try {
                val rawHtml = SecApi.fetchDocumentContent(url)

                // Perform financial analysis on IO dispatcher
                val analysis =
                        withContext(Dispatchers.IO) {
                            FinancialAnalyzer.analyzeForBeginners(
                                    "${filing.form} (${fileFormat.displayName})",
                                    rawHtml
                            )
                        }

                val analysisWithCik = analysis.copy(cik = cik)

                analysisState = AnalysisState.FinancialAnalysisResult(analysisWithCik)
                currentAnalyzingFiling = null
            } catch (e: Exception) {
                analysisState =
                        AnalysisState.Error(
                                message = AppStrings.Analysis.analysisFailed(e.message),
                                retryAction = { analyzeFiling(filing, cik, fileFormat) }
                        )
                currentAnalyzingFiling = null
            }
        }
    }

    /**
     * Reset analysis state to idle
     *
     * Called when navigating back or clearing results
     */
    fun resetAnalysis() {
        analysisState = AnalysisState.Idle
        currentAnalyzingFiling = null
    }
}
