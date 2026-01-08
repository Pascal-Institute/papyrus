package com.pascal.institute.ahmes.parser

import java.math.BigDecimal
import java.math.RoundingMode
import org.jsoup.Jsoup
import com.pascal.institute.ahmes.model.*

/**
 * SEC document table specialized parser
 *
 * Accurately parses financial statement tables in SEC 10-K, 10-Q, etc. Recognizes HTML table
 * structure and extracts numeric data accurately.
 */
object SecTableParser {

    /** Financial statement table parsing result */
    data class ParsedFinancialTable(
        val statementType: StatementType,
        val title: String,
        val periods: List<String> = emptyList(),
        val rows: List<TableRow>,
        val unit: MetricUnit,
        val currency: String = "USD",
        val rawHtml: String = ""
    )

    data class TableRow(
        val label: String,
        val values: List<BigDecimal?>,
        val isSubtotal: Boolean = false,
        val isTotal: Boolean = false,
        val indentLevel: Int = 0,
        val category: MetricCategory? = null
    )

    private val extendedSectionPatterns = mapOf(
        StatementType.INCOME_STATEMENT to listOf(
            "CONSOLIDATED STATEMENTS OF OPERATIONS",
            "CONSOLIDATED STATEMENTS OF INCOME",
            "CONSOLIDATED STATEMENTS OF EARNINGS",
            "STATEMENTS OF OPERATIONS",
            "INCOME STATEMENT",
            "STATEMENT OF OPERATIONS",
            "STATEMENT OF INCOME",
            "RESULTS OF OPERATIONS"
        ),
        StatementType.BALANCE_SHEET to listOf(
            "CONSOLIDATED BALANCE SHEETS",
            "CONSOLIDATED BALANCE SHEET",
            "BALANCE SHEETS",
            "BALANCE SHEET",
            "STATEMENT OF FINANCIAL POSITION",
            "CONSOLIDATED STATEMENTS OF FINANCIAL POSITION",
            "FINANCIAL POSITION"
        ),
        StatementType.CASH_FLOW_STATEMENT to listOf(
            "CONSOLIDATED STATEMENTS OF CASH FLOWS",
            "STATEMENTS OF CASH FLOWS",
            "CASH FLOW STATEMENT",
            "STATEMENT OF CASH FLOWS",
            "CASH FLOW STATEMENTS"
        ),
        StatementType.COMPREHENSIVE_INCOME to listOf(
            "CONSOLIDATED STATEMENTS OF COMPREHENSIVE INCOME",
            "STATEMENTS OF COMPREHENSIVE INCOME",
            "COMPREHENSIVE INCOME",
            "STATEMENT OF COMPREHENSIVE INCOME"
        ),
        StatementType.EQUITY_STATEMENT to listOf(
            "CONSOLIDATED STATEMENTS OF EQUITY",
            "STATEMENTS OF STOCKHOLDERS' EQUITY",
            "STATEMENT OF CHANGES IN EQUITY",
            "CONSOLIDATED STATEMENTS OF SHAREHOLDERS' EQUITY",
            "STOCKHOLDERS' EQUITY"
        )
    )

    /** Find and parse financial statement tables from HTML document */
    fun parseFinancialTables(htmlContent: String): List<ParsedFinancialTable> {
        val tables = mutableListOf<ParsedFinancialTable>()

        for ((statementType, patterns) in extendedSectionPatterns) {
            val parsed = findAndParseTableWithPatterns(htmlContent, statementType, patterns)
            if (parsed != null) {
                tables.add(parsed)
            }
        }

        // Fallback to text-based parsing if no HTML tables found
        if (tables.isEmpty()) {
            val fallbackTables = parsePlainTextTables(htmlContent)
            tables.addAll(fallbackTables)
        }

        return tables
    }

