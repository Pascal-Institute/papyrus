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

// NOTE: Avoid circular dependencies or missing types if AiAnalysisResult is undefined still.
// I will check AiAnalysisService.kt content. For now I'll comment out AiAnalysisResult if not
// found,
// but I saw it in FinancialAnalysis in FinancialData.kt.
// Wait, AiAnalysisResult was imported? Let me check FinancialData.kt again.
// It seems AiAnalysisResult is likely in AiAnalysisService.kt. I need to make sure I have that
// model.

// Assuming AiAnalysisResult is in another file, I might need to move it or import it.
// Actually, looking at reference, AiAnalysisResult is used in FinancialAnalysis.
// I will create a placeholder or move AiAnalysisResult if I can find it.
// Checking FileUtils list earlier: AiAnalysisService.kt exists.
// I'll assume it's defined there. I will move it to AiModels.kt later.

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
        val managementDiscussion: ManagementDiscussion? = null, // Management discussion and analysis

        // AI Analysis
        val aiAnalysis: AiAnalysisResult? = null,
        val aiSummary: String? = null,
        val industryComparison: String? = null,
        val investmentAdvice: String? = null,

        // XBRL / iXBRL extracted metrics
        val xbrlMetrics: List<ExtendedFinancialMetric> = emptyList()
)
// Correction: In FinancialData.kt line 97: val aiAnalysis: AiAnalysisResult? = null
// I need to find where AiAnalysisResult is defined.

@Serializable
data class AiAnalysisResult(
        val success: Boolean = true,
        val provider: String = "OpenRouter",
        val model: String = "meta-llama/llama-3.1-8b-instruct:free",
        val summary: String,
        val keyInsights: List<String>,
        val recommendations: List<String>,
        val riskAssessment: String,
        val confidence: Double
)
