package com.pascal.institute.ahmes.ai

import ai.djl.Application
import ai.djl.Device
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.djl.inference.Predictor
import ai.djl.modality.Classifications
import ai.djl.modality.nlp.qa.QAInput
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ZooModel
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory

/**
 * DJL Model Manager
 *
 * Manages deep learning models for SEC document analysis. Provides lazy-loading, caching, and
 * resource management for AI models.
 */
object DjlModelManager {

    private val logger = LoggerFactory.getLogger(DjlModelManager::class.java)

    // Model and Tokenizer cache
    private val modelCache = ConcurrentHashMap<String, ZooModel<*, *>>()
    private val tokenizerCache = ConcurrentHashMap<String, HuggingFaceTokenizer>()

    // Predictor pools to reuse expensive predictor objects
    private val sentimentPredictorPool =
            java.util.concurrent.ConcurrentLinkedQueue<Predictor<String, Classifications>>()
    private val qaPredictorPool =
            java.util.concurrent.ConcurrentLinkedQueue<Predictor<QAInput, String>>()
    private val summarizationPredictorPool =
            java.util.concurrent.ConcurrentLinkedQueue<Predictor<String, String>>()

    // Check if GPU is available
    private fun isGpuAvailable(): Boolean {
        return try {
            val engine = ai.djl.engine.Engine.getInstance()
            val hasGpu = Device.gpu().isGpu
            val engineName = engine.engineName
            logger.info("DJL Engine: $engineName, GPU available: $hasGpu")
            hasGpu
        } catch (e: Exception) {
            logger.warn("GPU detection failed: ${e.message}. Falling back to CPU.")
            false
        }
    }

    // Default device (CPU or GPU if available)
    val defaultDevice: Device by lazy {
        val device =
                try {
                    if (isGpuAvailable()) {
                        val gpu = Device.gpu()
                        logger.info("Using GPU: $gpu")
                        gpu
                    } else {
                        logger.info("Using CPU")
                        Device.cpu()
                    }
                } catch (e: Exception) {
                    logger.warn("Error selecting device, defaulting to CPU: ${e.message}")
                    Device.cpu()
                }
        device
    }

    /** Model types available for SEC parsing */
    enum class ModelType(val modelId: String, val description: String) {
        SENTIMENT("ProsusAI/finbert", "Financial Sentiment Analysis"),
        NER("dbmdz/bert-large-cased-finetuned-conll03-english", "Named Entity Recognition"),
        QUESTION_ANSWERING(
                "deepset/bert-large-uncased-whole-word-masking-squad2",
                "High-Precision Question Answering"
        ),
        SUMMARIZATION("sshleifer/distilbart-cnn-12-6", "AI Document Summarization"),
        TEXT_CLASSIFICATION("facebook/bart-large-mnli", "Zero-shot Classification")
    }

