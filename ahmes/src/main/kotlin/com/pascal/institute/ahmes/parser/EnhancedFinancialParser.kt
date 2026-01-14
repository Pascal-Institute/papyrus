package com.pascal.institute.ahmes.parser

import com.pascal.institute.ahmes.model.*
import com.pascal.institute.ahmes.util.*
import java.math.BigDecimal
import java.math.RoundingMode
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Enhanced Financial Parser
 *
 * Provides comprehensive parsing and extraction of financial data from SEC documents. This parser
 * combines multiple strategies including:
 * - Regex-based pattern matching for financial metrics
 * - Table structure parsing with column alignment detection
 * - XBRL inline tag extraction
 * - Risk factor categorization and severity assessment
 * - Automatic financial ratio calculation
 *
 * ## Features
 *
 * ### Financial Metric Extraction
 * - Revenue, profit, and loss metrics
 * - Balance sheet items (assets, liabilities, equity)
 * - Cash flow statement components
 * - Per-share values (EPS, book value)
 * - Segment-specific metrics
 *
 * ### Data Quality
 * - All monetary values use [BigDecimal] for precision
 * - Automatic unit detection (thousands, millions, billions)
 * - Period detection (annual, quarterly, year-to-date)
 * - Confidence scoring for extracted metrics
 * - Duplicate metric detection and removal
 *
 * ### Financial Ratios
 * - Profitability: Gross margin, net margin, ROE, ROA
 * - Liquidity: Current ratio, quick ratio
 * - Leverage: Debt-to-equity, debt-to-assets
 * - Efficiency: Asset turnover, inventory turnover
 *
 * ## Usage Examples
 *
 * ### Basic Metric Extraction
 * ```kotlin
 * val content = File("10-K.html").readText()
 * val metrics = EnhancedFinancialParser.parseFinancialMetrics(content)
 *
 * metrics.forEach { metric ->
 *     println("${metric.name}: ${metric.value}")
 *     println("  Category: ${metric.category}")
 *     println("  Period: ${metric.period}")
 * }
 * ```
 *
 * ### Financial Statement Parsing
 * ```kotlin
 * val statements = EnhancedFinancialParser.parseFinancialStatements(content)
 *
 * statements.forEach { statement ->
 *     println("Statement Type: ${statement.type}")
 *     println("Period: ${statement.period}")
 *     statement.items.forEach { item ->
 *         println("  ${item.label}: ${item.value}")
 *     }
 * }
 * ```
 *
 * ### Ratio Calculation
 * ```kotlin
 * val metrics = EnhancedFinancialParser.parseFinancialMetrics(content)
 * val ratios = EnhancedFinancialParser.calculateRatios(metrics)
 *
 * ratios.filter { it.category == RatioCategory.PROFITABILITY }
 *     .forEach { ratio ->
 *         println("${ratio.name}: ${ratio.displayValue}")
 *         println("  Health: ${ratio.health}")
 *         println("  ${ratio.interpretation}")
 *     }
 * ```
 *
 * ### Risk Factor Analysis
 * ```kotlin
 * val riskFactors = EnhancedFinancialParser.parseRiskFactors(content)
 *
 * riskFactors.groupBy { it.severity }
 *     .forEach { (severity, risks) ->
 *         println("$severity: ${risks.size} risks")
 *         risks.forEach { risk ->
 *             println("  - ${risk.title} (${risk.category})")
 *         }
 *     }
 * ```
 *
 * ## Performance Considerations
 *
 * - Pre-compile regex patterns for repeated parsing
 * - Use streaming for documents > 10MB
 * - Cache parse results when processing same document multiple times
 * - Consider parallel processing for batch operations
 *
 * ## Thread Safety
 *
 * This singleton object is thread-safe and can be used concurrently from multiple threads without
 * synchronization.
 *
 * @see ExtendedFinancialMetric
 * @see FinancialStatement
 * @see FinancialRatio
 * @see RiskFactor
 */
object EnhancedFinancialParser {

