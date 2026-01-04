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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import papyrus.FinancialAnalysis
import papyrus.FinancialMetric
import papyrus.FinancialRatio
import papyrus.FinancialTermExplanation
import papyrus.BeginnerInsight
import papyrus.FinancialHealthScore
import papyrus.HealthStatus
import papyrus.RatioCategory

/**
 * Helper function to format currency values
 */
private fun formatCurrency(value: Double): String {
    return when {
        value >= 1_000_000_000 -> String.format("$%.2fB", value / 1_000_000_000)
        value >= 1_000_000 -> String.format("$%.2fM", value / 1_000_000)
        value >= 1_000 -> String.format("$%.2fK", value / 1_000)
        else -> String.format("$%.2f", value)
    }
}

/**
 * Enhanced Quick Analyze Result View
 * Shows analysis results in a structured, modern UI
 */
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
                    colors = ButtonDefaults.outlinedButtonColors(
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
                colors = ButtonDefaults.outlinedButtonColors(
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
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
                modifier = Modifier
                    .background(
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
                SectionHeader(
                    title = "Document Content Preview",
                    icon = Icons.Outlined.Article
                )
                
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
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.SurfaceVariant, shape = AppShapes.Small)
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
 * Enhanced Financial Analysis View
 * Used for local file analysis with detailed metrics
 * Now includes beginner-friendly insights and explanations
 */
@Composable
fun FinancialAnalysisPanel(
    analysis: FinancialAnalysis,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    // 초보자 친화적 탭 추가
    val tabs = if (analysis.beginnerInsights.isNotEmpty() || analysis.healthScore != null) {
        listOf("📊 건강점수", "💡 쉬운 설명", "📖 용어사전", "📈 상세 지표", "📄 원본")
    } else {
        listOf("Overview", "Metrics", "Raw Data")
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Header
        FinancialAnalysisHeader(
            analysis = analysis,
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
        if (analysis.beginnerInsights.isNotEmpty() || analysis.healthScore != null) {
            when (selectedTab) {
                0 -> HealthScoreTab(analysis)
                1 -> BeginnerInsightsTab(analysis.beginnerInsights, analysis.keyTakeaways)
                2 -> TermGlossaryTab(analysis.termExplanations)
                3 -> FinancialRatiosTab(analysis.ratios, analysis.metrics)
                4 -> FinancialRawDataTab(analysis.rawContent)
            }
        } else {
            when (selectedTab) {
                0 -> FinancialOverviewTab(analysis)
                1 -> FinancialMetricsTab(analysis.metrics)
                2 -> FinancialRawDataTab(analysis.rawContent)
            }
        }
    }
}

/**
 * 재무 건전성 점수 탭 - 초보자가 한눈에 파악할 수 있는 점수 카드
 */
@Composable
private fun HealthScoreTab(analysis: FinancialAnalysis) {
    val scrollState = rememberScrollState()
    val healthScore = analysis.healthScore
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // 건강 점수 메인 카드
        if (healthScore != null) {
            HealthScoreMainCard(healthScore)
            
            Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))
            
            // 강점과 약점
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall)
            ) {
                // 강점 카드
                StrengthWeaknessCard(
                    title = "💪 강점",
                    items = healthScore.strengths,
                    backgroundColor = AppColors.SuccessLight,
                    modifier = Modifier.weight(1f)
                )
                
                // 약점 카드
                StrengthWeaknessCard(
                    title = "⚠️ 개선 필요",
                    items = healthScore.weaknesses,
                    backgroundColor = AppColors.WarningLight,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))
            
            // 권장사항
            if (healthScore.recommendations.isNotEmpty()) {
                RecommendationsCard(healthScore.recommendations)
            }
        }
        
        Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))
        
        // 핵심 요점
        if (analysis.keyTakeaways.isNotEmpty()) {
            KeyTakeawaysCard(analysis.keyTakeaways)
        }
        
        // 보고서 유형 설명
        if (analysis.reportTypeExplanation != null) {
            Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))
            ReportTypeCard(analysis.reportType, analysis.reportTypeExplanation)
        }
    }
}

@Composable
private fun HealthScoreMainCard(healthScore: FinancialHealthScore) {
    val scoreColor = when {
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
                text = "재무 건전성 점수",
                style = AppTypography.Headline3,
                color = AppColors.OnSurface,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))
            
            // 큰 점수 표시
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
                        text = "/ 100점",
                        style = AppTypography.Caption,
                        color = AppColors.OnSurfaceSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))
            
            // 프로그레스 바
            LinearProgressIndicator(
                progress = healthScore.overallScore / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(AppShapes.Pill),
                color = scoreColor,
                backgroundColor = scoreColor.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))
            
            // 요약 설명
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
            SectionHeader(
                title = "💡 투자 팁 & 권장사항",
                icon = Icons.Outlined.Lightbulb
            )
            
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
            SectionHeader(
                title = "📌 핵심 요점",
                icon = Icons.Outlined.Star
            )
            
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
            
            Text(
                text = explanation,
                style = AppTypography.Body2,
                color = AppColors.OnSurface
            )
        }
    }
}

/**
 * 초보자 인사이트 탭 - 쉬운 설명
 */
