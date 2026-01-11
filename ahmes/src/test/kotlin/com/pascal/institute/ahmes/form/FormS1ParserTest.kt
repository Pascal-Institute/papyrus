package com.pascal.institute.ahmes.form

import com.pascal.institute.ahmes.model.*
import kotlin.test.*

/**
 * Test suite for FormS1Parser
 *
 * Tests S-1 IPO registration statement parsing including:
 * - Prospectus summary extraction
 * - Use of proceeds
 * - Offering price and share count
 * - Business description
 * - Risk factors
 */
class FormS1ParserTest {

    private val parser = FormS1Parser()

    private fun createTestMetadata(cik: String = "0001018724") =
            SecReportMetadata(
                    formType = "S-1",
                    filingDate = "2024-02-15",
                    reportDate = "2024-02-15",
                    fiscalYearEnd = "1231",
                    companyName = "Tech Startup Inc.",
                    ticker = "TECH",
                    cik = cik,
                    accessionNumber = "$cik-24-000005",
                    primaryDocument = "test-s1.htm"
            )

    @Test
    fun `parseHtml should extract basic S-1 structure`() {
        val html =
                """
            <html>
            <body>
                <h1>PROSPECTUS</h1>

                <h2>Prospectus Summary</h2>
                <p>We are a leading provider of cloud-based software solutions.
                This offering consists of 10,000,000 shares of common stock at a
                price range of ${'$'}15.00 to ${'$'}17.00 per share.</p>

                <h2>Risk Factors</h2>
                <p>Investing in our company involves significant risks including:</p>
                <ul>
                    <li>We have a history of losses</li>
                    <li>Intense competition in the SaaS market</li>
                </ul>

                <h2>Use of Proceeds</h2>
                <p>We intend to use the net proceeds from this offering for:</p>
                <ul>
                    <li>Working capital and general corporate purposes</li>
                    <li>Research and development</li>
                    <li>Potential acquisitions</li>
                </ul>

                <h2>Our Business</h2>
                <p>Founded in 2020, we provide enterprise software solutions
                that enable businesses to streamline their operations.</p>

                <h2>Dilution</h2>
                <p>If you invest in our common stock, your ownership interest
                will be immediately diluted to the extent of the difference
                between the offering price and the pro forma net tangible book value.</p>
            </body>
            </html>
        """.trimIndent()

        val result = parser.parseHtml(html, createTestMetadata())

        assertNotNull(result)
        assertTrue(result.sections.isNotEmpty())

        // Verify prospectus summary
        assertNotNull(result.prospectus)
        assertTrue(
                result.prospectus!!.contains("cloud-based") ||
                        result.prospectus!!.contains("software")
        )

        // Verify use of proceeds
        assertNotNull(result.useOfProceeds)
        assertTrue(
                result.useOfProceeds!!.contains("proceeds") ||
                        result.useOfProceeds!!.contains("working capital")
        )

        // Verify business description
        assertNotNull(result.businessDescription)
        assertTrue(
                result.businessDescription!!.contains("business") ||
                        result.businessDescription!!.contains("enterprise")
        )

        // Verify dilution section
        assertNotNull(result.dilution)
        assertTrue(result.dilution!!.contains("dilut") || result.dilution!!.contains("ownership"))
    }

    @Test
    fun `extractOfferingPrice should recognize price ranges`() {
        val content1 = "Price range: ${'$'}15.00 to ${'$'}17.00 per share"
        val content2 = "Offering price between ${'$'}20.00 to ${'$'}24.00 per share"
        val content3 = "Expected price: ${'$'}10.50 to ${'$'}12.50 per share"

        val extractOfferingPrice =
                parser::class.java.getDeclaredMethod("extractOfferingPrice", String::class.java)
        extractOfferingPrice.isAccessible = true

        val price1 = extractOfferingPrice.invoke(parser, content1) as String?
        val price2 = extractOfferingPrice.invoke(parser, content2) as String?
        val price3 = extractOfferingPrice.invoke(parser, content3) as String?

        assertNotNull(price1)
        assertTrue(price1.contains("15.00") && price1.contains("17.00"))

        assertNotNull(price2)
        assertTrue(price2.contains("20.00") && price2.contains("24.00"))

        assertNotNull(price3)
        assertTrue(price3.contains("10.50") && price3.contains("12.50"))
    }

