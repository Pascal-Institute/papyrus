package com.pascal.institute.ahmes.ai

import com.pascal.institute.ahmes.model.*
import com.pascal.institute.ahmes.parser.ParseResult
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

/** AI-enhanced parsing result */
@Serializable
data class AiEnhancedParseResult(
        // Original parse result data
        val metrics: List<FinancialMetric>,
        val documentName: String,
        val parserType: String,
        val cleanedContent: String,
        val metadata: Map<String, String>,

        // AI enhancements
        val sentiment: DocumentSentimentSummary? = null,
        val entities: List<FinancialEntity> = emptyList(),
        val sectionClassifications: Map<String, SectionClassification> = emptyMap(),
        val riskAnalysis: List<RiskAnalysis> = emptyList(),
        val documentSummary: DocumentSummary? = null,
        val aiConfidence: String = "0.0", // Overall AI confidence score
        val aiModelUsed: String = "rule-based" // Which AI model was used
) {
    fun getAiConfidenceBigDecimal(): BigDecimal = BigDecimal(aiConfidence)
}

/** AI Enhancement Options */
data class AiEnhancementOptions(
        val enableSentimentAnalysis: Boolean = true,
        val enableEntityExtraction: Boolean = true,
        val enableSectionClassification: Boolean = true,
        val enableRiskAnalysis: Boolean = true,
        val enableDocumentSummary: Boolean = true,
        val enableMetricHealing: Boolean = true, // Attempt to find missing metrics via AI
        val maxTextLength: Int = 50000, // Limit for processing
        val minConfidenceThreshold: Double = 0.5 // Minimum confidence to include results
)

/**
 * AI-Enhanced SEC Parser
 *
 * Enhances standard SEC parsing with deep learning capabilities:
 * - Sentiment analysis for risk assessment
 * - Named entity extraction for structured data
 * - Section classification for document understanding
 * - Document summarization for quick insights
 */
object AiEnhancedSecParser {

    private val logger = LoggerFactory.getLogger(AiEnhancedSecParser::class.java)

    /** Enhance a standard parse result with AI analysis */
    fun enhance(
            parseResult: ParseResult,
            options: AiEnhancementOptions = AiEnhancementOptions()
    ): AiEnhancedParseResult {
        val startTime = System.currentTimeMillis()

        val content = parseResult.cleanedContent.take(options.maxTextLength)
        val isAiAvailable = DjlModelManager.isAvailable()
        val aiModel = if (isAiAvailable) "djl-pytorch" else "rule-based"

        logger.info("Enhancing parse result with AI (model: $aiModel)")

        // Entity extraction (always useful)
        val entities =
                if (options.enableEntityExtraction) {
                    SecEntityExtractor.extractEntities(content)
                } else {
                    emptyList()
                }

        // Extract sections for further analysis
        val sections = extractTextSections(content)

        // Sentiment analysis
        val sentiment =
                if (options.enableSentimentAnalysis && sections.isNotEmpty()) {
                    SecSentimentAnalyzer.getDocumentSentimentSummary(sections)
                } else {
                    null
                }

        // Section classification
        val sectionClassifications =
                if (options.enableSectionClassification && sections.isNotEmpty()) {
                    SecSectionClassifier.classifySections(sections)
                } else {
                    emptyMap()
                }

        // Risk analysis
        val riskAnalysis =
                if (options.enableRiskAnalysis) {
                    val riskSection =
                            sections.entries.find { it.key.lowercase().contains("risk") }?.value
                                    ?: ""

                    if (riskSection.isNotEmpty()) {
                        val riskFactors = extractRiskFactorTexts(riskSection)
                        SecSentimentAnalyzer.analyzeRiskFactors(riskFactors)
                    } else {
                        emptyList()
                    }
                } else {
                    emptyList()
                }

        // Document summary
        val documentSummary =
                if (options.enableDocumentSummary) {
                    SecSectionClassifier.generateDocumentSummary(content, sections)
                } else {
                    null
                }

        // Calculate overall AI confidence
        val confidenceScores = mutableListOf<BigDecimal>()
        entities.forEach { confidenceScores.add(it.getConfidenceBigDecimal()) }
        sectionClassifications.values.forEach { confidenceScores.add(it.getConfidenceBigDecimal()) }
        riskAnalysis.forEach { confidenceScores.add(it.sentiment.getConfidenceBigDecimal()) }

        val aiConfidence =
                if (confidenceScores.isNotEmpty()) {
                    confidenceScores
                            .fold(BigDecimal.ZERO) { acc, bd -> acc.add(bd) }
                            .divide(BigDecimal(confidenceScores.size), 4, RoundingMode.HALF_UP)
                            .toPlainString()
                } else {
                    "0.0"
                }

        val processingTime = System.currentTimeMillis() - startTime
        logger.info("AI enhancement completed in ${processingTime}ms")

        // Try to heal missing core metrics if enabled
        val healedMetrics =
                if (options.enableMetricHealing && isAiAvailable) {
                    healMissingMetrics(parseResult.metrics, content)
                } else {
                    emptyList()
                }

        if (healedMetrics.isNotEmpty()) {
            logger.info("Healed ${healedMetrics.size} missing metrics using AI")
        }

        return AiEnhancedParseResult(
                metrics = parseResult.metrics + healedMetrics,
                documentName = parseResult.documentName,
                parserType = parseResult.parserType,
                cleanedContent = content,
                metadata =
                        parseResult.metadata +
                                mapOf(
                                        "aiEnhanced" to "true",
                                        "aiProcessingTimeMs" to processingTime.toString(),
                                        "aiModel" to aiModel
                                ),
                sentiment = sentiment,
                entities = entities,
                sectionClassifications = sectionClassifications,
                riskAnalysis = riskAnalysis,
                documentSummary = documentSummary,
                aiConfidence = aiConfidence,
                aiModelUsed = aiModel
        )
    }

