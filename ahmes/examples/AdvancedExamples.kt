package com.pascal.institute.ahmes.examples

import com.pascal.institute.ahmes.ai.DjlModelManager
import com.pascal.institute.ahmes.form.Form10KParser
import com.pascal.institute.ahmes.model.*
import com.pascal.institute.ahmes.parser.EnhancedFinancialParser
import java.math.BigDecimal
import kotlinx.coroutines.*

/**
 * Advanced Examples - Ahmes Library
 *
 * Demonstrates advanced features including:
 * - AI-powered analysis
 * - Parallel processing
 * - Batch operations
 * - Performance optimization
 */
fun main() = runBlocking {
    println("=== Ahmes Library - Advanced Examples ===\n")

    // Example 1: AI-Powered Sentiment Analysis
    example1_SentimentAnalysis()

    // Example 2: Parallel Processing Multiple Documents
    example2_ParallelProcessing()

    // Example 3: Batch Financial Ratio Calculation
    example3_BatchRatioCalculation()

    // Example 4: Custom XBRL Data Extraction
    example4_XbrlExtraction()
}

/**
 * Example 1: AI-Powered Sentiment Analysis
 *
 * Uses Deep Learning models to analyze sentiment in MD&A sections.
 */
suspend fun example1_SentimentAnalysis() = coroutineScope {
    println("--- Example 1: AI-Powered Sentiment Analysis ---")

    // Check if AI models are available
    if (!DjlModelManager.isAvailable()) {
        println("⚠️  AI models not available (DJL not initialized)")
        println("   Continuing with fallback analysis...")
        println()
        return@coroutineScope
    }

    val mdaText =
            """
        Revenue for fiscal 2023 increased significantly due to strong demand
        for our products globally. However, we face ongoing challenges from
        supply chain constraints and increased competition. Despite these
        headwinds, we remain cautiously optimistic about future growth prospects
        and continue to invest heavily in research and development.
    """.trimIndent()

    try {
        // Get sentiment predictor
        val sentimentPredictor = DjlModelManager.getPredictor(DjlModelManager.ModelType.SENTIMENT)

        println("Analyzing MD&A text...")
        println("Text: ${mdaText.take(100)}...\n")

        // Perform sentiment analysis
        val result = sentimentPredictor.predict(mdaText)

        println("Sentiment Analysis Result:")
        println("  Classification: ${result.classifications.firstOrNull()?.className ?: "N/A"}")
        println(
                "  Confidence: ${(result.classifications.firstOrNull()?.probability ?: 0.0) * 100}%"
        )

        // Analyze by sentence
        println("\nSentence-level Analysis:")
        mdaText.split(". ").forEach { sentence ->
            if (sentence.isNotBlank()) {
                val sentenceResult = sentimentPredictor.predict(sentence)
                val sentiment = sentenceResult.classifications.firstOrNull()
                println("  • ${sentence.take(50)}...")
                println("    → ${sentiment?.className} (${(sentiment?.probability ?: 0.0) * 100}%)")
            }
        }
    } catch (e: Exception) {
        println("❌ Sentiment analysis failed: ${e.message}")
        println("   Using fallback keyword-based analysis...")
    }

    println()
}

/**
 * Example 2: Parallel Processing Multiple Documents
 *
 * Efficiently process multiple SEC filings concurrently.
 */
suspend fun example2_ParallelProcessing() = coroutineScope {
    println("--- Example 2: Parallel Document Processing ---")

    // Simulate multiple documents to process
    val documents =
            listOf(
                    Triple("AAPL", "10-K", "Apple Inc. FY2023 10-K content..."),
                    Triple("MSFT", "10-K", "Microsoft Corp FY2023 10-K content..."),
                    Triple("GOOGL", "10-Q", "Alphabet Inc. Q2 2023 10-Q content..."),
                    Triple("TSLA", "10-Q", "Tesla Inc. Q3 2023 10-Q content..."),
                    Triple("AMZN", "10-K", "Amazon.com Inc. FY2023 10-K content...")
            )

    println("Processing ${documents.size} documents in parallel...")

    val startTime = System.currentTimeMillis()

    // Process all documents concurrently
    val results =
            documents
                    .map { (ticker, formType, content) ->
                        async(Dispatchers.Default) {
                            try {
                                delay(100) // Simulate processing time

                                val metadata =
                                        SecReportMetadata(
                                                formType = formType,
                                                filingDate = "2023-11-01",
                                                reportDate = "2023-09-30",
                                                fiscalYearEnd = "1231",
                                                companyName = "$ticker Inc.",
                                                ticker = ticker,
                                                cik = "000000000${ticker.hashCode()}",
                                                accessionNumber = "0000000001-23-000001",
                                                primaryDocument = "$ticker-10k.html"
                                        )

                                val parser = Form10KParser()
                                val result = parser.parseText(content, metadata)

                                Triple(ticker, "SUCCESS", result.sections.size)
                            } catch (e: Exception) {
                                Triple(ticker, "FAILED", 0)
                            }
                        }
                    }
                    .awaitAll()

    val elapsed = System.currentTimeMillis() - startTime

    // Print results
    println("\nResults (processed in ${elapsed}ms):")
    results.forEach { (ticker, status, sections) ->
        println("  $ticker: $status (${sections} sections)")
    }

    val successCount = results.count { it.second == "SUCCESS" }
    println(
            "\nSuccess Rate: $successCount/${documents.size} (${successCount * 100 / documents.size}%)"
    )

    println()
}

