package papyrus

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Desktop
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import papyrus.core.resource.AppStrings
import papyrus.core.state.AnalysisState
import papyrus.core.viewmodel.AnalysisViewModel
import papyrus.core.viewmodel.MainViewModel
import papyrus.ui.AppColors
import papyrus.ui.PapyrusTheme
import papyrus.ui.components.LeftPanel
import papyrus.ui.components.RightPanel
import papyrus.util.file.FileUtils

/** Main Application Entry Point */
fun main() = application {
        Window(
                onCloseRequest = ::exitApplication,
                title = AppStrings.WINDOW_TITLE,
                icon = painterResource("papyrus_icon.png")
        ) { PapyrusApp() }
}

/** Main Application Composable */
@Composable
fun PapyrusApp() {
        val scope = rememberCoroutineScope()

        // ViewModels - centralized business logic
        val mainViewModel = remember { MainViewModel(scope) }
        val analysisViewModel = remember { AnalysisViewModel(scope) }

        // Extract state for easier access
        val appState = mainViewModel.appState
        val bookmarks = mainViewModel.bookmarks
        val analysisState = analysisViewModel.analysisState

        PapyrusTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = AppColors.Background) {
                        Row(modifier = Modifier.fillMaxSize()) {
                                // Left Panel: Search & Navigation
                                LeftPanel(
                                        appState = appState,
                                        bookmarks = bookmarks,
                                        onSearchTextChange = mainViewModel::onSearchTextChange,
                                        onTickerSelected = mainViewModel::onTickerSelected,
                                        onBookmarkClick = mainViewModel::onBookmarkClick,
                                        onBookmarkedTickerClick =
                                                mainViewModel::onBookmarkedTickerClick,
                                        onRemoveBookmark = mainViewModel::onRemoveBookmark,
                                        onBackToSearch = {
                                                mainViewModel.onBackToSearch()
                                                analysisViewModel.resetAnalysis()
                                        },
                                        onAnalyze = { filing, fileFormat ->
                                                appState.selectedTicker?.let { ticker ->
                                                        analysisViewModel.analyzeFiling(
                                                                filing,
                                                                ticker.cik,
                                                                fileFormat
                                                        )
                                                }
                                        },
                                        onOpenInBrowser = { filing ->
                                                appState.selectedTicker?.let { ticker ->
                                                        val url =
                                                                papyrus.core.secApiClient
                                                                        .getDocumentUrl(
                                                                                ticker.cik
                                                                                        .toString(),
                                                                                filing.accessionNumber,
                                                                                filing.primaryDocument
                                                                        )
                                                        if (Desktop.isDesktopSupported()) {
                                                                Desktop.getDesktop()
                                                                        .browse(URI(url))
                                                        }
                                                }
                                        }
                                )

                                // Divider
                                Divider(
                                        modifier = Modifier.fillMaxHeight().width(1.dp),
                                        color = AppColors.Divider
                                )

                                // Right Panel: Analysis Results
                                RightPanel(
                                        appState = appState.copy(analysisState = analysisState),
                                        onFileDropped = { file ->
                                                scope.launch {
                                                        // Immediately show loading state
                                                        analysisViewModel.analysisState =
                                                                AnalysisState.Loading(
                                                                        AppStrings.FileProcessing
                                                                                .readingFile(
                                                                                        file.name
                                                                                )
                                                                )

                                                        try {
                                                                if (!FileUtils.isSupportedFile(file)
                                                                ) {
                                                                        analysisViewModel
                                                                                .analysisState =
                                                                                AnalysisState.Error(
                                                                                        message =
                                                                                                AppStrings
                                                                                                        .FileProcessing
                                                                                                        .unsupportedFileType(
                                                                                                                file.extension
                                                                                                        ),
                                                                                        retryAction =
                                                                                                null
                                                                                )
                                                                        return@launch
                                                                }

                                                                // Update loading message for
                                                                // content extraction
                                                                analysisViewModel.analysisState =
                                                                        AnalysisState.Loading(
                                                                                AppStrings
                                                                                        .FileProcessing
                                                                                        .EXTRACTING_CONTENT
                                                                        )

                                                                val extracted =
                                                                        FileUtils.extractDocument(
                                                                                file
                                                                        )

                                                                val content =
                                                                        when (file.extension
                                                                                        .lowercase()
                                                                        ) {
                                                                                "html", "htm" ->
                                                                                        extracted
                                                                                                .rawContent
                                                                                else ->
                                                                                        extracted
                                                                                                .extractedText
                                                                        }

                                                                // Update loading message for
                                                                // analysis
                                                                analysisViewModel.analysisState =
                                                                        AnalysisState.Loading(
                                                                                AppStrings
                                                                                        .FileProcessing
                                                                                        .ANALYZING_DATA
                                                                        )

                                                                // Use beginner-friendly analysis
                                                                val analysis =
                                                                        withContext(
                                                                                Dispatchers.IO
                                                                        ) {
                                                                                papyrus.core.service
                                                                                        .analyzer
                                                                                        .FinancialAnalyzer
                                                                                        .analyzeForBeginners(
                                                                                                file.name,
                                                                                                content
                                                                                        )
                                                                        }

                                                                analysisViewModel.analysisState =
                                                                        AnalysisState
                                                                                .FinancialAnalysisResult(
                                                                                        analysis
                                                                                )
                                                        } catch (e: Exception) {
                                                                analysisViewModel.analysisState =
                                                                        AnalysisState.Error(
                                                                                message =
                                                                                        AppStrings
                                                                                                .FileProcessing
                                                                                                .errorReadingFile(
                                                                                                        e.message
                                                                                                ),
                                                                                retryAction = null
                                                                        )
                                                        }
                                                }
                                        },
                                        onDragStateChange = mainViewModel::setDragging,
                                        onCloseAnalysis = analysisViewModel::resetAnalysis,
                                        onOpenInBrowser = { url ->
                                                if (Desktop.isDesktopSupported()) {
                                                        Desktop.getDesktop().browse(URI(url))
                                                }
                                        }
                                )
                        }
                }
        }
}

