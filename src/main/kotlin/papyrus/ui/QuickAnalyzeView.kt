package papyrus.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import papyrus.core.model.BeginnerInsight
import papyrus.core.model.ExtendedFinancialMetric
import papyrus.core.model.FinancialAnalysis
import papyrus.core.model.FinancialHealthScore
import papyrus.core.model.FinancialMetric
import papyrus.core.model.FinancialRatio
import papyrus.core.model.FinancialTermExplanation
import papyrus.core.model.HealthStatus
import papyrus.core.model.MetricCategory
import papyrus.core.model.RatioCategory
import papyrus.core.model.XbrlCompanyFact
import papyrus.core.network.SecApi
import papyrus.core.service.parser.XbrlCompanyFactsExtractor

private val uiEmojiMarkers =
        listOf(
                "✅",
                "⚠️",
                "⚠",
                "📌",
                "📊",
                "📈",
                "📋",
                "🔍",
                "✨",
                "🚀",
                "⭐",
                "💡",
                "🏢",
                "💰",
                "⚖️",
                "💵",
                "💧",
                "🏦",
                "👤",
                "⚙️",
                "📜",
                "🏃",
                "💻",
                "🌍",
                "🌐"
        )

private fun sanitizeUiText(text: String): String {
        var result = text
        for (marker in uiEmojiMarkers) {
                result = result.replace(marker, "")
        }
        return result.replace("\uFE0F", "")
}

/** Helper function to format currency values */
private fun formatCurrency(value: Double): String {
        return when {
                value >= 1_000_000_000 -> String.format("$%.2fB", value / 1_000_000_000)
                value >= 1_000_000 -> String.format("$%.2fM", value / 1_000_000)
                value >= 1_000 -> String.format("$%.2fK", value / 1_000)
                else -> String.format("$%.2f", value)
        }
}

/** Benchmark information for financial ratios */
private data class BenchmarkInfo(
        val benchmarkText: String,
        val investorPerspective: String,
        val example: String = ""
)

/** Get benchmark information for a given ratio */
private fun getBenchmarkInfo(ratioName: String): BenchmarkInfo? {
        return when {
                ratioName.contains("매출총이익률") || ratioName.contains("Gross Margin") ->
                        BenchmarkInfo(
                                benchmarkText =
                                        "• 제조업: 25-40%\n• 소프트웨어/기술: 60-80%\n• 소매업: 20-35%\n• 제약/바이오: 70-85%",
                                investorPerspective =
                                        "높은 매출총이익률은 제품 차별화와 가격 경쟁력을 나타냅니다. 특히 기술 기업의 경우 70% 이상이면 매우 우수한 수준입니다.",
                                example = "Apple의 매출총이익률은 약 43% (2023), Microsoft는 약 69% 수준입니다."
                        )
                ratioName.contains("영업이익률") || ratioName.contains("Operating Margin") ->
                        BenchmarkInfo(
                                benchmarkText =
                                        "• S&P 500 평균: 10-12%\n• 기술 기업: 15-30%\n• 금융: 25-35%\n• 소매: 3-8%",
                                investorPerspective =
                                        "영업이익률이 15% 이상이면 효율적인 운영 구조를 갖춘 것으로 평가됩니다. 경쟁이 치열한 산업에서는 5-10%도 양호한 수준입니다.",
                                example = "Google의 영업이익률은 약 27% (2023), Amazon은 약 5% 수준입니다."
                        )
                ratioName.contains("순이익률") || ratioName.contains("Net Profit Margin") ->
                        BenchmarkInfo(
                                benchmarkText =
                                        "• 산업 평균: 5-10%\n• 우수 기업: 15-20%\n• 고성장 기업: 3-8%\n• 성숙 기업: 10-15%",
                                investorPerspective =
                                        "10% 이상이면 건강한 수익 창출 능력을 의미합니다. 성장기 기업은 재투자로 인해 낮을 수 있으나, 안정기 기업은 높아야 합니다.",
                                example = "Tesla의 순이익률은 약 15% (2023), Walmart는 약 2.4% 수준입니다."
                        )
                ratioName.contains("ROE") || ratioName.contains("자기자본이익률") ->
                        BenchmarkInfo(
                                benchmarkText =
                                        "• 우수: 15-20% 이상\n• 양호: 10-15%\n• 평균: 7-10%\n• 주의: 7% 미만",
                                investorPerspective =
                                        "Warren Buffett은 ROE 15% 이상을 우량 기업의 기준으로 봅니다. 지속적으로 20% 이상을 유지하는 기업은 매우 드뭅니다.",
                                example = "Coca-Cola의 ROE는 약 40% (2023), JP Morgan은 약 15% 수준입니다."
                        )
                ratioName.contains("ROA") || ratioName.contains("총자산이익률") ->
                        BenchmarkInfo(
                                benchmarkText = "• 우수: 5% 이상\n• 양호: 3-5%\n• 평균: 1-3%\n• 주의: 1% 미만",
                                investorPerspective =
                                        "자산 집약적 산업(제조업, 항공)은 낮고, 자산 경량 산업(소프트웨어, 서비스)은 높습니다. 5% 이상이면 자산을 효율적으로 활용하는 것입니다.",
                                example = "Adobe의 ROA는 약 28% (2023), Ford는 약 1.2% 수준입니다."
                        )
                ratioName.contains("유동비율") || ratioName.contains("Current Ratio") ->
                        BenchmarkInfo(
                                benchmarkText =
                                        "• 안전: 1.5-3.0\n• 최소: 1.0 이상\n• 주의: 1.0 미만\n• 과다: 3.0 초과 (비효율 가능)",
                                investorPerspective =
                                        "1.5-2.0이 이상적입니다. 너무 높으면 자산을 효율적으로 활용하지 못하는 것일 수 있고, 1.0 미만이면 단기 지급 능력에 문제가 있을 수 있습니다.",
                                example = "일반적으로 건강한 기업은 1.5-2.5 범위를 유지합니다."
                        )
                ratioName.contains("당좌비율") || ratioName.contains("Quick Ratio") ->
                        BenchmarkInfo(
                                benchmarkText = "• 안전: 1.0 이상\n• 최소: 0.5-1.0\n• 주의: 0.5 미만",
                                investorPerspective =
                                        "재고를 제외한 즉시 현금화 가능 자산으로 단기 부채를 갚을 수 있는지 측정합니다. 1.0 이상이면 안정적입니다.",
                                example = "기술 기업은 재고가 적어 당좌비율이 유동비율과 비슷합니다."
                        )
                ratioName.contains("부채비율") ||
                        ratioName.contains("Debt to Equity") && !ratioName.contains("Debt Ratio") ->
                        BenchmarkInfo(
                                benchmarkText =
                                        "• 안전: 50% 이하\n• 평균: 50-150%\n• 주의: 150-200%\n• 위험: 200% 초과",
                                investorPerspective =
                                        "산업마다 다르지만 100% 이하가 일반적으로 안전합니다. 금융업은 높을 수 있으나, 제조업은 낮아야 합니다.",
                                example = "Tesla의 부채비율은 약 17% (2023), AT&T는 약 120% 수준입니다."
                        )
                ratioName.contains("총자산회전율") || ratioName.contains("Asset Turnover") ->
                        BenchmarkInfo(
                                benchmarkText =
                                        "• 소매업: 2-3회\n• 제조업: 0.5-1.5회\n• 서비스업: 1-2회\n• 자본집약 산업: 0.3-0.8회",
                                investorPerspective =
                                        "높을수록 자산을 효율적으로 활용해 매출을 창출하는 것입니다. 산업 특성에 따라 큰 차이가 있습니다.",
                                example = "Walmart의 총자산회전율은 약 2.4회, ExxonMobil은 약 0.9회입니다."
                        )
                ratioName.contains("이자보상배율") || ratioName.contains("Interest Coverage") ->
                        BenchmarkInfo(
                                benchmarkText =
                                        "• 매우 안전: 8배 이상\n• 안전: 4-8배\n• 평균: 2.5-4배\n• 위험: 1.5배 미만",
                                investorPerspective =
                                        "영업이익으로 이자비용을 몇 번 갚을 수 있는지 나타냅니다. 2.5배 미만이면 부채 상환 능력에 주의가 필요합니다.",
                                example = "건강한 기업은 최소 5배 이상을 유지합니다."
                        )
                else -> null
        }
}

/** Enhanced Quick Analyze Result View Shows analysis results in a structured, modern UI */
@Composable
fun QuickAnalyzeResultView(
        documentTitle: String,
        documentUrl: String? = null,
        analysisContent: String,
        analysisSummary: String,
        onClose: () -> Unit,
        onOpenInBrowser: (() -> Unit)? = null,
        modifier: Modifier = Modifier
) {
        var selectedTab by remember { mutableStateOf(0) }
        val tabs = listOf("Summary", "Full Content")

        Column(modifier = modifier.fillMaxSize()) {
                // Header
                QuickAnalyzeHeader(
                        title = "Document Analysis",
                        documentTitle = documentTitle,
                        onClose = onClose,
                        onOpenInBrowser = onOpenInBrowser
                )

                Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

                // Tab Row
                AnalysisTabRow(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        tabs = tabs
                )

                Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

                // Content based on selected tab
                when (selectedTab) {
                        0 -> QuickAnalyzeSummaryTab(analysisSummary)
                        1 -> QuickAnalyzeContentTab(analysisContent)
                }
        }
}