/**
 * Example 3: Batch Financial Ratio Calculation
 *
 * Calculate comprehensive financial ratios from extracted metrics.
 */
fun example3_BatchRatioCalculation() {
    println("--- Example 3: Batch Financial Ratio Calculation ---")

    // Sample financial metrics extracted from a 10-K
    val metrics =
            listOf(
                    ExtendedFinancialMetric(
                            name = "Total Revenue",
                            value = "$383.3B",
                            rawValue = "383285000000",
                            unit = MetricUnit.DOLLARS,
                            category = MetricCategory.REVENUE,
                            period = "FY 2023"
                    ),
                    ExtendedFinancialMetric(
                            name = "Cost of Sales",
                            value = "$214.1B",
                            rawValue = "214137000000",
                            unit = MetricUnit.DOLLARS,
                            category = MetricCategory.COST_OF_REVENUE,
                            period = "FY 2023"
                    ),
                    ExtendedFinancialMetric(
                            name = "Gross Profit",
                            value = "$169.1B",
                            rawValue = "169148000000",
                            unit = MetricUnit.DOLLARS,
                            category = MetricCategory.GROSS_PROFIT,
                            period = "FY 2023"
                    ),
                    ExtendedFinancialMetric(
                            name = "Operating Income",
                            value = "$114.3B",
                            rawValue = "114301000000",
                            unit = MetricUnit.DOLLARS,
                            category = MetricCategory.OPERATING_INCOME,
                            period = "FY 2023"
                    ),
                    ExtendedFinancialMetric(
                            name = "Net Income",
                            value = "$97.0B",
                            rawValue = "96995000000",
                            unit = MetricUnit.DOLLARS,
                            category = MetricCategory.NET_INCOME,
                            period = "FY 2023"
                    ),
                    ExtendedFinancialMetric(
                            name = "Total Assets",
                            value = "$352.8B",
                            rawValue = "352755000000",
                            unit = MetricUnit.DOLLARS,
                            category = MetricCategory.TOTAL_ASSETS,
                            period = "FY 2023"
                    ),
                    ExtendedFinancialMetric(
                            name = "Total Equity",
                            value = "$62.1B",
                            rawValue = "62146000000",
                            unit = MetricUnit.DOLLARS,
                            category = MetricCategory.TOTAL_EQUITY,
                            period = "FY 2023"
                    )
            )

    println("Calculating ratios from ${metrics.size} metrics...\n")

    // Calculate ratios using EnhancedFinancialParser
    val ratios = EnhancedFinancialParser.calculateRatios(metrics)

    // Group by category
    val profitability = ratios.filter { it.category == RatioCategory.PROFITABILITY }
    val efficiency = ratios.filter { it.category == RatioCategory.EFFICIENCY }
    val leverage = ratios.filter { it.category == RatioCategory.LEVERAGE }

    println("Profitability Ratios:")
    profitability.forEach { ratio ->
        println("  ${ratio.name}: ${ratio.displayValue}")
        println("    ${ratio.description}")
        println("    Status: ${ratio.health} - ${ratio.interpretation}")
        println()
    }

    if (efficiency.isNotEmpty()) {
        println("Efficiency Ratios:")
        efficiency.forEach { ratio ->
            println("  ${ratio.name}: ${ratio.displayValue}")
            println("    Status: ${ratio.health}")
        }
        println()
    }

    if (leverage.isNotEmpty()) {
        println("Leverage Ratios:")
        leverage.forEach { ratio ->
            println("  ${ratio.name}: ${ratio.displayValue}")
            println("    Status: ${ratio.health}")
        }
        println()
    }

    // Summary
    val healthy = ratios.count { it.health == HealthStatus.GOOD }
    val caution = ratios.count { it.health == HealthStatus.CAUTION }
    val poor = ratios.count { it.health == HealthStatus.POOR }

    println("Financial Health Summary:")
    println("  Good: $healthy")
    println("  Caution: $caution")
    println("  Poor: $poor")
    println("  Overall: ${if (healthy > caution + poor) "HEALTHY" else "NEEDS ATTENTION"}")

    println()
}

