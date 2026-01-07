package papyrus

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Desktop
import java.net.URI
import kotlinx.coroutines.launch
import papyrus.core.model.BookmarkedTicker
import papyrus.core.model.FilingItem
import papyrus.core.model.TickerEntry
import papyrus.core.state.AppState
import papyrus.core.state.AnalysisState
import papyrus.core.viewmodel.AnalysisViewModel
import papyrus.core.viewmodel.MainViewModel
import papyrus.ui.*
import papyrus.util.data.BookmarkManager
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

/** Left Panel - Search and Filing List */
@Composable
private fun LeftPanel(
        appState: AppState,
        bookmarks: List<BookmarkedTicker>,
        onSearchTextChange: (String) -> Unit,
        onTickerSelected: (TickerEntry) -> Unit,
        onBookmarkClick: (TickerEntry) -> Unit,
        onBookmarkedTickerClick: (Int) -> Unit,
        onRemoveBookmark: (Int) -> Unit,
        onBackToSearch: () -> Unit,
        onAnalyze: (FilingItem, papyrus.ui.FileFormatType) -> Unit,
        onOpenInBrowser: (FilingItem) -> Unit,

) {
        Column(
                modifier =
                        Modifier.width(AppDimens.SidebarWidth)
                                .fillMaxHeight()
                                .background(AppColors.Surface)
        ) {
                // App Header
                AppHeader(
                        title = "Papyrus",
                        subtitle = "SEC Financial Analyzer",

                )

                // Search Box
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .background(AppColors.Background)
                                        .padding(AppDimens.PaddingMedium)
                ) {
                        SearchBox(
                                value = appState.searchText,
                                onValueChange = onSearchTextChange,
                                placeholder = "Search company or ticker...",
                                isLoading = appState.isLoading && appState.selectedTicker == null
                        )
                }

                // Loading indicator
                AnimatedVisibility(visible = appState.isLoading) {
                        LinearProgressIndicator(
                                modifier =
                                        Modifier.fillMaxWidth().height(AppDimens.ProgressBarHeight),
                                color = AppColors.Primary,
                                backgroundColor = AppColors.PrimaryLight
                        )
                }

                // Content
                if (appState.selectedTicker == null) {
                        // Bookmarks and Search Results
                        Column(modifier = Modifier.fillMaxSize()) {
                                // Bookmark section (only shown when search is empty)
                                if (appState.searchText.isEmpty() && bookmarks.isNotEmpty()) {
                                        BookmarkHorizontalList(
                                                bookmarks = bookmarks,
                                                onTickerClick = onBookmarkedTickerClick,
                                                onRemove = onRemoveBookmark
                                        )
                                }

                                // Search Results
                                SearchResultsList(
                                        results = appState.searchResults,
                                        onTickerSelected = onTickerSelected,
                                        showEmptyState =
                                                appState.searchText.isEmpty() && bookmarks.isEmpty()
                                )
                        }
                } else {
                        // Company Detail & Filings
                        CompanyFilingsPanel(
                                ticker = appState.selectedTicker,
                                filings = appState.submissions,
                                currentAnalyzingFiling = appState.currentAnalyzingFiling,
                                isBookmarked =
                                        BookmarkManager.isBookmarked(appState.selectedTicker.cik),
                                onBackClick = onBackToSearch,
                                onBookmarkClick = { onBookmarkClick(appState.selectedTicker) },
                                onAnalyze = onAnalyze,
                                onOpenInBrowser = onOpenInBrowser
                        )
                }
        }
}

@Composable
private fun SearchResultsList(
        results: List<TickerEntry>,
        onTickerSelected: (TickerEntry) -> Unit,
        showEmptyState: Boolean = true
) {
        if (results.isEmpty()) {
                if (showEmptyState) {
                        EmptyState(
                                icon = Icons.Outlined.Search,
                                title = "Search for companies",
                                description = "Enter a ticker symbol or company name"
                        )
                }
        } else {
                LazyColumn(
                        modifier =
                                Modifier.fillMaxSize().padding(horizontal = AppDimens.PaddingSmall),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall)
                ) {
                        items(results) { ticker ->
                                TickerCard(ticker = ticker, onClick = { onTickerSelected(ticker) })
                        }
                }
        }
}

