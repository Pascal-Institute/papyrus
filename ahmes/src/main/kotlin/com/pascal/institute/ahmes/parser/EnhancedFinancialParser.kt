package com.pascal.institute.ahmes.parser

import com.pascal.institute.ahmes.model.*
import com.pascal.institute.ahmes.util.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Enhanced Financial Parser
 *
 * Provides comprehensive parsing of SEC documents including table parsing,
 * text pattern matching, financial ratio calculations, and segment analysis.
 */
object EnhancedFinancialParser {

    /**
     * SEC PDF document parsing - uses PdfParser's SecDocumentText
     */
    fun parseFromSecDocument(secDoc: SecDocumentText): List<ExtendedFinancialMetric> {
        val metrics = mutableListOf<ExtendedFinancialMetric>()

        for (section in secDoc.sections) {
            val sectionMetrics = when (section.type) {
                SecSectionType.INCOME_STATEMENT -> parseIncomeStatementSection(section.content)
                SecSectionType.BALANCE_SHEET -> parseBalanceSheetSection(section.content)
                SecSectionType.CASH_FLOW -> parseCashFlowSection(section.content)
                else -> emptyList()
            }
            metrics.addAll(sectionMetrics)
        }

        val foundCategories = metrics.map { it.category }.toSet()
        val additionalMetrics = parseFinancialMetrics(secDoc.fullText)
            .filter { it.category !in foundCategories }

        metrics.addAll(additionalMetrics)

        return metrics.map { metric ->
            metric.copy(
                context = "${secDoc.companyName} - ${secDoc.fiscalYear ?: ""} ${metric.context ?: ""}".trim()
            )
        }
    }

