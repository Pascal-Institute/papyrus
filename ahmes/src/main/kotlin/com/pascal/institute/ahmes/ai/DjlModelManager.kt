package com.pascal.institute.ahmes.ai

import ai.djl.Application
import ai.djl.Device
import ai.djl.MalformedModelException
import ai.djl.Model
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.djl.inference.Predictor
import ai.djl.modality.Classifications
import ai.djl.modality.nlp.qa.QAInput
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ModelNotFoundException
import ai.djl.repository.zoo.ZooModel
import ai.djl.translate.TranslateException
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * DJL Model Manager
 *
 * Manages deep learning models for SEC document analysis.
 * Provides lazy-loading, caching, and resource management for AI models.
 */
object DjlModelManager {

    private val logger = LoggerFactory.getLogger(DjlModelManager::class.java)

    // Model cache to avoid repeated loading
    private val modelCache = ConcurrentHashMap<String, ZooModel<*, *>>()
    private val tokenizerCache = ConcurrentHashMap<String, HuggingFaceTokenizer>()

    // Check if GPU is available
    private fun isGpuAvailable(): Boolean {
        return try {
            val gpu = Device.gpu()
            gpu.isGpu
        } catch (e: Exception) {
            false
        }
    }

    // Default device (CPU or GPU if available)
    val defaultDevice: Device by lazy {
        try {
            if (isGpuAvailable()) Device.gpu() else Device.cpu()
        } catch (e: Exception) {
            Device.cpu()
        }
    }

    /**
     * Model types available for SEC parsing
     */
    enum class ModelType(val modelName: String, val description: String) {
        SENTIMENT("distilbert-base-uncased-finetuned-sst-2-english", "Sentiment Analysis"),
        NER("dbmdz/bert-large-cased-finetuned-conll03-english", "Named Entity Recognition"),
        QUESTION_ANSWERING("distilbert-base-cased-distilled-squad", "Question Answering"),
        TEXT_CLASSIFICATION("facebook/bart-large-mnli", "Zero-shot Classification")
    }

    /**
     * Load or get cached sentiment analysis model
     */
    @Suppress("UNCHECKED_CAST")
    fun getSentimentModel(): ZooModel<String, Classifications>? {
        return try {
            modelCache.getOrPut("sentiment") {
                val criteria = Criteria.builder()
                    .optApplication(Application.NLP.SENTIMENT_ANALYSIS)
                    .setTypes(String::class.java, Classifications::class.java)
                    .optEngine("PyTorch")
                    .optDevice(defaultDevice)
                    .optProgress(ai.djl.training.util.ProgressBar())
                    .build()

                criteria.loadModel()
            } as ZooModel<String, Classifications>
        } catch (e: Exception) {
            logger.warn("Failed to load sentiment model: ${e.message}")
            null
        }
    }

    /**
     * Load or get cached question answering model
     */
    @Suppress("UNCHECKED_CAST")
    fun getQuestionAnsweringModel(): ZooModel<QAInput, String>? {
        return try {
            modelCache.getOrPut("qa") {
                val criteria = Criteria.builder()
                    .optApplication(Application.NLP.QUESTION_ANSWER)
                    .setTypes(QAInput::class.java, String::class.java)
                    .optEngine("PyTorch")
                    .optDevice(defaultDevice)
                    .optProgress(ai.djl.training.util.ProgressBar())
                    .build()

                criteria.loadModel()
            } as ZooModel<QAInput, String>
        } catch (e: Exception) {
            logger.warn("Failed to load QA model: ${e.message}")
            null
        }
    }

    /**
     * Get HuggingFace tokenizer for text processing
     */
    fun getTokenizer(modelName: String = "bert-base-uncased"): HuggingFaceTokenizer? {
        return try {
            tokenizerCache.getOrPut(modelName) {
                HuggingFaceTokenizer.newInstance(modelName)
            }
        } catch (e: Exception) {
            logger.warn("Failed to load tokenizer $modelName: ${e.message}")
            null
        }
    }

    /**
     * Check if AI capabilities are available
     */
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

    /**
     * Get device information
     */
    fun getDeviceInfo(): Map<String, Any> {
        val isGpu = try { defaultDevice.isGpu } catch (e: Exception) { false }
        return mapOf(
            "defaultDevice" to defaultDevice.toString(),
            "isGpuAvailable" to isGpu
        )
    }

    /**
     * Release all cached models and resources
     */
    fun shutdown() {
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
