package papyrus

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Desktop
import java.net.URI
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    val scope = rememberCoroutineScope()
    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<TickerEntry>>(emptyList()) }
    var selectedTicker by remember { mutableStateOf<TickerEntry?>(null) }
    var submissions by remember { mutableStateOf<List<FilingItem>>(emptyList()) }
    var displayedContent by remember { mutableStateOf<String?>(null) }
    var analysisSummary by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var financialAnalysis by remember { mutableStateOf<FinancialAnalysis?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        SecApi.loadTickers()
        isLoading = false
    }

    MaterialTheme {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Panel: Search & Holdings
            Column(
                    modifier = Modifier.width(400.dp).fillMaxHeight().background(Color(0xFFF5F5F5))
            ) {
                TopAppBar(
                        title = { Text("Papyrus SEC Analyzer") },
                        backgroundColor = Color(0xFF6200EE),
                        contentColor = Color.White
                )

                // Search Box
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                            value = searchText,
                            onValueChange = {
                                searchText = it
                                searchResults = SecApi.searchTicker(it)
                            },
                            label = { Text("Search Company / Ticker") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().background(Color.White)
                    )
                }

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (selectedTicker == null) {
                    LazyColumn(modifier = Modifier.padding(horizontal = 8.dp)) {
                        items(searchResults) { ticker ->
                            TickerRow(ticker) {
                                selectedTicker = ticker
                                searchText = ""
                                searchResults = emptyList()
                                isLoading = true
                                scope.launch {
                                    val sub = SecApi.getSubmissions(ticker.cik)
                                    submissions =
                                            sub?.filings?.recent?.let {
                                                SecApi.transformFilings(it)
                                            }
                                                    ?: emptyList()
                                    isLoading = false
                                }
                            }
                        }
                    }
                } else {
                    // Show selected ticker info header
                    Column(
                            modifier =
                                    Modifier.padding(16.dp)
                                            .fillMaxWidth()
                                            .background(
                                                    Color.White,
                                                    shape = MaterialTheme.shapes.medium
                                            )
                                            .padding(16.dp)
                    ) {
                        Text(
                                selectedTicker?.title ?: "",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.h6
                        )
                        Text(
                                "CIK: ${selectedTicker?.cik}",
                                style = MaterialTheme.typography.caption
                        )
                        Text(
                                "Ticker: ${selectedTicker?.ticker}",
                                style = MaterialTheme.typography.caption
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                                onClick = {
                                    selectedTicker = null
                                    submissions = emptyList()
                                    displayedContent = null
                                    analysisSummary = null
                                }
                        ) { Text("Back to Search") }
                    }

                    Divider()

                    LazyColumn(modifier = Modifier.padding(horizontal = 8.dp)) {
                        items(submissions) { filing ->
                            FilingRow(
                                    filing = filing,
                                    cik = selectedTicker!!.cik.toString(),
                                    onAnalyze = { url ->
                                        scope.launch {
                                            isLoading = true
                                            displayedContent = "Loading content..."
                                            val rawHtml = SecApi.fetchDocumentContent(url)
                                            // Simple heuristic strip
                                            val text =
                                                    rawHtml.replace(Regex("<[^>]*>"), " ")
                                                            .replace(Regex("\\s+"), " ")
                                                            .take(10000)
                                            displayedContent = text
                                            analysisSummary =
                                                    "Analysis Summary:\nLength: ${rawHtml.length} chars\nContains 'Revenue': ${text.contains("Revenue", true)}\nContains 'Risk': ${text.contains("Risk", true)}\nContains 'Net Income': ${text.contains("Net Income", true)}"
                                            isLoading = false
                                        }
                                    }
                            )
                        }
                    }
                }
            }

            // Right Panel: Analysis / Details with Drag & Drop
            Column(
                    modifier =
                            Modifier.weight(1f)
                                    .fillMaxHeight()
                                    .background(Color.White)
                                    .padding(24.dp)
            ) {
                if (financialAnalysis != null) {
                    // Show financial analysis
                    FinancialAnalysisView(
                            analysis = financialAnalysis!!,
                            onClose = {
                                financialAnalysis = null
                                displayedContent = null
                                analysisSummary = null
                            }
                    )
                } else if (displayedContent != null) {
                    // Show regular document analysis
                    Text(
                            "Document Analysis",
                            style = MaterialTheme.typography.h4,
                            color = Color(0xFF6200EE)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (analysisSummary != null) {
                        Card(
                                backgroundColor = Color(0xFFE8EAF6),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) { Text(analysisSummary!!, modifier = Modifier.padding(16.dp)) }
                    }
                    Divider()
                    Text(
                            "Preview (First 10k chars):",
                            style = MaterialTheme.typography.subtitle1,
                            modifier = Modifier.padding(vertical = 8.dp)
                    )

                    val scrollState = rememberScrollState()
                    Text(
                            displayedContent!!,
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.verticalScroll(scrollState)
                    )
                } else {
                    // Show drag & drop area
                    DragDropArea(
                            isDragging = isDragging,
                            onDragStateChange = { isDragging = it },
                            onFileDropped = { file ->
                                scope.launch {
                                    isLoading = true
                                    try {
                                        // Check if file type is supported
                                        if (!FileUtils.isSupportedFile(file)) {
                                            analysisSummary =
                                                    "⚠️ Unsupported file type: ${file.extension}\nSupported types: PDF, HTML, HTM, TXT"
                                            return@launch
                                        }

                                        // Extract text based on file type
                                        val content = FileUtils.extractTextFromFile(file)
                                        val analysis =
                                                FinancialAnalyzer.analyzeDocument(
                                                        file.name,
                                                        content
                                                )
                                        financialAnalysis = analysis
                                    } catch (e: Exception) {
                                        analysisSummary = "Error reading file: ${e.message}"
                                        e.printStackTrace()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun TickerRow(ticker: TickerEntry, onClick: () -> Unit) {
    Card(
            modifier =
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
            elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(ticker.ticker, fontWeight = FontWeight.Bold, color = Color(0xFF6200EE))
            Text(ticker.title, style = MaterialTheme.typography.body2)
        }
    }
}

@Composable
fun FilingRow(filing: FilingItem, cik: String, onAnalyze: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = 1.dp) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                            filing.form,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3700B3),
                            modifier = Modifier.width(60.dp)
                    )
                    Text(filing.filingDate, style = MaterialTheme.typography.caption)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(filing.description, maxLines = 2, style = MaterialTheme.typography.body2)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(
                        onClick = {
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
                        modifier = Modifier.height(36.dp)
                ) { Text("Open Browser") }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                        onClick = {
                            val url =
                                    SecApi.getDocumentUrl(
                                            cik,
                                            filing.accessionNumber,
                                            filing.primaryDocument
                                    )
                            onAnalyze(url)
                        },
                        modifier = Modifier.height(36.dp)
                ) { Text("Quick Analyze") }
            }
        }
    }
}

