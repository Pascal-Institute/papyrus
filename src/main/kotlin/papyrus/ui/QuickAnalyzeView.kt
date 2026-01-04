package papyrus.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
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
import papyrus.core.model.AiAnalysisResult
import papyrus.core.model.BeginnerInsight
import papyrus.core.model.FinancialAnalysis
import papyrus.core.model.FinancialHealthScore
import papyrus.core.model.FinancialMetric
import papyrus.core.model.FinancialRatio
import papyrus.core.model.FinancialTermExplanation
import papyrus.core.model.HealthStatus
import papyrus.core.model.RatioCategory
import papyrus.core.service.AiAnalysisService

/** Helper function to format currency values */
private fun formatCurrency(value: Double): String {
    return when {
        value >= 1_000_000_000 -> String.format("$%.2fB", value / 1_000_000_000)
        value >= 1_000_000 -> String.format("$%.2fM", value / 1_000_000)
        value >= 1_000 -> String.format("$%.2fK", value / 1_000)
        else -> String.format("$%.2f", value)
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
        AnalysisTabRow(selectedTab = selectedTab, onTabSelected = { selectedTab = it }, tabs = tabs)

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
                            Icons.Default.OpenInNew,
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
                SectionHeader(title = "Analysis Summary", icon = Icons.Outlined.Insights)

                Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                Text(text = summary, style = AppTypography.Monospace, color = AppColors.OnSurface)
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
                icon = Icons.Outlined.TrendingUp,
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
            backgroundColor = if (isFound) color.copy(alpha = 0.1f) else AppColors.SurfaceVariant
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

    Card(
            modifier = Modifier.fillMaxSize(),
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
                SectionHeader(title = "Document Content Preview", icon = Icons.Outlined.Article)

                Text(
                        text = "${content.length} characters",
                        style = AppTypography.Caption,
                        color = AppColors.OnSurfaceSecondary
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

            Divider(color = AppColors.Divider)

            Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

            Box(
                    modifier =
                            Modifier.fillMaxSize()
                                    .background(AppColors.SurfaceVariant, shape = AppShapes.Small)
                                    .padding(AppDimens.PaddingMedium)
                                    .verticalScroll(scrollState)
            ) { Text(text = content, style = AppTypography.Monospace, color = AppColors.OnSurface) }
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
        onReanalyzeWithAI: ((FinancialAnalysis) -> Unit)? = null,
        modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }

    // Check if AI analysis tab should be included
    val hasAiAnalysis =
            analysis.aiAnalysis != null ||
                    analysis.aiSummary != null ||
                    analysis.industryComparison != null ||
                    analysis.investmentAdvice != null

    // Clean tab names (emoji removed)
    val tabs = buildList {
        if (analysis.beginnerInsights.isNotEmpty() || analysis.healthScore != null) {
            add("Health Score")
            if (hasAiAnalysis) add("AI Analysis")
            add("Insights")
            add("Glossary")
            add("Ratios")
            add("Raw Data")
        } else {
            add("Overview")
            if (hasAiAnalysis) add("AI Analysis")
            add("Metrics")
            add("Raw Data")
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        FinancialAnalysisHeader(analysis = analysis, onClose = onClose)

        Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

        // Tab Row
        AnalysisTabRow(selectedTab = selectedTab, onTabSelected = { selectedTab = it }, tabs = tabs)

        Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

        // Content based on selected tab
        if (analysis.beginnerInsights.isNotEmpty() || analysis.healthScore != null) {
            when (selectedTab) {
                0 -> HealthScoreTab(analysis)
                1 ->
                        if (hasAiAnalysis) AiAnalysisTab(analysis, onReanalyzeWithAI)
                        else BeginnerInsightsTab(analysis.beginnerInsights, analysis.keyTakeaways)
                2 ->
                        if (hasAiAnalysis)
                                BeginnerInsightsTab(
                                        analysis.beginnerInsights,
                                        analysis.keyTakeaways
                                )
                        else TermGlossaryTab(analysis.termExplanations)
                3 ->
                        if (hasAiAnalysis) TermGlossaryTab(analysis.termExplanations)
                        else FinancialRatiosTab(analysis.ratios, analysis.metrics)
                4 ->
                        if (hasAiAnalysis) FinancialRatiosTab(analysis.ratios, analysis.metrics)
                        else FinancialRawDataTab(analysis.rawContent)
                5 -> FinancialRawDataTab(analysis.rawContent)
            }
        } else {
            when (selectedTab) {
                0 -> FinancialOverviewTab(analysis)
                1 ->
                        if (hasAiAnalysis) AiAnalysisTab(analysis, onReanalyzeWithAI)
                        else FinancialMetricsTab(analysis.metrics)
                2 ->
                        if (hasAiAnalysis) FinancialMetricsTab(analysis.metrics)
                        else FinancialRawDataTab(analysis.rawContent)
                3 -> FinancialRawDataTab(analysis.rawContent)
            }
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
                        title = "💪 Strengths",
                        items = healthScore.strengths,
                        backgroundColor = AppColors.SuccessLight,
                        modifier = Modifier.weight(1f)
                )

                // Weaknesses card
                StrengthWeaknessCard(
                        title = "⚠️ Needs Improvement",
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
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
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
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(AppShapes.Pill),
                    color = scoreColor,
                    backgroundColor = scoreColor.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

            // 요약 설명
            Card(backgroundColor = Color.White, elevation = 0.dp, shape = AppShapes.Medium) {
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
                            text = item,
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
            SectionHeader(title = "💡 투자 팁 & 권장사항", icon = Icons.Outlined.Lightbulb)

            Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

            recommendations.forEach { recommendation ->
                Text(
                        text = recommendation,
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
            SectionHeader(title = "📌 핵심 요점", icon = Icons.Outlined.Star)

            Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

            takeaways.forEach { takeaway ->
                Text(
                        text = takeaway,
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

            Text(text = explanation, style = AppTypography.Body2, color = AppColors.OnSurface)
        }
    }
}

/** 초보자 인사이트 탭 - 쉬운 설명 */
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
            // 헤더
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = insight.emoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                                text = insight.title,
                                style = AppTypography.Subtitle1,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.Primary
                        )
                        Text(
                                text = insight.summary,
                                style = AppTypography.Body2,
                                color = AppColors.OnSurface
                        )
                    }
                }

                Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "자세히 보기",
                        tint = AppColors.OnSurfaceSecondary
                )
            }

            // 확장 콘텐츠
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = AppDimens.PaddingMedium)) {
                    Divider(color = AppColors.Divider)

                    Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                    // 상세 설명
                    InsightSection(
                            title = "📝 상세 설명",
                            content = insight.detailedExplanation,
                            backgroundColor = AppColors.SurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                    // 이것이 의미하는 것
                    InsightSection(
                            title = "🤔 이게 무슨 뜻이에요?",
                            content = insight.whatItMeans,
                            backgroundColor = AppColors.InfoLight
                    )

                    Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                    // 왜 중요한지
                    InsightSection(
                            title = "❓ 왜 중요한가요?",
                            content = insight.whyItMatters,
                            backgroundColor = AppColors.WarningLight
                    )

                    Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                    // 실행 가능한 조언
                    InsightSection(
                            title = "💡 투자자 팁",
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
            Text(text = content, style = AppTypography.Body2, color = AppColors.OnSurface)
        }
    }
}

/** 용어 사전 탭 */
@Composable
private fun TermGlossaryTab(terms: List<FinancialTermExplanation>) {
    if (terms.isEmpty()) {
        EmptyState(
                icon = Icons.Outlined.Book,
                title = "용어 사전",
                description = "재무 용어 설명이 로드되지 않았습니다."
        )
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall)) {
            items(terms) { term -> TermExplanationCard(term) }
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
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
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

                    // 비유
                    Card(
                            backgroundColor = AppColors.InfoLight,
                            elevation = 0.dp,
                            shape = AppShapes.Small
                    ) {
                        Column(modifier = Modifier.padding(AppDimens.PaddingSmall)) {
                            Text(
                                    text = "🎯 쉬운 비유",
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

                    // 예시
                    Card(
                            backgroundColor = AppColors.SuccessLight,
                            elevation = 0.dp,
                            shape = AppShapes.Small
                    ) {
                        Column(modifier = Modifier.padding(AppDimens.PaddingSmall)) {
                            Text(
                                    text = "📋 실제 예시",
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

/** 재무 비율 탭 - 상세 지표 */
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
                        if (showVisualization) Icons.Default.ViewList else Icons.Default.BarChart,
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
                    text = category.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
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
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(AppShapes.Pill)
                .background(statusColor.copy(alpha = 0.2f))
        ) {
            val progress = when (ratio.healthStatus) {
                HealthStatus.EXCELLENT -> 1.0f
                HealthStatus.GOOD -> 0.8f
                HealthStatus.NEUTRAL -> 0.6f
                HealthStatus.CAUTION -> 0.4f
                HealthStatus.WARNING -> 0.2f
            }
            
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(AppShapes.Pill)
                    .background(statusColor)
            )
        }
    }
}

@Composable
private fun EnhancedRatioCard(ratio: FinancialRatio, compact: Boolean = false) {
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
                HealthStatus.EXCELLENT -> "Excellent"
                HealthStatus.GOOD -> "Good"
                HealthStatus.NEUTRAL -> "Neutral"
                HealthStatus.CAUTION -> "Caution"
                HealthStatus.WARNING -> "Warning"
            }

    Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = AppDimens.CardElevation,
            shape = AppShapes.Medium,
            backgroundColor = if (compact) Color.Transparent else statusColor.copy(alpha = 0.05f)
    ) {
        Column(modifier = Modifier.padding(AppDimens.PaddingMedium)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            text = ratio.name,
                            style = AppTypography.Subtitle1,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.OnSurface
                    )
                    if (!compact) {
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
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
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
            val categoryName = when(category) {
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
private fun FinancialAnalysisHeader(analysis: FinancialAnalysis, onClose: () -> Unit) {
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
                        icon = Icons.Outlined.InsertDriveFile,
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

        OutlinedButton(
                onClick = onClose,
                colors =
                        ButtonDefaults.outlinedButtonColors(
                                contentColor = AppColors.OnSurfaceSecondary
                        ),
                shape = AppShapes.Small
        ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Close")
        }
    }
}

@Composable
private fun FinancialOverviewTab(analysis: FinancialAnalysis) {
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        // Metrics Summary Cards
        MetricsSummaryGrid(analysis.metrics)

        Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

        // Summary Text Card
        Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = AppDimens.CardElevation,
                shape = AppShapes.Medium,
                backgroundColor = AppColors.SuccessLight
        ) {
            Column(modifier = Modifier.padding(AppDimens.PaddingMedium)) {
                SectionHeader(title = "Analysis Summary", icon = Icons.Outlined.Summarize)

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
                            metric.name.contains("Sales", ignoreCase = true) -> "Revenue"
                    metric.name.contains("Income", ignoreCase = true) ||
                            metric.name.contains("Profit", ignoreCase = true) ||
                            metric.name.contains("Earnings", ignoreCase = true) -> "Income"
                    metric.name.contains("Assets", ignoreCase = true) -> "Assets"
                    metric.name.contains("Liabilities", ignoreCase = true) -> "Liabilities"
                    metric.name.contains("Equity", ignoreCase = true) -> "Equity"
                    else -> "Other"
                }
            }

    val categories =
            listOf(
                    Triple("Revenue", Icons.Outlined.AttachMoney, AppColors.Revenue),
                    Triple("Income", Icons.Outlined.TrendingUp, AppColors.Income),
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
                    val categoryMetrics = groupedMetrics[category] ?: emptyList()
                    MetricCategoryCard(
                            category = category,
                            icon = icon,
                            color = color,
                            count = categoryMetrics.size,
                            topValue =
                                    categoryMetrics.firstOrNull()?.let {
                                        if (it.rawValue != null) formatCurrency(it.rawValue)
                                        else it.value
                                    },
                            modifier = Modifier.weight(1f)
                    )
                }

                // Fill remaining space if row is not complete
                repeat(3 - rowCategories.size) { Spacer(modifier = Modifier.weight(1f)) }
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
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))

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
                Text(text = "—", style = AppTypography.Body2, color = AppColors.Divider)
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
private fun FinancialRawDataTab(rawContent: String) {
    val scrollState = rememberScrollState()
    var copySuccess by remember { mutableStateOf(false) }

    Card(
            modifier = Modifier.fillMaxSize(),
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
                SectionHeader(title = "Raw Document Content", icon = Icons.Outlined.Code)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                            text = "${rawContent.length} characters",
                            style = AppTypography.Caption,
                            color = AppColors.OnSurfaceSecondary
                    )
                    
                    Spacer(modifier = Modifier.width(AppDimens.PaddingMedium))
                    
                    // Copy to clipboard button
                    Button(
                        onClick = {
                            try {
                                val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                                val stringSelection = java.awt.datatransfer.StringSelection(rawContent)
                                clipboard.setContents(stringSelection, null)
                                copySuccess = true
                            } catch (e: Exception) {
                                println("Failed to copy to clipboard: ${e.message}")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Primary),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy to clipboard",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (copySuccess) "Copied!" else "Copy",
                            color = Color.White,
                            style = AppTypography.Caption
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

            Divider(color = AppColors.Divider)

            Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))
            
            if (copySuccess) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    copySuccess = false
                }
            }

            Box(
                    modifier =
                            Modifier.fillMaxSize()
                                    .background(AppColors.SurfaceVariant, shape = AppShapes.Small)
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
            CircularProgressIndicator(color = AppColors.Primary, modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

        Text(text = message, style = AppTypography.Subtitle1, color = AppColors.OnSurfaceSecondary)

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
                        colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Primary),
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

/** AI Analysis Tab - Clean and professional AI financial analysis display */
@Composable
private fun AiAnalysisTab(
        analysis: FinancialAnalysis,
        onReanalyze: ((FinancialAnalysis) -> Unit)?
) {
    val scrollState = rememberScrollState()

    Column(
            modifier =
                    Modifier.fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = AppDimens.PaddingMedium)
    ) {
        // AI Configuration Check
        if (!AiAnalysisService.isConfigured()) {
            AiConfigurationCard()
            return
        }

        // AI Analysis Results Check
        val hasAnyAiResult =
                analysis.aiAnalysis != null ||
                        analysis.aiSummary != null ||
                        analysis.industryComparison != null ||
                        analysis.investmentAdvice != null

        // Always show AI analysis button at the top
        if (onReanalyze != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                        onClick = { onReanalyze(analysis) },
                        modifier = Modifier.padding(bottom = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (hasAnyAiResult) AppColors.Surface else AppColors.Primary
                        ),
                        elevation = if (hasAnyAiResult) ButtonDefaults.elevation(0.dp, 2.dp) else ButtonDefaults.elevation()
                ) {
                    Icon(
                            if (hasAnyAiResult) Icons.Default.Refresh else Icons.Default.Psychology,
                            contentDescription = if (hasAnyAiResult) "Reanalyze" else "Analyze",
                            modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (hasAnyAiResult) "Reanalyze with AI" else "Analyze with AI")
                }
            }
        }
        
        // Show existing AI results or prompt message
        if (!hasAnyAiResult) {
            AiNotAvailableCard(analysis, null) // Pass null since button is already shown above
            return
        }

        // Summary Section
        if (analysis.aiSummary != null) {
            AiSummaryCard(analysis.aiSummary)
            Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))
        }

        // Detailed Analysis
        if (analysis.aiAnalysis != null) {
            AiDetailedAnalysisCard(analysis.aiAnalysis)
            Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))
        }

        // Investment Advice
        if (analysis.investmentAdvice != null) {
            AiInvestmentAdviceCard(analysis.investmentAdvice)
            Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))
        }

        // Industry Comparison
        if (analysis.industryComparison != null) {
            AiIndustryComparisonCard(analysis.industryComparison)
        }
    }
}