    /**
     * PDF text-based table parsing with column alignment recognition
     */
    fun parsePdfTextTable(text: String): List<ExtendedFinancialMetric> {
        val metrics = mutableListOf<ExtendedFinancialMetric>()
        val lines = text.split("\n")

        val unit = detectUnit(text)

        val yearPattern = Regex("""20\d{2}""")
        val headerLine = lines.take(20).find { line -> yearPattern.findAll(line).count() >= 2 }
        val years = headerLine?.let { yearPattern.findAll(it).map { m -> m.value }.toList() } ?: emptyList()

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

            val priorValue = if (numbers.size >= 2) {
                parseSecValue(numbers[1].value, unit)
            } else null

            val category = inferCategoryFromLabel(label) ?: continue

            val yoyChange = if (priorValue != null && priorValue != BigDecimal.ZERO) {
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
        if (label.all { it.isDigit() || it.isWhitespace() || it == ',' || it == '.' || it == '$' }) return false
        if (label.contains("Page ") || label.matches(Regex("""F-\d+.*"""))) return false
        if (label.matches(Regex("""^\d+.*"""))) return false
        if (label.contains("---")) return false
        return true
    }

    private fun parseSecValue(valueStr: String, unit: MetricUnit): BigDecimal? {
        val unitStr = when (unit) {
            MetricUnit.BILLIONS -> "billions"
            MetricUnit.MILLIONS -> "millions"
            MetricUnit.THOUSANDS -> "thousands"
            else -> "dollars"
        }

        val monetaryAmount = FinancialPrecision.parseSecValue(valueStr, unitStr, "USD")
        return monetaryAmount?.number?.numberValue(BigDecimal::class.java)
    }

    private fun inferCategoryFromLabel(label: String): MetricCategory? {
        val lowerLabel = label.lowercase().trim()

        return when {
            lowerLabel.matches(Regex(".*total.*(?:revenue|sales).*")) -> MetricCategory.REVENUE
            lowerLabel.matches(Regex(".*net.*(?:revenue|sales).*")) -> MetricCategory.REVENUE
            lowerLabel == "revenue" || lowerLabel == "revenues" -> MetricCategory.REVENUE
            lowerLabel == "net sales" || lowerLabel == "total net sales" -> MetricCategory.REVENUE
            lowerLabel.contains("products") && lowerLabel.contains("net sales") -> MetricCategory.PRODUCT_REVENUE
            lowerLabel.contains("services") && (lowerLabel.contains("revenue") || lowerLabel.contains("sales")) -> MetricCategory.SERVICE_REVENUE

            lowerLabel.matches(Regex(".*cost.*(?:revenue|sales|goods).*")) -> MetricCategory.COST_OF_REVENUE
            lowerLabel == "cogs" -> MetricCategory.COST_OF_REVENUE

            lowerLabel == "gross profit" || lowerLabel == "gross margin" -> MetricCategory.GROSS_PROFIT
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
            lowerLabel.contains("inventories") || lowerLabel == "inventory" -> MetricCategory.INVENTORY
            lowerLabel.contains("marketable securities") -> MetricCategory.MARKETABLE_SECURITIES
            lowerLabel.contains("property") && lowerLabel.contains("equipment") -> MetricCategory.FIXED_ASSETS

            lowerLabel == "total liabilities" -> MetricCategory.TOTAL_LIABILITIES
            lowerLabel.matches(Regex(".*total.*current.*liabilities.*")) -> MetricCategory.CURRENT_LIABILITIES
            lowerLabel.contains("accounts payable") -> MetricCategory.ACCOUNTS_PAYABLE
            lowerLabel.matches(Regex(".*long.*term.*debt.*")) -> MetricCategory.LONG_TERM_DEBT
            lowerLabel.contains("term debt") -> MetricCategory.LONG_TERM_DEBT
            lowerLabel.contains("deferred revenue") -> MetricCategory.DEFERRED_REVENUE

            lowerLabel.matches(Regex(".*total.*(?:equity|stockholders|shareholders).*")) -> MetricCategory.TOTAL_EQUITY
            lowerLabel.matches(Regex(".*(?:stockholders|shareholders).*equity.*")) -> MetricCategory.TOTAL_EQUITY
            lowerLabel.contains("retained earnings") -> MetricCategory.RETAINED_EARNINGS
            lowerLabel.contains("accumulated deficit") -> MetricCategory.RETAINED_EARNINGS

            lowerLabel.matches(Regex(".*(?:net )?cash.*(?:provided|generated|used).*operating.*")) -> MetricCategory.OPERATING_CASH_FLOW
            lowerLabel.matches(Regex(".*operating.*(?:cash flow|activities).*")) -> MetricCategory.OPERATING_CASH_FLOW
            lowerLabel.matches(Regex(".*(?:net )?cash.*(?:provided|used).*investing.*")) -> MetricCategory.INVESTING_CASH_FLOW
            lowerLabel.matches(Regex(".*(?:net )?cash.*(?:provided|used).*financing.*")) -> MetricCategory.FINANCING_CASH_FLOW
            lowerLabel.contains("capital expenditures") || lowerLabel == "capex" -> MetricCategory.CAPITAL_EXPENDITURES
            lowerLabel.contains("free cash flow") -> MetricCategory.FREE_CASH_FLOW

            lowerLabel.contains("research and development") || lowerLabel.contains("r&d") -> MetricCategory.RD_EXPENSE
            lowerLabel.matches(Regex(".*selling.*(?:general|admin).*")) -> MetricCategory.SGA_EXPENSE
            lowerLabel.contains("sg&a") -> MetricCategory.SGA_EXPENSE
            lowerLabel.contains("interest expense") -> MetricCategory.INTEREST_EXPENSE
            lowerLabel.contains("depreciation") -> MetricCategory.DEPRECIATION
            lowerLabel.contains("income tax") || lowerLabel.contains("provision for") -> MetricCategory.INCOME_TAX

            lowerLabel.matches(Regex(".*basic.*(?:earnings|eps).*(?:share)?.*")) -> MetricCategory.EPS_BASIC
            lowerLabel.matches(Regex(".*diluted.*(?:earnings|eps).*(?:share)?.*")) -> MetricCategory.EPS_DILUTED
            lowerLabel == "earnings per share" || lowerLabel == "eps" -> MetricCategory.EPS_BASIC

            lowerLabel.contains("shares outstanding") -> MetricCategory.SHARES_OUTSTANDING
            lowerLabel.contains("weighted average shares") -> MetricCategory.SHARES_OUTSTANDING
            else -> null
        }
    }

    private fun parseIncomeStatementSection(content: String): List<ExtendedFinancialMetric> {
        val metrics = parsePdfTextTable(content)
        val incomeCategories = setOf(
            MetricCategory.REVENUE, MetricCategory.PRODUCT_REVENUE, MetricCategory.SERVICE_REVENUE,
            MetricCategory.COST_OF_REVENUE, MetricCategory.GROSS_PROFIT, MetricCategory.OPERATING_INCOME,
            MetricCategory.NET_INCOME, MetricCategory.EBITDA, MetricCategory.RD_EXPENSE,
            MetricCategory.SGA_EXPENSE, MetricCategory.INTEREST_EXPENSE, MetricCategory.INCOME_TAX,
            MetricCategory.EPS_BASIC, MetricCategory.EPS_DILUTED
        )
        return metrics.filter { it.category in incomeCategories }
    }

    private fun parseBalanceSheetSection(content: String): List<ExtendedFinancialMetric> {
        val metrics = parsePdfTextTable(content)
        val balanceCategories = setOf(
            MetricCategory.TOTAL_ASSETS, MetricCategory.CURRENT_ASSETS, MetricCategory.CASH_AND_EQUIVALENTS,
            MetricCategory.ACCOUNTS_RECEIVABLE, MetricCategory.INVENTORY, MetricCategory.MARKETABLE_SECURITIES,
            MetricCategory.FIXED_ASSETS, MetricCategory.TOTAL_LIABILITIES, MetricCategory.CURRENT_LIABILITIES,
            MetricCategory.ACCOUNTS_PAYABLE, MetricCategory.LONG_TERM_DEBT, MetricCategory.DEFERRED_REVENUE,
            MetricCategory.TOTAL_EQUITY, MetricCategory.RETAINED_EARNINGS
        )
        return metrics.filter { it.category in balanceCategories }
    }

    private fun parseCashFlowSection(content: String): List<ExtendedFinancialMetric> {
        val metrics = parsePdfTextTable(content)
        val cashFlowCategories = setOf(
            MetricCategory.OPERATING_CASH_FLOW, MetricCategory.INVESTING_CASH_FLOW,
            MetricCategory.FINANCING_CASH_FLOW, MetricCategory.FREE_CASH_FLOW, MetricCategory.CAPITAL_EXPENDITURES
        )
        return metrics.filter { it.category in cashFlowCategories }
    }

    // Pattern definitions
    private data class PatternDef(val term: String, val category: MetricCategory, val confidence: Double)

    private val allPatterns = listOf(
        // Revenue
        PatternDef("Total Revenue", MetricCategory.REVENUE, 1.0),
        PatternDef("Total Revenues", MetricCategory.REVENUE, 1.0),
        PatternDef("Net Revenue", MetricCategory.REVENUE, 0.95),
        PatternDef("Net Revenues", MetricCategory.REVENUE, 0.95),
        PatternDef("Revenue", MetricCategory.REVENUE, 0.8),
        PatternDef("Net Sales", MetricCategory.REVENUE, 0.9),
        PatternDef("Total Net Sales", MetricCategory.REVENUE, 0.95),

        // Cost
        PatternDef("Cost of Revenue", MetricCategory.COST_OF_REVENUE, 1.0),
        PatternDef("Cost of Sales", MetricCategory.COST_OF_REVENUE, 0.95),
        PatternDef("Cost of Goods Sold", MetricCategory.COST_OF_REVENUE, 0.95),

        // Profit
        PatternDef("Gross Profit", MetricCategory.GROSS_PROFIT, 1.0),
        PatternDef("Operating Income", MetricCategory.OPERATING_INCOME, 1.0),
        PatternDef("Income from Operations", MetricCategory.OPERATING_INCOME, 0.95),
        PatternDef("Net Income", MetricCategory.NET_INCOME, 1.0),
        PatternDef("Net Earnings", MetricCategory.NET_INCOME, 0.95),
        PatternDef("Net Income (Loss)", MetricCategory.NET_INCOME, 1.0),
        PatternDef("EBITDA", MetricCategory.EBITDA, 1.0),

        // Assets
        PatternDef("Total Assets", MetricCategory.TOTAL_ASSETS, 1.0),
        PatternDef("Total Current Assets", MetricCategory.CURRENT_ASSETS, 1.0),
        PatternDef("Cash and Cash Equivalents", MetricCategory.CASH_AND_EQUIVALENTS, 1.0),
        PatternDef("Accounts Receivable", MetricCategory.ACCOUNTS_RECEIVABLE, 1.0),
        PatternDef("Inventories", MetricCategory.INVENTORY, 1.0),

        // Liabilities
        PatternDef("Total Liabilities", MetricCategory.TOTAL_LIABILITIES, 1.0),
        PatternDef("Total Current Liabilities", MetricCategory.CURRENT_LIABILITIES, 1.0),
        PatternDef("Long-term Debt", MetricCategory.LONG_TERM_DEBT, 1.0),

        // Equity
        PatternDef("Total Equity", MetricCategory.TOTAL_EQUITY, 1.0),
        PatternDef("Total Stockholders' Equity", MetricCategory.TOTAL_EQUITY, 1.0),
        PatternDef("Retained Earnings", MetricCategory.RETAINED_EARNINGS, 1.0),

        // Cash Flow
        PatternDef("Net Cash Provided by Operating", MetricCategory.OPERATING_CASH_FLOW, 1.0),
        PatternDef("Net Cash from Investing", MetricCategory.INVESTING_CASH_FLOW, 0.95),
        PatternDef("Net Cash from Financing", MetricCategory.FINANCING_CASH_FLOW, 0.95),
        PatternDef("Capital Expenditures", MetricCategory.CAPITAL_EXPENDITURES, 1.0),
        PatternDef("Free Cash Flow", MetricCategory.FREE_CASH_FLOW, 1.0),

        // Expenses
        PatternDef("Interest Expense", MetricCategory.INTEREST_EXPENSE, 1.0),
        PatternDef("Research and Development", MetricCategory.RD_EXPENSE, 1.0),
        PatternDef("Selling, General and Administrative", MetricCategory.SGA_EXPENSE, 0.95),

        // EPS
        PatternDef("Basic Earnings Per Share", MetricCategory.EPS_BASIC, 1.0),
        PatternDef("Diluted Earnings Per Share", MetricCategory.EPS_DILUTED, 1.0)
    )

    /**
     * Parse all financial metrics from document content
     */
    fun parseFinancialMetrics(content: String): List<ExtendedFinancialMetric> {
        val metrics = mutableListOf<ExtendedFinancialMetric>()

        // Step 1: Table-based parsing
        try {
            val tables = SecTableParser.parseFinancialTables(content)
            val tableMetrics = SecTableParser.convertToMetrics(tables)
            metrics.addAll(tableMetrics)
        } catch (e: Exception) {
            // Fallback to text parsing if table parsing fails
        }

        // Step 2: Text pattern-based parsing
        val cleanText = cleanHtml(content)
        val unit = detectUnit(cleanText)
        val period = detectPeriod(cleanText)
        val periodType = detectPeriodType(cleanText)

        val foundCategories = metrics.map { it.category }.toSet()

        for (pattern in allPatterns) {
            if (pattern.category in foundCategories) continue

            val found = searchMetricValues(
                cleanText, pattern.term, pattern.category, unit,
                period, periodType, pattern.confidence
            )
            metrics.addAll(found)
        }

        return deduplicateMetrics(metrics)
    }

    /**
     * Parse financial statements from document
     */
    fun parseFinancialStatements(content: String): List<FinancialStatement> {
        val statements = mutableListOf<FinancialStatement>()

        try {
            val tables = SecTableParser.parseFinancialTables(content)
            for (table in tables) {
                val tableMetrics = table.rows.filter { it.category != null }.map { row ->
                    ExtendedFinancialMetric(
                        name = row.label,
                        value = row.values.firstOrNull()?.toString() ?: "",
                        rawValue = row.values.firstOrNull()?.toString(),
                        category = row.category!!,
                        confidence = if (row.isTotal) 0.95 else 0.85
                    )
                }

                if (tableMetrics.isNotEmpty()) {
                    statements.add(
                        FinancialStatement(
                            type = table.statementType,
                            periodEnding = table.periods.firstOrNull(),
                            periodType = PeriodType.QUARTERLY,
                            metrics = tableMetrics,
                            rawSection = table.rawHtml.take(2000)
                        )
                    )
                }
            }

            if (statements.isNotEmpty()) return statements
        } catch (e: Exception) {
            // Fallback to text-based parsing
        }

        val cleanText = cleanHtml(content)

        val incomeSection = extractSection(cleanText, listOf(
            "CONSOLIDATED STATEMENTS OF OPERATIONS",
            "CONSOLIDATED STATEMENTS OF INCOME",
            "STATEMENTS OF OPERATIONS"
        ))
        if (incomeSection != null) {
            val metrics = parseFinancialMetrics(incomeSection).filter {
                it.category in listOf(
                    MetricCategory.REVENUE, MetricCategory.COST_OF_REVENUE,
                    MetricCategory.GROSS_PROFIT, MetricCategory.OPERATING_INCOME, MetricCategory.NET_INCOME
                )
            }
            if (metrics.isNotEmpty()) {
                statements.add(FinancialStatement(
                    type = StatementType.INCOME_STATEMENT,
                    periodEnding = detectPeriod(incomeSection),
                    periodType = detectPeriodType(incomeSection),
                    metrics = metrics,
                    rawSection = incomeSection.take(2000)
                ))
            }
        }

        val balanceSection = extractSection(cleanText, listOf(
            "CONSOLIDATED BALANCE SHEETS",
            "BALANCE SHEET",
            "STATEMENT OF FINANCIAL POSITION"
        ))
        if (balanceSection != null) {
            val metrics = parseFinancialMetrics(balanceSection).filter {
                it.category in listOf(
                    MetricCategory.TOTAL_ASSETS, MetricCategory.CURRENT_ASSETS,
                    MetricCategory.CASH_AND_EQUIVALENTS, MetricCategory.TOTAL_LIABILITIES,
                    MetricCategory.TOTAL_EQUITY
                )
            }
            if (metrics.isNotEmpty()) {
                statements.add(FinancialStatement(
                    type = StatementType.BALANCE_SHEET,
                    periodEnding = detectPeriod(balanceSection),
                    periodType = PeriodType.QUARTERLY,
                    metrics = metrics,
                    rawSection = balanceSection.take(2000)
                ))
            }
        }

        val cashFlowSection = extractSection(cleanText, listOf(
            "CONSOLIDATED STATEMENTS OF CASH FLOWS",
            "STATEMENTS OF CASH FLOWS"
        ))
        if (cashFlowSection != null) {
            val metrics = parseFinancialMetrics(cashFlowSection).filter {
                it.category in listOf(
                    MetricCategory.OPERATING_CASH_FLOW, MetricCategory.INVESTING_CASH_FLOW,
                    MetricCategory.FINANCING_CASH_FLOW, MetricCategory.FREE_CASH_FLOW
                )
            }
            if (metrics.isNotEmpty()) {
                statements.add(FinancialStatement(
                    type = StatementType.CASH_FLOW_STATEMENT,
                    periodEnding = detectPeriod(cashFlowSection),
                    periodType = detectPeriodType(cashFlowSection),
                    metrics = metrics,
                    rawSection = cashFlowSection.take(2000)
                ))
            }
        }

        return statements
    }

    /**
     * Parse risk factors from SEC document
     */
    fun parseRiskFactors(content: String): List<RiskFactor> {
        val risks = mutableListOf<RiskFactor>()
        val cleanText = cleanHtml(content)

        val riskSection = extractSection(cleanText, listOf("RISK FACTORS", "Item 1A")) ?: return risks

        val riskPatterns = listOf(
            Regex("""(?i)(?:^|\n)\s*([A-Z][^.\n]{10,100})\s*[-–—.]\s*([^\n]{50,500})"""),
            Regex("""(?i)(?:^|\n)\s*•\s*([^\n]{20,200})""")
        )

        for (pattern in riskPatterns) {
            val matches = pattern.findAll(riskSection)
            for (match in matches.take(15)) {
                val title = match.groupValues.getOrElse(1) { match.value }.trim()
                val summary = match.groupValues.getOrElse(2) { "" }.trim()

                val category = categorizeRisk(title + " " + summary)

                risks.add(
                    RiskFactor(
                        title = title.take(100),
                        summary = summary.take(300),
                        category = category,
                        severity = assessRiskSeverity(title + " " + summary)
                    )
                )
            }
        }

        return risks.distinctBy { it.title }.take(10)
    }

    /**
     * Calculate financial ratios from metrics
     */
    fun calculateRatios(metrics: List<ExtendedFinancialMetric>): List<FinancialRatio> {
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
                ratios.add(createRatio("Gross Margin", ratio, "%", RatioCategory.PROFITABILITY,
                    assessProfitabilityHealth(ratio, 30.0, 50.0)))
            }
        }

