package papyrus.util.finance

import java.math.BigDecimal
import java.math.RoundingMode
import papyrus.core.model.*

/**
 * Financial Calculation Utilities
 *
 * AGENTS.MD Principle 4: Embed Absolute Financial Precision Provides validated financial ratio
 * calculations with proper precision handling
 */
object FinancialCalculations {

    /** Calculate Financial Ratios from Structured Financial Data */
    fun calculateKeyMetrics(data: StructuredFinancialData): KeyFinancialMetrics {
        val income = data.incomeStatement
        val balance = data.balanceSheet
        val cashFlow = data.cashFlowStatement

        return KeyFinancialMetrics(
                // Profitability Ratios
                grossMargin = calculateGrossMargin(income),
                operatingMargin = calculateOperatingMargin(income),
                netProfitMargin = calculateNetProfitMargin(income),
                returnOnAssets = calculateROA(income, balance),
                returnOnEquity = calculateROE(income, balance),

                // Liquidity Ratios
                currentRatio = calculateCurrentRatio(balance),
                quickRatio = calculateQuickRatio(balance),
                cashRatio = calculateCashRatio(balance),

                // Solvency Ratios
                debtToEquity = calculateDebtToEquity(balance),
                debtRatio = calculateDebtRatio(balance),
                interestCoverage = calculateInterestCoverage(income),

                // Efficiency Ratios
                assetTurnover = calculateAssetTurnover(income, balance),

                // Growth Metrics
                revenueGrowth = extractGrowthRate(income?.totalRevenue),
                netIncomeGrowth = extractGrowthRate(income?.netIncome)
        )
    }

    // Profitability Ratios

