package com.pascal.institute.ahmes.ai

import ai.djl.modality.nlp.qa.QAInput
import java.math.BigDecimal
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

/** Extracted financial entity from SEC document */
@Serializable
data class FinancialEntity(
        val text: String,
        val entityType: FinancialEntityType,
        val value: String? = null, // Numeric value if applicable (BigDecimal as String)
        val unit: String? = null,
        val context: String = "",
        val confidence: String = "1.0"
) {
    fun getValueBigDecimal(): BigDecimal? = value?.let { BigDecimal(it) }
    fun getConfidenceBigDecimal(): BigDecimal = BigDecimal(confidence)
}

@Serializable
enum class FinancialEntityType {
    COMPANY_NAME,
    TICKER_SYMBOL,
    MONETARY_VALUE,
    PERCENTAGE,
    DATE,
    FISCAL_PERIOD,
    METRIC_NAME,
    EXECUTIVE_NAME,
    LOCATION,
    PRODUCT_NAME,
    INDUSTRY_TERM
}

/** Answer from question-answering model */
@Serializable
data class QAAnswer(
        val question: String,
        val answer: String,
        val context: String,
        val confidence: String = "1.0"
) {
    fun getConfidenceBigDecimal(): BigDecimal = BigDecimal(confidence)
}

/**
 * SEC Financial Entity Extractor
 *
 * Uses DJL deep learning models to extract structured financial entities from unstructured SEC
 * filing text.
 */
object SecEntityExtractor {

    private val logger = LoggerFactory.getLogger(SecEntityExtractor::class.java)

    // Patterns for rule-based extraction (fallback)
    private val monetaryPattern =
            Regex(
                    """\$\s*(\d{1,3}(?:,\d{3})*(?:\.\d+)?)\s*(million|billion|thousand|M|B|K)?""",
                    RegexOption.IGNORE_CASE
            )

    private val percentagePattern = Regex("""(\d+(?:\.\d+)?)\s*%""")

    private val datePattern =
            Regex(
                    """(January|February|March|April|May|June|July|August|September|October|November|December)\s+\d{1,2},?\s+\d{4}""",
                    RegexOption.IGNORE_CASE
            )

    private val fiscalPeriodPattern =
            Regex("""(FY|Q[1-4]|fiscal\s+year|quarter)\s*(\d{4}|\d{2})?""", RegexOption.IGNORE_CASE)

    private val tickerPattern = Regex("""\b([A-Z]{1,5})\b(?:\s*\([^)]*\))?""")

    // Common SEC metric names
    private val metricNames =
            setOf(
                    "revenue",
                    "net income",
                    "gross profit",
                    "operating income",
                    "ebitda",
                    "total assets",
                    "total liabilities",
                    "stockholders equity",
                    "cash and cash equivalents",
                    "accounts receivable",
                    "inventory",
                    "operating cash flow",
                    "free cash flow",
                    "capital expenditures",
                    "earnings per share",
                    "eps",
                    "diluted eps",
                    "basic eps",
                    "gross margin",
                    "operating margin",
                    "net margin",
                    "return on equity",
                    "roe",
                    "return on assets",
                    "roa",
                    "debt to equity",
                    "current ratio",
                    "quick ratio"
            )

    /** Extract financial entities from text */
    fun extractEntities(text: String): List<FinancialEntity> {
        val entities = mutableListOf<FinancialEntity>()

        // Extract monetary values
        entities.addAll(extractMonetaryValues(text))

        // Extract percentages
        entities.addAll(extractPercentages(text))

        // Extract dates
        entities.addAll(extractDates(text))

        // Extract fiscal periods
        entities.addAll(extractFiscalPeriods(text))

        // Extract metric names
        entities.addAll(extractMetricNames(text))

        return entities.distinctBy { "${it.entityType}:${it.text}" }
    }

    /** Answer questions about SEC document using QA model */
    fun answerQuestion(question: String, context: String): QAAnswer {
        return try {
            DjlModelManager.withQaPredictor { predictor ->
                val input = QAInput(question, context.take(2000))
                val answer = predictor.predict(input)
                QAAnswer(
                        question = question,
                        answer = answer,
                        context = context.take(500),
                        confidence = "0.8" // DJL QA doesn't provide confidence directly
                )
            }
        } catch (e: Exception) {
            logger.warn("QA failed, using fallback: ${e.message}")
            ruleBasedQA(question, context)
        }
    }

    /** Extract key financial facts using predefined questions */
    fun extractFinancialFacts(documentText: String): Map<String, QAAnswer> {
        val questions =
                listOf(
                        "What is the company's total revenue?",
                        "What is the net income?",
                        "What are the total assets?",
                        "What is the company's cash position?",
                        "What are the major risk factors?",
                        "Who is the CEO?",
                        "What is the fiscal year end date?"
                )

        return try {
            DjlModelManager.withQaPredictor { predictor ->
                questions.associateWith { question ->
                    val input = QAInput(question, documentText.take(2000))
                    val answer = predictor.predict(input)
                    QAAnswer(
                            question = question,
                            answer = answer,
                            context = documentText.take(500),
                            confidence = "0.8"
                    )
                }
            }
        } catch (e: Exception) {
            logger.warn("Batch QA failed, falling back to individual calls: ${e.message}")
            questions.associateWith { question -> answerQuestion(question, documentText) }
        }
    }