@Composable
private fun QuickAnalyzeHeader(
        title: String,
        documentTitle: String,
        onClose: () -> Unit,
        onOpenInBrowser: (() -> Unit)?
) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
        ) {
                Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = title,
                                style = AppTypography.Headline2,
                                color = AppColors.Primary,
                                fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                        Icons.Outlined.Description,
                                        contentDescription = null,
                                        tint = AppColors.OnSurfaceSecondary,
                                        modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                        text = documentTitle,
                                        style = AppTypography.Body2,
                                        color = AppColors.OnSurfaceSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )
                        }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (onOpenInBrowser != null) {
                                OutlinedButton(
                                        onClick = onOpenInBrowser,
                                        colors =
                                                ButtonDefaults.outlinedButtonColors(
                                                        contentColor = AppColors.Primary
                                                ),
                                        shape = AppShapes.Small
                                ) {
                                        Icon(
                                                Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Open")
                                }
                        }

                        OutlinedButton(
                                onClick = onClose,
                                colors =
                                        ButtonDefaults.outlinedButtonColors(
                                                contentColor = AppColors.OnSurfaceSecondary
                                        ),
                                shape = AppShapes.Small
                        ) {
                                Icon(
                                        Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Close")
                        }
                }
        }
}

@Composable
private fun QuickAnalyzeSummaryTab(summary: String) {
        val scrollState = rememberScrollState()

        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                // Analysis Stats Cards
                AnalysisStatsRow(summary)

                Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

                // Summary Content Card
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = AppDimens.CardElevation,
                        shape = AppShapes.Medium,
                        backgroundColor = AppColors.Surface
                ) {
                        Column(modifier = Modifier.padding(AppDimens.PaddingMedium)) {
                                SectionHeader(
                                        title = "Analysis Summary",
                                        icon = Icons.Outlined.Insights
                                )

                                Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                                Text(
                                        text = summary,
                                        style = AppTypography.Monospace,
                                        color = AppColors.OnSurface
                                )
                        }
                }
        }
}

@Composable
private fun AnalysisStatsRow(summary: String) {
        val hasRevenue = summary.contains("Revenue", ignoreCase = true)
        val hasRisk = summary.contains("Risk", ignoreCase = true)
        val hasNetIncome = summary.contains("Net Income", ignoreCase = true)

        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall)
        ) {
                StatCard(
                        title = "Revenue",
                        isFound = hasRevenue,
                        icon = Icons.Outlined.AttachMoney,
                        color = AppColors.Revenue,
                        modifier = Modifier.weight(1f)
                )

                StatCard(
                        title = "Net Income",
                        isFound = hasNetIncome,
                        icon = Icons.AutoMirrored.Outlined.TrendingUp,
                        color = AppColors.Income,
                        modifier = Modifier.weight(1f)
                )

                StatCard(
                        title = "Risk Factors",
                        isFound = hasRisk,
                        icon = Icons.Outlined.Warning,
                        color = AppColors.Warning,
                        modifier = Modifier.weight(1f)
                )
        }
}

@Composable
private fun StatCard(
        title: String,
        isFound: Boolean,
        icon: ImageVector,
        color: Color,
        modifier: Modifier = Modifier
) {
        Card(
                modifier = modifier,
                elevation = AppDimens.CardElevation,
                shape = AppShapes.Medium,
                backgroundColor =
                        if (isFound) color.copy(alpha = 0.1f) else AppColors.SurfaceVariant
        ) {
                Column(
                        modifier = Modifier.padding(AppDimens.PaddingMedium),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        Icon(
                                icon,
                                contentDescription = null,
                                tint = if (isFound) color else AppColors.Divider,
                                modifier = Modifier.size(28.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                                text = title,
                                style = AppTypography.Caption,
                                color = AppColors.OnSurfaceSecondary,
                                textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                                modifier =
                                        Modifier.background(
                                                        if (isFound) color else AppColors.Divider,
                                                        shape = AppShapes.Pill
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                                Text(
                                        text = if (isFound) "Found" else "Not Found",
                                        style = AppTypography.Caption,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 10.sp
                                )
                        }
                }
        }
}

@Composable
private fun QuickAnalyzeContentTab(content: String) {
        val scrollState = rememberScrollState()
        var copySuccess by remember { mutableStateOf(false) }

        Card(
                modifier = Modifier.fillMaxSize(),
                elevation = AppDimens.CardElevation,
                shape = AppShapes.Medium,
                backgroundColor = AppColors.Surface
        ) {
                Column(modifier = Modifier.padding(AppDimens.PaddingMedium)) {
                        SectionHeader(
                                title = "Document Content Preview",
                                icon = Icons.AutoMirrored.Outlined.Article,
                                action = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                        text = "${content.length} characters",
                                                        style = AppTypography.Caption,
                                                        color = AppColors.OnSurfaceSecondary
                                                )

                                                Spacer(
                                                        modifier =
                                                                Modifier.width(
                                                                        AppDimens.PaddingMedium
                                                                )
                                                )

                                                // Copy to clipboard button
                                                Button(
                                                        onClick = {
                                                                try {
                                                                        val clipboard =
                                                                                java.awt.Toolkit
                                                                                        .getDefaultToolkit()
                                                                                        .systemClipboard
                                                                        val stringSelection =
                                                                                java.awt
                                                                                        .datatransfer
                                                                                        .StringSelection(
                                                                                                content
                                                                                        )
                                                                        clipboard.setContents(
                                                                                stringSelection,
                                                                                null
                                                                        )
                                                                        copySuccess = true
                                                                } catch (e: Exception) {
                                                                        println(
                                                                                "Failed to copy to clipboard: ${e.message}"
                                                                        )
                                                                }
                                                        },
                                                        colors =
                                                                ButtonDefaults.buttonColors(
                                                                        backgroundColor =
                                                                                AppColors.Primary
                                                                ),
                                                        modifier = Modifier.height(32.dp)
                                                ) {
                                                        Icon(
                                                                Icons.Default.ContentCopy,
                                                                contentDescription =
                                                                        "Copy to clipboard",
                                                                modifier = Modifier.size(16.dp),
                                                                tint = Color.White
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                                text =
                                                                        if (copySuccess) "Copied!"
                                                                        else "Copy",
                                                                color = Color.White,
                                                                style = AppTypography.Caption
                                                        )
                                                }
                                        }
                                }
                        )

                        Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                        if (copySuccess) {
                                LaunchedEffect(Unit) {
                                        kotlinx.coroutines.delay(2000)
                                        copySuccess = false
                                }
                        }

                        Divider(color = AppColors.Divider)

                        Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                        Box(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .background(
                                                        AppColors.SurfaceVariant,
                                                        shape = AppShapes.Small
                                                )
                                                .padding(AppDimens.PaddingMedium)
                                                .verticalScroll(scrollState)
                        ) {
                                Text(
                                        text = content,
                                        style = AppTypography.Monospace,
                                        color = AppColors.OnSurface
                                )
                        }
                }
        }
}

/**
 * Enhanced Financial Analysis View Used for local file analysis with detailed metrics Now includes
 * beginner-friendly insights and explanations
 */
@Composable
fun FinancialAnalysisPanel(
        analysis: FinancialAnalysis,
        onClose: () -> Unit,
        modifier: Modifier = Modifier
) {
        var selectedTab by remember { mutableStateOf(0) }
        var showGlossaryDialog by remember { mutableStateOf(false) }

        val hasXbrlTab = analysis.xbrlMetrics.isNotEmpty() || analysis.cik != null

        val tabs = buildList {
                if (analysis.beginnerInsights.isNotEmpty() || analysis.healthScore != null) {
                        add("Health Score")
                        add("Insights")
                        add("Ratios")
                        if (hasXbrlTab) add("XBRL")
                        add("Raw Data")
                } else {
                        add("Overview")
                        add("Metrics")
                        if (hasXbrlTab) add("XBRL")
                        add("Raw Data")
                }
        }

        Column(modifier = modifier.fillMaxSize()) {
                // Header
                FinancialAnalysisHeader(
                        analysis = analysis,
                        onGlossaryClick = { showGlossaryDialog = true },
                        onClose = onClose
                )

                Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

                // Tab Row
                AnalysisTabRow(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        tabs = tabs
                )

                Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

                // Content based on selected tab
                when (tabs.getOrNull(selectedTab)) {
                        "Health Score" -> HealthScoreTab(analysis)
                        "Overview" -> FinancialOverviewTab(analysis)
                        "Insights" ->
                                BeginnerInsightsTab(
                                        analysis.beginnerInsights,
                                        analysis.keyTakeaways
                                )
                        "Ratios" -> FinancialRatiosTab(analysis.ratios, analysis.metrics)
                        "Metrics" -> FinancialMetricsTab(analysis.metrics)
                        "XBRL" -> XbrlTab(analysis)
                        "Raw Data" -> FinancialRawDataTab(analysis.rawContent, analysis)
                        else -> FinancialRawDataTab(analysis.rawContent, analysis)
                }

                if (showGlossaryDialog) {
                        GlossaryDialog(
                                terms = analysis.termExplanations,
                                onDismiss = { showGlossaryDialog = false }
                        )
                }
        }
}

