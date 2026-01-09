package papyrus.core.model

import kotlinx.serialization.Serializable
import com.pascal.institute.ahmes.util.SecSectionType

/**
 * AI-powered financial sentiment analysis result
 * Uses FinBERT model for sentiment classification
 */
@Serializable
data class FinancialSentiment(
    val sentiment: String, // "positive", "negative", "neutral"
    val confidence: Double, // 0.0 to 1.0
    val highlights: List<String> = emptyList() // Key phrases that influenced the sentiment
)

/**
 * Financial entity extracted using NER (Named Entity Recognition)
 * Examples: Revenue figures, profit margins, company names, dates
 */
@Serializable
data class FinancialEntity(
    val text: String, // Original text: "Total Revenue: $1.5M"
    val type: String, // "MONEY", "PERCENT", "ORG", "DATE", "METRIC"
    val value: Double?, // Normalized numeric value: 1500000.0
    val unit: String?, // "USD", "percent", null
    val context: String, // Surrounding sentence for context
    val confidence: Double // 0.0 to 1.0
)

/**
 * AI-enhanced section of SEC filing
 * Automatically classified and analyzed
 */
@Serializable
data class SecSection(
    val title: String, // "Management Discussion & Analysis", "Risk Factors"
    val type: SecSectionType,
    val content: String,
    val sentiment: FinancialSentiment? = null,
    val entities: List<FinancialEntity> = emptyList(),
    val keyInsights: List<String> = emptyList()
)

/**
 * AI-enhanced financial analysis result
 * Combines traditional parsing with AI insights
 */
@Serializable
data class AIFinancialAnalysis(
    val traditionalMetrics: List<FinancialMetric>,
    val aiExtractedEntities: List<FinancialEntity>,
    val mdaSentiment: FinancialSentiment? = null,
    val sections: List<SecSection> = emptyList(),
    val overallRiskLevel: String? = null, // "LOW", "MEDIUM", "HIGH"
    val aiConfidence: Double // Overall confidence in AI extractions
)