@Composable
private fun AiConfigurationCard() {
    Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = AppColors.InfoLight,
            elevation = 2.dp,
            shape = AppShapes.Medium
    ) {
        Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                    imageVector = Icons.Filled.Psychology,
                    contentDescription = "AI Configuration",
                    modifier = Modifier.size(56.dp),
                    tint = AppColors.Info
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                    text = "AI Financial Analysis Setup",
                    style = AppTypography.Headline2,
                    color = AppColors.OnSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                    text =
                            "Configure OpenRouter API to enable in-depth AI-powered financial analysis.",
                    style = AppTypography.Body1,
                    color = AppColors.OnSurfaceSecondary,
                    textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                    backgroundColor = Color.White,
                    elevation = 0.dp,
                    shape = AppShapes.Small,
                    border = BorderStroke(1.dp, AppColors.Divider)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    val configHelp = AiAnalysisService.getConfigurationHelp()
                    configHelp.forEach { line ->
                        if (line.isNotBlank()) {
                            Text(
                                    text = line,
                                    style = AppTypography.Body2,
                                    color =
                                            if (line.startsWith("•") || line.startsWith("-"))
                                                    AppColors.OnSurfaceSecondary
                                            else AppColors.OnSurface,
                                    modifier = Modifier.padding(vertical = 2.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiNotAvailableCard(
        analysis: FinancialAnalysis,
        onReanalyze: ((FinancialAnalysis) -> Unit)?
) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = AppColors.WarningLight,
            elevation = 2.dp,
            shape = AppShapes.Medium
    ) {
        Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = "No AI Analysis",
                    modifier = Modifier.size(48.dp),
                    tint = AppColors.Warning
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                    text = "AI Analysis Not Available",
                    style = AppTypography.Headline3,
                    color = AppColors.OnSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                    text =
                            "This analysis was performed without AI. Configure your OpenRouter API key in settings to enable detailed AI analysis.",
                    style = AppTypography.Body1,
                    color = AppColors.OnSurfaceSecondary,
                    textAlign = TextAlign.Center
            )

            if (onReanalyze != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                        onClick = { onReanalyze(analysis) },
                        colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Primary)
                ) {
                    Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reanalyze",
                            modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyze with AI")
                }
            }
        }
    }
}