@Composable
private fun XbrlTab(analysis: FinancialAnalysis) {
        val scrollState = rememberScrollState()
        val cik = analysis.cik

        var isLoadingFacts by remember { mutableStateOf(false) }
        var factsError by remember { mutableStateOf<String?>(null) }
        var companyFacts by remember { mutableStateOf<List<XbrlCompanyFact>>(emptyList()) }

        LaunchedEffect(cik) {
                companyFacts = emptyList()
                factsError = null

                if (cik == null) return@LaunchedEffect

                isLoadingFacts = true
                try {
                        val facts = SecApi.getCompanyFacts(cik)
                        companyFacts =
                                if (facts != null) {
                                        XbrlCompanyFactsExtractor.extractKeyFacts(facts)
                                } else {
                                        emptyList()
                                }
                } catch (e: Exception) {
                        factsError = e.message ?: "Failed to load company facts"
                } finally {
                        isLoadingFacts = false
                }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                if (analysis.xbrlMetrics.isNotEmpty()) {
                        Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = AppDimens.CardElevationHigh,
                                shape = AppShapes.Large,
                        ) {
                                Column(modifier = Modifier.padding(AppDimens.PaddingLarge)) {
                                        Text(
                                                text = "Inline XBRL (from document)",
                                                style = AppTypography.Headline3,
                                                color = AppColors.OnSurface,
                                                fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                                        analysis.xbrlMetrics.take(50).forEach { metric ->
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween
                                                ) {
                                                        Text(
                                                                text = metric.name,
                                                                style = AppTypography.Body2,
                                                                color = AppColors.OnSurface,
                                                                modifier = Modifier.weight(1f)
                                                        )
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Text(
                                                                text = metric.value,
                                                                style = AppTypography.Body2,
                                                                color = AppColors.OnSurface,
                                                                textAlign = TextAlign.End
                                                        )
                                                }

                                                if (!metric.period.isNullOrBlank() ||
                                                                metric.source.isNotBlank()
                                                ) {
                                                        Text(
                                                                text =
                                                                        buildString {
                                                                                if (!metric.period
                                                                                                .isNullOrBlank()
                                                                                )
                                                                                        append(
                                                                                                "Period: ${metric.period}"
                                                                                        )
                                                                                if (!metric.period
                                                                                                .isNullOrBlank() &&
                                                                                                metric.source
                                                                                                        .isNotBlank()
                                                                                )
                                                                                        append(
                                                                                                " · "
                                                                                        )
                                                                                if (metric.source
                                                                                                .isNotBlank()
                                                                                )
                                                                                        append(
                                                                                                metric.source
                                                                                        )
                                                                        },
                                                                style = AppTypography.Caption,
                                                                color = AppColors.OnSurfaceSecondary
                                                        )
                                                }

                                                Divider(
                                                        color = AppColors.Divider,
                                                        modifier = Modifier.padding(vertical = 8.dp)
                                                )
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))
                }

                if (cik != null) {
                        Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = AppDimens.CardElevationHigh,
                                shape = AppShapes.Large,
                        ) {
                                Column(modifier = Modifier.padding(AppDimens.PaddingLarge)) {
                                        Text(
                                                text = "SEC Company Facts (XBRL)",
                                                style = AppTypography.Headline3,
                                                color = AppColors.OnSurface,
                                                fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                                        if (isLoadingFacts) {
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        CircularProgressIndicator(
                                                                modifier = Modifier.size(18.dp),
                                                                strokeWidth = 2.dp,
                                                                color = AppColors.Primary
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                                text = "Loading company facts...",
                                                                style = AppTypography.Body2,
                                                                color = AppColors.OnSurfaceSecondary
                                                        )
                                                }
                                        } else if (factsError != null) {
                                                Text(
                                                        text = factsError
                                                                        ?: "Failed to load company facts",
                                                        style = AppTypography.Body2,
                                                        color = AppColors.Error
                                                )
                                        } else if (companyFacts.isEmpty()) {
                                                Text(
                                                        text = "No company facts available.",
                                                        style = AppTypography.Body2,
                                                        color = AppColors.OnSurfaceSecondary
                                                )
                                        } else {
                                                companyFacts.forEach { fact ->
                                                        Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement =
                                                                        Arrangement.SpaceBetween
                                                        ) {
                                                                Text(
                                                                        text = fact.label,
                                                                        style = AppTypography.Body2,
                                                                        color = AppColors.OnSurface,
                                                                        modifier =
                                                                                Modifier.weight(1f)
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.width(
                                                                                        12.dp
                                                                                )
                                                                )
                                                                Text(
                                                                        text =
                                                                                fact.value
                                                                                        ?.toString()
                                                                                        ?: "—",
                                                                        style = AppTypography.Body2,
                                                                        color = AppColors.OnSurface,
                                                                        textAlign = TextAlign.End
                                                                )
                                                        }
                                                        Text(
                                                                text =
                                                                        buildString {
                                                                                if (!fact.periodEnd
                                                                                                .isNullOrBlank()
                                                                                )
                                                                                        append(
                                                                                                "End: ${fact.periodEnd}"
                                                                                        )
                                                                                if (!fact.periodEnd
                                                                                                .isNullOrBlank() &&
                                                                                                !fact.unit
                                                                                                        .isNullOrBlank()
                                                                                )
                                                                                        append(
                                                                                                " · "
                                                                                        )
                                                                                if (!fact.unit
                                                                                                .isNullOrBlank()
                                                                                )
                                                                                        append(
                                                                                                "Unit: ${fact.unit}"
                                                                                        )
                                                                                if ((fact.periodEnd !=
                                                                                                null ||
                                                                                                fact.unit !=
                                                                                                        null)
                                                                                )
                                                                                        append(
                                                                                                " · "
                                                                                        )
                                                                                append(fact.concept)
                                                                        },
                                                                style = AppTypography.Caption,
                                                                color = AppColors.OnSurfaceSecondary
                                                        )
                                                        Divider(
                                                                color = AppColors.Divider,
                                                                modifier =
                                                                        Modifier.padding(
                                                                                vertical = 8.dp
                                                                        )
                                                        )
                                                }
                                        }
                                }
                        }
                } else if (analysis.xbrlMetrics.isEmpty()) {
                        EmptyState(
                                icon = Icons.Outlined.DataObject,
                                title = "No XBRL data",
                                description = "This analysis does not include XBRL facts."
                        )
                }
        }
}

/** Financial health score tab - Score card for beginners to understand at a glance */
@Composable
private fun HealthScoreTab(analysis: FinancialAnalysis) {
        val scrollState = rememberScrollState()
        val healthScore = analysis.healthScore

        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                // Health score main card
                if (healthScore != null) {
                        HealthScoreMainCard(healthScore)

                        Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

                        // Strengths and weaknesses
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall)
                        ) {
                                // Strengths card
                                StrengthWeaknessCard(
                                        title = "Strengths",
                                        items = healthScore.strengths,
                                        backgroundColor = AppColors.SuccessLight,
                                        modifier = Modifier.weight(1f)
                                )

                                // Weaknesses card
                                StrengthWeaknessCard(
                                        title = "Needs Improvement",
                                        items = healthScore.weaknesses,
                                        backgroundColor = AppColors.WarningLight,
                                        modifier = Modifier.weight(1f)
                                )
                        }

                        Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

                        // Recommendations
                        if (healthScore.recommendations.isNotEmpty()) {
                                RecommendationsCard(healthScore.recommendations)
                        }
                }

                Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

                // Key takeaways
                if (analysis.keyTakeaways.isNotEmpty()) {
                        KeyTakeawaysCard(analysis.keyTakeaways)
                }

                // Report type explanation
                if (analysis.reportTypeExplanation != null) {
                        Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))
                        ReportTypeCard(analysis.reportType, analysis.reportTypeExplanation)
                }
        }
}

@Composable
private fun HealthScoreMainCard(healthScore: FinancialHealthScore) {
        val scoreColor =
                when {
                        healthScore.overallScore >= 80 -> AppColors.Success
                        healthScore.overallScore >= 60 -> AppColors.Warning
                        else -> AppColors.Error
                }

        Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = AppDimens.CardElevationHigh,
                shape = AppShapes.Large,
                backgroundColor = scoreColor.copy(alpha = 0.1f)
        ) {
                Column(
                        modifier = Modifier.padding(AppDimens.PaddingLarge),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        Text(
                                text = "Financial Health Score",
                                style = AppTypography.Headline3,
                                color = AppColors.OnSurface,
                                fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

                        // Large score display
                        Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                        ) {
                                Text(
                                        text = healthScore.grade,
                                        fontSize = 72.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = scoreColor
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(horizontalAlignment = Alignment.Start) {
                                        Text(
                                                text = "${healthScore.overallScore}",
                                                fontSize = 36.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = scoreColor
                                        )
                                        Text(
                                                text = "/ 100 points",
                                                style = AppTypography.Caption,
                                                color = AppColors.OnSurfaceSecondary
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

                        // Progress bar
                        LinearProgressIndicator(
                                progress = healthScore.overallScore / 100f,
                                modifier =
                                        Modifier.fillMaxWidth().height(12.dp).clip(AppShapes.Pill),
                                color = scoreColor,
                                backgroundColor = scoreColor.copy(alpha = 0.2f)
                        )

                        Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

                        // Summary description
                        Card(
                                backgroundColor = Color.White,
                                elevation = 0.dp,
                                shape = AppShapes.Medium
                        ) {
                                Text(
                                        text = healthScore.summary,
                                        style = AppTypography.Body1,
                                        color = AppColors.OnSurface,
                                        modifier = Modifier.padding(AppDimens.PaddingMedium),
                                        textAlign = TextAlign.Center
                                )
                        }
                }
        }
}

@Composable
private fun StrengthWeaknessCard(
        title: String,
        items: List<String>,
        backgroundColor: Color,
        modifier: Modifier = Modifier
) {
        Card(
                modifier = modifier,
                elevation = AppDimens.CardElevation,
                shape = AppShapes.Medium,
                backgroundColor = backgroundColor
        ) {
                Column(modifier = Modifier.padding(AppDimens.PaddingMedium)) {
                        Text(
                                text = title,
                                style = AppTypography.Subtitle1,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.OnSurface
                        )

                        Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                        if (items.isEmpty()) {
                                Text(
                                        text = "해당 항목 없음",
                                        style = AppTypography.Body2,
                                        color = AppColors.OnSurfaceSecondary
                                )
                        } else {
                                items.forEach { item ->
                                        Text(
                                                text = sanitizeUiText(item).trim(),
                                                style = AppTypography.Body2,
                                                color = AppColors.OnSurface,
                                                modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                }
                        }
                }
        }
}

@Composable
private fun RecommendationsCard(recommendations: List<String>) {
        Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = AppDimens.CardElevation,
                shape = AppShapes.Medium,
                backgroundColor = AppColors.InfoLight
        ) {
                Column(modifier = Modifier.padding(AppDimens.PaddingMedium)) {
                        SectionHeader(title = "투자 팁 & 권장사항", icon = Icons.Outlined.Lightbulb)

                        Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                        recommendations.forEach { recommendation ->
                                Text(
                                        text = sanitizeUiText(recommendation).trim(),
                                        style = AppTypography.Body2,
                                        color = AppColors.OnSurface,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                )
                        }
                }
        }
}

@Composable
private fun KeyTakeawaysCard(takeaways: List<String>) {
        Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = AppDimens.CardElevation,
                shape = AppShapes.Medium,
                backgroundColor = AppColors.PrimaryLight
        ) {
                Column(modifier = Modifier.padding(AppDimens.PaddingMedium)) {
                        SectionHeader(title = "핵심 요점", icon = Icons.Outlined.Star)

                        Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                        takeaways.forEach { takeaway ->
                                Text(
                                        text = sanitizeUiText(takeaway).trim(),
                                        style = AppTypography.Body2,
                                        color = AppColors.OnSurface,
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        fontWeight = FontWeight.Medium
                                )
                        }
                }
        }
}

@Composable
private fun ReportTypeCard(reportType: String?, explanation: String?) {
        if (explanation == null) return

        Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = AppDimens.CardElevation,
                shape = AppShapes.Medium,
                backgroundColor = AppColors.Surface
        ) {
                Column(modifier = Modifier.padding(AppDimens.PaddingMedium)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                        Icons.Outlined.Description,
                                        contentDescription = null,
                                        tint = AppColors.Primary,
                                        modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                        text = "이 보고서는 무엇인가요?",
                                        style = AppTypography.Subtitle1,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.Primary
                                )
                        }

                        Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                        Text(
                                text = sanitizeUiText(explanation).trim(),
                                style = AppTypography.Body2,
                                color = AppColors.OnSurface
                        )
                }
        }
}

