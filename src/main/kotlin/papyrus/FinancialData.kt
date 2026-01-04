package papyrus

import kotlinx.serialization.Serializable

@Serializable
data class FinancialMetric(
        val name: String,
        val value: String,
        val rawValue: Double? = null,
        val context: String = ""
)

/**
 * 재무 비율 - 기업의 재무 건전성을 평가하는 핵심 지표
 */
@Serializable
data class FinancialRatio(
        val name: String,
        val value: Double,
        val formattedValue: String,
        val description: String,
        val interpretation: String,
        val healthStatus: HealthStatus,
        val category: RatioCategory
)

enum class HealthStatus {
    EXCELLENT, GOOD, NEUTRAL, CAUTION, WARNING
}

enum class RatioCategory {
    PROFITABILITY,  // 수익성
    LIQUIDITY,      // 유동성  
    SOLVENCY,       // 지급능력
    EFFICIENCY,     // 효율성
    VALUATION       // 가치평가
}

/**
 * 초보자를 위한 인사이트 - SEC 보고서를 쉽게 이해할 수 있도록 설명
 */
@Serializable
data class BeginnerInsight(
        val title: String,
        val emoji: String,
        val summary: String,
        val detailedExplanation: String,
        val whatItMeans: String,
        val whyItMatters: String,
        val actionableAdvice: String,
        val relatedTerms: List<FinancialTermExplanation> = emptyList()
)

/**
 * 재무 용어 해설 - 초보자도 이해할 수 있는 설명
 */
@Serializable
data class FinancialTermExplanation(
        val term: String,
        val simpleDefinition: String,
        val analogy: String,  // 실생활 비유
        val example: String
)

/**
 * 전체 재무 건전성 스코어
 */
@Serializable
data class FinancialHealthScore(
        val overallScore: Int,  // 0-100
        val grade: String,      // A+, A, B+, B, C, D, F
        val summary: String,
        val strengths: List<String>,
        val weaknesses: List<String>,
        val recommendations: List<String>
)

/**
 * 확장된 재무 분석 결과
 */
data class FinancialAnalysis(
        val fileName: String,
        val companyName: String?,
        val reportType: String?,
        val periodEnding: String?,
        val metrics: List<FinancialMetric>,
        val rawContent: String,
        val summary: String,
        // 새로운 초보자 친화적 필드
        val ratios: List<FinancialRatio> = emptyList(),
        val beginnerInsights: List<BeginnerInsight> = emptyList(),
        val termExplanations: List<FinancialTermExplanation> = emptyList(),
        val healthScore: FinancialHealthScore? = null,
        val reportTypeExplanation: String? = null,
        val keyTakeaways: List<String> = emptyList()
)

object FinancialAnalyzer {
    // Key financial terms to search for
    private val revenueTerms =
            listOf(
                    "Total Revenue",
                    "Total Revenues",
                    "Net Revenue",
                    "Net Revenues",
                    "Revenue",
                    "Revenues",
                    "Sales",
                    "Net Sales"
            )

    private val incomeTerms =
            listOf(
                    "Net Income",
                    "Net Earnings",
                    "Net Loss",
                    "Net Income (Loss)",
                    "Profit",
                    "Net Profit"
            )

    private val assetsTerms = listOf("Total Assets", "Total Current Assets")

    private val liabilitiesTerms = listOf("Total Liabilities", "Total Current Liabilities")

    private val equityTerms =
            listOf(
                    "Total Equity",
                    "Stockholders' Equity",
                    "Shareholders' Equity",
                    "Total Stockholders' Equity"
            )

    private val epsTerms = listOf("Earnings Per Share", "EPS", "Basic EPS", "Diluted EPS")

    fun analyzeDocument(fileName: String, content: String): FinancialAnalysis {
        // Remove HTML tags and normalize whitespace
        val cleanText =
                content.replace(Regex("<[^>]*>"), " ")
                        .replace(Regex("\\s+"), " ")
                        .replace("&nbsp;", " ")
                        .trim()

        // Extract company name (usually in first few lines)
        val companyName = extractCompanyName(cleanText)

        // Detect report type (10-K, 10-Q, etc.)
        val reportType = extractReportType(cleanText)

        // Extract period
        val period = extractPeriod(cleanText)

        // Extract financial metrics
        val metrics = mutableListOf<FinancialMetric>()

        // Search for each category
        metrics.addAll(searchMetrics(cleanText, revenueTerms, "Revenue"))
        metrics.addAll(searchMetrics(cleanText, incomeTerms, "Net Income"))
        metrics.addAll(searchMetrics(cleanText, assetsTerms, "Assets"))
        metrics.addAll(searchMetrics(cleanText, liabilitiesTerms, "Liabilities"))
        metrics.addAll(searchMetrics(cleanText, equityTerms, "Equity"))
        metrics.addAll(searchMetrics(cleanText, epsTerms, "EPS"))

        // Generate summary
        val summary = generateSummary(companyName, reportType, period, metrics)

        return FinancialAnalysis(
                fileName = fileName,
                companyName = companyName,
                reportType = reportType,
                periodEnding = period,
                metrics = metrics,
                rawContent = cleanText.take(50000), // Limit size
                summary = summary
        )
    }

