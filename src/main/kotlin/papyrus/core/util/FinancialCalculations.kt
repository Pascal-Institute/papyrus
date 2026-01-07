package papyrus.core.util

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
        if (income?.grossProfit == null || income.totalRevenue == null) return null

        val grossProfit = income.grossProfit.toBigDecimal()
        val revenue = income.totalRevenue.toBigDecimal()

        if (revenue == BigDecimal.ZERO) return null

        return grossProfit
                .divide(revenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
                .toDouble()
    }

    private fun calculateOperatingMargin(income: StructuredIncomeStatement?): Double? {
        if (income?.operatingIncome == null || income.totalRevenue == null) return null

        val opIncome = income.operatingIncome.toBigDecimal()
        val revenue = income.totalRevenue.toBigDecimal()

        if (revenue == BigDecimal.ZERO) return null

        return opIncome.divide(revenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
                .toDouble()
    }

    private fun calculateNetProfitMargin(income: StructuredIncomeStatement?): Double? {
        if (income?.netIncome == null || income.totalRevenue == null) return null

        val netIncome = income.netIncome.toBigDecimal()
        val revenue = income.totalRevenue.toBigDecimal()

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
        if (income?.netIncome == null || balance?.totalAssets == null) return null

        val netIncome = income.netIncome.toBigDecimal()
        val totalAssets = balance.totalAssets.toBigDecimal()

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
        if (income?.netIncome == null || balance?.totalStockholdersEquity == null) return null

        val netIncome = income.netIncome.toBigDecimal()
        val equity = balance.totalStockholdersEquity.toBigDecimal()

        if (equity == BigDecimal.ZERO) return null

        return netIncome
                .divide(equity, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
                .toDouble()
    }

    // Liquidity Ratios

    private fun calculateCurrentRatio(balance: StructuredBalanceSheet?): Double? {
        if (balance?.totalCurrentAssets == null || balance.totalCurrentLiabilities == null)
                return null

        val currentAssets = balance.totalCurrentAssets.toBigDecimal()
        val currentLiabilities = balance.totalCurrentLiabilities.toBigDecimal()

        if (currentLiabilities == BigDecimal.ZERO) return null

        return currentAssets.divide(currentLiabilities, 4, RoundingMode.HALF_UP).toDouble()
    }

    private fun calculateQuickRatio(balance: StructuredBalanceSheet?): Double? {
        if (balance?.totalCurrentAssets == null ||
                        balance.inventory == null ||
                        balance.totalCurrentLiabilities == null
        )
                return null

        val currentAssets = balance.totalCurrentAssets.toBigDecimal()
        val inventory = balance.inventory.toBigDecimal()
        val currentLiabilities = balance.totalCurrentLiabilities.toBigDecimal()

        if (currentLiabilities == BigDecimal.ZERO) return null

        val quickAssets = currentAssets.subtract(inventory)
        return quickAssets.divide(currentLiabilities, 4, RoundingMode.HALF_UP).toDouble()
    }

    private fun calculateCashRatio(balance: StructuredBalanceSheet?): Double? {
        if (balance?.cashAndEquivalents == null || balance.totalCurrentLiabilities == null)
                return null

        val cash = balance.cashAndEquivalents.toBigDecimal()
        val currentLiabilities = balance.totalCurrentLiabilities.toBigDecimal()

        if (currentLiabilities == BigDecimal.ZERO) return null

        return cash.divide(currentLiabilities, 4, RoundingMode.HALF_UP).toDouble()
    }

    // Solvency Ratios

    private fun calculateDebtToEquity(balance: StructuredBalanceSheet?): Double? {
        if (balance?.totalLiabilities == null || balance.totalStockholdersEquity == null)
                return null

        val debt = balance.totalLiabilities.toBigDecimal()
        val equity = balance.totalStockholdersEquity.toBigDecimal()

        if (equity == BigDecimal.ZERO) return null

        return debt.divide(equity, 4, RoundingMode.HALF_UP).toDouble()
    }

    private fun calculateDebtRatio(balance: StructuredBalanceSheet?): Double? {
        if (balance?.totalLiabilities == null || balance.totalAssets == null) return null

        val debt = balance.totalLiabilities.toBigDecimal()
        val assets = balance.totalAssets.toBigDecimal()

        if (assets == BigDecimal.ZERO) return null

        return debt.divide(assets, 4, RoundingMode.HALF_UP).toDouble()
    }

    private fun calculateInterestCoverage(income: StructuredIncomeStatement?): Double? {
        if (income?.operatingIncome == null || income.interestExpense == null) return null

        val opIncome = income.operatingIncome.toBigDecimal()
        val interest = income.interestExpense.toBigDecimal()

        if (interest == BigDecimal.ZERO) return null

        return opIncome.divide(interest, 4, RoundingMode.HALF_UP).toDouble()
    }

    // Efficiency Ratios

    private fun calculateAssetTurnover(
            income: StructuredIncomeStatement?,
            balance: StructuredBalanceSheet?
    ): Double? {
        if (income?.totalRevenue == null || balance?.totalAssets == null) return null

        val revenue = income.totalRevenue.toBigDecimal()
        val assets = balance.totalAssets.toBigDecimal()

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
