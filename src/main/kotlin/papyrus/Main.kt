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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import papyrus.core.model.BookmarkedTicker
import papyrus.core.model.FilingItem
import papyrus.core.model.FinancialAnalysis
import papyrus.core.model.TickerEntry
import papyrus.core.network.SecApi
import papyrus.core.service.analyzer.FinancialAnalyzer
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

        // State management
        var appState by remember { mutableStateOf(AppState()) }


        // Bookmark state
        var bookmarks by remember { mutableStateOf(BookmarkManager.getAllBookmarks()) }
        var recentlyViewedCiks by remember { mutableStateOf(BookmarkManager.getRecentlyViewed()) }

        // Load tickers on startup
        LaunchedEffect(Unit) {
                appState = appState.copy(isLoading = true)
                SecApi.loadTickers()
                bookmarks = BookmarkManager.getAllBookmarks()
                recentlyViewedCiks = BookmarkManager.getRecentlyViewed()
                appState = appState.copy(isLoading = false)
        }

        PapyrusTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = AppColors.Background) {
                        Row(modifier = Modifier.fillMaxSize()) {
                                // Left Panel: Search & Navigation
                                LeftPanel(
                                        appState = appState,
                                        bookmarks = bookmarks,
                                        onSearchTextChange = { query ->
                                                appState =
                                                        appState.copy(
                                                                searchText = query,
                                                                searchResults =
                                                                        SecApi.searchTicker(query)
                                                        )
                                        },
                                        onTickerSelected = { ticker ->
                                                scope.launch {
                                                        // Add to recent view history
                                                        BookmarkManager.addToRecentlyViewed(
                                                                ticker.cik
                                                        )
                                                        recentlyViewedCiks =
                                                                BookmarkManager.getRecentlyViewed()

                                                        appState =
                                                                appState.copy(
                                                                        selectedTicker = ticker,
                                                                        searchText = "",
                                                                        searchResults = emptyList(),
                                                                        isLoading = true,
                                                                        analysisState =
                                                                                AnalysisState.Idle
                                                                )

                                                        // Fetch submissions and news in parallel
                                                        val submissionsDeferred = async {
                                                                val sub =
                                                                        SecApi.getSubmissions(
                                                                                ticker.cik
                                                                        )
                                                                sub?.filings?.recent?.let {
                                                                        SecApi.transformFilings(it)
                                                                }
                                                                        ?: emptyList()
                                                        }

                                                        val filings = submissionsDeferred.await()

                                                        appState =
                                                                appState.copy(
                                                                        submissions = filings,
                                                                        isLoading = false
                                                                )
                                                }
                                        },
                                        onBookmarkClick = { ticker ->
                                                if (BookmarkManager.isBookmarked(ticker.cik)) {
                                                        BookmarkManager.removeBookmark(ticker.cik)
                                                } else {
                                                        BookmarkManager.addBookmark(ticker)
                                                }
                                                bookmarks = BookmarkManager.getAllBookmarks()
                                        },
                                        onBookmarkedTickerClick = { cik ->
                                                scope.launch {
                                                        // Find ticker info by CIK
                                                        val ticker =
                                                                SecApi.searchTicker("").find {
                                                                        it.cik == cik
                                                                }
                                                                        ?: bookmarks
                                                                                .find {
                                                                                        it.cik ==
                                                                                                cik
                                                                                }
                                                                                ?.let {
                                                                                        TickerEntry(
                                                                                                it.cik,
                                                                                                it.ticker,
                                                                                                it.companyName
                                                                                        )
                                                                                }

                                                        if (ticker != null) {
                                                                BookmarkManager.addToRecentlyViewed(
                                                                        cik
                                                                )
                                                                recentlyViewedCiks =
                                                                        BookmarkManager
                                                                                .getRecentlyViewed()

                                                                appState =
                                                                        appState.copy(
                                                                                selectedTicker =
                                                                                        ticker,
                                                                                searchText = "",
                                                                                searchResults =
                                                                                        emptyList(),
                                                                                isLoading = true,
                                                                                analysisState =
                                                                                        AnalysisState
                                                                                                .Idle
                                                                        )

                                                                // Fetch submissions and news in
                                                                // parallel
                                                                val submissionsDeferred = async {
                                                                        val sub =
                                                                                SecApi.getSubmissions(
                                                                                        cik
                                                                                )
                                                                        sub?.filings?.recent?.let {
                                                                                SecApi.transformFilings(
                                                                                        it
                                                                                )
                                                                        }
                                                                                ?: emptyList()
                                                                }

                                                                val filings =
                                                                        submissionsDeferred.await()

                                                                appState =
                                                                        appState.copy(
                                                                                submissions =
                                                                                        filings,
                                                                                isLoading = false
                                                                        )
                                                        }
                                                }
                                        },
                                        onRemoveBookmark = { cik ->
                                                BookmarkManager.removeBookmark(cik)
                                                bookmarks = BookmarkManager.getAllBookmarks()
                                        },
                                        onBackToSearch = {
                                                appState =
                                                        appState.copy(
                                                                selectedTicker = null,
                                                                submissions = emptyList(),
                                                                analysisState = AnalysisState.Idle
                                                        )
                                        },
                                        onAnalyze = { filing, fileFormat ->
                                                scope.launch {
                                                        val cik =
                                                                appState.selectedTicker?.cik
                                                                        .toString()
                                                        val url =
                                                                SecApi.getDocumentUrlWithFormat(
                                                                        cik,
                                                                        filing.accessionNumber,
                                                                        filing.primaryDocument,
                                                                        fileFormat.extension
                                                                )

                                                        appState =
                                                                appState.copy(
                                                                        analysisState =
                                                                                AnalysisState
                                                                                        .Loading(
                                                                                                "${fileFormat.displayName} 臾몄꽌瑜?遺꾩꽍?섍퀬 ?덉뒿?덈떎..."
                                                                                        ),
                                                                        currentAnalyzingFiling =
                                                                                filing.accessionNumber
                                                                )

                                                        try {
                                                                val rawHtml =
                                                                        SecApi.fetchDocumentContent(
                                                                                url
                                                                        )

                                                                // Perform financial analysis
                                                                val analysis =
                                                                        withContext(
                                                                                kotlinx.coroutines
                                                                                        .Dispatchers
                                                                                        .IO
                                                                        ) {
                                                                                FinancialAnalyzer
                                                                                        .analyzeForBeginners(
                                                                                                "${filing.form} (${fileFormat.displayName})",
                                                                                                rawHtml
                                                                                        )
                                                                        }

                                                                val analysisWithCik =
                                                                        analysis.copy(
                                                                                cik =
                                                                                        appState.selectedTicker
                                                                                                ?.cik
                                                                        )

                                                                appState =
                                                                        appState.copy(
                                                                                analysisState =
                                                                                        AnalysisState
                                                                                                .FinancialAnalysisResult(
                                                                                                        analysisWithCik
                                                                                                ),
                                                                                currentAnalyzingFiling =
                                                                                        null
                                                                        )
                                                        } catch (e: Exception) {
                                                                appState =
                                                                        appState.copy(
                                                                                analysisState =
                                                                                        AnalysisState
                                                                                                .Error(
                                                                                                        message =
                                                                                                                "Document analysis failed: ${e.message}",
                                                                                                        retryAction =
                                                                                                                null
                                                                                                ),
                                                                                currentAnalyzingFiling =
                                                                                        null
                                                                        )
                                                        }
                                                }
                                        },
                                        onOpenInBrowser = { filing ->
                                                val cik = appState.selectedTicker?.cik.toString()
                                                val url =
                                                        SecApi.getDocumentUrl(
                                                                cik,
                                                                filing.accessionNumber,
                                                                filing.primaryDocument
                                                        )
                                                if (Desktop.isDesktopSupported()) {
                                                        Desktop.getDesktop().browse(URI(url))
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
                                        appState = appState,
                                        onFileDropped = { file ->
                                                scope.launch {
                                                        // Immediately show loading state
                                                        appState =
                                                                appState.copy(
                                                                        analysisState =
                                                                                AnalysisState
                                                                                        .Loading(
                                                                                                "Reading file... ${file.name}"
                                                                                        )
                                                                )

                                                        try {
                                                                if (!FileUtils.isSupportedFile(file)
                                                                ) {
                                                                        appState =
                                                                                appState.copy(
                                                                                        analysisState =
                                                                                                AnalysisState
                                                                                                        .Error(
                                                                                                                message =
                                                                                                                        "Unsupported file type: ${file.extension}\nSupported: PDF, HTML, HTM, TXT",
                                                                                                                retryAction =
                                                                                                                        null
                                                                                                        )
                                                                                )
                                                                        return@launch
                                                                }

                                                                // Update loading message for
                                                                // content extraction
                                                                appState =
                                                                        appState.copy(
                                                                                analysisState =
                                                                                        AnalysisState
                                                                                                .Loading(
                                                                                                        "臾몄꽌 ?댁슜??異붿텧?섎뒗 以?.."
                                                                                                )
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
                                                                appState =
                                                                        appState.copy(
                                                                                analysisState =
                                                                                        AnalysisState
                                                                                                .Loading(
                                                                                                        "?щТ ?곗씠?곕? 遺꾩꽍?섎뒗 以?.."
                                                                                                )
                                                                        )

                                                                // Use beginner-friendly analysis
                                                                val analysis =
                                                                        FinancialAnalyzer
                                                                                .analyzeForBeginners(
                                                                                        file.name,
                                                                                        content
                                                                                )

                                                                appState =
                                                                        appState.copy(
                                                                                analysisState =
                                                                                        AnalysisState
                                                                                                .FinancialAnalysisResult(
                                                                                                        analysis
                                                                                                )
                                                                        )
                                                        } catch (e: Exception) {
                                                                appState =
                                                                        appState.copy(
                                                                                analysisState =
                                                                                        AnalysisState
                                                                                                .Error(
                                                                                                        message =
                                                                                                                "Error reading file: ${e.message}",
                                                                                                        retryAction =
                                                                                                                null
                                                                                                )
                                                                        )
                                                        }
                                                }
                                        },
                                        onDragStateChange = { isDragging ->
                                                appState = appState.copy(isDragging = isDragging)
                                        },
                                        onCloseAnalysis = {
                                                appState =
                                                        appState.copy(
                                                                analysisState = AnalysisState.Idle
                                                        )
                                        },
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

/** Application State */
data class AppState(
        val searchText: String = "",
        val searchResults: List<TickerEntry> = emptyList(),
        val selectedTicker: TickerEntry? = null,
        val submissions: List<FilingItem> = emptyList(),
        val isLoading: Boolean = false,
        val isDragging: Boolean = false,
        val analysisState: AnalysisState = AnalysisState.Idle,
        val currentAnalyzingFiling: String? = null
)

/** Analysis State - sealed class for different states */
sealed class AnalysisState {
        object Idle : AnalysisState()

        data class Loading(val message: String = "Loading...") : AnalysisState()

        data class AnalyzeResult(
                val documentTitle: String,
                val documentUrl: String?,
                val content: String,
                val summary: String
        ) : AnalysisState()

        data class FinancialAnalysisResult(val analysis: FinancialAnalysis) : AnalysisState()

        data class Error(val message: String, val retryAction: (() -> Unit)?) : AnalysisState()
}
