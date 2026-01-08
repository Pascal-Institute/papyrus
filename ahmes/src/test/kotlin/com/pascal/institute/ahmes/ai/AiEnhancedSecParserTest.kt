package com.pascal.institute.ahmes.ai

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.math.BigDecimal

/**
 * Tests for AI-Enhanced SEC Parser
 *
 * Note: Some tests require DJL native libraries to be available.
 * Tests will be skipped if DJL is not properly configured.
 */
class AiEnhancedSecParserTest {

    /**
     * Check if DJL is available without triggering full initialization
     */
    private fun isDjlAvailable(): Boolean {
        return try {
            // Just check if the class exists, don't initialize the engine
            Class.forName("ai.djl.engine.Engine")
            // Try a lightweight check
            DjlModelManager.isAvailable()
        } catch (e: Throwable) {
            false
        }
    }

    @Test
    fun `AiEnhancedSecParser object should be accessible`() {
        assertNotNull(AiEnhancedSecParser)
    }

    @Test
    fun `AiEnhancedParseResult data class works correctly`() {
        val result = AiEnhancedParseResult(
            metrics = emptyList(),
            documentName = "test-report.htm",
            parserType = "html",
            cleanedContent = "Sample SEC content",
            metadata = mapOf("source" to "test"),
            aiConfidence = "0.85",
            aiModelUsed = "rule-based"
        )

        assertEquals("test-report.htm", result.documentName)
        assertEquals("0.85", result.aiConfidence)
        assertEquals(BigDecimal("0.85"), result.getAiConfidenceBigDecimal())
        assertEquals("rule-based", result.aiModelUsed)
        assertEquals("Sample SEC content", result.cleanedContent)
    }

    @Test
    fun `AiEnhancementOptions default values`() {
        val options = AiEnhancementOptions()

        assertTrue(options.enableSentimentAnalysis)
        assertTrue(options.enableEntityExtraction)
        assertTrue(options.enableSectionClassification)
        assertTrue(options.enableRiskAnalysis)
        assertTrue(options.enableDocumentSummary)
        assertEquals(50000, options.maxTextLength)
        assertEquals(0.5, options.minConfidenceThreshold)
    }

    @Test
    fun `AiEnhancementOptions custom values`() {
        val options = AiEnhancementOptions(
            enableSentimentAnalysis = false,
            enableEntityExtraction = true,
            enableSectionClassification = false,
            enableRiskAnalysis = false,
            enableDocumentSummary = false,
            maxTextLength = 10000,
            minConfidenceThreshold = 0.75
        )

        assertFalse(options.enableSentimentAnalysis)
        assertTrue(options.enableEntityExtraction)
        assertFalse(options.enableSectionClassification)
        assertEquals(10000, options.maxTextLength)
        assertEquals(0.75, options.minConfidenceThreshold)
    }

    @Test
    fun `AiEnhancedParseResult with empty collections`() {
        val result = AiEnhancedParseResult(
            metrics = emptyList(),
            documentName = "empty.htm",
            parserType = "html",
            cleanedContent = "",
            metadata = emptyMap()
        )

        assertTrue(result.metrics.isEmpty())
        assertTrue(result.entities.isEmpty())
        assertTrue(result.sectionClassifications.isEmpty())
        assertTrue(result.riskAnalysis.isEmpty())
        assertNull(result.sentiment)
        assertNull(result.documentSummary)
    }

    @Test
    fun `AiEnhancedParseResult aiConfidence BigDecimal conversion`() {
        val result = AiEnhancedParseResult(
            metrics = emptyList(),
            documentName = "test.htm",
            parserType = "html",
            cleanedContent = "content",
            metadata = emptyMap(),
            aiConfidence = "0.123456789"
        )

        assertEquals(BigDecimal("0.123456789"), result.getAiConfidenceBigDecimal())
    }

    @Test
    fun `AiEnhancementOptions copy with modifications`() {
        val original = AiEnhancementOptions()
        val modified = original.copy(enableSentimentAnalysis = false)

        assertTrue(original.enableSentimentAnalysis)
        assertFalse(modified.enableSentimentAnalysis)
        assertEquals(original.maxTextLength, modified.maxTextLength)
    }
}