/** Beginner insights tab - easy explanations */
@Composable
private fun BeginnerInsightsTab(insights: List<BeginnerInsight>, keyTakeaways: List<String>) {
        if (insights.isEmpty()) {
                EmptyState(
                        icon = Icons.Outlined.Lightbulb,
                        title = "인사이트 분석 중",
                        description = "초보자용 인사이트를 생성하려면 더 많은 재무 데이터가 필요합니다."
                )
        } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(AppDimens.PaddingMedium)) {
                        items(insights) { insight -> BeginnerInsightCard(insight) }
                }
        }
}

@Composable
private fun BeginnerInsightCard(insight: BeginnerInsight) {
        var isExpanded by remember { mutableStateOf(false) }

        Card(
                modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
                elevation = AppDimens.CardElevation,
                shape = AppShapes.Medium,
                backgroundColor = AppColors.Surface
        ) {
                Column(modifier = Modifier.padding(AppDimens.PaddingMedium)) {
                        // Header
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                                imageVector = Icons.Outlined.Lightbulb,
                                                contentDescription = null,
                                                tint = AppColors.Primary,
                                                modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                                Text(
                                                        text = insight.title,
                                                        style = AppTypography.Subtitle1,
                                                        fontWeight = FontWeight.Bold,
                                                        color = AppColors.Primary
                                                )
                                                Text(
                                                        text =
                                                                sanitizeUiText(insight.summary)
                                                                        .trim(),
                                                        style = AppTypography.Body2,
                                                        color = AppColors.OnSurface
                                                )
                                        }
                                }

                                Icon(
                                        if (isExpanded) Icons.Default.ExpandLess
                                        else Icons.Default.ExpandMore,
                                        contentDescription = "자세히 보기",
                                        tint = AppColors.OnSurfaceSecondary
                                )
                        }

                        // Expanded content
                        AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = AppDimens.PaddingMedium)) {
                                        Divider(color = AppColors.Divider)

                                        Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                                        // Detailed explanation
                                        InsightSection(
                                                title = "상세 설명",
                                                content = insight.detailedExplanation,
                                                backgroundColor = AppColors.SurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                                        // What this means
                                        InsightSection(
                                                title = "이게 무슨 뜻이에요?",
                                                content = insight.whatItMeans,
                                                backgroundColor = AppColors.InfoLight
                                        )

                                        Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                                        // Why it's important
                                        InsightSection(
                                                title = "왜 중요한가요?",
                                                content = insight.whyItMatters,
                                                backgroundColor = AppColors.WarningLight
                                        )

                                        Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                                        // Actionable advice
                                        InsightSection(
                                                title = "투자자 팁",
                                                content = insight.actionableAdvice,
                                                backgroundColor = AppColors.SuccessLight
                                        )
                                }
                        }
                }
        }
}

@Composable
private fun InsightSection(title: String, content: String, backgroundColor: Color) {
        Card(backgroundColor = backgroundColor, elevation = 0.dp, shape = AppShapes.Small) {
                Column(modifier = Modifier.padding(AppDimens.PaddingSmall)) {
                        Text(
                                text = title,
                                style = AppTypography.Caption,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.OnSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                                text = sanitizeUiText(content).trim(),
                                style = AppTypography.Body2,
                                color = AppColors.OnSurface
                        )
                }
        }
}

/** Glossary tab */
@Composable
private fun TermGlossaryTab(terms: List<FinancialTermExplanation>, modifier: Modifier = Modifier) {
        if (terms.isEmpty()) {
                Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                                icon = Icons.Outlined.Book,
                                title = "용어 사전",
                                description = "재무 용어 설명이 로드되지 않았습니다."
                        )
                }
                return
        }

        LazyColumn(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall)
        ) { items(terms) { term -> TermExplanationCard(term) } }
}

@Composable
private fun GlossaryDialog(terms: List<FinancialTermExplanation>, onDismiss: () -> Unit) {
        androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
                Surface(
                        modifier = Modifier.width(700.dp).heightIn(max = 700.dp),
                        shape = AppShapes.Medium,
                        color = AppColors.Surface,
                        elevation = AppDimens.CardElevation
                ) {
                        Column(modifier = Modifier.padding(AppDimens.PaddingMedium)) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                        imageVector = Icons.Outlined.Book,
                                                        contentDescription = null,
                                                        tint = AppColors.Primary,
                                                        modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                        text = "Glossary",
                                                        style = AppTypography.Subtitle1,
                                                        fontWeight = FontWeight.Bold,
                                                        color = AppColors.Primary
                                                )
                                        }

                                        IconButton(onClick = onDismiss) {
                                                Icon(Icons.Default.Close, contentDescription = null)
                                        }
                                }

                                Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

                                TermGlossaryTab(
                                        terms = terms,
                                        modifier = Modifier.fillMaxWidth().weight(1f)
                                )
                        }
                }
        }
}

@Composable
private fun TermExplanationCard(term: FinancialTermExplanation) {
        var isExpanded by remember { mutableStateOf(false) }

        Card(
                modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
                elevation = AppDimens.CardElevation,
                shape = AppShapes.Medium,
                backgroundColor = AppColors.Surface
        ) {
                Column(modifier = Modifier.padding(AppDimens.PaddingMedium)) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                                Icons.Outlined.Book,
                                                contentDescription = null,
                                                tint = AppColors.Primary,
                                                modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                                text = term.term,
                                                style = AppTypography.Subtitle1,
                                                fontWeight = FontWeight.Bold,
                                                color = AppColors.Primary
                                        )
                                }

                                Icon(
                                        if (isExpanded) Icons.Default.ExpandLess
                                        else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = AppColors.OnSurfaceSecondary
                                )
                        }

                        Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                        Text(
                                text = term.simpleDefinition,
                                style = AppTypography.Body2,
                                color = AppColors.OnSurface
                        )

                        AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = AppDimens.PaddingMedium)) {
                                        Divider(color = AppColors.Divider)

                                        Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                                        // Analogy
                                        Card(
                                                backgroundColor = AppColors.InfoLight,
                                                elevation = 0.dp,
                                                shape = AppShapes.Small
                                        ) {
                                                Column(
                                                        modifier =
                                                                Modifier.padding(
                                                                        AppDimens.PaddingSmall
                                                                )
                                                ) {
                                                        Text(
                                                                text = "쉬운 비유",
                                                                style = AppTypography.Caption,
                                                                fontWeight = FontWeight.Bold,
                                                                color = AppColors.OnSurface
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                                text = term.analogy,
                                                                style = AppTypography.Body2,
                                                                color = AppColors.OnSurface
                                                        )
                                                }
                                        }

                                        Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                                        // Example
                                        Card(
                                                backgroundColor = AppColors.SuccessLight,
                                                elevation = 0.dp,
                                                shape = AppShapes.Small
                                        ) {
                                                Column(
                                                        modifier =
                                                                Modifier.padding(
                                                                        AppDimens.PaddingSmall
                                                                )
                                                ) {
                                                        Text(
                                                                text = "실제 예시",
                                                                style = AppTypography.Caption,
                                                                fontWeight = FontWeight.Bold,
                                                                color = AppColors.OnSurface
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                                text = term.example,
                                                                style = AppTypography.Body2,
                                                                color = AppColors.OnSurface
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}

/** Financial ratios tab - detailed metrics */
@Composable
private fun FinancialRatiosTab(ratios: List<FinancialRatio>, metrics: List<FinancialMetric>) {
        val scrollState = rememberScrollState()
        var showVisualization by remember { mutableStateOf(true) }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                // Toggle between visual and detailed view
                Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Text(
                                text = "Financial Ratios & Metrics",
                                style = AppTypography.Headline3,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.OnSurface
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showVisualization = !showVisualization }) {
                                        Icon(
                                                if (showVisualization)
                                                        Icons.AutoMirrored.Filled.ViewList
                                                else Icons.Default.BarChart,
                                                contentDescription = "Toggle View",
                                                tint = AppColors.Primary
                                        )
                                }
                        }
                }

                if (ratios.isNotEmpty()) {
                        if (showVisualization) {
                                // Visual representation of ratios
                                RatioVisualizationPanel(ratios)
                                Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))
                        }

                        // Detailed ratio cards
                        ratios.forEach { ratio ->
                                EnhancedRatioCard(ratio, showVisualization)
                                Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))
                        }
                }

                if (metrics.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

                        Text(
                                text = "Extracted Financial Metrics",
                                style = AppTypography.Headline3,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.OnSurface
                        )

                        Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

                        // Group ratios by category
                        val groupedRatios = ratios.groupBy { it.category }
                        groupedRatios.forEach { (category, categoryRatios) ->
                                MetricCategoryCard(category, categoryRatios)
                                Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))
                        }
                }

                if (ratios.isEmpty() && metrics.isEmpty()) {
                        EmptyState(
                                icon = Icons.Outlined.Analytics,
                                title = "No Metrics Found",
                                description = "Unable to extract financial metrics from document."
                        )
                }
        }
}