        // Operating Margin
        if (operatingIncome != null && revenue != null && revenue > 0) {
            val ratio = (operatingIncome / revenue) * 100
            if (ratio <= 100) {
                ratios.add(createRatio("Operating Margin", ratio, "%", RatioCategory.PROFITABILITY,
                    assessProfitabilityHealth(ratio, 10.0, 20.0)))
            }
        }

        // Net Profit Margin
        if (netIncome != null && revenue != null && revenue > 0) {
            val ratio = (netIncome / revenue) * 100
            if (ratio <= 100) {
                ratios.add(createRatio("Net Profit Margin", ratio, "%", RatioCategory.PROFITABILITY,
                    assessProfitabilityHealth(ratio, 5.0, 15.0)))
            }
        }

        // ROA
        if (netIncome != null && totalAssets != null && totalAssets > 0) {
            val ratio = (netIncome / totalAssets) * 100
            ratios.add(createRatio("ROA", ratio, "%", RatioCategory.PROFITABILITY,
                assessProfitabilityHealth(ratio, 2.0, 8.0)))
        }

        // ROE
        if (netIncome != null && totalEquity != null && totalEquity > 0) {
            val ratio = (netIncome / totalEquity) * 100
            ratios.add(createRatio("ROE", ratio, "%", RatioCategory.PROFITABILITY,
                assessProfitabilityHealth(ratio, 10.0, 20.0)))
        }