    private fun findAndParseTableWithPatterns(
        content: String,
        type: StatementType,
        patterns: List<String>
    ): ParsedFinancialTable? {
        val section = findSectionWithPatterns(content, patterns) ?: return null
        val unit = detectTableUnit(section)
        val periods = extractPeriodHeaders(section)
        val rows = parseTableRows(section, type, periods.size)

        if (rows.isEmpty()) return null

        return ParsedFinancialTable(
            statementType = type,
            title = patterns.firstOrNull() ?: type.name,
            periods = periods,
            rows = rows,
            unit = unit,
            rawHtml = section.take(3000)
        )
    }

    private fun findSectionWithPatterns(content: String, patterns: List<String>): String? {
        val lowerContent = content.lowercase()

        for (pattern in patterns) {
            val lowerPattern = pattern.lowercase()
            val startIdx = lowerContent.indexOf(lowerPattern)

            if (startIdx != -1) {
                val endPatterns = listOf(
                    "consolidated statements of",
                    "notes to consolidated",
                    "notes to the consolidated",
                    "item 1a", "item 1b", "item 1c",
                    "item 2", "item 3", "item 4",
                    "part ii", "part iii",
                    "signatures",
                    "report of independent"
                )

                var endIdx = content.length
                for (endPattern in endPatterns) {
                    val possibleEnd = lowerContent.indexOf(endPattern, startIdx + pattern.length + 100)
                    if (possibleEnd != -1 && possibleEnd < endIdx) {
                        endIdx = possibleEnd
                    }
                }

                val section = content.substring(startIdx, minOf(endIdx, startIdx + 50000))
                if (section.length > 500) return section
            }
        }
        return null
    }

    private fun parsePlainTextTables(text: String): List<ParsedFinancialTable> {
        val tables = mutableListOf<ParsedFinancialTable>()

        val incomeSection = findIncomeStatementInText(text)
        if (incomeSection != null) {
            val rows = parseTextTableRows(incomeSection, StatementType.INCOME_STATEMENT)
            if (rows.isNotEmpty()) {
                tables.add(ParsedFinancialTable(
                    statementType = StatementType.INCOME_STATEMENT,
                    title = "Income Statement",
                    periods = extractYearsFromText(incomeSection),
                    rows = rows,
                    unit = detectTableUnit(incomeSection)
                ))
            }
        }

        val balanceSection = findBalanceSheetInText(text)
        if (balanceSection != null) {
            val rows = parseTextTableRows(balanceSection, StatementType.BALANCE_SHEET)
            if (rows.isNotEmpty()) {
                tables.add(ParsedFinancialTable(
                    statementType = StatementType.BALANCE_SHEET,
                    title = "Balance Sheet",
                    periods = extractYearsFromText(balanceSection),
                    rows = rows,
                    unit = detectTableUnit(balanceSection)
                ))
            }
        }

        val cashFlowSection = findCashFlowInText(text)
        if (cashFlowSection != null) {
            val rows = parseTextTableRows(cashFlowSection, StatementType.CASH_FLOW_STATEMENT)
            if (rows.isNotEmpty()) {
                tables.add(ParsedFinancialTable(
                    statementType = StatementType.CASH_FLOW_STATEMENT,
                    title = "Cash Flow Statement",
                    periods = extractYearsFromText(cashFlowSection),
                    rows = rows,
                    unit = detectTableUnit(cashFlowSection)
                ))
            }
        }

        return tables
    }

    private fun findIncomeStatementInText(text: String): String? {
        val patterns = listOf(
            "Statements of Operations",
            "Statement of Income",
            "Income Statement",
            "Consolidated Statements of Operations",
            "Results of Operations"
        )
        return findTextSection(text, patterns)
    }

    private fun findBalanceSheetInText(text: String): String? {
        val patterns = listOf(
            "Balance Sheets",
            "Balance Sheet",
            "Financial Position",
            "Consolidated Balance Sheets",
            "Statement of Financial Position"
        )
        return findTextSection(text, patterns)
    }

    private fun findCashFlowInText(text: String): String? {
        val patterns = listOf(
            "Cash Flows",
            "Statement of Cash Flows",
            "Consolidated Statements of Cash Flows"
        )
        return findTextSection(text, patterns)
    }

