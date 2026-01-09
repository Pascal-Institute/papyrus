package papyrus.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.pascal.institute.ahmes.model.XbrlCompanyFact
import com.pascal.institute.ahmes.parser.XbrlCompanyFactsExtractor
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
import papyrus.core.secApiClient

/** Map icon name to Material Icon ImageVector */
private fun getIconForName(iconName: String): ImageVector {
    return when (iconName) {
        "Business" -> Icons.Filled.Business
        "AttachMoney" -> Icons.Filled.AttachMoney
        "Balance" -> Icons.Filled.Balance
        "Payments" -> Icons.Filled.Payments
        "Warning" -> Icons.Filled.Warning
        "Description" -> Icons.Filled.Description
        "TrendingUp" -> Icons.Filled.TrendingUp
        "TrendingDown" -> Icons.Filled.TrendingDown
        "AccountBalance" -> Icons.Filled.AccountBalance
        "Assessment" -> Icons.Filled.Assessment
        "CheckCircle" -> Icons.Filled.CheckCircle
        "Error" -> Icons.Filled.Error
        "Info" -> Icons.Filled.Info
        "Search" -> Icons.Filled.Search
        "Star" -> Icons.Filled.Star
        "Lightbulb" -> Icons.Outlined.Lightbulb
        "Analytics" -> Icons.Filled.Analytics
        "MonetizationOn" -> Icons.Filled.MonetizationOn
        "ShowChart" -> Icons.Filled.ShowChart
        "BarChart" -> Icons.Filled.BarChart
        "PieChart" -> Icons.Filled.PieChart
        "Receipt" -> Icons.Filled.Receipt
        "CreditCard" -> Icons.Filled.CreditCard
        "Wallet" -> Icons.Filled.AccountBalanceWallet
        "Person" -> Icons.Filled.Person
        "Settings" -> Icons.Filled.Settings
        "Article" -> Icons.Filled.Article
        "Speed" -> Icons.Filled.Speed
        "Computer" -> Icons.Filled.Computer
        "Public" -> Icons.Filled.Public
        "Language" -> Icons.Filled.Language
        "PushPin" -> Icons.Filled.PushPin
        else -> Icons.Filled.Info // Default fallback
    }
}

/** Helper function to format currency values with BigDecimal precision */
private fun formatCurrency(value: java.math.BigDecimal): String {
    val absValue = value.abs()
    val sign = if (value.signum() < 0) "-" else ""
    val billion = java.math.BigDecimal("1000000000")
    val million = java.math.BigDecimal("1000000")
    val thousand = java.math.BigDecimal("1000")

    return when {
        absValue >= billion ->
                String.format(
                        "%s$%.2fB",
                        sign,
                        absValue.divide(billion, 2, java.math.RoundingMode.HALF_UP).toDouble()
                )
        absValue >= million ->
                String.format(
                        "%s$%.2fM",
                        sign,
                        absValue.divide(million, 2, java.math.RoundingMode.HALF_UP).toDouble()
                )
        absValue >= thousand ->
                String.format(
                        "%s$%.2fK",
                        sign,
                        absValue.divide(thousand, 2, java.math.RoundingMode.HALF_UP).toDouble()
                )
        else ->
                String.format(
                        "%s$%.2f",
                        sign,
                        absValue.setScale(2, java.math.RoundingMode.HALF_UP).toDouble()
                )
    }
}

/** Overload for String input (BigDecimal as String) */
private fun formatCurrency(value: String): String {
    return try {
        formatCurrency(java.math.BigDecimal(value))
    } catch (e: NumberFormatException) {
        value
    }
}

/** Deprecated: Use BigDecimal version for precision */
@Deprecated("Use formatCurrency(BigDecimal) or formatCurrency(String)")
private fun formatCurrency(value: Double): String {
    return formatCurrency(java.math.BigDecimal(value.toString()))
}

/**
 * Helper function to format number values (for shares, quantities, etc.) with BigDecimal precision
 */
private fun formatNumber(value: java.math.BigDecimal): String {
    val absValue = value.abs()
    val sign = if (value.signum() < 0) "-" else ""
    val billion = java.math.BigDecimal("1000000000")
    val million = java.math.BigDecimal("1000000")
    val thousand = java.math.BigDecimal("1000")

    return when {
        absValue >= billion ->
                String.format(
                        "%s%.2fB",
                        sign,
                        absValue.divide(billion, 2, java.math.RoundingMode.HALF_UP).toDouble()
                )
        absValue >= million ->
                String.format(
                        "%s%.2fM",
                        sign,
                        absValue.divide(million, 2, java.math.RoundingMode.HALF_UP).toDouble()
                )
        absValue >= thousand ->
                String.format(
                        "%s%.2fK",
                        sign,
                        absValue.divide(thousand, 2, java.math.RoundingMode.HALF_UP).toDouble()
                )
        else ->
                String.format(
                        "%s%.0f",
                        sign,
                        absValue.setScale(0, java.math.RoundingMode.HALF_UP).toDouble()
                )
    }
}

/** Overload for String input */
private fun formatNumber(value: String): String {
    return try {
        formatNumber(java.math.BigDecimal(value))
    } catch (e: NumberFormatException) {
        value
    }
}

/** Deprecated: Use BigDecimal version */
@Deprecated("Use formatNumber(BigDecimal) or formatNumber(String)")
private fun formatNumber(value: Double): String {
    return formatNumber(java.math.BigDecimal(value.toString()))
}

