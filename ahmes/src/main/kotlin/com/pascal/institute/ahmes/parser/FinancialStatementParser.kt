package com.pascal.institute.ahmes.parser

import com.pascal.institute.ahmes.model.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Financial Statement Parser
 *
 * Responsible for extracting structured financial statements from SEC documents. Extracted from
 * EnhancedFinancialParser (AGENTS.md Principle #12: Seek the Essence)
 *
 * Strategies:
 * - Table structure parsing (primary)
 * - Section-based parsing (fallback)
 * - Statement type detection and categorization
 */
object FinancialStatementParser {

    /** Parse financial statements from document */
    fun parseFinancialStatements(content: String): List<FinancialStatement> {
        val statements = mutableListOf<FinancialStatement>()

        try {
            val tables = SecTableParser.parseFinancialTables(content)
            for (table in tables) {
                // Determine statement type from table context or content
                // For now, simple heuristic based on row categories
                val categories = table.rows.mapNotNull { it.category }
                if (categories.isEmpty()) continue

                val type = determineStatementType(categories)
                if (type == null) continue

                val tableMetrics =
                        table.rows.filter { it.category != null }.map { row ->
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
                                    type = type,
                                    periodEnding = ParsingHelpers.detectPeriod(content),
                                    periodType = ParsingHelpers.detectPeriodType(content)
                                                    ?: PeriodType.QUARTERLY,
                                    metrics = tableMetrics,
                                    rawSection = "Table extraction"
                            )
                    )
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Table parsing for statements failed" }
        }

        // Fallback to section extraction if no tables found or incomplete
        if (statements.isEmpty()) {
            val cleanText = ParsingHelpers.cleanHtml(content)

            // Income Statement
            val incomeSection =
                    ParsingHelpers.extractSection(
                            cleanText,
                            listOf(
                                    "CONSOLIDATED STATEMENTS OF OPERATIONS",
                                    "CONSOLIDATED STATEMENTS OF INCOME",
                                    "STATEMENTS OF OPERATIONS",
                                    "STATEMENTS OF INCOME"
                            )
                    )
            if (incomeSection != null) {
                val metrics =
                        EnhancedFinancialParser.parseFinancialMetrics(incomeSection).filter {
                            it.category in
                                    listOf(
                                            MetricCategory.REVENUE,
                                            MetricCategory.COST_OF_REVENUE,
                                            MetricCategory.GROSS_PROFIT,
                                            MetricCategory.OPERATING_INCOME,
                                            MetricCategory.NET_INCOME,
                                            MetricCategory.EPS_BASIC,
                                            MetricCategory.EPS_DILUTED
                                    )
                        }
                if (metrics.isNotEmpty()) {
                    statements.add(
                            FinancialStatement(
                                    type = StatementType.INCOME_STATEMENT,
                                    periodEnding = ParsingHelpers.detectPeriod(incomeSection),
                                    periodType = ParsingHelpers.detectPeriodType(incomeSection)
                                                    ?: PeriodType.QUARTERLY,
                                    metrics = metrics,
                                    rawSection = incomeSection.take(2000)
                            )
                    )
                }
            }

            // Balance Sheet
            val balanceSection =
                    ParsingHelpers.extractSection(
                            cleanText,
                            listOf(
                                    "CONSOLIDATED BALANCE SHEETS",
                                    "BALANCE SHEET",
                                    "STATEMENT OF FINANCIAL POSITION"
                            )
                    )
            if (balanceSection != null) {
                val metrics =
                        EnhancedFinancialParser.parseFinancialMetrics(balanceSection).filter {
                            it.category in
                                    listOf(
                                            MetricCategory.TOTAL_ASSETS,
                                            MetricCategory.CURRENT_ASSETS,
                                            MetricCategory.CASH_AND_EQUIVALENTS,
                                            MetricCategory.TOTAL_LIABILITIES,
                                            MetricCategory.TOTAL_EQUITY
                                    )
                        }
                if (metrics.isNotEmpty()) {
                    statements.add(
                            FinancialStatement(
                                    type = StatementType.BALANCE_SHEET,
                                    periodEnding = ParsingHelpers.detectPeriod(balanceSection),
                                    periodType = PeriodType.QUARTERLY,
                                    metrics = metrics,
                                    rawSection = balanceSection.take(2000)
                            )
                    )
                }
            }

            // Cash Flow
            val cashFlowSection =
                    ParsingHelpers.extractSection(
                            cleanText,
                            listOf(
                                    "CONSOLIDATED STATEMENTS OF CASH FLOWS",
                                    "STATEMENTS OF CASH FLOWS"
                            )
                    )
            if (cashFlowSection != null) {
                val cashFlowCategories =
                        listOf(
                                MetricCategory.OPERATING_CASH_FLOW,
                                MetricCategory.INVESTING_CASH_FLOW,
                                MetricCategory.FINANCING_CASH_FLOW,
                                MetricCategory.FREE_CASH_FLOW,
                                MetricCategory.CAPITAL_EXPENDITURES
                        )
                val metrics =
                        EnhancedFinancialParser.parseFinancialMetrics(cashFlowSection).filter {
                            it.category in cashFlowCategories
                        }
                if (metrics.isNotEmpty()) {
                    statements.add(
                            FinancialStatement(
                                    type = StatementType.CASH_FLOW_STATEMENT,
                                    periodEnding = ParsingHelpers.detectPeriod(cashFlowSection),
                                    periodType = PeriodType.QUARTERLY,
                                    metrics = metrics,
                                    rawSection = cashFlowSection.take(2000)
                            )
                    )
                }
            }
        }

        return statements
    }

    private fun determineStatementType(categories: List<MetricCategory>): StatementType? {
        val incomeKeywords =
                setOf(MetricCategory.REVENUE, MetricCategory.NET_INCOME, MetricCategory.EPS_BASIC)
        val balanceKeywords =
                setOf(
                        MetricCategory.TOTAL_ASSETS,
                        MetricCategory.TOTAL_LIABILITIES,
                        MetricCategory.TOTAL_EQUITY
                )
        val cashFlowKeywords =
                setOf(MetricCategory.OPERATING_CASH_FLOW, MetricCategory.INVESTING_CASH_FLOW)

        val incomeCount = categories.count { it in incomeKeywords }
        val balanceCount = categories.count { it in balanceKeywords }
        val cashFlowCount = categories.count { it in cashFlowKeywords }

        return when {
            incomeCount >= 1 -> StatementType.INCOME_STATEMENT
            balanceCount >= 1 -> StatementType.BALANCE_SHEET
            cashFlowCount >= 1 -> StatementType.CASH_FLOW_STATEMENT
            else -> null
        }
    }

    /** Helper to validate section parsing labels */
    internal fun isValidLabel(label: String): Boolean {
        // Exclude common non-metric labels
        val invalidLabels =
                listOf(
                        "total",
                        "net",
                        "legacy",
                        "consolidated",
                        "statements",
                        "notes",
                        "see accompanying queries"
                )
        return label.split(" ").size in 1..10 &&
                !invalidLabels.any { label.lowercase().contains(it) }
    }
}