        // Current Ratio
        if (currentAssets != null && currentLiabilities != null && currentLiabilities > 0) {
            val ratio = currentAssets / currentLiabilities
            ratios.add(createRatio("Current Ratio", ratio, "x", RatioCategory.LIQUIDITY,
                assessLiquidityHealth(ratio, 1.0, 2.0)))
        }

        // Quick Ratio
        if (currentAssets != null && inventory != null && currentLiabilities != null && currentLiabilities > 0) {
            val ratio = (currentAssets - inventory) / currentLiabilities
            ratios.add(createRatio("Quick Ratio", ratio, "x", RatioCategory.LIQUIDITY,
                assessLiquidityHealth(ratio, 0.8, 1.5)))
        }

        // Debt to Equity
        if (totalLiabilities != null && totalEquity != null && totalEquity > 0) {
            val ratio = (totalLiabilities / totalEquity) * 100
            ratios.add(createRatio("Debt to Equity", ratio, "%", RatioCategory.SOLVENCY,
                assessDebtHealth(ratio, 100.0, 200.0)))
        }

        // Cash Ratio
        if (cash != null && currentLiabilities != null && currentLiabilities > 0) {
            val ratio = cash / currentLiabilities
            ratios.add(createRatio("Cash Ratio", ratio, "x", RatioCategory.LIQUIDITY,
                assessLiquidityHealth(ratio, 0.2, 0.5)))
        }