/**
 * Example 4: Custom XBRL Data Extraction
 *
 * Extract and process XBRL tagged data from SEC filings.
 */
fun example4_XbrlExtraction() {
    println("--- Example 4: XBRL Data Extraction ---")

    // Sample XBRL-tagged HTML content
    val xbrlContent =
            """
        <html xmlns:us-gaap="http://fasb.org/us-gaap/2023">
        <body>
            <p>
                <span>Net Sales</span>
                <ix:nonFraction name="us-gaap:RevenueFromContractWithCustomerExcludingAssessedTax"
                                contextRef="FY2023"
                                unitRef="usd"
                                decimals="-6">383285000000</ix:nonFraction>
            </p>
            <p>
                <span>Cost of Sales</span>
                <ix:nonFraction name="us-gaap:CostOfGoodsAndServicesSold"
                                contextRef="FY2023"
                                unitRef="usd"
                                decimals="-6">214137000000</ix:nonFraction>
            </p>
            <p>
                <span>Gross Margin</span>
                <ix:nonFraction name="us-gaap:GrossProfit"
                                contextRef="FY2023"
                                unitRef="usd"
                                decimals="-6">169148000000</ix:nonFraction>
            </p>
        </body>
        </html>
    """.trimIndent()

    println("Extracting XBRL data...")

    // Note: This is a simplified example
    // In production, use InlineXbrlExtractor
    val xbrlPattern =
            Regex(
                    """ix:nonFraction\s+name="([^"]+)"\s+contextRef="([^"]+)"\s+unitRef="([^"]+)"\s+decimals="([^"]+)">([^<]+)""",
                    RegexOption.IGNORE_CASE
            )

    val facts =
            xbrlPattern
                    .findAll(xbrlContent)
                    .map { match ->
                        val (name, context, unit, decimals, value) = match.destructured
                        mapOf(
                                "name" to name,
                                "context" to context,
                                "unit" to unit,
                                "value" to BigDecimal(value),
                                "formatted" to formatXbrlValue(BigDecimal(value))
                        )
                    }
                    .toList()

    println("Extracted ${facts.size} XBRL facts:\n")

    facts.forEach { fact ->
        println("Concept: ${fact["name"]}")
        println("  Period: ${fact["context"]}")
        println("  Value: ${fact["formatted"]}")
        println("  Raw: ${fact["value"]}")
        println()
    }

    // Calculate derived metrics
    if (facts.size >= 2) {
        val revenue = facts[0]["value"] as BigDecimal
        val costOfSales = facts[1]["value"] as BigDecimal
        val grossMargin = (revenue - costOfSales).divide(revenue, 4, java.math.RoundingMode.HALF_UP)

        println("Derived Metrics:")
        println("  Gross Margin %: ${grossMargin.multiply(BigDecimal(100))}%")
    }

    println()
}

/** Helper: Format XBRL monetary values */
fun formatXbrlValue(value: BigDecimal): String {
    return when {
        value >= BigDecimal("1000000000") ->
                "$${value.divide(BigDecimal("1000000000"), 2, java.math.RoundingMode.HALF_UP)}B"
        value >= BigDecimal("1000000") ->
                "$${value.divide(BigDecimal("1000000"), 2, java.math.RoundingMode.HALF_UP)}M"
        else -> "$${value}"
    }
}

/** Example 5: Performance Benchmarking */
fun example5_Benchmark() {
    println("--- Example 5: Performance Benchmarking ---")

    val iterations = 100
    val sampleContent =
            """
        Revenue: $383,285,000,000
        Cost of Sales: $214,137,000,000
        Gross Profit: $169,148,000,000
    """.trimIndent()

    val times = mutableListOf<Long>()

    repeat(iterations) {
        val start = System.nanoTime()

        EnhancedFinancialParser.parseFinancialMetrics(sampleContent)

        val elapsed = System.nanoTime() - start
        times.add(elapsed / 1_000_000) // Convert to milliseconds
    }

    println("Benchmark Results ($iterations iterations):")
    println("  Average: ${times.average()}ms")
    println("  Min: ${times.minOrNull()}ms")
    println("  Max: ${times.maxOrNull()}ms")
    println("  Median: ${times.sorted()[times.size / 2]}ms")

    println()
}
