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
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
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
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    // 북마크 상태
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
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppColors.Background
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Panel: Search & Navigation
                LeftPanel(
                    appState = appState,
                    bookmarks = bookmarks,
                    onSearchTextChange = { query ->
                        appState = appState.copy(
                            searchText = query,
                            searchResults = SecApi.searchTicker(query)
                        )
                    },
                    onTickerSelected = { ticker ->
                        scope.launch {
                            // 최근 조회 기록 추가
                            BookmarkManager.addToRecentlyViewed(ticker.cik)
                            recentlyViewedCiks = BookmarkManager.getRecentlyViewed()
                            
                            appState = appState.copy(
                                selectedTicker = ticker,
                                searchText = "",
                                searchResults = emptyList(),
                                isLoading = true,
                                isLoadingNews = true,
                                analysisState = AnalysisState.Idle,
                                companyNews = null
                            )
                            
                            // 병렬로 제출 데이터와 뉴스를 가져옴
                            val submissionsDeferred = async {
                                val sub = SecApi.getSubmissions(ticker.cik)
                                sub?.filings?.recent?.let { SecApi.transformFilings(it) } ?: emptyList()
                            }
                            
                            val newsDeferred = async {
                                NewsApi.getCompanyNews(ticker.ticker, ticker.title)
                            }
                            
                            val filings = submissionsDeferred.await()
                            val news = newsDeferred.await()
                            
                            appState = appState.copy(
                                submissions = filings,
                                companyNews = news,
                                isLoading = false,
                                isLoadingNews = false
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
                            // CIK로 티커 정보 찾기
                            val ticker = SecApi.searchTicker("").find { it.cik == cik }
                                ?: bookmarks.find { it.cik == cik }?.let { 
                                    TickerEntry(it.cik, it.ticker, it.companyName) 
                                }
                            
                            if (ticker != null) {
                                BookmarkManager.addToRecentlyViewed(cik)
                                recentlyViewedCiks = BookmarkManager.getRecentlyViewed()
                                
                                appState = appState.copy(
                                    selectedTicker = ticker,
                                    searchText = "",
                                    searchResults = emptyList(),
                                    isLoading = true,
                                    isLoadingNews = true,
                                    analysisState = AnalysisState.Idle,
                                    companyNews = null
                                )
                                
                                // 병렬로 제출 데이터와 뉴스를 가져옴
                                val submissionsDeferred = async {
                                    val sub = SecApi.getSubmissions(cik)
                                    sub?.filings?.recent?.let { SecApi.transformFilings(it) } ?: emptyList()
                                }
                                
                                val newsDeferred = async {
                                    NewsApi.getCompanyNews(ticker.ticker, ticker.title)
                                }
                                
                                val filings = submissionsDeferred.await()
                                val news = newsDeferred.await()
                                
                                appState = appState.copy(
                                    submissions = filings,
                                    companyNews = news,
                                    isLoading = false,
                                    isLoadingNews = false
                                )
                            }
                        }
                    },
                    onRemoveBookmark = { cik ->
                        BookmarkManager.removeBookmark(cik)
                        bookmarks = BookmarkManager.getAllBookmarks()
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
                                analysisState = AnalysisState.Loading("SEC 문서를 분석하고 있습니다..."),
                                currentAnalyzingFiling = filing.accessionNumber
                            )
                            
                            try {
                                val rawHtml = SecApi.fetchDocumentContent(url)
                                
                                // AI 분석 메시지 업데이트
                                if (AiAnalysisService.isConfigured()) {
                                    appState = appState.copy(
                                        analysisState = AnalysisState.Loading("AI로 심층 분석 중...")
                                    )
                                }
                                
                                // AI 지원 재무 분석 사용
                                val analysis = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    FinancialAnalyzer.analyzeWithAI(
                                        filing.primaryDocument, 
                                        rawHtml
                                    )
                                }
                                
                                appState = appState.copy(
                                    analysisState = AnalysisState.FinancialAnalysisResult(analysis),
                                    currentAnalyzingFiling = null
                                )
                            } catch (e: Exception) {
                                appState = appState.copy(
                                    analysisState = AnalysisState.Error(
                                        message = "문서 분석 실패: ${e.message}",
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
                    },
                    onSettingsClick = {
                        showSettingsDialog = true
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
                                // 초보자 친화적 분석 기능 사용
                                val analysis = FinancialAnalyzer.analyzeForBeginners(file.name, content)
                                
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
            
            // 설정 다이얼로그
            if (showSettingsDialog) {
                SettingsDialog(
                    onDismiss = { showSettingsDialog = false }
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
    bookmarks: List<BookmarkedTicker>,
    onSearchTextChange: (String) -> Unit,
    onTickerSelected: (TickerEntry) -> Unit,
    onBookmarkClick: (TickerEntry) -> Unit,
    onBookmarkedTickerClick: (Int) -> Unit,
    onRemoveBookmark: (Int) -> Unit,
    onBackToSearch: () -> Unit,
    onQuickAnalyze: (FilingItem) -> Unit,
    onOpenInBrowser: (FilingItem) -> Unit,
    onSettingsClick: () -> Unit
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
            subtitle = "SEC Financial Analyzer",
            onSettingsClick = onSettingsClick
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
            // Bookmarks and Search Results
            Column(modifier = Modifier.fillMaxSize()) {
                // 북마크 섹션 (검색어가 비어있을 때만 표시)
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
                    showEmptyState = appState.searchText.isEmpty() && bookmarks.isEmpty()
                )
            }
        } else {
            // Company Detail & Filings
            CompanyFilingsPanel(
                ticker = appState.selectedTicker,
                filings = appState.submissions,
                currentAnalyzingFiling = appState.currentAnalyzingFiling,
                isBookmarked = BookmarkManager.isBookmarked(appState.selectedTicker.cik),
                companyNews = appState.companyNews,
                isLoadingNews = appState.isLoadingNews,
                onBackClick = onBackToSearch,
                onBookmarkClick = { onBookmarkClick(appState.selectedTicker) },
                onQuickAnalyze = onQuickAnalyze,
                onOpenInBrowser = onOpenInBrowser,
                onOpenNewsInBrowser = { url ->
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(URI(url))
                    }
                }
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
    isBookmarked: Boolean,
    companyNews: CompanyNews?,
    isLoadingNews: Boolean,
    onBackClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onQuickAnalyze: (FilingItem) -> Unit,
    onOpenInBrowser: (FilingItem) -> Unit,
    onOpenNewsInBrowser: (String) -> Unit
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
                isBookmarked = isBookmarked,
                onBackClick = onBackClick,
                onBookmarkClick = onBookmarkClick
            )
        }
        
        Divider(color = AppColors.Divider)
        
        // 탭 선택
        var selectedTab by remember { mutableStateOf(0) }
        TabRow(
            selectedTabIndex = selectedTab,
            backgroundColor = AppColors.Surface,
            contentColor = AppColors.Primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("SEC Filings (${filings.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("News${if (companyNews != null) " (${companyNews.articles.size})" else ""}") }
            )
        }
        
        Divider(color = AppColors.Divider)
        
        // 탭 컨텐츠
        when (selectedTab) {
            0 -> {
                // SEC Filings
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
            1 -> {
                // News
                if (isLoadingNews) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = AppColors.Primary)
                            Text(
                                text = "뉴스를 불러오는 중...",
                                style = AppTypography.Body1,
                                color = AppColors.OnSurfaceSecondary
                            )
                        }
                    }
                } else if (companyNews != null) {
                    NewsArticleList(
                        news = companyNews,
                        onOpenInBrowser = onOpenNewsInBrowser
                    )
                } else {
                    EmptyState(
                        icon = Icons.Outlined.Newspaper,
                        title = "뉴스를 불러올 수 없습니다",
                        description = "잠시 후 다시 시도해주세요"
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
    val currentAnalyzingFiling: String? = null,
    val companyNews: CompanyNews? = null,
    val isLoadingNews: Boolean = false
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
