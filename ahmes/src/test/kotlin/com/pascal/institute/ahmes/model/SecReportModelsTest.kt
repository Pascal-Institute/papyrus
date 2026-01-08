package com.pascal.institute.ahmes.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for SEC Report Models
 */
class SecReportModelsTest {

    @Test
    fun `SecReportType should parse form types correctly`() {
        assertEquals(SecReportType.FORM_10K, SecReportType.fromFormType("10-K"))
        assertEquals(SecReportType.FORM_10K, SecReportType.fromFormType("10K"))
        assertEquals(SecReportType.FORM_10Q, SecReportType.fromFormType("10-Q"))
        assertEquals(SecReportType.FORM_10Q, SecReportType.fromFormType("10Q"))
        assertEquals(SecReportType.FORM_8K, SecReportType.fromFormType("8-K"))
        assertEquals(SecReportType.FORM_8K, SecReportType.fromFormType("8K"))
        assertEquals(SecReportType.FORM_S1, SecReportType.fromFormType("S-1"))
        assertEquals(SecReportType.FORM_S1, SecReportType.fromFormType("S1"))
        assertEquals(SecReportType.FORM_DEF14A, SecReportType.fromFormType("DEF14A"))
        assertEquals(SecReportType.FORM_20F, SecReportType.fromFormType("20-F"))
        assertEquals(SecReportType.FORM_20F, SecReportType.fromFormType("20F"))
    }

    @Test
    fun `SecReportType should return UNKNOWN for invalid types`() {
        assertEquals(SecReportType.UNKNOWN, SecReportType.fromFormType("INVALID"))
        assertEquals(SecReportType.UNKNOWN, SecReportType.fromFormType(""))
        assertEquals(SecReportType.UNKNOWN, SecReportType.fromFormType("XYZ-123"))
        assertEquals(SecReportType.UNKNOWN, SecReportType.fromFormType("RANDOMFORM"))
    }

    @Test
    fun `SecReportType should have correct display names`() {
        assertEquals("10-K Annual Report", SecReportType.FORM_10K.displayName)
        assertEquals("10-Q Quarterly Report", SecReportType.FORM_10Q.displayName)
        assertEquals("8-K Current Report", SecReportType.FORM_8K.displayName)
        assertEquals("S-1 IPO Registration", SecReportType.FORM_S1.displayName)
        assertEquals("DEF 14A Proxy Statement", SecReportType.FORM_DEF14A.displayName)
        assertEquals("20-F Foreign Annual Report", SecReportType.FORM_20F.displayName)
    }

    @Test
    fun `SecReportType importance should be ordered correctly`() {
        assertTrue(SecReportType.FORM_10K.importance >= SecReportType.FORM_10Q.importance)
        assertTrue(SecReportType.FORM_10Q.importance >= SecReportType.FORM_8K.importance)
        assertTrue(SecReportType.FORM_8K.importance > SecReportType.UNKNOWN.importance)
    }

    @Test
    fun `Form10KItem enum should have all items`() {
        val items = Form10KItem.values()

        assertTrue(items.any { it.itemNumber == "1" })
        assertTrue(items.any { it.itemNumber == "1A" })
        assertTrue(items.any { it.itemNumber == "7" })
        assertTrue(items.any { it.itemNumber == "8" })
    }

    @Test
    fun `Form10KItem should have correct titles`() {
        assertEquals("Business", Form10KItem.ITEM_1.title)
        assertEquals("Risk Factors", Form10KItem.ITEM_1A.title)
        assertEquals("Management's Discussion and Analysis", Form10KItem.ITEM_7.title)
        assertEquals("Financial Statements and Supplementary Data", Form10KItem.ITEM_8.title)
    }

    @Test
    fun `Form10QItem enum should have all parts`() {
        val items = Form10QItem.values()

        assertTrue(items.any { it.part == "Part I" })
        assertTrue(items.any { it.part == "Part II" })
    }

    @Test
    fun `Form8KItem enum should have all event types`() {
        val items = Form8KItem.values()

        assertTrue(items.any { it.itemNumber == "1.01" })
        assertTrue(items.any { it.itemNumber == "2.02" })
        assertTrue(items.any { it.itemNumber == "5.02" })
    }

    @Test
    fun `Form8KItem should have categories`() {
        assertEquals("Corporate", Form8KItem.ITEM_1_01.category)
        assertEquals("Financial", Form8KItem.ITEM_2_02.category)
        assertEquals("Securities", Form8KItem.ITEM_3_01.category)
        assertEquals("Governance", Form8KItem.ITEM_5_02.category)
    }

    @Test
    fun `SecReportType should handle case insensitive input`() {
        assertEquals(SecReportType.FORM_10K, SecReportType.fromFormType("10-k"))
        assertEquals(SecReportType.FORM_10Q, SecReportType.fromFormType("10-q"))
        assertEquals(SecReportType.FORM_8K, SecReportType.fromFormType("8-k"))
    }
}
