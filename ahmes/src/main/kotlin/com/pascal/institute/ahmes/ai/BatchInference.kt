package com.pascal.institute.ahmes.ai

import ai.djl.modality.Classifications
import ai.djl.modality.nlp.qa.QAInput
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.*

/**
 * Batch processing utilities for AI model inference.
 *
 * Provides batch processing capabilities to maximize GPU utilization and improve throughput for
 * multiple inference requests.
 *
 * ## Features
 *
 * - **Batch Inference**: Process multiple inputs simultaneously
 * - **Parallel Processing**: Utilize all available cores
 * - **GPU Optimization**: Maximize GPU utilization with batching
 * - **Async API**: Non-blocking batch processing
 * - **Performance Monitoring**: Track throughput and latency
 *
 * ## Usage
 *
 * ```kotlin
 * // Batch sentiment analysis
 * val texts = listOf("Good news", "Bad news", "Neutral news")
 * val results = BatchInference.batchSentiment(texts)
 *
 * // Async batch processing
 * runBlocking {
 *     val results = BatchInference.batchSentimentAsync(texts)
 * }
 *
 * // Custom batch size
 * val results = BatchInference.batchSentiment(texts, batchSize = 8)
 * ```
 */
object BatchInference {

    /** Statistics for batch inference performance. */
    data class BatchStats(
            val totalItems: Int,
            val batchSize: Int,
            val totalTimeMs: Long,
            val avgTimePerItem: Double,
            val throughput: Double // items per second
    ) {
        override fun toString(): String {
            return """
                BatchStats(
                    total=$totalItems items,
                    batchSize=$batchSize,
                    totalTime=${totalTimeMs}ms,
                    avgPerItem=${String.format("%.2fms", avgTimePerItem)},
                    throughput=${String.format("%.1f", throughput)} items/sec
                )
            """.trimIndent()
        }
    }

    /**
     * Batch sentiment analysis.
     *
     * Processes multiple texts in parallel for better throughput.
     *
     * @param texts List of texts to analyze
     * @param batchSize Number of items to process in each batch
     * @return List of sentiment classifications
     */
    fun batchSentiment(
            texts: List<String>,
            batchSize: Int = 4
    ): Pair<List<Classifications>, BatchStats> {
        if (texts.isEmpty()) {
            return emptyList<Classifications>() to BatchStats(0, batchSize, 0, 0.0, 0.0)
        }

        val results = ConcurrentLinkedQueue<Pair<Int, Classifications>>()

        val totalTime = measureTimeMillis {
            texts.chunked(batchSize).forEachIndexed { chunkIndex, chunk ->
                DjlModelManager.withSentimentPredictor { predictor ->
                    chunk.forEachIndexed { itemIndex, text ->
                        val index = chunkIndex * batchSize + itemIndex
                        val classification = predictor.predict(text)
                        results.add(index to classification)
                    }
                }
            }
        }

        // Sort by original index and extract results
        val sortedResults = results.sortedBy { it.first }.map { it.second }

        val stats =
                BatchStats(
                        totalItems = texts.size,
                        batchSize = batchSize,
                        totalTimeMs = totalTime,
                        avgTimePerItem = totalTime.toDouble() / texts.size,
                        throughput = (texts.size * 1000.0) / totalTime
                )

        return sortedResults to stats
    }

