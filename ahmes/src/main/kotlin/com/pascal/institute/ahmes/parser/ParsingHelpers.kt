package com.pascal.institute.ahmes.parser

import com.pascal.institute.ahmes.model.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Parsing Helpers
 *
 * Utility functions for financial document parsing. Extracted from EnhancedFinancialParser
 * (AGENTS.md Principle #12: Seek the Essence)
 *
 * Phase 2a of refactoring plan - separate helper functions for better organization.
 */
object ParsingHelpers {

    /** Clean HTML content for parsing Removes scripts, styles, XBRL tags, and normalizes text */
    fun cleanHtml(content: String): String {
        var cleaned = content

        // Remove scripts, styles, and headers
        cleaned =
                cleaned.replace(
                        Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL),
                        " "
                )
        cleaned =
                cleaned.replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), " ")
        cleaned = cleaned.replace(Regex("<head[^>]*>.*?</head>", RegexOption.DOT_MATCHES_ALL), " ")
        cleaned =
                cleaned.replace(
                        Regex("<ix:header[^>]*>.*?</ix:header>", RegexOption.DOT_MATCHES_ALL),
                        " "
                )

        // Remove XBRL tags
        cleaned = cleaned.replace(Regex("</?ix:[^>]*>", RegexOption.IGNORE_CASE), " ")
        cleaned = cleaned.replace(Regex("</?us-gaap:[^>]*>", RegexOption.IGNORE_CASE), " ")
        cleaned = cleaned.replace(Regex("</?dei:[^>]*>", RegexOption.IGNORE_CASE), " ")

        // Convert tables to text format
        cleaned = cleaned.replace(Regex("<tr[^>]*>", RegexOption.IGNORE_CASE), "\n| ")
        cleaned = cleaned.replace(Regex("<td[^>]*>|<th[^>]*>", RegexOption.IGNORE_CASE), " | ")
        cleaned = cleaned.replace(Regex("</td>|</th>", RegexOption.IGNORE_CASE), " ")
        cleaned = cleaned.replace(Regex("</tr>", RegexOption.IGNORE_CASE), " |\n")

        // Handle line breaks and paragraphs
        cleaned = cleaned.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        cleaned = cleaned.replace(Regex("<p[^>]*>", RegexOption.IGNORE_CASE), "\n\n")
        cleaned = cleaned.replace(Regex("<div[^>]*>", RegexOption.IGNORE_CASE), "\n")

        // Remove all remaining HTML tags
        cleaned = cleaned.replace(Regex("<[^>]*>"), " ")

        // Decode entities and normalize whitespace
        cleaned = SecTextNormalization.decodeBasicEntities(cleaned)
        cleaned = SecTextNormalization.normalizeWhitespacePreserveNewlines(cleaned)

        return cleaned
    }

    /** Detect unit from document text (millions, billions, etc.) */
    fun detectUnit(text: String): MetricUnit {
        val lowerText = text.lowercase()
        return when {
            lowerText.contains("in billions") || lowerText.contains("(in billions)") ->
                    MetricUnit.BILLIONS
            lowerText.contains("in millions") || lowerText.contains("(in millions)") ->
                    MetricUnit.MILLIONS
            lowerText.contains("in thousands") || lowerText.contains("(in thousands)") ->
                    MetricUnit.THOUSANDS
            else -> MetricUnit.MILLIONS
        }
    }

    /** Detect reporting period from document text */
    fun detectPeriod(text: String): String? {
        val patterns =
                listOf(
                        Regex(
                                "(?i)(?:For the |Quarter Ended |Year Ended |Period Ended )([A-Za-z]+\\s+\\d{1,2},?\\s+\\d{4})"
                        ),
                        Regex("(?i)(Q[1-4]\\s+\\d{4})"),
                        Regex("(?i)(FY\\s*\\d{4})")
                )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) return match.groupValues[1].trim()
        }
        return null
    }

    /** Detect period type (quarterly, annual, etc.) */
    fun detectPeriodType(text: String): PeriodType? {
        val lowerText = text.lowercase()
        return when {
            lowerText.contains("three months") || lowerText.contains("quarterly") ->
                    PeriodType.QUARTERLY
            lowerText.contains("twelve months") ||
                    lowerText.contains("annual") ||
                    lowerText.contains("fiscal year") -> PeriodType.ANNUAL
            lowerText.contains("nine months") || lowerText.contains("six months") -> PeriodType.YTD
            else -> null
        }
    }

    /**
     * Parse numeric value with unit scaling
     *
     * AGENTS.md Principle #4: Uses BigDecimal for precision
     */
    fun parseNumber(value: String, unit: MetricUnit, isNegative: Boolean = false): BigDecimal? {
        return try {
            val cleanValue = value.replace(",", "").replace("$", "").trim()
            val baseValue = BigDecimal(cleanValue)

            val scaledValue =
                    when (unit) {
                        MetricUnit.BILLIONS -> baseValue.multiply(BigDecimal("1000000000"))
                        MetricUnit.MILLIONS -> baseValue.multiply(BigDecimal("1000000"))
                        MetricUnit.THOUSANDS -> baseValue.multiply(BigDecimal("1000"))
                        else -> baseValue
                    }

            if (isNegative) scaledValue.negate() else scaledValue
        } catch (e: NumberFormatException) {
            null
        }
    }

    /** Format BigDecimal value for display */
    fun formatValue(value: BigDecimal): String {
        val absValue = value.abs()
        val prefix = if (value < BigDecimal.ZERO) "-" else ""

        return when {
            absValue >= BigDecimal("1000000000") ->
                    "${prefix}$${absValue.divide(BigDecimal("1000000000"), 2, RoundingMode.HALF_UP)}B"
            absValue >= BigDecimal("1000000") ->
                    "${prefix}$${absValue.divide(BigDecimal("1000000"), 2, RoundingMode.HALF_UP)}M"
            absValue >= BigDecimal("1000") ->
                    "${prefix}$${absValue.divide(BigDecimal("1000"), 2, RoundingMode.HALF_UP)}K"
            else -> "${prefix}$${absValue.setScale(2, RoundingMode.HALF_UP)}"
        }
    }

    /** Extract section from document text */
    fun extractSection(text: String, sectionNames: List<String>): String? {
        for (name in sectionNames) {
            val startPattern = Regex("(?i)$name")
            val startMatch = startPattern.find(text) ?: continue

            val endPatterns = listOf("CONSOLIDATED STATEMENTS", "NOTES TO", "Item \\d+", "PART II")

            var endIndex = text.length
            for (endPattern in endPatterns) {
                val endMatch = Regex("(?i)$endPattern").find(text, startMatch.range.last)
                if (endMatch != null && endMatch.range.first > startMatch.range.last + 100) {
                    endIndex = minOf(endIndex, endMatch.range.first)
                }
            }

            val section =
                    text.substring(
                            startMatch.range.first,
                            minOf(endIndex, startMatch.range.first + 15000)
                    )
            if (section.length > 200) return section
        }
        return null
    }

    /** Infer metric category from a text label Used by table parsers to identify rows */
    fun inferCategoryFromLabel(label: String): MetricCategory? {
        val lowerLabel = label.lowercase().trim()

        return when {
            lowerLabel == "revenue" ||
                    lowerLabel == "total revenue" ||
                    lowerLabel == "sales" ||
                    lowerLabel == "net sales" ||
                    lowerLabel == "net revenue" -> MetricCategory.REVENUE
            lowerLabel.contains("products") && lowerLabel.contains("net sales") ->
                    MetricCategory.PRODUCT_REVENUE
            lowerLabel.contains("services") &&
                    (lowerLabel.contains("revenue") || lowerLabel.contains("sales")) ->
                    MetricCategory.SERVICE_REVENUE
            lowerLabel.matches(Regex(".*cost.*(?:revenue|sales|goods).*")) ->
                    MetricCategory.COST_OF_REVENUE
            lowerLabel == "cogs" -> MetricCategory.COST_OF_REVENUE
            lowerLabel == "gross profit" || lowerLabel == "gross margin" ->
                    MetricCategory.GROSS_PROFIT
            lowerLabel.contains("operating income") -> MetricCategory.OPERATING_INCOME
            lowerLabel.contains("income from operations") -> MetricCategory.OPERATING_INCOME
            lowerLabel == "operating profit" -> MetricCategory.OPERATING_INCOME
            lowerLabel.matches(Regex(".*net income.*")) -> MetricCategory.NET_INCOME
            lowerLabel == "net earnings" || lowerLabel == "net profit" -> MetricCategory.NET_INCOME
            lowerLabel.contains("net loss") -> MetricCategory.NET_INCOME
            lowerLabel.contains("ebitda") -> MetricCategory.EBITDA
            lowerLabel == "total assets" -> MetricCategory.TOTAL_ASSETS
            lowerLabel.matches(Regex(".*total.*current.*assets.*")) -> MetricCategory.CURRENT_ASSETS
            lowerLabel.matches(Regex(".*current.*assets.*total.*")) -> MetricCategory.CURRENT_ASSETS
            lowerLabel.contains("cash and cash equivalents") -> MetricCategory.CASH_AND_EQUIVALENTS
            lowerLabel == "cash" -> MetricCategory.CASH_AND_EQUIVALENTS
            lowerLabel.contains("accounts receivable") -> MetricCategory.ACCOUNTS_RECEIVABLE
            lowerLabel.contains("inventories") || lowerLabel == "inventory" ->
                    MetricCategory.INVENTORY
            lowerLabel.contains("marketable securities") -> MetricCategory.MARKETABLE_SECURITIES
            lowerLabel.contains("property") && lowerLabel.contains("equipment") ->
                    MetricCategory.FIXED_ASSETS
            lowerLabel == "total liabilities" -> MetricCategory.TOTAL_LIABILITIES
            lowerLabel.matches(Regex(".*total.*current.*liabilities.*")) ->
                    MetricCategory.CURRENT_LIABILITIES
            lowerLabel.contains("accounts payable") -> MetricCategory.ACCOUNTS_PAYABLE
            lowerLabel.matches(Regex(".*long.*term.*debt.*")) -> MetricCategory.LONG_TERM_DEBT
            lowerLabel.contains("term debt") -> MetricCategory.LONG_TERM_DEBT
            lowerLabel.contains("deferred revenue") -> MetricCategory.DEFERRED_REVENUE
            lowerLabel.matches(Regex(".*total.*(?:equity|stockholders|shareholders).*")) ->
                    MetricCategory.TOTAL_EQUITY
            lowerLabel.matches(Regex(".*(?:stockholders|shareholders).*equity.*")) ->
                    MetricCategory.TOTAL_EQUITY
            lowerLabel.contains("retained earnings") -> MetricCategory.RETAINED_EARNINGS
            lowerLabel.contains("accumulated deficit") -> MetricCategory.RETAINED_EARNINGS
            lowerLabel.matches(
                    Regex(".*(?:net )?cash.*(?:provided|generated|used).*operating.*")
            ) -> MetricCategory.OPERATING_CASH_FLOW
            lowerLabel.matches(Regex(".*operating.*(?:cash flow|activities).*")) ->
                    MetricCategory.OPERATING_CASH_FLOW
            lowerLabel.matches(Regex(".*(?:net )?cash.*(?:provided|used).*investing.*")) ->
                    MetricCategory.INVESTING_CASH_FLOW
            lowerLabel.matches(Regex(".*(?:net )?cash.*(?:provided|used).*financing.*")) ->
                    MetricCategory.FINANCING_CASH_FLOW
            lowerLabel.contains("capital expenditures") || lowerLabel == "capex" ->
                    MetricCategory.CAPITAL_EXPENDITURES
            lowerLabel.contains("free cash flow") -> MetricCategory.FREE_CASH_FLOW
            lowerLabel.contains("research and development") || lowerLabel.contains("r&d") ->
                    MetricCategory.RD_EXPENSE
            lowerLabel.matches(Regex(".*selling.*(?:general|admin).*")) ->
                    MetricCategory.SGA_EXPENSE
            lowerLabel.contains("sg&a") -> MetricCategory.SGA_EXPENSE
            lowerLabel.contains("interest expense") -> MetricCategory.INTEREST_EXPENSE
            lowerLabel.contains("depreciation") -> MetricCategory.DEPRECIATION
            lowerLabel.contains("income tax") || lowerLabel.contains("provision for") ->
                    MetricCategory.INCOME_TAX
            lowerLabel.matches(Regex(".*basic.*(?:earnings|eps).*(?:share)?.*")) ->
                    MetricCategory.EPS_BASIC
            lowerLabel.matches(Regex(".*diluted.*(?:earnings|eps).*(?:share)?.*")) ->
                    MetricCategory.EPS_DILUTED
            lowerLabel == "earnings per share" || lowerLabel == "eps" -> MetricCategory.EPS_BASIC
            lowerLabel.contains("shares outstanding") -> MetricCategory.SHARES_OUTSTANDING
            lowerLabel.contains("weighted average shares") -> MetricCategory.SHARES_OUTSTANDING
            else -> null
        }
    }

    /** Deduplicate metrics by category, keeping highest confidence */
    fun deduplicateMetrics(metrics: List<ExtendedFinancialMetric>): List<ExtendedFinancialMetric> {
        return metrics
                .groupBy { it.category }
                .mapValues { (_, list) -> list.maxByOrNull { it.confidence } ?: list.first() }
                .values
                .toList()
                .sortedBy { it.category.ordinal }
    }
}