    /** SEC PDF document parsing - uses PdfParser's SecDocumentText */
    fun parseFromSecDocument(secDoc: SecDocumentText): List<ExtendedFinancialMetric> {
        logger.info {
            "Starting SEC document parsing: company=${secDoc.companyName}, fiscalYear=${secDoc.fiscalYear}, sections=${secDoc.sections.size}"
        }

        val metrics = mutableListOf<ExtendedFinancialMetric>()

        for (section in secDoc.sections) {
            logger.debug {
                "Parsing section: type=${section.type}, contentLength=${section.content.length}"
            }

            val sectionMetrics =
                    if (section.type in
                                    listOf(
                                            SecSectionType.INCOME_STATEMENT,
                                            SecSectionType.BALANCE_SHEET,
                                            SecSectionType.CASH_FLOW
                                    )
                    ) {
                        parseFinancialMetrics(section.content)
                    } else {
                        emptyList()
                    }

            logger.debug { "Section ${section.type} extracted ${sectionMetrics.size} metrics" }
            metrics.addAll(sectionMetrics)
        }

        val foundCategories = metrics.map { it.category }.toSet()
        logger.debug { "Found categories from sections: $foundCategories" }

        val additionalMetrics =
                parseFinancialMetrics(secDoc.fullText).filter { it.category !in foundCategories }

        logger.debug { "Added ${additionalMetrics.size} additional metrics from full text" }
        metrics.addAll(additionalMetrics)

        val result =
                metrics.map { metric ->
                    metric.copy(
                            context =
                                    "${secDoc.companyName} - ${secDoc.fiscalYear ?: ""} ${metric.context ?: ""}".trim()
                    )
                }

        logger.info { "SEC document parsing complete: extracted ${result.size} total metrics" }
        return result
    }

    /** PDF text-based table parsing with column alignment recognition */
    fun parsePdfTextTable(text: String): List<ExtendedFinancialMetric> {
        val metrics = mutableListOf<ExtendedFinancialMetric>()
        val lines = text.split("\n")

        val unit = detectUnit(text)

        val yearPattern = Regex("""20\d{2}""")
        val headerLine = lines.take(20).find { line -> yearPattern.findAll(line).count() >= 2 }
        val years =
                headerLine?.let { yearPattern.findAll(it).map { m -> m.value }.toList() }
                        ?: emptyList()

        val numberPattern = Regex("""\(?\$?\s*[\d,]+(?:\.\d+)?\)?""")

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.length < 10) continue

            val numbers = numberPattern.findAll(trimmedLine).toList()
            if (numbers.isEmpty()) continue

            val firstNumberStart = numbers.first().range.first
            val label = trimmedLine.substring(0, firstNumberStart).trim()

            if (!isValidLabel(label)) continue

            val currentValueStr = numbers.first().value
            val currentValue = parseSecValue(currentValueStr, unit) ?: continue

            val priorValue =
                    if (numbers.size >= 2) {
                        parseSecValue(numbers[1].value, unit)
                    } else null

            val category = inferCategoryFromLabel(label) ?: continue

            val yoyChange =
                    if (priorValue != null && priorValue != BigDecimal.ZERO) {
                        currentValue
                                .subtract(priorValue)
                                .divide(priorValue.abs(), 10, RoundingMode.HALF_UP)
                                .multiply(BigDecimal("100"))
                                .setScale(2, RoundingMode.HALF_UP)
                    } else null