    /** Quick sentiment check without full enhancement */
    fun quickSentimentCheck(text: String): SentimentResult {
        return SecSentimentAnalyzer.analyzeSentiment(text)
    }

    /** Extract entities only (lightweight operation) */
    fun extractEntitiesOnly(text: String): List<FinancialEntity> {
        return SecEntityExtractor.extractEntities(text)
    }

    /** Answer a question about the document */
    fun askQuestion(question: String, documentText: String): QAAnswer {
        return SecEntityExtractor.answerQuestion(question, documentText)
    }

    /** Get AI system status */
    fun getAiStatus(): Map<String, Any> {
        return mapOf(
                "available" to DjlModelManager.isAvailable(),
                "device" to DjlModelManager.getDeviceInfo(),
                "capabilities" to
                        listOf(
                                "sentiment_analysis",
                                "entity_extraction",
                                "section_classification",
                                "risk_analysis",
                                "question_answering",
                                "document_summarization"
                        )
        )
    }

    private fun extractTextSections(text: String): Map<String, String> {
        val sections = mutableMapOf<String, String>()

        // Common SEC section headers
        val sectionPatterns =
                listOf(
                        "ITEM 1\\." to "Item 1 - Business",
                        "ITEM 1A\\." to "Item 1A - Risk Factors",
                        "ITEM 1B\\." to "Item 1B - Unresolved Staff Comments",
                        "ITEM 2\\." to "Item 2 - Properties",
                        "ITEM 3\\." to "Item 3 - Legal Proceedings",
                        "ITEM 4\\." to "Item 4 - Mine Safety",
                        "ITEM 5\\." to "Item 5 - Market",
                        "ITEM 6\\." to "Item 6 - Selected Financial Data",
                        "ITEM 7\\." to "Item 7 - MD&A",
                        "ITEM 7A\\." to "Item 7A - Market Risk",
                        "ITEM 8\\." to "Item 8 - Financial Statements",
                        "ITEM 9\\." to "Item 9 - Changes in Accountants",
                        "ITEM 9A\\." to "Item 9A - Controls",
                        "ITEM 10\\." to "Item 10 - Directors",
                        "ITEM 11\\." to "Item 11 - Executive Compensation"
                )

        val upperText = text.uppercase()

        sectionPatterns.forEachIndexed { index, (pattern, name) ->
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            val match = regex.find(upperText)
            if (match != null) {
                val startIndex = match.range.first

                // Find next section or end of text
                val nextPattern = sectionPatterns.getOrNull(index + 1)?.first
                val endIndex =
                        if (nextPattern != null) {
                            Regex(nextPattern, RegexOption.IGNORE_CASE)
                                    .find(upperText, startIndex + 1)
                                    ?.range
                                    ?.first
                                    ?: text.length
                        } else {
                            text.length
                        }

                val sectionContent = text.substring(startIndex, endIndex.coerceAtMost(text.length))
                sections[name] = sectionContent.take(10000) // Limit section size
            }
        }

        // If no sections found, treat whole text as one section
        if (sections.isEmpty()) {
            sections["Full Document"] = text.take(10000)
        }

        return sections
    }

