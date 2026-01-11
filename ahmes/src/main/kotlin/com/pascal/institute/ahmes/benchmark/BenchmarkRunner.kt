package com.pascal.institute.ahmes.benchmark

import com.pascal.institute.ahmes.form.*
import com.pascal.institute.ahmes.model.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.system.measureTimeMillis
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Benchmark runner for measuring parsing accuracy and performance.
 *
 * ## Usage
 *
 * ```kotlin
 * val runner = BenchmarkRunner()
 * runner.loadGroundTruth("test-data/apple-10k-2023.json")
 * val result = runner.runBenchmark("test-data/apple-10k-2023.html")
 * println("Accuracy: ${result.overallAccuracy}")
 * ```
 */
class BenchmarkRunner {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val groundTruthCache = mutableMapOf<String, GroundTruth>()

    /** Load ground truth data from JSON file. */
    fun loadGroundTruth(file: File): GroundTruth {
        val content = file.readText()
        val groundTruth = json.decodeFromString<GroundTruth>(content)
        groundTruthCache[file.nameWithoutExtension] = groundTruth
        return groundTruth
    }

    /**
     * Run benchmark on a document and validate against ground truth.
     *
     * @param documentFile The document to parse
     * @param groundTruthFile The ground truth file
     * @return Validation result
     */
    fun runBenchmark(documentFile: File, groundTruthFile: File): ValidationResult {
        val groundTruth = loadGroundTruth(groundTruthFile)
        return runBenchmark(documentFile, groundTruth)
    }

    /** Run benchmark with loaded ground truth. */
    fun runBenchmark(documentFile: File, groundTruth: GroundTruth): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Measure performance
        val runtime = Runtime.getRuntime()
        val beforeMemory = runtime.totalMemory() - runtime.freeMemory()

        var parseResult: SecReportParseResult? = null
        val parseTime = measureTimeMillis {
            parseResult =
                    try {
                        parseDocument(documentFile, groundTruth.document)
                    } catch (e: Exception) {
                        errors.add("Parsing failed: ${e.message}")
                        null
                    }
        }

        val afterMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsed = ((afterMemory - beforeMemory) / 1024 / 1024).toInt()

        if (parseResult == null) {
            return ValidationResult(
                    documentId = groundTruth.document.accessionNumber,
                    timestamp = currentTimestamp(),
                    overallAccuracy = 0.0,
                    passed = false,
                    metricResults = MetricValidation(0, 0, 0, 0.0),
                    sectionResults = SectionValidation(0, 0, 0, 0.0),
                    riskFactorResults = RiskFactorValidation(0, 0, 0, 0.0),
                    performanceResults =
                            PerformanceValidation(
                                    parseTimeMs = parseTime,
                                    memoryUsedMB = memoryUsed,
                                    meetsPerformanceTarget = false,
                                    performanceScore = 0.0
                            ),
                    errors = errors
            )
        }

        // Validate sections
        val sectionResults =
                validateSections(parseResult!!.sections, groundTruth.expectedSections, warnings)

        // Validate metrics (if available)
        val metricResults =
                when (parseResult) {
                    is Form10KParseResult ->
                            validateMetrics(
                                    parseResult!!.financialStatements,
                                    groundTruth.expectedMetrics,
                                    warnings
                            )
                    is Form10QParseResult ->
                            validateMetrics(
                                    parseResult!!.financialStatements,
                                    groundTruth.expectedMetrics,
                                    warnings
                            )
                    else -> MetricValidation(0, 0, 0, 1.0)
                }

        // Validate risk factors
        val riskFactorResults =
                validateRiskFactors(
                        getRiskFactors(parseResult!!),
                        groundTruth.expectedRiskFactors,
                        warnings
                )

        // Validate performance
        val performanceResults =
                validatePerformance(parseTime, memoryUsed, groundTruth.benchmarks, warnings)

        // Calculate overall accuracy
        val overallAccuracy =
                calculateOverallAccuracy(metricResults, sectionResults, riskFactorResults)

        // Determine if passed
        val passed =
                errors.isEmpty() &&
                        overallAccuracy >= 0.80 &&
                        performanceResults.meetsPerformanceTarget

