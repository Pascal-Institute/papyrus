package com.pascal.institute.ahmes.parser

import com.pascal.institute.ahmes.model.*

/**
 * Financial Data Mapper
 *
 * Converts extracted FinancialMetric lists to structured financial statement objects.
 */
object FinancialDataMapper {

    fun convertToStructuredIncome(stmt: FinancialStatement): StructuredIncomeStatement {
        val metrics = stmt.metrics
        fun getVal(category: MetricCategory) = getMonetaryValue(metrics, category)

        return StructuredIncomeStatement(
            periodEnding = stmt.periodEnding ?: "",
            periodType = stmt.periodType,
            totalRevenue = getVal(MetricCategory.REVENUE),
            productRevenue = getVal(MetricCategory.PRODUCT_REVENUE),
            serviceRevenue = getVal(MetricCategory.SERVICE_REVENUE),
            costOfRevenue = getVal(MetricCategory.COST_OF_REVENUE),
            grossProfit = getVal(MetricCategory.GROSS_PROFIT),
            researchAndDevelopment = getVal(MetricCategory.RD_EXPENSE),
            sellingGeneralAdmin = getVal(MetricCategory.SGA_EXPENSE),
            totalOperatingExpenses = getVal(MetricCategory.OPERATING_EXPENSES)
                ?: getVal(MetricCategory.TOTAL_EXPENSES),
            operatingIncome = getVal(MetricCategory.OPERATING_INCOME),
            interestExpense = getVal(MetricCategory.INTEREST_EXPENSE),
            interestIncome = getVal(MetricCategory.INTEREST_INCOME),
            otherIncome = getVal(MetricCategory.OTHER_INCOME),
            incomeBeforeTax = getVal(MetricCategory.INCOME_BEFORE_TAX),
            incomeTaxExpense = getVal(MetricCategory.INCOME_TAX),
            netIncome = getVal(MetricCategory.NET_INCOME),
            basicEPS = getDoubleValue(metrics, MetricCategory.EPS_BASIC),
            dilutedEPS = getDoubleValue(metrics, MetricCategory.EPS_DILUTED),
            basicSharesOutstanding = getLongValue(metrics, MetricCategory.SHARES_OUTSTANDING),
            dilutedSharesOutstanding = getLongValue(metrics, MetricCategory.SHARES_DILUTED)
        )
    }

    fun convertToStructuredBalance(stmt: FinancialStatement): StructuredBalanceSheet {
        val metrics = stmt.metrics
        fun getVal(category: MetricCategory) = getMonetaryValue(metrics, category)

        return StructuredBalanceSheet(
            periodEnding = stmt.periodEnding ?: "",
            cashAndEquivalents = getVal(MetricCategory.CASH_AND_EQUIVALENTS),
            shortTermInvestments = getVal(MetricCategory.MARKETABLE_SECURITIES),
            accountsReceivable = getVal(MetricCategory.ACCOUNTS_RECEIVABLE),
            inventory = getVal(MetricCategory.INVENTORY),
            prepaidExpenses = getVal(MetricCategory.PREPAID_EXPENSES),
            otherCurrentAssets = getVal(MetricCategory.OTHER_CURRENT_ASSETS),
            totalCurrentAssets = getVal(MetricCategory.CURRENT_ASSETS),
            propertyPlantEquipment = getVal(MetricCategory.FIXED_ASSETS),
            longTermInvestments = getVal(MetricCategory.LONG_TERM_INVESTMENTS),
            deferredTaxAssets = getVal(MetricCategory.DEFERRED_TAX_ASSETS),
            totalAssets = getVal(MetricCategory.TOTAL_ASSETS),
            accountsPayable = getVal(MetricCategory.ACCOUNTS_PAYABLE),
            accruedExpenses = getVal(MetricCategory.ACCRUED_EXPENSES),
            deferredRevenue = getVal(MetricCategory.DEFERRED_REVENUE),
            totalCurrentLiabilities = getVal(MetricCategory.CURRENT_LIABILITIES),
            longTermDebt = getVal(MetricCategory.LONG_TERM_DEBT),
            totalLiabilities = getVal(MetricCategory.TOTAL_LIABILITIES),
            retainedEarnings = getVal(MetricCategory.RETAINED_EARNINGS),
            totalStockholdersEquity = getVal(MetricCategory.TOTAL_EQUITY)
        )
    }

    fun convertToStructuredCashFlow(stmt: FinancialStatement): StructuredCashFlowStatement {
        val metrics = stmt.metrics
        fun getVal(category: MetricCategory) = getMonetaryValue(metrics, category)

        return StructuredCashFlowStatement(
            periodEnding = stmt.periodEnding ?: "",
            periodType = stmt.periodType,
            netIncome = getVal(MetricCategory.NET_INCOME),
            depreciation = getVal(MetricCategory.DEPRECIATION),
            stockBasedCompensation = getVal(MetricCategory.STOCK_COMPENSATION),
            changesInWorkingCapital = getVal(MetricCategory.WORKING_CAPITAL_CHANGES),
            netCashFromOperating = getVal(MetricCategory.OPERATING_CASH_FLOW),
            capitalExpenditures = getVal(MetricCategory.CAPITAL_EXPENDITURES),
            purchaseOfInvestments = getVal(MetricCategory.INVESTMENT_PURCHASES),
            saleOfInvestments = getVal(MetricCategory.INVESTMENT_PROCEEDS),
            netCashFromInvesting = getVal(MetricCategory.INVESTING_CASH_FLOW),
            dividendsPaid = getVal(MetricCategory.DIVIDENDS_PAID),
            netCashFromFinancing = getVal(MetricCategory.FINANCING_CASH_FLOW),
            freeCashFlow = getVal(MetricCategory.FREE_CASH_FLOW)
        )
    }

    private fun getMonetaryValue(
        metrics: List<ExtendedFinancialMetric>,
        category: MetricCategory
    ): MonetaryValue? {
        return metrics.find { it.category == category }?.let { metric ->
            metric.getRawValueBigDecimal()?.let { value -> MonetaryValue.fromBigDecimal(value) }
        }
    }

    private fun getDoubleValue(
        metrics: List<ExtendedFinancialMetric>,
        category: MetricCategory
    ): Double? {
        return metrics.find { it.category == category }?.getRawValueBigDecimal()?.toDouble()
    }

    private fun getLongValue(
        metrics: List<ExtendedFinancialMetric>,
        category: MetricCategory
    ): Long? {
        return metrics.find { it.category == category }?.getRawValueBigDecimal()?.toLong()
    }
}
