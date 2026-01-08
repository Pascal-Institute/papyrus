package com.pascal.institute.ahmes.format

import com.pascal.institute.ahmes.parser.*
import com.pascal.institute.ahmes.model.FinancialMetric

/**
 * Text Document Parser
 *
 * Specialized parser for plain text (.txt) SEC filing submissions.
 * SEC's complete submission text files have a specific format with headers,
 * sections, and embedded documents.
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
            metadata = mapOf(
                "sections" to sections.keys.joinToString(", "),
                "sectionCount" to sections.size.toString(),
                "hasSecHeader" to detectSecHeader(content).toString()
            )
        )
    }

    override fun canParse(content: String): Boolean {
        return !content.trim().startsWith("<") &&
            (content.contains("SECURITIES AND EXCHANGE COMMISSION", ignoreCase = true) ||
                content.contains("ACCESSION NUMBER:", ignoreCase = true) ||
                content.length > 100)
    }

    override fun getSupportedExtension(): String = "txt"

    private fun cleanTextContent(text: String): String {
        var cleaned = text

        cleaned = cleaned.replace("\u000C", "\n\n")
        cleaned = cleaned.replace(Regex("\\n-{3,}\\n"), "\n\n")
        cleaned = cleaned.replace("\r\n", "\n")
        cleaned = cleaned.replace("\r", "\n")
        cleaned = cleaned.replace(Regex("\\n{3,}"), "\n\n")
        cleaned = cleaned.replace(Regex(" {2,}"), "  ")

        return cleaned.trim()
    }

    private fun detectSecHeader(content: String): Boolean {
        val headerMarkers = listOf(
            "SECURITIES AND EXCHANGE COMMISSION",
            "ACCESSION NUMBER:",
            "CONFORMED SUBMISSION TYPE:",
            "PUBLIC DOCUMENT COUNT:"
        )
        return headerMarkers.any { marker -> content.contains(marker, ignoreCase = true) }
    }

    private fun extractSecSections(content: String): Map<String, String> {
        val sections = mutableMapOf<String, String>()

        val documentPattern = Regex(
            "<DOCUMENT>\\s*(?:<TYPE>([^\\n]+))?\\s*(?:<SEQUENCE>([^\\n]+))?\\s*(?:<FILENAME>([^\\n]+))?[^<]*<TEXT>\\s*(.*?)\\s*</TEXT>\\s*</DOCUMENT>",
            RegexOption.DOT_MATCHES_ALL
        )

        documentPattern.findAll(content).forEachIndexed { index, match ->
            val type = match.groupValues.getOrNull(1)?.trim() ?: "UNKNOWN"
            val text = match.groupValues.getOrNull(4)?.trim() ?: ""
            sections["$type-$index"] = text.take(10000)
        }

        if (sections.isEmpty()) {
            sections["content"] = content
        }

        return sections
    }
}