    @Suppress("UNCHECKED_CAST")
    fun getSentimentModel(): ZooModel<String, Classifications>? {
        return try {
            modelCache.getOrPut("sentiment") {
                val modelId = ModelType.SENTIMENT.modelId
                val criteria =
                        Criteria.builder()
                                .optApplication(Application.NLP.TEXT_CLASSIFICATION)
                                .setTypes(String::class.java, Classifications::class.java)
                                .optModelUrls("djl://ai.djl.huggingface.pytorch/$modelId")
                                .optEngine("PyTorch")
                                .optDevice(defaultDevice)
                                .optProgress(ai.djl.training.util.ProgressBar())
                                .build()

                criteria.loadModel()
            } as
                    ZooModel<String, Classifications>
        } catch (e: Exception) {
            logger.error(
                    "Failed to load sentiment model (${ModelType.SENTIMENT.modelId}): ${e.message}",
                    e
            )
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun getQuestionAnsweringModel(): ZooModel<QAInput, String>? {
        return try {
            modelCache.getOrPut("qa") {
                val modelId = ModelType.QUESTION_ANSWERING.modelId
                val criteria =
                        Criteria.builder()
                                .optApplication(Application.NLP.QUESTION_ANSWER)
                                .setTypes(QAInput::class.java, String::class.java)
                                .optModelUrls("djl://ai.djl.huggingface.pytorch/$modelId")
                                .optEngine("PyTorch")
                                .optDevice(defaultDevice)
                                .optProgress(ai.djl.training.util.ProgressBar())
                                .build()

                criteria.loadModel()
            } as
                    ZooModel<QAInput, String>
        } catch (e: Exception) {
            logger.error(
                    "Failed to load QA model (${ModelType.QUESTION_ANSWERING.modelId}): ${e.message}",
                    e
            )
            null
        }
    }

    /** Get HuggingFace tokenizer for text processing */
    fun getTokenizer(modelName: String = "bert-base-uncased"): HuggingFaceTokenizer? {
        return try {
            tokenizerCache.getOrPut(modelName) { HuggingFaceTokenizer.newInstance(modelName) }
        } catch (e: Exception) {
            logger.warn("Failed to load tokenizer $modelName: ${e.message}")
            null
        }
    }

    /** Check if AI capabilities are available */
    fun isAvailable(): Boolean {
        return try {
            // Try to load a simple tokenizer to check DJL availability
            val tokenizer = HuggingFaceTokenizer.newInstance("bert-base-uncased")
            tokenizer.close()
            true
        } catch (e: Exception) {
            logger.info("DJL AI capabilities not available: ${e.message}")
            false
        }
    }

    /** Load or get cached summarization model */
    @Suppress("UNCHECKED_CAST")
    fun getSummarizationModel(): ZooModel<String, String>? {
        return try {
            modelCache.getOrPut("summarization") {
                val modelId = ModelType.SUMMARIZATION.modelId
                val criteria =
                        Criteria.builder()
                                .optApplication(Application.NLP.ANY)
                                .setTypes(String::class.java, String::class.java)
                                .optModelUrls("djl://ai.djl.huggingface.pytorch/$modelId")
                                .optEngine("PyTorch")
                                .optDevice(defaultDevice)
                                .optProgress(ai.djl.training.util.ProgressBar())
                                .build()

                criteria.loadModel()
            } as
                    ZooModel<String, String>
        } catch (e: Exception) {
            logger.error(
                    "Failed to load summarization model (${ModelType.SUMMARIZATION.modelId}): ${e.message}",
                    e
            )
            null
        }
    }

    /** Get device information */
    fun getDeviceInfo(): Map<String, Any> {
        val isGpu =
                try {
                    defaultDevice.isGpu
                } catch (e: Exception) {
                    false
                }
        return mapOf("defaultDevice" to defaultDevice.toString(), "isGpuAvailable" to isGpu)
    }

    /** Executes a block of code using a reused Sentiment Predictor. */
    fun <R> withSentimentPredictor(block: (Predictor<String, Classifications>) -> R): R {
        val predictor =
                sentimentPredictorPool.poll()
                        ?: getSentimentModel()?.newPredictor()
                                ?: throw IllegalStateException("Sentiment model not available")

        return try {
            block(predictor)
        } finally {
            sentimentPredictorPool.offer(predictor)
        }
    }

    /** Executes a block of code using a reused QA Predictor. */
    fun <R> withQaPredictor(block: (Predictor<QAInput, String>) -> R): R {
        val predictor =
                qaPredictorPool.poll()
                        ?: getQuestionAnsweringModel()?.newPredictor()
                                ?: throw IllegalStateException("QA model not available")

        return try {
            block(predictor)
        } finally {
            qaPredictorPool.offer(predictor)
        }
    }

    /** Executes a block of code using a reused Summarization Predictor. */
    fun <R> withSummarizationPredictor(block: (Predictor<String, String>) -> R): R {
        val predictor =
                summarizationPredictorPool.poll()
                        ?: getSummarizationModel()?.newPredictor()
                                ?: throw IllegalStateException("Summarization model not available")

        return try {
            block(predictor)
        } finally {
            summarizationPredictorPool.offer(predictor)
        }
    }

    /** Release all cached models, predictors and resources */
    fun shutdown() {
        // Close and clear sentiment predictors
        while (sentimentPredictorPool.isNotEmpty()) {
            sentimentPredictorPool.poll()?.close()
        }

        // Close and clear QA predictors
        while (qaPredictorPool.isNotEmpty()) {
            qaPredictorPool.poll()?.close()
        }

        // Close and clear summarization predictors
        while (summarizationPredictorPool.isNotEmpty()) {
            summarizationPredictorPool.poll()?.close()
        }

        modelCache.values.forEach { model ->
            try {
                model.close()
            } catch (e: Exception) {
                logger.warn("Error closing model: ${e.message}")
            }
        }
        modelCache.clear()

        tokenizerCache.values.forEach { tokenizer ->
            try {
                tokenizer.close()
            } catch (e: Exception) {
                logger.warn("Error closing tokenizer: ${e.message}")
            }
        }
        tokenizerCache.clear()

        logger.info("DJL Model Manager shutdown complete")
    }
}
