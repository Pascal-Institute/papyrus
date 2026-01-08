package com.pascal.institute.ahmes.ai

import ai.djl.modality.Classifications
import ai.djl.modality.nlp.qa.QAInput
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Sentiment Analysis Result for SEC Documents
 */
@Serializable
data class SentimentResult(
    val text: String,
    val sentiment: String, // POSITIVE, NEGATIVE, NEUTRAL
    val confidence: String, // BigDecimal as String for precision
    val details: Map<String, String> = emptyMap() // class -> probability
) {
    fun getConfidenceBigDecimal(): BigDecimal = BigDecimal(confidence)

    val isPositive: Boolean get() = sentiment == "POSITIVE"
    val isNegative: Boolean get() = sentiment == "NEGATIVE"
    val isNeutral: Boolean get() = sentiment == "NEUTRAL"
}

/**
 * Risk Factor Analysis Result
 */
@Serializable
data class RiskAnalysis(
    val riskFactor: String,
    val severity: RiskSeverity,
    val sentiment: SentimentResult,
    val category: String,
    val keywords: List<String> = emptyList()
)

@Serializable
enum class RiskSeverity {
    CRITICAL,   // Severe risk that could significantly impact business
    HIGH,       // Major risk requiring attention
    MEDIUM,     // Moderate risk
    LOW,        // Minor risk
    INFORMATIONAL // FYI type disclosure
}

/**
 * SEC Sentiment Analyzer
 *
 * Uses DJL deep learning models to analyze sentiment and risk in SEC filings.
 * Provides investment-relevant insights from financial text.
 */
object SecSentimentAnalyzer {

    private val logger = LoggerFactory.getLogger(SecSentimentAnalyzer::class.java)

    // Risk-related keywords for categorization
    private val riskCategories = mapOf(
        "MARKET" to listOf("market", "competition", "competitive", "pricing", "demand", "economic"),
        "REGULATORY" to listOf("regulatory", "regulation", "compliance", "government", "legal", "law"),
        "OPERATIONAL" to listOf("operational", "supply chain", "manufacturing", "production", "labor"),
        "FINANCIAL" to listOf("financial", "liquidity", "debt", "credit", "interest rate", "currency"),
        "TECHNOLOGY" to listOf("technology", "cybersecurity", "cyber", "data breach", "IT", "software"),
        "ENVIRONMENTAL" to listOf("environmental", "climate", "sustainability", "emissions", "ESG"),
        "GEOPOLITICAL" to listOf("geopolitical", "war", "conflict", "sanctions", "tariff", "trade")
    )

    // Keywords indicating severity
    private val severityKeywords = mapOf(
        RiskSeverity.CRITICAL to listOf(
            "material adverse", "significant harm", "bankruptcy", "insolvency",
            "going concern", "catastrophic", "existential"
        ),
        RiskSeverity.HIGH to listOf(
            "substantial", "significant risk", "major impact", "adversely affect",
            "material impact", "severely", "significantly harm"
        ),
        RiskSeverity.MEDIUM to listOf(
            "could affect", "may impact", "potential risk", "possible",
            "moderate", "uncertain"
        ),
        RiskSeverity.LOW to listOf(
            "minor", "limited", "manageable", "unlikely", "remote"
        )
    )

    /**
     * Analyze sentiment of text using DJL model
     */
    fun analyzeSentiment(text: String): SentimentResult {
        // Truncate text for model input (most models have 512 token limit)
        val truncatedText = text.take(1000)

        return try {
            val model = DjlModelManager.getSentimentModel()
            if (model != null) {
                model.newPredictor().use { predictor ->
                    val result = predictor.predict(truncatedText)
                    classificationToSentiment(truncatedText, result)
                }
            } else {
                // Fallback to rule-based sentiment
                ruleBasedSentiment(truncatedText)
            }
        } catch (e: Exception) {
            logger.warn("Sentiment analysis failed, using fallback: ${e.message}")
            ruleBasedSentiment(truncatedText)
        }
    }

    /**
     * Analyze multiple texts in batch (more efficient)
     */
    fun analyzeSentimentBatch(texts: List<String>): List<SentimentResult> {
        return texts.map { analyzeSentiment(it) }
    }

    /**
     * Analyze risk factors from SEC filing
     */
    fun analyzeRiskFactors(riskFactors: List<String>): List<RiskAnalysis> {
        return riskFactors.map { riskFactor ->
            val sentiment = analyzeSentiment(riskFactor)
            val category = categorizeRisk(riskFactor)
            val severity = assessSeverity(riskFactor, sentiment)
            val keywords = extractRiskKeywords(riskFactor)

            RiskAnalysis(
                riskFactor = riskFactor.take(500), // Truncate for storage
                severity = severity,
                sentiment = sentiment,
                category = category,
                keywords = keywords
            )
        }
    }