        // Asset Turnover
        if (revenue != null && totalAssets != null && totalAssets > 0) {
            val ratio = revenue / totalAssets
            ratios.add(createRatio("Asset Turnover", ratio, "x", RatioCategory.EFFICIENCY,
                assessEfficiencyHealth(ratio, 0.5, 1.5)))
        }

        return ratios
    }

    // Helper functions
    fun cleanHtml(content: String): String {
        var cleaned = content

        cleaned = cleaned.replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), " ")
        cleaned = cleaned.replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), " ")
        cleaned = cleaned.replace(Regex("<head[^>]*>.*?</head>", RegexOption.DOT_MATCHES_ALL), " ")
        cleaned = cleaned.replace(Regex("<ix:header[^>]*>.*?</ix:header>", RegexOption.DOT_MATCHES_ALL), " ")

        cleaned = cleaned.replace(Regex("</?ix:[^>]*>", RegexOption.IGNORE_CASE), " ")
        cleaned = cleaned.replace(Regex("</?us-gaap:[^>]*>", RegexOption.IGNORE_CASE), " ")
        cleaned = cleaned.replace(Regex("</?dei:[^>]*>", RegexOption.IGNORE_CASE), " ")

        cleaned = cleaned.replace(Regex("<tr[^>]*>", RegexOption.IGNORE_CASE), "\n| ")
        cleaned = cleaned.replace(Regex("<td[^>]*>|<th[^>]*>", RegexOption.IGNORE_CASE), " | ")
        cleaned = cleaned.replace(Regex("</td>|</th>", RegexOption.IGNORE_CASE), " ")
        cleaned = cleaned.replace(Regex("</tr>", RegexOption.IGNORE_CASE), " |\n")

        cleaned = cleaned.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        cleaned = cleaned.replace(Regex("<p[^>]*>", RegexOption.IGNORE_CASE), "\n\n")
        cleaned = cleaned.replace(Regex("<div[^>]*>", RegexOption.IGNORE_CASE), "\n")

        cleaned = cleaned.replace(Regex("<[^>]*>"), " ")

        cleaned = SecTextNormalization.decodeBasicEntities(cleaned)
        cleaned = SecTextNormalization.normalizeWhitespacePreserveNewlines(cleaned)

        return cleaned
    }

    private fun detectUnit(text: String): MetricUnit {
        val lowerText = text.lowercase()
        return when {
            lowerText.contains("in billions") || lowerText.contains("(in billions)") -> MetricUnit.BILLIONS
            lowerText.contains("in millions") || lowerText.contains("(in millions)") -> MetricUnit.MILLIONS
            lowerText.contains("in thousands") || lowerText.contains("(in thousands)") -> MetricUnit.THOUSANDS
            else -> MetricUnit.MILLIONS
        }
    }

    private fun detectPeriod(text: String): String? {
        val patterns = listOf(
            Regex("(?i)(?:For the |Quarter Ended |Year Ended |Period Ended )([A-Za-z]+\\s+\\d{1,2},?\\s+\\d{4})"),
            Regex("(?i)(Q[1-4]\\s+\\d{4})"),
            Regex("(?i)(FY\\s*\\d{4})")
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) return match.groupValues[1].trim()
        }
        return null
    }

    private fun detectPeriodType(text: String): PeriodType? {
        val lowerText = text.lowercase()
        return when {
            lowerText.contains("three months") || lowerText.contains("quarterly") -> PeriodType.QUARTERLY
            lowerText.contains("twelve months") || lowerText.contains("annual") || lowerText.contains("fiscal year") -> PeriodType.ANNUAL
            lowerText.contains("nine months") || lowerText.contains("six months") -> PeriodType.YTD
            else -> null
        }
    }

    private fun searchMetricValues(
        text: String, term: String, category: MetricCategory,
        unit: MetricUnit, period: String?, periodType: PeriodType?, baseConfidence: Double
    ): List<ExtendedFinancialMetric> {
        val results = mutableListOf<ExtendedFinancialMetric>()

        val patterns = listOf(
            Regex("(?i)${Regex.escape(term)}[:\\s\\|]*\\(?\\$?\\s*([\\d,]+(?:\\.\\d+)?)\\)?", RegexOption.IGNORE_CASE),
            Regex("(?i)${Regex.escape(term)}\\s*\\|\\s*\\$?\\s*\\(?([\\d,]+(?:\\.\\d+)?)\\)?", RegexOption.IGNORE_CASE),
            Regex("(?i)${Regex.escape(term)}[:\\s]*\\(\\$?\\s*([\\d,]+(?:\\.\\d+)?)\\)", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val matches = pattern.findAll(text)
            for ((index, match) in matches.take(5).withIndex()) {
                val valueStr = match.groupValues.getOrNull(1) ?: continue

                val context = text.substring(
                    maxOf(0, match.range.first - 100),
                    minOf(text.length, match.range.last + 100)
                )

                val isNegative = match.value.trim().startsWith("(") && match.value.trim().endsWith(")")
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

    private fun parseNumber(value: String, unit: MetricUnit, isNegative: Boolean = false): BigDecimal? {
        val unitStr = when (unit) {
            MetricUnit.BILLIONS -> "billions"
            MetricUnit.MILLIONS -> "millions"
            MetricUnit.THOUSANDS -> "thousands"
            else -> "dollars"
        }

        val result = FinancialPrecision.parseSecValue(value, unitStr, "USD")
        return result?.number?.numberValue(BigDecimal::class.java)?.let {
            if (isNegative && it > BigDecimal.ZERO) it.negate() else it
        }
    }

    private fun formatValue(value: BigDecimal): String {
        val absValue = value.abs()
        val prefix = if (value < BigDecimal.ZERO) "-" else ""

        return when {
            absValue >= BigDecimal("1000000000") -> "${prefix}$${absValue.divide(BigDecimal("1000000000"), 2, RoundingMode.HALF_UP)}B"
            absValue >= BigDecimal("1000000") -> "${prefix}$${absValue.divide(BigDecimal("1000000"), 2, RoundingMode.HALF_UP)}M"
            absValue >= BigDecimal("1000") -> "${prefix}$${absValue.divide(BigDecimal("1000"), 2, RoundingMode.HALF_UP)}K"
            else -> "${prefix}$${absValue.setScale(2, RoundingMode.HALF_UP)}"
        }
    }

    private fun extractSection(text: String, sectionNames: List<String>): String? {
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

            val section = text.substring(startMatch.range.first, minOf(endIndex, startMatch.range.first + 15000))
            if (section.length > 200) return section
        }
        return null
    }

    private fun deduplicateMetrics(metrics: List<ExtendedFinancialMetric>): List<ExtendedFinancialMetric> {
        return metrics
            .groupBy { it.category }
            .mapValues { (_, list) -> list.maxByOrNull { it.confidence } ?: list.first() }
            .values
            .toList()
            .sortedBy { it.category.ordinal }
    }

    private fun categorizeRisk(text: String): RiskCategory {
        val lowerText = text.lowercase()
        return when {
            lowerText.contains("market") || lowerText.contains("economic") -> RiskCategory.MARKET
            lowerText.contains("operation") || lowerText.contains("supply chain") -> RiskCategory.OPERATIONAL
            lowerText.contains("debt") || lowerText.contains("credit") || lowerText.contains("financial") -> RiskCategory.FINANCIAL
            lowerText.contains("regulat") || lowerText.contains("compliance") -> RiskCategory.REGULATORY
            lowerText.contains("competi") -> RiskCategory.COMPETITIVE
            lowerText.contains("technolog") || lowerText.contains("cyber") -> RiskCategory.TECHNOLOGY
            lowerText.contains("legal") || lowerText.contains("litigation") -> RiskCategory.LEGAL
            lowerText.contains("environment") || lowerText.contains("climate") -> RiskCategory.ENVIRONMENTAL
            lowerText.contains("geopolit") || lowerText.contains("international") -> RiskCategory.GEOPOLITICAL
            else -> RiskCategory.OTHER
        }
    }

    private fun assessRiskSeverity(text: String): RiskSeverity {
        val lowerText = text.lowercase()
        return when {
            lowerText.contains("material adverse") || lowerText.contains("significant risk") -> RiskSeverity.HIGH
            lowerText.contains("may adversely") || lowerText.contains("could harm") -> RiskSeverity.MEDIUM
            lowerText.contains("minor") || lowerText.contains("limited impact") -> RiskSeverity.LOW
            else -> RiskSeverity.MEDIUM
        }
    }

    private fun createRatio(
        name: String, value: Double, suffix: String,
        category: RatioCategory, health: HealthStatus
    ): FinancialRatio {
        val formatted = when (suffix) {
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

    private fun assessProfitabilityHealth(value: Double, cautionThreshold: Double, goodThreshold: Double): HealthStatus {
        return when {
            value >= goodThreshold * 1.5 -> HealthStatus.EXCELLENT
            value >= goodThreshold -> HealthStatus.GOOD
            value >= cautionThreshold -> HealthStatus.NEUTRAL
            value >= 0 -> HealthStatus.CAUTION
            else -> HealthStatus.WARNING
        }
    }

    private fun assessLiquidityHealth(value: Double, cautionThreshold: Double, goodThreshold: Double): HealthStatus {
        return when {
            value >= goodThreshold * 1.5 -> HealthStatus.EXCELLENT
            value >= goodThreshold -> HealthStatus.GOOD
            value >= cautionThreshold -> HealthStatus.NEUTRAL
            value >= 1.0 -> HealthStatus.CAUTION
            else -> HealthStatus.WARNING
        }
    }

    private fun assessDebtHealth(value: Double, goodThreshold: Double, cautionThreshold: Double): HealthStatus {
        return when {
            value <= goodThreshold -> HealthStatus.EXCELLENT
            value <= (goodThreshold + cautionThreshold) / 2 -> HealthStatus.GOOD
            value <= cautionThreshold -> HealthStatus.NEUTRAL
            value <= cautionThreshold * 1.5 -> HealthStatus.CAUTION
            else -> HealthStatus.WARNING
        }
    }

    private fun assessEfficiencyHealth(value: Double, cautionThreshold: Double, goodThreshold: Double): HealthStatus {
        return when {
            value >= goodThreshold * 1.5 -> HealthStatus.EXCELLENT
            value >= goodThreshold -> HealthStatus.GOOD
            value >= cautionThreshold -> HealthStatus.NEUTRAL
            value >= cautionThreshold * 0.5 -> HealthStatus.CAUTION
            else -> HealthStatus.WARNING
        }
    }
}