/** Build analysis summary from document content */
private fun buildAnalysisSummary(rawHtml: String, cleanText: String): String {
        val hasRevenue = cleanText.contains(AppStrings.Analysis.TERM_REVENUE, ignoreCase = true)
        val hasRisk = cleanText.contains(AppStrings.Analysis.TERM_RISK_FACTORS, ignoreCase = true)
        val hasNetIncome =
                cleanText.contains(AppStrings.Analysis.TERM_NET_INCOME, ignoreCase = true)
        val hasAssets = cleanText.contains(AppStrings.Analysis.TERM_TOTAL_ASSETS, ignoreCase = true)
        val hasLiabilities =
                cleanText.contains(AppStrings.Analysis.TERM_LIABILITIES, ignoreCase = true)

        return buildString {
                appendLine(AppStrings.Analysis.SUMMARY_TITLE)
                appendLine(AppStrings.Analysis.SUMMARY_SEPARATOR)
                appendLine()
                appendLine(AppStrings.Analysis.documentSize(rawHtml.length.formatWithCommas()))
                appendLine()
                appendLine(AppStrings.Analysis.KEY_TERMS_HEADER)
                appendLine(
                        "  ${if (hasRevenue) AppStrings.Analysis.MARKER_FOUND else AppStrings.Analysis.MARKER_NOT_FOUND} ${AppStrings.Analysis.TERM_REVENUE}"
                )
                appendLine(
                        "  ${if (hasNetIncome) AppStrings.Analysis.MARKER_FOUND else AppStrings.Analysis.MARKER_NOT_FOUND} ${AppStrings.Analysis.TERM_NET_INCOME}"
                )
                appendLine(
                        "  ${if (hasAssets) AppStrings.Analysis.MARKER_FOUND else AppStrings.Analysis.MARKER_NOT_FOUND} ${AppStrings.Analysis.TERM_TOTAL_ASSETS}"
                )
                appendLine(
                        "  ${if (hasLiabilities) AppStrings.Analysis.MARKER_FOUND else AppStrings.Analysis.MARKER_NOT_FOUND} ${AppStrings.Analysis.TERM_LIABILITIES}"
                )
                appendLine(
                        "  ${if (hasRisk) AppStrings.Analysis.MARKER_FOUND else AppStrings.Analysis.MARKER_NOT_FOUND} ${AppStrings.Analysis.TERM_RISK_FACTORS}"
                )
                appendLine()
                appendLine(AppStrings.Analysis.SUMMARY_TIP)
        }
}

private fun Int.formatWithCommas(): String {
        return String.format("%,d", this)
}
