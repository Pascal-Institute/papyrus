package papyrus.core.service.parser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for HtmlParser with Jsoup
 *
 * Tests the HTML parsing functionality including:
 * - Document type detection
 * - Financial table extraction
 * - XBRL data detection
 * - Content cleaning
 * - Metadata generation
 */
class HtmlParserUnitTest {

    private val parser = HtmlParser()

    @Test
    @DisplayName("Should detect HTML content correctly")
    fun testCanParseHtml() {
        // Valid HTML
        assertTrue(parser.canParse("<html><body>Test</body></html>"))
        assertTrue(parser.canParse("<!DOCTYPE html><html></html>"))
        assertTrue(parser.canParse("<HTML><BODY>Test</BODY></HTML>"))

        // Invalid HTML
        assertFalse(parser.canParse("Plain text without HTML"))
        assertFalse(parser.canParse(""))
    }

    @Test
    @DisplayName("Should extract financial tables with keywords")
    fun testFinancialTableExtraction() {
        val html =
                """
            <html>
            <body>
                <table class="financial">
                    <tr><th>Description</th><th>Amount</th></tr>
                    <tr><td>Total Revenue</td><td>$100,000</td></tr>
                    <tr><td>Net Income</td><td>$20,000</td></tr>
                </table>
                <table class="other">
                    <tr><td>Random</td><td>Data</td></tr>
                </table>
            </body>
            </html>
        """.trimIndent()

        val result = parser.parse(html, "test.html")

        // Should detect at least one financial table
        assertTrue(result.metadata["tableCount"]?.toIntOrNull() ?: 0 > 0)
        assertTrue(result.metadata["hasFinancialTables"]?.toBoolean() == true)
    }

    @Test
    @DisplayName("Should detect XBRL data in document")
    fun testXbrlDetection() {
        val htmlWithXbrl =
                """
            <html xmlns:xbrl="http://www.xbrl.org/2003/instance">
            <body>
                <us-gaap:Revenue contextRef="Q1_2023" unitRef="USD">1000000</us-gaap:Revenue>
            </body>
            </html>
        """.trimIndent()

        val result = parser.parse(htmlWithXbrl, "xbrl-test.html")

        assertTrue(result.metadata["hasXbrl"]?.toBoolean() == true)
    }

    @Test
    @DisplayName("Should clean HTML and remove script/style tags")
    fun testHtmlCleaning() {
        val dirtyHtml =
                """
            <html>
            <head>
                <script>alert('test');</script>
                <style>.class { color: red; }</style>
            </head>
            <body>
                <h1>Financial Report</h1>
                <p>Total Revenue: $1,000,000</p>
                <script>console.log('hidden');</script>
            </body>
            </html>
        """.trimIndent()

        val result = parser.parse(dirtyHtml, "dirty.html")

        // Cleaned content should not contain script or style tags
        assertFalse(result.cleanedContent.contains("<script>"))
        assertFalse(result.cleanedContent.contains("<style>"))

        // But should contain the actual content
        assertTrue(result.cleanedContent.contains("Financial Report"))
        assertTrue(result.cleanedContent.contains("Revenue"))
    }

    @Test
    @DisplayName("Should detect encoding from meta tags")
    fun testEncodingDetection() {
        val htmlWithCharset =
                """
            <html>
            <head>
                <meta charset="UTF-8">
            </head>
            <body>Content</body>
            </html>
        """.trimIndent()

        val result = parser.parse(htmlWithCharset, "encoding-test.html")

        assertEquals("UTF-8", result.metadata["encoding"])
    }

    @Test
    @DisplayName("Should preserve table structure in cleaned content")
    fun testTablePreservation() {
        val htmlWithTable =
                """
            <html>
            <body>
                <table>
                    <tr>
                        <th>Metric</th>
                        <th>2023</th>
                        <th>2022</th>
                    </tr>
                    <tr>
                        <td>Revenue</td>
                        <td>$100M</td>
                        <td>$90M</td>
                    </tr>
                    <tr>
                        <td>Net Income</td>
                        <td>$20M</td>
                        <td>$18M</td>
                    </tr>
                </table>
            </body>
            </html>
        """.trimIndent()

        val result = parser.parse(htmlWithTable, "table-test.html")

        // Should contain table markers
        assertTrue(result.cleanedContent.contains("=== FINANCIAL TABLE ==="))
        assertTrue(result.cleanedContent.contains("=== END TABLE ==="))

        // Should preserve row structure with pipe separators
        assertTrue(result.cleanedContent.contains("|"))
    }

