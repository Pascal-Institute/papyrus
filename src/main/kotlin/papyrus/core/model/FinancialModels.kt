package papyrus.core.model

import kotlinx.serialization.Serializable

@Serializable
data class FinancialMetric(
        val name: String,
        val value: String,
        val rawValue: String? = null,
        val context: String = ""
) {
    fun getRawValueBigDecimal(): java.math.BigDecimal? = rawValue?.let { java.math.BigDecimal(it) }
}

@Serializable
data class FinancialRatio(
        val name: String,
        val value: String,
        val formattedValue: String,
        val description: String,
        val interpretation: String,
        val healthStatus: HealthStatus,
        val category: RatioCategory
) {
    fun getValueBigDecimal(): java.math.BigDecimal = java.math.BigDecimal(value)
}

@Serializable
enum class HealthStatus {
    EXCELLENT,
    GOOD,
    NEUTRAL,
    CAUTION,
    WARNING
}

@Serializable
enum class RatioCategory {
    PROFITABILITY,
    LIQUIDITY,
    SOLVENCY,
    EFFICIENCY,
    VALUATION
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
    NONE
}

@Serializable
enum class PeriodType {
    QUARTERLY,
    ANNUAL,
    YTD,
    TTM
}

@Serializable
data class ExtendedFinancialMetric(
        val name: String,
        val value: String,
        val rawValue: String? = null,
        val unit: MetricUnit = MetricUnit.DOLLARS,
        val period: String? = null,
        val periodType: PeriodType? = null,
        val category: MetricCategory = MetricCategory.OTHER,
        val source: String = "",
        val confidence: Double = 1.0,
        val yearOverYearChange: String? = null,
        val context: String = ""
) {
    fun getRawValueBigDecimal(): java.math.BigDecimal? = rawValue?.let { java.math.BigDecimal(it) }
    fun getYoyChangeBigDecimal(): java.math.BigDecimal? =
            yearOverYearChange?.let { java.math.BigDecimal(it) }
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

@Serializable
data class BeginnerInsight(
        val title: String,
        val icon: String, // Material Icon name (e.g., "Business", "AttachMoney", "TrendingUp")
        val summary: String,
        val detailedExplanation: String,
        val whatItMeans: String,
        val whyItMatters: String,
        val actionableAdvice: String,
        val relatedTerms: List<FinancialTermExplanation> = emptyList()
)

@Serializable
data class FinancialTermExplanation(
        val term: String,
        val simpleDefinition: String,
        val analogy: String, // Real-life analogy
        val example: String
)

typealias DocumentSentimentSummary = com.pascal.institute.ahmes.ai.DocumentSentimentSummary

typealias RiskAnalysis = com.pascal.institute.ahmes.ai.RiskAnalysis

typealias DocumentSummary = com.pascal.institute.ahmes.ai.DocumentSummary

typealias SectionClassification = com.pascal.institute.ahmes.ai.SectionClassification

typealias FinancialEntityAi = com.pascal.institute.ahmes.ai.FinancialEntity

@Serializable
data class MonetaryValue(
        val amount: String,
        val formatted: String,
        val currency: String = "USD",
        val originalUnit: MetricUnit = MetricUnit.DOLLARS,
        val isNegative: Boolean = false,
        val yearOverYearChange: String? = null,
        val confidence: Double = 1.0,
        val source: String = ""
) {
    companion object {
        fun fromBigDecimal(
                value: java.math.BigDecimal,
                unit: MetricUnit = MetricUnit.DOLLARS,
                yoyChange: java.math.BigDecimal? = null,
                confidence: Double = 1.0,
                source: String = ""
        ): MonetaryValue {
            val absValue = value.abs()
            val formatted =
                    when {
                        absValue >= java.math.BigDecimal("1000000000") ->
                                "$${absValue.divide(java.math.BigDecimal("1000000000"), 2, java.math.RoundingMode.HALF_UP)}B"
                        absValue >= java.math.BigDecimal("1000000") ->
                                "$${absValue.divide(java.math.BigDecimal("1000000"), 2, java.math.RoundingMode.HALF_UP)}M"
                        absValue >= java.math.BigDecimal("1000") ->
                                "$${absValue.divide(java.math.BigDecimal("1000"), 2, java.math.RoundingMode.HALF_UP)}K"
                        else -> "$${absValue.setScale(2, java.math.RoundingMode.HALF_UP)}"
                    }
            return MonetaryValue(
                    amount = value.setScale(2, java.math.RoundingMode.HALF_UP).toString(),
                    formatted = if (value < java.math.BigDecimal.ZERO) "-$formatted" else formatted,
                    currency = "USD",
                    originalUnit = unit,
                    isNegative = value < java.math.BigDecimal.ZERO,
                    yearOverYearChange =
                            yoyChange?.setScale(2, java.math.RoundingMode.HALF_UP)?.toString(),
                    confidence = confidence,
                    source = source
            )
        }
    }
    fun toBigDecimal(): java.math.BigDecimal = java.math.BigDecimal(amount)
    fun getYoyChangeBigDecimal(): java.math.BigDecimal? =
            yearOverYearChange?.let { java.math.BigDecimal(it) }
}

@Serializable
data class KeyFinancialMetrics(
        val grossMargin: Double? = null,
        val operatingMargin: Double? = null,
        val netProfitMargin: Double? = null,
        val returnOnAssets: Double? = null,
        val returnOnEquity: Double? = null,
        val currentRatio: Double? = null,
        val quickRatio: Double? = null,
        val cashRatio: Double? = null,
        val debtToEquity: Double? = null,
        val debtRatio: Double? = null,
        val interestCoverage: Double? = null,
        val assetTurnover: Double? = null,
        val inventoryTurnover: Double? = null,
        val receivablesTurnover: Double? = null,
        val revenueGrowth: Double? = null,
        val netIncomeGrowth: Double? = null,
        val epsGrowth: Double? = null
)

@Serializable
data class StructuredIncomeStatement(
        val periodEnding: String?,
        val periodType: PeriodType?,
        val totalRevenue: MonetaryValue? = null,
        val productRevenue: MonetaryValue? = null,
        val serviceRevenue: MonetaryValue? = null,
        val costOfRevenue: MonetaryValue? = null,
        val grossProfit: MonetaryValue? = null,
        val researchAndDevelopment: MonetaryValue? = null,
        val sellingGeneralAdmin: MonetaryValue? = null,
        val totalOperatingExpenses: MonetaryValue? = null,
        val operatingIncome: MonetaryValue? = null,
        val interestExpense: MonetaryValue? = null,
        val interestIncome: MonetaryValue? = null,
        val otherIncome: MonetaryValue? = null,
        val incomeBeforeTax: MonetaryValue? = null,
        val incomeTaxExpense: MonetaryValue? = null,
        val netIncome: MonetaryValue? = null,
        val basicEPS: Double? = null,
        val dilutedEPS: Double? = null,
        val basicSharesOutstanding: Long? = null,
        val dilutedSharesOutstanding: Long? = null
)

@Serializable
data class StructuredBalanceSheet(
        val periodEnding: String?,
        val cashAndEquivalents: MonetaryValue? = null,
        val shortTermInvestments: MonetaryValue? = null,
        val accountsReceivable: MonetaryValue? = null,
        val inventory: MonetaryValue? = null,
        val prepaidExpenses: MonetaryValue? = null,
        val otherCurrentAssets: MonetaryValue? = null,
        val totalCurrentAssets: MonetaryValue? = null,
        val propertyPlantEquipment: MonetaryValue? = null,
        val longTermInvestments: MonetaryValue? = null,
        val goodwill: MonetaryValue? = null,
        val intangibleAssets: MonetaryValue? = null,
        val deferredTaxAssets: MonetaryValue? = null,
        val otherNonCurrentAssets: MonetaryValue? = null,
        val totalAssets: MonetaryValue? = null,
        val accountsPayable: MonetaryValue? = null,
        val shortTermDebt: MonetaryValue? = null,
        val accruedExpenses: MonetaryValue? = null,
        val deferredRevenue: MonetaryValue? = null,
        val otherCurrentLiabilities: MonetaryValue? = null,
        val totalCurrentLiabilities: MonetaryValue? = null,
        val longTermDebt: MonetaryValue? = null,
        val deferredTaxLiabilities: MonetaryValue? = null,
        val otherNonCurrentLiabilities: MonetaryValue? = null,
        val totalLiabilities: MonetaryValue? = null,
        val commonStock: MonetaryValue? = null,
        val retainedEarnings: MonetaryValue? = null,
        val accumulatedOtherComprehensiveIncome: MonetaryValue? = null,
        val totalStockholdersEquity: MonetaryValue? = null,
        val totalLiabilitiesAndEquity: MonetaryValue? = null
)

@Serializable
data class StructuredCashFlowStatement(
        val periodEnding: String?,
        val periodType: PeriodType?,
        val netIncome: MonetaryValue? = null,
        val depreciation: MonetaryValue? = null,
        val stockBasedCompensation: MonetaryValue? = null,
        val changesInWorkingCapital: MonetaryValue? = null,
        val netCashFromOperating: MonetaryValue? = null,
        val capitalExpenditures: MonetaryValue? = null,
        val purchaseOfInvestments: MonetaryValue? = null,
        val saleOfInvestments: MonetaryValue? = null,
        val acquisitions: MonetaryValue? = null,
        val netCashFromInvesting: MonetaryValue? = null,
        val dividendsPaid: MonetaryValue? = null,
        val shareRepurchases: MonetaryValue? = null,
        val debtRepayment: MonetaryValue? = null,
        val debtIssuance: MonetaryValue? = null,
        val netCashFromFinancing: MonetaryValue? = null,
        val netChangeInCash: MonetaryValue? = null,
        val beginningCash: MonetaryValue? = null,
        val endingCash: MonetaryValue? = null,
        val freeCashFlow: MonetaryValue? = null
)

@Serializable
data class StructuredFinancialData(
        val companyName: String? = null,
        val reportType: String? = null,
        val fiscalYear: String? = null,
        val fiscalPeriod: String? = null,
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

@Serializable
data class ManagementDiscussion(
        val keyBusinessDrivers: List<String>,
        val marketConditions: String,
        val futureOutlook: String,
        val criticalAccountingPolicies: List<String>
)

@Serializable
data class XbrlCompanyFact(
        val concept: String,
        val label: String,
        val unit: String,
        val periodEnd: String?,
        val value: String
)

@Serializable
data class FinancialHealthScore(
        val overallScore: Int,
        val grade: String,
        val summary: String,
        val strengths: List<String> = emptyList(),
        val weaknesses: List<String> = emptyList(),
        val recommendations: List<String> = emptyList()
)

@Serializable
data class FinancialAnalysis(
        val fileName: String,
        val companyName: String?,
        val reportType: String?,
        val periodEnding: String?,
        val cik: Int? = null,
        val metrics: List<FinancialMetric>,
        val rawContent: String,
        val summary: String,
        val ratios: List<FinancialRatio> = emptyList(),
        val beginnerInsights: List<BeginnerInsight> = emptyList(),
        val termExplanations: List<FinancialTermExplanation> = emptyList(),
        val healthScore: FinancialHealthScore? = null,
        val reportTypeExplanation: String? = null,
        val keyTakeaways: List<String> = emptyList(),
        val extendedMetrics: List<ExtendedFinancialMetric> = emptyList(),

        // Enhanced financial information (AGENTS.md principle 3 & 5)
        val segmentAnalysis: List<SegmentRevenue> = emptyList(), // Revenue analysis by segment
        val managementDiscussion: ManagementDiscussion? =
                null, // Management discussion and analysis

        // XBRL / iXBRL extracted metrics
        val xbrlMetrics: List<ExtendedFinancialMetric> = emptyList(),

        // AI-Enhanced Insights (NEW)
        val aiSentiment: DocumentSentimentSummary? = null,
        val aiEntities: List<FinancialEntityAi> = emptyList(),
        val aiRiskAnalysis: List<RiskAnalysis> = emptyList(),
        val aiDocumentSummary: DocumentSummary? = null,
        val aiSectionClassifications: Map<String, SectionClassification> = emptyMap(),
        val aiModelUsed: String? = null,
        val aiProcessingTimeMs: Long? = null
)