    private fun calculateGrossMargin(income: StructuredIncomeStatement?): Double? {
        val grossProfitValue = income?.grossProfit ?: return null
        val revenueValue = income.totalRevenue ?: return null

        val grossProfit = grossProfitValue.toBigDecimal()
        val revenue = revenueValue.toBigDecimal()

        if (revenue == BigDecimal.ZERO) return null

        return grossProfit
                .divide(revenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
                .toDouble()
    }

    private fun calculateOperatingMargin(income: StructuredIncomeStatement?): Double? {
        val opIncomeValue = income?.operatingIncome ?: return null
        val revenueValue = income.totalRevenue ?: return null

        val opIncome = opIncomeValue.toBigDecimal()
        val revenue = revenueValue.toBigDecimal()

        if (revenue == BigDecimal.ZERO) return null

        return opIncome.divide(revenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
                .toDouble()
    }

    private fun calculateNetProfitMargin(income: StructuredIncomeStatement?): Double? {
        val netIncomeValue = income?.netIncome ?: return null
        val revenueValue = income.totalRevenue ?: return null

        val netIncome = netIncomeValue.toBigDecimal()
        val revenue = revenueValue.toBigDecimal()

        if (revenue == BigDecimal.ZERO) return null

        return netIncome
                .divide(revenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
                .toDouble()
    }

    private fun calculateROA(
            income: StructuredIncomeStatement?,
            balance: StructuredBalanceSheet?
    ): Double? {
        val netIncomeValue = income?.netIncome ?: return null
        val totalAssetsValue = balance?.totalAssets ?: return null

        val netIncome = netIncomeValue.toBigDecimal()
        val totalAssets = totalAssetsValue.toBigDecimal()

        if (totalAssets == BigDecimal.ZERO) return null

        return netIncome
                .divide(totalAssets, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
                .toDouble()
    }

    private fun calculateROE(
            income: StructuredIncomeStatement?,
            balance: StructuredBalanceSheet?
    ): Double? {
        val netIncomeValue = income?.netIncome ?: return null
        val equityValue = balance?.totalStockholdersEquity ?: return null

        val netIncome = netIncomeValue.toBigDecimal()
        val equity = equityValue.toBigDecimal()

        if (equity == BigDecimal.ZERO) return null

        return netIncome
                .divide(equity, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
                .toDouble()
    }

    // Liquidity Ratios

    private fun calculateCurrentRatio(balance: StructuredBalanceSheet?): Double? {
        val currentAssetsValue = balance?.totalCurrentAssets ?: return null
        val currentLiabilitiesValue = balance.totalCurrentLiabilities ?: return null

        val currentAssets = currentAssetsValue.toBigDecimal()
        val currentLiabilities = currentLiabilitiesValue.toBigDecimal()

        if (currentLiabilities == BigDecimal.ZERO) return null

        return currentAssets.divide(currentLiabilities, 4, RoundingMode.HALF_UP).toDouble()
    }

    private fun calculateQuickRatio(balance: StructuredBalanceSheet?): Double? {
        val currentAssetsValue = balance?.totalCurrentAssets ?: return null
        val inventoryValue = balance.inventory ?: return null
        val currentLiabilitiesValue = balance.totalCurrentLiabilities ?: return null

        val currentAssets = currentAssetsValue.toBigDecimal()
        val inventory = inventoryValue.toBigDecimal()
        val currentLiabilities = currentLiabilitiesValue.toBigDecimal()

        if (currentLiabilities == BigDecimal.ZERO) return null

        val quickAssets = currentAssets.subtract(inventory)
        return quickAssets.divide(currentLiabilities, 4, RoundingMode.HALF_UP).toDouble()
    }

    private fun calculateCashRatio(balance: StructuredBalanceSheet?): Double? {
        val cashValue = balance?.cashAndEquivalents ?: return null
        val currentLiabilitiesValue = balance.totalCurrentLiabilities ?: return null

        val cash = cashValue.toBigDecimal()
        val currentLiabilities = currentLiabilitiesValue.toBigDecimal()

        if (currentLiabilities == BigDecimal.ZERO) return null

        return cash.divide(currentLiabilities, 4, RoundingMode.HALF_UP).toDouble()
    }

    // Solvency Ratios

    private fun calculateDebtToEquity(balance: StructuredBalanceSheet?): Double? {
        val debtValue = balance?.totalLiabilities ?: return null
        val equityValue = balance.totalStockholdersEquity ?: return null

        val debt = debtValue.toBigDecimal()
        val equity = equityValue.toBigDecimal()

        if (equity == BigDecimal.ZERO) return null

        return debt.divide(equity, 4, RoundingMode.HALF_UP).toDouble()
    }

    private fun calculateDebtRatio(balance: StructuredBalanceSheet?): Double? {
        val debtValue = balance?.totalLiabilities ?: return null
        val assetsValue = balance.totalAssets ?: return null

        val debt = debtValue.toBigDecimal()
        val assets = assetsValue.toBigDecimal()

        if (assets == BigDecimal.ZERO) return null

        return debt.divide(assets, 4, RoundingMode.HALF_UP).toDouble()
    }

    private fun calculateInterestCoverage(income: StructuredIncomeStatement?): Double? {
        val opIncomeValue = income?.operatingIncome ?: return null
        val interestValue = income.interestExpense ?: return null

        val opIncome = opIncomeValue.toBigDecimal()
        val interest = interestValue.toBigDecimal()

        if (interest == BigDecimal.ZERO) return null

        return opIncome.divide(interest, 4, RoundingMode.HALF_UP).toDouble()
    }

    // Efficiency Ratios

    private fun calculateAssetTurnover(
            income: StructuredIncomeStatement?,
            balance: StructuredBalanceSheet?
    ): Double? {
        val revenueValue = income?.totalRevenue ?: return null
        val assetsValue = balance?.totalAssets ?: return null

        val revenue = revenueValue.toBigDecimal()
        val assets = assetsValue.toBigDecimal()

        if (assets == BigDecimal.ZERO) return null

        return revenue.divide(assets, 4, RoundingMode.HALF_UP).toDouble()
    }

    // Helper Methods

    private fun extractGrowthRate(value: MonetaryValue?): Double? {
        return value?.getYoyChangeBigDecimal()?.toDouble()
    }

    /** Validate that a ratio is within plausible bounds */
    fun validateRatio(
            ratio: Double,
            name: String,
            min: Double = -100.0,
            max: Double = 1000.0
    ): Double {
        if (ratio < min || ratio > max) {
            throw IllegalFinancialStateException(
                    "Implausible $name: $ratio is outside expected range [$min, $max]"
            )
        }
        return ratio
    }
}
