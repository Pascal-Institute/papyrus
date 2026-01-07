package papyrus

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import papyrus.core.network.SecApi
import papyrus.core.service.parser.HtmlParser
import papyrus.core.service.parser.ParserFactory

/**
 * Test Jsoup-based HtmlParser with real SEC filings. Note: These are integration tests that require
 * network access.
 */
class HtmlParserTest {

    private fun formatNumber(value: Double): String {
        return when {
            value >= 1_000_000_000 -> String.format("$%.2fB", value / 1_000_000_000)
            value >= 1_000_000 -> String.format("$%.2fM", value / 1_000_000)
            value >= 1_000 -> String.format("$%.2fK", value / 1_000)
            else -> String.format("$%.2f", value)
        }
    }

    @Test
    fun testApple10Q() = runBlocking {
        println("📊 Test Case 1: Apple Inc. 10-Q (Quarterly Report)")

        // Download Apple's latest 10-Q filing
        val submissions = SecApi.getSubmissions(320193) // Apple CIK
        assertNotNull(submissions, "Failed to fetch Apple submissions")

        val filings = SecApi.transformFilings(submissions!!.filings.recent)
        val latest10Q = filings.firstOrNull { it.form == "10-Q" }
        assertNotNull(latest10Q, "No 10-Q filing found")

        println("📄 Filing: ${latest10Q!!.form} - ${latest10Q.filingDate}")

        val url =
                SecApi.getDocumentUrl(
                        cik = "320193",
                        accessionNumber = latest10Q.accessionNumber,
                        primaryDocument = latest10Q.primaryDocument
                )

        val content = SecApi.fetchDocumentContent(url)
        assertFalse(content.startsWith("Error"), "Failed to download document: $content")

        println("✅ Downloaded: ${content.length} characters")

        val htmlParser = HtmlParser()
        assertTrue(htmlParser.canParse(content), "Parser should be able to parse this content")

        val parseResult = htmlParser.parse(content, latest10Q.primaryDocument)

        println("📋 Parse Results:")
        println("  • Metrics Found: ${parseResult.metrics.size}")

        assertTrue(parseResult.metrics.isNotEmpty(), "Should find some metrics in Apple 10-Q")
    }

    @Test
    fun testTesla10K() = runBlocking {
        println("📊 Test Case 2: Tesla Inc. 10-K (Annual Report)")

        val submissions = SecApi.getSubmissions(1318605) // Tesla CIK
        assertNotNull(submissions, "Failed to fetch Tesla submissions")

        val filings = SecApi.transformFilings(submissions!!.filings.recent)
        val latest10K = filings.firstOrNull { it.form == "10-K" }
        assertNotNull(latest10K, "No 10-K filing found")

        val url =
                SecApi.getDocumentUrl(
                        cik = "1318605",
                        accessionNumber = latest10K!!.accessionNumber,
                        primaryDocument = latest10K.primaryDocument
                )

        val content = SecApi.fetchDocumentContent(url)
        assertFalse(content.startsWith("Error"), "Failed to download document: $content")

        val parser = ParserFactory.getParserByContent(content)
        val parseResult = parser.parse(content, latest10K.primaryDocument)

        println("📋 Parse Results:")
        println("  • Metrics Found: ${parseResult.metrics.size}")

        // We expect plenty of metrics in a 10-K
        assertTrue(parseResult.metrics.size > 10, "Should find > 10 metrics in Tesla 10-K")
    }

    @Test
    fun testMicrosoft10K() = runBlocking {
        println("📊 Test Case 3: Microsoft Corporation 10-K (Annual Report)")

        val submissions = SecApi.getSubmissions(789019)
        assertNotNull(submissions)

        val filings = SecApi.transformFilings(submissions!!.filings.recent)
        val latest10K = filings.firstOrNull { it.form == "10-K" }
        assertNotNull(latest10K)

        val url =
                SecApi.getDocumentUrl(
                        cik = "789019",
                        accessionNumber = latest10K!!.accessionNumber,
                        primaryDocument = latest10K.primaryDocument
                )

        val content = SecApi.fetchDocumentContent(url)
        assertFalse(content.startsWith("Error"))

        val htmlParser = HtmlParser()
        val parseResult = htmlParser.parse(content, latest10K.primaryDocument)

        println("  • Metrics Found: ${parseResult.metrics.size}")
        assertTrue(parseResult.metrics.isNotEmpty())
    }

    @Test
    fun testParserFactoryAutoDetection() {
        println("📊 Test Case 5: ParserFactory Auto-Detection")

        // Test 1: HTML Content
        val htmlContent =
                """
            <!DOCTYPE html>
            <html>
            <head><title>Test HTML</title></head>
            <body>
                <h1>Financial Report</h1>
                <p>Revenue: $1,000,000</p>
            </body>
            </html>
        """.trimIndent()

        val htmlParser = ParserFactory.getParserByContent(htmlContent)
        assertTrue(htmlParser is HtmlParser, "Should detect HTML parser")

        // Test 2: Plain Text Content
        val textContent =
                """
            ACCESSION NUMBER: 0001234567-24-000001
            SECURITIES AND EXCHANGE COMMISSION
             Washington, D.C. 20549
            
            FINANCIAL REPORT
            ================
            Revenue: $1,000,000
            Net Income: $500,000
            
            This is a sample text report that acts as a valid SEC submission stub.
        """.trimIndent()

        val textParser = ParserFactory.getParserByContent(textContent)
        // Check simpleClassName because we might not have reference to TextParser class easily if
        // it's internal
        assertTrue(
                textParser::class.simpleName?.contains("Text", ignoreCase = true) == true ||
                        textParser::class.simpleName?.contains("Txt", ignoreCase = true) == true,
                "Should detect Text parser, got ${textParser::class.simpleName}"
        )
    }

    @Test
    fun testErrorHandling() {
        println("📊 Test Case 6: Error Handling")
        val htmlParser = HtmlParser()

        // Test 1: Empty content
        val resultEmpty = htmlParser.parse("", "empty.html")
        assertTrue(resultEmpty.metrics.isEmpty())
        assertTrue(resultEmpty.cleanedContent.isEmpty())

        // Test 2: Malformed HTML
        val malformedHtml = "<html><body><table><tr><td>Incomplete"
        val resultMalformed = htmlParser.parse(malformedHtml, "malformed.html")
        assertNotNull(resultMalformed)
    }
}
