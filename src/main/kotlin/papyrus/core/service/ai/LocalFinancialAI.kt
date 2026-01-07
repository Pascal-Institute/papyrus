package papyrus.core.service.ai

import ai.djl.Application
import ai.djl.MalformedModelException
import ai.djl.inference.Predictor
import ai.djl.modality.Classifications
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ModelZoo
import ai.djl.repository.zoo.ZooModel
import ai.djl.training.util.ProgressBar
import ai.djl.translate.TranslateException
import papyrus.core.model.FinancialEntity
import papyrus.core.model.FinancialSentiment
import java.io.IOException
import java.util.logging.Logger

/**
 * Local AI-powered financial analysis using DJL + FinBERT
 * No API calls required - runs entirely on local GPU/CPU
 *
 * Uses:
 * - FinBERT for sentiment analysis (financial domain-specific BERT)
 * - Runs offline after initial model download (~440MB)
 * - GPU accelerated if available, falls back to CPU
 */
class LocalFinancialAI {

    private val logger = Logger.getLogger(LocalFinancialAI::class.java.name)

    private var sentimentModel: ZooModel<String, Classifications>? = null
    private var isInitialized = false

    /**
     * Initialize AI models (downloads on first run)
     * Call this once at application startup
     */
    fun initialize() {
        if (isInitialized) return

        try {
            logger.info("Initializing FinBERT model...")
            logger.info("First run will download ~440MB model from HuggingFace")

            val criteria = Criteria.builder()
                .setTypes(String::class.java, Classifications::class.java)
                .optModelUrls("djl://ai.djl.huggingface.pytorch/ProsusAI/finbert")
                .optEngine("PyTorch")
                .optProgress(ProgressBar())
                .build()

            sentimentModel = ModelZoo.loadModel(criteria)
            isInitialized = true

            logger.info("✓ FinBERT model loaded successfully")
            logger.info("✓ Model cached at: ~/.djl.ai/cache/")

        } catch (e: Exception) {
            logger.severe("Failed to initialize AI models: ${e.message}")
            logger.warning("AI features will be disabled. Parser will fall back to rule-based extraction.")
            // Don't throw - allow app to continue without AI
        }
    }

    /**
     * Analyze sentiment of financial text (MD&A, Risk Factors, etc.)
     * Returns: POSITIVE, NEGATIVE, or NEUTRAL with confidence score
     *
     * Example:
     * - "strong revenue growth" → POSITIVE (0.95)
     * - "challenging market conditions" → NEGATIVE (0.87)
     */
    fun analyzeSentiment(text: String): FinancialSentiment? {
        if (!isInitialized || sentimentModel == null) {
            logger.warning("AI not initialized - skipping sentiment analysis")
            return null
        }

        try {
            val predictor = sentimentModel!!.newPredictor()

            // FinBERT works best with chunks of 200-500 words
            val textChunk = text.take(2000) // ~500 words max
            val result = predictor.predict(textChunk)

            val topPrediction = result.best<Classifications.Classification>()

            return FinancialSentiment(
                sentiment = topPrediction.className,
                confidence = topPrediction.probability,
                highlights = extractKeyPhrases(text, result)
            )

        } catch (e: TranslateException) {
            logger.warning("Sentiment analysis failed: ${e.message}")
            return null
        } catch (e: Exception) {
            logger.warning("Unexpected error in sentiment analysis: ${e.message}")
            return null
        }
    }

    /**
     * Analyze MD&A (Management Discussion & Analysis) section
     * Provides detailed sentiment breakdown
     */
    fun analyzeMDA(mdaText: String): FinancialSentiment? {
        if (mdaText.length < 100) {
            logger.info("MD&A text too short for meaningful analysis")
            return null
        }

        logger.info("Analyzing MD&A sentiment (${mdaText.length} chars)...")
        return analyzeSentiment(mdaText)
    }

    /**
     * Analyze Risk Factors section
     * Typically negative, but degree matters
     */
    fun analyzeRiskFactors(riskText: String): FinancialSentiment? {
        if (riskText.length < 100) {
            logger.info("Risk Factors text too short for meaningful analysis")
            return null
        }

        logger.info("Analyzing Risk Factors sentiment (${riskText.length} chars)...")
        return analyzeSentiment(riskText)
    }

    /**
     * Extract financial entities using simple rule-based NER
     * TODO: Enhance with DJL NER model if needed
     */
    fun extractFinancialEntities(text: String): List<FinancialEntity> {
        val entities = mutableListOf<FinancialEntity>()

        // Money pattern: $1.5M, $2.3B, $500K
        val moneyPattern = Regex("""\$\s*(\d+\.?\d*)\s*([MBK])?""")
        moneyPattern.findAll(text).forEach { match ->
            val value = match.groupValues[1].toDoubleOrNull()
            val multiplier = when (match.groupValues[2]) {
                "M" -> 1_000_000.0
                "B" -> 1_000_000_000.0
                "K" -> 1_000.0
                else -> 1.0
            }

            if (value != null) {
                entities.add(
                    FinancialEntity(
                        text = match.value,
                        type = "MONEY",
                        value = value * multiplier,
                        unit = "USD",
                        context = getContext(text, match.range),
                        confidence = 0.9
                    )
                )
            }
        }

        // Percentage pattern: 15%, 3.5%
        val percentPattern = Regex("""(\d+\.?\d*)\s*%""")
        percentPattern.findAll(text).forEach { match ->
            val value = match.groupValues[1].toDoubleOrNull()

            if (value != null) {
                entities.add(
                    FinancialEntity(
                        text = match.value,
                        type = "PERCENT",
                        value = value,
                        unit = "percent",
                        context = getContext(text, match.range),
                        confidence = 0.85
                    )
                )
            }
        }

        return entities
    }

    /**
     * Extract key phrases that influenced sentiment
     * Returns top 3 most relevant sentences
     */
    private fun extractKeyPhrases(text: String, sentiment: Classifications): List<String> {
        // Simple sentence extraction
        val sentences = text.split(Regex("""[.!?]\s+"""))
            .filter { it.length > 20 }

        // Return first 3 meaningful sentences as highlights
        return sentences.take(3).map { it.trim() }
    }

    /**
     * Get surrounding context for an entity (50 chars before/after)
     */
    private fun getContext(text: String, range: IntRange): String {
        val start = maxOf(0, range.first - 50)
        val end = minOf(text.length, range.last + 50)
        return text.substring(start, end).trim()
    }

    /**
     * Clean up resources
     */
    fun close() {
        sentimentModel?.close()
        isInitialized = false
        logger.info("AI models closed")
    }

    companion object {
        @Volatile
        private var instance: LocalFinancialAI? = null

        /**
         * Get singleton instance
         */
        fun getInstance(): LocalFinancialAI {
            return instance ?: synchronized(this) {
                instance ?: LocalFinancialAI().also {
                    instance = it
                    // Auto-initialize on first access
                    Thread {
                        it.initialize()
                    }.start()
                }
            }
        }
    }
}
