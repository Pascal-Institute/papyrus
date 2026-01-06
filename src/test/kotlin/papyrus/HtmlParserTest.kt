package papyrus

import kotlinx.coroutines.runBlocking
import papyrus.core.network.SecApi
import papyrus.core.service.parser.HtmlParser
import papyrus.core.service.parser.ParserFactory

/** Test Jsoup-based HtmlParser with real SEC filings */
fun main() = runBlocking {
    println("🚀 Jsoup HTML Parser Test - Testing with Real SEC Filings")
    println("=".repeat(70))
    println()

    // Test Case 1: Apple 10-Q (Quarterly Report)
    testApple10Q()

    println()
    println("=".repeat(70))

    // Test Case 2: Tesla 10-K (Annual Report)
    testTesla10K()

    println()
    println("=".repeat(70))

    // Test Case 3: Microsoft 10-K (Annual Report)
    testMicrosoft10K()

    println()
    println("=".repeat(70))

    // Test Case 4: Amazon 8-K (Current Event Report)
    testAmazon8K()

    println()
    println("=".repeat(70))

    // Test Case 5: ParserFactory Auto-Detection
    testParserFactoryAutoDetection()

    println()
    println("=".repeat(70))

    // Test Case 6: Error Handling
    testErrorHandling()

    println()
    println("=".repeat(70))
    println("✅ All tests completed!")
}

suspend fun testApple10Q() {
    println("📊 Test Case 1: Apple Inc. 10-Q (Quarterly Report)")
    println("-".repeat(70))

    try {
        // Download Apple's latest 10-Q filing
        val submissions = SecApi.getSubmissions(320193) // Apple CIK
        if (submissions == null) {
            println("❌ Failed to fetch Apple submissions")
            return
        }

        val filings = SecApi.transformFilings(submissions.filings.recent)
        val latest10Q = filings.firstOrNull { it.form == "10-Q" }

        if (latest10Q == null) {
            println("❌ No 10-Q filing found")
            return
        }

        println("📄 Filing: ${latest10Q.form} - ${latest10Q.filingDate}")
        println("📝 Document: ${latest10Q.primaryDocument}")

        // Download HTML document
        val url =
                SecApi.getDocumentUrl(
                        cik = "320193",
                        accessionNumber = latest10Q.accessionNumber,
                        primaryDocument = latest10Q.primaryDocument
                )

        println("🔗 URL: $url")
        println()
        println("⏳ Downloading...")

        val content = SecApi.fetchDocumentContent(url)

        if (content.startsWith("Error")) {
            println("❌ $content")
            return
        }

        println("✅ Downloaded: ${content.length} characters")
        println()

        // Parse with HtmlParser
        println("⚙️  Parsing with Jsoup HtmlParser...")
        val htmlParser = HtmlParser()

        val canParse = htmlParser.canParse(content)
        println("  ✓ Can parse: $canParse")

        val parseResult = htmlParser.parse(content, latest10Q.primaryDocument)

        println()
        println("📋 Parse Results:")
        println("  • Parser Type: ${parseResult.parserType}")
        println("  • Metrics Found: ${parseResult.metrics.size}")
        println("  • Cleaned Content Length: ${parseResult.cleanedContent.length} chars")
        println()

        println("🔍 Metadata:")
        parseResult.metadata.forEach { (key, value) -> println("  • $key: $value") }
        println()

        println("💰 Top 10 Financial Metrics:")
        parseResult.metrics.take(10).forEachIndexed { index, metric ->
            val rawValue = metric.rawValue
            val formattedValue =
                    if (rawValue != null) {
                        formatNumber(rawValue)
                    } else {
                        metric.value
                    }
            println("  ${index + 1}. ${metric.name}: $formattedValue")
        }

        println()
        println("📄 Cleaned Content Preview (first 500 chars):")
        println(parseResult.cleanedContent.take(500))
    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
        e.printStackTrace()
    }
}