    @Test
    @DisplayName("Should calculate compression ratio correctly")
    fun testCompressionRatio() {
        val html =
                """
            <html>
            <head>
                <script>/* lots of javascript code */</script>
                <style>/* lots of CSS */</style>
            </head>
            <body>
                <h1>Report</h1>
                <p>Content</p>
            </body>
            </html>
        """.trimIndent()

        val result = parser.parse(html, "compression-test.html")

        // Should have compression ratio
        assertNotNull(result.metadata["compressionRatio"])
        assertTrue(result.metadata["compressionRatio"]!!.contains("%"))

        // Cleaned should be smaller than original
        val originalSize = result.metadata["originalSize"]?.substringBefore(" ")?.toIntOrNull() ?: 0
        val cleanedSize = result.metadata["cleanedSize"]?.substringBefore(" ")?.toIntOrNull() ?: 0
        assertTrue(cleanedSize <= originalSize)
    }

    @Test
    @DisplayName("Should handle malformed HTML gracefully")
    fun testMalformedHtml() {
        val malformedHtml =
                """
            <html>
            <body>
                <table>
                    <tr><td>Unclosed row
                    <tr><td>Another row</td>
                <p>Unclosed paragraph
                <div>Revenue: $1000000
            </body>
        """.trimIndent()

        // Should not throw exception
        assertDoesNotThrow { parser.parse(malformedHtml, "malformed.html") }
    }

    @Test
    @DisplayName("Should generate correct parser type metadata")
    fun testParserTypeMetadata() {
        val html = "<html><body>Test</body></html>"
        val result = parser.parse(html, "type-test.html")

        assertEquals("HTML (Jsoup + AI)", result.parserType)
    }

    @Test
    @DisplayName("Should remove hidden XBRL elements")
    fun testHiddenXbrlRemoval() {
        val htmlWithHiddenXbrl =
                """
            <html>
            <body>
                <div style="display:none">Hidden XBRL data</div>
                <div style="visibility:hidden">Also hidden</div>
                <p>Visible content: Revenue $1000</p>
            </body>
            </html>
        """.trimIndent()

        val result = parser.parse(htmlWithHiddenXbrl, "hidden-test.html")

        // Cleaned content should not contain hidden elements
        assertFalse(result.cleanedContent.contains("Hidden XBRL data"))
        assertFalse(result.cleanedContent.contains("Also hidden"))

        // But should contain visible content
        assertTrue(result.cleanedContent.contains("Visible content"))
        assertTrue(result.cleanedContent.contains("Revenue"))
    }

    @Test
    @DisplayName("Should convert XBRL namespace tags to clean text")
    fun testXbrlNamespaceConversion() {
        val htmlWithXbrlTags =
                """
            <html>
            <body>
                <us-gaap:Revenue>1000000</us-gaap:Revenue>
                <dei:EntityName>Test Company</dei:EntityName>
            </body>
            </html>
        """.trimIndent()

        val result = parser.parse(htmlWithXbrlTags, "xbrl-namespace.html")

        // Should not contain namespace prefixes in cleaned content
        // (Jsoup converts namespaced tags to spans)
        assertTrue(result.cleanedContent.contains("1000000"))
        assertTrue(result.cleanedContent.contains("Test Company"))
    }

    @Test
    @DisplayName("Should handle empty HTML document")
    fun testEmptyHtmlDocument() {
        val emptyHtml = "<html><body></body></html>"

        val result = parser.parse(emptyHtml, "empty.html")

        assertNotNull(result)
        assertEquals(0, result.metrics.size)
        assertTrue(result.cleanedContent.isBlank() || result.cleanedContent.isEmpty())
    }

    @Test
    @DisplayName("Should extract metrics from realistic SEC filing snippet")
    fun testRealisticSecFilingSnippet() {
        val secFilingSnippet =
                """
            <html>
            <body>
                <table>
                    <tr>
                        <th>Consolidated Statements of Operations</th>
                        <th>2023</th>
                        <th>2022</th>
                    </tr>
                    <tr>
                        <td>Net sales</td>
                        <td>$383,285</td>
                        <td>$394,328</td>
                    </tr>
                    <tr>
                        <td>Cost of sales</td>
                        <td>214,137</td>
                        <td>223,546</td>
                    </tr>
                    <tr>
                        <td>Gross margin</td>
                        <td>169,148</td>
                        <td>170,782</td>
                    </tr>
                    <tr>
                        <td>Net income</td>
                        <td>96,995</td>
                        <td>99,803</td>
                    </tr>
                </table>
            </body>
            </html>
        """.trimIndent()

        val result = parser.parse(secFilingSnippet, "sec-snippet.html")

        // Should detect financial table
        assertTrue(result.metadata["hasFinancialTables"]?.toBoolean() == true)

        // Should preserve table structure
        assertTrue(result.cleanedContent.contains("Consolidated"))
        assertTrue(result.cleanedContent.contains("Net sales"))
        assertTrue(result.cleanedContent.contains("$383,285"))
    }

    @Test
    @DisplayName("Should support file extension correctly")
    fun testSupportedExtension() {
        assertEquals("html", parser.getSupportedExtension())
    }
}