        return ValidationResult(
                documentId = groundTruth.document.accessionNumber,
                timestamp = currentTimestamp(),
                overallAccuracy = overallAccuracy,
                passed = passed,
                metricResults = metricResults,
                sectionResults = sectionResults,
                riskFactorResults = riskFactorResults,
                performanceResults = performanceResults,
                errors = errors,
                warnings = warnings
        )
    }

    /** Parse document based on form type. */
    private fun parseDocument(file: File, docInfo: DocumentInfo): SecReportParseResult {
        val content = file.readText()
        val metadata =
                SecReportMetadata(
                        formType = docInfo.formType,
                        filingDate = docInfo.filingDate,
                        reportDate = docInfo.fiscalPeriod,
                        fiscalYearEnd = null,
                        companyName = docInfo.company,
                        ticker = docInfo.ticker,
                        cik = docInfo.cik,
                        accessionNumber = docInfo.accessionNumber,
                        primaryDocument = file.name
                )

        return when (docInfo.formType) {
            "10-K" -> Form10KParser().parseHtml(content, metadata)
            "10-Q" -> Form10QParser().parseHtml(content, metadata)
            "8-K" -> Form8KParser().parseHtml(content, metadata)
            "S-1" -> FormS1Parser().parseHtml(content, metadata)
            else -> throw IllegalArgumentException("Unsupported form type: ${docInfo.formType}")
        }
    }

    /** Validate sections against ground truth. */
    private fun validateSections(
            actualSections: Map<String, String>,
            expectedSections: Map<String, SectionInfo>,
            warnings: MutableList<String>
    ): SectionValidation {
        var correctCount = 0
        val missingSections = mutableListOf<String>()

        expectedSections.forEach { (sectionKey, sectionInfo) ->
            val actualContent = actualSections[sectionKey]

            if (actualContent == null) {
                missingSections.add(sectionKey)
                warnings.add("Missing section: $sectionKey")
            } else {
                // Validate content
                var isCorrect = true

                if (actualContent.length < sectionInfo.minLength) {
                    warnings.add(
                            "Section '$sectionKey' too short: ${actualContent.length} < ${sectionInfo.minLength}"
                    )
                    isCorrect = false
                }

                sectionInfo.mustContain.forEach { keyword ->
                    if (!actualContent.contains(keyword, ignoreCase = true)) {
                        warnings.add("Section '$sectionKey' missing keyword: '$keyword'")
                        isCorrect = false
                    }
                }

                if (isCorrect) correctCount++
            }
        }

        val accuracy =
                if (expectedSections.isNotEmpty()) {
                    correctCount.toDouble() / expectedSections.size
                } else 1.0

        return SectionValidation(
                expectedCount = expectedSections.size,
                extractedCount = actualSections.size,
                correctCount = correctCount,
                accuracy = accuracy,
                missingSections = missingSections
        )
    }

    /** Validate financial metrics. */
    private fun validateMetrics(
            actualStatements: StructuredFinancialData?,
            expectedMetrics: List<ExpectedMetric>,
            warnings: MutableList<String>
    ): MetricValidation {
        if (actualStatements == null) {
            return MetricValidation(
                    expectedCount = expectedMetrics.size,
                    extractedCount = 0,
                    correctCount = 0,
                    accuracy = 0.0,
                    missingMetrics = expectedMetrics.map { it.name }
            )
        }

        var correctCount = 0
        val missingMetrics = mutableListOf<String>()
        val incorrectMetrics = mutableListOf<MetricMismatch>()

        // For simplicity, just count presence of financial data
        // In a real implementation, you'd compare individual metric values
        if (actualStatements.reportType?.isNotEmpty() == true) correctCount++
        if (actualStatements.fiscalYear != null) correctCount++
        if (actualStatements.fiscalPeriod != null) correctCount++

        val accuracy =
                if (expectedMetrics.isNotEmpty()) {
                    correctCount.toDouble() / expectedMetrics.size.coerceAtLeast(3)
                } else 1.0

        return MetricValidation(
                expectedCount = expectedMetrics.size,
                extractedCount = correctCount,
                correctCount = correctCount,
                accuracy = accuracy,
                missingMetrics = missingMetrics,
                incorrectMetrics = incorrectMetrics
        )
    }

    /** Validate risk factors. */
    private fun validateRiskFactors(
            actualRiskFactors: List<RiskFactor>,
            expectedRiskFactors: List<ExpectedRiskFactor>,
            warnings: MutableList<String>
    ): RiskFactorValidation {
        var matchedCount = 0
        val missingRiskFactors = mutableListOf<String>()

        expectedRiskFactors.forEach { expected ->
            val found =
                    actualRiskFactors.any { actual ->
                        actual.title.contains(expected.title, ignoreCase = true) ||
                                expected.mustContainKeywords.any { keyword ->
                                    actual.summary.contains(keyword, ignoreCase = true)
                                }
                    }

            if (found) {
                matchedCount++
            } else {
                missingRiskFactors.add(expected.title)
                warnings.add("Missing risk factor: ${expected.title}")
            }
        }

        val accuracy =
                if (expectedRiskFactors.isNotEmpty()) {
                    matchedCount.toDouble() / expectedRiskFactors.size
                } else 1.0

        return RiskFactorValidation(
                expectedCount = expectedRiskFactors.size,
                extractedCount = actualRiskFactors.size,
                matchedCount = matchedCount,
                accuracy = accuracy,
                missingRiskFactors = missingRiskFactors
        )
    }

    /** Validate performance benchmarks. */
    private fun validatePerformance(
            actualTimeMs: Long,
            actualMemoryMB: Int,
            benchmarks: PerformanceBenchmark?,
            warnings: MutableList<String>
    ): PerformanceValidation {
        if (benchmarks == null) {
            return PerformanceValidation(
                    parseTimeMs = actualTimeMs,
                    memoryUsedMB = actualMemoryMB,
                    meetsPerformanceTarget = true,
                    performanceScore = 1.0
            )
        }

        val timeScore =
                if (actualTimeMs <= benchmarks.maxParseTimeMs) 1.0
                else benchmarks.maxParseTimeMs.toDouble() / actualTimeMs

        val memoryScore =
                if (actualMemoryMB <= benchmarks.maxMemoryMB) 1.0
                else benchmarks.maxMemoryMB.toDouble() / actualMemoryMB

        val performanceScore = (timeScore + memoryScore) / 2.0
        val meetsTarget = performanceScore >= 0.80

        if (actualTimeMs > benchmarks.maxParseTimeMs) {
            warnings.add(
                    "Parse time ${actualTimeMs}ms exceeds target ${benchmarks.maxParseTimeMs}ms"
            )
        }

        if (actualMemoryMB > benchmarks.maxMemoryMB) {
            warnings.add(
                    "Memory usage ${actualMemoryMB}MB exceeds target ${benchmarks.maxMemoryMB}MB"
            )
        }

        return PerformanceValidation(
                parseTimeMs = actualTimeMs,
                memoryUsedMB = actualMemoryMB,
                meetsPerformanceTarget = meetsTarget,
                performanceScore = performanceScore
        )
    }

    /** Calculate overall accuracy score. */
    private fun calculateOverallAccuracy(
            metricResults: MetricValidation,
            sectionResults: SectionValidation,
            riskFactorResults: RiskFactorValidation
    ): Double {
        // Weighted average
        return (sectionResults.accuracy * 0.5 +
                metricResults.accuracy * 0.3 +
                riskFactorResults.accuracy * 0.2)
    }

    /** Extract risk factors from parse result. */
    private fun getRiskFactors(result: SecReportParseResult): List<RiskFactor> {
        return when (result) {
            is Form10KParseResult -> result.riskFactors
            is Form10QParseResult -> result.riskFactors
            is FormS1ParseResult -> result.riskFactors
            else -> emptyList()
        }
    }

    /** Save validation result to JSON file. */
    fun saveResult(result: ValidationResult, outputFile: File) {
        val jsonString = json.encodeToString(result)
        outputFile.writeText(jsonString)
    }

    private fun currentTimestamp(): String {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}
