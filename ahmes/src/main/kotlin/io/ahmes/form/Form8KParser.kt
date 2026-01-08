package io.ahmes.form

import io.ahmes.model.*
import io.ahmes.parser.*

/**
 * 8-K Current Report Parser
 *
 * 8-K is filed for material events including:
 * - Financial results announcements
 * - Mergers and acquisitions
 * - Executive changes
 * - Bankruptcy and default
 * - Other material events
 */
class Form8KParser : BaseSecReportParser<Form8KParseResult>(SecReportType.FORM_8K) {

    override fun parseHtml(htmlContent: String, metadata: SecReportMetadata): Form8KParseResult {
        val cleanedContent = cleanHtml(htmlContent)
        return parseContent(cleanedContent, htmlContent, metadata)
    }

    override fun parseText(textContent: String, metadata: SecReportMetadata): Form8KParseResult {
        return parseContent(textContent, textContent, metadata)
    }

    private fun parseContent(
        cleanedContent: String,
        rawContent: String,
        metadata: SecReportMetadata
    ): Form8KParseResult {
        val sections = extractSections(cleanedContent)
        val eventItems = extractEventItems(cleanedContent)

        return Form8KParseResult(
            metadata = metadata,
            rawContent = rawContent,
            sections = sections,
            eventDate = extractEventDate(cleanedContent),
            eventItems = eventItems.keys.toList(),
            eventDescriptions = eventItems,
            financialResults = sections["Item 2.02"],
            acquisitions = sections["Item 2.01"],
            dispositions = sections["Item 2.01"],
            executiveChanges = sections["Item 5.02"],
            bankruptcy = sections["Item 1.03"],
            exhibits = extractExhibits(cleanedContent)
        )
    }

    override fun extractSections(content: String): Map<String, String> {
        val itemPatterns = Form8KItem.values().map { item ->
            Regex("(?i)item\\s+${Regex.escape(item.itemNumber)}[.:\\-\\s]+", RegexOption.IGNORE_CASE)
        }

        val headerMatches = findSectionHeader(content, itemPatterns)

        return extractSectionsFromHeaderMatches(content, headerMatches) { _, headerText ->
            val itemMatch = Regex("(?i)item\\s+(\\d+\\.\\d+)").find(headerText)
            val itemNumber = itemMatch?.groupValues?.get(1) ?: return@extractSectionsFromHeaderMatches null
            "Item $itemNumber"
        }
    }

    private fun extractEventItems(content: String): Map<String, String> {
        val eventItems = mutableMapOf<String, String>()

        Form8KItem.values().forEach { item ->
            val itemPattern = Regex(
                "(?i)item\\s+${Regex.escape(item.itemNumber)}[.:\\-\\s]+([^\n]*)",
                RegexOption.IGNORE_CASE
            )

            val match = itemPattern.find(content)
            if (match != null) {
                val itemNumber = "Item ${item.itemNumber}"
                val description = "${item.title} (${item.category})"
                eventItems[itemNumber] = description
            }
        }

        return eventItems
    }

    private fun extractEventDate(content: String): String? {
        val datePattern = Regex(
            "(?i)date\\s+of\\s+(?:earliest\\s+)?event\\s+(?:reported)?[:\\s]+(\\w+\\s+\\d+,?\\s+\\d{4})",
            RegexOption.IGNORE_CASE
        )

        val match = datePattern.find(content)
        return match?.groupValues?.get(1)?.trim()
    }

    private fun extractExhibits(content: String): List<String> {
        val exhibits = mutableListOf<String>()

        val item901Section = extractSections(content)["Item 9.01"]

        if (!item901Section.isNullOrBlank()) {
            val exhibitPattern = Regex("(?i)exhibit\\s+(\\d+\\.\\d+)[.:\\-\\s]*([^\n]+)", RegexOption.IGNORE_CASE)

            exhibitPattern.findAll(item901Section).forEach { match ->
                val exhibitNumber = match.groupValues[1]
                val exhibitDescription = match.groupValues[2].trim().take(150)
                exhibits.add("Exhibit $exhibitNumber: $exhibitDescription")
            }
        }

        return exhibits
    }

    override fun extractFinancialStatements(content: String): StructuredFinancialData? {
        val item202Section = extractSections(content)["Item 2.02"]

        if (item202Section.isNullOrBlank()) {
            return null
        }

        return StructuredFinancialData(
            companyName = null,
            reportType = "8-K",
            fiscalYear = null,
            fiscalPeriod = "Event",
            incomeStatement = null,
            balanceSheet = null,
            cashFlowStatement = null,
            dataQuality = DataQuality.LOW
        )
    }

    override fun extractRiskFactors(content: String): List<RiskFactor> {
        val riskFactors = mutableListOf<RiskFactor>()

        val bankruptcySection = extractSections(content)["Item 1.03"]
        if (!bankruptcySection.isNullOrBlank()) {
            riskFactors.add(
                RiskFactor(
                    title = "Bankruptcy Event",
                    summary = "Bankruptcy or receivership proceedings disclosed",
                    category = RiskCategory.FINANCIAL,
                    severity = RiskSeverity.CRITICAL
                )
            )
        }

        val defaultSection = extractSections(content)["Item 2.04"]
        if (!defaultSection.isNullOrBlank()) {
            riskFactors.add(
                RiskFactor(
                    title = "Debt Acceleration",
                    summary = "Triggering events that accelerate or increase obligations",
                    category = RiskCategory.FINANCIAL,
                    severity = RiskSeverity.HIGH
                )
            )
        }

        val impairmentSection = extractSections(content)["Item 2.06"]
        if (!impairmentSection.isNullOrBlank()) {
            riskFactors.add(
                RiskFactor(
                    title = "Material Impairment",
                    summary = "Material impairments disclosed",
                    category = RiskCategory.FINANCIAL,
                    severity = RiskSeverity.HIGH
                )
            )
        }

        return riskFactors
    }
}