    private fun findTextSection(text: String, patterns: List<String>): String? {
        val lowerText = text.lowercase()

        for (pattern in patterns) {
            val startIdx = lowerText.indexOf(pattern.lowercase())
            if (startIdx != -1) {
                val sectionMarkers = listOf(
                    "notes to", "item ", "part ", "signatures",
                    "consolidated statements of", "statement of"
                )

                var endIdx = minOf(text.length, startIdx + 20000)
                for (marker in sectionMarkers) {
                    val possibleEnd = lowerText.indexOf(marker, startIdx + pattern.length + 50)
                    if (possibleEnd != -1 && possibleEnd < endIdx) {
                        endIdx = possibleEnd
                    }
                }

                val section = text.substring(startIdx, endIdx)
                if (section.length > 300) return section
            }
        }
        return null
    }

    private fun extractYearsFromText(text: String): List<String> {
        val yearPattern = Regex("""20\d{2}""")
        return yearPattern
            .findAll(text.take(2000))
            .map { it.value }
            .toSet()
            .sorted()
            .takeLast(4)
            .reversed()
    }

    private fun parseTextTableRows(section: String, type: StatementType): List<TableRow> {
        val rows = mutableListOf<TableRow>()
        val lines = section.split("\n")

        val numberPattern = Regex("""\(?\$?\s*[\d,]+(?:\.\d+)?\)?""")

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.length < 5) continue

            val numbers = numberPattern.findAll(trimmedLine).toList()
            if (numbers.isEmpty()) continue

            val firstNumberStart = numbers.first().range.first
            val label = trimmedLine.substring(0, firstNumberStart).trim()

            if (label.length < 3) continue
            if (label.all { it.isDigit() || it == ',' || it == '.' || it.isWhitespace() || it == '$' }) continue
            if (label.contains("Page ") || label.matches(Regex("""F-\d+.*"""))) continue

            val values = numbers.map { parseTableValue(it.value) }
            if (values.all { it == null }) continue

            val category = mapLabelToCategory(label, type)
            val isTotal = label.lowercase().let { it.startsWith("total") || it.contains("total ") }

