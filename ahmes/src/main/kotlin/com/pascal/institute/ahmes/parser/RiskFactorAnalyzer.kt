package com.pascal.institute.ahmes.parser

import com.pascal.institute.ahmes.model.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Risk Factor Analyzer
 *
 * Responsible for parsing and analyzing risk factors from SEC documents. Extracted from
 * EnhancedFinancialParser (AGENTS.md Principle #12: Seek the Essence)
 */
object RiskFactorAnalyzer {

    /** Parse risk factors from SEC document */
    fun parseRiskFactors(content: String): List<RiskFactor> {
        val risks = mutableListOf<RiskFactor>()

        val cleanText = ParsingHelpers.cleanHtml(content)

        // Try to extract "Risk Factors" section
        var riskSection =
                ParsingHelpers.extractSection(
                        cleanText,
                        listOf("Item 1A. Risk Factors", "Item 1A", "Risk Factors")
                )

        // Fallback: If section extraction fails or returns too short content, try manual extraction
        // logic
        if (riskSection == null || riskSection.length < 1000) {
            // Look for "Risk Factors" header manually and take a chunk
            val match = Regex("(?i)(?:Item 1A|Risk Factors)").find(cleanText)
            if (match != null) {
                // Take up to 50,000 chars as a best guess for the section
                riskSection =
                        cleanText.substring(
                                match.range.first,
                                minOf(cleanText.length, match.range.first + 50000)
                        )
            }
        }

        if (riskSection == null) {
            logger.warn { "Could not extract Risk Factors section" }
            return emptyList()
        }

        // Strategy 1: Regex Pattern Matching (High Precision)
        val riskPatterns =
                listOf(
                        Regex(
                                """(?i)(?:^|\n)\s*([A-Z][^.\n]{10,100})\s*[-–—.]\s*([^\n]{50,500})"""
                        ), // Title - Description style
                        Regex("""(?i)(?:^|\n)\s*•\s*([^\n]{20,200})"""), // Bullet points
                        Regex("""(?i)(?:^|\n)\s*\d+\.\s*([^\n]{50,500})""") // Numbered items
                )

        for (pattern in riskPatterns) {
            val matches = pattern.findAll(riskSection)
            for (match in matches.take(20)) {
                val title = match.groupValues.getOrElse(1) { match.value }.trim()
                val summary = match.groupValues.getOrElse(2) { "" }.trim()

                if (title.length < 5) continue

                val category = categorizeRisk(title + " " + summary)
                val severity = assessRiskSeverity(title + " " + summary)

                risks.add(
                        RiskFactor(
                                title = title.take(100),
                                summary = summary.take(500),
                                category = category,
                                severity = severity
                        )
                )
            }
        }

        // Strategy 2: Paragraph Splitting (Fallback)
        if (risks.size < 3) {
            val paragraphs =
                    riskSection.split("\n\n").filter { it.length > 50 }.filter {
                        !it.uppercase().contains("TABLE OF CONTENTS")
                    }

            for (paragraph in paragraphs.take(30)) {
                val riskText = paragraph.trim()

                // Skip if already covered (naive check)
                if (risks.any { it.summary.contains(riskText.take(20)) }) continue

                val category = categorizeRisk(riskText)
                val severity = assessRiskSeverity(riskText)

                // Only add if it seems like a substantial risk
                if (category != RiskCategory.OTHER || severity != RiskSeverity.LOW) {
                    val title =
                            riskText.split(".").firstOrNull()?.trim()?.take(100) ?: "Risk Factor"

                    risks.add(
                            RiskFactor(
                                    title = title,
                                    summary = riskText.take(500),
                                    category = category,
                                    severity = severity
                            )
                    )
                }
            }
        }

        // Strategy 3: Global Regex Search (Last Resort)
        // If we still don't have enough risks, search the entire document
        if (risks.size < 3) {
            logger.warn {
                "Risk extraction from section failed or insufficient (found ${risks.size}). Attempting global search."
            }

            for (pattern in riskPatterns) {
                val matches = pattern.findAll(cleanText) // Search entire clean text
                for (match in matches.take(50)) { // Check more matches since global search is noisy
                    val title = match.groupValues.getOrElse(1) { match.value }.trim()
                    val summary = match.groupValues.getOrElse(2) { "" }.trim()

                    if (title.length < 5 || title.uppercase().contains("TABLE OF CONTENTS"))
                            continue

                    // Check if we already have this risk
                    if (risks.any { it.title == title.take(100) }) continue

                    val category = categorizeRisk(title + " " + summary)
                    val severity = assessRiskSeverity(title + " " + summary)

                    risks.add(
                            RiskFactor(
                                    title = title.take(100),
                                    summary = summary.take(500),
                                    category = category,
                                    severity = severity
                            )
                    )

                    if (risks.size >= 20) break
                }
                if (risks.size >= 20) break
            }
        }

        // Deduplicate
        return risks.distinctBy { it.title }.take(20)
    }

    internal fun categorizeRisk(text: String): RiskCategory {
        val lowerText = text.lowercase()
        return when {
            lowerText.contains("market") || lowerText.contains("economic") -> RiskCategory.MARKET
            lowerText.contains("operation") || lowerText.contains("supply chain") ->
                    RiskCategory.OPERATIONAL
            lowerText.contains("debt") ||
                    lowerText.contains("credit") ||
                    lowerText.contains("financial") -> RiskCategory.FINANCIAL
            lowerText.contains("regulat") || lowerText.contains("compliance") ->
                    RiskCategory.REGULATORY
            lowerText.contains("competi") -> RiskCategory.COMPETITIVE
            lowerText.contains("technolog") || lowerText.contains("cyber") ->
                    RiskCategory.TECHNOLOGY
            lowerText.contains("legal") || lowerText.contains("litigation") -> RiskCategory.LEGAL
            lowerText.contains("environment") || lowerText.contains("climate") ->
                    RiskCategory.ENVIRONMENTAL
            lowerText.contains("geopolit") || lowerText.contains("international") ->
                    RiskCategory.GEOPOLITICAL
            else -> RiskCategory.OTHER
        }
    }

    internal fun assessRiskSeverity(text: String): RiskSeverity {
        val lowerText = text.lowercase()
        return when {
            lowerText.contains("material adverse") || lowerText.contains("significant risk") ->
                    RiskSeverity.HIGH
            lowerText.contains("may adversely") || lowerText.contains("could harm") ->
                    RiskSeverity.MEDIUM
            lowerText.contains("minor") || lowerText.contains("limited impact") -> RiskSeverity.LOW
            else -> RiskSeverity.MEDIUM
        }
    }
}