@Composable
private fun RatioVisualizationPanel(ratios: List<FinancialRatio>) {
        Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = AppDimens.CardElevationHigh,
                shape = AppShapes.Large,
                backgroundColor = AppColors.Surface
        ) {
                Column(modifier = Modifier.padding(AppDimens.PaddingLarge)) {
                        Text(
                                text = "Financial Health Overview",
                                style = AppTypography.Headline3,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.OnSurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Visual bars for each ratio category
                        val categories = ratios.groupBy { it.category.toString() }
                        categories.forEach { (category, categoryRatios) ->
                                Text(
                                        text =
                                                category.replace("_", " ")
                                                        .lowercase()
                                                        .replaceFirstChar { it.uppercase() },
                                        style = AppTypography.Subtitle1,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.OnSurface,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                )

                                categoryRatios.forEach { ratio ->
                                        RatioVisualBar(ratio)
                                        Spacer(modifier = Modifier.height(8.dp))
                                }
                        }
                }
        }
}

@Composable
private fun RatioVisualBar(ratio: FinancialRatio) {
        val statusColor =
                when (ratio.healthStatus) {
                        HealthStatus.EXCELLENT -> AppColors.Success
                        HealthStatus.GOOD -> Color(0xFF4CAF50)
                        HealthStatus.NEUTRAL -> AppColors.Warning
                        HealthStatus.CAUTION -> Color(0xFFFF9800)
                        HealthStatus.WARNING -> AppColors.Error
                }

        Column {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Text(
                                text = ratio.name,
                                style = AppTypography.Body2,
                                color = AppColors.OnSurface,
                                modifier = Modifier.weight(1f)
                        )
                        Text(
                                text = ratio.formattedValue,
                                style = AppTypography.Subtitle1,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                        )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Visual progress bar
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(8.dp)
                                        .clip(AppShapes.Pill)
                                        .background(statusColor.copy(alpha = 0.2f))
                ) {
                        val progress =
                                when (ratio.healthStatus) {
                                        HealthStatus.EXCELLENT -> 1.0f
                                        HealthStatus.GOOD -> 0.8f
                                        HealthStatus.NEUTRAL -> 0.6f
                                        HealthStatus.CAUTION -> 0.4f
                                        HealthStatus.WARNING -> 0.2f
                                }

                        Box(
                                modifier =
                                        Modifier.fillMaxHeight()
                                                .fillMaxWidth(progress)
                                                .clip(AppShapes.Pill)
                                                .background(statusColor)
                        )
                }
        }
}

@Composable
private fun EnhancedRatioCard(ratio: FinancialRatio, compact: Boolean = false) {
        var isExpanded by remember { mutableStateOf(false) }

        val statusColor =
                when (ratio.healthStatus) {
                        HealthStatus.EXCELLENT -> AppColors.Success
                        HealthStatus.GOOD -> Color(0xFF4CAF50)
                        HealthStatus.NEUTRAL -> AppColors.Warning
                        HealthStatus.CAUTION -> Color(0xFFFF9800)
                        HealthStatus.WARNING -> AppColors.Error
                }

        val statusText =
                when (ratio.healthStatus) {
                        HealthStatus.EXCELLENT -> "우수"
                        HealthStatus.GOOD -> "양호"
                        HealthStatus.NEUTRAL -> "보통"
                        HealthStatus.CAUTION -> "주의"
                        HealthStatus.WARNING -> "위험"
                }

        // Get benchmark and explanation for this ratio
        val benchmark = getBenchmarkInfo(ratio.name)

        Card(
                modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
                elevation = AppDimens.CardElevation,
                shape = AppShapes.Medium,
                backgroundColor =
                        if (compact) Color.Transparent else statusColor.copy(alpha = 0.05f)
        ) {
                Column(modifier = Modifier.padding(AppDimens.PaddingMedium)) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                        text = ratio.name,
                                                        style = AppTypography.Subtitle1,
                                                        fontWeight = FontWeight.Bold,
                                                        color = AppColors.OnSurface
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                        if (isExpanded) Icons.Default.ExpandLess
                                                        else Icons.Default.Info,
                                                        contentDescription = "상세정보",
                                                        tint = AppColors.OnSurfaceSecondary,
                                                        modifier = Modifier.size(18.dp)
                                                )
                                        }
                                        if (!compact) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                        text = ratio.description,
                                                        style = AppTypography.Caption,
                                                        color = AppColors.OnSurfaceSecondary
                                                )
                                        }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                                text = ratio.formattedValue,
                                                style = AppTypography.Headline3,
                                                fontWeight = FontWeight.Bold,
                                                color = statusColor
                                        )
                                        Surface(
                                                shape = AppShapes.Pill,
                                                color = statusColor.copy(alpha = 0.2f)
                                        ) {
                                                Text(
                                                        text = statusText,
                                                        style = AppTypography.Caption,
                                                        color = statusColor,
                                                        modifier =
                                                                Modifier.padding(
                                                                        horizontal = 8.dp,
                                                                        vertical = 2.dp
                                                                ),
                                                        fontWeight = FontWeight.Bold
                                                )
                                        }
                                }
                        }

                        if (!compact) {
                                Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))
                                Divider(color = AppColors.Divider)
                                Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                                Text(
                                        text = ratio.interpretation,
                                        style = AppTypography.Body2,
                                        color = AppColors.OnSurface
                                )

                                // Expanded section with detailed info
                                AnimatedVisibility(visible = isExpanded) {
                                        Column(modifier = Modifier.padding(top = 12.dp)) {
                                                if (benchmark != null) {
                                                        Card(
                                                                backgroundColor =
                                                                        AppColors.InfoLight,
                                                                elevation = 0.dp,
                                                                shape = AppShapes.Small
                                                        ) {
                                                                Column(
                                                                        modifier =
                                                                                Modifier.padding(
                                                                                        12.dp
                                                                                )
                                                                ) {
                                                                        Row(
                                                                                verticalAlignment =
                                                                                        Alignment
                                                                                                .CenterVertically
                                                                        ) {
                                                                                Icon(
                                                                                        Icons.Default
                                                                                                .Analytics,
                                                                                        contentDescription =
                                                                                                null,
                                                                                        tint =
                                                                                                AppColors
                                                                                                        .Info,
                                                                                        modifier =
                                                                                                Modifier.size(
                                                                                                        18.dp
                                                                                                )
                                                                                )
                                                                                Spacer(
                                                                                        modifier =
                                                                                                Modifier.width(
                                                                                                        8.dp
                                                                                                )
                                                                                )
                                                                                Text(
                                                                                        text =
                                                                                                "산업 벤치마크",
                                                                                        style =
                                                                                                AppTypography
                                                                                                        .Subtitle2,
                                                                                        fontWeight =
                                                                                                FontWeight
                                                                                                        .Bold,
                                                                                        color =
                                                                                                AppColors
                                                                                                        .OnSurface
                                                                                )
                                                                        }
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.height(
                                                                                                8.dp
                                                                                        )
                                                                        )
                                                                        Text(
                                                                                text =
                                                                                        benchmark
                                                                                                .benchmarkText,
                                                                                style =
                                                                                        AppTypography
                                                                                                .Body2,
                                                                                color =
                                                                                        AppColors
                                                                                                .OnSurface,
                                                                                lineHeight = 20.sp
                                                                        )
                                                                        if (benchmark.example
                                                                                        .isNotBlank()
                                                                        ) {
                                                                                Spacer(
                                                                                        modifier =
                                                                                                Modifier.height(
                                                                                                        8.dp
                                                                                                )
                                                                                )
                                                                                Text(
                                                                                        text =
                                                                                                "예시: ${benchmark.example}",
                                                                                        style =
                                                                                                AppTypography
                                                                                                        .Caption,
                                                                                        color =
                                                                                                AppColors
                                                                                                        .OnSurfaceSecondary,
                                                                                        fontStyle =
                                                                                                androidx.compose
                                                                                                        .ui
                                                                                                        .text
                                                                                                        .font
                                                                                                        .FontStyle
                                                                                                        .Italic
                                                                                )
                                                                        }
                                                                }
                                                        }

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        Card(
                                                                backgroundColor =
                                                                        AppColors.WarningLight.copy(
                                                                                alpha = 0.3f
                                                                        ),
                                                                elevation = 0.dp,
                                                                shape = AppShapes.Small
                                                        ) {
                                                                Column(
                                                                        modifier =
                                                                                Modifier.padding(
                                                                                        12.dp
                                                                                )
                                                                ) {
                                                                        Row(
                                                                                verticalAlignment =
                                                                                        Alignment
                                                                                                .CenterVertically
                                                                        ) {
                                                                                Icon(
                                                                                        Icons.Default
                                                                                                .Lightbulb,
                                                                                        contentDescription =
                                                                                                null,
                                                                                        tint =
                                                                                                AppColors
                                                                                                        .Warning,
                                                                                        modifier =
                                                                                                Modifier.size(
                                                                                                        18.dp
                                                                                                )
                                                                                )
                                                                                Spacer(
                                                                                        modifier =
                                                                                                Modifier.width(
                                                                                                        8.dp
                                                                                                )
                                                                                )
                                                                                Text(
                                                                                        text =
                                                                                                "투자자 관점",
                                                                                        style =
                                                                                                AppTypography
                                                                                                        .Subtitle2,
                                                                                        fontWeight =
                                                                                                FontWeight
                                                                                                        .Bold,
                                                                                        color =
                                                                                                AppColors
                                                                                                        .OnSurface
                                                                                )
                                                                        }
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.height(
                                                                                                8.dp
                                                                                        )
                                                                        )
                                                                        Text(
                                                                                text =
                                                                                        benchmark
                                                                                                .investorPerspective,
                                                                                style =
                                                                                        AppTypography
                                                                                                .Body2,
                                                                                color =
                                                                                        AppColors
                                                                                                .OnSurface,
                                                                                lineHeight = 20.sp
                                                                        )
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }
}

@Composable
private fun MetricCategoryCard(category: RatioCategory, metrics: List<FinancialRatio>) {
        Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = AppDimens.CardElevation,
                shape = AppShapes.Medium,
                backgroundColor = AppColors.Surface
        ) {
                Column(modifier = Modifier.padding(AppDimens.PaddingMedium)) {
                        val categoryName =
                                when (category) {
                                        RatioCategory.PROFITABILITY -> "Profitability"
                                        RatioCategory.LIQUIDITY -> "Liquidity"
                                        RatioCategory.SOLVENCY -> "Solvency"
                                        RatioCategory.EFFICIENCY -> "Efficiency"
                                        RatioCategory.VALUATION -> "Valuation"
                                }

                        Text(
                                text = categoryName,
                                style = AppTypography.Subtitle1,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.Primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        metrics.forEach { metric ->
                                Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Text(
                                                text = metric.name,
                                                style = AppTypography.Body2,
                                                color = AppColors.OnSurface,
                                                modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                                text = metric.formattedValue,
                                                style = AppTypography.Subtitle1,
                                                fontWeight = FontWeight.Bold,
                                                color = AppColors.OnSurface
                                        )
                                }
                        }
                }
        }
}

