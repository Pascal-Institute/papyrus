package com.pascal.institute.ahmes.benchmark

import kotlinx.serialization.Serializable

/**
 * Ground truth data for benchmark validation.
 *
 * Contains manually verified correct answers for parsing tasks, used to measure parsing accuracy.
 */
@Serializable
data class GroundTruth(
        /** Metadata about the source document */
        val document: DocumentInfo,

        /** Expected financial metrics to be extracted */
        val expectedMetrics: List<ExpectedMetric>,

        /** Expected risk factors */
        val expectedRiskFactors: List<ExpectedRiskFactor>,

        /** Expected sections and their content summaries */
        val expectedSections: Map<String, SectionInfo>,

        /** Performance benchmarks */
        val benchmarks: PerformanceBenchmark? = null
)

@Serializable
data class DocumentInfo(
        /** Company information */
        val company: String,
        val ticker: String,
        val cik: String,

        /** Filing information */
        val formType: String,
        val filingDate: String,
        val fiscalPeriod: String,

        /** File metadata */
        val fileName: String,
        val fileSize: Long,
        val accessionNumber: String
)

@Serializable
data class ExpectedMetric(
        /** Metric identification */
        val name: String,
        val category: String,

        /** Expected values */
        val value: String,
        val rawValue: String,
        val unit: String,

        /** Validation info */
        val period: String? = null,
        val confidence: Double = 1.0,

        /** Source location (for debugging) */
        val sourceSection: String? = null,
        val sourceLine: Int? = null
)

@Serializable
data class ExpectedRiskFactor(
        val title: String,
        val category: String,
        val severity: String,
        val summary: String,

        /** Validation */
        val minLength: Int = 50,
        val mustContainKeywords: List<String> = emptyList()
)

@Serializable
data class SectionInfo(
        /** Section identification */
        val title: String,
        val type: String, // "Item", "Part", etc.

        /** Content validation */
        val minLength: Int,
        val mustContain: List<String> = emptyList(),
        val mustNotContain: List<String> = emptyList(),

        /** Subsections */
        val hasSubsections: Boolean = false,
        val subsectionCount: Int? = null
)

@Serializable
data class PerformanceBenchmark(
        /** Expected parsing time (milliseconds) */
        val maxParseTimeMs: Long,

        /** Expected memory usage (MB) */
        val maxMemoryMB: Int,

        /** Accuracy targets */
        val minMetricAccuracy: Double = 0.90,
        val minSectionAccuracy: Double = 0.95,
        val minRiskFactorAccuracy: Double = 0.85
)

/** Validation result comparing actual parsing output to ground truth. */
@Serializable
data class ValidationResult(
        val documentId: String,
        val timestamp: String,

        /** Overall scores */
        val overallAccuracy: Double,
        val passed: Boolean,

        /** Detailed results */
        val metricResults: MetricValidation,
        val sectionResults: SectionValidation,
        val riskFactorResults: RiskFactorValidation,
        val performanceResults: PerformanceValidation,

        /** Issues found */
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList()
)

@Serializable
data class MetricValidation(
        val expectedCount: Int,
        val extractedCount: Int,
        val correctCount: Int,
        val accuracy: Double,
        val missingMetrics: List<String> = emptyList(),
        val incorrectMetrics: List<MetricMismatch> = emptyList()
)

@Serializable
data class MetricMismatch(
        val metricName: String,
        val expected: String,
        val actual: String,
        val reason: String
)

@Serializable
data class SectionValidation(
        val expectedCount: Int,
        val extractedCount: Int,
        val correctCount: Int,
        val accuracy: Double,
        val missingSections: List<String> = emptyList()
)

@Serializable
data class RiskFactorValidation(
        val expectedCount: Int,
        val extractedCount: Int,
        val matchedCount: Int,
        val accuracy: Double,
        val missingRiskFactors: List<String> = emptyList()
)

@Serializable
data class PerformanceValidation(
        val parseTimeMs: Long,
        val memoryUsedMB: Int,
        val meetsPerformanceTarget: Boolean,
        val performanceScore: Double // 0.0 to 1.0
)