@Composable
fun DragDropArea(
        isDragging: Boolean,
        onDragStateChange: (Boolean) -> Unit,
        onFileDropped: (java.io.File) -> Unit
) {
    val interactionSource = remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }

    Box(
            modifier =
                    Modifier.fillMaxSize()
                            .background(
                                    if (isDragging) Color(0xFFE3F2FD) else Color(0xFFFAFAFA),
                                    shape = MaterialTheme.shapes.large
                            )
                            .padding(32.dp),
            contentAlignment = Alignment.Center
    ) {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = if (isDragging) Color(0xFF2196F3) else Color.LightGray
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                    if (isDragging) "Drop SEC Report Here" else "Drag & Drop SEC Report",
                    style = MaterialTheme.typography.h4,
                    color = if (isDragging) Color(0xFF2196F3) else Color.Gray,
                    fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                    "Supports: .pdf, .html, .htm, .txt files",
                    style = MaterialTheme.typography.body1,
                    color = Color.Gray
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Or button for file selection
            Button(
                    onClick = {
                        // Open file chooser
                        val fileChooser = javax.swing.JFileChooser()
                        fileChooser.fileFilter =
                                javax.swing.filechooser.FileNameExtensionFilter(
                                        "SEC Reports (*.pdf, *.html, *.htm, *.txt)",
                                        "pdf",
                                        "html",
                                        "htm",
                                        "txt"
                                )
                        val result = fileChooser.showOpenDialog(null)
                        if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                            onFileDropped(fileChooser.selectedFile)
                        }
                    },
                    modifier = Modifier.height(48.dp).width(200.dp)
            ) { Text("Or Browse Files", fontSize = 16.sp) }
        }
    }

    // Enable drag and drop
    DisposableEffect(Unit) {
        val dropTarget =
                object : java.awt.dnd.DropTarget() {
                    override fun drop(dtde: java.awt.dnd.DropTargetDropEvent) {
                        try {
                            dtde.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY)
                            val droppedFiles =
                                    dtde.transferable.getTransferData(
                                            java.awt.datatransfer.DataFlavor.javaFileListFlavor
                                    ) as
                                            List<*>

                            droppedFiles.firstOrNull()?.let { file ->
                                if (file is java.io.File) {
                                    onFileDropped(file)
                                }
                            }
                            dtde.dropComplete(true)
                            onDragStateChange(false)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            dtde.dropComplete(false)
                        }
                    }

                    override fun dragEnter(dtde: java.awt.dnd.DropTargetDragEvent) {
                        onDragStateChange(true)
                    }

                    override fun dragExit(dte: java.awt.dnd.DropTargetEvent) {
                        onDragStateChange(false)
                    }
                }

        onDispose {
            // Cleanup if needed
        }
    }
}

