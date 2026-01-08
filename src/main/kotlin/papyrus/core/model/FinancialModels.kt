package papyrus.core.model

import kotlinx.serialization.Serializable

typealias FinancialMetric = com.pascal.institute.ahmes.model.FinancialMetric
typealias FinancialRatio = com.pascal.institute.ahmes.model.FinancialRatio
typealias HealthStatus = com.pascal.institute.ahmes.model.HealthStatus
typealias RatioCategory = com.pascal.institute.ahmes.model.RatioCategory

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

typealias FinancialHealthScore = com.pascal.institute.ahmes.model.FinancialHealthScore

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
