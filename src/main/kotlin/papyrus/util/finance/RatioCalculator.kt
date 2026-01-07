package papyrus.util.finance

import papyrus.core.model.*

object RatioCalculator {

    fun calculateRatios(
            metrics: Map<MetricCategory, ExtendedFinancialMetric>
    ): List<FinancialRatio> {
        val calculated = mutableListOf<FinancialRatio>()

        // Helper to get double value
        fun getVal(cat: MetricCategory): Double? = metrics[cat]?.getRawValueBigDecimal()?.toDouble()

        val revenue = getVal(MetricCategory.REVENUE)
        val netIncome = getVal(MetricCategory.NET_INCOME)
        val currentAssets = getVal(MetricCategory.CURRENT_ASSETS)
        val currentLiabilities = getVal(MetricCategory.CURRENT_LIABILITIES)
        val totalAssets = getVal(MetricCategory.TOTAL_ASSETS)
        val totalEquity = getVal(MetricCategory.TOTAL_EQUITY)
        val operatingIncome = getVal(MetricCategory.OPERATING_INCOME)

        // 1. Net Margin
        if (revenue != null && netIncome != null && revenue != 0.0) {
            val value = (netIncome / revenue) * 100
            calculated.add(
                    FinancialRatio(
                            name = "Net Margin",
                            value = value,
                            formattedValue = String.format("%.2f%%", value),
                            description = "Net Income as % of Revenue",
                            interpretation = evaluateMargin(value),
                            healthStatus = getMarginHealth(value),
                            category = RatioCategory.PROFITABILITY
                    )
            )
        }

        // 2. Current Ratio
        if (currentAssets != null && currentLiabilities != null && currentLiabilities != 0.0) {
            val value = currentAssets / currentLiabilities
            calculated.add(
                    FinancialRatio(
                            name = "Current Ratio",
                            value = value,
                            formattedValue = String.format("%.2fx", value),
                            description = "Current Assets / Current Liabilities",
                            interpretation =
                                    if (value > 1.5) "Healthy Liquidity"
                                    else "Potential Liquidity Issue",
                            healthStatus =
                                    if (value > 1.5) HealthStatus.GOOD else HealthStatus.WARNING,
                            category = RatioCategory.LIQUIDITY
                    )
            )
        }

        // 3. ROE
        if (netIncome != null && totalEquity != null && totalEquity != 0.0) {
            val value = (netIncome / totalEquity) * 100
            calculated.add(
                    FinancialRatio(
                            name = "Return on Equity (ROE)",
                            value = value,
                            formattedValue = String.format("%.2f%%", value),
                            description = "Net Income / Total Equity",
                            interpretation = "Return created for shareholders",
                            healthStatus =
                                    if (value > 15) HealthStatus.EXCELLENT
                                    else HealthStatus.NEUTRAL,
                            category = RatioCategory.PROFITABILITY
                    )
            )
        }

        // 4. Asset Turnover
        if (revenue != null && totalAssets != null && totalAssets != 0.0) {
            val value = revenue / totalAssets
            calculated.add(
                    FinancialRatio(
                            name = "Asset Turnover",
                            value = value,
                            formattedValue = String.format("%.2fx", value),
                            description = "Revenue / Total Assets",
                            interpretation = "Efficiency of asset use",
                            healthStatus =
                                    HealthStatus.NEUTRAL, // Hard to judge without industry context
                            category = RatioCategory.EFFICIENCY
                    )
            )
        }

        // 5. Operating Margin
        if (revenue != null && operatingIncome != null && revenue != 0.0) {
            val value = (operatingIncome / revenue) * 100
            calculated.add(
                    FinancialRatio(
                            name = "Operating Margin",
                            value = value,
                            formattedValue = String.format("%.2f%%", value),
                            description = "Operating Income as % of Revenue",
                            interpretation = evaluateMargin(value),
                            healthStatus = getMarginHealth(value),
                            category = RatioCategory.PROFITABILITY
                    )
            )
        }

        // 6. Debt-to-Equity
        val totalLiabilities = getVal(MetricCategory.TOTAL_LIABILITIES)
        if (totalLiabilities != null && totalEquity != null && totalEquity != 0.0) {
            val value = totalLiabilities / totalEquity
            calculated.add(
                    FinancialRatio(
                            name = "Debt-to-Equity",
                            value = value,
                            formattedValue = String.format("%.2fx", value),
                            description = "Total Liabilities / Total Equity",
                            interpretation =
                                    if (value > 2.0) "High Leverage" else "Healthy Leverage",
                            healthStatus =
                                    if (value > 2.0) HealthStatus.WARNING else HealthStatus.GOOD,
                            category = RatioCategory.SOLVENCY
                    )
            )
        }

        return calculated
    }

    private fun evaluateMargin(margin: Double): String {
        return when {
            margin > 20 -> "High Profitability"
            margin > 10 -> "Healthy Profitability"
            else -> "Low Profitability"
        }
    }

    private fun getMarginHealth(margin: Double): HealthStatus {
        return when {
            margin > 20 -> HealthStatus.EXCELLENT
            margin > 10 -> HealthStatus.GOOD
            margin > 0 -> HealthStatus.NEUTRAL
            else -> HealthStatus.WARNING
        }
    }
}
