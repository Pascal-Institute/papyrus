package io.ahmes.model

import kotlinx.serialization.Serializable

/** Extended financial metrics - includes more detailed information */
@Serializable
data class ExtendedFinancialMetric(
    val name: String,
    val value: String,
    val rawValue: String? = null, // BigDecimal as String for precision
    val unit: MetricUnit = MetricUnit.DOLLARS,
    val period: String? = null, // e.g., "Q3 2024", "FY 2024"
    val periodType: PeriodType? = null, // QUARTERLY, ANNUAL
    val category: MetricCategory,
    val source: String = "", // Where in the document this was found (traceability)
    val confidence: Double = 1.0, // How confident we are in this value (0-1)
    val yearOverYearChange: String? = null, // % change as BigDecimal String
    val context: String = ""
) {
    /** Get rawValue as BigDecimal for calculations */
    fun getRawValueBigDecimal(): java.math.BigDecimal? {
        return rawValue?.let { java.math.BigDecimal(it) }
    }

    /** Get YoY change as BigDecimal */
    fun getYoyChangeBigDecimal(): java.math.BigDecimal? {
        return yearOverYearChange?.let { java.math.BigDecimal(it) }
    }

    /** Convert to simplified FinancialMetric */
    fun toFinancialMetric(): FinancialMetric {
        return FinancialMetric(
            name = name,
            value = value,
            rawValue = rawValue,
            context = context
        )
    }
}

@Serializable
enum class MetricUnit {
    DOLLARS,
    THOUSANDS,
    MILLIONS,
    BILLIONS,
    PERCENTAGE,
    RATIO,
    SHARES,
    PER_SHARE,
    NONE // For dimensionless values
}

@Serializable
enum class PeriodType {
    QUARTERLY,
    ANNUAL,
    YTD, // Year to Date
    TTM // Trailing Twelve Months
}

@Serializable
enum class MetricCategory {
    // Income Statement
    REVENUE,
    COST_OF_REVENUE,
    GROSS_PROFIT,
    OPERATING_EXPENSES,
    OPERATING_INCOME,
    NET_INCOME,
    EBITDA,
    INTEREST_EXPENSE,
    RD_EXPENSE,
    SGA_EXPENSE,
    PRODUCT_REVENUE,
    SERVICE_REVENUE,
    RD_REVENUE,
    TOTAL_EXPENSES,
    INCOME_BEFORE_TAX,
    INCOME_TAX,
    INTEREST_INCOME,
    OTHER_INCOME,
    DEPRECIATION,
    AMORTIZATION,

    // Balance Sheet - Assets
    TOTAL_ASSETS,
    CURRENT_ASSETS,
    CASH_AND_EQUIVALENTS,
    ACCOUNTS_RECEIVABLE,
    INVENTORY,
    MARKETABLE_SECURITIES,
    LONG_TERM_INVESTMENTS,
    PREPAID_EXPENSES,
    OTHER_CURRENT_ASSETS,
    FIXED_ASSETS,
    DEFERRED_TAX_ASSETS,

    // Inventory Detail
    RAW_MATERIALS,
    WORK_IN_PROCESS,
    FINISHED_GOODS,

    // Balance Sheet - Liabilities
    TOTAL_LIABILITIES,
    CURRENT_LIABILITIES,
    LONG_TERM_DEBT,
    ACCOUNTS_PAYABLE,
    ACCRUED_EXPENSES,
    OPERATING_LEASE,
    LONG_TERM_LEASE,
    DEFERRED_REVENUE,

    // Balance Sheet - Equity
    TOTAL_EQUITY,
    RETAINED_EARNINGS,

    // Cash Flow
    OPERATING_CASH_FLOW,
    INVESTING_CASH_FLOW,
    FINANCING_CASH_FLOW,
    FREE_CASH_FLOW,
    CAPITAL_EXPENDITURES,
    INVESTMENT_PURCHASES,
    INVESTMENT_PROCEEDS,
    DIVIDENDS_PAID,
    STOCK_COMPENSATION,
    WORKING_CAPITAL_CHANGES,

    // Per Share Metrics
    EPS_BASIC,
    EPS_DILUTED,
    BOOK_VALUE_PER_SHARE,
    DIVIDENDS_PER_SHARE,

    // Shares
    SHARES_OUTSTANDING,
    SHARES_DILUTED,

