package papyrus.core.service.parser

import papyrus.core.model.ExtendedFinancialMetric
import papyrus.core.model.FinancialMetric

/**
 * Text Document Parser
 *
 * Specialized parser for plain text (.txt) SEC filing submissions. SEC's complete submission text
 * files have a specific format with headers, sections, and embedded documents.
 */
class TxtParser : DocumentParser {

    override fun parse(content: String, documentName: String): ParseResult {
        // Clean and normalize text content
        val cleanedContent = cleanTextContent(content)

        // Extract sections from SEC text format
        val sections = extractSecSections(cleanedContent)

        // Extract financial metrics using existing parser
        val extendedMetrics = EnhancedFinancialParser.parsePdfTextTable(cleanedContent)
        val metrics = extendedMetrics.map { it.toFinancialMetric() }

        return ParseResult(
                metrics = metrics,
                documentName = documentName,
                parserType = "TXT",
                rawContent = content,
                cleanedContent = cleanedContent,
                metadata =
                        mapOf(
                                "sections" to sections.keys.joinToString(", "),
                                "sectionCount" to sections.size.toString(),
                                "hasSecHeader" to detectSecHeader(content).toString()
                        )
        )
    }

    override fun canParse(content: String): Boolean {
        // Plain text files don't have special markers
        // Check for SEC-specific text markers
        return !content.trim().startsWith("<") &&
                (content.contains("SECURITIES AND EXCHANGE COMMISSION", ignoreCase = true) ||
                        content.contains("ACCESSION NUMBER:", ignoreCase = true) ||
                        content.length > 100) // Basic validation
    }

    override fun getSupportedExtension(): String = "txt"

    /** Clean text content by normalizing whitespace and removing artifacts */
    private fun cleanTextContent(text: String): String {
        var cleaned = text

        // Remove page breaks and form feeds
        cleaned = cleaned.replace("\u000C", "\n\n") // Form feed
        cleaned = cleaned.replace(Regex("\\n-{3,}\\n"), "\n\n") // Separator lines

        // Normalize line endings
        cleaned = cleaned.replace("\r\n", "\n")
        cleaned = cleaned.replace("\r", "\n")

        // Remove excessive blank lines (keep max 2)
        cleaned = cleaned.replace(Regex("\\n{3,}"), "\n\n")

        // Normalize spacing
        cleaned = cleaned.replace(Regex(" {2,}"), "  ") // Keep some spacing for alignment

        return cleaned.trim()
    }

    /** Detect if document has SEC submission header */
    private fun detectSecHeader(content: String): Boolean {
        val headerMarkers =
                listOf(
                        "SECURITIES AND EXCHANGE COMMISSION",
                        "ACCESSION NUMBER:",
                        "CONFORMED SUBMISSION TYPE:",
                        "PUBLIC DOCUMENT COUNT:"
                )
        return headerMarkers.any { marker -> content.contains(marker, ignoreCase = true) }
    }

    /**
     * Extract sections from SEC text submission format SEC text submissions have clear section
     * delimiters
     */
    private fun extractSecSections(content: String): Map<String, String> {
        val sections = mutableMapOf<String, String>()

        // SEC text files use <DOCUMENT> and </DOCUMENT> tags
        val documentPattern =
                Regex(
                        "<DOCUMENT>\\s*(?:<TYPE>([^\\n]+))?\\s*(?:<SEQUENCE>([^\\n]+))?\\s*(?:<FILENAME>([^\\n]+))?[^<]*<TEXT>\\s*(.*?)\\s*</TEXT>\\s*</DOCUMENT>",
                        RegexOption.DOT_MATCHES_ALL
                )

        documentPattern.findAll(content).forEach { match ->
            val type = match.groupValues.getOrNull(1)?.trim() ?: "UNKNOWN"
            val text = match.groupValues.getOrNull(4)?.trim() ?: ""

            if (text.isNotEmpty()) {
                sections[type] = text
            }
        }

        // If no explicit sections found, treat entire content as one section
        if (sections.isEmpty()) {
            sections["MAIN"] = content
        }

        return sections
    }
}

/** Extension function to convert ExtendedFinancialMetric to FinancialMetric */
private fun ExtendedFinancialMetric.toFinancialMetric(): FinancialMetric {
    return FinancialMetric(
            name = this.name,
            value = this.value,
            rawValue = this.rawValue,
            context = this.context
    )
}
