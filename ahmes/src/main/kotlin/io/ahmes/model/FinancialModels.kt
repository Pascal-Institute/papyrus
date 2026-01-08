package io.ahmes.model

import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class FinancialMetric(
    val name: String,
    val value: String,
    val rawValue: String? = null, // BigDecimal stored as String for precision
    val context: String = ""
) {
    /** Get rawValue as BigDecimal for calculations */
    fun getRawValueBigDecimal(): BigDecimal? = rawValue?.let { BigDecimal(it) }
}

@Serializable
data class FinancialRatio(
    val name: String,
    val value: String, // BigDecimal stored as String for precision
    val formattedValue: String,
    val description: String,
    val interpretation: String,
    val healthStatus: HealthStatus,
    val category: RatioCategory
) {
    /** Get value as BigDecimal for calculations */
    fun getValueBigDecimal(): BigDecimal = BigDecimal(value)

    /** Get value as Double for backward compatibility (use sparingly) */
    @Deprecated("Use getValueBigDecimal() for precision")
    fun getValueDouble(): Double = value.toDouble()
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
data class FinancialHealthScore(
    val overallScore: Int, // 0-100
    val grade: String, // A+, A, B+, B, C, D, F
    val summary: String,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val recommendations: List<String>
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
    val healthScore: FinancialHealthScore? = null,
    val reportTypeExplanation: String? = null,
    val keyTakeaways: List<String> = emptyList(),
    val extendedMetrics: List<ExtendedFinancialMetric> = emptyList(),

    // Enhanced financial information
    val segmentAnalysis: List<SegmentRevenue> = emptyList(),
    val managementDiscussion: ManagementDiscussion? = null,

    // XBRL / iXBRL extracted metrics
    val xbrlMetrics: List<ExtendedFinancialMetric> = emptyList()
)