    @Test
    fun `extractSharesOffered should recognize share counts`() {
        val content1 = "This prospectus relates to 10,000,000 shares of common stock"
        val content2 = "We are offering 5,500,000 shares of common stock"
        val content3 = "Total offering: 25000000 shares of common stock"

        val extractSharesOffered =
                parser::class.java.getDeclaredMethod("extractSharesOffered", String::class.java)
        extractSharesOffered.isAccessible = true

        val shares1 = extractSharesOffered.invoke(parser, content1) as String?
        val shares2 = extractSharesOffered.invoke(parser, content2) as String?
        val shares3 = extractSharesOffered.invoke(parser, content3) as String?

        assertNotNull(shares1)
        assertTrue(shares1.contains("10,000,000") || shares1.contains("10000000"))

        assertNotNull(shares2)
        assertTrue(shares2.contains("5,500,000") || shares2.contains("5500000"))

        assertNotNull(shares3)
        assertTrue(shares3.contains("25000000") || shares3.contains("25,000,000"))
    }

    @Test
    fun `extractSections should handle S-1 section structure`() {
        val content =
                """
            PROSPECTUS

            Prospectus Summary
            Our company overview.

            Risk Factors
            Investment risks.

            Use of Proceeds
            How we'll use the money.

            Dividend Policy
            No dividends planned.

            Dilution
            Dilution information.

            Capitalization
            Capital structure.

            Our Business
            Detailed business description.

            Management's Discussion and Analysis
            MD&A section.

            Executive Compensation
            Compensation details.

            Principal Stockholders
            Major shareholders.

            Underwriting
            Underwriter information.
        """.trimIndent()

        val sections = parser.extractSections(content)

        assertTrue(sections.isNotEmpty())
        assertNotNull(sections["Prospectus Summary"])
        assertNotNull(sections["Risk Factors"])
        assertNotNull(sections["Use of Proceeds"])
        assertNotNull(sections["Dilution"])
        assertNotNull(sections["Business"])
        assertNotNull(sections["Underwriting"])
    }

    @Test
    fun `parseHtml should extract offering details`() {
        val html =
                """
            <html>
            <body>
                <p>We are offering 15,000,000 shares of common stock.
                The initial public offering price is expected to be between
                ${'$'}18.00 to ${'$'}20.00 per share.</p>

                <p>Total gross proceeds are expected to be approximately
                ${'$'}270 million to ${'$'}300 million.</p>
            </body>
            </html>
        """.trimIndent()

        val result = parser.parseHtml(html, createTestMetadata())

        assertNotNull(result.offeringPrice)
        assertTrue(
                result.offeringPrice!!.contains("18.00") && result.offeringPrice!!.contains("20.00")
        )

        assertNotNull(result.sharesOffered)
        assertTrue(
                result.sharesOffered!!.contains("15,000,000") ||
                        result.sharesOffered!!.contains("15000000")
        )
    }

    @Test
    fun `parseHtml should extract underwriting information`() {
        val html =
                """
            <html>
            <body>
                <h2>Underwriting</h2>
                <p>Goldman Sachs & Co. LLC and Morgan Stanley & Co. LLC are acting
                as joint book-running managers and representatives of the underwriters.</p>
                <p>The underwriters have agreed to purchase the shares from us at a
                price of ${'$'}16.50 per share.</p>
            </body>
            </html>
        """.trimIndent()

        val result = parser.parseHtml(html, createTestMetadata())

        assertNotNull(result.underwriting)
        assertTrue(
                result.underwriting!!.contains("Goldman Sachs") ||
                        result.underwriting!!.contains("underwriter") ||
                        result.underwriting!!.contains("book-running")
        )
    }

    @Test
    fun `parseText should handle plain text S-1 format`() {
        val text =
                """
            REGISTRATION STATEMENT UNDER THE SECURITIES ACT OF 1933

            FORM S-1

            Tech Startup Inc.

            Prospectus Summary

            We are a technology company focused on AI solutions.

            Risk Factors

            Our business is subject to numerous risks.
        """.trimIndent()

        val result = parser.parseText(text, createTestMetadata())

        assertNotNull(result)
        assertTrue(result.sections.isNotEmpty())
    }

    @Test
    fun `parseHtml should preserve raw content`() {
        val html = """<html><body><h1>S-1 REGISTRATION</h1></body></html>"""

        val result = parser.parseHtml(html, createTestMetadata())

        assertNotNull(result.rawContent)
        assertEquals(html, result.rawContent)
    }

    @Test
    fun `parser should create parse result with metadata`() {
        val html = """<html><body><p>Test S-1</p></body></html>"""
        val metadata = createTestMetadata()

        val result = parser.parseHtml(html, metadata)

        assertEquals(metadata.cik, result.metadata.cik)
        assertEquals(metadata.formType, result.metadata.formType)
        assertEquals(metadata.companyName, result.metadata.companyName)
    }

    @Test
    fun `parseHtml should handle missing optional sections gracefully`() {
        val html =
                """
            <html>
            <body>
                <h2>Prospectus Summary</h2>
                <p>Minimal S-1 content.</p>
            </body>
            </html>
        """.trimIndent()

        val result = parser.parseHtml(html, createTestMetadata())

        assertNotNull(result)
        assertNotNull(result.prospectus)
        // Optional sections can be null
    }
}