/** Check if metric represents a quantity rather than currency */
private fun isQuantityMetric(metricName: String): Boolean {
    val lowerName = metricName.lowercase()
    return lowerName.contains("shares") ||
            lowerName.contains("outstanding") ||
            lowerName.contains("quantity") ||
            lowerName.contains("count") ||
            lowerName.contains("number of")
}

/** Parse metric value string to double for formatting */
private fun parseMetricValue(valueString: String): Double? {
    return try {
        // Check for negative sign first
        val isNegative = valueString.trim().startsWith("-")

        // Remove currency symbols, commas, whitespace, and negative sign
        val cleaned = valueString.replace("[$,\\s-]".toRegex(), "")

        // Handle existing M/B/K suffixes
        val parsedValue =
                when {
                    cleaned.endsWith("B", ignoreCase = true) ->
                            cleaned.dropLast(1).toDoubleOrNull()?.times(1_000_000_000)
                    cleaned.endsWith("M", ignoreCase = true) ->
                            cleaned.dropLast(1).toDoubleOrNull()?.times(1_000_000)
                    cleaned.endsWith("K", ignoreCase = true) ->
                            cleaned.dropLast(1).toDoubleOrNull()?.times(1_000)
                    else -> cleaned.toDoubleOrNull()
                }

        // Apply negative sign if present
        parsedValue?.let { if (isNegative) -it else it }
    } catch (e: Exception) {
        null
    }
}

/** Translate financial metric names to Korean */
private fun getKoreanTranslation(englishName: String): String? {
    return when (englishName.lowercase().trim()) {
        "revenue", "revenues" -> "매출"
        "net income", "net income (loss)" -> "순이익"
        "net loss" -> "순손실"
        "total assets" -> "총자산"
        "current assets", "total current assets" -> "유동자산"
        "total liabilities" -> "총부채"
        "current liabilities", "total current liabilities" -> "유동부채"
        "stockholders' equity", "total equity", "shareholders' equity" -> "자본총계"
        "cash and cash equivalents" -> "현금및현금성자산"
        "operating income" -> "영업이익"
        "operating expenses" -> "영업비용"
        "gross profit" -> "매출총이익"
        "cost of revenue", "cost of goods sold" -> "매출원가"
        "accounts receivable" -> "매출채권"
        "accounts payable" -> "매입채무"
        "inventory" -> "재고자산"
        "long-term debt" -> "장기부채"
        "retained earnings" -> "이익잉여금"
        "operating cash flow" -> "영업현금흐름"
        "free cash flow" -> "잉여현금흐름"
        "capital expenditures", "capex" -> "자본적지출"
        "total expenses" -> "총비용"
        "research and development", "r&d" -> "연구개발비"
        "selling, general and administrative", "sg&a" -> "판매관리비"
        "depreciation" -> "감가상각비"
        "amortization" -> "무형자산상각비"
        "interest expense" -> "이자비용"
        "income tax" -> "법인세"
        "earnings per share", "eps" -> "주당순이익"
        "dividends" -> "배당금"
        "working capital" -> "운전자본"
        else -> null
    }
}

/** Format metric name with English and Korean translation */
private fun formatMetricName(name: String): String {
    val korean = getKoreanTranslation(name)
    return if (korean != null) {
        "$name ($korean)"
    } else {
        name
    }
}

