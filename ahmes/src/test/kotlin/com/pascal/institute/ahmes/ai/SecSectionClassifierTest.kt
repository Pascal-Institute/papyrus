package com.pascal.institute.ahmes.ai

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for SEC Section Classifier
 *
 * Note: Tests avoid DJL native initialization.
 * Only data class and enum tests are included.
 */
class SecSectionClassifierTest {

    @Test
    fun `SecSectionClassifier object should be accessible`() {
        assertNotNull(SecSectionClassifier)
    }

    @Test
    fun `SecSectionType enum should have all types`() {
        val types = SecSectionType.values()

        assertTrue(types.contains(SecSectionType.BUSINESS))
        assertTrue(types.contains(SecSectionType.RISK_FACTORS))
        assertTrue(types.contains(SecSectionType.PROPERTIES))
        assertTrue(types.contains(SecSectionType.LEGAL_PROCEEDINGS))
        assertTrue(types.contains(SecSectionType.MD_AND_A))
        assertTrue(types.contains(SecSectionType.FINANCIAL_STATEMENTS))
        assertTrue(types.contains(SecSectionType.CONTROLS))
        assertTrue(types.contains(SecSectionType.EXECUTIVE_COMPENSATION))
        assertTrue(types.contains(SecSectionType.CORPORATE_GOVERNANCE))
        assertTrue(types.contains(SecSectionType.UNKNOWN))
    }

    @Test
    fun `SecSectionType should have descriptions`() {
        assertEquals("Business Description", SecSectionType.BUSINESS.description)
        assertEquals("Risk Factors", SecSectionType.RISK_FACTORS.description)
        assertEquals("Management's Discussion and Analysis", SecSectionType.MD_AND_A.description)
        assertEquals("Financial Statements", SecSectionType.FINANCIAL_STATEMENTS.description)
    }

    @Test
    fun `SectionClassification should store properties`() {
        val classification = SectionClassification(
            text = "Sample section text",
            sectionType = SecSectionType.BUSINESS,
            confidence = "0.92",
            alternativeTypes = mapOf("RISK_FACTORS" to "0.05")
        )

        assertEquals("Sample section text", classification.text)
        assertEquals(SecSectionType.BUSINESS, classification.sectionType)
        assertEquals(java.math.BigDecimal("0.92"), classification.getConfidenceBigDecimal())
    }

    @Test
    fun `DocumentSummary should store all fields`() {
        val summary = DocumentSummary(
            executiveSummary = "Company performed well",
            keyFindings = listOf("Revenue up 15%", "Margins improved"),
            financialHighlights = listOf("\$50B revenue", "\$10B profit"),
            riskHighlights = listOf("Market competition"),
            outlook = "Positive growth expected",
            investmentImplications = listOf("Strong buy candidate")
        )

        assertEquals("Company performed well", summary.executiveSummary)
        assertEquals(2, summary.keyFindings.size)
        assertEquals(2, summary.financialHighlights.size)
        assertEquals(1, summary.riskHighlights.size)
    }

    @Test
    fun `SecSectionType count`() {
        val types = SecSectionType.values()
        assertTrue(types.size >= 10)
    }

    @Test
    fun `SectionClassification with empty alternative types`() {
        val classification = SectionClassification(
            text = "Test text",
            sectionType = SecSectionType.UNKNOWN,
            confidence = "0.5"
        )

        assertNotNull(classification.alternativeTypes)
        assertTrue(classification.alternativeTypes.isEmpty())
    }

    @Test
    fun `DocumentSummary with empty lists`() {
        val summary = DocumentSummary(
            executiveSummary = "Brief summary",
            keyFindings = emptyList(),
            financialHighlights = emptyList(),
            riskHighlights = emptyList(),
            outlook = "",
            investmentImplications = emptyList()
        )

        assertTrue(summary.keyFindings.isEmpty())
        assertTrue(summary.financialHighlights.isEmpty())
        assertEquals("", summary.outlook)
    }

    @Test
    fun `SecSectionType PROPERTIES description`() {
        assertEquals("Properties", SecSectionType.PROPERTIES.description)
    }

    @Test
    fun `SecSectionType LEGAL_PROCEEDINGS description`() {
        assertEquals("Legal Proceedings", SecSectionType.LEGAL_PROCEEDINGS.description)
    }

    @Test
    fun `SectionClassification confidence precision`() {
        val classification = SectionClassification(
            text = "Test",
            sectionType = SecSectionType.BUSINESS,
            confidence = "0.12345678"
        )

        assertEquals(java.math.BigDecimal("0.12345678"), classification.getConfidenceBigDecimal())
    }

    @Test
    fun `DocumentSummary investmentImplications`() {
        val summary = DocumentSummary(
            executiveSummary = "Summary",
            keyFindings = listOf("Finding 1"),
            financialHighlights = listOf("Highlight 1"),
            riskHighlights = listOf("Risk 1"),
            outlook = "Positive",
            investmentImplications = listOf("Buy", "Hold", "Accumulate")
        )

        assertEquals(3, summary.investmentImplications.size)
        assertTrue(summary.investmentImplications.contains("Buy"))
    }
}