@Composable
private fun BeginnerInsightsTab(
    insights: List<BeginnerInsight>,
    keyTakeaways: List<String>
) {
    if (insights.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.Lightbulb,
            title = "인사이트 분석 중",
            description = "초보자용 인사이트를 생성하려면 더 많은 재무 데이터가 필요합니다."
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(AppDimens.PaddingMedium)
        ) {
            items(insights) { insight ->
                BeginnerInsightCard(insight)
            }
        }
    }
}

@Composable
private fun BeginnerInsightCard(insight: BeginnerInsight) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
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
                    Text(
                        text = insight.emoji,
                        fontSize = 28.sp
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
private fun InsightSection(
    title: String,
    content: String,
    backgroundColor: Color
) {
    Card(
        backgroundColor = backgroundColor,
        elevation = 0.dp,
        shape = AppShapes.Small
    ) {
        Column(modifier = Modifier.padding(AppDimens.PaddingSmall)) {
            Text(
                text = title,
                style = AppTypography.Caption,
                fontWeight = FontWeight.Bold,
                color = AppColors.OnSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                style = AppTypography.Body2,
                color = AppColors.OnSurface
            )
        }
    }
}

/**
 * 용어 사전 탭
 */
@Composable
private fun TermGlossaryTab(terms: List<FinancialTermExplanation>) {
    if (terms.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.Book,
            title = "용어 사전",
            description = "재무 용어 설명이 로드되지 않았습니다."
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall)
        ) {
            items(terms) { term ->
                TermExplanationCard(term)
            }
        }
    }
}

@Composable
private fun TermExplanationCard(term: FinancialTermExplanation) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
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

/**
 * 재무 비율 탭 - 상세 지표
 */
@Composable
private fun FinancialRatiosTab(
    ratios: List<FinancialRatio>,
    metrics: List<FinancialMetric>
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        if (ratios.isNotEmpty()) {
            Text(
                text = "📊 핵심 재무 비율",
                style = AppTypography.Headline3,
                fontWeight = FontWeight.Bold,
                color = AppColors.OnSurface
            )
            
            Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))
            
            ratios.forEach { ratio ->
                RatioDetailCard(ratio)
                Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))
            }
        }
        
        if (metrics.isNotEmpty()) {
            Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))
            
            Text(
                text = "📈 추출된 재무 지표",
                style = AppTypography.Headline3,
                fontWeight = FontWeight.Bold,
                color = AppColors.OnSurface
            )
            
            Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))
            
            metrics.forEach { metric ->
                MetricDetailCard(metric)
                Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))
            }
        }
        
        if (ratios.isEmpty() && metrics.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Analytics,
                title = "지표 없음",
                description = "문서에서 재무 지표를 추출할 수 없었습니다."
            )
        }
    }
}

@Composable
private fun RatioDetailCard(ratio: FinancialRatio) {
    val statusColor = when (ratio.healthStatus) {
        HealthStatus.EXCELLENT -> AppColors.Success
        HealthStatus.GOOD -> Color(0xFF4CAF50)
        HealthStatus.NEUTRAL -> AppColors.Warning
        HealthStatus.CAUTION -> Color(0xFFFF9800)
        HealthStatus.WARNING -> AppColors.Error
    }
    
    val statusEmoji = when (ratio.healthStatus) {
        HealthStatus.EXCELLENT -> "🌟"
        HealthStatus.GOOD -> "👍"
        HealthStatus.NEUTRAL -> "📊"
        HealthStatus.CAUTION -> "⚠️"
        HealthStatus.WARNING -> "🚨"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = AppDimens.CardElevation,
        shape = AppShapes.Medium,
        backgroundColor = statusColor.copy(alpha = 0.05f)
    ) {
        Column(modifier = Modifier.padding(AppDimens.PaddingMedium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = statusEmoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = ratio.name,
                            style = AppTypography.Subtitle1,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.OnSurface
                        )
                        Text(
                            text = ratio.description,
                            style = AppTypography.Caption,
                            color = AppColors.OnSurfaceSecondary
                        )
                    }
                }
                
                Text(
                    text = ratio.formattedValue,
                    style = AppTypography.Headline3,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
            
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

@Composable
private fun FinancialAnalysisHeader(
    analysis: FinancialAnalysis,
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
            colors = ButtonDefaults.outlinedButtonColors(
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

@Composable
private fun FinancialOverviewTab(analysis: FinancialAnalysis) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
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
    val groupedMetrics = metrics.groupBy { metric ->
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
    
    val categories = listOf(
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
                        topValue = categoryMetrics.firstOrNull()?.let { 
                            if (it.rawValue != null) formatCurrency(it.rawValue) else it.value 
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.PaddingMedium)
        ) {
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
                    modifier = Modifier
                        .background(color, shape = CircleShape)
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
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall)
        ) {
            items(metrics) { metric ->
                MetricDetailCard(metric)
            }
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
                SectionHeader(
                    title = "Raw Document Content",
                    icon = Icons.Outlined.Code
                )
                
                Text(
                    text = "${rawContent.length} characters",
                    style = AppTypography.Caption,
                    color = AppColors.OnSurfaceSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))
            
            Divider(color = AppColors.Divider)
            
            Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
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

/**
 * Loading state for analysis
 */
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

/**
 * Error state for analysis
 */
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
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppColors.OnSurfaceSecondary
                ),
                shape = AppShapes.Small
            ) {
                Text("Close")
            }
            
            if (onRetry != null) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
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