/** Get Korean explanation for financial terms */
private fun getFinancialTermExplanation(englishName: String): String? {
    return when (englishName.lowercase().trim()) {
        "revenue", "revenues" -> "회사가 제품이나 서비스를 판매하여 벌어들인 총 수입입니다. 매출이 높을수록 사업 규모가 큽니다."
        "net income", "net income (loss)" -> "모든 비용과 세금을 제외하고 남은 최종 이익입니다. 기업의 실제 수익성을 보여줍니다."
        "net loss" -> "수익보다 비용이 더 많아 발생한 손실입니다. 적자 상태를 의미합니다."
        "total assets" -> "회사가 보유한 모든 자산(현금, 재고, 부동산, 설비 등)의 총합입니다."
        "current assets", "total current assets" -> "1년 내에 현금으로 바꿀 수 있는 자산입니다. 단기 지급 능력을 보여줍니다."
        "total liabilities" -> "회사가 갚아야 할 모든 빚의 총합입니다. 부채가 많으면 재무 부담이 큽니다."
        "current liabilities", "total current liabilities" ->
                "1년 내에 갚아야 하는 빚입니다. 단기 부채 상환 능력이 중요합니다."
        "stockholders' equity", "total equity", "shareholders' equity" ->
                "자산에서 부채를 뺀 순자산입니다. 주주에게 귀속되는 회사의 실제 가치입니다."
        "cash and cash equivalents" -> "즉시 사용 가능한 현금과 현금성 자산입니다. 유동성이 가장 높은 자산입니다."
        "operating income" -> "본업에서 벌어들인 이익입니다. 회사의 핵심 사업 수익성을 나타냅니다."
        "operating expenses" -> "사업 운영에 필요한 비용(인건비, 마케팅비 등)입니다. 효율적 관리가 중요합니다."
        "gross profit" -> "매출에서 제품 원가만 뺀 이익입니다. 제품의 수익성을 직접 보여줍니다."
        "cost of revenue", "cost of goods sold" -> "제품 생산이나 서비스 제공에 직접 든 비용입니다. 낮을수록 수익성이 좋습니다."
        "accounts receivable" -> "상품을 팔았지만 아직 받지 못한 돈입니다. 빨리 회수할수록 좋습니다."
        "accounts payable" -> "상품을 받았지만 아직 지불하지 않은 돈입니다. 적절한 관리가 필요합니다."
        "inventory" -> "판매를 위해 보유한 재고 상품입니다. 너무 많으면 현금이 묶입니다."
        "long-term debt" -> "1년 이후에 갚아야 하는 장기 부채입니다. 이자 부담을 고려해야 합니다."
        "retained earnings" -> "회사가 벌어서 쌓아둔 이익입니다. 재투자나 배당금 지급에 사용됩니다."
        "operating cash flow" -> "영업 활동으로 실제 벌어들인 현금입니다. 현금 창출 능력을 보여줍니다."
        "free cash flow" -> "투자 후 남은 여유 현금입니다. 배당이나 부채 상환에 사용 가능합니다."
        "capital expenditures", "capex" -> "설비, 부동산 등 장기 자산 구매에 쓴 돈입니다. 미래 성장을 위한 투자입니다."
        "total expenses" -> "회사가 지출한 모든 비용의 합계입니다. 효율적 관리가 수익성을 높입니다."
        "research and development", "r&d" -> "신제품 개발과 연구에 투자한 비용입니다. 미래 경쟁력을 위한 투자입니다."
        "selling, general and administrative", "sg&a" ->
                "판매, 관리, 일반 업무에 든 비용입니다. 인건비, 마케팅비 등이 포함됩니다."
        "depreciation" -> "시간이 지나면서 자산 가치가 줄어드는 것을 비용으로 인식한 것입니다."
        "amortization" -> "무형자산(특허, 영업권 등)의 가치 감소를 비용으로 처리한 것입니다."
        "interest expense" -> "빌린 돈에 대해 지급하는 이자입니다. 부채가 많으면 이자 부담도 큽니다."
        "income tax" -> "이익에 대해 정부에 내는 세금입니다. 법인세율에 따라 달라집니다."
        "earnings per share", "eps" -> "주식 한 주당 벌어들인 이익입니다. 주주 가치를 직접 나타냅니다."
        "dividends" -> "주주에게 지급하는 이익 배당금입니다. 안정적인 배당은 투자 매력도를 높입니다."
        "working capital" -> "유동자산에서 유동부채를 뺀 금액입니다. 단기 재무 건전성을 보여줍니다."
        "profit" -> "수익에서 비용을 뺀 이익입니다. 기업의 수익성을 나타냅니다."
        "cash" -> "회사가 보유한 현금입니다. 가장 유동성이 높은 자산입니다."
        "marketable securities" -> "단기간 내 현금화 가능한 유가증권입니다. 예금이나 채권 등이 포함됩니다."
        "other current assets" -> "1년 내 현금화 가능한 기타 자산입니다. 선급금, 미수금 등이 포함됩니다."
        "accumulated deficit" -> "누적된 손실액입니다. 지속적인 적자로 쌓인 마이너스 자본입니다."
        "purchases of marketable securities" -> "유가증권 구매에 사용한 현금입니다. 단기 투자 활동입니다."
        "shares outstanding" -> "발행되어 유통 중인 주식 수입니다. 이 수로 EPS 등을 계산합니다."
        else -> null
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
                        benchmarkText = "• 우수: 15-20% 이상\n• 양호: 10-15%\n• 평균: 7-10%\n• 주의: 7% 미만",
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
                        benchmarkText = "• 매우 안전: 8배 이상\n• 안전: 4-8배\n• 평균: 2.5-4배\n• 위험: 1.5배 미만",
                        investorPerspective =
                                "영업이익으로 이자비용을 몇 번 갚을 수 있는지 나타냅니다. 2.5배 미만이면 부채 상환 능력에 주의가 필요합니다.",
                        example = "건강한 기업은 최소 5배 이상을 유지합니다."
                )
        else -> null
    }
}

/** Enhanced Analyze Result View Shows analysis results in a structured, modern UI */
@Composable
fun AnalyzeResultView(
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
        AnalyzeHeader(
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
            0 -> AnalyzeSummaryTab(analysisSummary)
            1 -> AnalyzeContentTab(analysisContent)
        }
    }
}

