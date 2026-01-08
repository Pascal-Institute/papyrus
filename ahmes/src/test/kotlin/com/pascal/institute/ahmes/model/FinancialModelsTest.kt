package com.pascal.institute.ahmes.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal

/**
 * Tests for Financial Models
 */
class FinancialModelsTest {

    @Test
    fun `FinancialMetric should store and retrieve values correctly`() {
        val metric = FinancialMetric(
            name = "Total Revenue",
            value = "$1.5 billion",
            rawValue = "1500000000",
            context = "Fiscal Year 2024"
        )

        assertEquals("Total Revenue", metric.name)
        assertEquals("$1.5 billion", metric.value)
        assertEquals("1500000000", metric.rawValue)
        assertEquals("Fiscal Year 2024", metric.context)
    }

    @Test
    fun `FinancialMetric getRawValueBigDecimal should convert correctly`() {
        val metric = FinancialMetric(
            name = "Net Income",
            value = "$500M",
            rawValue = "500000000.50"
        )

        val bigDecimal = metric.getRawValueBigDecimal()
        assertNotNull(bigDecimal)
        assertEquals(BigDecimal("500000000.50"), bigDecimal)
    }

    @Test
    fun `FinancialMetric with null rawValue should return null BigDecimal`() {
        val metric = FinancialMetric(
            name = "Unknown Metric",
            value = "N/A"
        )

        assertNull(metric.getRawValueBigDecimal())
    }

    @Test
    fun `FinancialRatio should store values correctly`() {
        val ratio = FinancialRatio(
            name = "Current Ratio",
            value = "2.5",
            formattedValue = "2.50",
            description = "Measures liquidity",
            interpretation = "Strong liquidity position",
            healthStatus = HealthStatus.EXCELLENT,
            category = RatioCategory.LIQUIDITY
        )

        assertEquals(HealthStatus.EXCELLENT, ratio.healthStatus)
        assertEquals(RatioCategory.LIQUIDITY, ratio.category)
        assertEquals(BigDecimal("2.5"), ratio.getValueBigDecimal())
    }

    @Test
    fun `HealthStatus enum should have all expected values`() {
        val statuses = HealthStatus.values()

        assertTrue(statuses.contains(HealthStatus.EXCELLENT))
        assertTrue(statuses.contains(HealthStatus.GOOD))
        assertTrue(statuses.contains(HealthStatus.NEUTRAL))
        assertTrue(statuses.contains(HealthStatus.CAUTION))
        assertTrue(statuses.contains(HealthStatus.WARNING))
    }

    @Test
    fun `RatioCategory enum should have all expected values`() {
        val categories = RatioCategory.values()

        assertTrue(categories.contains(RatioCategory.PROFITABILITY))
        assertTrue(categories.contains(RatioCategory.LIQUIDITY))
        assertTrue(categories.contains(RatioCategory.SOLVENCY))
        assertTrue(categories.contains(RatioCategory.EFFICIENCY))
        assertTrue(categories.contains(RatioCategory.VALUATION))
    }

    @Test
    fun `FinancialHealthScore should have valid range`() {
        val score = FinancialHealthScore(
            overallScore = 85,
            grade = "A",
            summary = "Strong financial health",
            strengths = listOf("High liquidity", "Low debt"),
            weaknesses = listOf("Declining margins"),
            recommendations = listOf("Monitor margin trends")
        )

        assertTrue(score.overallScore in 0..100)
        assertEquals("A", score.grade)
        assertEquals(2, score.strengths.size)
        assertEquals(1, score.weaknesses.size)
    }

    @Test
    fun `FinancialAnalysis should aggregate data correctly`() {
        val metrics = listOf(
            FinancialMetric("Revenue", "$1B", "1000000000"),
            FinancialMetric("Net Income", "$100M", "100000000")
        )

        val analysis = FinancialAnalysis(
            fileName = "apple-10k.htm",
            companyName = "Apple Inc.",
            reportType = "10-K",
            periodEnding = "2024-09-30",
            cik = 320193,
            metrics = metrics,
            rawContent = "Sample content",
            summary = "Annual report summary"
        )

        assertEquals("Apple Inc.", analysis.companyName)
        assertEquals(2, analysis.metrics.size)
        assertEquals(320193, analysis.cik)
    }

    @Test
    fun `FinancialAnalysis should handle optional fields`() {
        val analysis = FinancialAnalysis(
            fileName = "test.htm",
            companyName = null,
            reportType = null,
            periodEnding = null,
            metrics = emptyList(),
            rawContent = "",
            summary = ""
        )

        assertNull(analysis.companyName)
        assertTrue(analysis.metrics.isEmpty())
        assertTrue(analysis.ratios.isEmpty())
        assertNull(analysis.healthScore)
    }
}
