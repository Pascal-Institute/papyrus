package papyrus.core.model

import kotlinx.serialization.Serializable

/** 확장된 재무 메트릭 - 더 상세한 정보 포함 */
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
    // Income Statement (손익계산서)
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

    // Balance Sheet - Assets (재무상태표 - 자산)
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

    // Balance Sheet - Liabilities (재무상태표 - 부채)
    TOTAL_LIABILITIES,
    CURRENT_LIABILITIES,
    LONG_TERM_DEBT,
    ACCOUNTS_PAYABLE,
    ACCRUED_EXPENSES,
    OPERATING_LEASE,
    LONG_TERM_LEASE,
    DEFERRED_REVENUE,

    // Balance Sheet - Equity (재무상태표 - 자본)
    TOTAL_EQUITY,
    RETAINED_EARNINGS,

    // Cash Flow (현금흐름표)
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

/** 재무제표 섹션 */
@Serializable
data class FinancialStatement(
        val type: StatementType,
        val periodEnding: String?,
        val periodType: PeriodType?,
        val metrics: List<ExtendedFinancialMetric>,
        val rawSection: String = ""
)

enum class StatementType {
    INCOME_STATEMENT, // 손익계산서
    BALANCE_SHEET, // 재무상태표
    CASH_FLOW_STATEMENT, // 현금흐름표
    COMPREHENSIVE_INCOME, // 포괄손익계산서
    EQUITY_STATEMENT // 자본변동표
}

/** 위험 요소 */
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

/** 경영진 정보 */
@Serializable
data class ExecutiveInfo(val name: String, val title: String, val compensation: Double? = null)

/** 산업 정보 */
@Serializable
data class IndustryInfo(
        val sicCode: String?,
        val sicDescription: String?,
        val sector: String?,
        val industry: String?
)

// ========================================
// 세그먼트 분석 및 MD&A 모델
// ========================================

/** 세그먼트별 매출 정보 */
@Serializable
data class SegmentRevenue(
        val segmentName: String, // 세그먼트 이름 (예: "Americas", "iPhone")
        val segmentType: SegmentType, // 세그먼트 유형
        val revenue: Double, // 매출액
        val percentOfTotal: Double? = null, // 전체 매출 대비 비율 (%)
        val operatingIncome: Double? = null, // 영업이익 (있는 경우)
        val source: String = "" // 데이터 출처 (페이지/라인 번호)
)

enum class SegmentType {
    GEOGRAPHIC, // 지역별
    PRODUCT, // 제품별
    SERVICE, // 서비스별
    CUSTOMER, // 고객 유형별
    OTHER
}

/** 경영진 논의 및 분석 (MD&A) */
@Serializable
data class ManagementDiscussion(
        val keyBusinessDrivers: List<String>, // 핵심 비즈니스 동인
        val marketConditions: String, // 시장 상황
        val futureOutlook: String, // 향후 전망
        val criticalAccountingPolicies: List<String> // 중요한 회계 정책
)

// ========================================
// 구조화된 재무제표 모델 (Structured Financial Statements)
// ========================================

/** 완전한 재무제표 세트 손익계산서, 재무상태표, 현금흐름표를 구조화하여 담습니다. */
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
        val parsingConfidence: Double = 0.0, // 0-1, 파싱 신뢰도
        val dataQuality: DataQuality = DataQuality.UNKNOWN
)

enum class DataQuality {
    HIGH, // 테이블 파싱 성공, 모든 주요 항목 존재
    MEDIUM, // 일부 항목 누락 또는 패턴 파싱
    LOW, // 대부분 패턴 파싱, 신뢰도 낮음
    UNKNOWN
}

/** 구조화된 손익계산서 (Income Statement) */
@Serializable
data class StructuredIncomeStatement(
        val periodEnding: String?,
        val periodType: PeriodType?,

        // 매출
        val totalRevenue: MonetaryValue? = null,
        val productRevenue: MonetaryValue? = null,
        val serviceRevenue: MonetaryValue? = null,

        // 비용
        val costOfRevenue: MonetaryValue? = null,
        val grossProfit: MonetaryValue? = null,

        // 영업비용
        val researchAndDevelopment: MonetaryValue? = null,
        val sellingGeneralAdmin: MonetaryValue? = null,
        val totalOperatingExpenses: MonetaryValue? = null,

        // 이익
        val operatingIncome: MonetaryValue? = null,
        val interestExpense: MonetaryValue? = null,
        val interestIncome: MonetaryValue? = null,
        val otherIncome: MonetaryValue? = null,
        val incomeBeforeTax: MonetaryValue? = null,
        val incomeTaxExpense: MonetaryValue? = null,
        val netIncome: MonetaryValue? = null,

        // 주당 지표
        val basicEPS: Double? = null,
        val dilutedEPS: Double? = null,
        val basicSharesOutstanding: Long? = null,
        val dilutedSharesOutstanding: Long? = null
)

