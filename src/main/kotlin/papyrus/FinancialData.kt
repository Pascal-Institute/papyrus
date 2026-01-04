package papyrus

import kotlinx.serialization.Serializable

@Serializable
data class FinancialMetric(
        val name: String,
        val value: String,
        val rawValue: Double? = null,
        val context: String = ""
)

data class FinancialAnalysis(
        val fileName: String,
        val companyName: String?,
        val reportType: String?,
        val periodEnding: String?,
        val metrics: List<FinancialMetric>,
        val rawContent: String,
        val summary: String
)

object FinancialAnalyzer {
    // Key financial terms to search for
    private val revenueTerms =
            listOf(
                    "Total Revenue",
                    "Total Revenues",
                    "Net Revenue",
                    "Net Revenues",
                    "Revenue",
                    "Revenues",
                    "Sales",
                    "Net Sales"
            )

    private val incomeTerms =
            listOf(
                    "Net Income",
                    "Net Earnings",
                    "Net Loss",
                    "Net Income (Loss)",
                    "Profit",
                    "Net Profit"
            )

    private val assetsTerms = listOf("Total Assets", "Total Current Assets")

    private val liabilitiesTerms = listOf("Total Liabilities", "Total Current Liabilities")

    private val equityTerms =
            listOf(
                    "Total Equity",
                    "Stockholders' Equity",
                    "Shareholders' Equity",
                    "Total Stockholders' Equity"
            )

    private val epsTerms = listOf("Earnings Per Share", "EPS", "Basic EPS", "Diluted EPS")

    fun analyzeDocument(fileName: String, content: String): FinancialAnalysis {
        // Remove HTML tags and normalize whitespace
        val cleanText =
                content.replace(Regex("<[^>]*>"), " ")
                        .replace(Regex("\\s+"), " ")
                        .replace("&nbsp;", " ")
                        .trim()

        // Extract company name (usually in first few lines)
        val companyName = extractCompanyName(cleanText)

        // Detect report type (10-K, 10-Q, etc.)
        val reportType = extractReportType(cleanText)

        // Extract period
        val period = extractPeriod(cleanText)

        // Extract financial metrics
        val metrics = mutableListOf<FinancialMetric>()

        // Search for each category
        metrics.addAll(searchMetrics(cleanText, revenueTerms, "Revenue"))
        metrics.addAll(searchMetrics(cleanText, incomeTerms, "Net Income"))
        metrics.addAll(searchMetrics(cleanText, assetsTerms, "Assets"))
        metrics.addAll(searchMetrics(cleanText, liabilitiesTerms, "Liabilities"))
        metrics.addAll(searchMetrics(cleanText, equityTerms, "Equity"))
        metrics.addAll(searchMetrics(cleanText, epsTerms, "EPS"))

        // Generate summary
        val summary = generateSummary(companyName, reportType, period, metrics)

        return FinancialAnalysis(
                fileName = fileName,
                companyName = companyName,
                reportType = reportType,
                periodEnding = period,
                metrics = metrics,
                rawContent = cleanText.take(50000), // Limit size
                summary = summary
        )
    }