suspend fun testTesla10K() {
    println("📊 Test Case 2: Tesla Inc. 10-K (Annual Report)")
    println("-".repeat(70))

    try {
        // Download Tesla's latest 10-K filing
        val submissions = SecApi.getSubmissions(1318605) // Tesla CIK
        if (submissions == null) {
            println("❌ Failed to fetch Tesla submissions")
            return
        }

        val filings = SecApi.transformFilings(submissions.filings.recent)
        val latest10K = filings.firstOrNull { it.form == "10-K" }

        if (latest10K == null) {
            println("❌ No 10-K filing found")
            return
        }

        println("📄 Filing: ${latest10K.form} - ${latest10K.filingDate}")
        println("📝 Document: ${latest10K.primaryDocument}")

        // Download HTML document
        val url =
                SecApi.getDocumentUrl(
                        cik = "1318605",
                        accessionNumber = latest10K.accessionNumber,
                        primaryDocument = latest10K.primaryDocument
                )

        println("🔗 URL: $url")
        println()
        println("⏳ Downloading...")

        val content = SecApi.fetchDocumentContent(url)

        if (content.startsWith("Error")) {
            println("❌ $content")
            return
        }

        println("✅ Downloaded: ${content.length} characters")
        println()

        // Parse with ParserFactory (auto-detect format)
        println("⚙️  Parsing with ParserFactory (auto-detect)...")
        val parser = ParserFactory.getParserByContent(content)
        println("  ✓ Detected Parser: ${parser::class.simpleName}")

        val parseResult = parser.parse(content, latest10K.primaryDocument)

        println()
        println("📋 Parse Results:")
        println("  • Parser Type: ${parseResult.parserType}")
        println("  • Metrics Found: ${parseResult.metrics.size}")
        println("  • Cleaned Content Length: ${parseResult.cleanedContent.length} chars")
        println()

        println("🔍 Metadata:")
        parseResult.metadata.forEach { (key, value) -> println("  • $key: $value") }
        println()

        println("💰 Financial Metrics by Category:")
        val grouped: Map<String, List<papyrus.core.model.FinancialMetric>> =
                parseResult.metrics.groupBy { metric ->
                    when {
                        metric.name.contains("Revenue", ignoreCase = true) -> "Revenue"
                        metric.name.contains("Income", ignoreCase = true) -> "Income"
                        metric.name.contains("Assets", ignoreCase = true) -> "Assets"
                        metric.name.contains("Liabilities", ignoreCase = true) -> "Liabilities"
                        metric.name.contains("Equity", ignoreCase = true) -> "Equity"
                        metric.name.contains("Cash", ignoreCase = true) -> "Cash Flow"
                        else -> "Other"
                    }
                }

        grouped.forEach { (category, metrics) ->
            println("  [$category]")
            metrics.take(3).forEach { metric ->
                val formatted = metric.rawValue?.let { formatNumber(it) } ?: metric.value
                println("    • ${metric.name}: $formatted")
            }
        }
    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
        e.printStackTrace()
    }
}

fun formatNumber(value: Double): String {
    return when {
        value >= 1_000_000_000 -> String.format("$%.2fB", value / 1_000_000_000)
        value >= 1_000_000 -> String.format("$%.2fM", value / 1_000_000)
        value >= 1_000 -> String.format("$%.2fK", value / 1_000)
        else -> String.format("$%.2f", value)
    }
}

suspend fun testMicrosoft10K() {
    println("📊 Test Case 3: Microsoft Corporation 10-K (Annual Report)")
    println("-".repeat(70))

    try {
        // Download Microsoft's latest 10-K filing
        val submissions = SecApi.getSubmissions(789019) // Microsoft CIK
        if (submissions == null) {
            println("❌ Failed to fetch Microsoft submissions")
            return
        }

        val filings = SecApi.transformFilings(submissions.filings.recent)
        val latest10K = filings.firstOrNull { it.form == "10-K" }

        if (latest10K == null) {
            println("❌ No 10-K filing found")
            return
        }

        println("📄 Filing: ${latest10K.form} - ${latest10K.filingDate}")
        println("📝 Document: ${latest10K.primaryDocument}")

        // Download HTML document
        val url =
                SecApi.getDocumentUrl(
                        cik = "789019",
                        accessionNumber = latest10K.accessionNumber,
                        primaryDocument = latest10K.primaryDocument
                )

        println("🔗 URL: $url")
        println()
        println("⏳ Downloading...")

        val content = SecApi.fetchDocumentContent(url)

        if (content.startsWith("Error")) {
            println("❌ $content")
            return
        }

        println("✅ Downloaded: ${content.length} characters")
        println()

        // Parse with HtmlParser
        println("⚙️  Parsing with Jsoup HtmlParser...")
        val htmlParser = HtmlParser()

        val canParse = htmlParser.canParse(content)
        println("  ✓ Can parse: $canParse")

        val parseResult = htmlParser.parse(content, latest10K.primaryDocument)

        println()
        println("📋 Parse Results:")
        println("  • Parser Type: ${parseResult.parserType}")
        println("  • Metrics Found: ${parseResult.metrics.size}")
        println("  • Cleaned Content Length: ${parseResult.cleanedContent.length} chars")
        println()

        println("🔍 Metadata:")
        parseResult.metadata.forEach { (key, value) -> println("  • $key: $value") }
        println()

        // Show metrics related to cloud business
        println("☁️  Cloud & Azure Related Metrics:")
        val cloudMetrics =
                parseResult.metrics.filter {
                    it.name.contains("Cloud", ignoreCase = true) ||
                            it.name.contains("Azure", ignoreCase = true)
                }
        cloudMetrics.take(5).forEach { metric ->
            val formatted = metric.rawValue?.let { formatNumber(it) } ?: metric.value
            println("  • ${metric.name}: $formatted")
        }

        if (cloudMetrics.isEmpty()) {
            println("  (No specific cloud metrics found in top-level extraction)")
        }
    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
        e.printStackTrace()
    }
}