            metrics.add(
                    ExtendedFinancialMetric(
                            name = label,
                            value = formatValue(currentValue),
                            rawValue = currentValue.setScale(2, RoundingMode.HALF_UP).toString(),
                            unit = unit,
                            period = years.firstOrNull(),
                            category = category,
                            source = "PDF Text Table Parser",
                            confidence = if (label.lowercase().startsWith("total")) 0.95 else 0.85,
                            yearOverYearChange = yoyChange?.toString()
                    )
            )
        }

        return deduplicateMetrics(metrics)
    }

    private fun isValidLabel(label: String): Boolean {
        if (label.length < 3) return false
        if (label.all { it.isDigit() || it.isWhitespace() || it == ',' || it == '.' || it == '$' })
                return false
        if (label.contains("Page ") || label.matches(Regex("""F-\d+.*"""))) return false
        if (label.matches(Regex("""^\d+.*"""))) return false
        if (label.contains("---")) return false
        return true
    }

    private fun parseSecValue(valueStr: String, unit: MetricUnit): BigDecimal? {
        val unitStr =
                when (unit) {
                    MetricUnit.BILLIONS -> "billions"
                    MetricUnit.MILLIONS -> "millions"
                    MetricUnit.THOUSANDS -> "thousands"
                    else -> "dollars"
                }

        val monetaryAmount = FinancialPrecision.parseSecValue(valueStr, unitStr, "USD")
        return monetaryAmount?.number?.numberValue(BigDecimal::class.java)
    }

    // Delegated to ParsingHelpers (Phase 3)
    internal fun inferCategoryFromLabel(label: String): MetricCategory? =
            ParsingHelpers.inferCategoryFromLabel(label)

    // Section parsers removed - logic moved to FinancialStatementParser

    // isValidLabel removed - logic moved to FinancialStatementParser
    private val allPatterns = allFinancialMetricPatterns

    /** Parse all financial metrics from document content */
    fun parseFinancialMetrics(content: String): List<ExtendedFinancialMetric> {
        logger.info { "Starting financial metrics parsing: contentLength=${content.length}" }

        val metrics = mutableListOf<ExtendedFinancialMetric>()

        // Step 1: Table-based parsing
        try {
            logger.debug { "Attempting table-based parsing" }
            val tables = SecTableParser.parseFinancialTables(content)
            val tableMetrics = SecTableParser.convertToMetrics(tables)
            logger.debug {
                "Table parsing extracted ${tableMetrics.size} metrics from ${tables.size} tables"
            }
            metrics.addAll(tableMetrics)
        } catch (e: Exception) {
            logger.warn(e) { "Table parsing failed, falling back to text parsing" }
        }

        // Step 2: Text pattern-based parsing
        val cleanText = cleanHtml(content)
        val unit = detectUnit(cleanText)
        val period = detectPeriod(cleanText)
        val periodType = detectPeriodType(cleanText)

        logger.debug { "Detected: unit=$unit, period=$period, periodType=$periodType" }

        val foundCategories = metrics.map { it.category }.toSet()
        logger.debug {
            "Searching for ${allPatterns.size} patterns (skipping ${foundCategories.size} already found)"
        }

        for (pattern in allPatterns) {
            if (pattern.category in foundCategories) continue

            val found =
                    searchMetricValues(
                            cleanText,
                            pattern.term,
                            pattern.category,
                            unit,
                            period,
                            periodType,
                            pattern.confidence
                    )
            if (found.isNotEmpty()) {
                logger.trace { "Pattern '${pattern.term}' matched ${found.size} metrics" }
            }
            metrics.addAll(found)
        }

        val result = deduplicateMetrics(metrics)
        logger.info {
            "Financial metrics parsing complete: ${result.size} unique metrics (${metrics.size} before deduplication)"
        }
        return result
    }

    /** Parse financial statements from document */
    fun parseFinancialStatements(content: String): List<FinancialStatement> {
        // Delegated to FinancialStatementParser (Phase 3)
        return FinancialStatementParser.parseFinancialStatements(content)
    }

    /** Parse risk factors from SEC document */
    fun parseRiskFactors(content: String): List<RiskFactor> {
        // Delegated to RiskFactorAnalyzer (Phase 4)
        return RiskFactorAnalyzer.parseRiskFactors(content)
    }

    /** Calculate financial ratios from metrics */
    fun calculateRatios(metrics: List<ExtendedFinancialMetric>): List<FinancialRatio> {
        logger.info { "Starting ratio calculation: ${metrics.size} input metrics" }

        val ratios = mutableListOf<FinancialRatio>()

        fun getValue(category: MetricCategory): Double? {
            return metrics.find { it.category == category }?.getRawValueBigDecimal()?.toDouble()
        }

        val revenue = getValue(MetricCategory.REVENUE)
        val grossProfit = getValue(MetricCategory.GROSS_PROFIT)
        val operatingIncome = getValue(MetricCategory.OPERATING_INCOME)
        val netIncome = getValue(MetricCategory.NET_INCOME)
        val totalAssets = getValue(MetricCategory.TOTAL_ASSETS)
        val totalLiabilities = getValue(MetricCategory.TOTAL_LIABILITIES)
        val totalEquity = getValue(MetricCategory.TOTAL_EQUITY)
        val currentAssets = getValue(MetricCategory.CURRENT_ASSETS)
        val currentLiabilities = getValue(MetricCategory.CURRENT_LIABILITIES)
        val cash = getValue(MetricCategory.CASH_AND_EQUIVALENTS)
        val inventory = getValue(MetricCategory.INVENTORY)

        // Gross Margin
        if (grossProfit != null && revenue != null && revenue > 0) {
            val ratio = (grossProfit / revenue) * 100
            if (ratio <= 100) {
                ratios.add(
                        createRatio(
                                "Gross Margin",
                                ratio,
                                "%",
                                RatioCategory.PROFITABILITY,
                                assessProfitabilityHealth(ratio, 30.0, 50.0)
                        )
                )
            }
        }

        // Operating Margin
        if (operatingIncome != null && revenue != null && revenue > 0) {
            val ratio = (operatingIncome / revenue) * 100
            if (ratio <= 100) {
                ratios.add(
                        createRatio(
                                "Operating Margin",
                                ratio,
                                "%",
                                RatioCategory.PROFITABILITY,
                                assessProfitabilityHealth(ratio, 10.0, 20.0)
                        )
                )
            }
        }

        // Net Profit Margin
        if (netIncome != null && revenue != null && revenue > 0) {
            val ratio = (netIncome / revenue) * 100
            if (ratio <= 100) {
                ratios.add(
                        createRatio(
                                "Net Profit Margin",
                                ratio,
                                "%",
                                RatioCategory.PROFITABILITY,
                                assessProfitabilityHealth(ratio, 5.0, 15.0)
                        )
                )
            }
        }

        // ROA
        if (netIncome != null && totalAssets != null && totalAssets > 0) {
            val ratio = (netIncome / totalAssets) * 100
            ratios.add(
                    createRatio(
                            "ROA",
                            ratio,
                            "%",
                            RatioCategory.PROFITABILITY,
                            assessProfitabilityHealth(ratio, 2.0, 8.0)
                    )
            )
        }

        // ROE
        if (netIncome != null && totalEquity != null && totalEquity > 0) {
            val ratio = (netIncome / totalEquity) * 100
            ratios.add(
                    createRatio(
                            "ROE",
                            ratio,
                            "%",
                            RatioCategory.PROFITABILITY,
                            assessProfitabilityHealth(ratio, 10.0, 20.0)
                    )
            )
        }

        // Current Ratio
        if (currentAssets != null && currentLiabilities != null && currentLiabilities > 0) {
            val ratio = currentAssets / currentLiabilities
            ratios.add(
                    createRatio(
                            "Current Ratio",
                            ratio,
                            "x",
                            RatioCategory.LIQUIDITY,
                            assessLiquidityHealth(ratio, 1.0, 2.0)
                    )
            )
        }

        // Quick Ratio
        if (currentAssets != null &&
                        inventory != null &&
                        currentLiabilities != null &&
                        currentLiabilities > 0
        ) {
            val ratio = (currentAssets - inventory) / currentLiabilities
            ratios.add(
                    createRatio(
                            "Quick Ratio",
                            ratio,
                            "x",
                            RatioCategory.LIQUIDITY,
                            assessLiquidityHealth(ratio, 0.8, 1.5)
                    )
            )
        }

        // Debt to Equity
        if (totalLiabilities != null && totalEquity != null && totalEquity > 0) {
            val ratio = (totalLiabilities / totalEquity) * 100
            ratios.add(
                    createRatio(
                            "Debt to Equity",
                            ratio,
                            "%",
                            RatioCategory.SOLVENCY,
                            assessDebtHealth(ratio, 100.0, 200.0)
                    )
            )
        }

        // Cash Ratio
        if (cash != null && currentLiabilities != null && currentLiabilities > 0) {
            val ratio = cash / currentLiabilities
            ratios.add(
                    createRatio(
                            "Cash Ratio",
                            ratio,
                            "x",
                            RatioCategory.LIQUIDITY,
                            assessLiquidityHealth(ratio, 0.2, 0.5)
                    )
            )
        }

        // Asset Turnover
        if (revenue != null && totalAssets != null && totalAssets > 0) {
            val ratio = revenue / totalAssets
            ratios.add(
                    createRatio(
                            "Asset Turnover",
                            ratio,
                            "x",
                            RatioCategory.EFFICIENCY,
                            assessEfficiencyHealth(ratio, 0.5, 1.5)
                    )
            )
        }

        logger.info { "Ratio calculation complete: ${ratios.size} ratios calculated" }
        logger.debug {
            "Ratio categories: ${ratios.groupBy { it.category }.mapValues { it.value.size }}"
        }
        return ratios
    }

    // Helper functions - delegated to ParsingHelpers (Phase 2a)
    internal fun cleanHtml(content: String): String = ParsingHelpers.cleanHtml(content)

    internal fun detectUnit(text: String): MetricUnit = ParsingHelpers.detectUnit(text)

    internal fun detectPeriod(text: String): String? = ParsingHelpers.detectPeriod(text)

    internal fun detectPeriodType(text: String): PeriodType? = ParsingHelpers.detectPeriodType(text)

    internal fun searchMetricValues(
            text: String,
            term: String,
            category: MetricCategory,
            unit: MetricUnit,
            period: String?,
            periodType: PeriodType?,
            baseConfidence: Double
    ): List<ExtendedFinancialMetric> {
        val results = mutableListOf<ExtendedFinancialMetric>()

        val patterns =
                listOf(
                        Regex(
                                "(?i)${Regex.escape(term)}[:\\s\\|]*\\(?\\$?\\s*([\\d,]+(?:\\.\\d+)?)\\)?",
                                RegexOption.IGNORE_CASE
                        ),
                        Regex(
                                "(?i)${Regex.escape(term)}\\s*\\|\\s*\\$?\\s*\\(?([\\d,]+(?:\\.\\d+)?)\\)?",
                                RegexOption.IGNORE_CASE
                        ),
                        Regex(
                                "(?i)${Regex.escape(term)}[:\\s]*\\(\\$?\\s*([\\d,]+(?:\\.\\d+)?)\\)",
                                RegexOption.IGNORE_CASE
                        )
                )

        for (pattern in patterns) {
            val matches = pattern.findAll(text)
            for ((index, match) in matches.take(5).withIndex()) {
                val valueStr = match.groupValues.getOrNull(1) ?: continue

                val context =
                        text.substring(
                                maxOf(0, match.range.first - 100),
                                minOf(text.length, match.range.last + 100)
                        )

                val isNegative =
                        match.value.trim().startsWith("(") && match.value.trim().endsWith(")")
                val rawValue = parseNumber(valueStr, unit, isNegative)

                if (rawValue != null && rawValue.abs() >= BigDecimal("1000")) {
                    results.add(
                            ExtendedFinancialMetric(
                                    name = term,
                                    value = formatValue(rawValue),
                                    rawValue = rawValue.toString(),
                                    unit = unit,
                                    period = period,
                                    periodType = periodType,
                                    category = category,
                                    source = "Enhanced document extraction",
                                    confidence = baseConfidence * (1.0 - index * 0.08),
                                    context = context.trim()
                            )
                    )
                }
            }
        }

        return results.distinctBy { it.rawValue }
    }

    internal fun parseNumber(
            value: String,
            unit: MetricUnit,
            isNegative: Boolean = false
    ): BigDecimal? = ParsingHelpers.parseNumber(value, unit, isNegative)

    internal fun formatValue(value: BigDecimal): String = ParsingHelpers.formatValue(value)

    private fun extractSection(text: String, sectionNames: List<String>): String? =
            ParsingHelpers.extractSection(text, sectionNames)

    internal fun deduplicateMetrics(
            metrics: List<ExtendedFinancialMetric>
    ): List<ExtendedFinancialMetric> = ParsingHelpers.deduplicateMetrics(metrics)

    // Risk classification methods moved to RiskFactorAnalyzer (Phase 4)

    private fun createRatio(
            name: String,
            value: Double,
            suffix: String,
            category: RatioCategory,
            health: HealthStatus
    ): FinancialRatio {
        val formatted =
                when (suffix) {
                    "%" -> String.format("%.1f%%", value)
                    "x" -> String.format("%.2fx", value)
                    else -> String.format("%.2f", value)
                }

        return FinancialRatio(
                name = name,
                value = value.toString(),
                formattedValue = formatted,
                description = getDescription(name),
                interpretation = getInterpretation(name, health),
                healthStatus = health,
                category = category
        )
    }

    private fun getDescription(name: String): String {
        return when (name) {
            "Gross Margin" -> "Revenue minus cost of revenue as a percentage of revenue"
            "Operating Margin" -> "Operating income as a percentage of revenue"
            "Net Profit Margin" -> "Net income as a percentage of revenue"
            "ROA" -> "Return on Assets - Net income as a percentage of total assets"
            "ROE" -> "Return on Equity - Net income as a percentage of shareholders' equity"
            "Current Ratio" -> "Current assets divided by current liabilities"
            "Quick Ratio" -> "Liquid assets divided by current liabilities"
            "Debt to Equity" -> "Total liabilities divided by total equity"
            "Cash Ratio" -> "Cash and equivalents divided by current liabilities"
            "Asset Turnover" -> "Revenue divided by total assets"
            else -> ""
        }
    }

    private fun getInterpretation(name: String, health: HealthStatus): String {
        return when (health) {
            HealthStatus.EXCELLENT -> "$name is excellent"
            HealthStatus.GOOD -> "$name is good"
            HealthStatus.NEUTRAL -> "$name is at average level"
            HealthStatus.CAUTION -> "$name needs attention"
            HealthStatus.WARNING -> "$name is at risk level"
        }
    }

    private fun assessProfitabilityHealth(
            value: Double,
            cautionThreshold: Double,
            goodThreshold: Double
    ): HealthStatus {
        return when {
            value >= goodThreshold * 1.5 -> HealthStatus.EXCELLENT
            value >= goodThreshold -> HealthStatus.GOOD
            value >= cautionThreshold -> HealthStatus.NEUTRAL
            value >= 0 -> HealthStatus.CAUTION
            else -> HealthStatus.WARNING
        }
    }

    private fun assessLiquidityHealth(
            value: Double,
            cautionThreshold: Double,
            goodThreshold: Double
    ): HealthStatus {
        return when {
            value >= goodThreshold * 1.5 -> HealthStatus.EXCELLENT
            value >= goodThreshold -> HealthStatus.GOOD
            value >= cautionThreshold -> HealthStatus.NEUTRAL
            value >= 1.0 -> HealthStatus.CAUTION
            else -> HealthStatus.WARNING
        }
    }

    private fun assessDebtHealth(
            value: Double,
            goodThreshold: Double,
            cautionThreshold: Double
    ): HealthStatus {
        return when {
            value <= goodThreshold -> HealthStatus.EXCELLENT
            value <= (goodThreshold + cautionThreshold) / 2 -> HealthStatus.GOOD
            value <= cautionThreshold -> HealthStatus.NEUTRAL
            value <= cautionThreshold * 1.5 -> HealthStatus.CAUTION
            else -> HealthStatus.WARNING
        }
    }

    private fun assessEfficiencyHealth(
            value: Double,
            cautionThreshold: Double,
            goodThreshold: Double
    ): HealthStatus {
        return when {
            value >= goodThreshold * 1.5 -> HealthStatus.EXCELLENT
            value >= goodThreshold -> HealthStatus.GOOD
            value >= cautionThreshold -> HealthStatus.NEUTRAL
            value >= cautionThreshold * 0.5 -> HealthStatus.CAUTION
            else -> HealthStatus.WARNING
        }
    }
}