    /** Extract company mentions from text */
    fun extractCompanyNames(text: String): List<FinancialEntity> {
        val entities = mutableListOf<FinancialEntity>()

        // Common patterns for company names in SEC filings
        val companyPatterns =
                listOf(
                        Regex("""([A-Z][a-zA-Z]+(?:\s+[A-Z][a-zA-Z]+)*)\s*,?\s*Inc\."""),
                        Regex("""([A-Z][a-zA-Z]+(?:\s+[A-Z][a-zA-Z]+)*)\s*,?\s*Corp\."""),
                        Regex("""([A-Z][a-zA-Z]+(?:\s+[A-Z][a-zA-Z]+)*)\s*,?\s*Corporation"""),
                        Regex("""([A-Z][a-zA-Z]+(?:\s+[A-Z][a-zA-Z]+)*)\s*,?\s*LLC"""),
                        Regex("""([A-Z][a-zA-Z]+(?:\s+[A-Z][a-zA-Z]+)*)\s*,?\s*Ltd\.""")
                )

        companyPatterns.forEach { pattern ->
            pattern.findAll(text).forEach { match ->
                entities.add(
                        FinancialEntity(
                                text = match.value.trim(),
                                entityType = FinancialEntityType.COMPANY_NAME,
                                context = extractContext(text, match.range),
                                confidence = "0.85"
                        )
                )
            }
        }

        return entities.distinctBy { it.text }
    }

    private fun extractMonetaryValues(text: String): List<FinancialEntity> {
        return monetaryPattern
                .findAll(text)
                .map { match ->
                    val numericValue = match.groupValues[1].replace(",", "")
                    val multiplier = match.groupValues.getOrNull(2)?.lowercase() ?: ""

                    val value =
                            try {
                                val base = BigDecimal(numericValue)
                                when (multiplier) {
                                    "billion", "b" -> base.multiply(BigDecimal("1000000000"))
                                    "million", "m" -> base.multiply(BigDecimal("1000000"))
                                    "thousand", "k" -> base.multiply(BigDecimal("1000"))
                                    else -> base
                                }
                            } catch (e: Exception) {
                                null
                            }

                    FinancialEntity(
                            text = match.value.trim(),
                            entityType = FinancialEntityType.MONETARY_VALUE,
                            value = value?.toPlainString(),
                            unit = "USD",
                            context = extractContext(text, match.range),
                            confidence = "0.95"
                    )
                }
                .toList()
    }

    private fun extractPercentages(text: String): List<FinancialEntity> {
        return percentagePattern
                .findAll(text)
                .map { match ->
                    FinancialEntity(
                            text = match.value.trim(),
                            entityType = FinancialEntityType.PERCENTAGE,
                            value = match.groupValues[1],
                            unit = "PERCENT",
                            context = extractContext(text, match.range),
                            confidence = "0.95"
                    )
                }
                .toList()
    }

    private fun extractDates(text: String): List<FinancialEntity> {
        return datePattern
                .findAll(text)
                .map { match ->
                    FinancialEntity(
                            text = match.value.trim(),
                            entityType = FinancialEntityType.DATE,
                            context = extractContext(text, match.range),
                            confidence = "0.90"
                    )
                }
                .toList()
    }

    private fun extractFiscalPeriods(text: String): List<FinancialEntity> {
        return fiscalPeriodPattern
                .findAll(text)
                .map { match ->
                    FinancialEntity(
                            text = match.value.trim(),
                            entityType = FinancialEntityType.FISCAL_PERIOD,
                            context = extractContext(text, match.range),
                            confidence = "0.90"
                    )
                }
                .toList()
    }

    private fun extractMetricNames(text: String): List<FinancialEntity> {
        val lowerText = text.lowercase()
        return metricNames.filter { lowerText.contains(it) }.map { metric ->
            FinancialEntity(
                    text = metric,
                    entityType = FinancialEntityType.METRIC_NAME,
                    confidence = "0.80"
            )
        }
    }

    private fun extractContext(text: String, range: IntRange, contextSize: Int = 50): String {
        val start = (range.first - contextSize).coerceAtLeast(0)
        val end = (range.last + contextSize).coerceAtMost(text.length)
        return text.substring(start, end).trim()
    }

    private fun ruleBasedQA(question: String, context: String): QAAnswer {
        val lowerQuestion = question.lowercase()
        val lowerContext = context.lowercase()

        // Simple keyword-based extraction
        val answer =
                when {
                    lowerQuestion.contains("revenue") -> {
                        monetaryPattern.find(context)?.value ?: "Not found"
                    }
                    lowerQuestion.contains("net income") -> {
                        val incomeSection = context.substringAfter("net income", "")
                        monetaryPattern.find(incomeSection)?.value ?: "Not found"
                    }
                    lowerQuestion.contains("ceo") || lowerQuestion.contains("chief executive") -> {
                        extractExecutiveName(context, "CEO") ?: "Not found"
                    }
                    else -> "Unable to extract answer"
                }

        return QAAnswer(
                question = question,
                answer = answer,
                context = context.take(200),
                confidence = "0.5"
        )
    }

    private fun extractExecutiveName(text: String, title: String): String? {
        val pattern =
                Regex(
                        """([A-Z][a-z]+\s+[A-Z][a-z]+(?:\s+[A-Z][a-z]+)?),?\s*(?:our\s+)?$title""",
                        RegexOption.IGNORE_CASE
                )
        return pattern.find(text)?.groupValues?.get(1)
    }
}
