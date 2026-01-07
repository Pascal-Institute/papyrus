package papyrus.core.model

import kotlinx.serialization.Serializable

@Serializable
data class FinancialMetric(
        val name: String,
        val value: String,
        val rawValue: Double? = null,
        val context: String = ""
)

@Serializable
data class FinancialRatio(
        val name: String,
        val value: Double,
        val formattedValue: String,
        val description: String,
        val interpretation: String,
        val healthStatus: HealthStatus,
        val category: RatioCategory
)

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
        PROFITABILITY, // Profitability
        LIQUIDITY, // Liquidity
        SOLVENCY, // Solvency
        EFFICIENCY, // Efficiency
        VALUATION // Valuation
}

@Serializable
data class BeginnerInsight(
        val title: String,
        val emoji: String,
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
        val xbrlMetrics: List<ExtendedFinancialMetric> = emptyList()
)