    /**
     * Get overall document sentiment summary
     */
    fun getDocumentSentimentSummary(sections: Map<String, String>): DocumentSentimentSummary {
        val sectionSentiments = sections.mapValues { (_, content) ->
            analyzeSentiment(content)
        }

        val overallPositive = sectionSentiments.values.count { it.isPositive }
        val overallNegative = sectionSentiments.values.count { it.isNegative }
        val overallNeutral = sectionSentiments.values.count { it.isNeutral }
        val total = sectionSentiments.size.coerceAtLeast(1)

        val averageConfidence = sectionSentiments.values
            .map { it.getConfidenceBigDecimal() }
            .fold(BigDecimal.ZERO) { acc, bd -> acc.add(bd) }
            .divide(BigDecimal(total), 4, RoundingMode.HALF_UP)

        val overallSentiment = when {
            overallPositive > overallNegative + overallNeutral -> "POSITIVE"
            overallNegative > overallPositive + overallNeutral -> "NEGATIVE"
            else -> "MIXED"
        }

        return DocumentSentimentSummary(
            overallSentiment = overallSentiment,
            positiveRatio = BigDecimal(overallPositive).divide(BigDecimal(total), 4, RoundingMode.HALF_UP).toPlainString(),
            negativeRatio = BigDecimal(overallNegative).divide(BigDecimal(total), 4, RoundingMode.HALF_UP).toPlainString(),
            neutralRatio = BigDecimal(overallNeutral).divide(BigDecimal(total), 4, RoundingMode.HALF_UP).toPlainString(),
            averageConfidence = averageConfidence.toPlainString(),
            sectionSentiments = sectionSentiments
        )
    }

    private fun classificationToSentiment(text: String, classifications: Classifications): SentimentResult {
        val best = classifications.best<Classifications.Classification>()
        val sentiment = when (best.className.uppercase()) {
            "POSITIVE", "POS", "1" -> "POSITIVE"
            "NEGATIVE", "NEG", "0" -> "NEGATIVE"
            else -> "NEUTRAL"
        }

        val details = classifications.items<Classifications.Classification>()
            .associate { it.className to BigDecimal(it.probability).setScale(4, RoundingMode.HALF_UP).toPlainString() }

        return SentimentResult(
            text = text.take(200),
            sentiment = sentiment,
            confidence = BigDecimal(best.probability).setScale(4, RoundingMode.HALF_UP).toPlainString(),
            details = details
        )
    }

    private fun ruleBasedSentiment(text: String): SentimentResult {
        val lowerText = text.lowercase()

        val positiveWords = listOf(
            "growth", "increase", "profit", "success", "strong", "improve",
            "exceed", "outperform", "opportunity", "innovation", "expand"
        )
        val negativeWords = listOf(
            "risk", "loss", "decline", "decrease", "adverse", "uncertain",
            "challenge", "difficult", "threat", "volatility", "impairment"
        )

        val positiveCount = positiveWords.count { lowerText.contains(it) }
        val negativeCount = negativeWords.count { lowerText.contains(it) }

        val (sentiment, confidence) = when {
            positiveCount > negativeCount * 2 -> "POSITIVE" to 0.7
            negativeCount > positiveCount * 2 -> "NEGATIVE" to 0.7
            positiveCount > negativeCount -> "POSITIVE" to 0.55
            negativeCount > positiveCount -> "NEGATIVE" to 0.55
            else -> "NEUTRAL" to 0.5
        }

        return SentimentResult(
            text = text.take(200),
            sentiment = sentiment,
            confidence = BigDecimal(confidence).setScale(4, RoundingMode.HALF_UP).toPlainString(),
            details = mapOf("method" to "rule-based")
        )
    }

    private fun categorizeRisk(riskText: String): String {
        val lowerText = riskText.lowercase()

        for ((category, keywords) in riskCategories) {
            if (keywords.any { lowerText.contains(it) }) {
                return category
            }
        }
        return "GENERAL"
    }

    private fun assessSeverity(riskText: String, sentiment: SentimentResult): RiskSeverity {
        val lowerText = riskText.lowercase()

        for ((severity, keywords) in severityKeywords) {
            if (keywords.any { lowerText.contains(it) }) {
                return severity
            }
        }

        // Use sentiment as fallback
        return when {
            sentiment.isNegative && sentiment.getConfidenceBigDecimal() > BigDecimal("0.8") -> RiskSeverity.HIGH
            sentiment.isNegative -> RiskSeverity.MEDIUM
            else -> RiskSeverity.LOW
        }
    }

    private fun extractRiskKeywords(riskText: String): List<String> {
        val lowerText = riskText.lowercase()
        val keywords = mutableListOf<String>()

        riskCategories.values.flatten().forEach { keyword ->
            if (lowerText.contains(keyword)) {
                keywords.add(keyword)
            }
        }

        severityKeywords.values.flatten().forEach { keyword ->
            if (lowerText.contains(keyword)) {
                keywords.add(keyword)
            }
        }

        return keywords.distinct().take(10)
    }
}

/**
 * Document-level sentiment summary
 */
@Serializable
data class DocumentSentimentSummary(
    val overallSentiment: String,
    val positiveRatio: String, // BigDecimal as String
    val negativeRatio: String,
    val neutralRatio: String,
    val averageConfidence: String,
    val sectionSentiments: Map<String, SentimentResult>
)
