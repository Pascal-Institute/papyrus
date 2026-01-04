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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch
import papyrus.ui.*
import java.awt.Desktop
import java.net.URI

/**
 * Main Application Entry Point
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Papyrus - SEC Financial Analyzer"
    ) {
        PapyrusApp()
    }
}

/**
 * Main Application Composable
 */
@Composable
fun PapyrusApp() {
    val scope = rememberCoroutineScope()
    
    // State management
    var appState by remember { mutableStateOf(AppState()) }
    
    // Load tickers on startup
    LaunchedEffect(Unit) {
        appState = appState.copy(isLoading = true)
        SecApi.loadTickers()
        appState = appState.copy(isLoading = false)
    }
    
    PapyrusTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppColors.Background
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Panel: Search & Navigation
                LeftPanel(
                    appState = appState,
                    onSearchTextChange = { query ->
                        appState = appState.copy(
                            searchText = query,
                            searchResults = SecApi.searchTicker(query)
                        )
                    },
                    onTickerSelected = { ticker ->
                        scope.launch {
                            appState = appState.copy(
                                selectedTicker = ticker,
                                searchText = "",
                                searchResults = emptyList(),
                                isLoading = true,
                                analysisState = AnalysisState.Idle
                            )
                            
                            val sub = SecApi.getSubmissions(ticker.cik)
                            val filings = sub?.filings?.recent?.let { SecApi.transformFilings(it) } ?: emptyList()
                            
                            appState = appState.copy(
                                submissions = filings,
                                isLoading = false
                            )
                        }
                    },
                    onBackToSearch = {
                        appState = appState.copy(
                            selectedTicker = null,
                            submissions = emptyList(),
                            analysisState = AnalysisState.Idle
                        )
                    },
                    onQuickAnalyze = { filing ->
                        scope.launch {
                            val cik = appState.selectedTicker?.cik.toString()
                            val url = SecApi.getDocumentUrl(
                                cik,
                                filing.accessionNumber,
                                filing.primaryDocument
                            )
                            
                            appState = appState.copy(
                                analysisState = AnalysisState.Loading("Fetching document..."),
                                currentAnalyzingFiling = filing.accessionNumber
                            )
                            
                            try {
                                val rawHtml = SecApi.fetchDocumentContent(url)
                                val text = rawHtml
                                    .replace(Regex("<[^>]*>"), " ")
                                    .replace(Regex("\\s+"), " ")
                                    .take(10000)
                                
                                val summary = buildAnalysisSummary(rawHtml, text)
                                
                                appState = appState.copy(
                                    analysisState = AnalysisState.QuickAnalyzeResult(
                                        documentTitle = filing.primaryDocument,
                                        documentUrl = url,
                                        content = text,
                                        summary = summary
                                    ),
                                    currentAnalyzingFiling = null
                                )
                            } catch (e: Exception) {
                                appState = appState.copy(
                                    analysisState = AnalysisState.Error(
                                        message = "Failed to analyze document: ${e.message}",
                                        retryAction = null
                                    ),
                                    currentAnalyzingFiling = null
                                )
                            }
                        }
                    },
                    onOpenInBrowser = { filing ->
                        val cik = appState.selectedTicker?.cik.toString()
                        val url = SecApi.getDocumentUrl(
                            cik,
                            filing.accessionNumber,
                            filing.primaryDocument
                        )
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().browse(URI(url))
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
                    appState = appState,
                    onFileDropped = { file ->
                        scope.launch {
                            appState = appState.copy(
                                analysisState = AnalysisState.Loading("Analyzing ${file.name}...")
                            )
                            
                            try {
                                if (!FileUtils.isSupportedFile(file)) {
                                    appState = appState.copy(
                                        analysisState = AnalysisState.Error(
                                            message = "Unsupported file type: ${file.extension}\nSupported: PDF, HTML, HTM, TXT",
                                            retryAction = null
                                        )
                                    )
                                    return@launch
                                }
                                
                                val content = FileUtils.extractTextFromFile(file)
                                val analysis = FinancialAnalyzer.analyzeDocument(file.name, content)
                                
                                appState = appState.copy(
                                    analysisState = AnalysisState.FinancialAnalysisResult(analysis)
                                )
                            } catch (e: Exception) {
                                appState = appState.copy(
                                    analysisState = AnalysisState.Error(
                                        message = "Error reading file: ${e.message}",
                                        retryAction = null
                                    )
                                )
                            }
                        }
                    },
                    onDragStateChange = { isDragging ->
                        appState = appState.copy(isDragging = isDragging)
                    },
                    onCloseAnalysis = {
                        appState = appState.copy(analysisState = AnalysisState.Idle)
                    },
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

/**
 * Left Panel - Search and Filing List
 */
@Composable
private fun LeftPanel(
    appState: AppState,
    onSearchTextChange: (String) -> Unit,
    onTickerSelected: (TickerEntry) -> Unit,
    onBackToSearch: () -> Unit,
    onQuickAnalyze: (FilingItem) -> Unit,
    onOpenInBrowser: (FilingItem) -> Unit
) {
    Column(
        modifier = Modifier
            .width(AppDimens.SidebarWidth)
            .fillMaxHeight()
            .background(AppColors.Surface)
    ) {
        // App Header
        AppHeader(
            title = "Papyrus",
            subtitle = "SEC Financial Analyzer"
        )
        
        // Search Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
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
                modifier = Modifier.fillMaxWidth().height(AppDimens.ProgressBarHeight),
                color = AppColors.Primary,
                backgroundColor = AppColors.PrimaryLight
            )
        }
        
        // Content
        if (appState.selectedTicker == null) {
            // Search Results
            SearchResultsList(
                results = appState.searchResults,
                onTickerSelected = onTickerSelected
            )
        } else {
            // Company Detail & Filings
            CompanyFilingsPanel(
                ticker = appState.selectedTicker,
                filings = appState.submissions,
                currentAnalyzingFiling = appState.currentAnalyzingFiling,
                onBackClick = onBackToSearch,
                onQuickAnalyze = onQuickAnalyze,
                onOpenInBrowser = onOpenInBrowser
            )
        }
    }
}

@Composable
private fun SearchResultsList(
    results: List<TickerEntry>,
    onTickerSelected: (TickerEntry) -> Unit
) {
    if (results.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.Search,
            title = "Search for companies",
            description = "Enter a ticker symbol or company name"
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppDimens.PaddingSmall),
            verticalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall)
        ) {
            items(results) { ticker ->
                TickerCard(
                    ticker = ticker,
                    onClick = { onTickerSelected(ticker) }
                )
            }
        }
    }
}