@Composable
private fun AiSummaryCard(summary: String) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = AppColors.Surface,
            elevation = 2.dp,
            shape = AppShapes.Medium
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "AI Summary",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Summary", style = AppTypography.Headline3, color = AppColors.OnSurface)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                    text = summary,
                    style = AppTypography.Body1,
                    color = AppColors.OnSurfaceSecondary,
                    lineHeight = 26.sp
            )
        }
    }
}

@Composable
private fun AiDetailedAnalysisCard(aiAnalysis: AiAnalysisResult) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = AppColors.Surface,
            elevation = 2.dp,
            shape = AppShapes.Medium
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                        imageVector = Icons.Filled.Psychology,
                        contentDescription = "AI Analysis",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                        text = "Detailed Analysis",
                        style = AppTypography.Headline3,
                        color = AppColors.OnSurface
                )
                Spacer(modifier = Modifier.weight(1f))

                // Confidence Badge
                if (aiAnalysis.confidence > 0) {
                    Card(
                            backgroundColor =
                                    when {
                                        aiAnalysis.confidence >= 0.8 -> AppColors.SuccessLight
                                        aiAnalysis.confidence >= 0.6 -> AppColors.WarningLight
                                        else -> AppColors.ErrorLight
                                    },
                            elevation = 0.dp,
                            shape = AppShapes.Pill
                    ) {
                        Text(
                                text = "Confidence ${(aiAnalysis.confidence * 100).toInt()}%",
                                style = AppTypography.Caption,
                                color = AppColors.OnSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 요약
            Text(
                    text = aiAnalysis.summary,
                    style = AppTypography.Body1,
                    color = AppColors.OnSurfaceSecondary,
                    lineHeight = 26.sp
            )

            // Key Insights
            if (aiAnalysis.keyInsights.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                        text = "Key Insights",
                        style = AppTypography.Body1.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.OnSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                aiAnalysis.keyInsights.forEach { insight ->
                    Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            backgroundColor = AppColors.PrimaryLight,
                            elevation = 0.dp,
                            shape = AppShapes.Small
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Text(
                                    text = "•",
                                    style = AppTypography.Body1,
                                    color = AppColors.Primary,
                                    modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                    text = insight,
                                    style = AppTypography.Body1,
                                    color = AppColors.OnSurface,
                                    modifier = Modifier.weight(1f),
                                    lineHeight = 24.sp
                            )
                        }
                    }
                }
            }

            // Recommendations
            if (aiAnalysis.recommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                        text = "Recommendations",
                        style = AppTypography.Body1.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.OnSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                aiAnalysis.recommendations.forEach { recommendation ->
                    Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            backgroundColor = AppColors.SuccessLight,
                            elevation = 0.dp,
                            shape = AppShapes.Small
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Icon(
                                    imageVector = Icons.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = AppColors.Success,
                                    modifier = Modifier.size(20.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                    text = recommendation,
                                    style = AppTypography.Body1,
                                    color = AppColors.OnSurface,
                                    modifier = Modifier.weight(1f),
                                    lineHeight = 24.sp
                            )
                        }
                    }
                }
            }

            // Risk Assessment
            if (aiAnalysis.riskAssessment.isNotBlank()) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = AppColors.WarningLight,
                        elevation = 0.dp,
                        shape = AppShapes.Small
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = "Risk",
                                    tint = AppColors.Warning,
                                    modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                    text = "Risk Assessment",
                                    style = AppTypography.Body1.copy(fontWeight = FontWeight.Bold),
                                    color = AppColors.OnSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                                text = aiAnalysis.riskAssessment,
                                style = AppTypography.Body1,
                                color = AppColors.OnSurface,
                                lineHeight = 24.sp
                        )
                    }
                }
            }

            // AI 모델 정보
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = AppColors.Divider)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = AppColors.OnSurfaceSecondary,
                        modifier = Modifier.size(16.dp)
                )
                Text(
                        text =
                                "Powered by ${aiAnalysis.provider} (${aiAnalysis.model.split("/").last()})",
                        style = AppTypography.Caption,
                        color = AppColors.OnSurfaceSecondary
                )
            }
        }
    }
}

@Composable
private fun AiInvestmentAdviceCard(advice: String) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = AppColors.Surface,
            elevation = 2.dp,
            shape = AppShapes.Medium
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = "Investment Advice",
                        tint = AppColors.Warning,
                        modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                        text = "Investment Strategy",
                        style = AppTypography.Headline3,
                        color = AppColors.OnSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                    text = advice,
                    style = AppTypography.Body1,
                    color = AppColors.OnSurfaceSecondary,
                    lineHeight = 26.sp
            )
        }
    }
}

@Composable
private fun AiIndustryComparisonCard(comparison: String) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = AppColors.Surface,
            elevation = 2.dp,
            shape = AppShapes.Medium
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        imageVector = Icons.Filled.CompareArrows,
                        contentDescription = "Industry Comparison",
                        tint = AppColors.Secondary,
                        modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                        text = "Industry Comparison",
                        style = AppTypography.Headline3,
                        color = AppColors.OnSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                    text = comparison,
                    style = AppTypography.Body1,
                    color = AppColors.OnSurfaceSecondary,
                    lineHeight = 26.sp
            )
        }
    }
}
