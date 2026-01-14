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
 * This parser delegates specialized tasks to helper objects:
 * - FinancialStatementParser
 * - RiskFactorAnalyzer
 * - FinancialRatioCalculator
 * - ParsingHelpers
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
    fun inferCategoryFromLabel(label: String): MetricCategory? =
            ParsingHelpers.inferCategoryFromLabel(label)

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
        // Delegated to FinancialRatioCalculator (Phase 5)
        return FinancialRatioCalculator.calculateRatios(metrics)
    }

    // Helper functions - delegated to ParsingHelpers (Phase 2a)
    fun cleanHtml(content: String): String = ParsingHelpers.cleanHtml(content)

    fun detectUnit(text: String): MetricUnit = ParsingHelpers.detectUnit(text)

    fun detectPeriod(text: String): String? = ParsingHelpers.detectPeriod(text)

    fun detectPeriodType(text: String): PeriodType? = ParsingHelpers.detectPeriodType(text)

    fun searchMetricValues(
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

    fun parseNumber(value: String, unit: MetricUnit, isNegative: Boolean = false): BigDecimal? =
            ParsingHelpers.parseNumber(value, unit, isNegative)

    fun formatValue(value: BigDecimal): String = ParsingHelpers.formatValue(value)

    private fun extractSection(text: String, sectionNames: List<String>): String? =
            ParsingHelpers.extractSection(text, sectionNames)

    fun deduplicateMetrics(metrics: List<ExtendedFinancialMetric>): List<ExtendedFinancialMetric> =
            ParsingHelpers.deduplicateMetrics(metrics)
}