@Composable
private fun AnalyzeHeader(
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
private fun AnalyzeSummaryTab(summary: String) {
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
private fun AnalyzeContentTab(content: String) {
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

                            Spacer(modifier = Modifier.width(AppDimens.PaddingMedium))

                            // Copy to clipboard button
                            Button(
                                    onClick = {
                                        try {
                                            val clipboard =
                                                    java.awt.Toolkit.getDefaultToolkit()
                                                            .systemClipboard
                                            val stringSelection =
                                                    java.awt.datatransfer.StringSelection(content)
                                            clipboard.setContents(stringSelection, null)
                                            copySuccess = true
                                        } catch (e: Exception) {
                                            println("Failed to copy to clipboard: ${e.message}")
                                        }
                                    },
                                    colors =
                                            ButtonDefaults.buttonColors(
                                                    backgroundColor = AppColors.Primary
                                            ),
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
        modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showGlossaryDialog by remember { mutableStateOf(false) }

    val hasXbrlTab = analysis.xbrlMetrics.isNotEmpty() || analysis.cik != null

    val tabs = buildList {
        val hasInsights = analysis.beginnerInsights.isNotEmpty() || analysis.aiSentiment != null
        val hasHealth = analysis.healthScore != null

        if (hasHealth) add("Health Score")
        if (hasInsights) add("Insights")

        if (!hasHealth && !hasInsights) {
            add("Overview")
        }

        add("Financials")
        if (hasXbrlTab) add("XBRL")
        add("Raw Data")
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
        AnalysisTabRow(selectedTab = selectedTab, onTabSelected = { selectedTab = it }, tabs = tabs)

        Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

        // Content based on selected tab
        when (tabs.getOrNull(selectedTab)) {
            "Health Score" -> HealthScoreTab(analysis)
            "Overview" -> FinancialOverviewTab(analysis)
            "Insights" -> BeginnerInsightsTab(analysis)
            "Financials" -> FinancialsTab(analysis.ratios, analysis.metrics)
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
            val facts = secApiClient.getCompanyFacts(cik)
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
                                horizontalArrangement = Arrangement.SpaceBetween
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

                        if (!metric.period.isNullOrBlank() || metric.source.isNotBlank()) {
                            Text(
                                    text =
                                            buildString {
                                                if (!metric.period.isNullOrBlank())
                                                        append("Period: ${metric.period}")
                                                if (!metric.period.isNullOrBlank() &&
                                                                metric.source.isNotBlank()
                                                )
                                                        append(" · ")
                                                if (metric.source.isNotBlank())
                                                        append(metric.source)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                text = factsError ?: "Failed to load company facts",
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
                                    horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                        text = fact.label,
                                        style = AppTypography.Body2,
                                        color = AppColors.OnSurface,
                                        modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                        text = fact.value?.toString() ?: "—",
                                        style = AppTypography.Body2,
                                        color = AppColors.OnSurface,
                                        textAlign = TextAlign.End
                                )
                            }
                            Text(
                                    text =
                                            buildString {
                                                if (!fact.periodEnd.isNullOrBlank())
                                                        append("End: ${fact.periodEnd}")
                                                if (!fact.periodEnd.isNullOrBlank() &&
                                                                !fact.unit.isNullOrBlank()
                                                )
                                                        append(" · ")
                                                if (!fact.unit.isNullOrBlank())
                                                        append("Unit: ${fact.unit}")
                                                if ((fact.periodEnd != null || fact.unit != null))
                                                        append(" · ")
                                                append(fact.concept)
                                            },
                                    style = AppTypography.Caption,
                                    color = AppColors.OnSurfaceSecondary
                            )
                            Divider(
                                    color = AppColors.Divider,
                                    modifier = Modifier.padding(vertical = 8.dp)
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

            // Summary description
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
                            text = item.trim(),
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
                        text = recommendation.trim(),
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
                        text = takeaway.trim(),
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
                    text = explanation.trim(),
                    style = AppTypography.Body2,
                    color = AppColors.OnSurface
            )
        }
    }
}

/** Beginner insights tab - easy explanations */
@Composable
private fun BeginnerInsightsTab(analysis: FinancialAnalysis) {
    val insights = analysis.beginnerInsights

    if (insights.isEmpty() && analysis.aiSentiment == null) {
        EmptyState(
                icon = Icons.Outlined.Lightbulb,
                title = "인사이트 분석 중",
                description = "초보자용 인사이트를 생성하려면 더 많은 재무 데이터가 필요합니다."
        )
    } else {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // AI Analysis (Prioritized if available)
            if (analysis.aiSentiment != null) {
                AiAnalysisContent(analysis)
                Spacer(modifier = Modifier.height(24.dp))

                if (insights.isNotEmpty()) {
                    Text(
                            text = "Rule-based Insights",
                            style = AppTypography.Headline3,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Rule-based insights
            insights.forEach { insight ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    BeginnerInsightCard(insight)
                }
            }
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
                            imageVector = getIconForName(insight.icon),
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
                                text = insight.summary.trim(),
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
            Text(text = content.trim(), style = AppTypography.Body2, color = AppColors.OnSurface)
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

                TermGlossaryTab(terms = terms, modifier = Modifier.fillMaxWidth().weight(1f))
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

                    // Analogy
                    Card(
                            backgroundColor = AppColors.InfoLight,
                            elevation = 0.dp,
                            shape = AppShapes.Small
                    ) {
                        Column(modifier = Modifier.padding(AppDimens.PaddingSmall)) {
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
                        Column(modifier = Modifier.padding(AppDimens.PaddingSmall)) {
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

/** Financials tab - comprehensive view of ratios and concrete financial metrics */
@Composable
private fun FinancialsTab(ratios: List<FinancialRatio>, metrics: List<FinancialMetric>) {
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
                    text = "Financial Data",
                    style = AppTypography.Headline3,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.OnSurface
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showVisualization = !showVisualization }) {
                    Icon(
                            if (showVisualization) Icons.AutoMirrored.Filled.ViewList
                            else Icons.Default.BarChart,
                            contentDescription = "Toggle View",
                            tint = AppColors.Primary
                    )
                }
            }
        }

        // Financial Metrics (Concrete values: dollars, quantities, etc.)
        if (metrics.isNotEmpty()) {
            Text(
                    text = "Financial Metrics (Concrete Values)",
                    style = AppTypography.Headline3,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary,
                    modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                    text =
                            "Extracted dollar amounts, quantities, and other absolute values from financial statements",
                    style = AppTypography.Body2,
                    color = AppColors.OnSurfaceSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
            )

            // Display metrics in a single card
            SimpleMetricsCard(metrics)

            Spacer(modifier = Modifier.height(AppDimens.PaddingLarge))
            Divider(color = AppColors.Divider, thickness = 2.dp)
            Spacer(modifier = Modifier.height(AppDimens.PaddingLarge))
        }

        // Financial Ratios (Calculated percentages and ratios)
        if (ratios.isNotEmpty()) {
            Text(
                    text = "Financial Ratios (Calculated Metrics)",
                    style = AppTypography.Headline3,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary,
                    modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                    text =
                            "Calculated financial ratios and percentages for analysis and benchmarking",
                    style = AppTypography.Body2,
                    color = AppColors.OnSurfaceSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
            )

            // Detailed ratio cards
            ratios.forEach { ratio ->
                EnhancedRatioCard(ratio, showVisualization)
                Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))
            }
        }

        if (ratios.isEmpty() && metrics.isEmpty()) {
            EmptyState(
                    icon = Icons.Outlined.Analytics,
                    title = "No Financial Data Found",
                    description = "Unable to extract financial metrics or ratios from document."
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
                                category.replace("_", " ").lowercase().replaceFirstChar {
                                    it.uppercase()
                                },
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
            backgroundColor = AppColors.Surface
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
                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.Info,
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
                    Surface(shape = AppShapes.Pill, color = statusColor.copy(alpha = 0.2f)) {
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

                // Expanded section with detailed info
                AnimatedVisibility(visible = isExpanded) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        if (benchmark != null) {
                            Card(
                                    backgroundColor = AppColors.InfoLight,
                                    elevation = 0.dp,
                                    shape = AppShapes.Small
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                                Icons.Default.Analytics,
                                                contentDescription = null,
                                                tint = AppColors.Info,
                                                modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                                text = "산업 벤치마크",
                                                style = AppTypography.Subtitle2,
                                                fontWeight = FontWeight.Bold,
                                                color = AppColors.OnSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                            text = benchmark.benchmarkText,
                                            style = AppTypography.Body2,
                                            color = AppColors.OnSurface,
                                            lineHeight = 20.sp
                                    )
                                    if (benchmark.example.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                                text = "예시: ${benchmark.example}",
                                                style = AppTypography.Caption,
                                                color = AppColors.OnSurfaceSecondary,
                                                fontStyle =
                                                        androidx.compose.ui.text.font.FontStyle
                                                                .Italic
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                    backgroundColor = AppColors.WarningLight.copy(alpha = 0.3f),
                                    elevation = 0.dp,
                                    shape = AppShapes.Small
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                                Icons.Default.Lightbulb,
                                                contentDescription = null,
                                                tint = AppColors.Warning,
                                                modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                                text = "투자자 관점",
                                                style = AppTypography.Subtitle2,
                                                fontWeight = FontWeight.Bold,
                                                color = AppColors.OnSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                            text = benchmark.investorPerspective,
                                            style = AppTypography.Body2,
                                            color = AppColors.OnSurface,
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

/** Display financial metrics in a simple card */
@Composable
private fun SimpleMetricsCard(metrics: List<FinancialMetric>) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = AppDimens.CardElevation,
            shape = AppShapes.Medium,
            backgroundColor = AppColors.Surface
    ) {
        Column(modifier = Modifier.padding(AppDimens.PaddingMedium)) {
            // Display each metric with value and raw value if available
            metrics.forEach { metric ->
                var isHovered by remember { mutableStateOf(false) }
                val interactionSource = remember { MutableInteractionSource() }

                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                            .hoverable(interactionSource)
                    ) {
                        // Track hover state
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect { interaction ->
                                when (interaction) {
                                    is HoverInteraction.Enter -> isHovered = true
                                    is HoverInteraction.Exit -> isHovered = false
                                }
                            }
                        }

                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                    text = formatMetricName(metric.name),
                                    style = AppTypography.Body1,
                                    fontWeight = FontWeight.Medium,
                                    color = AppColors.OnSurface,
                                    modifier = Modifier.weight(1f)
                            )

                            val displayValue =
                                    if (isQuantityMetric(metric.name)) {
                                        // For shares and
                                        // quantities, use number
                                        // formatting without $
                                        metric.rawValue?.let { formatNumber(it) }
                                                ?: parseMetricValue(metric.value)?.let {
                                                    formatNumber(it)
                                                }
                                                        ?: metric.value
                                    } else {
                                        // For currency values, use
                                        // currency formatting with
                                        // $
                                        metric.rawValue?.let { formatCurrency(it) }
                                                ?: parseMetricValue(metric.value)?.let {
                                                    formatCurrency(it)
                                                }
                                                        ?: metric.value
                                    }

                            Text(
                                    text = displayValue,
                                    style = AppTypography.Subtitle1,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.Primary
                            )
                        }

                        // Show explanation on hover
                        AnimatedVisibility(
                                visible = isHovered,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                        ) {
                            val explanation = getFinancialTermExplanation(metric.name)
                            if (explanation != null) {
                                Card(
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        backgroundColor = AppColors.InfoLight,
                                        elevation = 2.dp,
                                        shape = AppShapes.Small
                                ) {
                                    Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                                Icons.Outlined.Info,
                                                contentDescription = null,
                                                tint = AppColors.Info,
                                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                                text = explanation,
                                                style = AppTypography.Caption,
                                                color = AppColors.OnSurface,
                                                lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Add subtle divider between metrics
                        if (metric != metrics.last()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Divider(
                                    color = AppColors.Divider.copy(alpha = 0.3f),
                                    thickness = 0.5.dp
                            )
                        }
                    }
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
        // Financial summary card (new component)
        if (analysis.extendedMetrics.isNotEmpty()) {
            FinancialSummaryCard(analysis)
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
                    val categoryMetrics = groupedMetrics[category] ?: emptyList()
                    MetricCategoryCard(
                            category = category,
                            icon = icon,
                            color = color,
                            count = categoryMetrics.size,
                            topValue =
                                    categoryMetrics.firstOrNull()?.let {
                                        it.rawValue?.let { raw -> formatCurrency(raw) } ?: it.value
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

            val rawValue = metric.rawValue
            if (rawValue != null) {
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
                            text = formatCurrency(rawValue),
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

                            Spacer(modifier = Modifier.width(AppDimens.PaddingMedium))

                            // Copy to clipboard button
                            Button(
                                    onClick = {
                                        try {
                                            val clipboard =
                                                    java.awt.Toolkit.getDefaultToolkit()
                                                            .systemClipboard
                                            val stringSelection =
                                                    java.awt.datatransfer.StringSelection(
                                                            rawContent
                                                    )
                                            clipboard.setContents(stringSelection, null)
                                            copySuccess = true
                                        } catch (e: Exception) {
                                            println("Failed to copy to clipboard: ${e.message}")
                                        }
                                    },
                                    colors =
                                            ButtonDefaults.buttonColors(
                                                    backgroundColor = AppColors.Primary
                                            ),
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
                                Modifier.fillMaxWidth().padding(bottom = AppDimens.PaddingMedium),
                        backgroundColor = AppColors.InfoLight,
                        shape = AppShapes.Small,
                        elevation = 0.dp
                ) {
                    Column(modifier = Modifier.padding(AppDimens.PaddingMedium)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                    Icons.Default.Assessment,
                                    contentDescription = null,
                                    tint = AppColors.Info,
                                    modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                    "추출된 재무 데이터: ${analysis.extendedMetrics.size}개 항목",
                                    style = AppTypography.Body1.copy(fontWeight = FontWeight.Bold),
                                    color = AppColors.Info
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Group metrics by category
                        val groupedMetrics = analysis.extendedMetrics.groupBy { it.category }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                val halfSize = groupedMetrics.size / 2
                                groupedMetrics.entries.take(halfSize).forEach { (category, metrics)
                                    ->
                                    Text(
                                            "• ${category.name.replace("_", " ")}: ${metrics.size}개",
                                            style = AppTypography.Caption,
                                            color = AppColors.OnSurface
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                val halfSize = groupedMetrics.size / 2
                                groupedMetrics.entries.drop(halfSize).forEach { (category, metrics)
                                    ->
                                    Text(
                                            "• ${category.name.replace("_", " ")}: ${metrics.size}개",
                                            style = AppTypography.Caption,
                                            color = AppColors.OnSurface
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
        if (revenueMetrics.isEmpty() && balanceMetrics.isEmpty() && cashFlowMetrics.isEmpty()) {
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
                Box(modifier = Modifier.size(10.dp).background(qualityColor, CircleShape))
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
                                Modifier.background(accentColor.copy(alpha = 0.1f), AppShapes.Pill)
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
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
                        text = "Confidence: ${String.format("%.0f", metric.confidence * 100)}%",
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
fun FinancialSummaryCard(analysis: FinancialAnalysis, modifier: Modifier = Modifier) {
    val metrics = analysis.extendedMetrics

    // Extract key metrics (prefer extended metrics, fallback to AI healed metrics)
    val revenue =
            metrics.find { it.category == MetricCategory.REVENUE }
                    ?: analysis.metrics.find { it.name.contains("Revenue (AI)") }?.let {
                        ExtendedFinancialMetric(
                                name = it.name,
                                value = it.value,
                                rawValue = it.rawValue,
                                context = it.context,
                                category = MetricCategory.REVENUE
                        )
                    }
    val netIncome =
            metrics.find { it.category == MetricCategory.NET_INCOME }
                    ?: analysis.metrics.find { it.name.contains("Net Income (AI)") }?.let {
                        ExtendedFinancialMetric(
                                name = it.name,
                                value = it.value,
                                rawValue = it.rawValue,
                                context = it.context,
                                category = MetricCategory.NET_INCOME
                        )
                    }
    val totalAssets =
            metrics.find { it.category == MetricCategory.TOTAL_ASSETS }
                    ?: analysis.metrics.find { it.name.contains("Total Assets (AI)") }?.let {
                        ExtendedFinancialMetric(
                                name = it.name,
                                value = it.value,
                                rawValue = it.rawValue,
                                context = it.context,
                                category = MetricCategory.TOTAL_ASSETS
                        )
                    }
    val eps =
            metrics.find {
                it.category == MetricCategory.EPS_BASIC || it.category == MetricCategory.EPS_DILUTED
            }
                    ?: analysis.metrics.find { it.name.contains("EPS (AI)") }?.let {
                        ExtendedFinancialMetric(
                                name = it.name,
                                value = it.value,
                                rawValue = it.rawValue,
                                context = it.context,
                                category = MetricCategory.EPS_DILUTED
                        )
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
                        text = "주요 재무 요약",
                        style = AppTypography.Subtitle1,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.OnSurface
                )

                analysis.reportType?.let { type ->
                    Box(
                            modifier =
                                    Modifier.background(
                                                    AppColors.Primary.copy(alpha = 0.1f),
                                                    AppShapes.Pill
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
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
                Text(text = name, style = AppTypography.Body2, color = AppColors.OnSurfaceSecondary)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = AppColors.Divider)
            Spacer(modifier = Modifier.height(16.dp))

            // Key metrics grid
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                        label = "Revenue",
                        value = revenue?.value ?: "N/A",
                        icon = Icons.Outlined.AttachMoney,
                        color = AppColors.Revenue
                )

                MetricItem(
                        label = "Net Income",
                        value = netIncome?.value ?: "N/A",
                        icon = Icons.AutoMirrored.Outlined.TrendingUp,
                        color = AppColors.Income
                )

                MetricItem(
                        label = "Total Assets",
                        value = totalAssets?.value ?: "N/A",
                        icon = Icons.Outlined.AccountBalance,
                        color = AppColors.Primary
                )

                MetricItem(
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
private fun MetricItem(label: String, value: String, icon: ImageVector, color: Color) {
    val isAi = label.contains("(AI)")
    val cleanLabel = label.replace("(AI)", "").trim()
    val displayColor = if (isAi) Color(0xFF9C27B0) else color // Purple for AI metrics

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = displayColor,
                    modifier = Modifier.size(24.dp)
            )
            if (isAi) {
                Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Recovered",
                        tint = Color(0xFF9C27B0),
                        modifier = Modifier.size(10.dp).offset(x = 4.dp, y = (-4).dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = cleanLabel, style = AppTypography.Caption, color = AppColors.OnSurfaceSecondary)
        Text(
                text = value,
                style = AppTypography.Body2,
                fontWeight = FontWeight.Bold,
                color = displayColor
        )
    }
}

@Composable
private fun AiAnalysisContent(analysis: FinancialAnalysis) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // AI Header with Model Info
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                            text = "AI 심층 분석",
                            style = AppTypography.Headline2,
                            color = AppColors.OnSurface,
                            fontWeight = FontWeight.Bold
                    )
                    Text(
                            text = "딥러닝 모델을 활용한 자동 분석 결과입니다",
                            style = AppTypography.Caption,
                            color = AppColors.OnSurfaceSecondary
                    )
                }
            }

            // GPU Status Badge
            Surface(
                    color =
                            if (analysis.aiModelUsed?.contains("GPU") == true) Color(0xFFE8F5E9)
                            else AppColors.SurfaceVariant,
                    shape = AppShapes.Pill
            ) {
                Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                            imageVector =
                                    if (analysis.aiModelUsed?.contains("GPU") == true)
                                            Icons.Default.Speed
                                    else Icons.Default.Computer,
                            contentDescription = null,
                            tint =
                                    if (analysis.aiModelUsed?.contains("GPU") == true)
                                            Color(0xFF2E7D32)
                                    else AppColors.OnSurfaceSecondary,
                            modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                            text = analysis.aiModelUsed ?: "AI Ready",
                            style = AppTypography.Caption,
                            color =
                                    if (analysis.aiModelUsed?.contains("GPU") == true)
                                            Color(0xFF2E7D32)
                                    else AppColors.OnSurfaceSecondary,
                            fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sentiment Analysis Section
        analysis.aiSentiment?.let { sentiment ->
            Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = AppDimens.CardElevation,
                    shape = AppShapes.Large,
                    backgroundColor = AppColors.Surface
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val icon =
                                when (sentiment.overallSentiment.lowercase()) {
                                    "positive" -> Icons.Default.SentimentVerySatisfied
                                    "negative" -> Icons.Default.SentimentVeryDissatisfied
                                    else -> Icons.Default.SentimentNeutral
                                }
                        val color =
                                when (sentiment.overallSentiment.lowercase()) {
                                    "positive" -> Color(0xFF4CAF50)
                                    "negative" -> Color(0xFFF44336)
                                    else -> Color(0xFF2196F3)
                                }

                        Icon(
                                icon,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        val translatedSentiment =
                                when (sentiment.overallSentiment.uppercase()) {
                                    "POSITIVE" -> "긍정적"
                                    "NEGATIVE" -> "부정적"
                                    "NEUTRAL" -> "중립"
                                    "MIXED" -> "혼조세"
                                    else -> sentiment.overallSentiment
                                }
                        Text(
                                text = "시장 감성 분석: $translatedSentiment",
                                style = AppTypography.Headline3,
                                color = color,
                                fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sentiment Meter (Simplified)
                    Row(modifier = Modifier.fillMaxWidth().height(12.dp).clip(AppShapes.Pill)) {
                        val posWeight = sentiment.positiveRatio.toFloatOrNull() ?: 0f
                        val neuWeight = sentiment.neutralRatio.toFloatOrNull() ?: 0f
                        val negWeight = sentiment.negativeRatio.toFloatOrNull() ?: 0f

                        Box(
                                modifier =
                                        Modifier.weight(posWeight.coerceAtLeast(0.01f))
                                                .fillMaxHeight()
                                                .background(Color(0xFF4CAF50))
                        )
                        Box(
                                modifier =
                                        Modifier.weight(neuWeight.coerceAtLeast(0.01f))
                                                .fillMaxHeight()
                                                .background(Color(0xFF2196F3))
                        )
                        Box(
                                modifier =
                                        Modifier.weight(negWeight.coerceAtLeast(0.01f))
                                                .fillMaxHeight()
                                                .background(Color(0xFFF44336))
                        )
                    }

                    Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("긍정", style = AppTypography.Caption, color = Color(0xFF4CAF50))
                        Text("중립", style = AppTypography.Caption, color = Color(0xFF2196F3))
                        Text("부정", style = AppTypography.Caption, color = Color(0xFFF44336))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Summary Section
        analysis.aiDocumentSummary?.let { summary ->
            Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = AppDimens.CardElevation,
                    shape = AppShapes.Large
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    SectionHeader(title = "AI 핵심 요약", icon = Icons.Default.Insights)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                            text = translateAiText(summary.executiveSummary),
                            style = AppTypography.Body1,
                            color = AppColors.OnSurface,
                            lineHeight = 24.sp
                    )

                    if (summary.keyFindings.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                                text = "주요 포인트",
                                style = AppTypography.Subtitle1,
                                fontWeight = FontWeight.Bold
                        )
                        summary.keyFindings.forEach { point ->
                            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text("• ", color = AppColors.Primary)
                                Text(text = translateAiText(point), style = AppTypography.Body2)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Risk Analysis section
        if (analysis.aiRiskAnalysis.isNotEmpty()) {
            Text(
                    text = "AI 리스크 평가",
                    style = AppTypography.Headline3,
                    modifier = Modifier.padding(vertical = 12.dp)
            )

            analysis.aiRiskAnalysis.chunked(2).forEach { rowRisks ->
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowRisks.forEach { risk ->
                        Card(
                                modifier = Modifier.weight(1f),
                                shape = AppShapes.Medium,
                                backgroundColor = AppColors.SurfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val severityColor =
                                            when (risk.severity.toString().lowercase()) {
                                                "critical" -> Color(0xFFD32F2F)
                                                "high" -> Color(0xFFF44336)
                                                "medium" -> Color(0xFFFF9800)
                                                else -> Color(0xFF4CAF50)
                                            }
                                    val severityText =
                                            when (risk.severity.toString().uppercase()) {
                                                "CRITICAL" -> "위험"
                                                "HIGH" -> "높음"
                                                "MEDIUM" -> "보통"
                                                "LOW" -> "낮음"
                                                else -> "정보"
                                            }
                                    Box(
                                            modifier =
                                                    Modifier.size(8.dp)
                                                            .background(severityColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                            text = translateRiskCategory(risk.category),
                                            style = AppTypography.Caption,
                                            fontWeight = FontWeight.Bold,
                                            color = severityColor
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                            text = severityText,
                                            style = AppTypography.Caption,
                                            color = severityColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                        text = translateAiText(risk.riskFactor),
                                        style = AppTypography.Subtitle2,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val sentimentKor =
                                        when (risk.sentiment.sentiment.uppercase()) {
                                            "POSITIVE" -> "긍정"
                                            "NEGATIVE" -> "부정"
                                            else -> "중립"
                                        }
                                Text(
                                        text = "감성: $sentimentKor",
                                        style = AppTypography.Caption,
                                        color = AppColors.OnSurfaceSecondary
                                )
                            }
                        }
                    }
                    if (rowRisks.size < 2) Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Processing Time Footer
        analysis.aiProcessingTimeMs?.let { time ->
            Text(
                    text = "AI 분석 완료: ${time}ms",
                    style = AppTypography.Caption,
                    color = AppColors.OnSurfaceSecondary,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    textAlign = TextAlign.Center
            )
        }
    }
}

/** AI 추출 텍스트를 한글로 변환하는 간단한 헬퍼 함수 (실제 번역 모델을 사용하기 전까지 주요 패턴 기반으로 변환) */
private fun translateAiText(text: String?): String {
    if (text == null) return ""

    // 주요 문구 및 패턴 변환
    var result =
            text.replace("The company", "당사는")
                    .replace("filed this report", "보고서를 제출했습니다")
                    .replace("management outlook", "경영진 전망")
                    .replace("appears positive", "긍정적으로 보입니다")
                    .replace("Key financial figures include", "주요 재무 지표는 다음과 같습니다:")
                    .replace("monetary values disclosed", "개의 금액 지표가 공개되었습니다")
                    .replace("percentage figures mentioned", "개의 퍼센트 지표가 언급되었습니다")
                    .replace("Revenue growth mentioned", "매출 성장 언급")
                    .replace("Cost reduction initiatives discussed", "비용 절감 대책 논의")
                    .replace("M&A activity mentioned", "인수합병(M&A) 활동 언급")
                    .replace("Dividend-related information included", "배당 관련 정보 포함")
                    .replace("Stock repurchase program discussed", "자사주 매입 프로그램 논의")
                    .replace(
                            "Overall positive sentiment may indicate favorable conditions",
                            "전반적인 긍정적 기조는 유리한 시장 여건을 나타낼 수 있습니다"
                    )
                    .replace(
                            "Negative sentiment warrants careful due diligence",
                            "부정적 기조가 감지되어 신중한 실사가 필요합니다"
                    )
                    .replace(
                            "Mixed signals - detailed analysis recommended",
                            "혼조세 신호 - 상세 분석을 권장합니다"
                    )
                    .replace(
                            "Multiple risk factors disclosed - assess risk tolerance",
                            "다수의 리스크 요인 노출 - 위험 감수 성향 평가 필요"
                    )
                    .replace(
                            "Review disclosed financial figures against expectations",
                            "공개된 재무 수치를 기대치와 비교 검토하십시오"
                    )
                    .replace(
                            "Conduct thorough analysis before making investment decisions",
                            "투자 결정 전 철저한 분석을 수행하십시오"
                    )
                    .replace("Market volatility", "시장 변동성")
                    .replace("Supply chain disruption", "공급망 중단")
                    .replace("Cybersecurity threats", "사이버 보안 위협")
                    .replace("Regulatory changes", "규제 변화")
                    .replace("Competition pressure", "경쟁 압력")
                    .replace("Currency fluctuation", "환율 변동")
                    .replace("Interest rate risk", "금리 리스크")
                    .replace("Litigation risk", "소송 리스크")
                    .replace("Environmental compliance", "환경 규제 준수")

    return result
}

/** 리스크 카테고리 한글 변환 */
private fun translateRiskCategory(category: String): String {
    return when (category.uppercase()) {
        "MARKET" -> "시장 리스크"
        "REGULATORY" -> "규제/법률 리스크"
        "OPERATIONAL" -> "운영 리스크"
        "FINANCIAL" -> "재무 리스크"
        "TECHNOLOGY" -> "기술/보안 리스크"
        "ENVIRONMENTAL" -> "환경 리스크"
        "GEOPOLITICAL" -> "지정학 리스크"
        "GENERAL" -> "일반 리스크"
        else -> category
    }
}
