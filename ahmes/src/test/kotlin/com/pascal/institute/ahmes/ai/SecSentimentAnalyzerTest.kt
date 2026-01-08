package com.pascal.institute.ahmes.ai

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for SEC Sentiment Analyzer
 *
 * Note: Tests that require DJL native initialization are excluded.
 * Only data class and enum tests are included.
 */
class SecSentimentAnalyzerTest {

    @Test
    fun `SecSentimentAnalyzer object should be accessible`() {
        assertNotNull(SecSentimentAnalyzer)
    }

    @Test
    fun `SentimentResult should have correct properties`() {
        val result = SentimentResult(
            text = "Test text",
            sentiment = "POSITIVE",
            confidence = "0.95",
            details = mapOf("POSITIVE" to "0.95", "NEGATIVE" to "0.05")
        )

        assertTrue(result.isPositive)
        assertFalse(result.isNegative)
        assertFalse(result.isNeutral)
        assertEquals(java.math.BigDecimal("0.95"), result.getConfidenceBigDecimal())
    }

    @Test
    fun `SentimentResult isNegative should work correctly`() {
        val result = SentimentResult(
            text = "Test",
            sentiment = "NEGATIVE",
            confidence = "0.80"
        )

        assertFalse(result.isPositive)
        assertTrue(result.isNegative)
        assertFalse(result.isNeutral)
    }

    @Test
    fun `SentimentResult isNeutral should work correctly`() {
        val result = SentimentResult(
            text = "Test",
            sentiment = "NEUTRAL",
            confidence = "0.70"
        )

        assertFalse(result.isPositive)
        assertFalse(result.isNegative)
        assertTrue(result.isNeutral)
    }

    @Test
    fun `RiskSeverity enum should have all levels`() {
        val severities = RiskSeverity.values()

        assertEquals(5, severities.size)
        assertTrue(severities.contains(RiskSeverity.CRITICAL))
        assertTrue(severities.contains(RiskSeverity.HIGH))
        assertTrue(severities.contains(RiskSeverity.MEDIUM))
        assertTrue(severities.contains(RiskSeverity.LOW))
        assertTrue(severities.contains(RiskSeverity.INFORMATIONAL))
    }

    @Test
    fun `RiskAnalysis should store all properties`() {
        val sentiment = SentimentResult("test", "NEGATIVE", "0.8")
        val risk = RiskAnalysis(
            riskFactor = "Market volatility",
            severity = RiskSeverity.HIGH,
            sentiment = sentiment,
            category = "MARKET",
            keywords = listOf("volatility", "market")
        )

        assertEquals("Market volatility", risk.riskFactor)
        assertEquals(RiskSeverity.HIGH, risk.severity)
        assertEquals("MARKET", risk.category)
        assertEquals(2, risk.keywords.size)
    }

    @Test
    fun `SentimentResult with empty details`() {
        val result = SentimentResult(
            text = "Simple test",
            sentiment = "NEUTRAL",
            confidence = "0.5"
        )

        assertNotNull(result.details)
        assertTrue(result.details.isEmpty())
    }

    @Test
    fun `RiskSeverity ordering`() {
        assertTrue(RiskSeverity.CRITICAL.ordinal < RiskSeverity.HIGH.ordinal)
        assertTrue(RiskSeverity.HIGH.ordinal < RiskSeverity.MEDIUM.ordinal)
        assertTrue(RiskSeverity.MEDIUM.ordinal < RiskSeverity.LOW.ordinal)
        assertTrue(RiskSeverity.LOW.ordinal < RiskSeverity.INFORMATIONAL.ordinal)
    }

    @Test
    fun `SentimentResult confidence edge cases`() {
        val highConfidence = SentimentResult("test", "POSITIVE", "1.0")
        val lowConfidence = SentimentResult("test", "NEGATIVE", "0.0")

        assertEquals(java.math.BigDecimal("1.0"), highConfidence.getConfidenceBigDecimal())
        assertEquals(java.math.BigDecimal("0.0"), lowConfidence.getConfidenceBigDecimal())
    }
}