@Composable
private fun FinancialAnalysisHeader(
        analysis: FinancialAnalysis,
        onGlossaryClick: () -> Unit,
        onClose: () -> Unit
) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
        ) {
                Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = "Financial Analysis",
                                style = AppTypography.Headline2,
                                color = AppColors.Primary,
                                fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // File Info Row
                        Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                InfoChip(
                                        icon = Icons.AutoMirrored.Outlined.InsertDriveFile,
                                        label = analysis.fileName,
                                        color = AppColors.Primary
                                )

                                if (analysis.reportType != null) {
                                        InfoChip(
                                                icon = Icons.Outlined.Description,
                                                label = analysis.reportType,
                                                color = AppColors.Secondary
                                        )
                                }

                                if (analysis.periodEnding != null) {
                                        InfoChip(
                                                icon = Icons.Outlined.CalendarToday,
                                                label = analysis.periodEnding,
                                                color = AppColors.Info
                                        )
                                }
                        }

                        if (analysis.companyName != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                                Icons.Outlined.Business,
                                                contentDescription = null,
                                                tint = AppColors.OnSurfaceSecondary,
                                                modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                                text = analysis.companyName,
                                                style = AppTypography.Subtitle1,
                                                color = AppColors.OnSurface
                                        )
                                }
                        }
                }

                Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        OutlinedButton(
                                onClick = onGlossaryClick,
                                colors =
                                        ButtonDefaults.outlinedButtonColors(
                                                contentColor = AppColors.OnSurfaceSecondary
                                        ),
                                shape = AppShapes.Small
                        ) {
                                Icon(
                                        Icons.Outlined.Book,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Glossary")
                        }

                        OutlinedButton(
                                onClick = onClose,
                                colors =
                                        ButtonDefaults.outlinedButtonColors(
                                                contentColor = AppColors.OnSurfaceSecondary
                                        ),
                                shape = AppShapes.Small
                        ) {
                                Icon(
                                        Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Close")
                        }
                }
        }
}

@Composable
private fun FinancialOverviewTab(analysis: FinancialAnalysis) {
        val scrollState = rememberScrollState()

        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                // Quick financial summary card (new component)
                if (analysis.extendedMetrics.isNotEmpty()) {
                        QuickFinancialSummaryCard(analysis)
                        Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))
                }

                // Key financial metrics dashboard (new component)
                if (analysis.extendedMetrics.isNotEmpty()) {
                        KeyFinancialMetricsDashboard(analysis)
                        Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))
                } else {
                        // Fallback: existing metric summary grid
                        MetricsSummaryGrid(analysis.metrics)
                        Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))
                }

                // Summary Text Card
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = AppDimens.CardElevation,
                        shape = AppShapes.Medium,
                        backgroundColor = AppColors.SuccessLight
                ) {
                        Column(modifier = Modifier.padding(AppDimens.PaddingMedium)) {
                                SectionHeader(
                                        title = "Analysis Summary",
                                        icon = Icons.Outlined.Summarize
                                )

                                Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                                Text(
                                        text = analysis.summary,
                                        style = AppTypography.Monospace,
                                        color = AppColors.OnSurface
                                )
                        }
                }
        }
}

@Composable
private fun MetricsSummaryGrid(metrics: List<FinancialMetric>) {
        val groupedMetrics =
                metrics.groupBy { metric ->
                        when {
                                metric.name.contains("Revenue", ignoreCase = true) ||
                                        metric.name.contains("Sales", ignoreCase = true) ->
                                        "Revenue"
                                metric.name.contains("Income", ignoreCase = true) ||
                                        metric.name.contains("Profit", ignoreCase = true) ||
                                        metric.name.contains("Earnings", ignoreCase = true) ->
                                        "Income"
                                metric.name.contains("Assets", ignoreCase = true) -> "Assets"
                                metric.name.contains("Liabilities", ignoreCase = true) ->
                                        "Liabilities"
                                metric.name.contains("Equity", ignoreCase = true) -> "Equity"
                                else -> "Other"
                        }
                }

        val categories =
                listOf(
                        Triple("Revenue", Icons.Outlined.AttachMoney, AppColors.Revenue),
                        Triple("Income", Icons.AutoMirrored.Outlined.TrendingUp, AppColors.Income),
                        Triple("Assets", Icons.Outlined.AccountBalance, AppColors.Assets),
                        Triple("Liabilities", Icons.Outlined.Receipt, AppColors.Liabilities),
                        Triple("Equity", Icons.Outlined.Diamond, AppColors.Equity)
                )

        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall)) {
                categories.chunked(3).forEach { rowCategories ->
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall)
                        ) {
                                rowCategories.forEach { (category, icon, color) ->
                                        val categoryMetrics =
                                                groupedMetrics[category] ?: emptyList()
                                        MetricCategoryCard(
                                                category = category,
                                                icon = icon,
                                                color = color,
                                                count = categoryMetrics.size,
                                                topValue =
                                                        categoryMetrics.firstOrNull()?.let {
                                                                if (it.rawValue != null)
                                                                        formatCurrency(it.rawValue)
                                                                else it.value
                                                        },
                                                modifier = Modifier.weight(1f)
                                        )
                                }

                                // Fill remaining space if row is not complete
                                repeat(3 - rowCategories.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                }
                        }
                }
        }
}

@Composable
private fun MetricCategoryCard(
        category: String,
        icon: ImageVector,
        color: Color,
        count: Int,
        topValue: String?,
        modifier: Modifier = Modifier
) {
        Card(
                modifier = modifier,
                elevation = AppDimens.CardElevation,
                shape = AppShapes.Medium,
                backgroundColor = color.copy(alpha = 0.05f)
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(AppDimens.PaddingMedium)) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(24.dp)
                                )

                                Box(
                                        modifier =
                                                Modifier.background(color, shape = CircleShape)
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                        Text(
                                                text = count.toString(),
                                                style = AppTypography.Caption,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                                text = category,
                                style = AppTypography.Subtitle2,
                                color = AppColors.OnSurfaceSecondary
                        )

                        if (topValue != null && count > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                        text = topValue,
                                        style = AppTypography.Subtitle1,
                                        color = color,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )
                        } else {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                        text = "—",
                                        style = AppTypography.Body2,
                                        color = AppColors.Divider
                                )
                        }
                }
        }
}

@Composable
private fun FinancialMetricsTab(metrics: List<FinancialMetric>) {
        if (metrics.isEmpty()) {
                EmptyState(
                        icon = Icons.Outlined.SearchOff,
                        title = "No Metrics Found",
                        description = "The document may not contain standard financial statements."
                )
        } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall)) {
                        items(metrics) { metric -> MetricDetailCard(metric) }
                }
        }
}

@Composable
private fun MetricDetailCard(metric: FinancialMetric) {
        Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 1.dp,
                shape = AppShapes.Medium,
                backgroundColor = AppColors.Surface
        ) {
                Column(modifier = Modifier.padding(AppDimens.PaddingMedium)) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text(
                                        text = metric.name,
                                        style = AppTypography.Subtitle1,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.Primary
                                )

                                Text(
                                        text = metric.value,
                                        style = AppTypography.Subtitle1,
                                        fontWeight = FontWeight.Medium,
                                        color = AppColors.OnSurface
                                )
                        }

                        if (metric.rawValue != null) {
                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                        Text(
                                                text = "Parsed Value",
                                                style = AppTypography.Caption,
                                                color = AppColors.OnSurfaceSecondary
                                        )

                                        Text(
                                                text = formatCurrency(metric.rawValue),
                                                style = AppTypography.Caption,
                                                color = AppColors.Success,
                                                fontWeight = FontWeight.Medium
                                        )
                                }
                        }
                }
        }
}