@Composable
private fun CompanyFilingsPanel(
    ticker: TickerEntry,
    filings: List<FilingItem>,
    currentAnalyzingFiling: String?,
    onBackClick: () -> Unit,
    onQuickAnalyze: (FilingItem) -> Unit,
    onOpenInBrowser: (FilingItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Company Info Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.PaddingMedium)
        ) {
            CompanyInfoCard(
                ticker = ticker,
                onBackClick = onBackClick
            )
        }
        
        Divider(color = AppColors.Divider)
        
        // Section Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.PaddingMedium, vertical = AppDimens.PaddingSmall)
        ) {
            SectionHeader(
                title = "SEC Filings",
                icon = Icons.Outlined.Description,
                action = {
                    Text(
                        text = "${filings.size} filings",
                        style = AppTypography.Caption,
                        color = AppColors.OnSurfaceSecondary
                    )
                }
            )
        }
        
        // Filings List
        if (filings.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.FolderOff,
                title = "No filings found",
                description = "This company has no recent SEC filings"
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppDimens.PaddingSmall),
                verticalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall)
            ) {
                items(filings) { filing ->
                    FilingCard(
                        filing = filing,
                        cik = ticker.cik.toString(),
                        onOpenBrowser = { onOpenInBrowser(filing) },
                        onQuickAnalyze = { onQuickAnalyze(filing) },
                        isAnalyzing = currentAnalyzingFiling == filing.accessionNumber
                    )
                }
            }
        }
    }
}

/**
 * Right Panel - Analysis Results
 */
@Composable
private fun RightPanel(
    appState: AppState,
    onFileDropped: (java.io.File) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    onCloseAnalysis: () -> Unit,
    onOpenInBrowser: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(AppDimens.PaddingLarge)
    ) {
        AnimatedContent(
            targetState = appState.analysisState,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            }
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
                
                is AnalysisState.QuickAnalyzeResult -> {
                    QuickAnalyzeResultView(
                        documentTitle = state.documentTitle,
                        documentUrl = state.documentUrl,
                        analysisContent = state.content,
                        analysisSummary = state.summary,
                        onClose = onCloseAnalysis,
                        onOpenInBrowser = state.documentUrl?.let { url ->
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

/**
 * Build analysis summary from document content
 */
private fun buildAnalysisSummary(rawHtml: String, cleanText: String): String {
    val hasRevenue = cleanText.contains("Revenue", ignoreCase = true)
    val hasRisk = cleanText.contains("Risk", ignoreCase = true)
    val hasNetIncome = cleanText.contains("Net Income", ignoreCase = true)
    val hasAssets = cleanText.contains("Total Assets", ignoreCase = true)
    val hasLiabilities = cleanText.contains("Liabilities", ignoreCase = true)
    
    return buildString {
        appendLine("📊 Quick Analysis Summary")
        appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLine()
        appendLine("📄 Document Size: ${rawHtml.length.formatWithCommas()} characters")
        appendLine()
        appendLine("🔍 Key Financial Terms Found:")
        appendLine("  ${if (hasRevenue) "✅" else "❌"} Revenue")
        appendLine("  ${if (hasNetIncome) "✅" else "❌"} Net Income")
        appendLine("  ${if (hasAssets) "✅" else "❌"} Total Assets")
        appendLine("  ${if (hasLiabilities) "✅" else "❌"} Liabilities")
        appendLine("  ${if (hasRisk) "✅" else "❌"} Risk Factors")
        appendLine()
        appendLine("💡 Tip: For detailed analysis, use the Full Content tab")
    }
}

private fun Int.formatWithCommas(): String {
    return String.format("%,d", this)
}

/**
 * Application State
 */
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

/**
 * Analysis State - sealed class for different states
 */
sealed class AnalysisState {
    object Idle : AnalysisState()
    
    data class Loading(
        val message: String = "Loading..."
    ) : AnalysisState()
    
    data class QuickAnalyzeResult(
        val documentTitle: String,
        val documentUrl: String?,
        val content: String,
        val summary: String
    ) : AnalysisState()
    
    data class FinancialAnalysisResult(
        val analysis: FinancialAnalysis
    ) : AnalysisState()
    
    data class Error(
        val message: String,
        val retryAction: (() -> Unit)?
    ) : AnalysisState()
}