    // Ratios (calculated)
    GROSS_MARGIN,
    OPERATING_MARGIN,
    NET_MARGIN,
    ROA,
    ROE,
    CURRENT_RATIO,
    DEBT_TO_EQUITY,

    // Other
    EMPLOYEES,
    OTHER
}

/** Financial statement section */
@Serializable
data class FinancialStatement(
    val type: StatementType,
    val periodEnding: String?,
    val periodType: PeriodType?,
    val metrics: List<ExtendedFinancialMetric>,
    val rawSection: String = ""
)

@Serializable
enum class StatementType {
    INCOME_STATEMENT,
    BALANCE_SHEET,
    CASH_FLOW_STATEMENT,
    COMPREHENSIVE_INCOME,
    EQUITY_STATEMENT
}

/** Risk factor */
@Serializable
data class RiskFactor(
    val title: String,
    val summary: String,
    val category: RiskCategory,
    val severity: RiskSeverity = RiskSeverity.MEDIUM
)

@Serializable
enum class RiskCategory {
    MARKET,
    OPERATIONAL,
    FINANCIAL,
    REGULATORY,
    COMPETITIVE,
    TECHNOLOGY,
    LEGAL,
    ENVIRONMENTAL,
    GEOPOLITICAL,
    OTHER
}

@Serializable
enum class RiskSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/** Executive information */
@Serializable
data class ExecutiveInfo(
    val name: String,
    val title: String,
    val compensation: Double? = null
)

/** Industry information */
@Serializable
data class IndustryInfo(
    val sicCode: String?,
    val sicDescription: String?,
    val sector: String?,
    val industry: String?
)

// ========================================
// Segment Analysis and MD&A Models
// ========================================

/** Segment revenue information */
@Serializable
data class SegmentRevenue(
    val segmentName: String,
    val segmentType: SegmentType,
    val revenue: Double,
    val percentOfTotal: Double? = null,
    val operatingIncome: Double? = null,
    val source: String = ""
)

@Serializable
enum class SegmentType {
    GEOGRAPHIC,
    PRODUCT,
    SERVICE,
    CUSTOMER,
    OTHER
}

/** Management Discussion and Analysis (MD&A) */
@Serializable
data class ManagementDiscussion(
    val keyBusinessDrivers: List<String>,
    val marketConditions: String,
    val futureOutlook: String,
    val criticalAccountingPolicies: List<String>
)

// ========================================
// Structured Financial Statements Models
// ========================================

/**
 * Complete financial statement set - contains structured income statement, balance sheet, and cash
 * flow statement.
 */
@Serializable
data class StructuredFinancialData(
    val companyName: String?,
    val reportType: String?,
    val fiscalYear: String?,
    val fiscalPeriod: String?,
    val currency: String = "USD",
    val incomeStatement: StructuredIncomeStatement? = null,
    val balanceSheet: StructuredBalanceSheet? = null,
    val cashFlowStatement: StructuredCashFlowStatement? = null,
    val keyMetrics: KeyFinancialMetrics? = null,
    val parsingConfidence: Double = 0.0,
    val dataQuality: DataQuality = DataQuality.UNKNOWN
)

@Serializable
enum class DataQuality {
    HIGH,
    MEDIUM,
    LOW,
    UNKNOWN
}

/** Structured Income Statement */
@Serializable
data class StructuredIncomeStatement(
    val periodEnding: String?,
    val periodType: PeriodType?,

    // Revenue
    val totalRevenue: MonetaryValue? = null,
    val productRevenue: MonetaryValue? = null,
    val serviceRevenue: MonetaryValue? = null,

    // Expenses
    val costOfRevenue: MonetaryValue? = null,
    val grossProfit: MonetaryValue? = null,

    // Operating expenses
    val researchAndDevelopment: MonetaryValue? = null,
    val sellingGeneralAdmin: MonetaryValue? = null,
    val totalOperatingExpenses: MonetaryValue? = null,

    // Profit
    val operatingIncome: MonetaryValue? = null,
    val interestExpense: MonetaryValue? = null,
    val interestIncome: MonetaryValue? = null,
    val otherIncome: MonetaryValue? = null,
    val incomeBeforeTax: MonetaryValue? = null,
    val incomeTaxExpense: MonetaryValue? = null,
    val netIncome: MonetaryValue? = null,

    // Per share metrics
    val basicEPS: Double? = null,
    val dilutedEPS: Double? = null,
    val basicSharesOutstanding: Long? = null,
    val dilutedSharesOutstanding: Long? = null
)

