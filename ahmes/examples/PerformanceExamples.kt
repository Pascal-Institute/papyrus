package com.pascal.institute.ahmes.examples

import com.pascal.institute.ahmes.cache.parseResultCache
import com.pascal.institute.ahmes.form.Form10KParser
import com.pascal.institute.ahmes.model.SecReportMetadata
import java.io.File
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.*

/**
 * Performance Optimization Examples
 *
 * Demonstrates various optimization techniques:
 * - Caching for repeated parsing
 * - Parallel processing with coroutines
 * - Batch operations
 * - Resource management
 */
fun main() = runBlocking {
    println("=== Ahmes Performance Optimization Examples ===\n")

    // Example 1: Caching
    example1_Caching()

    // Example 2: Parallel Processing
    example2_ParallelProcessing()

    // Example 3: Batch Processing with Cache
    example3_BatchWithCache()
}

/**
 * Example 1: Using Cache for Better Performance
 *
 * Demonstrates how caching can dramatically improve performance when parsing the same document
 * multiple times.
 */
fun example1_Caching() {
    println("--- Example 1: Caching ---")

    // Configure cache: 50 documents, 30 minute TTL
    val cache = parseResultCache {
        maxSize(50)
        ttlMinutes(30)
    }

    val parser = Form10KParser()
    val sampleDocument =
            """
        <html><body>
        <h2>ITEM 1. BUSINESS</h2>
        <p>Sample business description...</p>
        </body></html>
    """.trimIndent()

    val metadata = createSampleMetadata("AAPL", "10-K")
    val cacheKey = metadata.accessionNumber

    // First parse - cache miss
    val time1 = measureTimeMillis {
        val result =
                cache.get(cacheKey)
                        ?: run {
                            parser.parseHtml(sampleDocument, metadata).also {
                                cache.put(cacheKey, it)
                            }
                        }
    }

    // Second parse - cache hit!
    val time2 = measureTimeMillis {
        val result = cache.get(cacheKey) ?: error("Should be cached!")
    }

    println("First parse (cache miss): ${time1}ms")
    println("Second parse (cache hit): ${time2}ms")
    println("Speedup: ${time1 / time2.coerceAtLeast(1)}x")
    println("Cache stats: ${cache.getStatistics()}")
    println()
}

/**
 * Example 2: Parallel Processing Multiple Documents
 *
 * Process multiple SEC documents in parallel using Kotlin coroutines.
 */
suspend fun example2_ParallelProcessing() = coroutineScope {
    println("--- Example 2: Parallel Processing ---")

    val documents =
            listOf(
                    Triple("AAPL", "10-K", "Apple Inc. 10-K content..."),
                    Triple("MSFT", "10-K", "Microsoft Corp 10-K content..."),
                    Triple("GOOGL", "10-K", "Alphabet Inc. 10-K content..."),
                    Triple("AMZN", "10-K", "Amazon.com Inc. 10-K content..."),
                    Triple("TSLA", "10-K", "Tesla Inc. 10-K content...")
            )

    println("Processing ${documents.size} documents...")

    // Sequential processing
    val seqTime = measureTimeMillis {
        documents.forEach { (ticker, formType, content) ->
            val parser = Form10KParser()
            val metadata = createSampleMetadata(ticker, formType)
            parser.parseHtml(content, metadata)
            delay(100) // Simulate processing time
        }
    }

    // Parallel processing
    val parallelTime = measureTimeMillis {
        documents
                .map { (ticker, formType, content) ->
                    async(Dispatchers.Default) {
                        val parser = Form10KParser()
                        val metadata = createSampleMetadata(ticker, formType)
                        parser.parseHtml(content, metadata)
                        delay(100) // Simulate processing time
                    }
                }
                .awaitAll()
    }

    println("Sequential processing: ${seqTime}ms")
    println("Parallel processing: ${parallelTime}ms")
    println("Speedup: ${String.format("%.2fx", seqTime.toDouble() / parallelTime)}")
    println()
}

/**
 * Example 3: Batch Processing with Cache
 *
 * Combines caching and parallel processing for optimal performance.
 */
