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

    // Balance Sheet (재무상태표)
    TOTAL_ASSETS,
    CURRENT_ASSETS,
    CASH_AND_EQUIVALENTS,
    ACCOUNTS_RECEIVABLE,
    INVENTORY,
    TOTAL_LIABILITIES,
    CURRENT_LIABILITIES,
    LONG_TERM_DEBT,
    TOTAL_EQUITY,
    RETAINED_EARNINGS,

    // Cash Flow (현금흐름표)
    OPERATING_CASH_FLOW,
    INVESTING_CASH_FLOW,
    FINANCING_CASH_FLOW,
    FREE_CASH_FLOW,
    CAPITAL_EXPENDITURES,

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