/** Structured Balance Sheet */
@Serializable
data class StructuredBalanceSheet(
    val periodEnding: String?,

    // Current assets
    val cashAndEquivalents: MonetaryValue? = null,
    val shortTermInvestments: MonetaryValue? = null,
    val accountsReceivable: MonetaryValue? = null,
    val inventory: MonetaryValue? = null,
    val prepaidExpenses: MonetaryValue? = null,
    val otherCurrentAssets: MonetaryValue? = null,
    val totalCurrentAssets: MonetaryValue? = null,

    // Non-current assets
    val propertyPlantEquipment: MonetaryValue? = null,
    val longTermInvestments: MonetaryValue? = null,
    val goodwill: MonetaryValue? = null,
    val intangibleAssets: MonetaryValue? = null,
    val deferredTaxAssets: MonetaryValue? = null,
    val otherNonCurrentAssets: MonetaryValue? = null,
    val totalAssets: MonetaryValue? = null,

    // Current liabilities
    val accountsPayable: MonetaryValue? = null,
    val shortTermDebt: MonetaryValue? = null,
    val accruedExpenses: MonetaryValue? = null,
    val deferredRevenue: MonetaryValue? = null,
    val otherCurrentLiabilities: MonetaryValue? = null,
    val totalCurrentLiabilities: MonetaryValue? = null,

    // Non-current liabilities
    val longTermDebt: MonetaryValue? = null,
    val deferredTaxLiabilities: MonetaryValue? = null,
    val otherNonCurrentLiabilities: MonetaryValue? = null,
    val totalLiabilities: MonetaryValue? = null,

    // Equity
    val commonStock: MonetaryValue? = null,
    val retainedEarnings: MonetaryValue? = null,
    val accumulatedOtherComprehensiveIncome: MonetaryValue? = null,
    val totalStockholdersEquity: MonetaryValue? = null,
    val totalLiabilitiesAndEquity: MonetaryValue? = null
)

/** Structured Cash Flow Statement */
@Serializable
data class StructuredCashFlowStatement(
    val periodEnding: String?,
    val periodType: PeriodType?,

    // Cash flow from operating activities
    val netIncome: MonetaryValue? = null,
    val depreciation: MonetaryValue? = null,
    val stockBasedCompensation: MonetaryValue? = null,
    val changesInWorkingCapital: MonetaryValue? = null,
    val netCashFromOperating: MonetaryValue? = null,

    // Cash flow from investing activities
    val capitalExpenditures: MonetaryValue? = null,
    val purchaseOfInvestments: MonetaryValue? = null,
    val saleOfInvestments: MonetaryValue? = null,
    val acquisitions: MonetaryValue? = null,
    val netCashFromInvesting: MonetaryValue? = null,

    // Cash flow from financing activities
    val dividendsPaid: MonetaryValue? = null,
    val shareRepurchases: MonetaryValue? = null,
    val debtRepayment: MonetaryValue? = null,
    val debtIssuance: MonetaryValue? = null,
    val netCashFromFinancing: MonetaryValue? = null,

    // Summary
    val netChangeInCash: MonetaryValue? = null,
    val beginningCash: MonetaryValue? = null,
    val endingCash: MonetaryValue? = null,

    // Calculated metrics
    val freeCashFlow: MonetaryValue? = null
)

/**
 * Monetary value (maintains precision using String representation of BigDecimal)
 *
 * AGENTS.MD Principle 4: Handle Data with Integrity
 * - Stores amount as String to preserve BigDecimal precision during serialization
 * - Uses explicit currency units
 * - Maintains traceability through originalUnit and confidence
 */