    private fun extractRiskFactorTexts(riskSection: String): List<String> {
        // Split by bullet points, numbered items, or double newlines
        val riskPatterns =
                listOf(
                        Regex("""•\s*([^•]+)"""),
                        Regex("""(?:^|\n)\s*\d+\.\s*([^\n]+(?:\n(?!\d+\.).[^\n]+)*)"""),
                        Regex("""(?:^|\n)\s*[-]\s*([^\n]+)""")
                )

        val risks = mutableListOf<String>()

        for (pattern in riskPatterns) {
            pattern.findAll(riskSection).forEach { match ->
                val risk = match.groupValues.getOrNull(1)?.trim()
                if (risk != null && risk.length > 50) {
                    risks.add(risk.take(1000))
                }
            }
        }

        // Fallback: split by paragraphs
        if (risks.isEmpty()) {
            riskSection.split("\n\n").filter { it.length > 100 }.take(10).forEach {
                risks.add(it.take(1000))
            }
        }

        return risks.take(20)
    }

    private fun healMissingMetrics(
            existingMetrics: List<FinancialMetric>,
            content: String
    ): List<FinancialMetric> {
        val coreMetrics =
                mapOf(
                        "Revenue" to
                                listOf(
                                        "What is the total revenue or net sales?",
                                        "How much revenue was reported?"
                                ),
                        "Net Income" to
                                listOf(
                                        "What is the net income or net loss?",
                                        "How much was the net income for the period?"
                                ),
                        "Total Assets" to
                                listOf(
                                        "What are the total assets of the company?",
                                        "What is the total asset value?"
                                ),
                        "Total Liabilities" to
                                listOf(
                                        "What are the total liabilities?",
                                        "How much are the total liabilities?"
                                ),
                        "EPS" to
                                listOf(
                                        "What is the earnings per share (EPS)?",
                                        "What was the diluted earnings per share?"
                                )
                )

        val healed = mutableListOf<FinancialMetric>()

        for ((name, questions) in coreMetrics) {
            val alreadyExists = existingMetrics.any { it.name.contains(name, ignoreCase = true) }
            if (!alreadyExists) {
                // Try each question until we find a numeric answer
                for (question in questions) {
                    val answer =
                            askQuestion(
                                    question,
                                    content.take(10000)
                            ) // Focus on first part usually
                    if (answer.confidence.toDoubleOrNull() ?: 0.0 > 0.4) {
                        val extractedValue = tryExtractNumericValue(answer.answer)
                        if (extractedValue != null) {
                            healed.add(
                                    FinancialMetric(
                                            name = "$name (AI)",
                                            value = formatExtractedValue(extractedValue),
                                            rawValue = extractedValue.toPlainString(),
                                            context = answer.answer
                                    )
                            )
                            break // Found it
                        }
                    }
                }
            }
        }

        return healed
    }

    private fun tryExtractNumericValue(text: String): BigDecimal? {
        val cleaned = text.replace(",", "").replace("$", "")
        val match =
                Regex(
                                """([-+]?\d*\.?\d+)\s*(million|billion|thousand|M|B|K)?""",
                                RegexOption.IGNORE_CASE
                        )
                        .find(cleaned)

        if (match != null) {
            val number = match.groupValues[1].toBigDecimalOrNull() ?: return null
            val multiplier = match.groupValues.getOrNull(2)?.lowercase() ?: ""

            return when (multiplier) {
                "billion", "b" -> number.multiply(BigDecimal("1000000000"))
                "million", "m" -> number.multiply(BigDecimal("1000000"))
                "thousand", "k" -> number.multiply(BigDecimal("1000"))
                else -> number
            }
        }
        return null
    }

    private fun formatExtractedValue(value: BigDecimal): String {
        val billion = BigDecimal("1000000000")
        val million = BigDecimal("1000000")
        val thousand = BigDecimal("1000")

        return when {
            value.abs() >= billion -> "$${value.divide(billion, 2, RoundingMode.HALF_UP)}B"
            value.abs() >= million -> "$${value.divide(million, 2, RoundingMode.HALF_UP)}M"
            value.abs() >= thousand -> "$${value.divide(thousand, 2, RoundingMode.HALF_UP)}K"
            else -> "$${value.setScale(2, RoundingMode.HALF_UP)}"
        }
    }
}