    private fun extractCompanyName(text: String): String? {
        // Look for common patterns
        val patterns =
                listOf(
                        Regex(
                                "(?i)(?:UNITED STATES\\s+SECURITIES AND EXCHANGE COMMISSION.*?)(\\b[A-Z][A-Za-z\\s&,.-]+(?:Inc\\.?|Corp\\.?|Corporation|Company|LLC|Ltd\\.?))"
                        ),
                        Regex(
                                "(?i)(\\b[A-Z][A-Za-z\\s&,.-]+(?:Inc\\.?|Corp\\.?|Corporation|Company))(?=\\s+Form)"
                        )
                )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1].trim()
            }
        }

        return null
    }

    private fun extractReportType(text: String): String? {
        val match = Regex("(?i)Form\\s+(10-[KQ]|8-K|20-F)").find(text)
        return match?.groupValues?.get(1)?.uppercase()
    }

    private fun extractPeriod(text: String): String? {
        // Look for period ending dates
        val patterns =
                listOf(
                        Regex(
                                "(?i)(?:For the|Period Ending|Quarter Ended|Year Ended)\\s+([A-Za-z]+\\s+\\d{1,2},?\\s+\\d{4})"
                        ),
                        Regex("(?i)Three Months Ended\\s+([A-Za-z]+\\s+\\d{1,2},?\\s+\\d{4})"),
                        Regex("(?i)Twelve Months Ended\\s+([A-Za-z]+\\s+\\d{1,2},?\\s+\\d{4})")
                )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1].trim()
            }
        }

        return null
    }

    private fun searchMetrics(
            text: String,
            terms: List<String>,
            category: String
    ): List<FinancialMetric> {
        val results = mutableListOf<FinancialMetric>()

        for (term in terms) {
            // Look for pattern: Term + amount
            // Handles formats like: "Total Revenue $123,456", "Revenue: 123456", etc.
            val pattern = Regex("(?i)${Regex.escape(term)}[:\\s]*(\\$?\\s*[\\d,]+(?:\\.\\d+)?)")
            val matches = pattern.findAll(text)

            for (match in matches.take(3)) { // Take first 3 matches
                if (match.groupValues.size > 1) {
                    val valueStr = match.groupValues[1].trim()
                    val context =
                            text.substring(
                                    maxOf(0, match.range.first - 100),
                                    minOf(text.length, match.range.last + 100)
                            )

                    // Parse numeric value
                    val rawValue = parseAmount(valueStr)

                    results.add(
                            FinancialMetric(
                                    name = term,
                                    value = valueStr,
                                    rawValue = rawValue,
                                    context = context
                            )
                    )
                }
            }
        }

        return results
    }

    private fun parseAmount(amountStr: String): Double? {
        return try {
            val cleaned = amountStr.replace("$", "").replace(",", "").replace(" ", "").trim()
            cleaned.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    private fun generateSummary(
            companyName: String?,
            reportType: String?,
            period: String?,
            metrics: List<FinancialMetric>
    ): String {
        val sb = StringBuilder()

        // Header
        sb.appendLine("📊 Financial Analysis Summary")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine()

        if (companyName != null) {
            sb.appendLine("🏢 Company: $companyName")
        }
        if (reportType != null) {
            sb.appendLine("📋 Report Type: $reportType")
        }
        if (period != null) {
            sb.appendLine("📅 Period: $period")
        }
        sb.appendLine()

        // Group metrics by category
        val grouped =
                metrics.groupBy { metric ->
                    when {
                        metric.name.contains("Revenue", ignoreCase = true) ||
                                metric.name.contains("Sales", ignoreCase = true) -> "💰 Revenue"
                        metric.name.contains("Income", ignoreCase = true) ||
                                metric.name.contains("Profit", ignoreCase = true) ||
                                metric.name.contains("Earnings", ignoreCase = true) ->
                                "💵 Income/Earnings"
                        metric.name.contains("Assets", ignoreCase = true) -> "🏦 Assets"
                        metric.name.contains("Liabilities", ignoreCase = true) -> "📊 Liabilities"
                        metric.name.contains("Equity", ignoreCase = true) -> "💎 Equity"
                        metric.name.contains("EPS", ignoreCase = true) -> "📈 Per Share Metrics"
                        else -> "📌 Other Metrics"
                    }
                }

        for ((category, metricsList) in grouped) {
            sb.appendLine(category)
            for (metric in metricsList.take(5)) { // Limit to 5 per category
                val formattedValue =
                        if (metric.rawValue != null) {
                            formatNumber(metric.rawValue)
                        } else {
                            metric.value
                        }
                sb.appendLine("  • ${metric.name}: $formattedValue")
            }
            sb.appendLine()
        }

        if (metrics.isEmpty()) {
            sb.appendLine("⚠️  No financial metrics were automatically detected.")
            sb.appendLine("   The document may be in an unsupported format or")
            sb.appendLine("   may not contain standard financial statements.")
        }

        return sb.toString()
    }

    private fun formatNumber(value: Double): String {
        return when {
            value >= 1_000_000_000 -> String.format("$%.2fB", value / 1_000_000_000)
            value >= 1_000_000 -> String.format("$%.2fM", value / 1_000_000)
            value >= 1_000 -> String.format("$%.2fK", value / 1_000)
            else -> String.format("$%.2f", value)
        }
    }
}