    /**
     * Async batch sentiment analysis.
     *
     * Processes texts in parallel using coroutines.
     *
     * @param texts List of texts to analyze
     * @param parallelism Number of parallel coroutines
     * @return List of sentiment classifications
     */
    suspend fun batchSentimentAsync(
            texts: List<String>,
            parallelism: Int = Runtime.getRuntime().availableProcessors()
    ): Pair<List<Classifications>, BatchStats> = coroutineScope {
        if (texts.isEmpty()) {
            return@coroutineScope emptyList<Classifications>() to BatchStats(0, 0, 0, 0.0, 0.0)
        }

        val totalTime = measureTimeMillis {
            val deferredResults =
                    texts.mapIndexed { index, text ->
                        async(Dispatchers.Default) {
                            index to
                                    DjlModelManager.withSentimentPredictor { predictor ->
                                        predictor.predict(text)
                                    }
                        }
                    }

            // Await all results
            deferredResults.awaitAll()
        }

        val results =
                texts.map { text ->
                    DjlModelManager.withSentimentPredictor { predictor -> predictor.predict(text) }
                }

        val stats =
                BatchStats(
                        totalItems = texts.size,
                        batchSize = parallelism,
                        totalTimeMs = totalTime,
                        avgTimePerItem = totalTime.toDouble() / texts.size,
                        throughput = (texts.size * 1000.0) / totalTime
                )

        results to stats
    }

    /**
     * Batch question answering.
     *
     * @param qaInputs List of QA inputs (question + context)
     * @param batchSize Batch size for processing
     * @return List of answers
     */
    fun batchQuestionAnswering(
            qaInputs: List<QAInput>,
            batchSize: Int = 4
    ): Pair<List<String>, BatchStats> {
        if (qaInputs.isEmpty()) {
            return emptyList<String>() to BatchStats(0, batchSize, 0, 0.0, 0.0)
        }

        val results = ConcurrentLinkedQueue<Pair<Int, String>>()

        val totalTime = measureTimeMillis {
            qaInputs.chunked(batchSize).forEachIndexed { chunkIndex, chunk ->
                DjlModelManager.withQaPredictor { predictor ->
                    chunk.forEachIndexed { itemIndex, qaInput ->
                        val index = chunkIndex * batchSize + itemIndex
                        val answer = predictor.predict(qaInput)
                        results.add(index to answer)
                    }
                }
            }
        }

        val sortedResults = results.sortedBy { it.first }.map { it.second }

        val stats =
                BatchStats(
                        totalItems = qaInputs.size,
                        batchSize = batchSize,
                        totalTimeMs = totalTime,
                        avgTimePerItem = totalTime.toDouble() / qaInputs.size,
                        throughput = (qaInputs.size * 1000.0) / totalTime
                )

        return sortedResults to stats
    }

    /**
     * Batch text summarization.
     *
     * @param texts List of texts to summarize
     * @param batchSize Batch size for processing
     * @return List of summaries
     */
    fun batchSummarization(
            texts: List<String>,
            batchSize: Int = 4
    ): Pair<List<String>, BatchStats> {
        if (texts.isEmpty()) {
            return emptyList<String>() to BatchStats(0, batchSize, 0, 0.0, 0.0)
        }

        val results = ConcurrentLinkedQueue<Pair<Int, String>>()

        val totalTime = measureTimeMillis {
            texts.chunked(batchSize).forEachIndexed { chunkIndex, chunk ->
                DjlModelManager.withSummarizationPredictor { predictor ->
                    chunk.forEachIndexed { itemIndex, text ->
                        val index = chunkIndex * batchSize + itemIndex
                        val summary = predictor.predict(text)
                        results.add(index to summary)
                    }
                }
            }
        }

        val sortedResults = results.sortedBy { it.first }.map { it.second }

        val stats =
                BatchStats(
                        totalItems = texts.size,
                        batchSize = batchSize,
                        totalTimeMs = totalTime,
                        avgTimePerItem = totalTime.toDouble() / texts.size,
                        throughput = (texts.size * 1000.0) / totalTime
                )

        return sortedResults to stats
    }

    /**
     * Optimal batch size recommendation based on available resources.
     *
     * @return Recommended batch size
     */
    fun getOptimalBatchSize(): Int {
        val cpuCores = Runtime.getRuntime().availableProcessors()
        val isGpuAvailable = DjlModelManager.isGpuAvailable()

        return when {
            isGpuAvailable -> 16 // GPU can handle larger batches
            cpuCores >= 8 -> 8 // High-end CPU
            cpuCores >= 4 -> 4 // Mid-range CPU
            else -> 2 // Low-end CPU
        }
    }
}