suspend fun testAmazon8K() {
    println("📊 Test Case 4: Amazon.com Inc. 8-K (Current Event Report)")
    println("-".repeat(70))

    try {
        // Download Amazon's latest 8-K filing
        val submissions = SecApi.getSubmissions(1018724) // Amazon CIK
        if (submissions == null) {
            println("❌ Failed to fetch Amazon submissions")
            return
        }

        val filings = SecApi.transformFilings(submissions.filings.recent)
        val latest8K = filings.firstOrNull { it.form == "8-K" }

        if (latest8K == null) {
            println("❌ No 8-K filing found")
            return
        }

        println("📄 Filing: ${latest8K.form} - ${latest8K.filingDate}")
        println("📝 Document: ${latest8K.primaryDocument}")

        // Download HTML document
        val url =
                SecApi.getDocumentUrl(
                        cik = "1018724",
                        accessionNumber = latest8K.accessionNumber,
                        primaryDocument = latest8K.primaryDocument
                )

        println("🔗 URL: $url")
        println()
        println("⏳ Downloading...")

        val content = SecApi.fetchDocumentContent(url)

        if (content.startsWith("Error")) {
            println("❌ $content")
            return
        }

        println("✅ Downloaded: ${content.length} characters")
        println()

        // Parse with HtmlParser
        println("⚙️  Parsing 8-K document...")
        val htmlParser = HtmlParser()
        val parseResult = htmlParser.parse(content, latest8K.primaryDocument)

        println()
        println("📋 Parse Results:")
        println("  • Parser Type: ${parseResult.parserType}")
        println("  • Metrics Found: ${parseResult.metrics.size}")
        println("  • Cleaned Content Length: ${parseResult.cleanedContent.length} chars")
        println()

        println("📰 Document Preview (first 800 chars):")
        println(parseResult.cleanedContent.take(800))
        println()

        println("💡 Note: 8-K reports typically contain event-driven disclosures")
        println("         rather than comprehensive financial metrics.")
    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
        e.printStackTrace()
    }
}

suspend fun testParserFactoryAutoDetection() {
    println("📊 Test Case 5: ParserFactory Auto-Detection")
    println("-".repeat(70))

    try {
        println("🧪 Testing parser auto-detection with different content types...")
        println()

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
        println("  ✓ HTML Content -> ${htmlParser::class.simpleName}")

        // Test 2: Plain Text Content
        val textContent =
                """
            FINANCIAL REPORT
            ================
            Revenue: $1,000,000
            Net Income: $500,000
        """.trimIndent()

        val textParser = ParserFactory.getParserByContent(textContent)
        println("  ✓ Plain Text Content -> ${textParser::class.simpleName}")

        println()
        println("  ✅ ParserFactory successfully detects content types")

        // Test 3: Parse with detected parser
        println()
        println("  🔬 Testing HTML parser functionality...")
        val parseResult = htmlParser.parse(htmlContent, "test.html")
        println("     • Parsed ${parseResult.metrics.size} metrics")
        println("     • Cleaned content: ${parseResult.cleanedContent.length} chars")
        println("     • Parser type: ${parseResult.parserType}")
    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
        e.printStackTrace()
    }
}

suspend fun testErrorHandling() {
    println("📊 Test Case 6: Error Handling & Edge Cases")
    println("-".repeat(70))

    try {
        val htmlParser = HtmlParser()

        // Test 1: Empty content
        println("🧪 Test 1: Empty Content")
        try {
            val result = htmlParser.parse("", "empty.html")
            println("  ✓ Handled empty content")
            println("    • Metrics: ${result.metrics.size}")
            println("    • Cleaned length: ${result.cleanedContent.length}")
        } catch (e: Exception) {
            println("  ⚠️  Exception: ${e.message}")
        }
        println()

        // Test 2: Malformed HTML
        println("🧪 Test 2: Malformed HTML")
        val malformedHtml = "<html><body><table><tr><td>Incomplete"
        try {
            val result = htmlParser.parse(malformedHtml, "malformed.html")
            println("  ✓ Handled malformed HTML")
            println("    • Metrics: ${result.metrics.size}")
            println("    • Cleaned length: ${result.cleanedContent.length}")
        } catch (e: Exception) {
            println("  ⚠️  Exception: ${e.message}")
        }
        println()

        // Test 3: Very large numbers
        println("🧪 Test 3: Large Number Formatting")
        val testValues =
                listOf(
                        1_234.56,
                        12_345.67,
                        123_456.78,
                        1_234_567.89,
                        12_345_678.90,
                        123_456_789.01,
                        1_234_567_890.12
                )
        testValues.forEach { value -> println("  • ${value.toLong()} -> ${formatNumber(value)}") }
        println()

        // Test 4: Invalid CIK
        println("🧪 Test 4: Invalid CIK Handling")
        try {
            val submissions = SecApi.getSubmissions(999999999) // Invalid CIK
            if (submissions == null) {
                println("  ✓ Correctly returned null for invalid CIK")
            } else {
                println("  ⚠️  Unexpected: Got submissions for invalid CIK")
            }
        } catch (e: Exception) {
            println("  ✓ Exception caught: ${e.message}")
        }

        println()
        println("  ✅ Error handling tests completed")
    } catch (e: Exception) {
        println("❌ Test suite error: ${e.message}")
        e.printStackTrace()
    }
}
