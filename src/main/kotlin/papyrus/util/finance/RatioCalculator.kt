package papyrus.util.finance

import papyrus.core.model.*
import java.math.BigDecimal
import java.math.RoundingMode

object RatioCalculator {

    fun calculateRatios(
            metrics: Map<MetricCategory, ExtendedFinancialMetric>
    ): List<FinancialRatio> {
        val calculated = mutableListOf<FinancialRatio>()

        // Helper to get BigDecimal value for precision
        fun getVal(cat: MetricCategory): BigDecimal? = metrics[cat]?.getRawValueBigDecimal()

        val revenue = getVal(MetricCategory.REVENUE)
        val netIncome = getVal(MetricCategory.NET_INCOME)
        val currentAssets = getVal(MetricCategory.CURRENT_ASSETS)
        val currentLiabilities = getVal(MetricCategory.CURRENT_LIABILITIES)
        val totalAssets = getVal(MetricCategory.TOTAL_ASSETS)
        val totalEquity = getVal(MetricCategory.TOTAL_EQUITY)
        val operatingIncome = getVal(MetricCategory.OPERATING_INCOME)

        // 1. Net Margin
        if (revenue != null && netIncome != null && revenue != BigDecimal.ZERO) {
            val value = (netIncome.divide(revenue, 6, RoundingMode.HALF_UP)).multiply(BigDecimal("100"))
            val valueDouble = value.toDouble()
            calculated.add(
                    FinancialRatio(
                            name = "Net Margin",
                            value = value.toString(),
                            formattedValue = String.format("%.2f%%", valueDouble),
                            description = "Net Income as % of Revenue",
                            interpretation = evaluateMargin(valueDouble),
                            healthStatus = getMarginHealth(valueDouble),
                            category = RatioCategory.PROFITABILITY
                    )
            )
        }

        // 2. Current Ratio
        if (currentAssets != null && currentLiabilities != null && currentLiabilities != BigDecimal.ZERO) {
            val value = currentAssets.divide(currentLiabilities, 6, RoundingMode.HALF_UP)
            val valueDouble = value.toDouble()
            calculated.add(
                    FinancialRatio(
                            name = "Current Ratio",
                            value = value.toString(),
                            formattedValue = String.format("%.2fx", valueDouble),
                            description = "Current Assets / Current Liabilities",
                            interpretation =
                                    if (valueDouble > 1.5) "Healthy Liquidity"
                                    else "Potential Liquidity Issue",
                            healthStatus =
                                    if (valueDouble > 1.5) HealthStatus.GOOD else HealthStatus.WARNING,
                            category = RatioCategory.LIQUIDITY
                    )
            )
        }

        // 3. ROE
        if (netIncome != null && totalEquity != null && totalEquity != BigDecimal.ZERO) {
            val value = (netIncome.divide(totalEquity, 6, RoundingMode.HALF_UP)).multiply(BigDecimal("100"))
            val valueDouble = value.toDouble()
            calculated.add(
                    FinancialRatio(
                            name = "Return on Equity (ROE)",
                            value = value.toString(),
                            formattedValue = String.format("%.2f%%", valueDouble),
                            description = "Net Income / Total Equity",
                            interpretation = "Return created for shareholders",
                            healthStatus =
                                    if (valueDouble > 15) HealthStatus.EXCELLENT
                                    else HealthStatus.NEUTRAL,
                            category = RatioCategory.PROFITABILITY
                    )
            )
        }

        // 4. Asset Turnover
        if (revenue != null && totalAssets != null && totalAssets != BigDecimal.ZERO) {
            val value = revenue.divide(totalAssets, 6, RoundingMode.HALF_UP)
            val valueDouble = value.toDouble()
            calculated.add(
                    FinancialRatio(
                            name = "Asset Turnover",
                            value = value.toString(),
                            formattedValue = String.format("%.2fx", valueDouble),
                            description = "Revenue / Total Assets",
                            interpretation = "Efficiency of asset use",
                            healthStatus =
                                    HealthStatus.NEUTRAL, // Hard to judge without industry context
                            category = RatioCategory.EFFICIENCY
                    )
            )
        }

        // 5. Operating Margin
        if (revenue != null && operatingIncome != null && revenue != BigDecimal.ZERO) {
            val value = (operatingIncome.divide(revenue, 6, RoundingMode.HALF_UP)).multiply(BigDecimal("100"))
            val valueDouble = value.toDouble()
            calculated.add(
                    FinancialRatio(
                            name = "Operating Margin",
                            value = value.toString(),
                            formattedValue = String.format("%.2f%%", valueDouble),
                            description = "Operating Income as % of Revenue",
                            interpretation = evaluateMargin(valueDouble),
                            healthStatus = getMarginHealth(valueDouble),
                            category = RatioCategory.PROFITABILITY
                    )
            )
        }

        // 6. Debt-to-Equity
        val totalLiabilities = getVal(MetricCategory.TOTAL_LIABILITIES)
        if (totalLiabilities != null && totalEquity != null && totalEquity != BigDecimal.ZERO) {
            val value = totalLiabilities.divide(totalEquity, 6, RoundingMode.HALF_UP)
            val valueDouble = value.toDouble()
            calculated.add(
                    FinancialRatio(
                            name = "Debt-to-Equity",
                            value = value.toString(),
                            formattedValue = String.format("%.2fx", valueDouble),
                            description = "Total Liabilities / Total Equity",
                            interpretation =
                                    if (valueDouble > 2.0) "High Leverage" else "Healthy Leverage",
                            healthStatus =
                                    if (valueDouble > 2.0) HealthStatus.WARNING else HealthStatus.GOOD,
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