@Composable
fun FinancialAnalysisView(analysis: FinancialAnalysis, onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header with close button
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                    "Financial Analysis",
                    style = MaterialTheme.typography.h4,
                    color = Color(0xFF6200EE),
                    fontWeight = FontWeight.Bold
            )
            OutlinedButton(onClick = onClose) { Text("✕ Close") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // File info card
        Card(
                backgroundColor = Color(0xFFF5F5F5),
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        "📄 ${analysis.fileName}",
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.Medium
                )
                if (analysis.companyName != null) {
                    Text("🏢 ${analysis.companyName}", style = MaterialTheme.typography.body2)
                }
                if (analysis.reportType != null) {
                    Text("📋 ${analysis.reportType}", style = MaterialTheme.typography.body2)
                }
                if (analysis.periodEnding != null) {
                    Text("📅 ${analysis.periodEnding}", style = MaterialTheme.typography.body2)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Summary
        Card(backgroundColor = Color(0xFFE8F5E9), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        analysis.summary,
                        style = MaterialTheme.typography.body2,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Detailed metrics
        if (analysis.metrics.isNotEmpty()) {
            Text(
                    "Detailed Metrics (${analysis.metrics.size} found)",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val scrollState = rememberScrollState()
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(analysis.metrics) { metric ->
                    Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            elevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                    metric.name,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6200EE)
                            )
                            Text("Value: ${metric.value}", style = MaterialTheme.typography.body2)
                            if (metric.rawValue != null) {
                                Text(
                                        "Parsed: ${formatCurrency(metric.rawValue)}",
                                        style = MaterialTheme.typography.caption,
                                        color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                    "⚠️ No financial metrics found. The file may not contain standard financial statements.",
                    style = MaterialTheme.typography.body1,
                    color = Color.Gray,
                    modifier = Modifier.padding(32.dp)
            )
        }
    }
}

fun formatCurrency(value: Double): String {
    return when {
        value >= 1_000_000_000 -> String.format("$%.2fB", value / 1_000_000_000)
        value >= 1_000_000 -> String.format("$%.2fM", value / 1_000_000)
        value >= 1_000 -> String.format("$%.2fK", value / 1_000)
        else -> String.format("$%.2f", value)
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Papyrus - SEC Financial Analyzer") { App() }
}
