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
import papyrus.core.model.TickerEntry
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
                title = "Papyrus - SEC Financial Analyzer",
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
        val currentAnalyzingFiling = analysisViewModel.currentAnalyzingFiling

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
                                        onBookmarkedTickerClick = mainViewModel::onBookmarkedTickerClick,
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
                                                                papyrus.core.network.SecApi
                                                                        .getDocumentUrl(
                                                                                ticker.cik.toString(),
                                                                                filing.accessionNumber,
                                                                                filing.primaryDocument
                                                                        )
                                                        if (Desktop.isDesktopSupported()) {
                                                                Desktop.getDesktop().browse(URI(url))
                                                        }
                                                }
                                        },

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
                                                                        "Reading file... ${file.name}"
                                                                )

                                                        try {
                                                                if (!FileUtils.isSupportedFile(file)
                                                                ) {
                                                                        analysisViewModel
                                                                                .analysisState =
                                                                                        AnalysisState
                                                                                                .Error(
                                                                                                        message =
                                                                                                                "Unsupported file type: ${file.extension}\nSupported: PDF, HTML, HTM, TXT",
                                                                                                        retryAction =
                                                                                                                null
                                                                                                )
                                                                        return@launch
                                                                }

                                                                // Update loading message for
                                                                // content extraction
                                                                analysisViewModel.analysisState =
                                                                        AnalysisState.Loading(
                                                                                "문서 내용을 추출하는 중.."
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
                                                                                "재무 데이터를 분석하는 중.."
                                                                        )

                                                                // Use beginner-friendly analysis
                                                                val analysis =
                                                                        kotlinx.coroutines
                                                                                .withContext(
                                                                                        kotlinx
                                                                                                .coroutines
                                                                                                .Dispatchers
                                                                                                .IO
                                                                                ) {
                                                                                        papyrus.core
                                                                                                .service
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
                                                                                        "Error reading file: ${e.message}",
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

                        // Settings dialog

                }
        }
}

/** Build analysis summary from document content */
private fun buildAnalysisSummary(rawHtml: String, cleanText: String): String {
        val hasRevenue = cleanText.contains("Revenue", ignoreCase = true)
        val hasRisk = cleanText.contains("Risk", ignoreCase = true)
        val hasNetIncome = cleanText.contains("Net Income", ignoreCase = true)
        val hasAssets = cleanText.contains("Total Assets", ignoreCase = true)
        val hasLiabilities = cleanText.contains("Liabilities", ignoreCase = true)

        return buildString {
                appendLine("Quick Analysis Summary")
                appendLine("------------------------")
                appendLine()
                appendLine("Document Size: ${rawHtml.length.formatWithCommas()} characters")
                appendLine()
                appendLine("Key Financial Terms Found:")
                appendLine("  ${if (hasRevenue) "[O]" else "[ ]"} Revenue")
                appendLine("  ${if (hasNetIncome) "[O]" else "[ ]"} Net Income")
                appendLine("  ${if (hasAssets) "[O]" else "[ ]"} Total Assets")
                appendLine("  ${if (hasLiabilities) "[O]" else "[ ]"} Liabilities")
                appendLine("  ${if (hasRisk) "[O]" else "[ ]"} Risk Factors")
                appendLine()
                appendLine("Tip: For detailed analysis, use the Full Content tab")
        }
}

private fun Int.formatWithCommas(): String {
        return String.format("%,d", this)
}
