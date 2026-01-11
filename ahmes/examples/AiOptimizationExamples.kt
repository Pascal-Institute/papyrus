package com.pascal.institute.ahmes.examples

import com.pascal.institute.ahmes.ai.BatchInference
import com.pascal.institute.ahmes.ai.DjlModelManager
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.runBlocking

/**
 * AI Model Optimization Examples
 *
 * Demonstrates optimization techniques for AI inference:
 * - Batch processing for better throughput
 * - GPU utilization
 * - Async processing with coroutines
 * - Performance comparison
 */
fun main() {
    println("=== Ahmes AI Optimization Examples ===\n")

    // Check AI availability
    if (!DjlModelManager.isAvailable()) {
        println("⚠️  AI models not available. Please ensure DJL is properly configured.")
        return
    }

    println("Device: ${DjlModelManager.getDeviceInfo()}\n")

    // Example 1: Single vs Batch Processing
    example1_BatchProcessing()

    // Example 2: Optimal Batch Size
    example2_OptimalBatchSize()

    // Example 3: Async Batch Processing
    example3_AsyncProcessing()

    // Example 4: GPU vs CPU Performance
    example4_GpuVsCpu()

    // Cleanup
    DjlModelManager.shutdown()
}

/**
 * Example 1: Single vs Batch Processing
 *
 * Compare performance of processing items one-by-one vs in batches.
 */
fun example1_BatchProcessing() {
    println("--- Example 1: Single vs Batch Processing ---")

    val texts =
            listOf(
                    "The company reported strong revenue growth.",
                    "Quarterly earnings exceeded expectations.",
                    "Market conditions remain challenging.",
                    "New product launch was successful.",
                    "Operating expenses increased significantly.",
                    "Customer retention rates improved.",
                    "Supply chain disruptions impacted production.",
                    "R&D investments yielded positive results."
            )

    // Single item processing
    println("Processing ${texts.size} texts individually...")
    val singleTime = measureTimeMillis {
        texts.forEach { text ->
            DjlModelManager.withSentimentPredictor { predictor -> predictor.predict(text) }
        }
    }

    // Batch processing
    println("Processing ${texts.size} texts in batches...")
    val (batchResults, batchStats) = BatchInference.batchSentiment(texts, batchSize = 4)

    println("\nResults:")
    println("Single processing: ${singleTime}ms")
    println("Batch processing: ${batchStats.totalTimeMs}ms")
    println("Speedup: ${String.format("%.2fx", singleTime.toDouble() / batchStats.totalTimeMs)}")
    println("Throughput: ${String.format("%.1f", batchStats.throughput)} items/sec")

    // Show some results
    println("\nSample sentiment results:")
    batchResults.take(3).forEachIndexed { idx, classification ->
        val sentiment = classification.best<Classifications.Classification>().className
        val probability = classification.best<Classifications.Classification>().probability
        println(
                "  ${idx + 1}. \"${texts[idx].take(40)}...\" → $sentiment (${String.format("%.1f%%", probability * 100)})"
        )
    }
    println()
}

/**
 * Example 2: Optimal Batch Size
 *
 * Test different batch sizes to find optimal performance.
 */
fun example2_OptimalBatchSize() {
    println("--- Example 2: Optimal Batch Size ---")

    // Generate test data
    val texts =
            (1..32).map { idx ->
                "Sample financial text number $idx for testing batch processing performance."
            }

    val batchSizes = listOf(1, 2, 4, 8, 16)
    val results = mutableMapOf<Int, Long>()

    println("Testing batch sizes: $batchSizes")

    batchSizes.forEach { batchSize ->
        val (_, stats) = BatchInference.batchSentiment(texts, batchSize = batchSize)
        results[batchSize] = stats.totalTimeMs
        println(
                "Batch size $batchSize: ${stats.totalTimeMs}ms (${String.format("%.1f", stats.throughput)} items/sec)"
        )
    }

    val optimal = results.minByOrNull { it.value }
    println("\n✨ Optimal batch size: ${optimal?.key} (${optimal?.value}ms)")

    val recommended = BatchInference.getOptimalBatchSize()
    println("📊 Recommended batch size for this system: $recommended")
    println()
}

/**
 * Example 3: Async Batch Processing
 *
 * Use coroutines for non-blocking batch processing.
 */
fun example3_AsyncProcessing() = runBlocking {
    println("--- Example 3: Async Batch Processing ---")

    val texts =
            listOf(
                    "Positive financial outlook",
                    "Revenue declined",
                    "Neutral market conditions",
                    "Strong performance",
                    "Weak demand"
            )

    println("Processing ${texts.size} texts asynchronously...")

    val (results, stats) = BatchInference.batchSentimentAsync(texts)

    println("\nAsync batch processing completed:")
    println("Total time: ${stats.totalTimeMs}ms")
    println("Throughput: ${String.format("%.1f", stats.throughput)} items/sec")

    println("\nResults:")
    results.forEachIndexed { idx, classification ->
        val sentiment = classification.best<Classifications.Classification>().className
        println("  ${idx + 1}. $sentiment - \"${texts[idx]}\"")
    }
    println()
}

/**
 * Example 4: GPU vs CPU Performance
 *
 * Compare inference performance on GPU vs CPU.
 */
fun example4_GpuVsCpu() {
    println("--- Example 4: GPU vs CPU Performance ---")

    val isGpuAvailable = DjlModelManager.isGpuAvailable()

    if (!isGpuAvailable) {
        println("⚠️  GPU not available. Running CPU-only benchmark.")
    } else {
        println("✓ GPU available")
    }

    // Test data
    val texts = (1..20).map { "Sample text for GPU vs CPU comparison number $it." }

    // Run benchmark
    println("\nBenchmarking with ${texts.size} texts...")
    val (_, stats) = BatchInference.batchSentiment(texts, batchSize = 8)

    println("\nDevice: ${DjlModelManager.getDeviceInfo()}")
    println("Performance: ${String.format("%.1f", stats.throughput)} items/sec")
    println("Avg latency: ${String.format("%.2f", stats.avgTimePerItem)}ms per item")

    if (isGpuAvailable) {
        println("\n💡 GPU provides significant speedup for batch processing!")
    } else {
        println("\n💡 Consider using GPU for better performance with large batches.")
    }
    println()
}

/**
 * Example 5: Question Answering Batch
 *
 * Batch process multiple questions.
 */
fun example5_QaBatch() {
    println("--- Example 5: Question Answering Batch ---")

    // This example would require QAInput creation
    // Skipped for now as it requires more setup
    println("(Example implementation requires QAInput setup)")
    println()
}
