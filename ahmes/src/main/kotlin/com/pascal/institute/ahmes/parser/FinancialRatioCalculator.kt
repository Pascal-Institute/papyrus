package com.pascal.institute.ahmes.parser

import com.pascal.institute.ahmes.model.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Financial Ratio Calculator
 *
 * Responsible for calculating financial ratios and assessing financial health. Extracted from
 * EnhancedFinancialParser (Phase 5 of refactoring).
 */
object FinancialRatioCalculator {

    /** Calculate financial ratios from metrics */
    fun calculateRatios(metrics: List<ExtendedFinancialMetric>): List<FinancialRatio> {
        logger.info { "Starting ratio calculation: ${metrics.size} input metrics" }

        val ratios = mutableListOf<FinancialRatio>()

        fun getValue(category: MetricCategory): Double? {
            return metrics.find { it.category == category }?.getRawValueBigDecimal()?.toDouble()
        }

        // Extract key metrics
        val revenue = getValue(MetricCategory.REVENUE)
        val grossProfit = getValue(MetricCategory.GROSS_PROFIT)
        val operatingIncome = getValue(MetricCategory.OPERATING_INCOME)
        val netIncome = getValue(MetricCategory.NET_INCOME)
        val totalAssets = getValue(MetricCategory.TOTAL_ASSETS)
        val totalEquity = getValue(MetricCategory.TOTAL_EQUITY)
        val totalLiabilities = getValue(MetricCategory.TOTAL_LIABILITIES)
        val currentAssets = getValue(MetricCategory.CURRENT_ASSETS)
        val currentLiabilities = getValue(MetricCategory.CURRENT_LIABILITIES)
        val inventory = getValue(MetricCategory.INVENTORY)
        val cash = getValue(MetricCategory.CASH_AND_EQUIVALENTS)

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
                                assessProfitabilityHealth(ratio, 20.0, 40.0)
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