suspend fun example3_BatchWithCache() = coroutineScope {
    println("--- Example 3: Batch Processing with Cache ---")

    val cache = parseResultCache {
        maxSize(100)
        ttlHours(1)
    }

    // Simulate processing a batch of documents
    val batch =
            (1..20).map { idx ->
                Triple("TICK$idx", "10-K", "<html><body>Document $idx content...</body></html>")
            }

    println("Processing ${batch.size} documents with caching...")

    // First run - populate cache
    val firstRunTime = measureTimeMillis {
        batch
                .map { (ticker, formType, content) ->
                    async(Dispatchers.Default) {
                        val parser = Form10KParser()
                        val metadata = createSampleMetadata(ticker, formType)
                        val cacheKey = metadata.accessionNumber

                        cache.get(cacheKey)
                                ?: run {
                                    delay(50) // Simulate parsing time
                                    parser.parseHtml(content, metadata).also {
                                        cache.put(cacheKey, it)
                                    }
                                }
                    }
                }
                .awaitAll()
    }

    println("First run (cache population): ${firstRunTime}ms")
    println("Cache stats: ${cache.getStatistics()}")

    // Second run - from cache
    val secondRunTime = measureTimeMillis {
        batch
                .map { (ticker, formType, _) ->
                    async(Dispatchers.Default) {
                        val metadata = createSampleMetadata(ticker, formType)
                        val cacheKey = metadata.accessionNumber
                        cache.get(cacheKey) ?: error("Should be cached!")
                    }
                }
                .awaitAll()
    }

    println("Second run (from cache): ${secondRunTime}ms")
    println("Cache stats: ${cache.getStatistics()}")
    println("Speedup: ${String.format("%.2fx", firstRunTime.toDouble() / secondRunTime)}")
    println()
}

/**
 * Example 4: Resource-Aware Parallel Processing
 *
 * Limits parallelism based on available CPU cores.
 */
suspend fun example4_ResourceAwareProcessing() = coroutineScope {
    println("--- Example 4: Resource-Aware Processing ---")

    val cpuCores = Runtime.getRuntime().availableProcessors()
    val dispatcher = Dispatchers.Default.limitedParallelism(cpuCores)

    println("Available CPU cores: $cpuCores")
    println("Using dispatcher with $cpuCores parallel threads")

    val documents = (1..100).map { idx -> "Document $idx content..." }

    val time = measureTimeMillis {
        documents
                .map { content ->
                    async(dispatcher) {
                        // Simulate CPU-intensive parsing
                        delay(10)
                        content.length
                    }
                }
                .awaitAll()
    }

    println("Processed ${documents.size} documents in ${time}ms")
    println("Average time per document: ${time / documents.size}ms")
    println()
}

/**
 * Example 5: Memory-Efficient Streaming
 *
 * Process large files in chunks to reduce memory usage.
 */
fun example5_StreamingParsing() {
    println("--- Example 5: Streaming Parsing ---")

    // Simulate large file
    val largeFile = File.createTempFile("large-10k", ".html")
    largeFile.deleteOnExit()

    // Write large content in chunks
    largeFile.bufferedWriter().use { writer ->
        writer.write("<html><body>")
        repeat(10000) { idx -> writer.write("<p>Paragraph $idx with financial data...</p>") }
        writer.write("</body></html>")
    }

    println("File size: ${largeFile.length() / 1024}KB")

    // Stream parsing - process in chunks
    val chunkSize = 8192 // 8KB chunks
    val time = measureTimeMillis {
        largeFile.bufferedReader().use { reader ->
            val buffer = CharArray(chunkSize)
            var charsRead: Int

            while (reader.read(buffer).also { charsRead = it } != -1) {
                // Process chunk
                val chunk = String(buffer, 0, charsRead)
                // In real app, you would extract data from chunk
            }
        }
    }

    println("Streaming parse time: ${time}ms")
    println("Memory efficient: Yes (uses fixed buffer)")
    println()
}

/** Helper function to create sample metadata. */
private fun createSampleMetadata(ticker: String, formType: String): SecReportMetadata {
    return SecReportMetadata(
            formType = formType,
            filingDate = "2023-11-01",
            reportDate = "2023-09-30",
            fiscalYearEnd = "0930",
            companyName = "$ticker Inc.",
            ticker = ticker,
            cik = "000000000${ticker.hashCode()}",
            accessionNumber = "$ticker-$formType-2023",
            primaryDocument = "$ticker-10k.html"
    )
}