/** 구조화된 재무상태표 (Balance Sheet) */
@Serializable
data class StructuredBalanceSheet(
        val periodEnding: String?,

        // 유동자산
        val cashAndEquivalents: MonetaryValue? = null,
        val shortTermInvestments: MonetaryValue? = null,
        val accountsReceivable: MonetaryValue? = null,
        val inventory: MonetaryValue? = null,
        val prepaidExpenses: MonetaryValue? = null,
        val otherCurrentAssets: MonetaryValue? = null,
        val totalCurrentAssets: MonetaryValue? = null,

        // 비유동자산
        val propertyPlantEquipment: MonetaryValue? = null,
        val longTermInvestments: MonetaryValue? = null,
        val goodwill: MonetaryValue? = null,
        val intangibleAssets: MonetaryValue? = null,
        val deferredTaxAssets: MonetaryValue? = null,
        val otherNonCurrentAssets: MonetaryValue? = null,
        val totalAssets: MonetaryValue? = null,

        // 유동부채
        val accountsPayable: MonetaryValue? = null,
        val shortTermDebt: MonetaryValue? = null,
        val accruedExpenses: MonetaryValue? = null,
        val deferredRevenue: MonetaryValue? = null,
        val otherCurrentLiabilities: MonetaryValue? = null,
        val totalCurrentLiabilities: MonetaryValue? = null,

        // 비유동부채
        val longTermDebt: MonetaryValue? = null,
        val deferredTaxLiabilities: MonetaryValue? = null,
        val otherNonCurrentLiabilities: MonetaryValue? = null,
        val totalLiabilities: MonetaryValue? = null,

        // 자본
        val commonStock: MonetaryValue? = null,
        val retainedEarnings: MonetaryValue? = null,
        val accumulatedOtherComprehensiveIncome: MonetaryValue? = null,
        val totalStockholdersEquity: MonetaryValue? = null,
        val totalLiabilitiesAndEquity: MonetaryValue? = null
)

/** 구조화된 현금흐름표 (Cash Flow Statement) */
@Serializable
data class StructuredCashFlowStatement(
        val periodEnding: String?,
        val periodType: PeriodType?,

        // 영업활동 현금흐름
        val netIncome: MonetaryValue? = null,
        val depreciation: MonetaryValue? = null,
        val stockBasedCompensation: MonetaryValue? = null,
        val changesInWorkingCapital: MonetaryValue? = null,
        val netCashFromOperating: MonetaryValue? = null,

        // 투자활동 현금흐름
        val capitalExpenditures: MonetaryValue? = null,
        val purchaseOfInvestments: MonetaryValue? = null,
        val saleOfInvestments: MonetaryValue? = null,
        val acquisitions: MonetaryValue? = null,
        val netCashFromInvesting: MonetaryValue? = null,

        // 재무활동 현금흐름
        val dividendsPaid: MonetaryValue? = null,
        val shareRepurchases: MonetaryValue? = null,
        val debtRepayment: MonetaryValue? = null,
        val debtIssuance: MonetaryValue? = null,
        val netCashFromFinancing: MonetaryValue? = null,

        // 요약
        val netChangeInCash: MonetaryValue? = null,
        val beginningCash: MonetaryValue? = null,
        val endingCash: MonetaryValue? = null,

        // 계산된 지표
        val freeCashFlow: MonetaryValue? = null
)

/** 금액 값 (정밀도 유지) */
@Serializable
data class MonetaryValue(
        val amount: Double, // 실제 금액 (달러 단위로 정규화)
        val formatted: String, // 표시용 포맷 (예: "$1.23B")
        val originalUnit: MetricUnit, // 원본 문서의 단위
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

/** 핵심 재무 지표 (계산된 비율) */
@Serializable
data class KeyFinancialMetrics(
        // 수익성
        val grossMargin: Double? = null, // 매출총이익률
        val operatingMargin: Double? = null, // 영업이익률
        val netProfitMargin: Double? = null, // 순이익률
        val returnOnAssets: Double? = null, // ROA
        val returnOnEquity: Double? = null, // ROE

        // 유동성
        val currentRatio: Double? = null, // 유동비율
        val quickRatio: Double? = null, // 당좌비율
        val cashRatio: Double? = null, // 현금비율

        // 지급능력
        val debtToEquity: Double? = null, // 부채비율
        val debtRatio: Double? = null, // 총부채비율
        val interestCoverage: Double? = null, // 이자보상배율

        // 효율성
        val assetTurnover: Double? = null, // 자산회전율
        val inventoryTurnover: Double? = null, // 재고회전율
        val receivablesTurnover: Double? = null, // 매출채권회전율

        // 성장성
        val revenueGrowth: Double? = null, // 매출 성장률 (YoY)
        val netIncomeGrowth: Double? = null, // 순이익 성장률 (YoY)
        val epsGrowth: Double? = null // EPS 성장률 (YoY)
)

/** SEC 보고서 메타데이터 */
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

/** 파싱 결과 요약 */
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
