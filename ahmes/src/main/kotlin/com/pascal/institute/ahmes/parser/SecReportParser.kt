package com.pascal.institute.ahmes.parser

import com.pascal.institute.ahmes.model.*

/** SEC report parser interface */
interface SecReportParser<T : SecReportParseResult> {
    val reportType: SecReportType

    fun parseHtml(htmlContent: String, metadata: SecReportMetadata): T

    fun parseText(textContent: String, metadata: SecReportMetadata): T

    fun extractSections(content: String): Map<String, String>

    fun extractFinancialStatements(content: String): StructuredFinancialData?

    fun extractRiskFactors(content: String): List<RiskFactor>
}

abstract class BaseSecReportParser<T : SecReportParseResult>(
    override val reportType: SecReportType
) : SecReportParser<T> {

    protected fun cleanHtml(html: String): String {
        return SecTextNormalization.cleanHtmlToText(html)
    }

    /**
     * Extract sections by detecting a single header match per logical section name.
     * This is useful for forms like S-1 / DEF 14A / 20-F where sections are not strictly item-numbered.
     */
    protected fun extractNamedSections(
        content: String,
        sectionPatterns: List<Pair<String, Regex>>
    ): Map<String, String> {
        val sections = mutableMapOf<String, String>()

        val headerMatches = sectionPatterns
            .mapNotNull { (sectionName, pattern) ->
                pattern.find(content)?.let { match ->
                    Triple(match.range.first, sectionName, match.value)
                }
            }
            .sortedBy { it.first }

        for (i in headerMatches.indices) {
            val (startIndex, sectionName, _) = headerMatches[i]
            val endIndex = if (i < headerMatches.size - 1) {
                headerMatches[i + 1].first
            } else {
                null
            }

            val sectionContent = extractSection(content, startIndex, endIndex)
            sections[sectionName] = sectionContent
        }

        return sections
    }

    protected fun findSectionHeader(
        content: String,
        patterns: List<Regex>
    ): List<Pair<Int, String>> {
        val matches = mutableListOf<Pair<Int, String>>()

        for (pattern in patterns) {
            pattern.findAll(content).forEach { match ->
                matches.add(match.range.first to match.value)
            }
        }

        return matches.sortedBy { it.first }
    }

    protected fun extractSection(content: String, startIndex: Int, endIndex: Int?): String {
        val end = endIndex ?: content.length
        return content.substring(startIndex, end.coerceAtMost(content.length)).trim()
    }

    /**
     * Build a section map from header match positions.
     *
     * Keeps behavior consistent across parsers: each header spans until the next header.
     */
    protected fun extractSectionsFromHeaderMatches(
        content: String,
        headerMatches: List<Pair<Int, String>>,
        keyForHeader: (index: Int, headerText: String) -> String?
    ): Map<String, String> {
        val sections = mutableMapOf<String, String>()

        for (i in headerMatches.indices) {
            val (startIndex, headerText) = headerMatches[i]
            val endIndex = if (i < headerMatches.size - 1) {
                headerMatches[i + 1].first
            } else {
                null
            }

            val key = keyForHeader(i, headerText) ?: continue
            sections[key] = extractSection(content, startIndex, endIndex)
        }

        return sections
    }

    /** Default risk factor extraction */
    override fun extractRiskFactors(content: String): List<RiskFactor> {
        val riskFactors = mutableListOf<RiskFactor>()

        // Find "Risk Factor" section
        val riskSectionPattern = Regex("(?i)(item\\s+1a\\.|risk\\s+factors)", RegexOption.IGNORE_CASE)
        val match = riskSectionPattern.find(content) ?: return emptyList()

        // Extract until next section
        val nextSectionPattern = Regex("(?i)(item\\s+\\d+[a-z]?\\.|part\\s+[ivi]+)", RegexOption.IGNORE_CASE)
        val nextMatch = nextSectionPattern.find(content, match.range.last + 1)

        val riskSection = extractSection(content, match.range.first, nextMatch?.range?.first)

        // Simple risk factor parsing (paragraph-based)
        val paragraphs = riskSection.split(Regex("\n\n+"))

        paragraphs.forEach { paragraph ->
            if (paragraph.length > 100) {
                riskFactors.add(
                    RiskFactor(
                        title = "Risk Factor",
                        summary = paragraph.take(500),
                        category = RiskCategory.OTHER,
                        severity = RiskSeverity.MEDIUM
                    )
                )
            }
        }

        return riskFactors
    }

    /** Default financial statement extraction - override in subclasses */
    override fun extractFinancialStatements(content: String): StructuredFinancialData? {
        return null
    }
}
