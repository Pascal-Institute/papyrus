package com.pascal.institute.ahmes.ai

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for SEC Entity Extractor
 *
 * Note: Tests avoid DJL native initialization.
 * Only data class and enum tests are included.
 */
class SecEntityExtractorTest {

    @Test
    fun `SecEntityExtractor object should be accessible`() {
        assertNotNull(SecEntityExtractor)
    }

    @Test
    fun `FinancialEntityType enum should have all types`() {
        val types = FinancialEntityType.values()

        assertTrue(types.contains(FinancialEntityType.COMPANY_NAME))
        assertTrue(types.contains(FinancialEntityType.TICKER_SYMBOL))
        assertTrue(types.contains(FinancialEntityType.MONETARY_VALUE))
        assertTrue(types.contains(FinancialEntityType.PERCENTAGE))
        assertTrue(types.contains(FinancialEntityType.DATE))
        assertTrue(types.contains(FinancialEntityType.FISCAL_PERIOD))
        assertTrue(types.contains(FinancialEntityType.METRIC_NAME))
        assertTrue(types.contains(FinancialEntityType.EXECUTIVE_NAME))
        assertTrue(types.contains(FinancialEntityType.LOCATION))
    }

    @Test
    fun `FinancialEntity should store all properties`() {
        val entity = FinancialEntity(
            text = "\$50 billion",
            entityType = FinancialEntityType.MONETARY_VALUE,
            value = "50000000000",
            unit = "USD",
            context = "Total Revenue",
            confidence = "0.95"
        )

        assertEquals("\$50 billion", entity.text)
        assertEquals(FinancialEntityType.MONETARY_VALUE, entity.entityType)
        assertEquals(java.math.BigDecimal("50000000000"), entity.getValueBigDecimal())
        assertEquals(java.math.BigDecimal("0.95"), entity.getConfidenceBigDecimal())
    }

    @Test
    fun `FinancialEntity with null value should return null BigDecimal`() {
        val entity = FinancialEntity(
            text = "Apple Inc.",
            entityType = FinancialEntityType.COMPANY_NAME
        )

        assertNull(entity.getValueBigDecimal())
    }

    @Test
    fun `QAAnswer should store all properties`() {
        val answer = QAAnswer(
            question = "What was the revenue?",
            answer = "\$394 billion",
            context = "Apple reported revenue of \$394 billion",
            confidence = "0.88"
        )

        assertEquals("What was the revenue?", answer.question)
        assertEquals("\$394 billion", answer.answer)
        assertEquals(java.math.BigDecimal("0.88"), answer.getConfidenceBigDecimal())
    }

    @Test
    fun `FinancialEntity with empty optional fields`() {
        val entity = FinancialEntity(
            text = "Q1 2024",
            entityType = FinancialEntityType.FISCAL_PERIOD
        )

        assertEquals("Q1 2024", entity.text)
        assertEquals(FinancialEntityType.FISCAL_PERIOD, entity.entityType)
        assertNull(entity.value)
        assertNull(entity.unit)
        assertEquals("", entity.context) // context has default value of empty string
    }

    @Test
    fun `QAAnswer with low confidence`() {
        val answer = QAAnswer(
            question = "What is the CEO name?",
            answer = "Not found in context",
            context = "Some financial data",
            confidence = "0.1"
        )

        assertEquals(java.math.BigDecimal("0.1"), answer.getConfidenceBigDecimal())
    }

    @Test
    fun `FinancialEntityType count`() {
        val types = FinancialEntityType.values()
        assertTrue(types.size >= 9)
    }

    @Test
    fun `FinancialEntity confidence edge case`() {
        val entity = FinancialEntity(
            text = "test",
            entityType = FinancialEntityType.METRIC_NAME,
            confidence = "1.00"
        )

        assertEquals(java.math.BigDecimal("1.00"), entity.getConfidenceBigDecimal())
    }

    @Test
    fun `QAAnswer context preservation`() {
        val longContext = "A".repeat(1000)
        val answer = QAAnswer(
            question = "Test?",
            answer = "Answer",
            context = longContext,
            confidence = "0.5"
        )

        assertEquals(1000, answer.context.length)
    }
}
