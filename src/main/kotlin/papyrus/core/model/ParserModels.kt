package papyrus.core.model

import kotlinx.serialization.Serializable

/** Extended financial metrics - includes more detailed information */
@Serializable
data class ExtendedFinancialMetric(
        val name: String,
        val value: String,
        val rawValue: Double? = null,
        val unit: MetricUnit = MetricUnit.DOLLARS,
        val period: String? = null, // e.g., "Q3 2024", "FY 2024"
        val periodType: PeriodType? = null, // QUARTERLY, ANNUAL
        val category: MetricCategory,
        val source: String = "", // Where in the document this was found
        val confidence: Double = 1.0, // How confident we are in this value (0-1)
        val yearOverYearChange: Double? = null, // % change from previous year
        val context: String = ""
)

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

enum class PeriodType {
    QUARTERLY,
    ANNUAL,
    YTD, // Year to Date
    TTM // Trailing Twelve Months
}

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

enum class StatementType {
    INCOME_STATEMENT, // Income statement
    BALANCE_SHEET, // Balance sheet
    CASH_FLOW_STATEMENT, // Cash flow statement
    COMPREHENSIVE_INCOME, // Comprehensive income statement
    EQUITY_STATEMENT // Statement of changes in equity
}

/** Risk factor */
@Serializable
data class RiskFactor(
        val title: String,
        val summary: String,
        val category: RiskCategory,
        val severity: RiskSeverity = RiskSeverity.MEDIUM
)

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

enum class RiskSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/** Executive information */
@Serializable
data class ExecutiveInfo(val name: String, val title: String, val compensation: Double? = null)

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
        val segmentName: String, // Segment name (e.g., "Americas", "iPhone")
        val segmentType: SegmentType, // Segment type
        val revenue: Double, // Revenue
        val percentOfTotal: Double? = null, // Percentage of total revenue (%)
        val operatingIncome: Double? = null, // Operating income (if available)
        val source: String = "" // Data source (page/line number)
)

enum class SegmentType {
    GEOGRAPHIC, // By region
    PRODUCT, // By product
    SERVICE, // By service
    CUSTOMER, // By customer type
    OTHER
}

/** Management Discussion and Analysis (MD&A) */
@Serializable
data class ManagementDiscussion(
        val keyBusinessDrivers: List<String>, // Key business drivers
        val marketConditions: String, // Market conditions
        val futureOutlook: String, // Future outlook
        val criticalAccountingPolicies: List<String> // Critical accounting policies
)

// ========================================
// Structured Financial Statements Models
// ========================================

/** Complete financial statement set - contains structured income statement, balance sheet, and cash flow statement. */
@Serializable
data class StructuredFinancialData(
        val companyName: String?,
        val reportType: String?, // 10-K, 10-Q, 8-K
        val fiscalYear: String?,
        val fiscalPeriod: String?, // Q1, Q2, Q3, Q4, FY
        val currency: String = "USD",
        val incomeStatement: StructuredIncomeStatement? = null,
        val balanceSheet: StructuredBalanceSheet? = null,
        val cashFlowStatement: StructuredCashFlowStatement? = null,
        val keyMetrics: KeyFinancialMetrics? = null,
        val parsingConfidence: Double = 0.0, // 0-1, parsing confidence
        val dataQuality: DataQuality = DataQuality.UNKNOWN
)

enum class DataQuality {
    HIGH, // Table parsing successful, all major items present
    MEDIUM, // Some items missing or pattern parsing
    LOW, // Mostly pattern parsing, low confidence
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

/** Monetary value (maintains precision) */
@Serializable
data class MonetaryValue(
        val amount: Double, // Actual amount (normalized to dollars)
        val formatted: String, // Formatted for display (e.g., "$1.23B")
        val originalUnit: MetricUnit, // Unit in original document
        val isNegative: Boolean = false,
        val yearOverYearChange: Double? = null, // YoY 변화율 (%)
        val confidence: Double = 1.0
) {
    companion object {
        fun fromDouble(value: Double, unit: MetricUnit = MetricUnit.DOLLARS): MonetaryValue {
            val absValue = kotlin.math.abs(value)
            val formatted =
                    when {
                        absValue >= 1_000_000_000 ->
                                "$${String.format("%.2f", absValue / 1_000_000_000)}B"
                        absValue >= 1_000_000 -> "$${String.format("%.2f", absValue / 1_000_000)}M"
                        absValue >= 1_000 -> "$${String.format("%.2f", absValue / 1_000)}K"
                        else -> "$${String.format("%.2f", absValue)}"
                    }
            return MonetaryValue(
                    amount = value,
                    formatted = if (value < 0) "-$formatted" else formatted,
                    originalUnit = unit,
                    isNegative = value < 0
            )
        }
    }
}

/** Key financial metrics (calculated ratios) */
@Serializable
data class KeyFinancialMetrics(
        // Profitability
        val grossMargin: Double? = null, // Gross profit margin
        val operatingMargin: Double? = null, // Operating profit margin
        val netProfitMargin: Double? = null, // Net profit margin
        val returnOnAssets: Double? = null, // ROA
        val returnOnEquity: Double? = null, // ROE

        // Liquidity
        val currentRatio: Double? = null, // Current ratio
        val quickRatio: Double? = null, // Quick ratio
        val cashRatio: Double? = null, // Cash ratio

        // 지급능력
        val debtToEquity: Double? = null, // 부채비율
        val debtRatio: Double? = null, // 총부채비율
        val interestCoverage: Double? = null, // Interest coverage ratio

        // Efficiency
        val assetTurnover: Double? = null, // Asset turnover
        val inventoryTurnover: Double? = null, // Inventory turnover
        val receivablesTurnover: Double? = null, // Receivables turnover

        // 성장성
        val revenueGrowth: Double? = null, // 매출 성장률 (YoY)
        val netIncomeGrowth: Double? = null, // 순이익 성장률 (YoY)
        val epsGrowth: Double? = null // EPS 성장률 (YoY)
)

/** SEC report metadata */
@Serializable
data class SecReportMetadata(
        val formType: String, // 10-K, 10-Q, 8-K, 20-F
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
        val parsingMethod: String, // "table", "pattern", "hybrid"
        val processingTimeMs: Long,
        val warnings: List<String> = emptyList(),
        val errors: List<String> = emptyList()
)
