package com.pascal.institute.ahmes.parser

import com.pascal.institute.ahmes.model.MetricCategory

/**
 * Financial Metric Pattern Definitions
 *
 * Contains all pattern definitions for identifying financial metrics in SEC documents. Patterns are
 * organized by category (Revenue, Cost, Profit, Assets, etc.) with associated confidence scores.
 *
 * Following AGENTS.md Principle #1: Intuitive, Concise, Meaningful
 * - Extracted from EnhancedFinancialParser to reduce complexity
 * - Each pattern has a term, category, and confidence score
 */

/** Pattern definition for metric matching */
internal data class PatternDef(
        val term: String,
        val category: MetricCategory,
        val confidence: Double
)

/** All financial metric patterns */
internal val allFinancialMetricPatterns =
        listOf(
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
                PatternDef(
                        "Net Cash Provided by Operating",
                        MetricCategory.OPERATING_CASH_FLOW,
                        1.0
                ),
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