@Serializable
data class MonetaryValue(
    val amount: String,
    val formatted: String,
    val currency: String = "USD",
    val originalUnit: MetricUnit,
    val isNegative: Boolean = false,
    val yearOverYearChange: String? = null,
    val confidence: Double = 1.0,
    val source: String = ""
) {
    companion object {
        /** Create from Double (for backward compatibility) WARNING: Prefer fromBigDecimal when possible */
        fun fromDouble(
            value: Double,
            unit: MetricUnit = MetricUnit.DOLLARS
        ): MonetaryValue {
            return fromBigDecimal(java.math.BigDecimal(value.toString()), unit)
        }

        /** Create from BigDecimal (preferred method for precision) */
        fun fromBigDecimal(
            value: java.math.BigDecimal,
            unit: MetricUnit = MetricUnit.DOLLARS,
            yoyChange: java.math.BigDecimal? = null,
            confidence: Double = 1.0,
            source: String = ""
        ): MonetaryValue {
            val absValue = value.abs()
            val formatted = when {
                absValue >= java.math.BigDecimal("1000000000") ->
                    "$${absValue.divide(java.math.BigDecimal("1000000000"), 2, java.math.RoundingMode.HALF_UP)}B"
                absValue >= java.math.BigDecimal("1000000") ->
                    "$${absValue.divide(java.math.BigDecimal("1000000"), 2, java.math.RoundingMode.HALF_UP)}M"
                absValue >= java.math.BigDecimal("1000") ->
                    "$${absValue.divide(java.math.BigDecimal("1000"), 2, java.math.RoundingMode.HALF_UP)}K"
                else ->
                    "$${absValue.setScale(2, java.math.RoundingMode.HALF_UP)}"
            }

            return MonetaryValue(
                amount = value.setScale(2, java.math.RoundingMode.HALF_UP).toString(),
                formatted = if (value < java.math.BigDecimal.ZERO) "-$formatted" else formatted,
                currency = "USD",
                originalUnit = unit,
                isNegative = value < java.math.BigDecimal.ZERO,
                yearOverYearChange = yoyChange?.setScale(2, java.math.RoundingMode.HALF_UP)?.toString(),
                confidence = confidence,
                source = source
            )
        }

        /** Create from MonetaryAmount (JavaMoney) */
        fun fromMonetaryAmount(
            amount: javax.money.MonetaryAmount,
            unit: MetricUnit = MetricUnit.DOLLARS,
            yoyChange: java.math.BigDecimal? = null,
            confidence: Double = 1.0,
            source: String = ""
        ): MonetaryValue {
            val value = amount.number.numberValue(java.math.BigDecimal::class.java)
            return fromBigDecimal(value, unit, yoyChange, confidence, source)
        }
    }

    /** Get the amount as BigDecimal for calculations */
    fun toBigDecimal(): java.math.BigDecimal = java.math.BigDecimal(amount)

    /** Get the YoY change as BigDecimal */
    fun getYoyChangeBigDecimal(): java.math.BigDecimal? {
        return yearOverYearChange?.let { java.math.BigDecimal(it) }
    }
}

/** Key financial metrics (calculated ratios) */
@Serializable
data class KeyFinancialMetrics(
    // Profitability
    val grossMargin: Double? = null,
    val operatingMargin: Double? = null,
    val netProfitMargin: Double? = null,
    val returnOnAssets: Double? = null,
    val returnOnEquity: Double? = null,

    // Liquidity
    val currentRatio: Double? = null,
    val quickRatio: Double? = null,
    val cashRatio: Double? = null,

    // Solvency
    val debtToEquity: Double? = null,
    val debtRatio: Double? = null,
    val interestCoverage: Double? = null,

    // Efficiency
    val assetTurnover: Double? = null,
    val inventoryTurnover: Double? = null,
    val receivablesTurnover: Double? = null,

    // Growth
    val revenueGrowth: Double? = null,
    val netIncomeGrowth: Double? = null,
    val epsGrowth: Double? = null
)

/** SEC report metadata */
@Serializable
data class SecReportMetadata(
    val formType: String,
    val filingDate: String?,
    val reportDate: String?,
    val fiscalYearEnd: String?,
    val companyName: String?,
    val ticker: String?,
    val cik: String?,
    val accessionNumber: String?,
    val documentCount: Int = 0,
    val primaryDocument: String?
)

/** Parsing result summary */
@Serializable
data class ParseSummary(
    val totalMetricsFound: Int,
    val incomeStatementMetrics: Int,
    val balanceSheetMetrics: Int,
    val cashFlowMetrics: Int,
    val riskFactorsFound: Int,
    val tablesProcessed: Int,
    val parsingMethod: String,
    val processingTimeMs: Long,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList()
)