            rows.add(TableRow(label = label, values = values, isTotal = isTotal, category = category))
        }

        return rows
    }

    private fun detectTableUnit(section: String): MetricUnit {
        val lowerSection = section.lowercase()

        return when {
            lowerSection.contains("in billions") || lowerSection.contains("(billions)") -> MetricUnit.BILLIONS
            lowerSection.contains("in millions") || lowerSection.contains("(in millions)") ||
                    lowerSection.contains("$ in millions") -> MetricUnit.MILLIONS
            lowerSection.contains("in thousands") || lowerSection.contains("(in thousands)") -> MetricUnit.THOUSANDS
            else -> MetricUnit.MILLIONS
        }
    }

    private fun extractPeriodHeaders(section: String): List<String> {
        val datePattern = Regex(
            """(January|February|March|April|May|June|July|August|September|October|November|December)\s+\d{1,2},?\s+\d{4}""",
            RegexOption.IGNORE_CASE
        )
        val yearPattern = Regex("""\b(20\d{2})\b""")
        val quarterPattern = Regex("""Q[1-4]\s+20\d{2}""", RegexOption.IGNORE_CASE)

        val dateMatches = datePattern.findAll(section.take(2000)).toList()
        if (dateMatches.size >= 2) {
            return dateMatches.take(4).map { it.value }.distinct()
        }

        val quarterMatches = quarterPattern.findAll(section.take(2000)).toList()
        if (quarterMatches.size >= 2) {
            return quarterMatches.take(4).map { it.value }.distinct()
        }

        val yearMatches = yearPattern.findAll(section.take(2000)).toList()
        val years = yearMatches.map { it.value }.distinct().sorted().takeLast(4)

        return if (years.isNotEmpty()) years else listOf("Current Period", "Prior Period")
    }

    private fun parseTableRows(section: String, type: StatementType, expectedColumns: Int): List<TableRow> {
        val rows = mutableListOf<TableRow>()

        val doc = Jsoup.parseBodyFragment(section)
        val tableRows = doc.select("tr")

        for (tr in tableRows) {
            val cells = tr.select("th, td")
            if (cells.isEmpty()) continue

            val label = cells.first()?.text()?.trim() ?: continue

            if (label.length < 3) continue
            if (label.all { it.isDigit() || it == ',' || it == '.' || it.isWhitespace() }) continue

            val values = cells.drop(1).map { parseTableValue(it.text()) }

            if (values.all { it == null }) continue

            val category = mapLabelToCategory(label, type)
            val isTotal = label.lowercase().let { it.startsWith("total") || it.contains("total ") }
            val isSubtotal = label.lowercase().contains("subtotal")
            val indentLevel = estimateIndentLevel(label, tr.outerHtml())

            rows.add(TableRow(
                label = label,
                values = values,
                isSubtotal = isSubtotal,
                isTotal = isTotal,
                indentLevel = indentLevel,
                category = category
            ))
        }

        if (rows.isEmpty()) {
            return parseTextBasedTable(section, type, expectedColumns)
        }

        return rows
    }

    private fun parseTextBasedTable(section: String, type: StatementType, expectedColumns: Int): List<TableRow> {
        val rows = mutableListOf<TableRow>()
        val cleanSection = cleanHtmlForText(section)
        val lines = cleanSection.split("\n")

        val numberPattern = Regex("""\(?\$?\s*[\d,]+(?:\.\d+)?\)?""")

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.length < 5) continue

            val numbers = numberPattern.findAll(trimmedLine).toList()
            if (numbers.isEmpty()) continue

            val firstNumberStart = numbers.first().range.first
            val label = trimmedLine.substring(0, firstNumberStart).trim()

            if (label.length < 3) continue
            if (label.all { it.isDigit() || it == ',' || it == '.' || it.isWhitespace() }) continue

            val values = numbers.map { parseTableValue(it.value) }

            val category = mapLabelToCategory(label, type)
            val isTotal = label.lowercase().startsWith("total")

            rows.add(TableRow(label = label, values = values, isTotal = isTotal, category = category))
        }

        return rows
    }

    private fun cleanHtmlForText(html: String): String {
        var cleaned = html

        cleaned = cleaned.replace(Regex("""<script[^>]*>.*?</script>""", RegexOption.DOT_MATCHES_ALL), "")
        cleaned = cleaned.replace(Regex("""<style[^>]*>.*?</style>""", RegexOption.DOT_MATCHES_ALL), "")
        cleaned = cleaned.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
        cleaned = cleaned.replace(Regex("""</tr>""", RegexOption.IGNORE_CASE), "\n")
        cleaned = cleaned.replace(Regex("""</p>""", RegexOption.IGNORE_CASE), "\n")
        cleaned = cleaned.replace(Regex("""</div>""", RegexOption.IGNORE_CASE), "\n")
        cleaned = cleaned.replace(Regex("""</td>""", RegexOption.IGNORE_CASE), " | ")
        cleaned = cleaned.replace(Regex("""</th>""", RegexOption.IGNORE_CASE), " | ")
        cleaned = cleaned.replace(Regex("""<[^>]+>"""), " ")
        cleaned = SecTextNormalization.decodeBasicEntities(cleaned)
        cleaned = cleaned.replace("&#160;", " ")

        return cleaned
    }

    private fun parseTableValue(cellText: String): BigDecimal? {
        val cleaned = cellText.trim()

        if (cleaned.isEmpty() || cleaned == "-" || cleaned == "—" || cleaned == "–") {
            return null
        }

        val isNegative = cleaned.startsWith("(") && cleaned.endsWith(")")

        val numberStr = cleaned.replace("$", "")
            .replace(",", "")
            .replace("(", "")
            .replace(")", "")
            .replace(" ", "")
            .trim()

        return try {
            val value = BigDecimal(numberStr)
            if (isNegative) value.negate() else value
        } catch (e: Exception) {
            null
        }
    }

    private fun mapLabelToCategory(label: String, statementType: StatementType): MetricCategory? {
        val lowerLabel = label.lowercase().trim()

        return when {
            lowerLabel == "total revenue" || lowerLabel == "total revenues" -> MetricCategory.REVENUE
            lowerLabel == "net revenue" || lowerLabel == "net revenues" -> MetricCategory.REVENUE
            lowerLabel == "revenue" || lowerLabel == "revenues" -> MetricCategory.REVENUE
            lowerLabel == "net sales" || lowerLabel == "total net sales" -> MetricCategory.REVENUE
            lowerLabel == "sales" -> MetricCategory.REVENUE

            lowerLabel.contains("cost of revenue") -> MetricCategory.COST_OF_REVENUE
            lowerLabel.contains("cost of sales") -> MetricCategory.COST_OF_REVENUE
            lowerLabel.contains("cost of goods sold") || lowerLabel == "cogs" -> MetricCategory.COST_OF_REVENUE

            lowerLabel == "gross profit" || lowerLabel == "gross margin" -> MetricCategory.GROSS_PROFIT
            lowerLabel.contains("operating income") -> MetricCategory.OPERATING_INCOME
            lowerLabel.contains("income from operations") -> MetricCategory.OPERATING_INCOME
            lowerLabel == "net income" || lowerLabel.contains("net income (loss)") -> MetricCategory.NET_INCOME
            lowerLabel == "net earnings" -> MetricCategory.NET_INCOME
            lowerLabel == "net loss" -> MetricCategory.NET_INCOME
            lowerLabel == "ebitda" || lowerLabel == "adjusted ebitda" -> MetricCategory.EBITDA

            lowerLabel == "total assets" -> MetricCategory.TOTAL_ASSETS
            lowerLabel == "total current assets" -> MetricCategory.CURRENT_ASSETS
            lowerLabel.contains("cash and cash equivalents") -> MetricCategory.CASH_AND_EQUIVALENTS
            lowerLabel == "accounts receivable" -> MetricCategory.ACCOUNTS_RECEIVABLE
            lowerLabel == "inventories" || lowerLabel == "inventory" -> MetricCategory.INVENTORY

            lowerLabel == "total liabilities" -> MetricCategory.TOTAL_LIABILITIES
            lowerLabel == "total current liabilities" -> MetricCategory.CURRENT_LIABILITIES
            lowerLabel.contains("long-term debt") || lowerLabel.contains("long term debt") -> MetricCategory.LONG_TERM_DEBT
            lowerLabel == "accounts payable" -> MetricCategory.ACCOUNTS_PAYABLE

            lowerLabel == "total equity" -> MetricCategory.TOTAL_EQUITY
            lowerLabel.contains("stockholders' equity") || lowerLabel.contains("shareholders' equity") -> MetricCategory.TOTAL_EQUITY
            lowerLabel == "retained earnings" || lowerLabel == "accumulated deficit" -> MetricCategory.RETAINED_EARNINGS

            lowerLabel.contains("net cash provided by operating") -> MetricCategory.OPERATING_CASH_FLOW
            lowerLabel.contains("net cash from operating") -> MetricCategory.OPERATING_CASH_FLOW
            lowerLabel.contains("net cash used in investing") -> MetricCategory.INVESTING_CASH_FLOW
            lowerLabel.contains("net cash from investing") -> MetricCategory.INVESTING_CASH_FLOW
            lowerLabel.contains("net cash used in financing") -> MetricCategory.FINANCING_CASH_FLOW
            lowerLabel.contains("net cash from financing") -> MetricCategory.FINANCING_CASH_FLOW
            lowerLabel.contains("capital expenditures") || lowerLabel == "capex" -> MetricCategory.CAPITAL_EXPENDITURES
            lowerLabel == "free cash flow" -> MetricCategory.FREE_CASH_FLOW

            lowerLabel.contains("basic earnings per share") || lowerLabel == "basic eps" -> MetricCategory.EPS_BASIC
            lowerLabel.contains("diluted earnings per share") || lowerLabel == "diluted eps" -> MetricCategory.EPS_DILUTED

            lowerLabel.contains("research and development") || lowerLabel == "r&d expense" -> MetricCategory.RD_EXPENSE
            lowerLabel.contains("selling, general") || lowerLabel == "sg&a" -> MetricCategory.SGA_EXPENSE
            lowerLabel.contains("interest expense") -> MetricCategory.INTEREST_EXPENSE
            lowerLabel.contains("income tax") -> MetricCategory.INCOME_TAX
            lowerLabel.contains("depreciation") -> MetricCategory.DEPRECIATION
            else -> null
        }
    }

    private fun estimateIndentLevel(label: String, rowHtml: String): Int {
        val leadingSpaces = label.takeWhile { it.isWhitespace() }.length
        if (leadingSpaces > 0) return (leadingSpaces / 3).coerceAtMost(3)

        val paddingMatch = Regex("""padding-left:\s*(\d+)""").find(rowHtml)
        if (paddingMatch != null) {
            val padding = paddingMatch.groupValues[1].toIntOrNull() ?: 0
            return (padding / 20).coerceAtMost(3)
        }

        return 0
    }

    /** Convert parsed tables to ExtendedFinancialMetric list */
    fun convertToMetrics(tables: List<ParsedFinancialTable>): List<ExtendedFinancialMetric> {
        val metrics = mutableListOf<ExtendedFinancialMetric>()

        for (table in tables) {
            val multiplier = when (table.unit) {
                MetricUnit.BILLIONS -> BigDecimal("1000000000")
                MetricUnit.MILLIONS -> BigDecimal("1000000")
                MetricUnit.THOUSANDS -> BigDecimal("1000")
                else -> BigDecimal.ONE
            }

            for (row in table.rows) {
                val category = row.category ?: continue

                val currentValue = row.values.firstOrNull { it != null } ?: continue
                val priorValue = row.values.drop(1).firstOrNull { it != null }

                val scaledValue = currentValue
                    .multiply(multiplier)
                    .setScale(2, RoundingMode.HALF_UP)
                    .toDouble()

                val yoyChange = if (priorValue != null && priorValue != BigDecimal.ZERO) {
                    currentValue
                        .subtract(priorValue)
                        .divide(priorValue.abs(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal("100"))
                        .toDouble()
                } else null

                metrics.add(ExtendedFinancialMetric(
                    name = row.label,
                    value = formatValue(scaledValue),
                    rawValue = scaledValue.toString(),
                    unit = MetricUnit.DOLLARS,
                    period = table.periods.firstOrNull(),
                    periodType = detectPeriodType(table.periods.firstOrNull()),
                    category = category,
                    source = "SEC Table Parser - ${table.statementType.name}",
                    confidence = if (row.isTotal) 0.95 else 0.85,
                    yearOverYearChange = yoyChange?.toString(),
                    context = "From ${table.title}"
                ))
            }
        }

        return metrics
            .groupBy { it.category }
            .mapValues { (_, list) -> list.maxByOrNull { it.confidence } ?: list.first() }
            .values
            .toList()
    }

    private fun formatValue(value: Double): String {
        val absValue = kotlin.math.abs(value)
        val prefix = if (value < 0) "-" else ""

        return when {
            absValue >= 1_000_000_000 -> "${prefix}$${String.format("%.2f", absValue / 1_000_000_000)}B"
            absValue >= 1_000_000 -> "${prefix}$${String.format("%.2f", absValue / 1_000_000)}M"
            absValue >= 1_000 -> "${prefix}$${String.format("%.2f", absValue / 1_000)}K"
            else -> "${prefix}$${String.format("%.2f", absValue)}"
        }
    }

    private fun detectPeriodType(period: String?): PeriodType? {
        if (period == null) return null
        val lower = period.lowercase()

        return when {
            lower.contains("q1") || lower.contains("q2") || lower.contains("q3") || lower.contains("q4") -> PeriodType.QUARTERLY
            lower.matches(Regex(""".*20\d{2}.*""")) -> PeriodType.ANNUAL
            else -> null
        }
    }
}