@Composable
private fun CompanyFilingsPanel(
        ticker: TickerEntry,
        filings: List<FilingItem>,
        currentAnalyzingFiling: String?,
        isBookmarked: Boolean,
        onBackClick: () -> Unit,
        onBookmarkClick: () -> Unit,
        onAnalyze: (FilingItem, papyrus.ui.FileFormatType) -> Unit,
        onOpenInBrowser: (FilingItem) -> Unit
) {
        Column(modifier = Modifier.fillMaxSize()) {
                // Company Info Card
                Box(modifier = Modifier.fillMaxWidth().padding(AppDimens.PaddingMedium)) {
                        CompanyInfoCard(
                                ticker = ticker,
                                isBookmarked = isBookmarked,
                                onBackClick = onBackClick,
                                onBookmarkClick = onBookmarkClick
                        )
                }

                Divider(color = AppColors.Divider)

                // Tab selection

                // Report type filter state (default: 10-Q for quarterly insights)
                var selectedReportTypes by remember { mutableStateOf(setOf("10-Q")) }

                // Filtered filings list
                val filteredFilings =
                        remember(filings, selectedReportTypes) {
                                if (selectedReportTypes.isEmpty()) {
                                        filings
                                } else {
                                        filings.filter { filing ->
                                                selectedReportTypes.any { type ->
                                                        filing.form.contains(
                                                                type,
                                                                ignoreCase = true
                                                        )
                                                }
                                        }
                                }
                        }

                Divider(color = AppColors.Divider)

                // Tab content
                // SEC Filings
                if (filings.isEmpty()) {
                        EmptyState(
                                icon = Icons.Outlined.FolderOff,
                                title = "No filings found",
                                description = "This company has no recent SEC filings"
                        )
                } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                                // Report type filter
                                ReportTypeFilter(
                                        availableTypes =
                                                filings.map { it.form }.distinct().sorted(),
                                        selectedTypes = selectedReportTypes,
                                        onTypesChanged = { selectedReportTypes = it }
                                )

                                Divider(color = AppColors.Divider)

                                // If no filtered results
                                if (filteredFilings.isEmpty()) {
                                        EmptyState(
                                                icon = Icons.Outlined.FilterAlt,
                                                title = "?꾪꽣 寃곌낵 ?놁쓬",
                                                description = "?좏깮??蹂닿퀬????낆뿉 ?대떦?섎뒗 ?뚯씪???놁뒿?덈떎"
                                        )
                                } else {
                                        LazyColumn(
                                                modifier =
                                                        Modifier.fillMaxSize()
                                                                .padding(
                                                                        horizontal =
                                                                                AppDimens
                                                                                        .PaddingSmall
                                                                ),
                                                verticalArrangement =
                                                        Arrangement.spacedBy(AppDimens.PaddingSmall)
                                        ) {
                                                items(filteredFilings) { filing ->
                                                        FilingCard(
                                                                filing = filing,
                                                                onOpenBrowser = {
                                                                        onOpenInBrowser(filing)
                                                                },
                                                                onAnalyze = {
                                                                        filingItem,
                                                                        format ->
                                                                        onAnalyze(
                                                                                filingItem,
                                                                                format
                                                                        )
                                                                },
                                                                isAnalyzing =
                                                                        currentAnalyzingFiling ==
                                                                                filing.accessionNumber
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}

/** Right Panel - Analysis Results */
@Composable
private fun RightPanel(
        appState: AppState,
        onFileDropped: (java.io.File) -> Unit,
        onDragStateChange: (Boolean) -> Unit,
        onCloseAnalysis: () -> Unit,
        onOpenInBrowser: (String) -> Unit
) {
        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .background(AppColors.Background)
                                .padding(AppDimens.PaddingLarge)
        ) {
                AnimatedContent(
                        targetState = appState.analysisState,
                        transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) { state ->
                        when (state) {
                                is AnalysisState.Idle -> {
                                        DragDropPanel(
                                                isDragging = appState.isDragging,
                                                onDragStateChange = onDragStateChange,
                                                onFileDropped = onFileDropped
                                        )
                                }
                                is AnalysisState.Loading -> {
                                        AnalysisLoadingView(message = state.message)
                                }
                                is AnalysisState.AnalyzeResult -> {
                                        AnalyzeResultView(
                                                documentTitle = state.documentTitle,
                                                documentUrl = state.documentUrl,
                                                analysisContent = state.content,
                                                analysisSummary = state.summary,
                                                onClose = onCloseAnalysis,
                                                onOpenInBrowser =
                                                        state.documentUrl?.let { url ->
                                                                { onOpenInBrowser(url) }
                                                        }
                                        )
                                }
                                is AnalysisState.FinancialAnalysisResult -> {
                                        FinancialAnalysisPanel(
                                                analysis = state.analysis,
                                                onClose = onCloseAnalysis
                                        )
                                }
                                is AnalysisState.Error -> {
                                        AnalysisErrorView(
                                                message = state.message,
                                                onRetry = state.retryAction,
                                                onClose = onCloseAnalysis
                                        )
                                }
                        }
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