    private fun extractCompanyName(text: String): String? {
        // Look for common patterns
        val patterns =
                listOf(
                        Regex(
                                "(?i)(?:UNITED STATES\\s+SECURITIES AND EXCHANGE COMMISSION.*?)(\\b[A-Z][A-Za-z\\s&,.-]+(?:Inc\\.?|Corp\\.?|Corporation|Company|LLC|Ltd\\.?))"
                        ),
                        Regex(
                                "(?i)(\\b[A-Z][A-Za-z\\s&,.-]+(?:Inc\\.?|Corp\\.?|Corporation|Company))(?=\\s+Form)"
                        )
                )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1].trim()
            }
        }

        return null
    }

    private fun extractReportType(text: String): String? {
        val match = Regex("(?i)Form\\s+(10-[KQ]|8-K|20-F)").find(text)
        return match?.groupValues?.get(1)?.uppercase()
    }

    private fun extractPeriod(text: String): String? {
        // Look for period ending dates
        val patterns =
                listOf(
                        Regex(
                                "(?i)(?:For the|Period Ending|Quarter Ended|Year Ended)\\s+([A-Za-z]+\\s+\\d{1,2},?\\s+\\d{4})"
                        ),
                        Regex("(?i)Three Months Ended\\s+([A-Za-z]+\\s+\\d{1,2},?\\s+\\d{4})"),
                        Regex("(?i)Twelve Months Ended\\s+([A-Za-z]+\\s+\\d{1,2},?\\s+\\d{4})")
                )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1].trim()
            }
        }

        return null
    }

    private fun searchMetrics(
            text: String,
            terms: List<String>,
            @Suppress("UNUSED_PARAMETER") category: String
    ): List<FinancialMetric> {
        val results = mutableListOf<FinancialMetric>()

        for (term in terms) {
            // Look for pattern: Term + amount
            // Handles formats like: "Total Revenue $123,456", "Revenue: 123456", etc.
            val pattern = Regex("(?i)${Regex.escape(term)}[:\\s]*(\\$?\\s*[\\d,]+(?:\\.\\d+)?)")
            val matches = pattern.findAll(text)

            for (match in matches.take(3)) { // Take first 3 matches
                if (match.groupValues.size > 1) {
                    val valueStr = match.groupValues[1].trim()
                    val context =
                            text.substring(
                                    maxOf(0, match.range.first - 100),
                                    minOf(text.length, match.range.last + 100)
                            )

                    // Parse numeric value
                    val rawValue = parseAmount(valueStr)

                    results.add(
                            FinancialMetric(
                                    name = term,
                                    value = valueStr,
                                    rawValue = rawValue,
                                    context = context
                            )
                    )
                }
            }
        }

        return results
    }

    private fun parseAmount(amountStr: String): Double? {
        return try {
            val cleaned = amountStr.replace("$", "").replace(",", "").replace(" ", "").trim()
            cleaned.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    private fun generateSummary(
            companyName: String?,
            reportType: String?,
            period: String?,
            metrics: List<FinancialMetric>
    ): String {
        val sb = StringBuilder()

        // Header
        sb.appendLine("📊 Financial Analysis Summary")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine()

        if (companyName != null) {
            sb.appendLine("🏢 Company: $companyName")
        }
        if (reportType != null) {
            sb.appendLine("📋 Report Type: $reportType")
        }
        if (period != null) {
            sb.appendLine("📅 Period: $period")
        }
        sb.appendLine()

        // Group metrics by category
        val grouped =
                metrics.groupBy { metric ->
                    when {
                        metric.name.contains("Revenue", ignoreCase = true) ||
                                metric.name.contains("Sales", ignoreCase = true) -> "💰 Revenue"
                        metric.name.contains("Income", ignoreCase = true) ||
                                metric.name.contains("Profit", ignoreCase = true) ||
                                metric.name.contains("Earnings", ignoreCase = true) ->
                                "💵 Income/Earnings"
                        metric.name.contains("Assets", ignoreCase = true) -> "🏦 Assets"
                        metric.name.contains("Liabilities", ignoreCase = true) -> "📊 Liabilities"
                        metric.name.contains("Equity", ignoreCase = true) -> "💎 Equity"
                        metric.name.contains("EPS", ignoreCase = true) -> "📈 Per Share Metrics"
                        else -> "📌 Other Metrics"
                    }
                }

        for ((category, metricsList) in grouped) {
            sb.appendLine(category)
            for (metric in metricsList.take(5)) { // Limit to 5 per category
                val formattedValue =
                        if (metric.rawValue != null) {
                            formatNumber(metric.rawValue)
                        } else {
                            metric.value
                        }
                sb.appendLine("  • ${metric.name}: $formattedValue")
            }
            sb.appendLine()
        }

        if (metrics.isEmpty()) {
            sb.appendLine("⚠️  No financial metrics were automatically detected.")
            sb.appendLine("   The document may be in an unsupported format or")
            sb.appendLine("   may not contain standard financial statements.")
        }

        return sb.toString()
    }

    private fun formatNumber(value: Double): String {
        return when {
            value >= 1_000_000_000 -> String.format("$%.2fB", value / 1_000_000_000)
            value >= 1_000_000 -> String.format("$%.2fM", value / 1_000_000)
            value >= 1_000 -> String.format("$%.2fK", value / 1_000)
            else -> String.format("$%.2f", value)
        }
    }

    // ==========================================
    // 초보자 친화적 분석 기능
    // ==========================================

    /**
     * 초보자를 위한 심화 분석
     */
    fun analyzeForBeginners(fileName: String, content: String): FinancialAnalysis {
        val basicAnalysis = analyzeDocument(fileName, content)
        
        // 재무 비율 계산
        val ratios = calculateFinancialRatios(basicAnalysis.metrics)
        
        // 초보자 인사이트 생성
        val insights = generateBeginnerInsights(basicAnalysis, ratios)
        
        // 용어 설명 생성
        val termExplanations = generateTermExplanations(basicAnalysis.metrics)
        
        // 재무 건전성 점수 계산
        val healthScore = calculateHealthScore(basicAnalysis.metrics, ratios)
        
        // 보고서 유형 설명
        val reportExplanation = getReportTypeExplanation(basicAnalysis.reportType)
        
        // 핵심 요점 생성
        val keyTakeaways = generateKeyTakeaways(basicAnalysis, ratios, healthScore)
        
        return basicAnalysis.copy(
            ratios = ratios,
            beginnerInsights = insights,
            termExplanations = termExplanations,
            healthScore = healthScore,
            reportTypeExplanation = reportExplanation,
            keyTakeaways = keyTakeaways
        )
    }

    /**
     * 재무 비율 계산
     */
    private fun calculateFinancialRatios(metrics: List<FinancialMetric>): List<FinancialRatio> {
        val ratios = mutableListOf<FinancialRatio>()
        
        // 메트릭에서 값 추출
        val revenue = findMetricValue(metrics, listOf("Revenue", "Sales", "Net Revenue", "Total Revenue"))
        val netIncome = findMetricValue(metrics, listOf("Net Income", "Net Earnings", "Profit"))
        val totalAssets = findMetricValue(metrics, listOf("Total Assets"))
        val totalLiabilities = findMetricValue(metrics, listOf("Total Liabilities"))
        val totalEquity = findMetricValue(metrics, listOf("Total Equity", "Stockholders' Equity"))
        val currentAssets = findMetricValue(metrics, listOf("Total Current Assets", "Current Assets"))
        val currentLiabilities = findMetricValue(metrics, listOf("Total Current Liabilities", "Current Liabilities"))
        
        // 수익성 비율: 순이익률 (Net Profit Margin)
        if (revenue != null && netIncome != null && revenue > 0) {
            val margin = (netIncome / revenue) * 100
            val status = when {
                margin >= 20 -> HealthStatus.EXCELLENT
                margin >= 10 -> HealthStatus.GOOD
                margin >= 5 -> HealthStatus.NEUTRAL
                margin >= 0 -> HealthStatus.CAUTION
                else -> HealthStatus.WARNING
            }
            ratios.add(FinancialRatio(
                name = "순이익률 (Net Profit Margin)",
                value = margin,
                formattedValue = String.format("%.1f%%", margin),
                description = "매출 대비 순이익의 비율",
                interpretation = getMarginInterpretation(margin),
                healthStatus = status,
                category = RatioCategory.PROFITABILITY
            ))
        }
        
        // ROA (Return on Assets)
        if (netIncome != null && totalAssets != null && totalAssets > 0) {
            val roa = (netIncome / totalAssets) * 100
            val status = when {
                roa >= 10 -> HealthStatus.EXCELLENT
                roa >= 5 -> HealthStatus.GOOD
                roa >= 2 -> HealthStatus.NEUTRAL
                roa >= 0 -> HealthStatus.CAUTION
                else -> HealthStatus.WARNING
            }
            ratios.add(FinancialRatio(
                name = "총자산이익률 (ROA)",
                value = roa,
                formattedValue = String.format("%.1f%%", roa),
                description = "보유 자산으로 얼마나 효율적으로 수익을 창출하는지",
                interpretation = getRoaInterpretation(roa),
                healthStatus = status,
                category = RatioCategory.PROFITABILITY
            ))
        }
        
        // ROE (Return on Equity)
        if (netIncome != null && totalEquity != null && totalEquity > 0) {
            val roe = (netIncome / totalEquity) * 100
            val status = when {
                roe >= 20 -> HealthStatus.EXCELLENT
                roe >= 15 -> HealthStatus.GOOD
                roe >= 10 -> HealthStatus.NEUTRAL
                roe >= 0 -> HealthStatus.CAUTION
                else -> HealthStatus.WARNING
            }
            ratios.add(FinancialRatio(
                name = "자기자본이익률 (ROE)",
                value = roe,
                formattedValue = String.format("%.1f%%", roe),
                description = "주주가 투자한 자본으로 얼마나 수익을 창출하는지",
                interpretation = getRoeInterpretation(roe),
                healthStatus = status,
                category = RatioCategory.PROFITABILITY
            ))
        }
        
        // 부채비율 (Debt to Equity Ratio)
        if (totalLiabilities != null && totalEquity != null && totalEquity > 0) {
            val debtRatio = (totalLiabilities / totalEquity) * 100
            val status = when {
                debtRatio <= 50 -> HealthStatus.EXCELLENT
                debtRatio <= 100 -> HealthStatus.GOOD
                debtRatio <= 200 -> HealthStatus.NEUTRAL
                debtRatio <= 300 -> HealthStatus.CAUTION
                else -> HealthStatus.WARNING
            }
            ratios.add(FinancialRatio(
                name = "부채비율 (Debt to Equity)",
                value = debtRatio,
                formattedValue = String.format("%.0f%%", debtRatio),
                description = "자기자본 대비 부채의 비율",
                interpretation = getDebtRatioInterpretation(debtRatio),
                healthStatus = status,
                category = RatioCategory.SOLVENCY
            ))
        }
        
        // 유동비율 (Current Ratio)
        if (currentAssets != null && currentLiabilities != null && currentLiabilities > 0) {
            val currentRatio = currentAssets / currentLiabilities
            val status = when {
                currentRatio >= 2.0 -> HealthStatus.EXCELLENT
                currentRatio >= 1.5 -> HealthStatus.GOOD
                currentRatio >= 1.0 -> HealthStatus.NEUTRAL
                currentRatio >= 0.5 -> HealthStatus.CAUTION
                else -> HealthStatus.WARNING
            }
            ratios.add(FinancialRatio(
                name = "유동비율 (Current Ratio)",
                value = currentRatio,
                formattedValue = String.format("%.2f", currentRatio),
                description = "단기 부채를 갚을 수 있는 능력",
                interpretation = getCurrentRatioInterpretation(currentRatio),
                healthStatus = status,
                category = RatioCategory.LIQUIDITY
            ))
        }
        
        return ratios
    }
    
    private fun findMetricValue(metrics: List<FinancialMetric>, terms: List<String>): Double? {
        for (term in terms) {
            val metric = metrics.find { it.name.contains(term, ignoreCase = true) }
            if (metric?.rawValue != null) return metric.rawValue
        }
        return null
    }
    
    private fun getMarginInterpretation(margin: Double): String = when {
        margin >= 20 -> "🌟 매우 우수합니다! 매출 대비 높은 수익을 창출하고 있어 경쟁력이 뛰어납니다."
        margin >= 10 -> "👍 양호합니다. 건강한 수익 구조를 유지하고 있습니다."
        margin >= 5 -> "📊 보통 수준입니다. 업계 평균과 비교해 보세요."
        margin >= 0 -> "⚠️ 주의가 필요합니다. 수익성 개선이 필요할 수 있습니다."
        else -> "🚨 적자 상태입니다. 비용 구조 개선이 시급합니다."
    }
    
    private fun getRoaInterpretation(roa: Double): String = when {
        roa >= 10 -> "🌟 매우 효율적입니다! 자산을 활용해 높은 수익을 창출하고 있습니다."
        roa >= 5 -> "👍 효율적으로 자산을 운용하고 있습니다."
        roa >= 2 -> "📊 평균적인 수준입니다."
        roa >= 0 -> "⚠️ 자산 활용 효율성이 낮습니다."
        else -> "🚨 자산 대비 손실이 발생하고 있습니다."
    }
    
    private fun getRoeInterpretation(roe: Double): String = when {
        roe >= 20 -> "🌟 투자자에게 높은 수익을 제공하고 있습니다! 우수한 경영 성과입니다."
        roe >= 15 -> "👍 양호한 투자 수익률을 보여주고 있습니다."
        roe >= 10 -> "📊 평균적인 수익률입니다."
        roe >= 0 -> "⚠️ 투자 수익률이 낮습니다. 개선이 필요할 수 있습니다."
        else -> "🚨 주주 자본에 손실이 발생하고 있습니다."
    }
    
    private fun getDebtRatioInterpretation(ratio: Double): String = when {
        ratio <= 50 -> "🌟 매우 안정적입니다! 부채 부담이 적어 재무 위험이 낮습니다."
        ratio <= 100 -> "👍 건전한 부채 수준입니다."
        ratio <= 200 -> "📊 보통 수준의 부채입니다. 업계 특성을 고려하세요."
        ratio <= 300 -> "⚠️ 부채가 다소 높습니다. 금리 상승 시 주의가 필요합니다."
        else -> "🚨 부채가 과다합니다. 재무 위험이 높을 수 있습니다."
    }
    
    private fun getCurrentRatioInterpretation(ratio: Double): String = when {
        ratio >= 2.0 -> "🌟 매우 안정적입니다! 단기 부채를 충분히 갚을 수 있습니다."
        ratio >= 1.5 -> "👍 양호합니다. 단기 유동성에 문제가 없습니다."
        ratio >= 1.0 -> "📊 최소 기준은 충족합니다. 현금 흐름 관리가 중요합니다."
        ratio >= 0.5 -> "⚠️ 주의가 필요합니다. 단기 부채 상환에 어려움이 있을 수 있습니다."
        else -> "🚨 심각한 유동성 위험이 있습니다."
    }

    /**
     * 초보자 인사이트 생성
     */
    private fun generateBeginnerInsights(
        analysis: FinancialAnalysis,
        ratios: List<FinancialRatio>
    ): List<BeginnerInsight> {
        val insights = mutableListOf<BeginnerInsight>()
        
        // 회사 규모 인사이트
        val revenue = findMetricValue(analysis.metrics, listOf("Revenue", "Sales", "Total Revenue"))
        if (revenue != null) {
            val sizeCategory = when {
                revenue >= 10_000_000_000 -> "대기업" to "삼성, 애플과 같은 글로벌 대기업 수준"
                revenue >= 1_000_000_000 -> "중대형 기업" to "국내 유명 기업들과 비슷한 규모"
                revenue >= 100_000_000 -> "중형 기업" to "안정적인 성장 단계의 기업"
                revenue >= 10_000_000 -> "중소기업" to "성장 가능성이 있는 기업"
                else -> "소규모 기업" to "초기 단계 또는 소규모 기업"
            }
            insights.add(BeginnerInsight(
                title = "회사 규모",
                emoji = "🏢",
                summary = "${sizeCategory.first} (매출 ${formatNumber(revenue)})",
                detailedExplanation = "이 회사의 매출 규모는 ${formatNumber(revenue)}입니다. ${sizeCategory.second}입니다.",
                whatItMeans = "매출은 회사가 제품이나 서비스를 팔아서 벌어들인 총 금액입니다. 마치 가게의 '총 판매액'과 같습니다.",
                whyItMatters = "매출 규모가 크다고 무조건 좋은 것은 아니지만, 일반적으로 큰 회사는 더 안정적인 경향이 있습니다.",
                actionableAdvice = "같은 산업의 다른 회사들과 비교해 보세요. 업계에서의 위치를 파악하는 것이 중요합니다."
            ))
        }
        
        // 수익성 인사이트
        val profitMargin = ratios.find { it.name.contains("순이익률") }
        if (profitMargin != null) {
            val profitStatus = when (profitMargin.healthStatus) {
                HealthStatus.EXCELLENT -> "매우 우수한 수익성"
                HealthStatus.GOOD -> "양호한 수익성"
                HealthStatus.NEUTRAL -> "보통 수준의 수익성"
                HealthStatus.CAUTION -> "개선이 필요한 수익성"
                HealthStatus.WARNING -> "심각한 수익성 문제"
            }
            insights.add(BeginnerInsight(
                title = "돈을 잘 벌고 있나요?",
                emoji = "💰",
                summary = "$profitStatus (${profitMargin.formattedValue})",
                detailedExplanation = profitMargin.interpretation,
                whatItMeans = """
                    순이익률은 '100원 팔면 실제로 얼마가 남는가'를 보여줍니다.
                    예를 들어 순이익률이 10%라면, 100원 팔면 10원이 순수익으로 남는다는 뜻입니다.
                """.trimIndent(),
                whyItMatters = "순이익률이 높을수록 회사가 돈을 효율적으로 벌고 있다는 의미입니다. 투자자에게 배당을 주거나 회사 성장에 재투자할 여력이 있습니다.",
                actionableAdvice = when (profitMargin.healthStatus) {
                    HealthStatus.EXCELLENT, HealthStatus.GOOD -> "수익성이 좋습니다! 이 수익이 지속 가능한지 확인해 보세요."
                    HealthStatus.NEUTRAL -> "업계 평균과 비교해 보세요. 경쟁사보다 낮다면 원인을 파악해야 합니다."
                    else -> "수익성 개선이 필요합니다. 비용 절감이나 가격 인상 가능성을 살펴보세요."
                }
            ))
        }
        
        // 재무 안정성 인사이트
        val debtRatio = ratios.find { it.name.contains("부채비율") }
        if (debtRatio != null) {
            val stabilityStatus = when (debtRatio.healthStatus) {
                HealthStatus.EXCELLENT -> "매우 안정적"
                HealthStatus.GOOD -> "안정적"
                HealthStatus.NEUTRAL -> "보통"
                HealthStatus.CAUTION -> "주의 필요"
                HealthStatus.WARNING -> "위험 신호"
            }
            insights.add(BeginnerInsight(
                title = "빚은 얼마나 있나요?",
                emoji = "⚖️",
                summary = "$stabilityStatus (부채비율 ${debtRatio.formattedValue})",
                detailedExplanation = debtRatio.interpretation,
                whatItMeans = """
                    부채비율은 '내 돈(자기자본) 대비 빌린 돈(부채)의 비율'입니다.
                    100%라면 자기 돈만큼 빚이 있다는 뜻이고, 200%라면 자기 돈의 2배만큼 빚이 있다는 뜻입니다.
                """.trimIndent(),
                whyItMatters = "부채가 많으면 이자 비용이 높고, 경기가 나빠질 때 위험할 수 있습니다. 하지만 적절한 부채는 성장을 위해 필요할 수도 있습니다.",
                actionableAdvice = when (debtRatio.healthStatus) {
                    HealthStatus.EXCELLENT, HealthStatus.GOOD -> "재무가 안정적입니다. 경기 침체에도 버틸 여력이 있습니다."
                    HealthStatus.NEUTRAL -> "업계 특성을 고려하세요. 금융, 건설업은 일반적으로 부채가 높습니다."
                    else -> "부채 수준이 높습니다. 금리 인상 시 이자 부담이 커질 수 있어 주의가 필요합니다."
                }
            ))
        }
        
        // 보고서 유형 인사이트
        if (analysis.reportType != null) {
            insights.add(BeginnerInsight(
                title = "이 보고서는 무엇인가요?",
                emoji = "📋",
                summary = "SEC ${analysis.reportType} 보고서",
                detailedExplanation = getReportTypeExplanation(analysis.reportType) ?: "",
                whatItMeans = when (analysis.reportType) {
                    "10-K" -> "연간 보고서(10-K)는 회사의 1년간 성과를 담은 '성적표'입니다. 가장 포괄적인 재무 정보를 담고 있습니다."
                    "10-Q" -> "분기 보고서(10-Q)는 3개월간의 성과를 보여줍니다. 연간 보고서보다 간략하지만 최신 상황을 파악할 수 있습니다."
                    "8-K" -> "수시 보고서(8-K)는 중요한 사건 발생 시 제출됩니다. 인수합병, CEO 교체 등 큰 뉴스가 있을 때 나옵니다."
                    else -> "SEC에 제출되는 공식 재무 보고서입니다."
                },
                whyItMatters = "SEC 보고서는 법적으로 정확해야 하므로 회사 홍보 자료보다 신뢰할 수 있습니다. 투자 결정의 핵심 자료입니다.",
                actionableAdvice = "처음이라면 10-K(연간보고서)부터 읽어보세요. 'Business', 'Risk Factors', 'MD&A' 섹션이 가장 중요합니다."
            ))
        }
        
        return insights
    }

    /**
     * 용어 설명 생성
     */
    private fun generateTermExplanations(metrics: List<FinancialMetric>): List<FinancialTermExplanation> {
        val terms = mutableListOf<FinancialTermExplanation>()
        
        // 기본 용어들
        terms.add(FinancialTermExplanation(
            term = "매출 (Revenue)",
            simpleDefinition = "회사가 제품이나 서비스를 팔아서 받은 총 금액",
            analogy = "카페를 운영한다면, 커피를 팔아서 받은 총 금액이 매출입니다. 재료비, 인건비를 빼기 전 금액이에요.",
            example = "애플이 아이폰을 1억 대 팔아서 1000억 달러를 벌었다면, 그게 애플의 매출입니다."
        ))
        
        terms.add(FinancialTermExplanation(
            term = "순이익 (Net Income)",
            simpleDefinition = "모든 비용을 제외하고 실제로 남은 돈",
            analogy = "월급 300만원을 받고, 집세·식비·교통비 등을 다 내고 통장에 남은 50만원이 '순이익'입니다.",
            example = "매출이 100억이어도 비용이 95억이면 순이익은 5억뿐입니다. 매출보다 순이익이 중요해요!"
        ))
        
        terms.add(FinancialTermExplanation(
            term = "자산 (Assets)",
            simpleDefinition = "회사가 소유한 모든 가치 있는 것들",
            analogy = "개인으로 치면 집, 차, 저금통장, 주식 등 내가 가진 모든 재산입니다.",
            example = "삼성전자의 자산에는 공장, 특허권, 현금, 재고 상품 등이 포함됩니다."
        ))
        
        terms.add(FinancialTermExplanation(
            term = "부채 (Liabilities)",
            simpleDefinition = "회사가 갚아야 할 모든 빚",
            analogy = "주택담보대출, 카드 할부금, 친구에게 빌린 돈 등 언젠가 갚아야 할 돈입니다.",
            example = "은행 대출 50억, 미지급 세금 5억, 공급업체에 줘야 할 돈 10억 = 총 부채 65억"
        ))
        
        terms.add(FinancialTermExplanation(
            term = "자기자본 (Equity)",
            simpleDefinition = "자산에서 부채를 뺀 순수한 회사의 가치 (주주의 몫)",
            analogy = "5억짜리 집을 사고 대출이 3억이라면, 자기자본은 2억입니다. 이게 진짜 내 재산이에요.",
            example = "총자산 100억 - 총부채 60억 = 자기자본 40억 (이게 주주들의 몫)"
        ))
        
        terms.add(FinancialTermExplanation(
            term = "EPS (주당순이익)",
            simpleDefinition = "주식 1주당 벌어들인 순이익",
            analogy = "피자를 8조각으로 나눴을 때 한 조각의 크기와 같아요. 조각(주식)당 얼마나 맛있는지(수익)를 보여줍니다.",
            example = "순이익 100억원 ÷ 발행주식 1억주 = EPS 100원. 내가 1주를 가지면 100원의 이익에 해당하는 권리가 있어요."
        ))
        
        terms.add(FinancialTermExplanation(
            term = "SEC (미국 증권거래위원회)",
            simpleDefinition = "미국 주식시장을 감독하는 정부 기관",
            analogy = "학교의 교무처장 같은 존재입니다. 회사들이 정직하게 정보를 공개하는지 감시합니다.",
            example = "모든 미국 상장기업은 SEC에 재무보고서를 의무적으로 제출해야 합니다. 거짓 정보를 내면 큰 벌을 받아요!"
        ))
        
        return terms
    }

    /**
     * 재무 건전성 점수 계산
     */
    private fun calculateHealthScore(
        metrics: List<FinancialMetric>,
        ratios: List<FinancialRatio>
    ): FinancialHealthScore {
        var totalScore = 0
        var count = 0
        val strengths = mutableListOf<String>()
        val weaknesses = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        
        for (ratio in ratios) {
            val score = when (ratio.healthStatus) {
                HealthStatus.EXCELLENT -> 100
                HealthStatus.GOOD -> 80
                HealthStatus.NEUTRAL -> 60
                HealthStatus.CAUTION -> 40
                HealthStatus.WARNING -> 20
            }
            totalScore += score
            count++
            
            when (ratio.healthStatus) {
                HealthStatus.EXCELLENT, HealthStatus.GOOD -> 
                    strengths.add("✅ ${ratio.name}: ${ratio.formattedValue}")
                HealthStatus.CAUTION, HealthStatus.WARNING ->
                    weaknesses.add("⚠️ ${ratio.name}: ${ratio.formattedValue}")
                else -> {}
            }
        }
        
        val overallScore = if (count > 0) totalScore / count else 50
        val grade = when {
            overallScore >= 90 -> "A+"
            overallScore >= 85 -> "A"
            overallScore >= 80 -> "B+"
            overallScore >= 75 -> "B"
            overallScore >= 70 -> "C+"
            overallScore >= 60 -> "C"
            overallScore >= 50 -> "D"
            else -> "F"
        }
        
        val summary = when {
            overallScore >= 80 -> "전반적으로 재무 상태가 양호합니다. 안정적인 투자 대상으로 고려할 수 있습니다."
            overallScore >= 60 -> "평균적인 재무 상태입니다. 몇 가지 개선이 필요한 부분이 있습니다."
            overallScore >= 40 -> "주의가 필요한 재무 상태입니다. 투자 전 심층 분석을 권장합니다."
            else -> "재무 상태에 심각한 문제가 있을 수 있습니다. 신중한 판단이 필요합니다."
        }
        
        // 권장사항 생성
        if (weaknesses.any { it.contains("부채") }) {
            recommendations.add("💡 부채 수준을 주시하세요. 금리 인상 시 이자 부담이 커질 수 있습니다.")
        }
        if (weaknesses.any { it.contains("순이익") || it.contains("수익") }) {
            recommendations.add("💡 수익성 개선 노력이 필요합니다. 비용 구조를 확인해 보세요.")
        }
        if (strengths.isEmpty() && weaknesses.isEmpty()) {
            recommendations.add("💡 더 많은 재무 정보가 필요합니다. 전체 재무제표를 확인해 보세요.")
        }
        if (overallScore >= 70) {
            recommendations.add("💡 경쟁사와 비교 분석을 해보면 더 명확한 판단이 가능합니다.")
        }
        
        return FinancialHealthScore(
            overallScore = overallScore,
            grade = grade,
            summary = summary,
            strengths = strengths.take(5),
            weaknesses = weaknesses.take(5),
            recommendations = recommendations.take(3)
        )
    }

    /**
     * 보고서 유형 설명
     */
    private fun getReportTypeExplanation(reportType: String?): String? {
        return when (reportType) {
            "10-K" -> """
                📚 10-K 연간 보고서 (Annual Report)
                
                미국 상장기업이 매년 회계연도 종료 후 60~90일 이내에 SEC에 제출하는 가장 포괄적인 재무 보고서입니다.
                
                🔍 주요 섹션:
                • Part I - 사업 개요 (Business): 회사가 무슨 일을 하는지
                • Part I - 위험 요소 (Risk Factors): 투자 위험 요인
                • Part II - MD&A: 경영진이 설명하는 재무 상황
                • Part II - 재무제표: 숫자로 된 성적표
                
                💡 팁: 처음이라면 'Business'와 'Risk Factors'부터 읽어보세요!
            """.trimIndent()
            
            "10-Q" -> """
                📊 10-Q 분기 보고서 (Quarterly Report)
                
                매 분기(3개월)마다 제출하는 보고서입니다. 10-K보다 간략하지만 최신 상황을 파악할 수 있습니다.
                
                🔍 특징:
                • 감사받지 않은 재무제표 (검토만 받음)
                • 분기별 실적 비교 가능
                • 10-K 이후 변동사항 확인
                
                💡 팁: 전 분기, 전년 동기와 비교하면서 읽으면 트렌드를 파악할 수 있어요!
            """.trimIndent()
            
            "8-K" -> """
                ⚡ 8-K 수시 보고서 (Current Report)
                
                중요한 사건이 발생했을 때 4영업일 이내에 제출하는 긴급 보고서입니다.
                
                🔍 제출 사유 예시:
                • 인수합병 발표
                • CEO/CFO 교체
                • 중요 계약 체결 또는 해지
                • 파산 신청
                • 실적 발표 (Earnings Release)
                
                💡 팁: 8-K가 자주 나온다면 회사에 변화가 많다는 신호일 수 있어요!
            """.trimIndent()
            
            "20-F" -> """
                🌏 20-F 해외기업 연간 보고서
                
                미국에 상장된 외국 기업이 제출하는 연간 보고서입니다. 10-K와 유사합니다.
                
                🔍 특징:
                • 해당 국가의 회계 기준으로 작성될 수 있음
                • 미국 GAAP과의 차이점 설명 포함
                
                💡 팁: 알리바바, TSMC 같은 외국 기업 분석 시 확인하세요!
            """.trimIndent()
            
            else -> null
        }
    }

    /**
     * 핵심 요점 생성
     */
    private fun generateKeyTakeaways(
        analysis: FinancialAnalysis,
        ratios: List<FinancialRatio>,
        healthScore: FinancialHealthScore
    ): List<String> {
        val takeaways = mutableListOf<String>()
        
        takeaways.add("📊 재무 건전성 점수: ${healthScore.grade} (${healthScore.overallScore}점/100점)")
        
        analysis.companyName?.let {
            takeaways.add("🏢 분석 대상: $it")
        }
        
        analysis.reportType?.let {
            takeaways.add("📋 보고서 유형: SEC Form $it")
        }
        
        val excellentRatios = ratios.filter { it.healthStatus == HealthStatus.EXCELLENT }
        if (excellentRatios.isNotEmpty()) {
            takeaways.add("⭐ 강점: ${excellentRatios.first().name}이(가) 매우 우수합니다")
        }
        
        val warningRatios = ratios.filter { it.healthStatus == HealthStatus.WARNING }
        if (warningRatios.isNotEmpty()) {
            takeaways.add("🚨 주의: ${warningRatios.first().name}에 주의가 필요합니다")
        }
        
        if (takeaways.size < 4) {
            takeaways.add("💡 더 정확한 분석을 위해 여러 분기의 보고서를 비교해 보세요")
        }
        
        return takeaways.take(5)
    }
}
