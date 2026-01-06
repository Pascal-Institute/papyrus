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
            val formattedValue =
                    if (metric.rawValue != null) {
                        formatNumber(metric.rawValue)
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
                val formatted =
                        if (metric.rawValue != null) {
                            formatNumber(metric.rawValue)
                        } else {
                            metric.value
                        }
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
