package papyrus.core.service.analyzer

import org.jsoup.Jsoup
import papyrus.core.model.*
import papyrus.core.service.parser.EnhancedFinancialParser
import papyrus.core.service.parser.InlineXbrlExtractor
import papyrus.core.service.parser.SecTableParser
import papyrus.util.data.AnalysisCache
import papyrus.util.finance.*

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

        /**
         * 향상된 문서 분석 (Enhanced Document Analysis)
         *
         * 1단계: SecTableParser로 테이블 기반 정밀 파싱 2단계: EnhancedFinancialParser로 텍스트 패턴 파싱 보완 3단계: 구조화된
         * 재무제표 데이터 생성 4단계: 핵심 재무 비율 계산
         */
        fun analyzeDocument(fileName: String, content: String): FinancialAnalysis {
                val startTime = System.currentTimeMillis()

                // 메타데이터 추출
                val cleanText = EnhancedFinancialParser.cleanHtml(content)
                val companyName = extractCompanyName(cleanText)
                val reportType = extractReportType(cleanText)
                val period = extractPeriod(cleanText)

                // iXBRL/XBRL facts (when HTML content is available)
                val xbrlMetrics = extractInlineXbrlMetricsIfHtml(content)

                // 1단계: 테이블 기반 파싱 (가장 정확함)
                val tableMetrics = mutableListOf<ExtendedFinancialMetric>()
                var tablesFound = 0
                try {
                        val tables = SecTableParser.parseFinancialTables(content)
                        tablesFound = tables.size
                        tableMetrics.addAll(SecTableParser.convertToMetrics(tables))
                        println(
                                "📊 Table parsing: Found ${tableMetrics.size} metrics from $tablesFound tables"
                        )
                } catch (e: Exception) {
                        println("⚠ Table parsing error: ${e.message}")
                }

                // Inline XBRL has the most structured numeric facts; add with high confidence.
                if (xbrlMetrics.isNotEmpty()) {
                        tableMetrics.addAll(xbrlMetrics)
                        println("🧾 iXBRL parsing: Added ${xbrlMetrics.size} metrics")
                }

                // 2단계: 텍스트 패턴 파싱 (보완)
                val patternMetrics = mutableListOf<ExtendedFinancialMetric>()
                val foundCategories = tableMetrics.map { it.category }.toSet()

                // 기본 메트릭 추출
                val basicMetrics = mutableListOf<FinancialMetric>()
                basicMetrics.addAll(searchMetrics(cleanText, revenueTerms, "Revenue"))
                basicMetrics.addAll(searchMetrics(cleanText, incomeTerms, "Net Income"))
                basicMetrics.addAll(searchMetrics(cleanText, assetsTerms, "Assets"))
                basicMetrics.addAll(searchMetrics(cleanText, liabilitiesTerms, "Liabilities"))
                basicMetrics.addAll(searchMetrics(cleanText, equityTerms, "Equity"))
                basicMetrics.addAll(searchMetrics(cleanText, epsTerms, "EPS"))

                // ExtendedFinancialMetric으로 변환 (테이블에서 못 찾은 것만)
                for (metric in basicMetrics) {
                        val category = inferCategory(metric.name)
                        if (category !in foundCategories && metric.rawValue != null) {
                                patternMetrics.add(
                                        ExtendedFinancialMetric(
                                                name = metric.name,
                                                value = metric.value,
                                                rawValue =
                                                        metric.rawValue?.let {
                                                                java.math.BigDecimal(it.toString())
                                                                        .toString()
                                                        },
                                                category = category,
                                                source = "Pattern matching",
                                                confidence = 0.7,
                                                context = metric.context
                                        )
                                )
                        }
                }

                // 3단계: 모든 메트릭 병합
                val allExtendedMetrics =
                        (tableMetrics + patternMetrics)
                                .groupBy { it.category }
                                .mapValues { (_, list) ->
                                        list.maxByOrNull { it.confidence } ?: list.first()
                                }
                                .values
                                .toList()

                // 기본 메트릭 리스트 생성 (호환성 유지)
                val allBasicMetrics =
                        allExtendedMetrics.map { ext ->
                                FinancialMetric(
                                        name = ext.name,
                                        value = ext.value,
                                        rawValue = ext.getRawValueBigDecimal()?.toDouble(),
                                        context = ext.context
                                )
                        } +
                                basicMetrics.filter { basic ->
                                        allExtendedMetrics.none {
                                                it.name.equals(basic.name, ignoreCase = true)
                                        }
                                }

                // 4단계: 구조화된 데이터 및 핵심 비율 생성
                val structuredData =
                        buildStructuredFinancialData(
                                companyName,
                                reportType,
                                period,
                                allExtendedMetrics
                        )

                // Calculate Ratios using RatioCalculator
                val metricMap = allExtendedMetrics.associateBy { it.category }
                val calculatedRatios = RatioCalculator.calculateRatios(metricMap)

                // 처리 시간 기록
                val processingTime = System.currentTimeMillis() - startTime
                println(
                        "⏱ Analysis completed in ${processingTime}ms (${allExtendedMetrics.size} metrics)"
                )

                // 요약 생성
                val summary =
                        generateEnhancedSummary(
                                companyName,
                                reportType,
                                period,
                                allBasicMetrics,
                                structuredData
                        )

                return FinancialAnalysis(
                        fileName = fileName,
                        companyName = companyName,
                        reportType = reportType,
                        periodEnding = period,
                        metrics = allBasicMetrics.distinctBy { it.name.lowercase() },
                        rawContent = cleanText.take(50000),
                        summary = summary,
                        ratios = calculatedRatios,
                        extendedMetrics = allExtendedMetrics,
                        xbrlMetrics = xbrlMetrics
                )
        }

        private fun extractInlineXbrlMetricsIfHtml(content: String): List<ExtendedFinancialMetric> {
                val trimmed = content.trimStart()
                val looksLikeHtml =
                        trimmed.startsWith("<") || content.contains("<html", ignoreCase = true)
                if (!looksLikeHtml) return emptyList()

                return try {
                        val doc = Jsoup.parse(content)
                        InlineXbrlExtractor.extractMetrics(doc)
                } catch (_: Exception) {
                        emptyList()
                }
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
                                Regex(
                                        "(?i)Three Months Ended\\s+([A-Za-z]+\\s+\\d{1,2},?\\s+\\d{4})"
                                ),
                                Regex(
                                        "(?i)Twelve Months Ended\\s+([A-Za-z]+\\s+\\d{1,2},?\\s+\\d{4})"
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

        private fun searchMetrics(
                text: String,
                terms: List<String>,
                @Suppress("UNUSED_PARAMETER") category: String
        ): List<FinancialMetric> {
                val results = mutableListOf<FinancialMetric>()

                for (term in terms) {
                        // Look for pattern: Term + amount
                        // Handles formats like: "Total Revenue $123,456", "Revenue: 123456",
                        // "Revenue |
                        // 123,456" etc.
                        val pattern =
                                Regex(
                                        "(?i)${Regex.escape(term)}[:\\s\\|]*(\\$?\\s*[\\d,]+(?:\\.\\d+)?)"
                                )
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
                        val cleaned =
                                amountStr.replace("$", "").replace(",", "").replace(" ", "").trim()
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
                                                metric.name.contains("Sales", ignoreCase = true) ->
                                                "💰 Revenue"
                                        metric.name.contains("Income", ignoreCase = true) ||
                                                metric.name.contains("Profit", ignoreCase = true) ||
                                                metric.name.contains(
                                                        "Earnings",
                                                        ignoreCase = true
                                                ) -> "💵 Income/Earnings"
                                        metric.name.contains("Assets", ignoreCase = true) ->
                                                "🏦 Assets"
                                        metric.name.contains("Liabilities", ignoreCase = true) ->
                                                "📊 Liabilities"
                                        metric.name.contains("Equity", ignoreCase = true) ->
                                                "💎 Equity"
                                        metric.name.contains("EPS", ignoreCase = true) ->
                                                "📈 Per Share Metrics"
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

        /** 메트릭 이름에서 카테고리 추론 */
        private fun inferCategory(name: String): MetricCategory {
                val lower = name.lowercase()
                return when {
                        lower.contains("revenue") || lower.contains("sales") ->
                                MetricCategory.REVENUE
                        lower.contains("cost of") &&
                                (lower.contains("revenue") ||
                                        lower.contains("sales") ||
                                        lower.contains("goods")) -> MetricCategory.COST_OF_REVENUE
                        lower.contains("gross profit") || lower.contains("gross margin") ->
                                MetricCategory.GROSS_PROFIT
                        lower.contains("operating income") ||
                                lower.contains("income from operations") ->
                                MetricCategory.OPERATING_INCOME
                        lower.contains("net income") ||
                                lower.contains("net earnings") ||
                                lower.contains("net loss") -> MetricCategory.NET_INCOME
                        lower.contains("ebitda") -> MetricCategory.EBITDA
                        lower.contains("total assets") -> MetricCategory.TOTAL_ASSETS
                        lower.contains("current assets") -> MetricCategory.CURRENT_ASSETS
                        lower.contains("cash and") -> MetricCategory.CASH_AND_EQUIVALENTS
                        lower.contains("total liabilities") -> MetricCategory.TOTAL_LIABILITIES
                        lower.contains("current liabilities") -> MetricCategory.CURRENT_LIABILITIES
                        lower.contains("long-term debt") || lower.contains("long term debt") ->
                                MetricCategory.LONG_TERM_DEBT
                        lower.contains("total equity") ||
                                lower.contains("stockholders") ||
                                lower.contains("shareholders") -> MetricCategory.TOTAL_EQUITY
                        lower.contains("retained earnings") -> MetricCategory.RETAINED_EARNINGS
                        lower.contains("eps") || lower.contains("earnings per share") ->
                                MetricCategory.EPS_BASIC
                        lower.contains("diluted") && lower.contains("eps") ->
                                MetricCategory.EPS_DILUTED
                        else -> MetricCategory.OTHER
                }
        }

        /** 구조화된 재무 데이터 생성 */
        private fun buildStructuredFinancialData(
                companyName: String?,
                reportType: String?,
                period: String?,
                metrics: List<ExtendedFinancialMetric>
        ): StructuredFinancialData {
                // 카테고리별로 메트릭 맵 생성
                val metricMap = metrics.associateBy { it.category }

                fun getMonetaryValue(category: MetricCategory): MonetaryValue? {
                        val metric = metricMap[category] ?: return null
                        val rawValue = metric.getRawValueBigDecimal() ?: return null
                        return MonetaryValue.fromBigDecimal(
                                rawValue,
                                yoyChange = metric.getYoyChangeBigDecimal(),
                                confidence = metric.confidence
                        )
                }

                // 손익계산서 구성
                val incomeStatement =
                        StructuredIncomeStatement(
                                periodEnding = period,
                                periodType = metrics.firstOrNull()?.periodType,
                                totalRevenue = getMonetaryValue(MetricCategory.REVENUE),
                                costOfRevenue = getMonetaryValue(MetricCategory.COST_OF_REVENUE),
                                grossProfit = getMonetaryValue(MetricCategory.GROSS_PROFIT),
                                researchAndDevelopment =
                                        getMonetaryValue(MetricCategory.RD_EXPENSE),
                                sellingGeneralAdmin = getMonetaryValue(MetricCategory.SGA_EXPENSE),
                                operatingIncome = getMonetaryValue(MetricCategory.OPERATING_INCOME),
                                interestExpense = getMonetaryValue(MetricCategory.INTEREST_EXPENSE),
                                interestIncome = getMonetaryValue(MetricCategory.INTEREST_INCOME),
                                incomeBeforeTax =
                                        getMonetaryValue(MetricCategory.INCOME_BEFORE_TAX),
                                incomeTaxExpense = getMonetaryValue(MetricCategory.INCOME_TAX),
                                netIncome = getMonetaryValue(MetricCategory.NET_INCOME),
                                basicEPS =
                                        metricMap[MetricCategory.EPS_BASIC]
                                                ?.getRawValueBigDecimal()
                                                ?.toDouble(),
                                dilutedEPS =
                                        metricMap[MetricCategory.EPS_DILUTED]
                                                ?.getRawValueBigDecimal()
                                                ?.toDouble()
                        )

                // 재무상태표 구성
                val balanceSheet =
                        StructuredBalanceSheet(
                                periodEnding = period,
                                cashAndEquivalents =
                                        getMonetaryValue(MetricCategory.CASH_AND_EQUIVALENTS),
                                accountsReceivable =
                                        getMonetaryValue(MetricCategory.ACCOUNTS_RECEIVABLE),
                                inventory = getMonetaryValue(MetricCategory.INVENTORY),
                                totalCurrentAssets =
                                        getMonetaryValue(MetricCategory.CURRENT_ASSETS),
                                totalAssets = getMonetaryValue(MetricCategory.TOTAL_ASSETS),
                                accountsPayable = getMonetaryValue(MetricCategory.ACCOUNTS_PAYABLE),
                                totalCurrentLiabilities =
                                        getMonetaryValue(MetricCategory.CURRENT_LIABILITIES),
                                longTermDebt = getMonetaryValue(MetricCategory.LONG_TERM_DEBT),
                                totalLiabilities =
                                        getMonetaryValue(MetricCategory.TOTAL_LIABILITIES),
                                retainedEarnings =
                                        getMonetaryValue(MetricCategory.RETAINED_EARNINGS),
                                totalStockholdersEquity =
                                        getMonetaryValue(MetricCategory.TOTAL_EQUITY)
                        )

                // 현금흐름표 구성
                val cashFlowStatement =
                        StructuredCashFlowStatement(
                                periodEnding = period,
                                periodType = metrics.firstOrNull()?.periodType,
                                netCashFromOperating =
                                        getMonetaryValue(MetricCategory.OPERATING_CASH_FLOW),
                                capitalExpenditures =
                                        getMonetaryValue(MetricCategory.CAPITAL_EXPENDITURES),
                                netCashFromInvesting =
                                        getMonetaryValue(MetricCategory.INVESTING_CASH_FLOW),
                                dividendsPaid = getMonetaryValue(MetricCategory.DIVIDENDS_PAID),
                                netCashFromFinancing =
                                        getMonetaryValue(MetricCategory.FINANCING_CASH_FLOW),
                                freeCashFlow = getMonetaryValue(MetricCategory.FREE_CASH_FLOW)
                        )

                // 핵심 재무 비율 계산
                val keyMetrics = calculateKeyMetrics(metricMap)

                // 데이터 품질 평가
                val dataQuality = assessDataQuality(metrics)
                val parsingConfidence =
                        metrics.map { it.confidence }.average().takeIf { !it.isNaN() } ?: 0.0

                return StructuredFinancialData(
                        companyName = companyName,
                        reportType = reportType,
                        fiscalYear = extractFiscalYear(period),
                        fiscalPeriod = extractFiscalPeriod(period, reportType),
                        incomeStatement = incomeStatement,
                        balanceSheet = balanceSheet,
                        cashFlowStatement = cashFlowStatement,
                        keyMetrics = keyMetrics,
                        parsingConfidence = parsingConfidence,
                        dataQuality = dataQuality
                )
        }

        /** 핵심 재무 비율 계산 */
        private fun calculateKeyMetrics(
                metricMap: Map<MetricCategory, ExtendedFinancialMetric>
        ): KeyFinancialMetrics {
                val revenue = metricMap[MetricCategory.REVENUE]?.getRawValueBigDecimal()?.toDouble()
                val grossProfit =
                        metricMap[MetricCategory.GROSS_PROFIT]?.getRawValueBigDecimal()?.toDouble()
                val operatingIncome =
                        metricMap[MetricCategory.OPERATING_INCOME]
                                ?.getRawValueBigDecimal()
                                ?.toDouble()
                val netIncome =
                        metricMap[MetricCategory.NET_INCOME]?.getRawValueBigDecimal()?.toDouble()
                val totalAssets =
                        metricMap[MetricCategory.TOTAL_ASSETS]?.getRawValueBigDecimal()?.toDouble()
                val totalEquity =
                        metricMap[MetricCategory.TOTAL_EQUITY]?.getRawValueBigDecimal()?.toDouble()
                val totalLiabilities =
                        metricMap[MetricCategory.TOTAL_LIABILITIES]
                                ?.getRawValueBigDecimal()
                                ?.toDouble()
                val currentAssets =
                        metricMap[MetricCategory.CURRENT_ASSETS]
                                ?.getRawValueBigDecimal()
                                ?.toDouble()
                val currentLiabilities =
                        metricMap[MetricCategory.CURRENT_LIABILITIES]
                                ?.getRawValueBigDecimal()
                                ?.toDouble()
                val inventory =
                        metricMap[MetricCategory.INVENTORY]?.getRawValueBigDecimal()?.toDouble()
                val cash =
                        metricMap[MetricCategory.CASH_AND_EQUIVALENTS]
                                ?.getRawValueBigDecimal()
                                ?.toDouble()
                val interestExpense =
                        metricMap[MetricCategory.INTEREST_EXPENSE]
                                ?.getRawValueBigDecimal()
                                ?.toDouble()

                return KeyFinancialMetrics(
                        // 수익성
                        grossMargin =
                                if (revenue != null && grossProfit != null && revenue > 0)
                                        (grossProfit / revenue * 100).coerceIn(-1000.0, 1000.0)
                                else null,
                        operatingMargin =
                                if (revenue != null && operatingIncome != null && revenue > 0)
                                        (operatingIncome / revenue * 100).coerceIn(-1000.0, 1000.0)
                                else null,
                        netProfitMargin =
                                if (revenue != null && netIncome != null && revenue > 0)
                                        (netIncome / revenue * 100).coerceIn(-1000.0, 1000.0)
                                else null,
                        returnOnAssets =
                                if (netIncome != null && totalAssets != null && totalAssets > 0)
                                        (netIncome / totalAssets * 100).coerceIn(-1000.0, 1000.0)
                                else null,
                        returnOnEquity =
                                if (netIncome != null && totalEquity != null && totalEquity > 0)
                                        (netIncome / totalEquity * 100).coerceIn(-1000.0, 1000.0)
                                else null,

                        // 유동성
                        currentRatio =
                                if (currentAssets != null &&
                                                currentLiabilities != null &&
                                                currentLiabilities > 0
                                )
                                        (currentAssets / currentLiabilities).coerceIn(0.0, 100.0)
                                else null,
                        quickRatio =
                                if (currentAssets != null &&
                                                inventory != null &&
                                                currentLiabilities != null &&
                                                currentLiabilities > 0
                                )
                                        ((currentAssets - inventory) / currentLiabilities).coerceIn(
                                                0.0,
                                                100.0
                                        )
                                else null,
                        cashRatio =
                                if (cash != null &&
                                                currentLiabilities != null &&
                                                currentLiabilities > 0
                                )
                                        (cash / currentLiabilities).coerceIn(0.0, 100.0)
                                else null,

                        // 지급능력
                        debtToEquity =
                                if (totalLiabilities != null &&
                                                totalEquity != null &&
                                                totalEquity > 0
                                )
                                        (totalLiabilities / totalEquity * 100).coerceIn(
                                                0.0,
                                                10000.0
                                        )
                                else null,
                        debtRatio =
                                if (totalLiabilities != null &&
                                                totalAssets != null &&
                                                totalAssets > 0
                                )
                                        (totalLiabilities / totalAssets * 100).coerceIn(0.0, 100.0)
                                else null,
                        interestCoverage =
                                if (operatingIncome != null &&
                                                interestExpense != null &&
                                                interestExpense > 0
                                )
                                        (operatingIncome / interestExpense).coerceIn(-100.0, 1000.0)
                                else null,

                        // 효율성
                        assetTurnover =
                                if (revenue != null && totalAssets != null && totalAssets > 0)
                                        (revenue / totalAssets).coerceIn(0.0, 100.0)
                                else null,

                        // 성장성 (YoY 변화가 있으면)
                        revenueGrowth =
                                metricMap[MetricCategory.REVENUE]
                                        ?.getYoyChangeBigDecimal()
                                        ?.toDouble(),
                        netIncomeGrowth =
                                metricMap[MetricCategory.NET_INCOME]
                                        ?.getYoyChangeBigDecimal()
                                        ?.toDouble()
                )
        }

        /** 데이터 품질 평가 */
        private fun assessDataQuality(metrics: List<ExtendedFinancialMetric>): DataQuality {
                val coreCategories =
                        setOf(
                                MetricCategory.REVENUE,
                                MetricCategory.NET_INCOME,
                                MetricCategory.TOTAL_ASSETS,
                                MetricCategory.TOTAL_LIABILITIES,
                                MetricCategory.TOTAL_EQUITY
                        )

                val foundCore = metrics.count { it.category in coreCategories }
                val avgConfidence =
                        metrics.map { it.confidence }.average().takeIf { !it.isNaN() } ?: 0.0
                val hasTableSource = metrics.any { it.source.contains("Table") }

                return when {
                        foundCore >= 4 && avgConfidence >= 0.8 && hasTableSource -> DataQuality.HIGH
                        foundCore >= 3 && avgConfidence >= 0.6 -> DataQuality.MEDIUM
                        foundCore >= 1 -> DataQuality.LOW
                        else -> DataQuality.UNKNOWN
                }
        }

        private fun extractFiscalYear(period: String?): String? {
                if (period == null) return null
                val yearMatch = Regex("""20\d{2}""").find(period)
                return yearMatch?.value
        }

        private fun extractFiscalPeriod(period: String?, reportType: String?): String? {
                return when {
                        reportType == "10-K" -> "FY"
                        period?.lowercase()?.contains("q1") == true -> "Q1"
                        period?.lowercase()?.contains("q2") == true -> "Q2"
                        period?.lowercase()?.contains("q3") == true -> "Q3"
                        period?.lowercase()?.contains("q4") == true -> "Q4"
                        period?.lowercase()?.contains("march") == true -> "Q1"
                        period?.lowercase()?.contains("june") == true -> "Q2"
                        period?.lowercase()?.contains("september") == true -> "Q3"
                        period?.lowercase()?.contains("december") == true -> "Q4"
                        reportType == "10-Q" -> "Quarterly"
                        else -> null
                }
        }

        /** 향상된 요약 생성 (구조화된 데이터 활용) */
        private fun generateEnhancedSummary(
                companyName: String?,
                reportType: String?,
                period: String?,
                metrics: List<FinancialMetric>,
                structuredData: StructuredFinancialData
        ): String {
                val sb = StringBuilder()

                // 헤더
                sb.appendLine("📊 Financial Analysis Summary")
                sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                sb.appendLine()

                // 기본 정보
                companyName?.let { sb.appendLine("🏢 Company: $it") }
                reportType?.let { sb.appendLine("📋 Report Type: SEC Form $it") }
                period?.let { sb.appendLine("📅 Period: $it") }

                // 데이터 품질 표시
                val qualityEmoji =
                        when (structuredData.dataQuality) {
                                DataQuality.HIGH -> "🟢"
                                DataQuality.MEDIUM -> "🟡"
                                DataQuality.LOW -> "🟠"
                                DataQuality.UNKNOWN -> "⚪"
                        }
                sb.appendLine("📈 Data Quality: $qualityEmoji ${structuredData.dataQuality.name}")
                sb.appendLine()

                // 핵심 재무 지표 (구조화된 데이터 사용)
                val income = structuredData.incomeStatement
                val balance = structuredData.balanceSheet
                val keyMetrics = structuredData.keyMetrics

                if (income != null || balance != null) {
                        sb.appendLine("📌 Key Financial Highlights")
                        sb.appendLine("─────────────────────────────")

                        income?.totalRevenue?.let {
                                sb.appendLine(
                                        "  💰 Revenue: ${it.formatted}${formatYoY(it.getYoyChangeBigDecimal()?.toDouble())}"
                                )
                        }
                        income?.grossProfit?.let {
                                sb.appendLine("  📊 Gross Profit: ${it.formatted}")
                        }
                        income?.operatingIncome?.let {
                                sb.appendLine("  📈 Operating Income: ${it.formatted}")
                        }
                        income?.netIncome?.let {
                                sb.appendLine(
                                        "  💵 Net Income: ${it.formatted}${formatYoY(it.getYoyChangeBigDecimal()?.toDouble())}"
                                )
                        }
                        income?.basicEPS?.let {
                                sb.appendLine("  📉 EPS (Basic): $${String.format("%.2f", it)}")
                        }
                        sb.appendLine()

                        balance?.totalAssets?.let {
                                sb.appendLine("  🏦 Total Assets: ${it.formatted}")
                        }
                        balance?.cashAndEquivalents?.let {
                                sb.appendLine("  💵 Cash & Equivalents: ${it.formatted}")
                        }
                        balance?.totalLiabilities?.let {
                                sb.appendLine("  📋 Total Liabilities: ${it.formatted}")
                        }
                        balance?.totalStockholdersEquity?.let {
                                sb.appendLine("  💎 Shareholders' Equity: ${it.formatted}")
                        }
                        sb.appendLine()
                }

                // 핵심 비율
                if (keyMetrics != null) {
                        val hasRatios =
                                listOfNotNull(
                                                keyMetrics.grossMargin,
                                                keyMetrics.operatingMargin,
                                                keyMetrics.netProfitMargin,
                                                keyMetrics.currentRatio
                                        )
                                        .isNotEmpty()

                        if (hasRatios) {
                                sb.appendLine("📐 Key Financial Ratios")
                                sb.appendLine("─────────────────────────────")
                                keyMetrics.grossMargin?.let {
                                        sb.appendLine(
                                                "  • Gross Margin: ${String.format("%.1f", it)}%"
                                        )
                                }
                                keyMetrics.operatingMargin?.let {
                                        sb.appendLine(
                                                "  • Operating Margin: ${String.format("%.1f", it)}%"
                                        )
                                }
                                keyMetrics.netProfitMargin?.let {
                                        sb.appendLine(
                                                "  • Net Profit Margin: ${String.format("%.1f", it)}%"
                                        )
                                }
                                keyMetrics.returnOnEquity?.let {
                                        sb.appendLine("  • ROE: ${String.format("%.1f", it)}%")
                                }
                                keyMetrics.currentRatio?.let {
                                        sb.appendLine(
                                                "  • Current Ratio: ${String.format("%.2f", it)}x"
                                        )
                                }
                                keyMetrics.debtToEquity?.let {
                                        sb.appendLine(
                                                "  • Debt/Equity: ${String.format("%.0f", it)}%"
                                        )
                                }
                                sb.appendLine()
                        }
                }

                // 폴백: 기존 메트릭 그룹핑
                if (structuredData.incomeStatement?.totalRevenue == null && metrics.isNotEmpty()) {
                        sb.appendLine("📌 Detected Metrics")
                        sb.appendLine("─────────────────────────────")

                        val grouped =
                                metrics.groupBy { metric ->
                                        when {
                                                metric.name.contains(
                                                        "Revenue",
                                                        ignoreCase = true
                                                ) ||
                                                        metric.name.contains(
                                                                "Sales",
                                                                ignoreCase = true
                                                        ) -> "Revenue"
                                                metric.name.contains("Income", ignoreCase = true) ||
                                                        metric.name.contains(
                                                                "Profit",
                                                                ignoreCase = true
                                                        ) -> "Income"
                                                metric.name.contains("Assets", ignoreCase = true) ->
                                                        "Assets"
                                                metric.name.contains(
                                                        "Liabilities",
                                                        ignoreCase = true
                                                ) -> "Liabilities"
                                                metric.name.contains("Equity", ignoreCase = true) ->
                                                        "Equity"
                                                else -> "Other"
                                        }
                                }

                        for ((category, metricsList) in grouped) {
                                for (metric in metricsList.take(2)) {
                                        val formatted =
                                                metric.rawValue?.let { formatNumber(it) }
                                                        ?: metric.value
                                        sb.appendLine("  • ${metric.name}: $formatted")
                                }
                        }
                        sb.appendLine()
                }

                if (metrics.isEmpty() && structuredData.incomeStatement?.totalRevenue == null) {
                        sb.appendLine("⚠️ No financial metrics were automatically detected.")
                        sb.appendLine("   The document may be in an unsupported format or")
                        sb.appendLine("   may not contain standard financial statements.")
                }

                return sb.toString()
        }

        private fun formatYoY(change: Double?): String {
                if (change == null) return ""
                val sign = if (change >= 0) "+" else ""
                val emoji = if (change >= 0) "📈" else "📉"
                return " $emoji ${sign}${String.format("%.1f", change)}% YoY"
        }

        // ==========================================
        // 초보자 친화적 분석 기능 (향상된 파서 사용)
        // ==========================================

        /** 초보자를 위한 심화 분석 - Enhanced Parser 사용 */
        fun analyzeForBeginners(fileName: String, content: String): FinancialAnalysis {
                // Check cache first
                val cached = AnalysisCache.loadAnalysis(content)
                if (cached != null) {
                        println("✓ Loaded analysis from cache")
                        return cached
                }

                println("Performing fresh analysis...")
                val basicAnalysis = analyzeDocument(fileName, content)

                // 향상된 파서로 더 많은 지표 추출
                val extendedMetrics = EnhancedFinancialParser.parseFinancialMetrics(content)
                val riskFactors = EnhancedFinancialParser.parseRiskFactors(content)

                // 기존 메트릭과 새 메트릭 병합
                val allMetrics = mergeMetrics(basicAnalysis.metrics, extendedMetrics)

                // 향상된 비율 계산
                val ratios =
                        if (extendedMetrics.isNotEmpty()) {
                                EnhancedFinancialParser.calculateRatios(extendedMetrics)
                        } else {
                                calculateFinancialRatios(basicAnalysis.metrics)
                        }

                // 초보자 인사이트 생성 (확장된 데이터 사용)
                val insights =
                        generateEnhancedBeginnerInsights(
                                basicAnalysis,
                                ratios,
                                extendedMetrics,
                                riskFactors
                        )

                // 용어 설명 생성
                val termExplanations = generateTermExplanations()

                // 재무 건전성 점수 계산
                val healthScore = calculateEnhancedHealthScore(allMetrics, ratios, riskFactors)

                // 보고서 유형 설명
                val reportExplanation = getReportTypeExplanation(basicAnalysis.reportType)

                // 핵심 요점 생성
                val keyTakeaways =
                        generateEnhancedKeyTakeaways(
                                basicAnalysis,
                                ratios,
                                healthScore,
                                extendedMetrics,
                                riskFactors
                        )

                val result =
                        basicAnalysis.copy(
                                metrics = allMetrics,
                                ratios = ratios,
                                beginnerInsights = insights,
                                termExplanations = termExplanations,
                                healthScore = healthScore,
                                reportTypeExplanation = reportExplanation,
                                keyTakeaways = keyTakeaways,
                                extendedMetrics = extendedMetrics
                        )

                // Save to cache
                AnalysisCache.saveAnalysis(content, result)
                println("✓ Analysis cached for future use")

                return result
        }

        /** 기존 메트릭과 확장 메트릭 병합 */
        private fun mergeMetrics(
                basic: List<FinancialMetric>,
                extended: List<ExtendedFinancialMetric>
        ): List<FinancialMetric> {
                val merged = basic.toMutableList()

                // 확장 메트릭 중 기존에 없는 것 추가
                for (ext in extended) {
                        val exists = basic.any { it.name.equals(ext.name, ignoreCase = true) }
                        if (!exists && ext.rawValue != null) {
                                merged.add(
                                        FinancialMetric(
                                                name = ext.name,
                                                value = ext.value,
                                                rawValue = ext.getRawValueBigDecimal()?.toDouble(),
                                                context = ext.context
                                        )
                                )
                        }
                }

                return merged
        }

        /** 확장된 초보자 인사이트 생성 */
        private fun generateEnhancedBeginnerInsights(
                analysis: FinancialAnalysis,
                ratios: List<FinancialRatio>,
                extendedMetrics: List<ExtendedFinancialMetric>,
                riskFactors: List<RiskFactor>
        ): List<BeginnerInsight> {
                val insights = mutableListOf<BeginnerInsight>()

                // 회사 규모 인사이트
                val revenue =
                        extendedMetrics
                                .find { it.category == MetricCategory.REVENUE }
                                ?.getRawValueBigDecimal()
                                ?.toDouble()
                                ?: findMetricValue(
                                        analysis.metrics,
                                        listOf("Revenue", "Sales", "Total Revenue")
                                )

                if (revenue != null) {
                        insights.add(createCompanySizeInsight(revenue))
                }

                // 수익성 인사이트
                val profitMargin = ratios.find { it.name.contains("순이익률") }
                val grossMargin = ratios.find { it.name.contains("매출총이익률") }
                val operatingMargin = ratios.find { it.name.contains("영업이익률") }

                if (profitMargin != null || grossMargin != null || operatingMargin != null) {
                        insights.add(
                                createProfitabilityInsight(
                                        profitMargin,
                                        grossMargin,
                                        operatingMargin
                                )
                        )
                }

                // 재무 안정성 인사이트
                val debtRatio = ratios.find { it.name.contains("부채비율") }
                val currentRatio = ratios.find { it.name.contains("유동비율") }

                if (debtRatio != null || currentRatio != null) {
                        insights.add(createFinancialStabilityInsight(debtRatio, currentRatio))
                }

                // 현금 흐름 인사이트
                val cashFlow =
                        extendedMetrics.find { it.category == MetricCategory.OPERATING_CASH_FLOW }
                val freeCashFlow =
                        extendedMetrics.find { it.category == MetricCategory.FREE_CASH_FLOW }
                val cash =
                        extendedMetrics.find { it.category == MetricCategory.CASH_AND_EQUIVALENTS }

                if (cashFlow != null || freeCashFlow != null || cash != null) {
                        insights.add(createCashFlowInsight(cashFlow, freeCashFlow, cash))
                }

                // 위험 요소 인사이트
                if (riskFactors.isNotEmpty()) {
                        insights.add(createRiskInsight(riskFactors))
                }

                // 보고서 유형 인사이트
                if (analysis.reportType != null) {
                        insights.add(createReportTypeInsight(analysis.reportType))
                }

                // 투자 효율성 인사이트 (ROE, ROA)
                val roe = ratios.find { it.name.contains("ROE") || it.name.contains("자기자본이익률") }
                val roa = ratios.find { it.name.contains("ROA") || it.name.contains("총자산이익률") }

                if (roe != null || roa != null) {
                        insights.add(createInvestmentEfficiencyInsight(roe, roa))
                }

                return insights
        }

        private fun createCompanySizeInsight(revenue: Double): BeginnerInsight {
                val sizeInfo =
                        when {
                                revenue >= 50_000_000_000 -> Pair("초대형 기업", "포춘 500 수준의 글로벌 대기업")
                                revenue >= 10_000_000_000 -> Pair("대기업", "국내외 유명 대기업과 비슷한 규모")
                                revenue >= 1_000_000_000 -> Pair("중대형 기업", "안정적인 대형 기업")
                                revenue >= 100_000_000 -> Pair("중형 기업", "성장 중인 중견 기업")
                                revenue >= 10_000_000 -> Pair("중소기업", "성장 가능성이 있는 기업")
                                else -> Pair("소규모 기업", "초기 단계 또는 소규모 기업")
                        }
                val sizeCategory = sizeInfo.first
                val comparisonText = sizeInfo.second

                return BeginnerInsight(
                        title = "회사 규모 분석",
                        emoji = "🏢",
                        summary = "$sizeCategory (연매출 ${formatNumber(revenue)})",
                        detailedExplanation =
                                """
                이 회사의 연간 매출 규모는 ${formatNumber(revenue)}입니다.
                ${comparisonText}에 해당합니다.
                
                📊 규모별 특징:
                • 대기업: 안정적이지만 성장률은 낮을 수 있음
                • 중형기업: 성장과 안정성의 균형
                • 소형기업: 높은 성장 가능성, 하지만 리스크도 높음
            """.trimIndent(),
                        whatItMeans =
                                "매출은 회사가 제품이나 서비스를 팔아서 벌어들인 총 금액입니다. 회사의 '크기'를 나타내는 가장 기본적인 지표입니다.",
                        whyItMatters =
                                "매출 규모는 회사의 시장 지위, 협상력, 그리고 경기 변동에 대한 저항력을 보여줍니다. 일반적으로 규모가 클수록 안정적입니다.",
                        actionableAdvice =
                                "같은 산업의 경쟁사들과 매출을 비교해 보세요. 또한 매출 성장률도 함께 확인하면 회사의 성장성을 파악할 수 있습니다."
                )
        }

        private fun createProfitabilityInsight(
                netMargin: FinancialRatio?,
                grossMargin: FinancialRatio?,
                opMargin: FinancialRatio?
        ): BeginnerInsight {
                val mainRatio = netMargin ?: opMargin ?: grossMargin
                val status =
                        when (mainRatio?.healthStatus) {
                                HealthStatus.EXCELLENT -> "매우 우수"
                                HealthStatus.GOOD -> "양호"
                                HealthStatus.NEUTRAL -> "보통"
                                HealthStatus.CAUTION -> "주의 필요"
                                HealthStatus.WARNING -> "심각"
                                null -> "분석 불가"
                        }

                val ratioDetails = buildString {
                        grossMargin?.let { appendLine("• 매출총이익률: ${it.formattedValue}") }
                        opMargin?.let { appendLine("• 영업이익률: ${it.formattedValue}") }
                        netMargin?.let { appendLine("• 순이익률: ${it.formattedValue}") }
                }

                return BeginnerInsight(
                        title = "수익성 분석",
                        emoji = "💰",
                        summary = "$status (${mainRatio?.formattedValue ?: "N/A"})",
                        detailedExplanation =
                                """
                이 회사의 수익성 지표입니다:
                $ratioDetails
                
                📈 수익성 해석:
                • 매출총이익률: 제품/서비스 자체의 수익성
                • 영업이익률: 영업활동의 효율성
                • 순이익률: 최종적으로 남는 이익
            """.trimIndent(),
                        whatItMeans =
                                """
                수익성 지표는 '100원 팔았을 때 실제로 얼마가 남는가'를 보여줍니다.
                
                예시: 순이익률 10% = 100원 매출 시 10원이 순이익
            """.trimIndent(),
                        whyItMatters =
                                "수익성이 높을수록 회사가 효율적으로 돈을 벌고 있다는 뜻입니다. 주주에게 배당을 주거나 미래 성장에 투자할 여력이 있습니다.",
                        actionableAdvice =
                                when (mainRatio?.healthStatus) {
                                        HealthStatus.EXCELLENT, HealthStatus.GOOD ->
                                                "수익성이 좋습니다! 이 수익이 지속 가능한지, 그리고 경쟁사 대비 어느 수준인지 확인해 보세요."
                                        HealthStatus.NEUTRAL ->
                                                "업계 평균과 비교해 보세요. 마진 개선 가능성이 있는지 확인하세요."
                                        HealthStatus.CAUTION, HealthStatus.WARNING ->
                                                "수익성이 낮습니다. 비용 구조나 가격 경쟁력에 문제가 없는지 살펴보세요."
                                        null -> "재무제표에서 수익성 지표를 찾을 수 없습니다. 원본 문서를 확인해 주세요."
                                }
                )
        }

        private fun createFinancialStabilityInsight(
                debtRatio: FinancialRatio?,
                currentRatio: FinancialRatio?
        ): BeginnerInsight {
                val status =
                        when {
                                debtRatio?.healthStatus == HealthStatus.WARNING ||
                                        currentRatio?.healthStatus == HealthStatus.WARNING -> "위험"
                                debtRatio?.healthStatus == HealthStatus.CAUTION ||
                                        currentRatio?.healthStatus == HealthStatus.CAUTION ->
                                        "주의 필요"
                                debtRatio?.healthStatus == HealthStatus.EXCELLENT &&
                                        (currentRatio?.healthStatus == HealthStatus.EXCELLENT ||
                                                currentRatio == null) -> "매우 안정적"
                                else -> "양호"
                        }

                return BeginnerInsight(
                        title = "재무 안정성",
                        emoji = "⚖️",
                        summary = "$status",
                        detailedExplanation =
                                buildString {
                                        appendLine("이 회사의 재무 안정성 지표입니다:")
                                        appendLine()
                                        debtRatio?.let {
                                                appendLine("📊 부채비율: ${it.formattedValue}")
                                                appendLine("   → ${it.interpretation}")
                                        }
                                        currentRatio?.let {
                                                appendLine()
                                                appendLine("💧 유동비율: ${it.formattedValue}")
                                                appendLine("   → ${it.interpretation}")
                                        }
                                },
                        whatItMeans =
                                """
                • 부채비율: 자기 돈(자본) 대비 빌린 돈(부채)의 비율
                  예: 100% = 자기 돈만큼 빚이 있음
                  
                • 유동비율: 1년 내 갚아야 할 빚 대비 현금화 가능 자산
                  예: 2.0 = 단기 부채의 2배만큼 자산이 있음
            """.trimIndent(),
                        whyItMatters =
                                "재무 안정성이 낮으면 경기 침체나 금리 인상 시 위험할 수 있습니다. 특히 부채가 많으면 이자 비용이 수익을 깎아먹을 수 있습니다.",
                        actionableAdvice =
                                when (status) {
                                        "매우 안정적", "양호" ->
                                                "재무가 안정적입니다. 다만 지나치게 보수적인 경영은 아닌지도 확인해 보세요."
                                        "주의 필요" -> "부채 수준을 주시하세요. 향후 금리 인상 시 이자 부담이 커질 수 있습니다."
                                        else -> "재무 위험이 높습니다. 단기 부채 상환 계획과 현금 흐름을 면밀히 확인하세요."
                                }
                )
        }

        private fun createCashFlowInsight(
                operatingCashFlow: ExtendedFinancialMetric?,
                freeCashFlow: ExtendedFinancialMetric?,
                cash: ExtendedFinancialMetric?
        ): BeginnerInsight {
                val cashFlowValue = operatingCashFlow?.getRawValueBigDecimal()?.toDouble()
                val fcfValue = freeCashFlow?.getRawValueBigDecimal()?.toDouble()

                val status =
                        when {
                                cashFlowValue != null &&
                                        cashFlowValue > 0 &&
                                        fcfValue != null &&
                                        fcfValue > 0 -> "건강함"
                                cashFlowValue != null && cashFlowValue > 0 -> "양호"
                                cashFlowValue != null && cashFlowValue < 0 -> "주의 필요"
                                else -> "분석 필요"
                        }

                return BeginnerInsight(
                        title = "현금 흐름 분석",
                        emoji = "💵",
                        summary = status,
                        detailedExplanation =
                                buildString {
                                        appendLine("현금 흐름은 회사의 '실제 돈의 움직임'을 보여줍니다:")
                                        appendLine()
                                        operatingCashFlow?.let {
                                                appendLine("📈 영업현금흐름: ${it.value}")
                                                if (it.getRawValueBigDecimal()?.toDouble()?.let { v
                                                                ->
                                                                v > 0.0
                                                        } == true
                                                ) {
                                                        appendLine("   → 영업활동에서 현금이 들어오고 있습니다 ✅")
                                                } else {
                                                        appendLine("   → 영업활동에서 현금이 나가고 있습니다 ⚠️")
                                                }
                                        }
                                        freeCashFlow?.let {
                                                appendLine()
                                                appendLine("💰 잉여현금흐름(FCF): ${it.value}")
                                                appendLine("   → 투자 후 자유롭게 쓸 수 있는 현금")
                                        }
                                        cash?.let {
                                                appendLine()
                                                appendLine("🏦 보유 현금: ${it.value}")
                                        }
                                },
                        whatItMeans =
                                """
                • 영업현금흐름: 본업에서 실제로 들어온 현금
                • 잉여현금흐름(FCF): 투자 후 남는 현금 (배당, 자사주 매입에 사용 가능)
                • 보유 현금: 지금 당장 쓸 수 있는 현금
                
                💡 순이익이 있어도 현금흐름이 마이너스면 위험할 수 있습니다!
            """.trimIndent(),
                        whyItMatters =
                                "현금은 회사의 생명줄입니다. 아무리 이익이 나도 현금이 없으면 부도가 날 수 있습니다. 현금흐름은 회계 조작이 어려워 신뢰도가 높습니다.",
                        actionableAdvice =
                                when (status) {
                                        "건강함" -> "현금 창출 능력이 우수합니다! 이 현금을 어떻게 활용하는지 확인해 보세요."
                                        "양호" -> "영업에서 현금이 들어오고 있습니다. 투자 활동과 재무 활동도 함께 확인하세요."
                                        "주의 필요" -> "영업현금흐름이 마이너스입니다. 일시적인지 구조적인지 파악이 필요합니다."
                                        else -> "현금흐름표를 직접 확인해 보세요."
                                }
                )
        }

        private fun createRiskInsight(riskFactors: List<RiskFactor>): BeginnerInsight {
                val highRisks =
                        riskFactors.filter {
                                it.severity == RiskSeverity.HIGH ||
                                        it.severity == RiskSeverity.CRITICAL
                        }
                val riskByCategory = riskFactors.groupBy { it.category }

                val mainRisks = buildString {
                        riskFactors.take(5).forEachIndexed { index, risk ->
                                val emoji =
                                        when (risk.category) {
                                                RiskCategory.MARKET -> "📊"
                                                RiskCategory.OPERATIONAL -> "⚙️"
                                                RiskCategory.FINANCIAL -> "💰"
                                                RiskCategory.REGULATORY -> "📜"
                                                RiskCategory.COMPETITIVE -> "🏃"
                                                RiskCategory.TECHNOLOGY -> "💻"
                                                RiskCategory.LEGAL -> "⚖️"
                                                RiskCategory.ENVIRONMENTAL -> "🌍"
                                                RiskCategory.GEOPOLITICAL -> "🌐"
                                                RiskCategory.OTHER -> "📌"
                                        }
                                appendLine("${index + 1}. $emoji ${risk.title.take(50)}...")
                        }
                }

                return BeginnerInsight(
                        title = "주요 위험 요소",
                        emoji = "⚠️",
                        summary = "${riskFactors.size}개 위험 요소 (고위험 ${highRisks.size}개)",
                        detailedExplanation =
                                """
                SEC 보고서에서 발견된 주요 위험 요소입니다:
                
                $mainRisks
                
                📊 카테고리별 분류:
                ${riskByCategory.entries.take(5).joinToString("\n") { (cat, risks) ->
                    "• ${cat.name}: ${risks.size}개"
                }}
            """.trimIndent(),
                        whatItMeans =
                                """
                위험 요소(Risk Factors)는 회사가 직면한 잠재적 문제들입니다.
                SEC는 모든 상장기업에 위험 요소 공시를 의무화하고 있습니다.
                
                💡 모든 회사에 위험 요소가 있는 것은 정상입니다!
            """.trimIndent(),
                        whyItMatters =
                                "위험 요소를 미리 알면 투자 결정에 도움이 됩니다. 특히 경쟁 위험, 규제 위험, 재무 위험은 주의 깊게 살펴봐야 합니다.",
                        actionableAdvice =
                                if (highRisks.isNotEmpty()) {
                                        "고위험 요소가 있습니다. 해당 위험이 현실화될 가능성과 영향을 신중히 판단하세요."
                                } else {
                                        "위험 요소들이 관리 가능한 수준인지, 경쟁사와 비교하여 어떤지 확인해 보세요."
                                }
                )
        }

        private fun createReportTypeInsight(reportType: String): BeginnerInsight {
                return BeginnerInsight(
                        title = "이 보고서는?",
                        emoji = "📋",
                        summary = "SEC Form $reportType",
                        detailedExplanation = getReportTypeExplanation(reportType)
                                        ?: "SEC 공시 보고서입니다.",
                        whatItMeans =
                                when (reportType) {
                                        "10-K" ->
                                                "연간 보고서(10-K)는 회사의 1년간 성과를 담은 '성적표'입니다. 가장 포괄적인 재무 정보를 담고 있습니다."
                                        "10-Q" ->
                                                "분기 보고서(10-Q)는 3개월간의 성과를 보여줍니다. 연간 보고서보다 간략하지만 최신 상황을 파악할 수 있습니다."
                                        "8-K" ->
                                                "수시 보고서(8-K)는 중요한 사건 발생 시 제출됩니다. 인수합병, CEO 교체 등 큰 뉴스가 있을 때 나옵니다."
                                        else -> "SEC에 제출되는 공식 재무 보고서입니다."
                                },
                        whyItMatters =
                                "SEC 보고서는 법적으로 정확해야 하므로 회사 홍보 자료보다 신뢰할 수 있습니다. 투자 결정의 핵심 자료입니다.",
                        actionableAdvice =
                                when (reportType) {
                                        "10-K" ->
                                                "연간보고서의 'Business', 'Risk Factors', 'MD&A' 섹션을 중점적으로 읽어보세요."
                                        "10-Q" -> "전 분기, 전년 동기와 비교하면서 트렌드를 파악하세요."
                                        "8-K" -> "어떤 중요 사건이 발생했는지, 그 영향은 무엇인지 확인하세요."
                                        else -> "재무제표와 주석을 꼼꼼히 읽어보세요."
                                }
                )
        }

        private fun createInvestmentEfficiencyInsight(
                roe: FinancialRatio?,
                roa: FinancialRatio?
        ): BeginnerInsight {
                val mainRatio = roe ?: roa
                val status =
                        when (mainRatio?.healthStatus) {
                                HealthStatus.EXCELLENT -> "매우 효율적"
                                HealthStatus.GOOD -> "효율적"
                                HealthStatus.NEUTRAL -> "보통"
                                HealthStatus.CAUTION -> "비효율적"
                                HealthStatus.WARNING -> "매우 비효율적"
                                null -> "분석 불가"
                        }

                return BeginnerInsight(
                        title = "투자 효율성",
                        emoji = "📈",
                        summary = status,
                        detailedExplanation =
                                buildString {
                                        appendLine("이 회사가 자본을 얼마나 효율적으로 활용하는지 보여줍니다:")
                                        appendLine()
                                        roe?.let {
                                                appendLine("👤 ROE (자기자본이익률): ${it.formattedValue}")
                                                appendLine("   → ${it.interpretation}")
                                        }
                                        roa?.let {
                                                appendLine()
                                                appendLine("🏢 ROA (총자산이익률): ${it.formattedValue}")
                                                appendLine("   → ${it.interpretation}")
                                        }
                                },
                        whatItMeans =
                                """
                • ROE: 주주가 투자한 돈으로 얼마나 벌었는가
                  예: ROE 15% = 100만원 투자하면 15만원 수익 창출
                  
                • ROA: 회사의 모든 자산으로 얼마나 벌었는가
                  예: ROA 5% = 100억 자산으로 5억 수익 창출
            """.trimIndent(),
                        whyItMatters =
                                "높은 ROE/ROA는 경영진이 자본을 효율적으로 운용하고 있다는 뜻입니다. 다만 부채를 많이 쓰면 ROE가 높아질 수 있어 함께 분석해야 합니다.",
                        actionableAdvice =
                                when (mainRatio?.healthStatus) {
                                        HealthStatus.EXCELLENT, HealthStatus.GOOD ->
                                                "투자 효율성이 좋습니다! 이 수준이 지속 가능한지 확인하세요."
                                        HealthStatus.NEUTRAL -> "평균 수준입니다. 업계 평균과 비교해 보세요."
                                        HealthStatus.CAUTION, HealthStatus.WARNING ->
                                                "자본 활용 효율성이 낮습니다. 경영 효율화가 필요할 수 있습니다."
                                        null -> "ROE/ROA를 계산하기 위한 데이터가 부족합니다."
                                }
                )
        }

        /** 향상된 건강 점수 계산 */
        private fun calculateEnhancedHealthScore(
                metrics: List<FinancialMetric>,
                ratios: List<FinancialRatio>,
                riskFactors: List<RiskFactor>
        ): FinancialHealthScore {
                var totalScore = 0
                var count = 0
                val strengths = mutableListOf<String>()
                val weaknesses = mutableListOf<String>()
                val recommendations = mutableListOf<String>()

                // 비율 기반 점수
                for (ratio in ratios) {
                        val score =
                                when (ratio.healthStatus) {
                                        HealthStatus.EXCELLENT -> 100
                                        HealthStatus.GOOD -> 80
                                        HealthStatus.NEUTRAL -> 60
                                        HealthStatus.CAUTION -> 40
                                        HealthStatus.WARNING -> 20
                                }
                        totalScore += score
                        count++

                        when (ratio.healthStatus) {
                                HealthStatus.EXCELLENT ->
                                        strengths.add("🌟 ${ratio.name}: ${ratio.formattedValue}")
                                HealthStatus.GOOD ->
                                        strengths.add("✅ ${ratio.name}: ${ratio.formattedValue}")
                                HealthStatus.CAUTION ->
                                        weaknesses.add("⚠️ ${ratio.name}: ${ratio.formattedValue}")
                                HealthStatus.WARNING ->
                                        weaknesses.add("🚨 ${ratio.name}: ${ratio.formattedValue}")
                                else -> {}
                        }
                }

                // 위험 요소 반영 (고위험이 많으면 감점)
                val highRiskCount =
                        riskFactors.count {
                                it.severity == RiskSeverity.HIGH ||
                                        it.severity == RiskSeverity.CRITICAL
                        }
                if (highRiskCount > 0) {
                        totalScore -= highRiskCount * 5
                        weaknesses.add("⚠️ 고위험 요소 ${highRiskCount}개 발견")
                }

                // 데이터 충분성 보너스
                if (metrics.size >= 10) {
                        totalScore += 5
                }

                val overallScore = if (count > 0) (totalScore / count).coerceIn(0, 100) else 50
                val grade =
                        when {
                                overallScore >= 90 -> "A+"
                                overallScore >= 85 -> "A"
                                overallScore >= 80 -> "B+"
                                overallScore >= 75 -> "B"
                                overallScore >= 70 -> "C+"
                                overallScore >= 60 -> "C"
                                overallScore >= 50 -> "D"
                                else -> "F"
                        }

                val summary =
                        when {
                                overallScore >= 80 ->
                                        "📈 전반적으로 재무 상태가 양호합니다. 안정적인 투자 대상으로 고려할 수 있습니다."
                                overallScore >= 60 -> "📊 평균적인 재무 상태입니다. 몇 가지 개선이 필요한 부분이 있습니다."
                                overallScore >= 40 -> "⚠️ 주의가 필요한 재무 상태입니다. 투자 전 심층 분석을 권장합니다."
                                else -> "🚨 재무 상태에 심각한 문제가 있을 수 있습니다. 신중한 판단이 필요합니다."
                        }

                // 권장사항 생성
                if (weaknesses.any { it.contains("부채") }) {
                        recommendations.add("💡 부채 수준을 주시하세요. 금리 인상 시 이자 부담이 커질 수 있습니다.")
                }
                if (weaknesses.any { it.contains("이익") || it.contains("수익") }) {
                        recommendations.add("💡 수익성 개선 노력이 필요합니다. 비용 구조를 확인해 보세요.")
                }
                if (highRiskCount > 0) {
                        recommendations.add("💡 고위험 요소들을 면밀히 검토하세요. 해당 위험의 현실화 가능성을 평가하세요.")
                }
                if (ratios.size < 3) {
                        recommendations.add("💡 더 정확한 분석을 위해 전체 재무제표를 확인해 보세요.")
                }
                if (overallScore >= 70) {
                        recommendations.add("💡 경쟁사와 비교 분석을 통해 상대적 위치를 파악해 보세요.")
                }

                return FinancialHealthScore(
                        overallScore = overallScore,
                        grade = grade,
                        summary = summary,
                        strengths = strengths.take(5),
                        weaknesses = weaknesses.take(5),
                        recommendations = recommendations.take(4)
                )
        }

        /** 향상된 핵심 요점 생성 */
        private fun generateEnhancedKeyTakeaways(
                analysis: FinancialAnalysis,
                ratios: List<FinancialRatio>,
                healthScore: FinancialHealthScore,
                extendedMetrics: List<ExtendedFinancialMetric>,
                riskFactors: List<RiskFactor>
        ): List<String> {
                val takeaways = mutableListOf<String>()

                // 건강 점수
                takeaways.add("📊 재무 건전성: ${healthScore.grade} (${healthScore.overallScore}점)")

                // 회사 정보
                analysis.companyName?.let { takeaways.add("🏢 $it") }
                analysis.reportType?.let { takeaways.add("📋 SEC Form $it 보고서") }

                // 주요 수치
                val revenue = extendedMetrics.find { it.category == MetricCategory.REVENUE }
                val netIncome = extendedMetrics.find { it.category == MetricCategory.NET_INCOME }

                revenue?.let { takeaways.add("💰 매출: ${it.value}") }
                netIncome?.let { takeaways.add("💵 순이익: ${it.value}") }

                // 강점/약점
                val excellentRatios = ratios.filter { it.healthStatus == HealthStatus.EXCELLENT }
                val warningRatios = ratios.filter { it.healthStatus == HealthStatus.WARNING }

                if (excellentRatios.isNotEmpty()) {
                        takeaways.add(
                                "⭐ 강점: ${excellentRatios.first().name.substringBefore("(").trim()}"
                        )
                }
                if (warningRatios.isNotEmpty()) {
                        takeaways.add(
                                "🚨 주의: ${warningRatios.first().name.substringBefore("(").trim()}"
                        )
                }

                // 위험 요소
                if (riskFactors.isNotEmpty()) {
                        val highRisks = riskFactors.count { it.severity == RiskSeverity.HIGH }
                        takeaways.add("⚠️ 위험 요소: ${riskFactors.size}개 (고위험 ${highRisks}개)")
                }

                return takeaways.take(7)
        }

        /** 재무 비율 계산 (기본) */
        private fun calculateFinancialRatios(metrics: List<FinancialMetric>): List<FinancialRatio> {
                val ratios = mutableListOf<FinancialRatio>()

                // 메트릭에서 값 추출
                val revenue =
                        findMetricValue(
                                metrics,
                                listOf("Revenue", "Sales", "Net Revenue", "Total Revenue")
                        )
                val netIncome =
                        findMetricValue(metrics, listOf("Net Income", "Net Earnings", "Profit"))
                val totalAssets = findMetricValue(metrics, listOf("Total Assets"))
                val totalLiabilities = findMetricValue(metrics, listOf("Total Liabilities"))
                val totalEquity =
                        findMetricValue(metrics, listOf("Total Equity", "Stockholders' Equity"))
                val currentAssets =
                        findMetricValue(metrics, listOf("Total Current Assets", "Current Assets"))
                val currentLiabilities =
                        findMetricValue(
                                metrics,
                                listOf("Total Current Liabilities", "Current Liabilities")
                        )

                // 수익성 비율: 순이익률 (Net Profit Margin)
                if (revenue != null && netIncome != null && revenue > 0) {
                        val margin = (netIncome / revenue) * 100
                        val status =
                                when {
                                        margin >= 20 -> HealthStatus.EXCELLENT
                                        margin >= 10 -> HealthStatus.GOOD
                                        margin >= 5 -> HealthStatus.NEUTRAL
                                        margin >= 0 -> HealthStatus.CAUTION
                                        else -> HealthStatus.WARNING
                                }
                        ratios.add(
                                FinancialRatio(
                                        name = "순이익률 (Net Profit Margin)",
                                        value = margin,
                                        formattedValue = String.format("%.1f%%", margin),
                                        description = "매출 대비 순이익의 비율",
                                        interpretation = getMarginInterpretation(margin),
                                        healthStatus = status,
                                        category = RatioCategory.PROFITABILITY
                                )
                        )
                }

                // ROA (Return on Assets)
                if (netIncome != null && totalAssets != null && totalAssets > 0) {
                        val roa = (netIncome / totalAssets) * 100
                        val status =
                                when {
                                        roa >= 10 -> HealthStatus.EXCELLENT
                                        roa >= 5 -> HealthStatus.GOOD
                                        roa >= 2 -> HealthStatus.NEUTRAL
                                        roa >= 0 -> HealthStatus.CAUTION
                                        else -> HealthStatus.WARNING
                                }
                        ratios.add(
                                FinancialRatio(
                                        name = "총자산이익률 (ROA)",
                                        value = roa,
                                        formattedValue = String.format("%.1f%%", roa),
                                        description = "보유 자산으로 얼마나 효율적으로 수익을 창출하는지",
                                        interpretation = getRoaInterpretation(roa),
                                        healthStatus = status,
                                        category = RatioCategory.PROFITABILITY
                                )
                        )
                }

                // ROE (Return on Equity)
                if (netIncome != null && totalEquity != null && totalEquity > 0) {
                        val roe = (netIncome / totalEquity) * 100
                        val status =
                                when {
                                        roe >= 20 -> HealthStatus.EXCELLENT
                                        roe >= 15 -> HealthStatus.GOOD
                                        roe >= 10 -> HealthStatus.NEUTRAL
                                        roe >= 0 -> HealthStatus.CAUTION
                                        else -> HealthStatus.WARNING
                                }
                        ratios.add(
                                FinancialRatio(
                                        name = "자기자본이익률 (ROE)",
                                        value = roe,
                                        formattedValue = String.format("%.1f%%", roe),
                                        description = "주주가 투자한 자본으로 얼마나 수익을 창출하는지",
                                        interpretation = getRoeInterpretation(roe),
                                        healthStatus = status,
                                        category = RatioCategory.PROFITABILITY
                                )
                        )
                }

                // 부채비율 (Debt to Equity Ratio)
                if (totalLiabilities != null && totalEquity != null && totalEquity > 0) {
                        val debtRatio = (totalLiabilities / totalEquity) * 100
                        val status =
                                when {
                                        debtRatio <= 50 -> HealthStatus.EXCELLENT
                                        debtRatio <= 100 -> HealthStatus.GOOD
                                        debtRatio <= 200 -> HealthStatus.NEUTRAL
                                        debtRatio <= 300 -> HealthStatus.CAUTION
                                        else -> HealthStatus.WARNING
                                }
                        ratios.add(
                                FinancialRatio(
                                        name = "부채비율 (Debt to Equity)",
                                        value = debtRatio,
                                        formattedValue = String.format("%.0f%%", debtRatio),
                                        description = "자기자본 대비 부채의 비율",
                                        interpretation = getDebtRatioInterpretation(debtRatio),
                                        healthStatus = status,
                                        category = RatioCategory.SOLVENCY
                                )
                        )
                }

                // 유동비율 (Current Ratio)
                if (currentAssets != null && currentLiabilities != null && currentLiabilities > 0) {
                        val currentRatio = currentAssets / currentLiabilities
                        val status =
                                when {
                                        currentRatio >= 2.0 -> HealthStatus.EXCELLENT
                                        currentRatio >= 1.5 -> HealthStatus.GOOD
                                        currentRatio >= 1.0 -> HealthStatus.NEUTRAL
                                        currentRatio >= 0.5 -> HealthStatus.CAUTION
                                        else -> HealthStatus.WARNING
                                }
                        ratios.add(
                                FinancialRatio(
                                        name = "유동비율 (Current Ratio)",
                                        value = currentRatio,
                                        formattedValue = String.format("%.2f", currentRatio),
                                        description = "단기 부채를 갚을 수 있는 능력",
                                        interpretation =
                                                getCurrentRatioInterpretation(currentRatio),
                                        healthStatus = status,
                                        category = RatioCategory.LIQUIDITY
                                )
                        )
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

        private fun getMarginInterpretation(margin: Double): String =
                when {
                        margin >= 20 -> "🌟 매우 우수합니다! 매출 대비 높은 수익을 창출하고 있어 경쟁력이 뛰어납니다."
                        margin >= 10 -> "👍 양호합니다. 건강한 수익 구조를 유지하고 있습니다."
                        margin >= 5 -> "📊 보통 수준입니다. 업계 평균과 비교해 보세요."
                        margin >= 0 -> "⚠️ 주의가 필요합니다. 수익성 개선이 필요할 수 있습니다."
                        else -> "🚨 적자 상태입니다. 비용 구조 개선이 시급합니다."
                }

        private fun getRoaInterpretation(roa: Double): String =
                when {
                        roa >= 10 -> "🌟 매우 효율적입니다! 자산을 활용해 높은 수익을 창출하고 있습니다."
                        roa >= 5 -> "👍 효율적으로 자산을 운용하고 있습니다."
                        roa >= 2 -> "📊 평균적인 수준입니다."
                        roa >= 0 -> "⚠️ 자산 활용 효율성이 낮습니다."
                        else -> "🚨 자산 대비 손실이 발생하고 있습니다."
                }

        private fun getRoeInterpretation(roe: Double): String =
                when {
                        roe >= 20 -> "🌟 투자자에게 높은 수익을 제공하고 있습니다! 우수한 경영 성과입니다."
                        roe >= 15 -> "👍 양호한 투자 수익률을 보여주고 있습니다."
                        roe >= 10 -> "📊 평균적인 수익률입니다."
                        roe >= 0 -> "⚠️ 투자 수익률이 낮습니다. 개선이 필요할 수 있습니다."
                        else -> "🚨 주주 자본에 손실이 발생하고 있습니다."
                }

        private fun getDebtRatioInterpretation(ratio: Double): String =
                when {
                        ratio <= 50 -> "🌟 매우 안정적입니다! 부채 부담이 적어 재무 위험이 낮습니다."
                        ratio <= 100 -> "👍 건전한 부채 수준입니다."
                        ratio <= 200 -> "📊 보통 수준의 부채입니다. 업계 특성을 고려하세요."
                        ratio <= 300 -> "⚠️ 부채가 다소 높습니다. 금리 상승 시 주의가 필요합니다."
                        else -> "🚨 부채가 과다합니다. 재무 위험이 높을 수 있습니다."
                }

        private fun getCurrentRatioInterpretation(ratio: Double): String =
                when {
                        ratio >= 2.0 -> "🌟 매우 안정적입니다! 단기 부채를 충분히 갚을 수 있습니다."
                        ratio >= 1.5 -> "👍 양호합니다. 단기 유동성에 문제가 없습니다."
                        ratio >= 1.0 -> "📊 최소 기준은 충족합니다. 현금 흐름 관리가 중요합니다."
                        ratio >= 0.5 -> "⚠️ 주의가 필요합니다. 단기 부채 상환에 어려움이 있을 수 있습니다."
                        else -> "🚨 심각한 유동성 위험이 있습니다."
                }

        /** 용어 설명 생성 */
        private fun generateTermExplanations(): List<FinancialTermExplanation> {
                val terms = mutableListOf<FinancialTermExplanation>()

                // 기본 용어들
                terms.add(
                        FinancialTermExplanation(
                                term = "매출 (Revenue)",
                                simpleDefinition = "회사가 제품이나 서비스를 팔아서 받은 총 금액",
                                analogy =
                                        "카페를 운영한다면, 커피를 팔아서 받은 총 금액이 매출입니다. 재료비, 인건비를 빼기 전 금액이에요.",
                                example = "애플이 아이폰을 1억 대 팔아서 1000억 달러를 벌었다면, 그게 애플의 매출입니다."
                        )
                )

                terms.add(
                        FinancialTermExplanation(
                                term = "순이익 (Net Income)",
                                simpleDefinition = "모든 비용을 제외하고 실제로 남은 돈",
                                analogy = "월급 300만원을 받고, 집세·식비·교통비 등을 다 내고 통장에 남은 50만원이 '순이익'입니다.",
                                example = "매출이 100억이어도 비용이 95억이면 순이익은 5억뿐입니다. 매출보다 순이익이 중요해요!"
                        )
                )

                terms.add(
                        FinancialTermExplanation(
                                term = "자산 (Assets)",
                                simpleDefinition = "회사가 소유한 모든 가치 있는 것들",
                                analogy = "개인으로 치면 집, 차, 저금통장, 주식 등 내가 가진 모든 재산입니다.",
                                example = "삼성전자의 자산에는 공장, 특허권, 현금, 재고 상품 등이 포함됩니다."
                        )
                )

                terms.add(
                        FinancialTermExplanation(
                                term = "부채 (Liabilities)",
                                simpleDefinition = "회사가 갚아야 할 모든 빚",
                                analogy = "주택담보대출, 카드 할부금, 친구에게 빌린 돈 등 언젠가 갚아야 할 돈입니다.",
                                example = "은행 대출 50억, 미지급 세금 5억, 공급업체에 줘야 할 돈 10억 = 총 부채 65억"
                        )
                )

                terms.add(
                        FinancialTermExplanation(
                                term = "자기자본 (Equity)",
                                simpleDefinition = "자산에서 부채를 뺀 순수한 회사의 가치 (주주의 몫)",
                                analogy = "5억짜리 집을 사고 대출이 3억이라면, 자기자본은 2억입니다. 이게 진짜 내 재산이에요.",
                                example = "총자산 100억 - 총부채 60억 = 자기자본 40억 (이게 주주들의 몫)"
                        )
                )

                terms.add(
                        FinancialTermExplanation(
                                term = "EPS (주당순이익)",
                                simpleDefinition = "주식 1주당 벌어들인 순이익",
                                analogy =
                                        "피자를 8조각으로 나눴을 때 한 조각의 크기와 같아요. 조각(주식)당 얼마나 맛있는지(수익)를 보여줍니다.",
                                example =
                                        "순이익 100억원 ÷ 발행주식 1억주 = EPS 100원. 내가 1주를 가지면 100원의 이익에 해당하는 권리가 있어요."
                        )
                )

                terms.add(
                        FinancialTermExplanation(
                                term = "SEC (미국 증권거래위원회)",
                                simpleDefinition = "미국 주식시장을 감독하는 정부 기관",
                                analogy = "학교의 교무처장 같은 존재입니다. 회사들이 정직하게 정보를 공개하는지 감시합니다.",
                                example =
                                        "모든 미국 상장기업은 SEC에 재무보고서를 의무적으로 제출해야 합니다. 거짓 정보를 내면 큰 벌을 받아요!"
                        )
                )

                return terms
        }

        /** 재무 건전성 점수 계산 */
        private fun calculateHealthScore(ratios: List<FinancialRatio>): FinancialHealthScore {
                var totalScore = 0
                var count = 0
                val strengths = mutableListOf<String>()
                val weaknesses = mutableListOf<String>()
                val recommendations = mutableListOf<String>()

                for (ratio in ratios) {
                        val score =
                                when (ratio.healthStatus) {
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
                val grade =
                        when {
                                overallScore >= 90 -> "A+"
                                overallScore >= 85 -> "A"
                                overallScore >= 80 -> "B+"
                                overallScore >= 75 -> "B"
                                overallScore >= 70 -> "C+"
                                overallScore >= 60 -> "C"
                                overallScore >= 50 -> "D"
                                else -> "F"
                        }

                val summary =
                        when {
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

        /** 보고서 유형 설명 */
        private fun getReportTypeExplanation(reportType: String?): String? {
                return when (reportType) {
                        "10-K" ->
                                """
                📚 10-K 연간 보고서 (Annual Report)
                
                미국 상장기업이 매년 회계연도 종료 후 60~90일 이내에 SEC에 제출하는 가장 포괄적인 재무 보고서입니다.
                
                🔍 주요 섹션:
                • Part I - 사업 개요 (Business): 회사가 무슨 일을 하는지
                • Part I - 위험 요소 (Risk Factors): 투자 위험 요인
                • Part II - MD&A: 경영진이 설명하는 재무 상황
                • Part II - 재무제표: 숫자로 된 성적표
                
                💡 팁: 처음이라면 'Business'와 'Risk Factors'부터 읽어보세요!
            """.trimIndent()
                        "10-Q" ->
                                """
                📊 10-Q 분기 보고서 (Quarterly Report)
                
                매 분기(3개월)마다 제출하는 보고서입니다. 10-K보다 간략하지만 최신 상황을 파악할 수 있습니다.
                
                🔍 특징:
                • 감사받지 않은 재무제표 (검토만 받음)
                • 분기별 실적 비교 가능
                • 10-K 이후 변동사항 확인
                
                💡 팁: 전 분기, 전년 동기와 비교하면서 읽으면 트렌드를 파악할 수 있어요!
            """.trimIndent()
                        "8-K" ->
                                """
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
                        "20-F" ->
                                """
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

        /** 핵심 요점 생성 */
        private fun generateKeyTakeaways(
                analysis: FinancialAnalysis,
                ratios: List<FinancialRatio>,
                healthScore: FinancialHealthScore
        ): List<String> {
                val takeaways = mutableListOf<String>()

                takeaways.add(
                        "📊 재무 건전성 점수: ${healthScore.grade} (${healthScore.overallScore}점/100점)"
                )

                analysis.companyName?.let { takeaways.add("🏢 분석 대상: $it") }

                analysis.reportType?.let { takeaways.add("📋 보고서 유형: SEC Form $it") }

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
