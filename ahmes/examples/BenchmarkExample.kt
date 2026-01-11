package com.pascal.institute.ahmes.benchmark

import java.io.File

/**
 * Example benchmark runner script.
 *
 * Demonstrates how to run benchmarks on multiple documents and generate a summary report.
 */
fun main() {
    println("=== Ahmes Benchmark Suite ===\n")

    val runner = BenchmarkRunner()
    val testDataDir = File("ahmes/test-data")
    val groundTruthDir = File(testDataDir, "ground-truth")
    val documentsDir = File(testDataDir, "documents")
    val resultsDir = File(testDataDir, "results")

    // Ensure results directory exists
    resultsDir.mkdirs()

    // Find all ground truth files
    val groundTruthFiles =
            groundTruthDir.listFiles { file -> file.extension == "json" } ?: emptyArray()

    if (groundTruthFiles.isEmpty()) {
        println("⚠️  No ground truth files found in ${groundTruthDir.absolutePath}")
        println("   Please add ground truth JSON files to continue.")
        return
    }

    println("Found ${groundTruthFiles.size} ground truth file(s)\n")

    val results = mutableListOf<ValidationResult>()

    // Run benchmarks
    groundTruthFiles.forEach { groundTruthFile ->
        println("--- Processing: ${groundTruthFile.name} ---")

        // Find corresponding document
        val docName = groundTruthFile.nameWithoutExtension + ".html"
        val documentFile = File(documentsDir, docName)

        if (!documentFile.exists()) {
            println("⚠️  Document not found: $docName")
            println("   Skipping...\n")
            return@forEach
        }

        try {
            // Run benchmark
            val result = runner.runBenchmark(documentFile, groundTruthFile)
            results.add(result)

            // Print summary
            printResultSummary(result)

            // Save detailed results
            val resultFile = File(resultsDir, "${groundTruthFile.nameWithoutExtension}-result.json")
            runner.saveResult(result, resultFile)
            println("   Results saved to: ${resultFile.name}\n")
        } catch (e: Exception) {
            println("❌ Benchmark failed: ${e.message}\n")
        }
    }

    // Print overall summary
    if (results.isNotEmpty()) {
        printOverallSummary(results)
    }
}

/** Print summary for a single result. */
fun printResultSummary(result: ValidationResult) {
    val statusIcon = if (result.passed) "✅" else "❌"

    println("   $statusIcon Status: ${if (result.passed) "PASSED" else "FAILED"}")
    println("   Overall Accuracy: ${String.format("%.1f%%", result.overallAccuracy * 100)}")
    println("")

    // Sections
    with(result.sectionResults) {
        println(
                "   📄 Sections: $correctCount/$expectedCount (${String.format("%.1f%%", accuracy * 100)})"
        )
        if (missingSections.isNotEmpty()) {
            println("      Missing: ${missingSections.take(3).joinToString()}")
        }
    }

    // Metrics
    with(result.metricResults) {
        println(
                "   💰 Metrics: $correctCount/$expectedCount (${String.format("%.1f%%", accuracy * 100)})"
        )
    }

    // Risk Factors
    with(result.riskFactorResults) {
        println(
                "   ⚠️  Risk Factors: $matchedCount/$expectedCount (${String.format("%.1f%%", accuracy * 100)})"
        )
    }

    // Performance
    with(result.performanceResults) {
        val perfIcon = if (meetsPerformanceTarget) "🚀" else "🐌"
        println("   $perfIcon Performance: ${parseTimeMs}ms, ${memoryUsedMB}MB")
        println("      Score: ${String.format("%.1f%%", performanceScore * 100)}")
    }

    // Warnings
    if (result.warnings.isNotEmpty()) {
        println("   ⚠️  Warnings: ${result.warnings.size}")
        result.warnings.take(3).forEach { warning -> println("      - $warning") }
    }

    // Errors
    if (result.errors.isNotEmpty()) {
        println("   ❌ Errors: ${result.errors.size}")
        result.errors.forEach { error -> println("      - $error") }
    }
}

/** Print overall benchmark summary. */
fun printOverallSummary(results: List<ValidationResult>) {
    println("═══════════════════════════════════════")
    println("Overall Summary")
    println("═══════════════════════════════════════")

    val totalTests = results.size
    val passed = results.count { it.passed }
    val failed = totalTests - passed

    println("Total Tests: $totalTests")
    println("Passed: $passed (${String.format("%.1f%%", passed * 100.0 / totalTests)})")
    println("Failed: $failed")
    println("")

    // Average metrics
    val avgAccuracy = results.map { it.overallAccuracy }.average()
    val avgParseTime = results.map { it.performanceResults.parseTimeMs }.average()
    val avgMemory = results.map { it.performanceResults.memoryUsedMB }.average()

    println("Average Accuracy: ${String.format("%.1f%%", avgAccuracy * 100)}")
    println("Average Parse Time: ${String.format("%.0fms", avgParseTime)}")
    println("Average Memory: ${String.format("%.0fMB", avgMemory)}")
    println("")

    // Best/Worst
    val best = results.maxByOrNull { it.overallAccuracy }
    val worst = results.minByOrNull { it.overallAccuracy }

    println(
            "Best: ${best?.documentId} (${String.format("%.1f%%", (best?.overallAccuracy ?: 0.0) * 100)})"
    )
    println(
            "Worst: ${worst?.documentId} (${String.format("%.1f%%", (worst?.overallAccuracy ?: 0.0) * 100)})"
    )

    println("═══════════════════════════════════════\n")

    // Final verdict
    if (failed == 0) {
        println("🎉 All benchmarks passed!")
    } else {
        println("⚠️  $failed benchmark(s) failed. See details above.")
    }
}