@Composable
private fun FinancialRawDataTab(rawContent: String, analysis: FinancialAnalysis) {
        val scrollState = rememberScrollState()
        var copySuccess by remember { mutableStateOf(false) }

        Card(
                modifier = Modifier.fillMaxSize(),
                elevation = AppDimens.CardElevation,
                shape = AppShapes.Medium,
                backgroundColor = AppColors.Surface
        ) {
                Column(modifier = Modifier.padding(AppDimens.PaddingMedium)) {
                        SectionHeader(
                                title = "Raw Document Content",
                                icon = Icons.Outlined.Code,
                                action = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                        text = "${rawContent.length} characters",
                                                        style = AppTypography.Caption,
                                                        color = AppColors.OnSurfaceSecondary
                                                )

                                                Spacer(
                                                        modifier =
                                                                Modifier.width(
                                                                        AppDimens.PaddingMedium
                                                                )
                                                )

                                                // Copy to clipboard button
                                                Button(
                                                        onClick = {
                                                                try {
                                                                        val clipboard =
                                                                                java.awt.Toolkit
                                                                                        .getDefaultToolkit()
                                                                                        .systemClipboard
                                                                        val stringSelection =
                                                                                java.awt
                                                                                        .datatransfer
                                                                                        .StringSelection(
                                                                                                rawContent
                                                                                        )
                                                                        clipboard.setContents(
                                                                                stringSelection,
                                                                                null
                                                                        )
                                                                        copySuccess = true
                                                                } catch (e: Exception) {
                                                                        println(
                                                                                "Failed to copy to clipboard: ${e.message}"
                                                                        )
                                                                }
                                                        },
                                                        colors =
                                                                ButtonDefaults.buttonColors(
                                                                        backgroundColor =
                                                                                AppColors.Primary
                                                                ),
                                                        modifier = Modifier.height(32.dp)
                                                ) {
                                                        Icon(
                                                                Icons.Default.ContentCopy,
                                                                contentDescription =
                                                                        "Copy to clipboard",
                                                                modifier = Modifier.size(16.dp),
                                                                tint = Color.White
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                                text =
                                                                        if (copySuccess) "Copied!"
                                                                        else "Copy",
                                                                color = Color.White,
                                                                style = AppTypography.Caption
                                                        )
                                                }
                                        }
                                }
                        )

                        Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                        Divider(color = AppColors.Divider)

                        Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                        if (copySuccess) {
                                LaunchedEffect(Unit) {
                                        kotlinx.coroutines.delay(2000)
                                        copySuccess = false
                                }
                        }

                        // Display extracted metrics summary first
                        if (analysis.extendedMetrics.isNotEmpty()) {
                                Card(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .padding(bottom = AppDimens.PaddingMedium),
                                        backgroundColor = AppColors.InfoLight,
                                        shape = AppShapes.Small,
                                        elevation = 0.dp
                                ) {
                                        Column(
                                                modifier = Modifier.padding(AppDimens.PaddingMedium)
                                        ) {
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Icon(
                                                                Icons.Default.Assessment,
                                                                contentDescription = null,
                                                                tint = AppColors.Info,
                                                                modifier = Modifier.size(20.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                                "추출된 재무 데이터: ${analysis.extendedMetrics.size}개 항목",
                                                                style =
                                                                        AppTypography.Body1.copy(
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold
                                                                        ),
                                                                color = AppColors.Info
                                                        )
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Group metrics by category
                                                val groupedMetrics =
                                                        analysis.extendedMetrics.groupBy {
                                                                it.category
                                                        }

                                                Row(modifier = Modifier.fillMaxWidth()) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                                val halfSize =
                                                                        groupedMetrics.size / 2
                                                                groupedMetrics.entries.take(
                                                                                halfSize
                                                                        )
                                                                        .forEach {
                                                                                (category, metrics)
                                                                                ->
                                                                                Text(
                                                                                        "• ${category.name.replace("_", " ")}: ${metrics.size}개",
                                                                                        style =
                                                                                                AppTypography
                                                                                                        .Caption,
                                                                                        color =
                                                                                                AppColors
                                                                                                        .OnSurface
                                                                                )
                                                                        }
                                                        }
                                                        Column(modifier = Modifier.weight(1f)) {
                                                                val halfSize =
                                                                        groupedMetrics.size / 2
                                                                groupedMetrics.entries.drop(
                                                                                halfSize
                                                                        )
                                                                        .forEach {
                                                                                (category, metrics)
                                                                                ->
                                                                                Text(
                                                                                        "• ${category.name.replace("_", " ")}: ${metrics.size}개",
                                                                                        style =
                                                                                                AppTypography
                                                                                                        .Caption,
                                                                                        color =
                                                                                                AppColors
                                                                                                        .OnSurface
                                                                                )
                                                                        }
                                                        }
                                                }
                                        }
                                }
                        }

                        Box(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .background(
                                                        AppColors.SurfaceVariant,
                                                        shape = AppShapes.Small
                                                )
                                                .padding(AppDimens.PaddingMedium)
                                                .verticalScroll(scrollState)
                        ) {
                                Text(
                                        text = rawContent.take(20000),
                                        style = AppTypography.Monospace.copy(fontSize = 11.sp),
                                        color = AppColors.OnSurface
                                )
                        }
                }
        }
}

/** Loading state for analysis */
@Composable
fun AnalysisLoadingView(
        message: String = "Analyzing document...",
        progress: Float? = null,
        modifier: Modifier = Modifier
) {
        Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
                // Animated icon
                Icon(
                        Icons.Default.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = AppColors.Primary
                )

                Spacer(modifier = Modifier.height(AppDimens.PaddingLarge))

                if (progress != null) {
                        LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier.width(200.dp).height(6.dp),
                                color = AppColors.Primary,
                                backgroundColor = AppColors.PrimaryLight
                        )
                } else {
                        CircularProgressIndicator(
                                color = AppColors.Primary,
                                modifier = Modifier.size(48.dp)
                        )
                }

                Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

                Text(
                        text = message,
                        style = AppTypography.Subtitle1,
                        color = AppColors.OnSurfaceSecondary
                )

                Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                Text(
                        text = "This may take a few moments...",
                        style = AppTypography.Body2,
                        color = AppColors.OnSurfaceSecondary.copy(alpha = 0.7f)
                )
        }
}

/** Error state for analysis */
@Composable
fun AnalysisErrorView(
        message: String,
        onRetry: (() -> Unit)? = null,
        onClose: () -> Unit,
        modifier: Modifier = Modifier
) {
        Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
                Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = AppColors.Error
                )

                Spacer(modifier = Modifier.height(AppDimens.PaddingLarge))

                Text(
                        text = "Analysis Failed",
                        style = AppTypography.Headline3,
                        color = AppColors.Error,
                        fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                Card(
                        modifier = Modifier.widthIn(max = 400.dp),
                        elevation = 0.dp,
                        shape = AppShapes.Medium,
                        backgroundColor = AppColors.ErrorLight
                ) {
                        Text(
                                text = message,
                                style = AppTypography.Body2,
                                color = AppColors.Error,
                                modifier = Modifier.padding(AppDimens.PaddingMedium),
                                textAlign = TextAlign.Center
                        )
                }

                Spacer(modifier = Modifier.height(AppDimens.PaddingLarge))

                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall)) {
                        OutlinedButton(
                                onClick = onClose,
                                colors =
                                        ButtonDefaults.outlinedButtonColors(
                                                contentColor = AppColors.OnSurfaceSecondary
                                        ),
                                shape = AppShapes.Small
                        ) { Text("Close") }

                        if (onRetry != null) {
                                Button(
                                        onClick = onRetry,
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        backgroundColor = AppColors.Primary
                                                ),
                                        shape = AppShapes.Small
                                ) {
                                        Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Retry")
                                }
                        }
                }
        }
}

// ============================================================
// Key Financial Metrics Visualization Component
// ============================================================

/** Key financial metrics dashboard card Visualizes parsed financial data. */
@Composable
fun KeyFinancialMetricsDashboard(analysis: FinancialAnalysis, modifier: Modifier = Modifier) {
        val extendedMetrics = analysis.extendedMetrics

        // Group metrics by category
        val revenueMetrics =
                extendedMetrics.filter {
                        it.category in
                                listOf(
                                        MetricCategory.REVENUE,
                                        MetricCategory.GROSS_PROFIT,
                                        MetricCategory.OPERATING_INCOME,
                                        MetricCategory.NET_INCOME
                                )
                }

        val balanceMetrics =
                extendedMetrics.filter {
                        it.category in
                                listOf(
                                        MetricCategory.TOTAL_ASSETS,
                                        MetricCategory.CASH_AND_EQUIVALENTS,
                                        MetricCategory.TOTAL_LIABILITIES,
                                        MetricCategory.TOTAL_EQUITY
                                )
                }

        val cashFlowMetrics =
                extendedMetrics.filter {
                        it.category in
                                listOf(
                                        MetricCategory.OPERATING_CASH_FLOW,
                                        MetricCategory.FREE_CASH_FLOW,
                                        MetricCategory.CAPITAL_EXPENDITURES
                                )
                }

        Column(modifier = modifier) {
                // Parsing quality indicator
                ParsingQualityIndicator(analysis)

                Spacer(modifier = Modifier.height(16.dp))

                // Income statement key metrics
                if (revenueMetrics.isNotEmpty()) {
                        FinancialStatementCard(
                                title = "Income Statement Highlights",
                                subtitle = "손익계산서 핵심 지표",
                                metrics = revenueMetrics,
                                accentColor = AppColors.Revenue
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                }

                // Balance sheet key metrics
                if (balanceMetrics.isNotEmpty()) {
                        FinancialStatementCard(
                                title = "Balance Sheet Highlights",
                                subtitle = "재무상태표 핵심 지표",
                                metrics = balanceMetrics,
                                accentColor = AppColors.Primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                }

                // Cash flow statement key metrics
                if (cashFlowMetrics.isNotEmpty()) {
                        FinancialStatementCard(
                                title = "Cash Flow Highlights",
                                subtitle = "현금흐름표 핵심 지표",
                                metrics = cashFlowMetrics,
                                accentColor = AppColors.Success
                        )
                }

                // If no metrics found
                if (revenueMetrics.isEmpty() &&
                                balanceMetrics.isEmpty() &&
                                cashFlowMetrics.isEmpty()
                ) {
                        NoMetricsFoundCard()
                }
        }
}

/** 파싱 품질 표시기 */
@Composable
private fun ParsingQualityIndicator(analysis: FinancialAnalysis) {
        val metrics = analysis.extendedMetrics
        val totalCount = metrics.size
        val avgConfidence =
                if (metrics.isNotEmpty()) {
                        metrics.map { it.confidence }.average()
                } else 0.0

        val qualityLevel =
                when {
                        totalCount >= 10 && avgConfidence >= 0.8 -> "High"
                        totalCount >= 5 && avgConfidence >= 0.6 -> "Medium"
                        totalCount >= 1 -> "Low"
                        else -> "None"
                }

        val qualityColor =
                when (qualityLevel) {
                        "High" -> AppColors.Success
                        "Medium" -> AppColors.Warning
                        "Low" -> AppColors.Error
                        else -> AppColors.Divider
                }

        Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 1.dp,
                shape = AppShapes.Small,
                backgroundColor = qualityColor.copy(alpha = 0.1f)
        ) {
                Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                        modifier =
                                                Modifier.size(10.dp)
                                                        .background(qualityColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                        text = "Data Quality: $qualityLevel",
                                        style = AppTypography.Body2,
                                        fontWeight = FontWeight.Medium,
                                        color = AppColors.OnSurface
                                )
                        }

                        Text(
                                text =
                                        "$totalCount metrics • ${String.format("%.0f", avgConfidence * 100)}% confidence",
                                style = AppTypography.Caption,
                                color = AppColors.OnSurfaceSecondary
                        )
                }
        }
}

