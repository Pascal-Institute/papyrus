package com.pascal.institute.ahmes.form

import com.pascal.institute.ahmes.model.*
import kotlin.test.*

/**
 * Test suite for Form10KParser
 *
 * Tests basic 10-K annual report parsing functionality
 */
class Form10KParserTest {

    private val parser = Form10KParser()

    private fun createTestMetadata(cik: String = "0000320193") =
            SecReportMetadata(
                    formType = "10-K",
                    filingDate = "2023-11-03",
                    reportDate = "2023-09-30",
                    fiscalYearEnd = "0930",
                    companyName = "Apple Inc.",
                    ticker = "AAPL",
                    cik = cik,
                    accessionNumber = "$cik-23-000077",
                    primaryDocument = "test-10k.htm"
            )

    @Test
    fun `parseHtml should extract basic 10-K structure`() {
        val html =
                """
            <html>
            <body>
                <h2>ITEM 1. BUSINESS</h2>
                <p>Apple Inc. designs, manufactures, and markets smartphones.</p>

                <h2>ITEM 7. MANAGEMENT'S DISCUSSION AND ANALYSIS</h2>
                <h3>Overview</h3>
                <p>Revenue for fiscal 2023 increased.</p>
            </body>
            </html>
        """.trimIndent()

        val result = parser.parseHtml(html, createTestMetadata())

        assertNotNull(result)
        assertTrue(result.sections.isNotEmpty(), "Sections should be extracted")
        assertNotNull(result.businessDescription, "Item 1 should be extracted")
        assertTrue(result.businessDescription!!.contains("Apple Inc."))
    }

    @Test
    fun `extractSections should parse 10-K item headers`() {
        val content =
                """
            ITEM 1. BUSINESS
            Our company operates globally.

            Item 1A. Risk Factors
            Various risks exist.

            ITEM 2 - PROPERTIES
            We own several facilities.

            Item 3: Legal Proceedings
           No significant proceedings.
        """.trimIndent()

        val sections = parser.extractSections(content)

        assertNotNull(sections["Item 1"])
        assertNotNull(sections["Item 1A"])
        assertNotNull(sections["Item 2"])
        assertNotNull(sections["Item 3"])
    }

    @Test
    fun `extractSections should handle Part structure`() {
        val content =
                """
            PART I

            Item 1. Business
            Business content

            PART II

            Item 5. Market Data
            Market data content
        """.trimIndent()

        val sections = parser.extractSections(content)

        assertNotNull(sections["Part I"])
        assertNotNull(sections["Part II"])
    }

    @Test
    fun `parseText should handle plain text 10-K format`() {
        val text =
                """
            FORM 10-K

            For the fiscal year ended September 30, 2023

            Apple Inc.

            ITEM 1. BUSINESS

            Apple Inc. designs, manufactures, and markets smartphones.
        """.trimIndent()

        val result = parser.parseText(text, createTestMetadata())

        assertNotNull(result)
        assertTrue(result.sections.isNotEmpty())
    }

    @Test
    fun `parseHtml should preserve raw content`() {
        val html = """<html><body><h1>FORM 10-K</h1></body></html>"""

        val result = parser.parseHtml(html, createTestMetadata())

        assertNotNull(result.rawContent)
        assertEquals(html, result.rawContent)
    }

    @Test
    fun `parser should handle case-insensitive item headers`() {
        val content =
                """
            item 1. business
            Content

            ITEM 2. PROPERTIES
            More content
        """.trimIndent()

        val sections = parser.extractSections(content)

        assertNotNull(sections["Item 1"])
        assertNotNull(sections["Item 2"])
    }

    @Test
    fun `parser should create parse result with metadata`() {
        val html = """<html><body><h2>ITEM 1. BUSINESS</h2><p>Test</p></body></html>"""
        val metadata = createTestMetadata()

        val result = parser.parseHtml(html, metadata)

        assertEquals(metadata.cik, result.metadata.cik)
        assertEquals(metadata.formType, result.metadata.formType)
        assertEquals(metadata.companyName, result.metadata.companyName)
    }
}
