package com.pascal.institute.ahmes.form

import com.pascal.institute.ahmes.model.*
import kotlin.test.*

/**
 * Test suite for Form8KParser
 *
 * Tests 8-K current report parsing
 */
class Form8KParserTest {

    private val parser = Form8KParser()

    private fun createTestMetadata(cik: String = "0000789019") =
            SecReportMetadata(
                    formType = "8-K",
                    filingDate = "2024-01-26",
                    reportDate = "2024-01-25",
                    fiscalYearEnd = "1231",
                    companyName = "Microsoft Corporation",
                    ticker = "MSFT",
                    cik = cik,
                    accessionNumber = "$cik-24-000012",
                    primaryDocument = "test-8k.htm"
            )

    @Test
    fun `parseHtml should extract basic 8-K structure`() {
        val html =
                """
            <html>
            <body>
                <h1>FORM 8-K</h1>
                <p>Date of Report: January 25, 2024</p>

                <h2>Item 2.02 Results of Operations</h2>
                <p>Financial results announced.</p>
            </body>
            </html>
        """.trimIndent()

        val result = parser.parseHtml(html, createTestMetadata())

        assertNotNull(result)
        assertTrue(result.sections.isNotEmpty())
    }

    @Test
    fun `extractSections should handle 8-K item structure`() {
        val content =
                """
            Item 2.02 Results of Operations and Financial Condition
            Q4 earnings.

            Item 5.02 Departure of Directors
            Executive change.
        """.trimIndent()

        val sections = parser.extractSections(content)

        assertNotNull(sections["Item 2.02"])
        assertNotNull(sections["Item 5.02"])
    }

    @Test
    fun `parseHtml should extract executive changes`() {
        val html =
                """
            <html>
            <body>
                <h2>Item 5.02 Departure of Directors or Officers</h2>
                <p>Jane Smith was appointed as CFO.</p>
            </body>
            </html>
        """.trimIndent()

        val result = parser.parseHtml(html, createTestMetadata())

        assertNotNull(result.executiveChanges)
    }

    @Test
    fun `parseHtml should extract acquisitions`() {
        val html =
                """
            <html>
            <body>
                <h2>Item 2.01 Acquisition of Assets</h2>
                <p>Company acquired XYZ Corp.</p>
            </body>
            </html>
        """.trimIndent()

        val result = parser.parseHtml(html, createTestMetadata())

        assertNotNull(result.acquisitions)
    }

    @Test
    fun `parseText should handle plain text format`() {
        val text =
                """
            FORM 8-K

            Item 2.02 Results of Operations
            Earnings announced.
        """.trimIndent()

        val result = parser.parseText(text, createTestMetadata())

        assertNotNull(result)
        assertTrue(result.sections.isNotEmpty())
    }

    @Test
    fun `parseHtml should preserve raw content`() {
        val html = """<html><body><h1>8-K</h1></body></html>"""

        val result = parser.parseHtml(html, createTestMetadata())

        assertNotNull(result.rawContent)
        assertEquals(html, result.rawContent)
    }

    @Test
    fun `parser should create result with metadata`() {
        val html = """<html><body><p>Test</p></body></html>"""
        val metadata = createTestMetadata()

        val result = parser.parseHtml(html, metadata)

        assertEquals(metadata.cik, result.metadata.cik)
        assertEquals(metadata.formType, result.metadata.formType)
    }
}