/** 재무제표별 카드 컴포넌트 */
@Composable
private fun FinancialStatementCard(
        title: String,
        subtitle: String,
        metrics: List<ExtendedFinancialMetric>,
        accentColor: Color,
        modifier: Modifier = Modifier
) {
        Card(
                modifier = modifier.fillMaxWidth(),
                elevation = AppDimens.CardElevation,
                shape = AppShapes.Medium,
                backgroundColor = AppColors.Surface
        ) {
                Column(modifier = Modifier.padding(16.dp)) {
                        // Header
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Column {
                                        Text(
                                                text = title,
                                                style = AppTypography.Subtitle1,
                                                fontWeight = FontWeight.Bold,
                                                color = AppColors.OnSurface
                                        )
                                        Text(
                                                text = subtitle,
                                                style = AppTypography.Caption,
                                                color = AppColors.OnSurfaceSecondary
                                        )
                                }

                                // Metric count display
                                Box(
                                        modifier =
                                                Modifier.background(
                                                                accentColor.copy(alpha = 0.1f),
                                                                AppShapes.Pill
                                                        )
                                                        .padding(
                                                                horizontal = 12.dp,
                                                                vertical = 4.dp
                                                        )
                                ) {
                                        Text(
                                                text = "${metrics.size} items",
                                                style = AppTypography.Caption,
                                                color = accentColor,
                                                fontWeight = FontWeight.Medium
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = AppColors.Divider)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Metric grid
                        metrics.chunked(2).forEach { rowMetrics ->
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                        rowMetrics.forEach { metric ->
                                                MetricDisplayCard(
                                                        metric = metric,
                                                        accentColor = accentColor,
                                                        modifier = Modifier.weight(1f)
                                                )
                                        }
                                        // Fill empty space for odd count
                                        if (rowMetrics.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                        }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                        }
                }
        }
}

/** 개별 메트릭 표시 카드 */
@Composable
private fun MetricDisplayCard(
        metric: ExtendedFinancialMetric,
        accentColor: Color,
        modifier: Modifier = Modifier
) {
        val isNegative = (metric.getRawValueBigDecimal()?.toDouble() ?: 0.0) < 0
        val valueColor = if (isNegative) AppColors.Error else AppColors.OnSurface

        Card(
                modifier = modifier,
                elevation = 0.dp,
                shape = AppShapes.Small,
                backgroundColor = AppColors.SurfaceVariant.copy(alpha = 0.5f)
        ) {
                Column(modifier = Modifier.padding(12.dp)) {
                        // Metric name
                        Text(
                                text = metric.name,
                                style = AppTypography.Caption,
                                color = AppColors.OnSurfaceSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Value
                        Text(
                                text = metric.value,
                                style = AppTypography.Subtitle1,
                                fontWeight = FontWeight.Bold,
                                color = valueColor
                        )

                        // YoY change rate (if available)
                        metric.getYoyChangeBigDecimal()?.toDouble()?.let { yoy ->
                                Spacer(modifier = Modifier.height(4.dp))
                                val yoyColor = if (yoy >= 0) AppColors.Success else AppColors.Error
                                val yoySign = if (yoy >= 0) "+" else ""
                                val yoyIcon =
                                        if (yoy >= 0) Icons.AutoMirrored.Filled.TrendingUp
                                        else Icons.AutoMirrored.Filled.TrendingDown

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                                imageVector = yoyIcon,
                                                contentDescription = null,
                                                tint = yoyColor,
                                                modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                                text = "$yoySign${String.format("%.1f", yoy)}% YoY",
                                                style = AppTypography.Caption,
                                                color = yoyColor,
                                                fontWeight = FontWeight.Medium
                                        )
                                }
                        }

                        // Confidence display
                        if (metric.confidence < 0.9) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                        text =
                                                "Confidence: ${String.format("%.0f", metric.confidence * 100)}%",
                                        style = AppTypography.Caption,
                                        color = AppColors.Warning,
                                        fontSize = 10.sp
                                )
                        }
                }
        }
}

/** 메트릭이 없을 때 표시하는 카드 */
@Composable
private fun NoMetricsFoundCard() {
        Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = AppDimens.CardElevation,
                shape = AppShapes.Medium,
                backgroundColor = AppColors.WarningLight
        ) {
                Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        Icon(
                                imageVector = Icons.Outlined.SearchOff,
                                contentDescription = null,
                                tint = AppColors.Warning,
                                modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                                text = "No Financial Metrics Detected",
                                style = AppTypography.Subtitle1,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.OnSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                                text =
                                        "The document may not contain standard financial statements, " +
                                                "or the format is not yet supported. Try using AI Analysis for deeper insights.",
                                style = AppTypography.Body2,
                                color = AppColors.OnSurfaceSecondary,
                                textAlign = TextAlign.Center
                        )
                }
        }
}

/** 빠른 재무 요약 카드 (단일 카드로 핵심 정보 표시) */
@Composable
fun QuickFinancialSummaryCard(analysis: FinancialAnalysis, modifier: Modifier = Modifier) {
        val metrics = analysis.extendedMetrics

        // Extract key metrics
        val revenue = metrics.find { it.category == MetricCategory.REVENUE }
        val netIncome = metrics.find { it.category == MetricCategory.NET_INCOME }
        val totalAssets = metrics.find { it.category == MetricCategory.TOTAL_ASSETS }
        val eps =
                metrics.find {
                        it.category == MetricCategory.EPS_BASIC ||
                                it.category == MetricCategory.EPS_DILUTED
                }

        Card(
                modifier = modifier.fillMaxWidth(),
                elevation = AppDimens.CardElevationHigh,
                shape = AppShapes.Medium,
                backgroundColor = AppColors.Primary.copy(alpha = 0.05f)
        ) {
                Column(modifier = Modifier.padding(20.dp)) {
                        // Header
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text(
                                        text = "Quick Financial Summary",
                                        style = AppTypography.Subtitle1,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.OnSurface
                                )

                                analysis.reportType?.let { type ->
                                        Box(
                                                modifier =
                                                        Modifier.background(
                                                                        AppColors.Primary.copy(
                                                                                alpha = 0.1f
                                                                        ),
                                                                        AppShapes.Pill
                                                                )
                                                                .padding(
                                                                        horizontal = 10.dp,
                                                                        vertical = 4.dp
                                                                )
                                        ) {
                                                Text(
                                                        text = "Form $type",
                                                        style = AppTypography.Caption,
                                                        color = AppColors.Primary,
                                                        fontWeight = FontWeight.Medium
                                                )
                                        }
                                }
                        }

                        analysis.companyName?.let { name ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                        text = name,
                                        style = AppTypography.Body2,
                                        color = AppColors.OnSurfaceSecondary
                                )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = AppColors.Divider)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Key metrics grid
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                                QuickMetricItem(
                                        label = "Revenue",
                                        value = revenue?.value ?: "N/A",
                                        icon = Icons.Outlined.AttachMoney,
                                        color = AppColors.Revenue
                                )

                                QuickMetricItem(
                                        label = "Net Income",
                                        value = netIncome?.value ?: "N/A",
                                        icon = Icons.AutoMirrored.Outlined.TrendingUp,
                                        color = AppColors.Income
                                )

                                QuickMetricItem(
                                        label = "Total Assets",
                                        value = totalAssets?.value ?: "N/A",
                                        icon = Icons.Outlined.AccountBalance,
                                        color = AppColors.Primary
                                )

                                QuickMetricItem(
                                        label = "EPS",
                                        value = eps?.value ?: "N/A",
                                        icon = Icons.Outlined.BarChart,
                                        color = AppColors.Secondary
                                )
                        }
                }
        }
}

@Composable
private fun QuickMetricItem(label: String, value: String, icon: ImageVector, color: Color) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = label,
                        style = AppTypography.Caption,
                        color = AppColors.OnSurfaceSecondary
                )
                Text(
                        text = value,
                        style = AppTypography.Body2,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.OnSurface
                )
        }
}
